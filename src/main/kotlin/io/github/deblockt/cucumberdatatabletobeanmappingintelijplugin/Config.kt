package io.github.deblockt.cucumberdatatabletobeanmappingintelijplugin

import com.deblock.cucumber.datatable.backend.options.DefaultOptions
import com.deblock.cucumber.datatable.backend.options.FullOptions
import com.deblock.cucumber.datatable.backend.options.MergedOptions
import com.deblock.cucumber.datatable.backend.options.PropertiesOptions
import com.deblock.cucumber.datatable.mapper.name.ColumnNameBuilder
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.FilenameIndex
import java.io.ByteArrayInputStream
import java.util.*
import kotlin.collections.HashMap

fun options(): FullOptions {
    val files = FilenameIndex.getVirtualFilesByName("cucumber.properties", EverythingGlobalScope())
    if (files.isNotEmpty()) {
        val props = Properties()
        props.load(ByteArrayInputStream(files.first().contentsToByteArray()))
        val map = HashMap<String, String>()
        props.forEach { (key, value) ->
            map[key as String] = value as String
        }
        return MergedOptions(
                PropertiesOptions(map),
                DefaultOptions()
        )
    }
    return DefaultOptions()
}

val cache = HashMap<String, Any>()
fun <T> cached(key: String, callback: () -> T): T {
    if (key in cache) {
        return cache[key] as T
    }
    val response = callback()
    cache[key] = response as Any
    return response as T
}

fun nameResolver(options: FullOptions): ColumnNameBuilder {
    return cached(options.nameBuilderClass.toString()) {
        try {
            options.nameBuilderClass.getDeclaredConstructor().newInstance()
        } catch (e: Exception) {
            nameResolver(DefaultOptions())
        }
    }
}