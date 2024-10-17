package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix.generators

import com.intellij.codeInsight.daemon.impl.quickfix.CreateClassKind
import com.intellij.psi.PsiClass

interface DatatableClassGenerator {

    fun kind(): CreateClassKind

    fun initClass(newClass: PsiClass, fields: List<String>)
}