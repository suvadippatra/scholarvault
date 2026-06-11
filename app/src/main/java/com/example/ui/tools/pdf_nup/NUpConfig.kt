package com.scholarvault.ui.tools.pdf_nup

import android.graphics.Color
import android.net.Uri
import java.util.UUID

enum class ProcessingMode {
    MERGE_ALL, PARALLEL_BATCH, GRID_REPEAT
}

enum class MediaType {
    PDF, IMAGE
}

enum class CanvasSize(val widthPoints: Float, val heightPoints: Float, val displayName: String) {
    A4(595f, 842f, "A4"),
    A3(842f, 1191f, "A3"),
    LETTER(612f, 792f, "Letter"),
    LEGAL(612f, 1008f, "Legal"),
    CUSTOM(0f, 0f, "Custom")
}

enum class ArrangementOrder {
    LTR, RTL
}

enum class ReadingDirection {
    HORIZONTAL, VERTICAL
}

enum class PageLayoutMode {
    MULTIPLE_PAGES, SINGLE_PAGE
}

enum class PageOrientation {
    PORTRAIT, LANDSCAPE, AUTO
}

enum class PageFit {
    FULL_WIDTH,
    FULL_HEIGHT,
    AUTO,
    STRETCH
}

enum class FlowLayout {
    VERTICAL_TTB, VERTICAL_BTT, HORIZONTAL_TTB, HORIZONTAL_BTT
}

data class NUpConfig(
    val mode: PageLayoutMode = PageLayoutMode.MULTIPLE_PAGES,
    val processingMode: ProcessingMode = ProcessingMode.MERGE_ALL,
    val columns: Int = 2,
    val rows: Int = 1,
    val canvasSize: CanvasSize = CanvasSize.A4,
    val orientation: PageOrientation = PageOrientation.AUTO,
    val customWidthPoints: Float = 595f,
    val customHeightPoints: Float = 842f,
    val readingDirection: ReadingDirection = ReadingDirection.HORIZONTAL, // Deprecated, but left for compatibility if needed.
    val arrangementOrder: ArrangementOrder = ArrangementOrder.LTR,
    val flowLayout: FlowLayout = FlowLayout.HORIZONTAL_TTB,
    val marginTopDp: Float = 15f,
    val marginBottomDp: Float = 15f,
    val marginLeftDp: Float = 15f,
    val marginRightDp: Float = 15f,
    val innerSpacingDp: Float = 5f,
    val pageFit: PageFit = PageFit.AUTO,
    val drawBorder: Boolean = true,
    val borderTopWidthDp: Float = 1f,
    val borderBottomWidthDp: Float = 1f,
    val borderLeftWidthDp: Float = 1f,
    val borderRightWidthDp: Float = 1f,
    val isBorderIndependent: Boolean = false,
    val defaultBorderWidthMm: Float = 0.5f,
    val defaultBorderColor: Int = Color.BLACK,
    val outputFileName: String = "NUp_Document"
) {
    val actualCanvasWidthPoints: Float
        get() {
            val baseW = if (canvasSize == CanvasSize.CUSTOM) customWidthPoints else canvasSize.widthPoints
            val baseH = if (canvasSize == CanvasSize.CUSTOM) customHeightPoints else canvasSize.heightPoints
            return if (orientation == PageOrientation.LANDSCAPE) maxOf(baseW, baseH) 
                   else if (orientation == PageOrientation.PORTRAIT) minOf(baseW, baseH)
                   else baseW
        }

    val actualCanvasHeightPoints: Float
        get() {
            val baseW = if (canvasSize == CanvasSize.CUSTOM) customWidthPoints else canvasSize.widthPoints
            val baseH = if (canvasSize == CanvasSize.CUSTOM) customHeightPoints else canvasSize.heightPoints
            return if (orientation == PageOrientation.LANDSCAPE) minOf(baseW, baseH) 
                   else if (orientation == PageOrientation.PORTRAIT) maxOf(baseW, baseH)
                   else baseH
        }
}

enum class NUpProcessingState {
    IDLE, PROCESSING, SUCCESS, ERROR, PENDING
}

data class NUpItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val pageCount: Int,
    val mediaType: MediaType = MediaType.PDF,
    val aliasIdentifier: Char = 'A',
    val pageSelectionText: String = "",
    val isInvertedSelection: Boolean = false,
    val imageDpiSetting: Int = 300,
    val state: NUpProcessingState = NUpProcessingState.IDLE,
    val resultUri: Uri? = null,
    val errorMessage: String? = null
)
