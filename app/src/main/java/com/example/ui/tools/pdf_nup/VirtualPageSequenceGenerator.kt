package com.scholarvault.ui.tools.pdf_nup

class VirtualPageSequenceGenerator(
    private val items: List<NUpItem>,
    private val config: NUpConfig
) {
    private val processingMode = config.processingMode
    private val multiplier = config.rows * config.columns

    fun generateSequence(): Sequence<VirtualPage> {
        return sequence {
            when (processingMode) {
                ProcessingMode.MERGE_ALL -> {
                    for (item in items) {
                        val parser = PageRangeParser(item.pageSelectionText, item.pageCount)
                        val selectedPages = if (item.isInvertedSelection) {
                            parser.parseInverted()
                        } else {
                            parser.parse()
                        }
                        for (pageIdx in selectedPages) {
                            yield(
                                VirtualPage(
                                    fileAlias = item.aliasIdentifier,
                                    sourcePageIndex = pageIdx,
                                    displayLabel = "${item.aliasIdentifier}${pageIdx + 1}",
                                    mediaType = item.mediaType,
                                    sourceUri = item.uri
                                )
                            )
                        }
                    }
                }
                ProcessingMode.GRID_REPEAT -> {
                    for (item in items) {
                        val parser = PageRangeParser(item.pageSelectionText, item.pageCount)
                        val selectedPages = if (item.isInvertedSelection) {
                            parser.parseInverted()
                        } else {
                            parser.parse()
                        }
                        for (pageIdx in selectedPages) {
                            repeat(multiplier) {
                                yield(
                                    VirtualPage(
                                        fileAlias = item.aliasIdentifier,
                                        sourcePageIndex = pageIdx,
                                        displayLabel = "${item.aliasIdentifier}${pageIdx + 1}",
                                        mediaType = item.mediaType,
                                        sourceUri = item.uri
                                    )
                                )
                            }
                        }
                    }
                }
                ProcessingMode.PARALLEL_BATCH -> {
                    // For PARALLEL_BATCH, we process each item separately. 
                    // This generator is primarily used for MERGE_ALL overall sequence generation.
                }
            }
        }
    }

    // Generate sequence for a single item (useful for PARALLEL_BATCH)
    fun generateSequenceFor(item: NUpItem): Sequence<VirtualPage> {
        return sequence {
            val parser = PageRangeParser(item.pageSelectionText, item.pageCount)
            val selectedPages = if (item.isInvertedSelection) {
                parser.parseInverted()
            } else {
                parser.parse()
            }
            for (pageIdx in selectedPages) {
                if (processingMode == ProcessingMode.GRID_REPEAT) {
                    repeat(multiplier) {
                        yield(
                            VirtualPage(
                                fileAlias = item.aliasIdentifier,
                                sourcePageIndex = pageIdx,
                                displayLabel = "${item.aliasIdentifier}${pageIdx + 1}",
                                mediaType = item.mediaType,
                                sourceUri = item.uri
                            )
                        )
                    }
                } else {
                    yield(
                        VirtualPage(
                            fileAlias = item.aliasIdentifier,
                            sourcePageIndex = pageIdx,
                            displayLabel = "${item.aliasIdentifier}${pageIdx + 1}",
                            mediaType = item.mediaType,
                            sourceUri = item.uri
                        )
                    )
                }
            }
        }
    }

    fun calculateTotalOutputPages(rowsPerSheet: Int, colsPerSheet: Int): Int {
        if (processingMode == ProcessingMode.PARALLEL_BATCH) return 0
        val totalVirtualPages = generateSequence().count()
        val cellsPerSheet = rowsPerSheet * colsPerSheet
        if (cellsPerSheet == 0) return 0
        return kotlin.math.ceil(totalVirtualPages.toFloat() / cellsPerSheet).toInt()
    }
}
