package com.scholarvault.ui.pdf.flip

import androidx.compose.ui.unit.Density
import kotlin.math.absoluteValue
import kotlin.math.sin

object PageFlipGeometry {

    const val MAX_ROTATION = 180f
    
    fun calculateFlipState(
        pageOffset: Float,
        screenWidth: Float,
        density: Float
    ): PageFlipState {
        val sign = if (pageOffset < 0f) -1f else 1f
        val rawAbsOffset = pageOffset.absoluteValue
        
        // Apply spring-based easing to the page offset to ensure elegant, springy transitions when touch ends
        val absOffset = if (rawAbsOffset > 0f && rawAbsOffset < 1f) {
            val t = rawAbsOffset
            // Elastic underdamped spring function for organic, fluid physics
            (t + sin(t * Math.PI) * 0.10f * kotlin.math.cos(t * 3f * Math.PI)).toFloat().coerceIn(0f, 1f)
        } else {
            rawAbsOffset
        }
        
        val easedPageOffset = sign * absOffset
        
        var rotX = 0f
        var rotY = 0f
        var rotZ = 0f
        var transX = 0f
        var sX = 1f
        var sY = 1f
        var a = 1f
        var front = true
        var shadowAlpha = 0f
        
        // Emulate realistic camera distance for 3D perspective
        val camDist = 30f * density

        if (easedPageOffset < 0f && easedPageOffset >= -1f) {
            // Turning page peeling off to the left
            val u = 1f + easedPageOffset // ranges from 1.0 down to 0.0
            
            if (absOffset <= 0.5f) {
                // First half: follow the touch coordinate mapping closely using trigonometry
                val thetaRad = kotlin.math.acos(u.toDouble().coerceIn(0.01, 1.0)).toFloat()
                rotY = -thetaRad * (180f / Math.PI.toFloat())
            } else {
                // Second half: transition smoothly to fully flat -180 degrees
                val factor = (absOffset - 0.5f) / 0.5f
                rotY = -60f - (factor * 120f)
            }
            
            // Add slight curling peel effect along multiple axes
            val curveFactor = sin(absOffset * Math.PI).toFloat()
            rotX = curveFactor * 7.5f  // Curl top/bottom corners out of plane
            rotZ = -curveFactor * 3.5f // Assist the horizontal peel
            
            sX = 1f - (curveFactor * 0.08f)
            sY = 1f - (curveFactor * 0.02f)
            
            transX = easedPageOffset * screenWidth * 0.12f
            
            // Calculate which side of the page is visible
            front = rotY > -90f
            
            // Dynamic shadow alpha peaks at the midpoint of the curl
            shadowAlpha = curveFactor * 0.75f
            
            a = 1f
            
        } else if (easedPageOffset > 0f && easedPageOffset <= 1f) {
            // Next page waiting underneath
            transX = -pageOffset * screenWidth
            rotY = 0f
            a = 1f
            shadowAlpha = 0f
        } else if (easedPageOffset == 0f) {
            rotY = 0f
            a = 1f
            shadowAlpha = 0f
        } else {
            a = 0f
        }

        return PageFlipState(
            rotationX = rotX,
            rotationY = rotY,
            rotationZ = rotZ,
            translationX = transX,
            scaleX = sX,
            scaleY = sY,
            alpha = a,
            isFront = front,
            shadowAlpha = shadowAlpha,
            cameraDistance = camDist
        )
    }
}
