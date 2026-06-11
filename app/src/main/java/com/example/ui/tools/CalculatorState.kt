package com.scholarvault.ui.tools

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- ANGLE UNIT ENUM ---
enum class AngleUnit { DEG, RAD, GRA }

// --- HISTORY STATE ---
data class CalculatorHistoryItem(val expression: String, val result: String)

// --- GLOBAL MANAGED STATE FOR SEAMLESS PIP TRANSITION ---
object PipCalculatorManager {
    var showPip by mutableStateOf(false)
    var isMinimized by mutableStateOf(false)
    var positionX by mutableStateOf(100f)
    var positionY by mutableStateOf(300f)
    var width by mutableStateOf(290.dp)
    var height by mutableStateOf(440.dp)

    var displayFormula by mutableStateOf("0")
    var displayResult by mutableStateOf("")
    val history = mutableStateListOf<CalculatorHistoryItem>()
    var angleUnit by mutableStateOf(AngleUnit.DEG)

    private var isHistoryLoaded = false

    fun loadHistoryIfNeeded(context: Context) {
        if (isHistoryLoaded) return
        isHistoryLoaded = true
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.getSharedPreferences("calculator_prefs", Context.MODE_PRIVATE)
            val saved = prefs.getString("history", "") ?: ""
            if (saved.isNotEmpty()) {
                val parsed = saved.split("|||").mapNotNull {
                    val parts = it.split("===")
                    if (parts.size == 2) CalculatorHistoryItem(parts[0], parts[1]) else null
                }
                withContext(Dispatchers.Main) {
                    history.clear()
                    history.addAll(parsed)
                }
            }
        }
    }

    fun addToHistory(context: Context, expression: String, result: String) {
        if (expression.isNotBlank() && result.isNotBlank() && expression != result) {
            if (history.firstOrNull()?.expression == expression) return
            
            history.add(0, CalculatorHistoryItem(expression, result))
            if (history.size > 50) {
                history.removeLast()
            }
            saveHistory(context)
        }
    }

    fun clearHistory(context: Context) {
        history.clear()
        saveHistory(context)
    }

    private fun saveHistory(context: Context) {
        val prefs = context.getSharedPreferences("calculator_prefs", Context.MODE_PRIVATE)
        val data = history.joinToString("|||") { "${it.expression}===${it.result}" }
        prefs.edit().putString("history", data).apply()
    }
}
