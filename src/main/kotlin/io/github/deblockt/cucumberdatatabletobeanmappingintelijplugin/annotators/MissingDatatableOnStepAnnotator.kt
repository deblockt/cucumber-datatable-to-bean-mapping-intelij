package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.annotators

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.IS_STEP_DEFINITION
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix.CreateDatatableClass
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix.generators.DatatableClassGeneratorFactory
import org.jetbrains.plugins.cucumber.psi.GherkinStep
import java.util.regex.Pattern

class MissingDatatableOnStepAnnotator: Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!IS_STEP_DEFINITION.accepts(element) || element !is GherkinStep) {
            return;
        }
        val datatable = element.table ?: return

        val stepDefinition = element.findDefinitions().firstOrNull()
        if (stepDefinition?.cucumberRegex == null || stepDefinition.element !is PsiMethod) {
            return;
        }
        val numberOfStepParameters = numberOfStepParameter(stepDefinition.cucumberRegex!!)
        val methodNumberOfParams = stepDefinition.variableNames.size
        if (numberOfStepParameters == -1 || methodNumberOfParams >= numberOfStepParameters + 1) {
            return
        }

        val annotationBuilder = holder.newAnnotation(HighlightSeverity.ERROR, "Missing datable on step definition")
            .range(element.textRange)
            .highlightType(ProblemHighlightType.WARNING);

        DatatableClassGeneratorFactory.getGenerators(element).forEach { builder ->
            annotationBuilder.withFix(CreateDatatableClass(datatable, stepDefinition.element as PsiMethod, builder))
        }

        return annotationBuilder.create();
    }

    private fun numberOfStepParameter(regex: String): Int {
        return Pattern.compile(regex).matcher("").groupCount()
    }
}