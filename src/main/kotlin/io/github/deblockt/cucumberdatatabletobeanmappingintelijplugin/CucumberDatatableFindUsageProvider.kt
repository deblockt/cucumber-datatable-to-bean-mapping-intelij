package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.plugins.cucumber.psi.GherkinLexer
import org.jetbrains.plugins.cucumber.psi.PlainGherkinKeywordProvider

class CucumberDatatableFindUsageProvider: FindUsagesProvider {

    companion object {
        private val Log = Logger.getInstance(CucumberDatatableFindUsageProvider::class.java)
    }

    override fun getWordsScanner(): WordsScanner {
        Log.error("ON EST ICI getWordsScanner")

        return DefaultWordsScanner(
            GherkinLexer(PlainGherkinKeywordProvider()),
            TokenSet.EMPTY,
            TokenSet.EMPTY,
            TokenSet.EMPTY
        )
    }

    override fun canFindUsagesFor(p0: PsiElement): Boolean {
        Log.error("ON EST ICI canFindUsagesFor")
        return true
    }

    override fun getHelpId(p0: PsiElement): String? {
        Log.error("ON EST ICI getHelpId")
        return "helpId"
    }

    override fun getType(p0: PsiElement): String {
        Log.error("ON EST ICI getType")
        return "datatable.type"
    }

    override fun getDescriptiveName(p0: PsiElement): String {
        Log.error("ON EST ICI getDescriptiveName")
        return "test"
    }

    override fun getNodeText(p0: PsiElement, p1: Boolean): String {
        Log.error("ON EST ICI getNodeText")
        return "nodeText"
    }
}