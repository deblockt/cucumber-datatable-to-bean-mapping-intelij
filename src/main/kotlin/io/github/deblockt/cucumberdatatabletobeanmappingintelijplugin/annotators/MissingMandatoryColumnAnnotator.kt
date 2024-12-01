package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.annotators

import com.deblock.cucumber.datatable.mapper.datatable.ColumnName
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.IS_ROW_HEADER
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.datatableClass
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.datatableFields
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix.AddMissingColumns
import org.jetbrains.plugins.cucumber.psi.GherkinTableRow

class MissingMandatoryColumnAnnotator: Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!IS_ROW_HEADER.accepts(element) || datatableClass(element) == null) {
            return;
        }
        val row = element as GherkinTableRow
        val datatableFields = datatableFields(element)
        val currentFields = getAllFields(row)

        val missingColumns = datatableFields
            .filter { !it.optional }
            .filter { mandatoryColumn ->
                !currentFields.any { it.hasOneNameEquals(mandatoryColumn.name) }
            }

        if (missingColumns.isNotEmpty()) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Missing mandatory column")
                .range(element.textRange)
                .highlightType(ProblemHighlightType.WARNING)
                .withFix(AddMissingColumns(row, missingColumns.map { it.name.firstName() }))
                .create()
        }
    }

    private fun getAllFields(gherkinTableRow: GherkinTableRow): List<ColumnName> {
        return gherkinTableRow.psiCells
            .map { it.name?.trim() ?: "" }
            .map { ColumnName(it) }
    }
}