package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.CucumberDatatableBundle
import org.jetbrains.plugins.cucumber.CucumberElementFactory
import org.jetbrains.plugins.cucumber.psi.GherkinTableCell

class RenameColumnHeader(private val columnHeader: PsiElement, private val newName: String?): BaseIntentionAction() {

    override fun getFamilyName(): String {
        return CucumberDatatableBundle.message("intention.rename.column.family")
    }

    override fun getText(): String {
        return CucumberDatatableBundle.message("intention.rename.column.text", newName!!)
    }

    override fun isAvailable(project: Project, editor: Editor?, psiFile: PsiFile?): Boolean {
        return newName !== null
    }

    override fun invoke(project: Project, editor: Editor?, psiFile: PsiFile?) {
        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.writeCommandAction(project).run<RuntimeException> {
                val element = CucumberElementFactory.createTempPsiFile(project, """
                    Feature: dummy Feature
                        
                    Scenario: dummy Scenario
                        Given dummy step
                        | $newName |
                """.trimIndent())
                val newElement = PsiTreeUtil.findChildOfType(element, GherkinTableCell::class.java)
                columnHeader.replace(newElement!!)
            }
        }
    }
}
