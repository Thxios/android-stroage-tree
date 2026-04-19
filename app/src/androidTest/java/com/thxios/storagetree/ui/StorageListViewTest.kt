package com.thxios.storagetree.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.ui.explorer.listview.StorageListView
import com.thxios.storagetree.ui.theme.StorageTreeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StorageListViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val nodes = listOf(
        FileNode(name = "Downloads", path = "/Downloads", sizeBytes = 500_000_000L, isDirectory = true),
        FileNode(name = "DCIM", path = "/DCIM", sizeBytes = 300_000_000L, isDirectory = true),
        FileNode(name = "note.txt", path = "/note.txt", sizeBytes = 1024L, isDirectory = false)
    )

    @Test
    fun storageListView_rendersRowsForGivenNodes() {
        composeTestRule.setContent {
            StorageTreeTheme {
                StorageListView(nodes = nodes, onNodeClick = {})
            }
        }

        composeTestRule.onNodeWithText("Downloads").assertIsDisplayed()
        composeTestRule.onNodeWithText("DCIM").assertIsDisplayed()
        composeTestRule.onNodeWithText("note.txt").assertIsDisplayed()
    }

    @Test
    fun storageListView_clickingRowCallsOnNodeClick() {
        var clickedNode: FileNode? = null

        composeTestRule.setContent {
            StorageTreeTheme {
                StorageListView(nodes = nodes, onNodeClick = { clickedNode = it })
            }
        }

        composeTestRule.onNodeWithText("Downloads").performClick()
        assertEquals(nodes[0], clickedNode)
    }
}
