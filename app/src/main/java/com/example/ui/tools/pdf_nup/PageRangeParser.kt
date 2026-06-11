package com.scholarvault.ui.tools.pdf_nup

class PageRangeParser(private val selectionText: String, private val totalPages: Int) {

    fun parse(): List<Int> {
        if (totalPages <= 0) return emptyList()
        val pages = mutableSetOf<Int>()
        
        if (selectionText.isBlank()) {
            return (0 until totalPages).toList()
        }

        try {
            val parts = selectionText.split(",")
            for (part in parts) {
                val p = part.trim()
                if (p.isEmpty()) continue
                
                if (p.contains("-")) {
                    val bounds = p.split("-")
                    if (bounds.size == 2) {
                        val startStr = bounds[0].trim()
                        val endStr = bounds[1].trim()
                        
                        if (startStr.isEmpty() || endStr.isEmpty()) {
                            continue
                        }
                        
                        val start = startStr.toIntOrNull() ?: continue
                        val end = endStr.toIntOrNull() ?: continue
                        
                        val safeStart = start.coerceIn(1, totalPages)
                        val safeEnd = end.coerceIn(1, totalPages)
                        
                        val actualStart = minOf(safeStart, safeEnd)
                        val actualEnd = maxOf(safeStart, safeEnd)
                        
                        for (i in actualStart..actualEnd) {
                            pages.add(i - 1)
                        }
                    } else {
                        continue
                    }
                } else {
                    val num = p.toIntOrNull() ?: continue
                    if (num in 1..totalPages) {
                        pages.add(num - 1)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return pages.toList()
    }

    fun parseInverted(): List<Int> {
        val selected = parse().toSet()
        val allPages = (0 until totalPages).toList()
        return allPages.filter { it !in selected }
    }
}
