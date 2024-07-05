package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.intellij.patterns.ElementPattern
import com.intellij.patterns.ElementPatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.plugins.cucumber.psi.GherkinElementTypes

val IS_HEADER_CELL = PlatformPatterns.psiElement()
    .withElementType(GherkinElementTypes.TABLE_CELL)
    .withTreeParent(PlatformPatterns.psiElement().withElementType(GherkinElementTypes.TABLE_HEADER_ROW))