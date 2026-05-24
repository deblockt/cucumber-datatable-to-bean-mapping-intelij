package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiRecordComponent
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UseScopeEnlarger
import org.jetbrains.plugins.cucumber.psi.GherkinFileType

class WithCucumberFileScopesEnlarger: UseScopeEnlarger() {
    private val log = com.intellij.openapi.diagnostic.Logger.getInstance(WithCucumberFileScopesEnlarger::class.java)

    override fun getAdditionalUseScope(myElement: PsiElement): SearchScope? {
        if (myElement !is PsiField && myElement !is PsiRecordComponent) {
            return null
        }

        return ReadAction.nonBlocking<SearchScope?> {
            val psiClass = (myElement as PsiMember).containingClass
            if (!hasDataTableWithHeaderAnnotation(psiClass)) return@nonBlocking null

            val module = ProjectRootManager.getInstance(myElement.project)
                .fileIndex.getModuleForFile(myElement.containingFile.originalFile.virtualFile)
            val scope = module?.moduleWithDependentsScope ?: return@nonBlocking null

            GlobalSearchScope.getScopeRestrictedByFileTypes(scope, GherkinFileType.INSTANCE)
        }.executeSynchronously()
    }
}