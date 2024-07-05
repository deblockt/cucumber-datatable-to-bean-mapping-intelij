package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.deblock.cucumber.datatable.annotations.Column
import com.deblock.cucumber.datatable.annotations.DataTableWithHeader
import com.deblock.cucumber.datatable.annotations.Ignore
import com.deblock.cucumber.datatable.mapper.datatable.ColumnName
import com.deblock.cucumber.datatable.mapper.datatable.FieldResolver.FieldInfo
import com.deblock.cucumber.datatable.mapper.datatable.fieldresolvers.ImplicitFieldResolver
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.PsiTreeUtil
import kotlin.reflect.KClass

fun isADatatableColumn(field: PsiField): Boolean {
    if (options().fieldResolverClass.equals(ImplicitFieldResolver::class)) {
        return field.annotations
                .count { it.qualifiedName !== Ignore::class.qualifiedName } == 0
    }
    return field.annotations
                .count { it.qualifiedName == Column::class.qualifiedName } > 0
}

fun hasDataTableWithHeaderAnnotation(classField: PsiClass): Boolean {
    return classField.annotations
            .count { it.qualifiedName == DataTableWithHeader::class.qualifiedName } > 0
}

fun annotationParamValue(field: PsiField, type: KClass<out Any>, paramName: String, isDefaultParam: Boolean = false): List<String> {
    return field.annotations
            .asSequence()
            .filter { it.qualifiedName == type.qualifiedName }
            .flatMap { PsiTreeUtil.findChildrenOfType(it, PsiNameValuePair::class.java) }
            .filter { (isDefaultParam && it.name == null) || it.name == paramName }
            .flatMap {
                if (it.value is PsiLiteralExpression) {
                    arrayOf(it.value).asIterable()
                } else {
                    PsiTreeUtil.findChildrenOfType(it.value, PsiLiteralExpression::class.java)
                }
            }
            .mapNotNull { it?.text?.trim('"') }
            .toList()
}

fun fieldInfo(field: PsiField): FieldInfo {
    val annotationNames = annotationParamValue(field, Column::class, "value", true)
    val names = annotationNames.ifEmpty { nameResolver(options()).build(field.name) }
    val description = annotationParamValue(field, Column::class, "description")
    val defaultValue = annotationParamValue(field, Column::class, "defaultValue")
    val mandatory = annotationParamValue(field, Column::class, "mandatory")
    val optionalBoolean = if (mandatory.isEmpty()) {
        options().fieldResolverClass.equals(ImplicitFieldResolver::class)
    } else {
        mandatory.first() == "false"
    }
    return FieldInfo(
            ColumnName(names),
            optionalBoolean,
            description.firstOrNull(),
            defaultValue.firstOrNull()
    )
}

fun datatableClass(element: PsiElement): PsiClass? {
    val step = PsiTreeUtil.getParentOfType(element,org.jetbrains.plugins.cucumber.psi.impl.GherkinStepImpl::class.java)
            ?: return null;
    val reference = step.references[0] as PsiPolyVariantReference
    val results = reference.multiResolve(true)
    if (results.isEmpty()) {
        return null
    }
    val methodRef = results[0].element
    val parameters = PsiTreeUtil.findChildOfType(methodRef, PsiParameterList::class.java)
    val lastParameter = PsiTreeUtil.getChildrenOfType(parameters, PsiParameter::class.java)?.last()
    val lastParameterType = PsiTreeUtil.findChildOfType(lastParameter, PsiTypeElement::class.java)
    val paramterizedType = PsiTreeUtil.findChildOfType(lastParameterType, PsiReferenceParameterList::class.java) ?: return null;
    val clazz = if (paramterizedType.typeArguments.isEmpty()) {
        lastParameterType?.type as PsiClassReferenceType
    } else {
        paramterizedType.typeArguments.first() as PsiClassReferenceType
    }
    if (clazz.resolve() != null && hasDataTableWithHeaderAnnotation(clazz.resolve()!!)) {
        return clazz.resolve()
    }
    return null
}
fun datatableFields(element: PsiElement): List<DataTablePsiField> {
    val elementType = datatableClass(element)
    return if (elementType == null) emptyList() else datatableFields(elementType, ColumnName())
}

fun datatableFields(classField: PsiClass, parentName: ColumnName): List<DataTablePsiField> {
    if (!hasDataTableWithHeaderAnnotation(classField)) {
        return emptyList()
    }
    return classField.allFields
            .filter { isADatatableColumn(it) }
            .flatMap {
                val fieldInfo = fieldInfo(it)
                if (hasDataTableWithHeaderAnnotation((it.type as PsiClassReferenceType).resolve()!!)) {
                    datatableFields((it.type as PsiClassReferenceType).resolve()!!, fieldInfo.columnName)
                } else {
                    listOf(DataTablePsiField(it, parentName.addChild(fieldInfo.columnName), fieldInfo.description))
                }
            }
}

class DataTablePsiField(val psiField: PsiField, val name: ColumnName, val description: String?) {

}
