package org.example.desktopapp

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class AuthServiceTest {

    @Test
    fun `login returns successful response`() = runTest {
        val authService = AuthService()
        val result = authService.login("adarsh", "1234")

        assertTrue(result.contains("Successful"))
    }

    @Test
    fun `login with wrong password returns error`() = runTest {
        val authService = AuthService()
        val result = authService.login("adarsh", "wrongpassword")

        assertTrue(
            result.contains("Invalid") || result.contains("not found")
        )
    }
}