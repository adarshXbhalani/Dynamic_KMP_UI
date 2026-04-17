package org.example.desktopapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun DynamicTableScreen(
    configFile: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val authService = remember { AuthService() }

    val config = remember { TableConfigLoader.load(configFile) }
    val visibleColumns = config.columns.filter { it.visible }

    // table state
    var rows          by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var isLoading     by remember { mutableStateOf(false) }
    var errorMsg      by remember { mutableStateOf("") }
    var successMsg    by remember { mutableStateOf("") }

    // specialized search state
    val fkSearchStates = remember { mutableStateMapOf<String, String>() }

    // input field states
    var searchText    by remember { mutableStateOf("") }
    var fromDate      by remember { mutableStateOf("") }
    var toDate        by remember { mutableStateOf("") }
    val columnSearch  = remember {
        mutableStateMapOf<String, String>().apply {
            config.searchColumns.filter { it.enabled }.forEach { put(it.key, "") }
        }
    }

    // active values (used for local filtering logic)
    var activeSearchText by remember { mutableStateOf("") }
    var activeFromDate   by remember { mutableStateOf("") }
    var activeToDate     by remember { mutableStateOf("") }
    val activeColumnSearch = remember { mutableStateMapOf<String, String>() }

    // dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRow    by remember { mutableStateOf<Map<String, String>?>(null) }

    var isDutSearchActive by remember { mutableStateOf(false) }
    var dutFilteredRows   by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    // ── FILTER FUNCTION ──
    fun applyFilters(source: List<Map<String, String>>): List<Map<String, String>> {
        return source.filter { row ->
            val matchGlobal = activeSearchText.isBlank() ||
                    row.values.any { it.contains(activeSearchText, ignoreCase = true) }

            val matchColumns = activeColumnSearch.all { (key, value) ->
                value.isBlank() || (row[key] ?: "").contains(value, ignoreCase = true)
            }

            val matchDate = if (!config.dateFilterable) true else {
                val rowDateStr = row[config.dateColumn] ?: ""
                val rowDate = try { LocalDate.parse(rowDateStr) } catch (e: Exception) { null }
                val from = if (activeFromDate.isBlank()) null
                else try { LocalDate.parse(activeFromDate) } catch (e: Exception) { null }
                val to = if (activeToDate.isBlank()) LocalDate.now()
                else try { LocalDate.parse(activeToDate) } catch (e: Exception) { LocalDate.now() }

                if (rowDate == null) true
                else {
                    val afterFrom = if (from == null) true else !rowDate.isBefore(from)
                    !rowDate.isAfter(to) && afterFrom
                }
            }
            matchGlobal && matchColumns && matchDate
        }
    }

    val filteredRows = remember(rows, activeSearchText, activeColumnSearch.toMap(), activeFromDate, activeToDate) {
        applyFilters(rows)
    }

    fun loadAll() {
        scope.launch {
            isLoading = true
            errorMsg = ""
            try {
                if (isDutSearchActive) {
                    // filter within DUT results locally — no backend call
                    rows = applyFilters(dutFilteredRows)  // restore DUT results
                    // local column filters will apply via applyFilters()
                } else {
                    val params = mutableMapOf<String, String>()
                    if (activeFromDate.isNotBlank()) params["fromDate"] = activeFromDate
                    if (activeToDate.isNotBlank())   params["toDate"]   = activeToDate
                    activeColumnSearch.forEach { (key, value) ->
                        if (value.isNotBlank()) params[key] = value
                    }
                    rows = if (params.isEmpty()) {
                        authService.getAllRows(config.apiBase)
                    } else {
                        authService.getRowsWithFilter(config.apiBase, params)
                    }
                }
            } catch (e: Exception) {
                errorMsg = "Failed: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteRow(id: String) {
        scope.launch {
            try {
                authService.deleteRow(config.apiBase, id)
                successMsg = "Deleted successfully"

                // 🔥 FIX: Update the local list instead of calling loadAll()
                rows = rows.filter { it["id"] != id }

            } catch (e: Exception) {
                errorMsg = "Delete failed: ${e.message}"
            }
        }
    }

    // ── DIALOGS ──
    if (showAddDialog) {
        DynamicAddDialog(
            columns = config.columns.filter { it.addable },
            onDismiss = { showAddDialog = false },
            onSave = { formData ->
                scope.launch {
                    try {
                        val response = authService.addRow(config.apiBase, formData)
                        // Assuming the backend returns the saved object with its new ID
                        // If not, you may need a partial load or the backend to return the object
                        rows = rows + formData
                        showAddDialog = false
                        successMsg = "Record added successfully"
                    } catch (e: Exception) {
                        errorMsg = "Add failed: ${e.message}"
                    }
                }
            }
        )
    }

    editingRow?.let { row ->
        DynamicEditDialog(
            row = row,
            columns = config.columns.filter { it.editable },
            onDismiss = { editingRow = null },
            onSave = { formData ->
                scope.launch {
                    try {
                        authService.updateRow(config.apiBase, formData)
                        editingRow = null
                        successMsg = "Updated successfully"
                        // replace row — formData now has ALL fields
                        rows = rows.map { r ->
                            if (r["id"] == formData["id"]) formData else r
                        }
                    } catch (e: Exception) {
                        errorMsg = "Update failed: ${e.message}"
                    }
                }
            }
        )
    }

    // ── MAIN UI ──
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {

        // TOP BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(config.displayName, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Showing ${filteredRows.size} of ${rows.size} records",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            OutlinedButton(onClick = onBack) { Text("Back") }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        // ── SEARCH & FILTER AREA ──
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // 1. Foreign Key Search Row (e.g., Search by DUT)
            config.foreignKeySearch.filter { it.enabled }.forEach { fk ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = fkSearchStates[fk.foreignKey] ?: "",
                        onValueChange = { fkSearchStates[fk.foreignKey] = it },
                        label = { Text(fk.label) },
                        placeholder = { Text(fk.placeholder) },
                        singleLine = true,
                        modifier = Modifier.width(250.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    // FK search button
                    Button(onClick = {
                        val value = fkSearchStates[fk.foreignKey] ?: ""
                        if (value.isBlank()) {
                            errorMsg = "Please enter a value"
                            return@Button
                        }
                        scope.launch {
                            isLoading = true
                            errorMsg = ""
                            try {
                                val result = authService.getRowsByUrl("${fk.apiEndpoint}?value=$value")
                                rows = result
                                dutFilteredRows = result   // ← save DUT results separately
                                isDutSearchActive = true   // ← mark DUT mode active
                            } catch (e: Exception) {
                                errorMsg = "Search failed: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    }) { Text("Search ${fk.label}", fontSize = 12.sp) }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = {
                        fkSearchStates[fk.foreignKey] = ""
                        rows = emptyList()
                        dutFilteredRows = emptyList()
                        isDutSearchActive = false
                        errorMsg = ""
                        successMsg = ""
                    }) {
                        Text("Clear", fontSize = 12.sp)
                    }
                }
            }

            // 2. Global & Column Filters Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (config.searchable) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text("Search All") },
                        singleLine = true,
                        modifier = Modifier.width(160.dp)
                    )
                }

                config.searchColumns.filter { it.enabled }.forEach { searchCol ->
                    OutlinedTextField(
                        value = columnSearch[searchCol.key] ?: "",
                        onValueChange = { columnSearch[searchCol.key] = it },
                        label = { Text(searchCol.label) },
                        singleLine = true,
                        modifier = Modifier.width(140.dp)
                    )
                }

                if (config.dateFilterable) {
                    OutlinedTextField(
                        value = fromDate,
                        onValueChange = { fromDate = it },
                        label = { Text("From Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.width(150.dp)
                    )
                    OutlinedTextField(
                        value = toDate,
                        onValueChange = { toDate = it },
                        label = { Text("To Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.width(150.dp)
                    )
                }

                OutlinedButton(onClick = {
                    searchText = ""; activeSearchText = ""
                    fromDate = "";   activeFromDate   = ""
                    toDate = "";     activeToDate     = ""
                    columnSearch.keys.forEach { columnSearch[it] = "" }
                    activeColumnSearch.clear()
                    fkSearchStates.keys.forEach { fkSearchStates[it] = "" }
                    rows = emptyList()
                    dutFilteredRows = emptyList()
                    isDutSearchActive = false   // ← reset DUT mode
                    errorMsg = ""
                    successMsg = ""
                }) { Text("Reset", fontSize = 12.sp) }

                Button(onClick = {
                    activeSearchText = searchText
                    activeFromDate   = fromDate
                    activeToDate     = toDate
                    columnSearch.forEach { (k, v) -> activeColumnSearch[k] = v }
                    loadAll()
                }) { Text("Apply Filters") }

                if (config.canAdd) {
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { showAddDialog = true }) { Text("+ Add") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Feedback Messages
        if (successMsg.isNotEmpty()) {
            Text(successMsg, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        }
        if (errorMsg.isNotEmpty()) {
            Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(8.dp))

        // ── TABLE HEADER ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            visibleColumns.forEach { col ->
                Text(
                    col.label,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            if (config.canEdit || config.canDelete) {
                Text(
                    "Actions",
                    color = Color.White,
                    modifier = Modifier.width(160.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // ── TABLE BODY ──
        when {
            isLoading -> {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            filteredRows.isEmpty() -> {
                Text("No records found.", modifier = Modifier.padding(16.dp), color = Color.Gray)
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    itemsIndexed(filteredRows) { index, row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (index % 2 == 0) Color(0xFFF5F5F5) else Color.White)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            visibleColumns.forEach { col ->
                                Text(row[col.key] ?: "", modifier = Modifier.weight(1f), fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.width(160.dp)) {
                                if (config.canEdit) {
                                    TextButton(onClick = {
                                        successMsg = ""; errorMsg = ""; editingRow = row
                                    }) { Text("Edit", fontSize = 12.sp) }
                                }
                                if (config.canDelete) {
                                    TextButton(onClick = {
                                        successMsg = ""; errorMsg = ""; deleteRow(row["id"] ?: "")
                                    }) { Text("Delete", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicAddDialog(
    columns: List<ColumnConfig>,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    val formData = remember { mutableStateMapOf<String, String>().apply { columns.forEach { put(it.key, "") } } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Row") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                columns.forEach { col ->
                    OutlinedTextField(
                        value = formData[col.key] ?: "",
                        onValueChange = { formData[col.key] = it },
                        label = { Text(col.label) },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(formData.toMap()) }) { Text("Save") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DynamicEditDialog(
    row: Map<String, String>,
    columns: List<ColumnConfig>,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    val formData = remember { mutableStateMapOf<String, String>().apply { columns.forEach { put(it.key, row[it.key] ?: "") } } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Record") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                columns.forEach { col ->
                    OutlinedTextField(
                        value = formData[col.key] ?: "",
                        onValueChange = { formData[col.key] = it },
                        label = { Text(col.label) },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                // start with ALL original row data
                val updated = row.toMutableMap()
                // only overwrite the editable fields
                formData.forEach { (key, value) ->
                    updated[key] = value
                }
                updated["id"] = row["id"] ?: ""
                onSave(updated)
            }) { Text("Save") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}