package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.CucumberDatatableBundle
import org.jetbrains.plugins.cucumber.CucumberElementFactory
import org.jetbrains.plugins.cucumber.psi.GherkinTable
import org.jetbrains.plugins.cucumber.psi.GherkinTableCell
import org.jetbrains.plugins.cucumber.psi.GherkinTableRow


class AddMissingColumns(private val row: GherkinTableRow, private val missingColumns: List<String>) :
    BaseIntentionAction() {
    override fun getFamilyName(): String {
        return CucumberDatatableBundle.message("intention.missing.fields.add.family")
    }

    override fun getText(): String {
        return CucumberDatatableBundle.message("intention.missing.fields.text", missingColumns)
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return true
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        ApplicationManager.getApplication().invokeLater {
            val datatable = PsiTreeUtil.getParentOfType(row, GherkinTable::class.java)
            WriteCommandAction.writeCommandAction(project).run<RuntimeException> {
                val element = CucumberElementFactory.createTempPsiFile(
                    project, """
                        Feature: dummy Feature
                            
                        Scenario: dummy Scenario
                            Given dummy step
                            |${missingColumns.joinToString("|")}|
                    """.trimIndent()
                )
                val newElements = PsiTreeUtil.findChildrenOfType(element, GherkinTableCell::class.java)
                val pipe = newElements.first().prevSibling
                newElements.forEach { row.add(it); row.add(it.nextSibling) }

                if (pipe != null && datatable != null) {
                    addMissingPipeOnDataRow(pipe, datatable)
                }
                if (editor != null) {
                    val psiDocumentManager = PsiDocumentManager.getInstance(project)
                    psiDocumentManager.doPostponedOperationsAndUnblockDocument(editor.document)
                    datatable?.replace(CodeStyleManager.getInstance(project).reformat(datatable))
                }
            }
        }
    }

    private fun addMissingPipeOnDataRow(pipe: PsiElement, datatable: GherkinTable) {
        val headerCellLength = datatable.headerRow?.psiCells?.size ?: 0
        datatable.dataRows.forEach { dataRow ->
            if (dataRow.psiCells.size < headerCellLength) {
                for (i in 1..(headerCellLength - dataRow.psiCells.size)) {
                    dataRow.add(pipe)
                }
            }
        }
    }
}