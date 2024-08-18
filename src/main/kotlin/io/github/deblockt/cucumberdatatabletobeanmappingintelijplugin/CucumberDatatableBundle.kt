package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.CucumberDatatable"

internal object CucumberDatatableBundle {
    private val INSTANCE = DynamicBundle(CucumberDatatableBundle::class.java, BUNDLE)

    fun message(
        key: @PropertyKey(resourceBundle = BUNDLE) String,
        vararg params: Any
    ): String {
        return INSTANCE.getMessage(key, *params)
    }
}