package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.cucumber.CucumberElementFactory
import org.jetbrains.plugins.cucumber.psi.GherkinTableCell

class RowHeaderElementManipulator: AbstractElementManipulator<GherkinTableCell>() {

    override fun handleContentChange(element: GherkinTableCell, textRange: TextRange, newContent: String?): GherkinTableCell {
        val expectedName = nameResolver(options(element)).build(newContent)

        val gherkinsFile = CucumberElementFactory.createTempPsiFile(element.project, """
                    Feature: dummy Feature
                        
                    Scenario: dummy Scenario
                        Given dummy step
                        | ${expectedName.first()} |
                """.trimIndent())
        val newElement = PsiTreeUtil.findChildOfType(gherkinsFile, GherkinTableCell::class.java)!!

        return element.replace(newElement) as GherkinTableCell
    }
}