package com.scholarvault.ui.tools

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FloatingCalculatorOverlay(onFullscreenRestore: () -> Unit) {
    if (!PipCalculatorManager.showPip) return

    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current

    LaunchedEffect(PipCalculatorManager.displayFormula, PipCalculatorManager.angleUnit) {
        val resVal = evaluateExpression(PipCalculatorManager.displayFormula, PipCalculatorManager.angleUnit)
        PipCalculatorManager.displayResult = if (resVal != null) formatResult(resVal) else ""
    }

    val screenWidthPx = context.resources.displayMetrics.widthPixels.toFloat()
    val screenHeightPx = context.resources.displayMetrics.heightPixels.toFloat()

    var showSciInPip by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (PipCalculatorManager.isMinimized) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            PipCalculatorManager.positionX.toInt()
                                .coerceIn(0, (screenWidthPx - with(density) { 60.dp.toPx() }).toInt()),
                            PipCalculatorManager.positionY.toInt()
                                .coerceIn(0, (screenHeightPx - with(density) { 120.dp.toPx() }).toInt())
                        )
                    }
                    .size(60.dp)
                    .clip(CircleShape)
                    .alpha(0.5f)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(2.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
                    .shadow(12.dp, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val maxX = screenWidthPx - with(density) { 60.dp.toPx() }
                            val maxY = screenHeightPx - with(density) { 120.dp.toPx() }
                            PipCalculatorManager.positionX = (PipCalculatorManager.positionX + dragAmount.x).coerceIn(0f, maxX)
                            PipCalculatorManager.positionY = (PipCalculatorManager.positionY + dragAmount.y).coerceIn(0f, maxY)
                        }
                    }
                    .clickable {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        PipCalculatorManager.isMinimized = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = "Restore Calculator",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            Surface(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            PipCalculatorManager.positionX.toInt().coerceIn(
                                0,
                                (screenWidthPx - with(density) { PipCalculatorManager.width.toPx() }).toInt()
                            ),
                            PipCalculatorManager.positionY.toInt().coerceIn(
                                0,
                                (screenHeightPx - with(density) { PipCalculatorManager.height.toPx() + 100.dp.toPx() }).toInt()
                            )
                        )
                    }
                    .size(PipCalculatorManager.width, PipCalculatorManager.height)
                    .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                    .shadow(16.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val maxX = screenWidthPx - with(density) { PipCalculatorManager.width.toPx() }
                                    val maxY = screenHeightPx - with(density) { PipCalculatorManager.height.toPx() + 100.dp.toPx() }
                                    PipCalculatorManager.positionX = (PipCalculatorManager.positionX + dragAmount.x).coerceIn(0f, maxX)
                                    PipCalculatorManager.positionY = (PipCalculatorManager.positionY + dragAmount.y).coerceIn(0f, maxY)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "Calc",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sci ${PipCalculatorManager.angleUnit}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    PipCalculatorManager.angleUnit = when (PipCalculatorManager.angleUnit) {
                                        AngleUnit.DEG -> AngleUnit.RAD
                                        AngleUnit.RAD -> AngleUnit.GRA
                                        AngleUnit.GRA -> AngleUnit.DEG
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.height(20.dp)
                            ) {
                                Text(
                                    text = "to " + when (PipCalculatorManager.angleUnit) {
                                        AngleUnit.DEG -> "RAD"
                                        AngleUnit.RAD -> "GRA"
                                        AngleUnit.GRA -> "DEG"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(2.dp))

                            TextButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showSciInPip = !showSciInPip
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.height(20.dp)
                            ) {
                                Text(
                                    text = if (showSciInPip) "Basic" else "Sci",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            IconButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    PipCalculatorManager.showPip = false
                                    onFullscreenRestore()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Launch,
                                    contentDescription = "Restore Full Screen",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(2.dp))
                            
                            IconButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    PipCalculatorManager.isMinimized = true
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Minimize",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(2.dp))
                            
                            IconButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    PipCalculatorManager.showPip = false
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        val scrollStatePip = rememberScrollState()
                        LaunchedEffect(PipCalculatorManager.displayFormula) {
                            scrollStatePip.animateScrollTo(scrollStatePip.maxValue)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollStatePip),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = PipCalculatorManager.displayFormula,
                                fontSize = if (PipCalculatorManager.displayFormula.length > 15) 15.sp else 20.sp,
                                maxLines = 1,
                                textAlign = TextAlign.End,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = PipCalculatorManager.displayResult,
                            fontSize = 13.sp,
                            maxLines = 1,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        val miniKeys = if (showSciInPip) {
                            listOf(
                                listOf("AC", "DEL", "sin", "cos", "tan"),
                                listOf("7", "8", "9", "÷", "√"),
                                listOf("4", "5", "6", "×", "^"),
                                listOf("1", "2", "3", "-", "log"),
                                listOf("0", ".", "%", "+", "ln"),
                                listOf("π", "e", "()", "=", "!")
                            )
                        } else {
                            listOf(
                                listOf("AC", "()", "%", "÷"),
                                listOf("7", "8", "9", "×"),
                                listOf("4", "5", "6", "-"),
                                listOf("1", "2", "3", "+"),
                                listOf("0", ".", "DEL", "=")
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            miniKeys.forEach { row ->
                                Row(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    row.forEach { btn ->
                                        val isMainAction = btn in listOf("=", "÷", "+", "-", "×")
                                        val isSpecial = btn in listOf("AC", "DEL", "%", "sin", "cos", "tan", "log", "ln", "√", "^", "π", "e")
                                        val containerColor = when {
                                            btn == "=" -> MaterialTheme.colorScheme.primary
                                            isMainAction -> MaterialTheme.colorScheme.primaryContainer
                                            isSpecial -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                        val contentColor = when {
                                            btn == "=" -> MaterialTheme.colorScheme.onPrimary
                                            isMainAction -> MaterialTheme.colorScheme.onPrimaryContainer
                                            isSpecial -> MaterialTheme.colorScheme.onSecondaryContainer
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(containerColor)
                                                .clickable {
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    when (btn) {
                                                        "AC" -> {
                                                            PipCalculatorManager.displayFormula = "0"
                                                            PipCalculatorManager.displayResult = ""
                                                        }
                                                        "DEL" -> {
                                                            if (PipCalculatorManager.displayFormula.length > 1) {
                                                                val fStr = PipCalculatorManager.displayFormula
                                                                val endsWithFunc = listOf("sin(", "cos(", "tan(", "log(", "abs(", "asin(", "acos(", "atan(").firstOrNull { fStr.endsWith(it) }
                                                                if (endsWithFunc != null) {
                                                                    PipCalculatorManager.displayFormula = fStr.dropLast(endsWithFunc.length)
                                                                } else if (fStr.endsWith("ln(")) {
                                                                    PipCalculatorManager.displayFormula = fStr.dropLast(3)
                                                                } else {
                                                                    PipCalculatorManager.displayFormula = fStr.dropLast(1)
                                                                }
                                                            } else {
                                                                PipCalculatorManager.displayFormula = "0"
                                                            }
                                                        }
                                                        "=" -> {
                                                            val resVal = evaluateExpression(PipCalculatorManager.displayFormula, PipCalculatorManager.angleUnit)
                                                            if (resVal != null) {
                                                                val formatted = formatResult(resVal)
                                                                PipCalculatorManager.addToHistory(context, PipCalculatorManager.displayFormula, formatted)
                                                                PipCalculatorManager.displayFormula = formatted
                                                                PipCalculatorManager.displayResult = ""
                                                            }
                                                        }
                                                        "sin" -> PipCalculatorManager.displayFormula = handleAppend(PipCalculatorManager.displayFormula, "sin(")
                                                        "cos" -> PipCalculatorManager.displayFormula = handleAppend(PipCalculatorManager.displayFormula, "cos(")
                                                        "tan" -> PipCalculatorManager.displayFormula = handleAppend(PipCalculatorManager.displayFormula, "tan(")
                                                        "log" -> PipCalculatorManager.displayFormula = handleAppend(PipCalculatorManager.displayFormula, "log(")
                                                        "ln" -> PipCalculatorManager.displayFormula = handleAppend(PipCalculatorManager.displayFormula, "ln(")
                                                        "√" -> PipCalculatorManager.displayFormula = handleAppend(PipCalculatorManager.displayFormula, "√(")
                                                        "()" -> {
                                                            PipCalculatorManager.displayFormula = handleParentheses(PipCalculatorManager.displayFormula)
                                                        }
                                                        else -> {
                                                            val nextSymbol = when (btn) {
                                                                "×" -> "×"
                                                                "÷" -> "÷"
                                                                else -> btn
                                                            }
                                                            PipCalculatorManager.displayFormula = handleAppend(PipCalculatorManager.displayFormula, nextSymbol)
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = btn,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = contentColor
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(48.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val deltaW = (dragAmount.x / density.density).dp
                                        val deltaH = (dragAmount.y / density.density).dp
                                        val targetW = PipCalculatorManager.width + deltaW
                                        val targetH = PipCalculatorManager.height + deltaH

                                        PipCalculatorManager.width = targetW.coerceIn(240.dp, 400.dp)
                                        PipCalculatorManager.height = targetH.coerceIn(300.dp, 600.dp)
                                    }
                                },
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Resize",
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
