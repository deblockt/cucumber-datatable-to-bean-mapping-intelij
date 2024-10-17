package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix.generators

import com.intellij.codeInsight.daemon.impl.quickfix.CreateClassKind
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaParserFacade
import com.intellij.psi.codeStyle.JavaCodeStyleManager

class PublicFieldsImplicitClassGenerator(
    private val codeStyleManager: JavaCodeStyleManager,
    private val parser: PsiJavaParserFacade
): DatatableClassGenerator {
    override fun kind(): CreateClassKind {
        return CreateClassKind.CLASS
    }

    override fun initClass(newClass: PsiClass, fields: List<String>) {
        fields.forEach { f ->
            val fieldToAdd = parser.createFieldFromText("public String $f;", newClass)
            newClass.add(fieldToAdd)
        }
        val addedAnnotation = newClass.modifierList?.addAnnotation("com.deblock.cucumber.datatable.annotations.DataTableWithHeader");
        if (addedAnnotation != null) {
            codeStyleManager.shortenClassReferences(addedAnnotation)
        }
    }
}