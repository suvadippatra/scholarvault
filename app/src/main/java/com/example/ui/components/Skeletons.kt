package com.scholarvault.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.scholarvault.ui.theme.LocalThemeController

@Composable
fun AnimatedShimmer(modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)) {
    val isDark = LocalThemeController.current.isDarkTheme
    val shimmerColor = if (isDark) Color.LightGray.copy(alpha = 0.2f) else Color.DarkGray.copy(alpha = 0.15f)
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerColor.copy(alpha = shimmerColor.alpha * alpha))
    )
}

@Composable
fun TaskSkeletonLine() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedShimmer(modifier = Modifier.size(24.dp), shape = RoundedCornerShape(12.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            AnimatedShimmer(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp))
            Spacer(modifier = Modifier.height(6.dp))
            AnimatedShimmer(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp))
        }
    }
}

@Composable
fun MaterialSkeletonCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedShimmer(modifier = Modifier.size(32.dp), shape = RoundedCornerShape(8.dp))
            Spacer(modifier = Modifier.width(12.dp))
            AnimatedShimmer(modifier = Modifier.fillMaxWidth(0.6f).height(18.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        AnimatedShimmer(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp))
        Spacer(modifier = Modifier.height(8.dp))
        AnimatedShimmer(modifier = Modifier.fillMaxWidth(0.3f).height(14.dp))
    }
}

@Composable
fun AcademicCourseSkeletonLine() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedShimmer(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            AnimatedShimmer(modifier = Modifier.fillMaxWidth(0.8f).height(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            AnimatedShimmer(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        AnimatedShimmer(modifier = Modifier.size(24.dp), shape = RoundedCornerShape(12.dp))
    }
}
