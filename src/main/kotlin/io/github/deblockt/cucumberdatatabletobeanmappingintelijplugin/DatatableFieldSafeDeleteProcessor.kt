package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.deblock.cucumber.datatable.annotations.Column
import com.intellij.find.findUsages.JavaFindUsagesHelper
import com.intellij.find.findUsages.JavaMethodFindUsagesOptions
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.safeDelete.SafeDeleteProcessorDelegateBase
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo
import com.intellij.usageView.UsageInfo
import org.jetbrains.plugins.cucumber.psi.GherkinFileType
import org.jetbrains.plugins.cucumber.psi.GherkinTableCell
import org.jetbrains.plugins.cucumber.psi.GherkinTableRow

/**
 * Processor that handles safe deletion of datatable fields.
 * When a field from a @DataTableWithHeader class is deleted, this processor
 * finds all corresponding column headers in Gherkin feature files and offers
 * to delete them as well.
 */
class DatatableFieldSafeDeleteProcessor : SafeDeleteProcessorDelegateBase() {

    override fun handlesElement(element: PsiElement): Boolean {
        if (element !is PsiField && element !is PsiRecordComponent) {
            return false
        }

        // Check if the field has a @Column annotation with a name override
        val hasNameOverride = ReadAction.compute<Boolean, RuntimeException> {
            annotationParamValue(element as PsiModifierListOwner, Column::class, "value").isNotEmpty()
        }
        if (hasNameOverride) {
            // If the field has an explicit name in @Column annotation,
            // we can't automatically determine which columns to delete
            return false
        }

        // Check if the field belongs to a @DataTableWithHeader class
        val psiClass = ReadAction.compute<PsiClass?, RuntimeException> {
            (element as PsiMember).containingClass
        }
        return ReadAction.compute<Boolean, RuntimeException> {
            hasDataTableWithHeaderAnnotation(psiClass)
        }
    }

    override fun findUsages(
        element: PsiElement,
        allElementsToDelete: Array<out PsiElement>,
        result: MutableList<UsageInfo>
    ): Collection<PsiElement> {
        if (element !is PsiField && element !is PsiRecordComponent) {
            return emptyList()
        }

        val psiNamesElement = element as PsiNamedElement
        val psiClass = ReadAction.compute<PsiClass?, RuntimeException> {
            (element as PsiMember).containingClass
        }

        // Find the search scope for step definitions
        val stepSearchScope = ReadAction.compute<SearchScope, RuntimeException> {
            if (psiNamesElement.containingFile.virtualFile != null) {
                val module = ProjectRootManager.getInstance(psiNamesElement.project)
                    .fileIndex
                    .getModuleForFile(psiNamesElement.containingFile.virtualFile)
                module?.moduleWithDependentsScope ?: EverythingGlobalScope()
            } else {
                EverythingGlobalScope()
            }
        }

        // Find all Gherkin table cells that reference this field
        ReadAction.compute<Unit, RuntimeException> {
            val methods = buildStepDefAnnotations(element, stepSearchScope)
                .flatMap { AnnotatedElementsSearch.searchPsiMethods(it, stepSearchScope) }

            val stepsMethods = methods.filter { containsReference(datatableClass(it), psiClass!!) }
            val gherkinsSteps = stepsMethods.flatMap { findGherkinsSteps(it, stepSearchScope) }

            addColumnCellUsages(gherkinsSteps, psiNamesElement, result)
        }

        return emptyList()
    }

    /**
     * Adds usage information for all table cells that reference the given field
     */
    private fun addColumnCellUsages(
        steps: List<PsiElement>,
        deletedField: PsiNamedElement,
        result: MutableList<UsageInfo>
    ) {
        val expectedName = nameResolver(options(deletedField)).build(deletedField.name)

        steps.forEach { step ->
            val header = PsiTreeUtil.findChildOfType(step, GherkinTableRow::class.java)
            val cells = PsiTreeUtil.findChildrenOfType(header, GherkinTableCell::class.java)

            cells.forEachIndexed { index, cell ->
                if (expectedName.contains(cell.text.trim())) {
                    // Create a custom usage info that will delete the entire column
                    result.add(ColumnDeleteUsageInfo(cell, deletedField, index))
                }
            }
        }
    }

    /**
     * Finds all Gherkin steps that use the given step definition method
     */
    private fun findGherkinsSteps(method: PsiMethod, scope: SearchScope): List<PsiElement> {
        val gherkinsScope = GlobalSearchScope.getScopeRestrictedByFileTypes(
            GlobalSearchScope.EMPTY_SCOPE.union(scope),
            GherkinFileType.INSTANCE
        )
        val result = mutableListOf<PsiElement>()

        JavaFindUsagesHelper.processElementUsages(method, JavaMethodFindUsagesOptions(gherkinsScope)) {
            if (it.element != null) {
                result.add(it.element!!)
            }
            true
        }
        return result
    }

    /**
     * Checks if the datatable class contains a reference to the searching class
     */
    private fun containsReference(datatableClass: PsiClass?, searchingClass: PsiClass): Boolean {
        if (datatableClass == null) {
            return false
        }
        if (datatableClass == searchingClass) {
            return true
        }

        val module = ProjectRootManager.getInstance(datatableClass.project)
            .fileIndex
            .getModuleForFile(datatableClass.containingFile.originalFile.virtualFile)
        val customConverters = customConverters(module!!)

        return datatableClass.allFields
            .filter { isADatatableColumn(it) }
            .any {
                val hasConverter = customConverters.any { converter -> converter.returnType == it.type }
                val isNestedDataTableObject = !hasConverter &&
                        it.type is PsiClassReferenceType &&
                        hasDataTableWithHeaderAnnotation((it.type as PsiClassReferenceType).resolve())

                isNestedDataTableObject && containsReference(
                    (it.type as PsiClassReferenceType).resolve()!!,
                    searchingClass
                )
            }
    }

    /**
     * Builds the list of step definition annotation classes
     */
    private fun buildStepDefAnnotations(element: PsiElement, scope: SearchScope): List<PsiClass> {
        val allScope = GlobalSearchScope.allScope(element.project)
        return listOf(
            "io.cucumber.java.en.Given",
            "io.cucumber.java.en.When",
            "io.cucumber.java.en.Then",
            "io.cucumber.java.en.And"
        ).mapNotNull {
            JavaPsiFacade.getInstance(element.project).findClass(it, allScope)
        }
    }

    /**
     * Custom usage info that knows how to delete a column from a Gherkin table
     */
    private class ColumnDeleteUsageInfo(
        cell: GherkinTableCell,
        referencedElement: PsiElement,
        private val columnIndex: Int
    ) : SafeDeleteReferenceSimpleDeleteUsageInfo(cell, referencedElement, false) {

        override fun deleteElement() {
            val headerCell = element as? GherkinTableCell ?: return
            val headerRow = headerCell.parent as? GherkinTableRow ?: return
            val table = headerRow.parent ?: return

            // Get all rows in the table
            val allRows = PsiTreeUtil.findChildrenOfType(table, GherkinTableRow::class.java)

            // Delete the column from all rows
            allRows.forEach { row ->
                val cells = PsiTreeUtil.findChildrenOfType(row, GherkinTableCell::class.java).toList()
                if (columnIndex < cells.size) {
                    val cellToDelete = cells[columnIndex]

                    // Delete the cell and the pipe separator after it
                    // If it's the last cell, delete the pipe before it instead
                    if (columnIndex == cells.size - 1 && columnIndex > 0) {
                        // Last cell: delete pipe before + cell
                        val prevPipe = cellToDelete.prevSibling
                        prevPipe?.delete()
                    } else {
                        // Not last cell: delete cell + pipe after
                        val nextPipe = cellToDelete.nextSibling
                        nextPipe?.delete()
                    }
                    cellToDelete.delete()
                }
            }
        }
    }
}
