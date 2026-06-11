package com.scholarvault.ui.tools.pdf_nup

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

class NUpKeyboardController {
    val isVisible: MutableState<Boolean> = mutableStateOf(false)
    val activeFieldKey: MutableState<String?> = mutableStateOf(null)
    private var currentValue: String = ""
    private var onValueChanged: ((String) -> Unit)? = null

    fun requestKeyboard(fieldKey: String, initialValue: String, onChange: (String) -> Unit) {
        currentValue = initialValue
        onValueChanged = onChange
        activeFieldKey.value = fieldKey
        isVisible.value = true
    }
    
    fun updateCurrentValue(value: String) {
        currentValue = value
    }

    fun handleKeyPress(key: String) {
        val newVal = when (key) {
            "C" -> ""
            "DEL", "⌫" -> if (currentValue.isNotEmpty()) currentValue.dropLast(1) else ""
            "SPACE" -> "$currentValue "
            else -> currentValue + key
        }
        currentValue = newVal
        onValueChanged?.invoke(newVal)
    }
    
    fun hide() {
        isVisible.value = false
        activeFieldKey.value = null
        onValueChanged = null
    }
}

val LocalNUpKeyboard = compositionLocalOf<NUpKeyboardController?> { null }
