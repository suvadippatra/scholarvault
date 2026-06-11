package com.scholarvault.ui.pdf.flip

import androidx.compose.runtime.Immutable

@Immutable
data class PageFlipState(
    val rotationX: Float,
    val rotationY: Float,
    val rotationZ: Float,
    val translationX: Float,
    val scaleX: Float,
    val scaleY: Float,
    val alpha: Float,
    val isFront: Boolean,
    val shadowAlpha: Float,
    val cameraDistance: Float
)
