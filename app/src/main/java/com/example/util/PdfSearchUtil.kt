package com.scholarvault.util

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.IOException

data class SearchHighlight(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

class WordLocator(private val query: String) : PDFTextStripper() {
    val highlights = mutableListOf<List<TextPosition>>()

    @Throws(IOException::class)
    override fun writeString(string: String?, textPositions: MutableList<TextPosition>?) {
        if (string == null || textPositions == null) return
        val termLower = query.lowercase().trim()
        if (termLower.isEmpty()) return
        val strLower = string.lowercase()
        
        var index = strLower.indexOf(termLower)
        while (index != -1) {
            val length = termLower.length
            if (index + length <= textPositions.size) {
                val subList = textPositions.subList(index, index + length).toList()
                highlights.add(subList)
            }
            index = strLower.indexOf(termLower, index + 1)
        }
    }
}

object PdfSearchUtil {

    private val textCache = java.util.concurrent.ConcurrentHashMap<String, List<String>>()

    private fun getFileIdentifier(file: File?, uri: android.net.Uri?): String {
        return file?.absolutePath ?: uri?.toString() ?: "unknown"
    }

    private suspend fun getDocText(context: android.content.Context, file: File?, uri: android.net.Uri?): List<String> {
        val id = getFileIdentifier(file, uri)
        if (textCache.containsKey(id)) {
            return textCache[id]!!
        }

        val doc = when {
            file != null -> PDDocument.load(file)
            uri != null -> context.contentResolver.openInputStream(uri)?.use { PDDocument.load(it) }
            else -> null
        } ?: return emptyList()

        val textList = mutableListOf<String>()
        val stripper = PDFTextStripper()
        try {
            for (pageNum in 1..doc.numberOfPages) {
                kotlinx.coroutines.yield()
                stripper.startPage = pageNum
                stripper.endPage = pageNum
                textList.add(stripper.getText(doc) ?: "")
            }
            textCache[id] = textList
        } finally {
            doc.close()
        }
        return textList
    }

    private fun <T> runWithDocument(context: android.content.Context, file: File?, uri: android.net.Uri?, block: (PDDocument) -> T): T? {
        return try {
            val doc = when {
                file != null -> PDDocument.load(file)
                uri != null -> context.contentResolver.openInputStream(uri)?.use { PDDocument.load(it) }
                else -> null
            }
            doc?.use { block(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun searchPdfFlow(context: android.content.Context, file: File?, uri: android.net.Uri?, query: String): kotlinx.coroutines.flow.Flow<List<Int>> = kotlinx.coroutines.flow.flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }
        
        val textList = getDocText(context, file, uri)
        val results = mutableListOf<Int>()
        for (i in textList.indices) {
            if (textList[i].contains(query, ignoreCase = true)) {
                results.add(i)
                emit(results.toList())
            }
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)

    fun searchPdf(context: android.content.Context, file: File?, uri: android.net.Uri?, query: String): List<Int> {
        if (query.isBlank()) return emptyList()
        val textList = kotlinx.coroutines.runBlocking { getDocText(context, file, uri) }
        val results = mutableListOf<Int>()
        for (i in textList.indices) {
            if (textList[i].contains(query, ignoreCase = true)) {
                results.add(i)
            }
        }
        return results
    }

    fun getPageSearchSnippets(context: android.content.Context, file: File?, uri: android.net.Uri?, query: String, pageIndex: Int): List<String> {
        if (query.isBlank()) return emptyList()
        val textList = kotlinx.coroutines.runBlocking { getDocText(context, file, uri) }
        if (pageIndex >= textList.size) return emptyList()
        val snippets = mutableListOf<String>()
        val lines = textList[pageIndex].split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.contains(query, ignoreCase = true)) {
                snippets.add(trimmed)
            }
        }
        return snippets
    }

    fun getSearchHighlights(context: android.content.Context, file: File?, uri: android.net.Uri?, query: String, pageIndex: Int): List<SearchHighlight> {
        if (query.isBlank()) return emptyList()
        return runWithDocument(context, file, uri) { doc ->
            val list = mutableListOf<SearchHighlight>()
            if (pageIndex < doc.numberOfPages) {
                val page = doc.getPage(pageIndex)
                val cropBox = page.cropBox
                val pageWidth = cropBox.width
                val pageHeight = cropBox.height
                
                val locator = WordLocator(query)
                locator.startPage = pageIndex + 1
                locator.endPage = pageIndex + 1
                
                val writer = java.io.StringWriter()
                locator.writeText(doc, writer)
                
                for (match in locator.highlights) {
                    if (match.isEmpty()) continue
                    val minX = match.minOf { it.xDirAdj }
                    val maxX = match.maxOf { it.xDirAdj + it.widthDirAdj }
                    val minY = match.minOf { it.yDirAdj - it.heightDir }
                    val maxY = match.maxOf { it.yDirAdj }
                    
                    val paddingX = 1f
                    val paddingY = 1f
                    
                    val leftFrac = ((minX - paddingX) / pageWidth).coerceIn(0f, 1f)
                    val rightFrac = ((maxX + paddingX) / pageWidth).coerceIn(0f, 1f)
                    val topFrac = ((minY - paddingY) / pageHeight).coerceIn(0f, 1f)
                    val bottomFrac = ((maxY + paddingY) / pageHeight).coerceIn(0f, 1f)
                    
                    list.add(SearchHighlight(leftFrac, topFrac, rightFrac, bottomFrac))
                }
            }
            list
        } ?: emptyList()
    }
}
