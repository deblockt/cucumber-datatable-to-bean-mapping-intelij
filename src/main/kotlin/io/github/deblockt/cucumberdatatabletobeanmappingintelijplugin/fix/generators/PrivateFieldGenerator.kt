package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix.generators

import com.intellij.codeInsight.daemon.impl.quickfix.CreateClassKind
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier

class PrivateFieldGenerator(
    private val delegatedGenerator: DatatableClassGenerator
): DatatableClassGenerator {
    override fun kind(): CreateClassKind {
        return delegatedGenerator.kind()
    }

    override fun initClass(newClass: PsiClass, fields: List<String>) {
        delegatedGenerator.initClass(newClass, fields)
        newClass.fields.forEach { field ->
            field.modifierList?.setModifierProperty(PsiModifier.PUBLIC, false);
            field.modifierList?.setModifierProperty(PsiModifier.PRIVATE, true);
        }
    }
}