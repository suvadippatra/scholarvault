package com.scholarvault.ui.tools.pdf_nup

import org.junit.Assert.assertEquals
import org.junit.Test

class PageRangeParserTest {

    @Test
    fun testValidRangesWithSpacing() {
        val parser = PageRangeParser("1-5, 8, 10-12", 15)
        assertEquals(listOf(0, 1, 2, 3, 4, 7, 9, 10, 11), parser.parse())
    }

    @Test
    fun testSinglePages() {
        val parser = PageRangeParser("1, 3, 5", 10)
        assertEquals(listOf(0, 2, 4), parser.parse())
    }

    @Test
    fun testEdgeRanges() {
        val parser = PageRangeParser("1, 10", 10)
        assertEquals(listOf(0, 9), parser.parse())
    }

    @Test
    fun testBoundaryViolations() {
        // Clamp to file bounds: "1-100" on 50-page PDF -> [1..50]
        val parser = PageRangeParser("1-100", 50)
        val expected = (0 until 50).toList()
        assertEquals(expected, parser.parse())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testMalformedRejection() {
        val parser = PageRangeParser("abc", 10)
        parser.parse()
    }

    @Test
    fun testInversionLogic() {
        val parser = PageRangeParser("1-5", 10)
        assertEquals(listOf(5, 6, 7, 8, 9), parser.parseInverted())
    }

    @Test
    fun testAutoReverse() {
        val parser = PageRangeParser("10-5", 10)
        assertEquals(listOf(4, 5, 6, 7, 8, 9), parser.parse())
    }
    
    @Test
    fun testDeduplication() {
        val parser = PageRangeParser("1, 1, 5", 10)
        assertEquals(listOf(0, 4), parser.parse())
    }
}
