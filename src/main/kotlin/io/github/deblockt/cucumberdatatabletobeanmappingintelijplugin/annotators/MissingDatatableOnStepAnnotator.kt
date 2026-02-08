package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.annotators

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.IS_STEP_DEFINITION
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix.CreateDatatableClass
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix.generators.DatatableClassGeneratorFactory
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.stepMethod
import org.jetbrains.plugins.cucumber.psi.GherkinStep
import java.util.regex.Pattern

class MissingDatatableOnStepAnnotator: Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!IS_STEP_DEFINITION.accepts(element) || element !is GherkinStep) {
            return;
        }
        val datatable = element.table ?: return

        val stepDefinition = element.findDefinitions().firstOrNull()
        val stepMethod = stepMethod(stepDefinition?.element)
        if (stepDefinition?.cucumberRegex == null || stepMethod == null) {
            return;
        }
        val numberOfStepParameters = numberOfStepParameter(stepDefinition.cucumberRegex!!)
        val methodNumberOfParams = stepDefinition.variableNames.size
        if (methodNumberOfParams >= numberOfStepParameters + 1) {
            return
        }

        val annotationBuilder = holder.newAnnotation(HighlightSeverity.ERROR, "Missing datable on step definition")
            .range(element.textRange)
            .highlightType(ProblemHighlightType.WARNING);

        DatatableClassGeneratorFactory.getGenerators(element).forEach { builder ->
            annotationBuilder.withFix(CreateDatatableClass(datatable, stepMethod, builder))
        }

        return annotationBuilder.create();
    }

    private fun numberOfStepParameter(regex: String): Int {
        return Pattern.compile(regex).matcher("").groupCount()
    }
}