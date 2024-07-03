package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.findFile
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.ElementPatternCondition
import com.intellij.patterns.InitialPatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.contextOfType
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext
import org.apache.commons.imaging.common.bytesource.ByteSourceInputStream
import org.apache.lucene.util.fst.FST.BytesReader
import org.jetbrains.plugins.cucumber.psi.GherkinElementType
import org.jetbrains.plugins.cucumber.psi.GherkinElementTypes
import org.jetbrains.plugins.cucumber.psi.GherkinTokenTypes
import java.io.ByteArrayInputStream
import java.util.Properties


class DataTableCompletionContributor: CompletionContributor() {

    init {
        extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement()
                        .withElementType(GherkinTokenTypes.TABLE_CELL),
                object : CompletionProvider<CompletionParameters>() {
                    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, resultSet: CompletionResultSet) {
                        datatableFields(parameters.position)
                                .map {
                                    val element = LookupElementBuilder.create(it.name)
                                    if (it.description !== null) {
                                        element
                                                .appendTailText(" ${it.description}", false)
                                    } else {
                                        element
                                    }
                                }
                                .forEach { resultSet.addElement(it) }
                    }
                }
        )
    }
}
