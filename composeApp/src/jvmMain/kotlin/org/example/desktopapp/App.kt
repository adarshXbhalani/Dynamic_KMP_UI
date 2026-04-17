package org.example.desktopapp


import androidx.compose.runtime.*
import androidx.compose.runtime.Composable


enum class Screen{
    LOGIN,
    DASHBOARD,
    RECORDS,
    USERS,
    TESTRECORDS,
    TESTRESULTS
}

@Composable
fun App() {
    var currentScreen by remember { mutableStateOf(Screen.LOGIN)}
    var loggedInUsername by remember {mutableStateOf("")}

    when(currentScreen){
        Screen.LOGIN -> LoginScreen(
            onLoginSuccess = {
                username -> loggedInUsername = username
                currentScreen = Screen.DASHBOARD
            }
        )
        Screen.DASHBOARD -> DashboardScreen(
            username = loggedInUsername,
            onLogout = { currentScreen = Screen.LOGIN },
            onViewRecords = { currentScreen = Screen.RECORDS },
            onViewUsers = { currentScreen = Screen.USERS},
            onViewTestRecords = { currentScreen = Screen.TESTRECORDS },
            onViewTestResults = { currentScreen = Screen.TESTRESULTS}
        )
//        Screen.RECORDS -> RecordsScreen(
//            onBack = { currentScreen = Screen.DASHBOARD }
//        )

        // to show records table
        Screen.RECORDS -> DynamicTableScreen(
            configFile = "table_config.json",
            onBack = { currentScreen = Screen.DASHBOARD }
        )

// to show users table — just different JSON
        Screen.USERS -> DynamicTableScreen(
            configFile = "users_config.json",
            onBack = { currentScreen = Screen.DASHBOARD }
        )
        Screen.TESTRECORDS -> DynamicTableScreen(
            configFile = "testrecord_config.json",
            onBack = { currentScreen = Screen.DASHBOARD }
        )
        Screen.TESTRESULTS -> DynamicTableScreen(
        configFile = "testresult.json",
        onBack = { currentScreen = Screen.DASHBOARD }
        )
    }
}