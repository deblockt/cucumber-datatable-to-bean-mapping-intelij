package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix.generators

import com.intellij.codeInsight.daemon.impl.quickfix.CreateClassKind
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.codeStyle.JavaCodeStyleManager

class DeclarativeGenerator(
    private val codeStyleManager: JavaCodeStyleManager,
    private val delegatedGenerator: DatatableClassGenerator
): DatatableClassGenerator {
    override fun kind(): CreateClassKind {
        return delegatedGenerator.kind()
    }

    override fun initClass(newClass: PsiClass, fields: List<String>) {
        delegatedGenerator.initClass(newClass, fields)
        properties(newClass).forEach { field ->
            val addedAnnotation = field.modifierList?.addAnnotation("com.deblock.cucumber.datatable.annotations.Column")
            if (addedAnnotation != null) {
                codeStyleManager.shortenClassReferences(addedAnnotation)
            }
        }
    }

    private fun properties(newClass: PsiClass): Array<out PsiModifierListOwner> {
        if (newClass.recordHeader != null) {
            return newClass.recordComponents
        }
        return newClass.fields
    }
}