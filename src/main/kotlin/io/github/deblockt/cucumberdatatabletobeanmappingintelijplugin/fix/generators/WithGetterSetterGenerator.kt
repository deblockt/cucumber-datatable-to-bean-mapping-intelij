package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix.generators

import com.intellij.codeInsight.daemon.impl.quickfix.CreateClassKind
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaParserFacade

class WithGetterSetterGenerator(
    private val parser: PsiJavaParserFacade,
    private val delegatedGenerator: DatatableClassGenerator
): DatatableClassGenerator {
    override fun kind(): CreateClassKind {
        return delegatedGenerator.kind()
    }

    override fun initClass(newClass: PsiClass, fields: List<String>) {
        delegatedGenerator.initClass(newClass, fields)
        fields.forEach { field ->
            val upperCaseField = field.replaceFirstChar { it.uppercase() }
            val setterMethod = parser.createMethodFromText("""
                public void set$upperCaseField(String $field) {
                    this.$field = $field;
                }
            """.trimIndent(), newClass)
            val getterMethod = parser.createMethodFromText("""
                public String get$upperCaseField() {
                    return this.$field;
                }
            """.trimIndent(), newClass)
            newClass.add(setterMethod);
            newClass.add(getterMethod);
        }
    }
}