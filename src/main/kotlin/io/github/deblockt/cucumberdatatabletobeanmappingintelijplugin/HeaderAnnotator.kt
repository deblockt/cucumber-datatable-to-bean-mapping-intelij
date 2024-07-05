package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

class HeaderAnnotator: Annotator {
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
            holder.newAnnotation(HighlightSeverity.ERROR, "Unresolved header name")
                    .range(element.textRange)
                    .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                    // ** Tutorial step 19. - Add a quick fix for the string containing possible properties
                    //.withFix(CustomFix(element, "new name"))
                    //.withFix()
                    .create();
        }
    }
}