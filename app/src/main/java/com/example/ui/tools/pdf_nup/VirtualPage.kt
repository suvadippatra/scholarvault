package com.scholarvault.ui.tools.pdf_nup

import android.net.Uri

data class VirtualPage(
    val fileAlias: Char,
    val sourcePageIndex: Int,
    val displayLabel: String,
    val mediaType: MediaType,
    val sourceUri: Uri
)
