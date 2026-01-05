package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.codeInsight.intention.impl.CreateClassDialog
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo.CustomDiff
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo.EMPTY
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.jetbrains.rd.generator.nova.util.decapitalizeInvariant
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.CucumberDatatableBundle
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix.generators.ClassGeneratorInfo
import org.jetbrains.plugins.cucumber.psi.GherkinTable
import java.util.Locale.getDefault

class CreateDatatableClass(
    private val datatable: GherkinTable,
    private val stepJavaMethod: PsiMethod,
    private val classGenerator: ClassGeneratorInfo
): BaseIntentionAction() {
    private val fields = datatable.headerRow!!.psiCells.map { cell ->
        cell.text.trim().replace(Regex(" .")) { it.value.trim().uppercase() }
    }

    override fun getFamilyName(): String {
        return CucumberDatatableBundle.message("intention.create.datatable.class.family")
    }

    override fun getText(): String {
        return if (classGenerator.name == null) {
            CucumberDatatableBundle.message(
                "intention.create.datatable.class.text.without.name",
                classGenerator.generator.kind().name.lowercase()
            )
        } else {
            CucumberDatatableBundle.message(
                "intention.create.datatable.class.text.with.name",
                classGenerator.generator.kind().name.lowercase(),
                classGenerator.name
            )
        }
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return file != null && datatable.headerRow != null
    }

    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val module = ProjectRootManager.getInstance(datatable.project).fileIndex.getModuleForFile(datatable.containingFile.originalFile.virtualFile)
        if (module == null) {
            return;
        }
        val otherDatableSearchScope = GlobalSearchScope.moduleWithDependenciesScope(module)
        val otherCucumberClasses = AnnotatedElementsSearch.searchPsiClasses(datatableWithHeaderPsiClass(project, datatable), otherDatableSearchScope)
        val defaultPackage = otherCucumberClasses.mapNotNull { psiClass ->
            (psiClass.containingFile as PsiJavaFile).packageStatement?.packageName
        }.distinct().firstOrNull() ?: (stepJavaMethod.containingFile as PsiJavaFile).packageStatement?.packageName ?: ""

        val dialog = CreateClassDialog(
            project,
            CucumberDatatableBundle.message("intention.create.datatable.class.dialog.header",
                classGenerator.generator.kind().name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }
            ),
            "CucumberEntry",
            defaultPackage,
            classGenerator.generator.kind(),
            true,
            module
        );
        if (dialog.showAndGet()) {
            val createdClass = classGenerator.generator.kind().createInDirectory(dialog.targetDirectory, dialog.className)
            WriteAction.run<RuntimeException> {
                classGenerator.generator.initClass(createdClass, fields)
                addNewClassOnMethodParameter(createdClass)
            }
        }
    }

    private fun addNewClassOnMethodParameter(createdClass: PsiClass) {
        val parameter = JavaPsiFacade.getElementFactory(createdClass.project)
            .createParameterFromText("List<${createdClass.name}> ${createdClass.name?.decapitalizeInvariant()}List", createdClass)
        stepJavaMethod.parameterList.add(parameter)
    }

    private fun datatableWithHeaderPsiClass(project: Project, psiElement: PsiElement): PsiClass {
        return JavaPsiFacade.getElementFactory(project)
            .createAnnotationFromText("@com.deblock.cucumber.datatable.annotations.DataTableWithHeader", psiElement)
            .resolveAnnotationType()!!
    }

    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
        val module = ProjectRootManager.getInstance(datatable.project).fileIndex.getModuleForFile(datatable.containingFile.originalFile.virtualFile)
        if (module == null) {
            return EMPTY;
        }

        val createdClass = classGenerator.generator.kind().create(JavaPsiFacade.getElementFactory(project), "CucumberEntry")
        classGenerator.generator.initClass(createdClass, fields)
        CodeStyleManager.getInstance(project).reformat(createdClass)

        return CustomDiff(JavaFileType.INSTANCE, "", createdClass.text)
    }
}