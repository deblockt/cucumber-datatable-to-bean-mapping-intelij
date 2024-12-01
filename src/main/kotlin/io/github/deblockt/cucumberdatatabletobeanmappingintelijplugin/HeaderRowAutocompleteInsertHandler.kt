package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.cucumber.psi.GherkinTable

class HeaderRowAutocompleteInsertHandler: InsertHandler<LookupElement> {

    override fun handleInsert(context: InsertionContext, element: LookupElement) {
        val cell = context.file.findElementAt(context.startOffset)?.parent
        if (cell != null) {
            val hasTextAfterAutocomplete = cell.text.replace(element.lookupString, "").trim().isNotEmpty()
            val nextSibling = findNextNonSpaceSibling(cell)

            if (nextSibling == null || hasTextAfterAutocomplete) {
                insertPipe(context.selectionEndOffset, context)
                PsiDocumentManager.getInstance(context.project).commitDocument(context.editor.document)
            }

            val headerCell = getHeaderIndex(cell)
            val table = PsiTreeUtil.getParentOfType(cell, GherkinTable::class.java)
            addMissingColumnOnEachRow(table, headerCell, context)

            PsiDocumentManager.getInstance(context.project).commitDocument(context.editor.document)

            table?.replace(CodeStyleManager.getInstance(context.project).reformat(table))
        }
    }

    private fun addMissingColumnOnEachRow(
        table: GherkinTable?,
        headerCell: Int,
        context: InsertionContext
    ) {
        val headerColumnNumber = table?.headerRow?.psiCells?.size ?: 0;

        table?.dataRows
            ?.filter { it.psiCells.size < headerColumnNumber  }
            ?.forEachIndexed { index, it ->
                val endOffset =
                    if (it.psiCells.size < headerCell) {
                        it.textRange.endOffset
                    } else {
                        it.psiCells[headerCell - 1].textOffset
                    }
                insertPipe(endOffset + index, context)
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