package org.example.desktopapp

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class LoginScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun emptyFieldsShowsError() {
        rule.setContent {
            LoginScreen(onLoginSuccess = {})
        }
        rule.onNodeWithText("Login").performClick()
        rule.onNodeWithText("Fields cannot be empty").assertIsDisplayed()
    }

    @Test
    fun typingUsernameWorks() {
        rule.setContent {
            LoginScreen(onLoginSuccess = {})
        }
        rule.onNodeWithText("Username").performTextInput("adarsh")
        rule.onNodeWithText("adarsh").assertIsDisplayed()
    }
}