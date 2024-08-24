package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix.CreateFieldFromColumnHeader
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix.RenameColumnHeader
import org.apache.commons.text.similarity.LevenshteinDistance

class UnresolvedHeaderNameAnnotator: Annotator {
    private val levenshtein = LevenshteinDistance()

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!IS_HEADER_CELL.accepts(element) || datatableClass(element) == null) {
            return;
        }
        val fields = datatableFields(element)
        val headerContent = element.text.trim()
        val numberOfMatch = fields
                .flatMap { it.name }
                .count { it == headerContent }

        if (numberOfMatch == 0) {
            val bestHeaderMatch = fields
                .flatMap { it.name }
                .sortedBy { levenshtein.apply(it, headerContent) }
                .firstOrNull()

            holder.newAnnotation(HighlightSeverity.ERROR, CucumberDatatableBundle.message("annotation.unresolved.header.text"))
                    .range(element.textRange)
                    .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                    .withFix(CreateFieldFromColumnHeader(element))
                    .withFix(RenameColumnHeader(element, bestHeaderMatch))
                    .create();
        }
    }
}