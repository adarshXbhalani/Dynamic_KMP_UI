package org.example.desktopapp

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.statement.bodyAsText
 
class AuthService {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun login(username: String, password: String): String {
        return client.post("http://localhost:8080/api/login") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(username, password))
        }.body()
    }

    suspend fun uploadFile(file: File): String {
        return client.post("http://localhost:8080/file_upload") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        // "file" must match the @RequestParam("file") name in Spring Boot
                        append("file", file.readBytes(), Headers.build {
                            append(HttpHeaders.ContentDisposition,
                                "filename=\"${file.name}\"")
                        })
                    }
                )
            )
        }.body()
    }

    suspend fun getRecords(date: String, category: String): List<RecordResponse> {
        return client.post("http://localhost:8080/api/records/filter") {
            contentType(ContentType.Application.Json)
            setBody(RecordFilterRequest(date, category))
        }.body()
    }

    suspend fun getCategories(): List<String> {
        return client.get("http://localhost:8080/api/records/categories").body()
    }

    suspend fun addRecord(
        category: String,
        description: String,
        amount: Double,
        date: String
    ): String {
        return client.post("http://localhost:8080/api/records/add") {
            contentType(ContentType.Application.Json)
            setBody(RecordAddRequest(category, description, amount, date))
        }.body()
    }

    // generic get all rows
    suspend fun getAllRows(apiBase: String): List<Map<String, String>> {
        return client.get("$apiBase/all").body()
    }

    // generic add row
    suspend fun addRow(apiBase: String, data: Map<String, String>): String {
        return client.post("$apiBase/add") {
            contentType(ContentType.Application.Json)
            setBody(data)
        }.body<String>()
    }

    // generic delete row
    suspend fun deleteRow(apiBase: String, id: String): String {
        return client.delete("$apiBase/delete/$id").body()
    }

    // generic search
    suspend fun searchRows(apiBase: String, query: String): List<Map<String, String>> {
        return client.get("$apiBase/search?q=$query").body()
    }
    suspend fun updateRow(apiBase: String, data: Map<String, String>): String {
        return client.put("$apiBase/update") {
            contentType(ContentType.Application.Json)
            setBody(data)
        }.body<String>()
    }
//    suspend fun getRowsWithFilter(
//        apiBase: String,
//        params: Map<String, String>
//    ): List<Map<String, String>> {
//        val query = params.entries
//            .filter { it.value.isNotBlank() }
//            .joinToString("&") { "${it.key}=${it.value}" }
//        return client.get("$apiBase/filter-load?$query").body()
//    }
    suspend fun getRowsWithFilter(
        apiBase: String,
        params: Map<String, String>
    ): List<Map<String, String>> {
        val query = params.entries
            .filter { it.value.isNotBlank() }
            .joinToString("&") { "${it.key}=${it.value}" }

        return try {
            client.get("$apiBase/filter-load?$query").body()
        } catch (e: Exception) {
            println("Error in getRowsWithFilter: ${e.message}")
            emptyList()
        }
    }
//    suspend fun getRowsByForeignKey(
//        apiEndpoint: String,
//        value: String
//    ): List<Map<String, String>> {
//        return client.get("$apiEndpoint?value=$value").body()
//    }
    suspend fun getRowsByForeignKey(
        apiEndpoint: String,      // e.g. "/api/testrecords/by-dut-with-results"
        value: String
    ): List<Map<String, String>> {
        return try {
            client.get("$apiEndpoint?value=$value").body()
        } catch (e: Exception) {
            println("Error in getRowsByForeignKey: ${e.message}")
            emptyList()
        }
    }
    suspend fun getRowsByUrl(url: String): List<Map<String, String>> {
        // Replace this with your actual Ktor or Retrofit implementation
        // Example using a hypothetical client:
        return client.get(url).body()
    }
}
@Serializable
data class RecordFilterRequest(
    val date: String,
    val category: String
)

@Serializable
data class RecordResponse(
    val id: Long,
    val category: String,
    val description: String,
    val amount: Double,
    val recordDate: String
)
@Serializable
data class AuthRequest(
    val username: String,
    val password: String
)

@Serializable
data class RecordAddRequest(
    val category: String,
    val description: String,
    val amount: Double,
    val date: String
)


@Serializable
data class ColumnConfig(
    val key: String,
    val label: String,
    val visible: Boolean,
    val addable: Boolean = true,
    val editable: Boolean,
    val type: String
)

@Serializable
data class SearchColumnConfig(
    val key: String,
    val label: String,
    val enabled: Boolean
)

@Serializable
data class TableConfig(
    val tableName: String,
    val displayName: String,
    val apiBase: String,
    val columns: List<ColumnConfig>,
    val searchable: Boolean,
    val searchColumns: List<SearchColumnConfig> = emptyList(), // ← NEW
    val dateFilterable: Boolean = false,                        // ← NEW
    val dateColumn: String = "recordDate",                      // ← NEW
    val canAdd: Boolean,
    val canDelete: Boolean,
    val canEdit: Boolean,
    val foreignKeySearch: List<ForeignKeySearchConfig> = emptyList()
)

@Serializable
data class ForeignKeySearchConfig(
    val enabled: Boolean,
    val label: String,
    val placeholder: String,
    val foreignKey: String,
    val apiEndpoint: String
)