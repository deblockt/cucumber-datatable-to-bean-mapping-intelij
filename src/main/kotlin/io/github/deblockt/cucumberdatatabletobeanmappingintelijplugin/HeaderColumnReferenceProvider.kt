package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceParameterList
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class HeaderColumnReferenceProvider: PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val matchingField = datatableFields(element)
                .filter { it.name.contains(element.text) }
        if (matchingField.isNotEmpty()) {
            val range = TextRange(0, element.text.length)
            return arrayOf(
                    MyVariantReference(element, range, matchingField[0].psiField)
            )
        }
        return PsiReference.EMPTY_ARRAY
    }
}