package com.thxios.storagetree.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.ui.explorer.treemap.TreemapView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TreemapViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun makeNode(name: String, size: Long) = FileNode(
        name = name, path = "/$name", sizeBytes = size, isDirectory = false
    )

    @Test
    fun treemapView_withNonEmptyNodes_composesWithoutError() {
        val nodes = listOf(
            makeNode("alpha", 300),
            makeNode("beta", 200),
            makeNode("gamma", 100)
        )
        composeTestRule.setContent {
            TreemapView(nodes = nodes, onNodeClick = {})
        }
        composeTestRule.onNodeWithTag("treemap_canvas").assertExists()
    }

    @Test
    fun treemapView_clickOnCanvas_triggersOnNodeClick() {
        val nodes = listOf(
            makeNode("alpha", 500),
            makeNode("beta", 100)
        )
        var clickedNode: FileNode? = null
        composeTestRule.setContent {
            TreemapView(nodes = nodes, onNodeClick = { clickedNode = it })
        }
        composeTestRule.onNodeWithTag("treemap_canvas").performClick()
        assert(clickedNode != null)
    }
}
