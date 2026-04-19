package com.thxios.storagetree.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thxios.storagetree.domain.model.ViewMode
import com.thxios.storagetree.ui.explorer.ExplorerUiState
import com.thxios.storagetree.ui.theme.StorageTreeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewToggleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun viewToggle_initialViewModeIsLIST() {
        val uiState = MutableStateFlow(ExplorerUiState())

        composeTestRule.setContent {
            StorageTreeTheme {
                val state by uiState.collectAsState()
                Text(
                    text = state.viewMode.name,
                    modifier = Modifier.testTag("viewModeLabel")
                )
            }
        }

        composeTestRule.onNodeWithText("LIST").assertIsDisplayed()
    }

    @Test
    fun viewToggle_clickingToggleButton_changesViewModeThenRestores() {
        val uiState = MutableStateFlow(ExplorerUiState())

        composeTestRule.setContent {
            StorageTreeTheme {
                val state by uiState.collectAsState()
                Button(
                    onClick = {
                        val next = if (state.viewMode == ViewMode.LIST) ViewMode.TREEMAP else ViewMode.LIST
                        uiState.value = state.copy(viewMode = next)
                    },
                    modifier = Modifier.testTag("toggleBtn")
                ) {
                    Text(state.viewMode.name)
                }
            }
        }

        composeTestRule.onNodeWithTag("toggleBtn").assertIsDisplayed()
        composeTestRule.onNodeWithText("LIST").assertIsDisplayed()

        composeTestRule.onNodeWithTag("toggleBtn").performClick()
        composeTestRule.onNodeWithText("TREEMAP").assertIsDisplayed()

        composeTestRule.onNodeWithTag("toggleBtn").performClick()
        composeTestRule.onNodeWithText("LIST").assertIsDisplayed()

        assertEquals(ViewMode.LIST, uiState.value.viewMode)
    }
}
