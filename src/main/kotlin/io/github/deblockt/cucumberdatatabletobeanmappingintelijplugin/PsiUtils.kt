package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.intellij.patterns.PlatformPatterns
import org.jetbrains.plugins.cucumber.psi.GherkinElementTypes

val IS_ROW_HEADER = PlatformPatterns.psiElement()
    .withElementType(GherkinElementTypes.TABLE_HEADER_ROW)

val IS_HEADER_CELL = PlatformPatterns.psiElement()
    .withElementType(GherkinElementTypes.TABLE_CELL)
    .withTreeParent(IS_ROW_HEADER)

val IS_STEP_DEFINITION = PlatformPatterns.psiElement()
    .withElementType(GherkinElementTypes.STEP)