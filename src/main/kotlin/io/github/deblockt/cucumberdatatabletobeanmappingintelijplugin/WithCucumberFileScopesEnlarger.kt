package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UseScopeEnlarger
import org.jetbrains.plugins.cucumber.psi.GherkinFileType

class WithCucumberFileScopesEnlarger: UseScopeEnlarger() {
    private val log = com.intellij.openapi.diagnostic.Logger.getInstance(WithCucumberFileScopesEnlarger::class.java)

    override fun getAdditionalUseScope(myElement: PsiElement): SearchScope? {
        if (myElement !is PsiField && myElement !is PsiRecordComponent) {
            return null;
        }

        val psiClass: PsiClass? = ReadAction.compute<PsiClass?, RuntimeException> { (myElement as PsiMember).containingClass }
        val isDatatableClass = ReadAction.compute<Boolean, RuntimeException> { hasDataTableWithHeaderAnnotation(psiClass) }
        if (!isDatatableClass) {
            return null
        }
        val module = ProjectRootManager.getInstance(myElement.project).fileIndex.getModuleForFile(myElement.containingFile.originalFile.virtualFile)
        val scope = module?.moduleWithDependentsScope ?: return null

        log.warn("Return new scope with gherkins file for field ${myElement.text}")
        return GlobalSearchScope.getScopeRestrictedByFileTypes(scope, GherkinFileType.INSTANCE)
    }
}