package com.thxios.storagetree.ui

import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.ui.explorer.treemap.SquarifyAlgorithm
import com.thxios.storagetree.ui.explorer.treemap.TreemapRect
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class SquarifyAlgorithmTest {
    private val algorithm = SquarifyAlgorithm()

    private fun makeNode(name: String, size: Long) = FileNode(
        name = name, path = "/$name", sizeBytes = size, isDirectory = false
    )

    @Test
    fun `single item fills entire area`() {
        val nodes = listOf(makeNode("a", 100))
        val result = algorithm.compute(nodes, 200f, 100f)
        assertEquals(1, result.size)
        val rect = result[0]
        assertEquals(0f, rect.left, 0.01f)
        assertEquals(0f, rect.top, 0.01f)
        assertEquals(200f, rect.right, 0.01f)
        assertEquals(100f, rect.bottom, 0.01f)
    }

    @Test
    fun `two equal items each get half area`() {
        val nodes = listOf(makeNode("a", 100), makeNode("b", 100))
        val result = algorithm.compute(nodes, 200f, 100f)
        assertEquals(2, result.size)
        val totalArea = result.sumOf { (it.right - it.left) * (it.bottom - it.top).toDouble() }
        assertEquals(200.0 * 100.0, totalArea, 1.0)
    }

    @Test
    fun `total rect area equals canvas area`() {
        val nodes = listOf(
            makeNode("a", 300), makeNode("b", 200), makeNode("c", 100)
        )
        val result = algorithm.compute(nodes, 300f, 200f)
        val totalArea = result.sumOf { (it.right - it.left) * (it.bottom - it.top).toDouble() }
        assertEquals(300.0 * 200.0, totalArea, 1.0)
    }

    @Test
    fun `no rect exceeds canvas bounds`() {
        val nodes = (1..5).map { makeNode("node$it", it.toLong() * 100) }
        val result = algorithm.compute(nodes, 400f, 300f)
        for (rect in result) {
            assertTrue("left >= 0", rect.left >= -0.01f)
            assertTrue("top >= 0", rect.top >= -0.01f)
            assertTrue("right <= width", rect.right <= 400.01f)
            assertTrue("bottom <= height", rect.bottom <= 300.01f)
        }
    }

    @Test
    fun `rects do not overlap`() {
        val nodes = (1..10).map { makeNode("node$it", it.toLong() * 50) }
        val result = algorithm.compute(nodes, 400f, 300f)
        for (i in result.indices) {
            for (j in i + 1 until result.size) {
                val a = result[i]
                val b = result[j]
                val overlapX = a.left < b.right - 0.01f && a.right > b.left + 0.01f
                val overlapY = a.top < b.bottom - 0.01f && a.bottom > b.top + 0.01f
                assertFalse("rects $i and $j overlap", overlapX && overlapY)
            }
        }
    }

    @Test
    fun `empty list returns empty result`() {
        val result = algorithm.compute(emptyList(), 400f, 300f)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `zero size item produces zero area rect without crash`() {
        val nodes = listOf(makeNode("a", 100), makeNode("zero", 0))
        val result = algorithm.compute(nodes, 200f, 100f)
        assertEquals(2, result.size)
        val zeroRect = result.find { it.node.name == "zero" }!!
        val area = (zeroRect.right - zeroRect.left) * (zeroRect.bottom - zeroRect.top)
        assertEquals(0f, area, 0.01f)
    }
}
