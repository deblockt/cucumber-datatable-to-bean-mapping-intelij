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
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor
import org.jetbrains.plugins.cucumber.psi.GherkinFileType
import org.jetbrains.plugins.cucumber.psi.GherkinTableCell
import org.jetbrains.plugins.cucumber.psi.GherkinTableRow


class CucumberDatatableJavaFieldDefinitionSearch :
    QueryExecutor<PsiReference?, ReferencesSearch.SearchParameters?> {

    override fun execute(
        queryParameters: ReferencesSearch.SearchParameters,
        processor: Processor<in PsiReference?>
    ): Boolean {
        val myElement = queryParameters.elementToSearch
        if (myElement !is PsiField && myElement !is PsiRecordComponent) {
            return true;
        }
        val psiNamesElement = myElement as PsiNamedElement
        val hasNameOverride = ReadAction.compute<Boolean, RuntimeException> {
            annotationParamValue(myElement as PsiModifierListOwner, Column::class, "value").isNotEmpty()
        }
        if (hasNameOverride) {
            return true
        }

        val psiClass: PsiClass? = ReadAction.compute<PsiClass?, RuntimeException> { (myElement as PsiMember).containingClass }
        val isDatatableClass = ReadAction.compute<Boolean, RuntimeException> { hasDataTableWithHeaderAnnotation(psiClass) }
        if (!isDatatableClass) {
            return true
        }

        val stepSearchScope = ReadAction.compute<SearchScope, RuntimeException> {
            if (psiNamesElement.containingFile.virtualFile != null) {
                val module =
                    ProjectRootManager.getInstance(psiNamesElement.project).fileIndex.getModuleForFile(psiNamesElement.containingFile.virtualFile)
                module?.moduleWithDependentsScope ?: EverythingGlobalScope()
            } else {
                EverythingGlobalScope()
            }
        }
        ReadAction.compute<Unit, RuntimeException> {
            val methods = buildStepDefAnnotations(myElement, stepSearchScope)
                .flatMap { AnnotatedElementsSearch.searchPsiMethods(it, stepSearchScope) }

            val stepsMethods = methods.filter { containsReference(datatableClass(it), psiClass!!) }
            val gherkinsSteps = stepsMethods.flatMap { findGherkinsSteps(it, queryParameters.scopeDeterminedByUser) }
            processHeaderReference(gherkinsSteps, myElement, processor)
        }

        return true
    }

    private fun processHeaderReference(steps: List<PsiElement>, updatedField: PsiNamedElement, processor: Processor<in PsiReference?>) {
        val expectedName = nameResolver(options(updatedField)).build(updatedField.name)
        steps.forEach { step ->
            val header = PsiTreeUtil.findChildOfType(step, GherkinTableRow::class.java)
            val cells = PsiTreeUtil.findChildrenOfType(header, GherkinTableCell::class.java)
            cells.forEach { cell ->
                if (expectedName.contains(cell.text.trim())) {
                    processor.process(MyVariantReference(cell, TextRange(0, cell.text.length), updatedField))
                }
            }
        }
    }

    private fun findGherkinsSteps(method: PsiMethod, scope: SearchScope): List<PsiElement> {
        val gherkinsScope = GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.EMPTY_SCOPE.union(scope), GherkinFileType.INSTANCE)
        val result = mutableListOf<PsiElement>()
        JavaFindUsagesHelper.processElementUsages(method, JavaMethodFindUsagesOptions(gherkinsScope)) {
            if (it.element !== null) {
                result.add(it.element!!)
            }
            true
        }
        return result
    }

    private fun containsReference(datatableClass: PsiClass?, searchingClass: PsiClass): Boolean {
        if (datatableClass == null) {
            return false
        }
        if (datatableClass == searchingClass) {
            return true
        }
        val module = ProjectRootManager.getInstance(datatableClass.project).fileIndex.getModuleForFile(datatableClass.containingFile.originalFile.virtualFile)
        val customConverters = customConverters(module!!)

        return datatableClass.allFields
            .filter { isADatatableColumn(it) }
            .filter {
                val hasConverter = customConverters.any { converter -> converter.returnType == it.type }
                val isNestedDataTableObject = !hasConverter
                        && it.type is PsiClassReferenceType
                        && hasDataTableWithHeaderAnnotation((it.type as PsiClassReferenceType).resolve())
                return isNestedDataTableObject && containsReference(
                    (it.type as PsiClassReferenceType).resolve()!!,
                    searchingClass
                )
            }.isNotEmpty()
    }

    private fun buildStepDefAnnotations(element: PsiElement, scope: SearchScope): List<PsiClass> {
        val scope = GlobalSearchScope.allScope(element.project)
        return listOf("io.cucumber.java.en.Given", "io.cucumber.java.en.When", "io.cucumber.java.en.Then", "io.cucumber.java.en.And")
                .map { JavaPsiFacade.getInstance(element.project).findClass(it, scope)!! }
    }
}