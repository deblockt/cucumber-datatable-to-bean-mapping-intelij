package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix.CreateDatatableClass
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix.generators.DatatableClassGeneratorFactory
import org.jetbrains.plugins.cucumber.java.CucumberJavaUtil
import org.jetbrains.plugins.cucumber.psi.GherkinTable

class MissingDatatableOnStepAnnotator: Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!IS_STEP_DEFINITION.accepts(element)) {
            return;
        }
        val datatable = PsiTreeUtil.findChildOfType(element, GherkinTable::class.java)
        if (datatable == null) {
            return;
        }

        val reference = element.references[0]
        val method = reference.resolve()
        if (method == null || method !is PsiMethod) {
            return
        }

        val numberOfStepParameters = numberOfStepParameter(method)
        val methodNumberOfParams = method.parameters.count()
        if (numberOfStepParameters == -1 || methodNumberOfParams >= numberOfStepParameters + 1) {
            return
        }

        val annotationBuilder = holder.newAnnotation(HighlightSeverity.ERROR, "Missing datable on step definition")
            .range(element.textRange)
            .highlightType(ProblemHighlightType.WARNING);

        DatatableClassGeneratorFactory.getGenerators(element).forEach { builder ->
            annotationBuilder.withFix(CreateDatatableClass(datatable, method, builder))
        }

        return annotationBuilder.create();
    }

    private fun numberOfStepParameter(method: PsiMethod): Int {
        val stepAnnotations = CucumberJavaUtil.getCucumberStepAnnotations(method)
        for (stepAnnotation in stepAnnotations) {
            val pattern = CucumberJavaUtil.getPatternFromStepDefinition(stepAnnotation!!) ?: continue
            return if (CucumberJavaUtil.isCucumberExpression(pattern)) {
                pattern.count { it == '{' }
            } else {
                // TODO implement regex parameter count
                -1
            }
        }
        return -1
    }
}