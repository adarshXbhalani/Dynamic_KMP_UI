package org.example.desktopapp

import kotlinx.serialization.json.Json

object TableConfigLoader {

    private val json = Json { ignoreUnknownKeys = true }

    fun load(fileName: String): TableConfig {
        // reads from resources folder
        val text = object {}.javaClass
            .getResourceAsStream("/$fileName")
            ?.bufferedReader()
            ?.readText()
            ?: throw Exception("Config file $fileName not found")

        return json.decodeFromString(TableConfig.serializer(), text)
    }
}