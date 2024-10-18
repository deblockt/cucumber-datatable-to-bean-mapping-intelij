package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.endOffset
import org.jetbrains.plugins.cucumber.psi.GherkinTable

class HeaderRowAutocompleteInsertHandler: InsertHandler<LookupElement> {

    override fun handleInsert(context: InsertionContext, element: LookupElement) {
        val cell = context.file.findElementAt(context.startOffset)?.parent
        if (cell != null) {
            val hasTextAfterAutocomplete = cell.text.replace(element.lookupString, "").trim().length > 1
            val nextSibling = findNextNonSpaceSibling(cell)
            val table = PsiTreeUtil.getParentOfType(cell, GherkinTable::class.java)

            if (nextSibling == null || hasTextAfterAutocomplete) {
                insertPipe(context.selectionEndOffset, context)

                val headerCell = getHeaderIndex(cell)
                table?.dataRows?.forEach {
                    val endOffset =
                        if (it.psiCells.size < headerCell) {
                            it.endOffset
                        } else {
                            it.psiCells[headerCell - 1].textOffset
                        }
                    insertPipe(endOffset, context)
                }

                PsiDocumentManager.getInstance(context.project).commitDocument(context.editor.document)
            }
            table?.replace(CodeStyleManager.getInstance(context.project).reformat(table))
        }
    }

    private fun insertPipe(offset: Int, context: InsertionContext) {
        context.editor.document.insertString(offset, "|")
    }

    private fun getHeaderIndex(element: PsiElement): Int {
        return countPipeNumberPrevSibling(element)
    }

    private fun findNextNonSpaceSibling(element: PsiElement?): PsiElement? {
        if (element == null || element.nextSibling == null) return null
        if (element.nextSibling is PsiWhiteSpace) {
            return findNextNonSpaceSibling(element.nextSibling)
        }
        return element.nextSibling
    }

    private fun countPipeNumberPrevSibling(element: PsiElement?, count: Int = 0): Int {
        if (element == null || element.prevSibling == null) return count
        if (element.prevSibling.text == "|") {
            return countPipeNumberPrevSibling(element.prevSibling, count + 1)
        }
        return countPipeNumberPrevSibling(element.prevSibling, count)
    }
}