package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix

import com.deblock.cucumber.datatable.mapper.datatable.fieldresolvers.DeclarativeFieldResolver
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.*
import com.intellij.psi.util.elementType
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.CucumberDatatableBundle
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.datatableClass
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.options

class CreateFieldFromColumnHeader(columnHeader: PsiElement): BaseIntentionAction() {
    private val targetClass = datatableClass(columnHeader)
    private val fieldName = columnHeader.text.replace(Regex(" .")) { it.value.trim().uppercase() }

    override fun getFamilyName(): String {
        return CucumberDatatableBundle.message("intention.create.field.from.column.family")
    }

    override fun getText(): String {
        return CucumberDatatableBundle.message("intention.create.field.from.column.text", fieldName, targetClass?.name!!)
    }

    override fun isAvailable(project: Project, editor: Editor?, psiFile: PsiFile?): Boolean {
        return targetClass != null
    }

    override fun invoke(project: Project, editor: Editor?, psiFile: PsiFile?) {
        if (psiFile == null) {
            return;
        }
        ApplicationManager.getApplication().invokeLater {
            val virtualFile = targetClass!!.containingFile.virtualFile

            WriteCommandAction.writeCommandAction(project).run<RuntimeException> {
                val classFile = (PsiManager.getInstance(project).findFile(virtualFile))!! as PsiJavaFile
                val classToUpdate = classFile.classes.first { clazz -> clazz.name == targetClass.name }
                if (classToUpdate != null) {
                    val needColumnAnnotation = options(psiFile).fieldResolverClass == DeclarativeFieldResolver::class.java
                    if (classToUpdate.isRecord) {
                        val recordHeader = JavaPsiFacade.getInstance(project).parserFacade.createRecordHeaderFromText(
                            ", ${if (needColumnAnnotation) "@com.deblock.cucumber.datatable.annotations.Column" else ""} String $fieldName",
                            classToUpdate
                        )
                        val recordToAdd = recordHeader.recordComponents.first()
                        if (needColumnAnnotation) {
                            classFile.importClass(recordToAdd.annotations[0].resolveAnnotationType()!!)
                        }
                        val newComponent = classToUpdate.addAfter(recordToAdd, classToUpdate.recordComponents.last())
                        if (classToUpdate.recordComponents.size > 1) {
                            classToUpdate.addBefore(
                                findPrevComma(recordToAdd)!!,
                                newComponent
                            )
                        }
                        focusRecordComponentType(classToUpdate, project)
                    } else {
                        val havePublicFields = classToUpdate.fields.count { field -> field.modifierList!!.hasModifierProperty("public") }
                        val modifier = if (havePublicFields > 0) "public" else "private"

                        val parser = JavaPsiFacade.getInstance(project).parserFacade;
                        val fieldToAdd = parser.createFieldFromText(
                            """
                                ${if (needColumnAnnotation) "@com.deblock.cucumber.datatable.annotations.Column" else ""}
                                $modifier String $fieldName;
                            """.trimIndent().trim(),
                            classToUpdate
                        )
                        if (needColumnAnnotation) {
                            classFile.importClass(
                                fieldToAdd.annotations[0].resolveAnnotationType()!!
                            )
                        }

                        classToUpdate.addAfter(fieldToAdd, classToUpdate.fields.last())

                        val lastGetterSetter = classToUpdate.allMethods
                            .filter { method -> method.containingClass == classToUpdate }
                            .lastOrNull { method -> method.name.startsWith("get") || method.name.startsWith("set") }

                        if (lastGetterSetter != null && lastGetterSetter.isPhysical) {
                            val getterToAdd = parser.createMethodFromText("""
                                public String get${fieldName[0].uppercase()}${fieldName.substring(1)}() {
                                    return $fieldName;
                                }
                            """.trimIndent(), classToUpdate)
                            val addedGetter = classToUpdate.addAfter(getterToAdd, lastGetterSetter)

                            val setterToAdd = parser.createMethodFromText("""
                                public void set${fieldName[0].uppercase()}${fieldName.substring(1)}(String $fieldName) {
                                    this.$fieldName = $fieldName;
                                }
                            """.trimIndent(), classToUpdate)
                            classToUpdate.addAfter(setterToAdd, addedGetter)
                        }

                        focusFieldType(classToUpdate, project)
                    }
                }
            }
        }
    }

    private fun findPrevComma(element: PsiElement?): PsiElement? {
        if (element == null) {
            return null
        }
        if (element.elementType == JavaTokenType.COMMA) {
            return element
        }
        return findPrevComma(element.prevSibling)
    }

    private fun focusFieldType(classToUpdate: PsiClass, project: Project) {
        (classToUpdate.fields.last().typeElement!!.navigationElement as Navigatable).navigate(
            true
        )
        val editor = FileEditorManager.getInstance(project).selectedTextEditor!!

        editor.caretModel.moveCaretRelatively(
            classToUpdate.fields.last().typeElement!!.textLength,
            0,
            true,
            true,
            true
        )
    }

    private fun focusRecordComponentType(classToUpdate: PsiClass, project: Project) {
        (classToUpdate.recordComponents.last().typeElement!!.navigationElement as Navigatable).navigate(
            true
        )
        val editor = FileEditorManager.getInstance(project).selectedTextEditor!!
        editor!!.caretModel.moveCaretRelatively(
            classToUpdate.fields.last().typeElement!!.textLength,
            0,
            true,
            true,
            true
        )
    }

}
