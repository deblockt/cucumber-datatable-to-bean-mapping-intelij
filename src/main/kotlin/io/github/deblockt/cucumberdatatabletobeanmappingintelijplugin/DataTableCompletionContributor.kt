package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import org.jetbrains.plugins.cucumber.psi.GherkinTokenTypes
import org.jetbrains.plugins.cucumber.psi.impl.GherkinTableHeaderRowImpl


class DataTableCompletionContributor: CompletionContributor() {

    init {
        extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement()
                        .withElementType(GherkinTokenTypes.TABLE_CELL)
                        .withSuperParent(2, GherkinTableHeaderRowImpl::class.java),
                object : CompletionProvider<CompletionParameters>() {
                    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, resultSet: CompletionResultSet) {
                        datatableFields(parameters.position)
                                .flatMap { createElementsForEachName(it) }
                                .forEach { resultSet.addElement(it) }
                    }

                    private fun createElementsForEachName(datatableField: DataTablePsiField) =
                        datatableField.name.map { name ->
                            val element = LookupElementBuilder.create(name)
                                .withInsertHandler(HeaderRowAutocompleteInsertHandler())
                            if (datatableField.description !== null) {
                                element
                                    .appendTailText(" ${datatableField.description}", false)
                            } else {
                                element
                            }
                        }
                }
        )
    }
}
