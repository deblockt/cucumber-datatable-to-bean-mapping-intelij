package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix.generators

import com.intellij.codeInsight.daemon.impl.quickfix.CreateClassKind
import com.intellij.psi.PsiClass
import com.intellij.psi.codeStyle.JavaCodeStyleManager

class LombokClassGenerator(
    private val codeStyleManager: JavaCodeStyleManager,
    private val delegatedGenerator: DatatableClassGenerator
): DatatableClassGenerator {
    override fun kind(): CreateClassKind {
        return delegatedGenerator.kind()
    }

    override fun initClass(newClass: PsiClass, fields: List<String>) {
        delegatedGenerator.initClass(newClass, fields)
        val addedLombok = newClass.modifierList?.addAnnotation("lombok.Data");
        if (addedLombok != null) {
            codeStyleManager.shortenClassReferences(addedLombok)
        }
    }
}