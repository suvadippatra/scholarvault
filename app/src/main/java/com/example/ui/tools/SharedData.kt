package com.scholarvault.ui.tools

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

data class PrintJobItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val pageSequence: String = "",
    val isImage: Boolean = false,
    val resolution: String = "",
    val originalDpi: Int = 72,
    val outputDpi: Int = 300
)

object SharedData {
    val pendingUris = MutableStateFlow<List<Uri>>(emptyList())
    val navigateToPrePrint = MutableStateFlow(false)
    val pendingPdfUri = MutableStateFlow<Uri?>(null)
    val isQuickNoteWidgetVisible = MutableStateFlow(false)
}
