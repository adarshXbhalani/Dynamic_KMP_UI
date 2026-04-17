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

@Composable
fun RecordsScreen(onBack: () -> Unit) {

    val scope = rememberCoroutineScope()
    val authService = remember { AuthService() }

    // add form state
    var newCategory    by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }
    var newAmount      by remember { mutableStateOf("") }
    var newDate        by remember { mutableStateOf("") }
    var addError       by remember { mutableStateOf("") }
    var isAdding       by remember { mutableStateOf(false) }

    // filter state
    var selectedDate     by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var expanded         by remember { mutableStateOf(false) }
    var categories       by remember { mutableStateOf<List<String>>(emptyList()) }

    // table state
    var records   by remember { mutableStateOf<List<RecordResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg  by remember { mutableStateOf("") }

    fun loadAll() {
        scope.launch {
            isLoading = true
            errorMsg = ""
            try {
                records = authService.getRecords("All", "All")
            } catch (e: Exception) {
                errorMsg = "Failed to load: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val cats = authService.getCategories()
            categories = listOf("All") + cats
        } catch (e: Exception) { }
        loadAll()
    }

    fun search() {
        scope.launch {
            isLoading = true
            errorMsg = ""
            try {
                records = authService.getRecords(
                    if (selectedDate.isBlank()) "All" else selectedDate,
                    selectedCategory
                )
            } catch (e: Exception) {
                errorMsg = "Failed: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun addRecord() {
        if (newCategory.isBlank() || newDescription.isBlank()
            || newAmount.isBlank() || newDate.isBlank()) {
            addError = "All fields required"
            return
        }
        scope.launch {
            isAdding = true
            addError = ""
            try {
                authService.addRecord(
                    category    = newCategory,
                    description = newDescription,
                    amount      = newAmount.toDouble(),
                    date        = newDate
                )
                newCategory    = ""
                newDescription = ""
                newAmount      = ""
                newDate        = ""
                loadAll()
            } catch (e: Exception) {
                addError = "Failed: ${e.message}"
            } finally {
                isAdding = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp)
    ) {

        // TOP BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Records", style = MaterialTheme.typography.headlineMedium)
            OutlinedButton(onClick = onBack) { Text("Back") }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        // ADD FORM
        Text("Add Record", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newCategory,
                onValueChange = { newCategory = it },
                label = { Text("Category") },
                singleLine = true,
                modifier = Modifier.width(140.dp)
            )
            OutlinedTextField(
                value = newDescription,
                onValueChange = { newDescription = it },
                label = { Text("Description") },
                singleLine = true,
                modifier = Modifier.width(180.dp)
            )
            OutlinedTextField(
                value = newAmount,
                onValueChange = { newAmount = it },
                label = { Text("Amount") },
                singleLine = true,
                modifier = Modifier.width(110.dp)
            )
            OutlinedTextField(
                value = newDate,
                onValueChange = { newDate = it },
                label = { Text("Date YYYY-MM-DD") },
                singleLine = true,
                modifier = Modifier.width(170.dp)
            )
            Button(
                onClick = { addRecord() },
                enabled = !isAdding
            ) {
                if (isAdding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Add")
                }
            }
        }

        if (addError.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(addError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        // FILTER ROW
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = selectedDate,
                onValueChange = { selectedDate = it },
                label = { Text("Date (Search)") },
                singleLine = true,
                modifier = Modifier.width(200.dp)
            )
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(selectedCategory)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = { selectedCategory = cat; expanded = false }
                        )
                    }
                }
            }
            Button(onClick = { search() }) { Text("Search") }
            OutlinedButton(onClick = { loadAll() }) { Text("Reset") }
        }

        Spacer(Modifier.height(12.dp))

        // TABLE HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text("ID",          color = Color.White, modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Category",    color = Color.White, modifier = Modifier.weight(1f),   fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Description", color = Color.White, modifier = Modifier.weight(2f),   fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Amount",      color = Color.White, modifier = Modifier.weight(1f),   fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Date",        color = Color.White, modifier = Modifier.weight(1f),   fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        // TABLE BODY
        when {
            isLoading -> {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMsg.isNotEmpty() -> {
                Text(errorMsg, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            records.isEmpty() -> {
                Text("No records found.", modifier = Modifier.padding(16.dp), color = Color.Gray, fontSize = 13.sp)
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    itemsIndexed(records) { index, record ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (index % 2 == 0) Color(0xFFF5F5F5) else Color.White)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(record.id.toString(),     modifier = Modifier.weight(0.5f), fontSize = 13.sp)
                            Text(record.category,          modifier = Modifier.weight(1f),   fontSize = 13.sp)
                            Text(record.description,       modifier = Modifier.weight(2f),   fontSize = 13.sp)
                            Text(record.amount.toString(), modifier = Modifier.weight(1f),   fontSize = 13.sp)
                            Text(record.recordDate,        modifier = Modifier.weight(1f),   fontSize = 13.sp)
                        }
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                    }
                }
            }
        }
    }
}