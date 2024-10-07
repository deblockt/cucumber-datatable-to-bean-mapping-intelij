package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*

class MyVariantReference(element: PsiElement, rangeInElement: TextRange?, private val result: PsiElement)
    : PsiReferenceBase<PsiElement?>(element, rangeInElement), PsiPolyVariantReference {

    override fun multiResolve(b: Boolean): Array<ResolveResult> {
        return arrayOf(
                PsiElementResolveResult(result)
        )
    }

    override fun resolve(): PsiElement? {
        val resolveResults = multiResolve(false)
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

}
