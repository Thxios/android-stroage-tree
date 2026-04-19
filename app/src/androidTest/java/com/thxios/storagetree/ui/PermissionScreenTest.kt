package com.thxios.storagetree.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thxios.storagetree.ui.permission.PermissionScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenPermissionNotGranted_showsGrantButton() {
        composeTestRule.setContent {
            PermissionScreen(
                hasPermission = false,
                onRequestPermission = {},
                onNavigateToExplorer = {}
            )
        }
        composeTestRule.onNodeWithText("권한 허용").assertIsDisplayed()
    }

    @Test
    fun whenButtonClicked_doesNotNavigateWithoutPermission() {
        var navigateCalled = false
        composeTestRule.setContent {
            PermissionScreen(
                hasPermission = false,
                onRequestPermission = {},
                onNavigateToExplorer = { navigateCalled = true }
            )
        }
        composeTestRule.onNodeWithText("권한 허용").performClick()
        assert(!navigateCalled)
    }

    @Test
    fun whenPermissionGranted_callsNavigateCallback() {
        var navigateCalled = false
        composeTestRule.setContent {
            PermissionScreen(
                hasPermission = true,
                onRequestPermission = {},
                onNavigateToExplorer = { navigateCalled = true }
            )
        }
        assert(navigateCalled)
    }
}
