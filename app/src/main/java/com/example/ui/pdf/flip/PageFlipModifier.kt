package com.scholarvault.ui.pdf.flip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.pageFlipApply(state: PageFlipState): Modifier = this.graphicsLayer {
    cameraDistance = state.cameraDistance
    transformOrigin = TransformOrigin(0f, 0.5f) // Flip from left spine
    
    rotationX = state.rotationX
    rotationY = state.rotationY
    rotationZ = state.rotationZ
    translationX = state.translationX
    scaleX = state.scaleX
    scaleY = state.scaleY
    alpha = state.alpha
}

@Composable
fun PageFlipShadowOverlay(state: PageFlipState, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        
        // Dynamic drop shadow simulating the curl occlusion
        if (state.shadowAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            0.0f to Color.Black.copy(alpha = state.shadowAlpha * 0.8f),
                            0.1f to Color.Black.copy(alpha = state.shadowAlpha * 0.2f),
                            0.3f to Color.Transparent,
                            0.8f to Color.White.copy(alpha = state.shadowAlpha * 0.4f),
                            1.0f to Color.Black.copy(alpha = state.shadowAlpha * 0.9f)
                        )
                    )
            )
        }
        
        // Blank white back page if we pass 90 degrees
        if (!state.isFront) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5)) // Off-white back page
            )
        }
    }
}
