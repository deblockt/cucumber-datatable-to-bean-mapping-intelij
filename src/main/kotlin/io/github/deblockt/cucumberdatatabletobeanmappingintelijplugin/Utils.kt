package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.PsiTreeUtil
import java.util.*

fun fieldNames(field: PsiField): Array<String> {
    if (field.annotations.isNotEmpty()) {
        val name = field.annotations
                .asSequence()
                .filter { it.qualifiedName == "com.deblock.cucumber.datatable.annotations.Column" }
                .flatMap { PsiTreeUtil.findChildrenOfType(it, PsiNameValuePair::class.java) }
                .filter { it.name == null || it.name == "value" }
                .flatMap {
                    if (it.value is PsiLiteralExpression) {
                        arrayOf(it.value).asIterable()
                    } else {
                        PsiTreeUtil.findChildrenOfType(it.value, PsiLiteralExpression::class.java)
                    }
                }
                .mapNotNull { it?.text?.trim('"') }
                .toList()
        if (name.isNotEmpty()) {
            return name.toTypedArray()
        }
    }
    return arrayOf(field.name, humanName(field.name))
}

fun humanName(fieldName: String): String {
    return fieldName.replace("([a-z])([A-Z])".toRegex(), "$1 $2").trim(' ').lowercase(Locale.getDefault())
}

fun datatableFields(element: PsiElement): Array<PsiField> {
    val step = PsiTreeUtil.getParentOfType(element,org.jetbrains.plugins.cucumber.psi.impl.GherkinStepImpl::class.java)
            ?: return emptyArray();
    val reference = step.references[0] as PsiPolyVariantReference
    val results = reference.multiResolve(true)
    if (results.isEmpty()) {
        return emptyArray()
    }
    val methodRef = results[0].element
    val parameters = PsiTreeUtil.findChildOfType(methodRef, PsiParameterList::class.java)
    val lastParameter = PsiTreeUtil.getChildrenOfType(parameters, PsiParameter::class.java)?.last()
    val lastParameterType = PsiTreeUtil.findChildOfType(lastParameter, PsiTypeElement::class.java)
    val paramterizedType = PsiTreeUtil.findChildOfType(lastParameterType, PsiReferenceParameterList::class.java) ?: return emptyArray();
    val elementType = if (paramterizedType.typeArguments.isEmpty()) {
        lastParameterType?.type as PsiClassReferenceType
    } else {
        paramterizedType.typeArguments.first() as PsiClassReferenceType
    }
    return elementType.resolve()?.allFields ?: return emptyArray()
}
