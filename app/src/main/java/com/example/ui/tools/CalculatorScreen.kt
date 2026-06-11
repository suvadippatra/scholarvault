package com.scholarvault.ui.tools

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarvault.ui.theme.LocalThemeController

@Composable
fun ColorizeExpression(expr: String): androidx.compose.ui.text.AnnotatedString {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    return buildAnnotatedString {
        var i = 0
        while (i < expr.length) {
            val char = expr[i]
            when {
                char == '(' || char == ')' -> {
                    pushStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold))
                    append(char)
                    pop()
                    i++
                }
                char in listOf('+', '-', '×', '÷', '^', '%', '!') -> {
                    pushStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.SemiBold))
                    append(char)
                    pop()
                    i++
                }
                char == 'π' || char == 'e' -> {
                    pushStyle(SpanStyle(color = tertiaryColor, fontWeight = FontWeight.Bold))
                    append(char)
                    pop()
                    i++
                }
                expr.startsWith("sin(", i) -> {
                    pushStyle(SpanStyle(color = tertiaryColor, fontWeight = FontWeight.SemiBold))
                    append("sin")
                    pop()
                    pushStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold))
                    append("(")
                    pop()
                    i += 4
                }
                expr.startsWith("cos(", i) -> {
                    pushStyle(SpanStyle(color = tertiaryColor, fontWeight = FontWeight.SemiBold))
                    append("cos")
                    pop()
                    pushStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold))
                    append("(")
                    pop()
                    i += 4
                }
                expr.startsWith("tan(", i) -> {
                    pushStyle(SpanStyle(color = tertiaryColor, fontWeight = FontWeight.SemiBold))
                    append("tan")
                    pop()
                    pushStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold))
                    append("(")
                    pop()
                    i += 4
                }
                expr.startsWith("asin(", i) -> {
                    pushStyle(SpanStyle(color = tertiaryColor, fontWeight = FontWeight.SemiBold))
                    append("asin")
                    pop()
                    pushStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold))
                    append("(")
                    pop()
                    i += 5
                }
                expr.startsWith("acos(", i) -> {
                    pushStyle(SpanStyle(color = tertiaryColor, fontWeight = FontWeight.SemiBold))
                    append("acos")
                    pop()
                    pushStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold))
                    append("(")
                    pop()
                    i += 5
                }
                expr.startsWith("atan(", i) -> {
                    pushStyle(SpanStyle(color = tertiaryColor, fontWeight = FontWeight.SemiBold))
                    append("atan")
                    pop()
                    pushStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold))
                    append("(")
                    pop()
                    i += 5
                }
                expr.startsWith("log(", i) -> {
                    pushStyle(SpanStyle(color = tertiaryColor, fontWeight = FontWeight.SemiBold))
                    append("log")
                    pop()
                    pushStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold))
                    append("(")
                    pop()
                    i += 4
                }
                expr.startsWith("ln(", i) -> {
                    pushStyle(SpanStyle(color = tertiaryColor, fontWeight = FontWeight.SemiBold))
                    append("ln")
                    pop()
                    pushStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold))
                    append("(")
                    pop()
                    i += 3
                }
                expr.startsWith("abs(", i) -> {
                    pushStyle(SpanStyle(color = tertiaryColor, fontWeight = FontWeight.SemiBold))
                    append("abs")
                    pop()
                    pushStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold))
                    append("(")
                    pop()
                    i += 4
                }
                char == '√' -> {
                    pushStyle(SpanStyle(color = tertiaryColor, fontWeight = FontWeight.SemiBold))
                    append("√")
                    pop()
                    i++
                }
                char == '∛' -> {
                    pushStyle(SpanStyle(color = tertiaryColor, fontWeight = FontWeight.SemiBold))
                    append("∛")
                    pop()
                    i++
                }
                else -> {
                    pushStyle(SpanStyle(color = onSurfaceColor))
                    append(char)
                    pop()
                    i++
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    
    LaunchedEffect(Unit) {
        PipCalculatorManager.showPip = false
        PipCalculatorManager.loadHistoryIfNeeded(context)
    }

    var showHistoryDialog by remember { mutableStateOf(false) }
    var showScientificMode by remember { mutableStateOf(false) }
    var isTrigInverseActive by remember { mutableStateOf(false) }

    LaunchedEffect(PipCalculatorManager.displayFormula, PipCalculatorManager.angleUnit) {
        val resVal = evaluateExpression(PipCalculatorManager.displayFormula, PipCalculatorManager.angleUnit)
        PipCalculatorManager.displayResult = if (resVal != null) formatResult(resVal) else ""
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            com.scholarvault.ui.components.TopSearchBar(
                title = "Scientific Calculator",
                isBackButton = true,
                onOpenDrawer = onBack,
                showSearchBar = false,
                showProfileIcon = false,
                actions = {
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(imageVector = Icons.Outlined.History, contentDescription = "History")
                    }
                    IconButton(onClick = {
                        PipCalculatorManager.showPip = true
                        onBack()
                    }) {
                        Icon(imageVector = Icons.Outlined.PictureInPicture, contentDescription = "Picture-in-Picture")
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val isTablet = maxWidth > 600.dp

            val DisplayContent = @Composable { modifier: Modifier ->
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    val scrollState = rememberScrollState()
                    LaunchedEffect(PipCalculatorManager.displayFormula) {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.End
                    ) {
                        val formulaTextSize = when {
                            PipCalculatorManager.displayFormula.length > 20 -> 26.sp
                            PipCalculatorManager.displayFormula.length > 12 -> 34.sp
                            else -> 48.sp
                        }
                        Text(
                            text = ColorizeExpression(PipCalculatorManager.displayFormula),
                            fontSize = formulaTextSize,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = PipCalculatorManager.displayResult,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1
                    )
                }
            }

            val KeypadContent = @Composable { modifier: Modifier ->
                val isDark = LocalThemeController.current.isDarkTheme
                
                val btnAcColor = if (isDark) Color(0xFF00E676) else Color(0xFFA5D6A7)
                val btnOpColor = if (isDark) Color(0xFF2196F3) else Color(0xFF90CAF9)
                val btnEqColor = if (isDark) Color(0xFFE91E63) else Color(0xFFF48FB1)
                val btnBgNumColor = if (isDark) Color(0xFF333333) else Color.White
                val btnNumTextColor = if (isDark) Color.White else Color.Black
                
                Column(
                    modifier = modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    val hapticFeedback = LocalHapticFeedback.current

                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp).padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Angle Mode Switcher explicitly designed
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .weight(1.5f)
                                .fillMaxHeight()
                                .clickable {
                                    PipCalculatorManager.angleUnit = when (PipCalculatorManager.angleUnit) {
                                        AngleUnit.DEG -> AngleUnit.RAD
                                        AngleUnit.RAD -> AngleUnit.GRA
                                        AngleUnit.GRA -> AngleUnit.DEG
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = PipCalculatorManager.angleUnit.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        val topActions = listOf("√", "π", "^")
                        topActions.forEach { btn ->
                            IconButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    PipCalculatorManager.displayFormula = handleAppend(PipCalculatorManager.displayFormula, if (btn == "√") "√(" else btn)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(btn, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        IconButton(
                            onClick = { 
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                showScientificMode = !showScientificMode 
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (showScientificMode) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Scientific Mode",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).padding(4.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // All buttons combined here dynamically
                        val buttonGrid = mutableListOf<List<String>>()

                        if (showScientificMode) {
                            val row1 = if (isTrigInverseActive) listOf("asin", "acos", "atan", "INV") else listOf("sin", "cos", "tan", "INV")
                            val row2 = listOf("!", "e", "ln", "log")
                            buttonGrid.add(row1)
                            buttonGrid.add(row2)
                        }

                        buttonGrid.addAll(listOf(
                            listOf("AC", "()", "%", "÷"),
                            listOf("7", "8", "9", "×"),
                            listOf("4", "5", "6", "-"),
                            listOf("1", "2", "3", "+"),
                            listOf("0", ".", "DEL", "=")
                        ))

                        buttonGrid.forEach { row ->
                            Row(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { btn ->
                                    val isOperator = btn in listOf("÷", "×", "-", "+")
                                    val isSpecial = btn in listOf("AC", "()", "%")
                                    val isSciBtn = showScientificMode && (row == buttonGrid.getOrNull(0) || row == buttonGrid.getOrNull(1))
                                    
                                    val containerColor = when {
                                        btn == "=" -> btnEqColor
                                        isOperator || isSpecial -> btnOpColor
                                        btn == "AC" -> btnAcColor
                                        isSciBtn -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> btnBgNumColor
                                    }

                                    val contentColor = when {
                                        btn == "=" || isOperator || isSpecial || btn == "AC" -> Color.Black
                                        isSciBtn -> MaterialTheme.colorScheme.onSurfaceVariant
                                        else -> btnNumTextColor
                                    }

                                    Button(
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            when (btn) {
                                                "INV" -> {
                                                    isTrigInverseActive = !isTrigInverseActive
                                                }
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
                                                        } else if (fStr.endsWith("∛(")) {
                                                            PipCalculatorManager.displayFormula = fStr.dropLast(2)
                                                        } else if (fStr.endsWith("mod")) {
                                                            PipCalculatorManager.displayFormula = fStr.dropLast(3)
                                                        } else {
                                                            PipCalculatorManager.displayFormula = fStr.dropLast(1)
                                                        }
                                                    } else {
                                                        PipCalculatorManager.displayFormula = "0"
                                                    }
                                                }
                                                "()" -> {
                                                    PipCalculatorManager.displayFormula = handleParentheses(PipCalculatorManager.displayFormula)
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
                                                else -> {
                                                    val appendedBtn = if (isSciBtn && btn != "!" && btn != "e" && btn != "INV") "$btn(" else btn
                                                    PipCalculatorManager.displayFormula = handleAppend(PipCalculatorManager.displayFormula, appendedBtn)
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(24.dp),
                                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = containerColor,
                                            contentColor = contentColor
                                        ),
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        if (btn == "DEL") {
                                            Icon(imageVector = Icons.Default.Backspace, contentDescription = "Delete", tint = contentColor, modifier = Modifier.size(24.dp))
                                        } else {
                                            Text(
                                                text = btn,
                                                fontSize = if (isSciBtn) 16.sp else 24.sp,
                                                fontWeight = if (isSciBtn) if (btn == "INV" && isTrigInverseActive) FontWeight.Black else FontWeight.Bold else FontWeight.Medium,
                                                color = if (btn == "INV" && isTrigInverseActive) MaterialTheme.colorScheme.primary else contentColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isTablet) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    DisplayContent(Modifier.weight(1f).fillMaxHeight())
                    KeypadContent(Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    DisplayContent(Modifier.weight(0.4f))
                    KeypadContent(Modifier.weight(0.6f))
                }
            }

            if (showHistoryDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showHistoryDialog = false }
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.65f)
                            .align(Alignment.TopCenter)
                            .clickable(enabled = false) {},
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Calculated History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Row {
                                    IconButton(onClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        PipCalculatorManager.clearHistory(context)
                                    }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear All", tint = MaterialTheme.colorScheme.error)
                                    }
                                    IconButton(onClick = { showHistoryDialog = false }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            if (PipCalculatorManager.history.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No calculation history yet", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(PipCalculatorManager.history) { item ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    PipCalculatorManager.displayFormula = item.expression
                                                    showHistoryDialog = false
                                                }
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                    RoundedCornerShape(14.dp)
                                                )
                                                .padding(14.dp),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(item.expression, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(item.result, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
