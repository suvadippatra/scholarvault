package com.scholarvault.ui.tools.pdf_inverter

import android.net.Uri
import java.util.UUID

enum class InvertMode(val label: String, val description: String) {
    WORD_PDF("Word PDF", "Inverts background, keeps text legible. Ideal for typical documents."),
    SCANNED_PDF("Scanned PDF", "Inverts whole page colors. Good for image-based PDFs."),
    SMART_INVERT("Smart Invert", "Preserves some original colors.")
}

data class PdfItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val pageCount: Int,
    val newName: String,
    val pagesToInvertStr: String = "All",
    val mode: InvertMode = InvertMode.WORD_PDF,
    val resultUri: Uri? = null,
    val isProcessing: Boolean = false,
    val error: String? = null
)
