package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.fix.generators

import com.deblock.cucumber.datatable.mapper.datatable.fieldresolvers.ImplicitFieldResolver
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.util.containers.ContainerUtil
import io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin.options

object DatatableClassGeneratorFactory {

    fun getGenerators(element: PsiElement): List<ClassGeneratorInfo> {
        val codeStyleManager = JavaCodeStyleManager.getInstance(element.project)
        val parser = JavaPsiFacade.getInstance(element.project).parserFacade

        val defaultGenerator = if (isImplicitResolver(element)) {
            PublicFieldsImplicitClassGenerator(codeStyleManager, parser)
        } else {
            DeclarativeGenerator(codeStyleManager, PublicFieldsImplicitClassGenerator(codeStyleManager, parser))
        }
        val privateFieldClassGenerator = PrivateFieldGenerator(defaultGenerator)
        val withAccessorGenerator = if (hasLombokDependency(element)) {
            LombokClassGenerator(codeStyleManager, privateFieldClassGenerator)
        } else {
            WithGetterSetterGenerator(parser, privateFieldClassGenerator)
        }
        val recordImplicitGenerator = RecordImplicitGenerator(codeStyleManager, parser)
        val recordGenerator = if (isImplicitResolver(element)) {
            recordImplicitGenerator
        } else {
            DeclarativeGenerator(codeStyleManager, recordImplicitGenerator)
        }
        return listOf(
            ClassGeneratorInfo(null, recordGenerator),
            ClassGeneratorInfo("private fields", withAccessorGenerator),
            ClassGeneratorInfo("public fields", defaultGenerator)
        )
    }

    private fun isImplicitResolver(element: PsiElement) =
        options(element.containingFile).fieldResolverClass == ImplicitFieldResolver::class.java

    private fun hasLombokDependency(element: PsiElement): Boolean {
        val module = ProjectRootManager.getInstance(element.project).fileIndex.getModuleForFile(element.containingFile.originalFile.virtualFile)
        if (module == null) {
            return false;
        }
        return ContainerUtil.find(ModuleRootManager.getInstance(module).orderEntries) { entry ->
            if (entry !is LibraryOrderEntry) {
                false
            } else {
                entry.libraryName?.contains("org.projectlombok:lombok") == true
            }
        } != null
    }

}

class ClassGeneratorInfo(val name: String?, val generator: DatatableClassGenerator)