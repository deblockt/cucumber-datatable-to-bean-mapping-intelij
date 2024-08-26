package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix.generators

import com.intellij.codeInsight.daemon.impl.quickfix.CreateClassKind
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaParserFacade
import com.intellij.psi.codeStyle.JavaCodeStyleManager

class RecordImplicitGenerator(
    private val codeStyleManager: JavaCodeStyleManager,
    private val parser: PsiJavaParserFacade
) : DatatableClassGenerator {
    override fun kind(): CreateClassKind {
        return CreateClassKind.RECORD
    }

    override fun initClass(newClass: PsiClass, fields: List<String>) {
        val recordComponent = fields.joinToString(",") { field -> "String $field" }
        val recordHeader = parser.createRecordHeaderFromText(recordComponent, newClass)
        newClass.recordHeader?.replace(recordHeader)

        val addedAnnotation =
            newClass.modifierList?.addAnnotation("com.deblock.cucumber.datatable.annotations.DataTableWithHeader");
        if (addedAnnotation != null) {
            codeStyleManager.shortenClassReferences(addedAnnotation)
        }
    }
}