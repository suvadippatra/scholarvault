package com.scholarvault.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.foundation.border
import com.scholarvault.ui.theme.LocalThemeController

// Simple Mathematical Expression Evaluator for the Mini Calculator Keypad
class ExpressionParser(private val str: String) {
    private var pos = -1
    private var ch = 0

    private fun nextChar() {
        pos++
        ch = if (pos < str.length) str[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    fun parse(): Double {
        nextChar()
        val x = parseExpression()
        if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
        return x
    }

    private fun parseExpression(): Double {
        var x = parseTerm()
        while (true) {
            if (eat('+'.code)) x += parseTerm() // addition
            else if (eat('-'.code)) x -= parseTerm() // subtraction
            else return x
        }
    }

    private fun parseTerm(): Double {
        var x = parseFactor()
        while (true) {
            if (eat('*'.code) || eat('x'.code) || eat('×'.code)) x *= parseFactor() // multiplication
            else if (eat('/'.code) || eat('÷'.code)) {
                val divisor = parseFactor()
                if (divisor == 0.0) throw ArithmeticException("Division by zero")
                x /= divisor
            }
            else return x
        }
    }

    private fun parseFactor(): Double {
        if (eat('+'.code)) return parseFactor() // unary plus
        if (eat('-'.code)) return -parseFactor() // unary minus

        var x: Double
        val startPos = this.pos
        if (eat('('.code)) { // parentheses
            x = parseExpression()
            eat(')'.code)
        } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) { // numbers
            while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
            x = str.substring(startPos, this.pos).toDouble()
        } else {
            throw RuntimeException("Unexpected character")
        }
        return x
    }
}

fun evaluateExpression(expr: String): Double? {
    val cleanExpr = expr.replace("×", "*").replace("÷", "/").replace(" ", "")
    if (cleanExpr.isEmpty()) return null
    return try {
        ExpressionParser(cleanExpr).parse()
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(onBack: () -> Unit) {
    var category by remember { mutableStateOf(UnitConverterState.categories.first()) }
    
    val units = UnitConverterState.unitData[category] ?: emptyList()
    var fromUnit by remember { mutableStateOf(units.firstOrNull() ?: UnitItem("", 1.0)) }
    var toUnit by remember { mutableStateOf(units.getOrNull(1) ?: units.firstOrNull() ?: UnitItem("", 1.0)) }
    
    var inputValue by remember { mutableStateOf("1") }
    var outputValue by remember { mutableStateOf("") }
    
    var fromDropdownExpanded by remember { mutableStateOf(false) }
    var toDropdownExpanded by remember { mutableStateOf(false) }
    
    // Custom dialog states
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddUnitDialog by remember { mutableStateOf(false) }

    fun updateConversion(input: String) {
        // Try to evaluate expression first to show continuous results if it's a simple number
        val evaluatedVal = evaluateExpression(input)
        if (evaluatedVal != null) {
            val result = convertValue(evaluatedVal, fromUnit, toUnit, category)
            outputValue = if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                "%.6f".format(result).trimEnd('0').trimEnd('.')
            }
        } else {
            // Check if it's a raw parseable number directly
            val value = input.toDoubleOrNull()
            if (value != null) {
                val result = convertValue(value, fromUnit, toUnit, category)
                outputValue = if (result % 1.0 == 0.0) {
                    result.toLong().toString()
                } else {
                    "%.6f".format(result).trimEnd('0').trimEnd('.')
                }
            } else {
                outputValue = "—"
            }
        }
    }

    LaunchedEffect(category) {
        val currentUnits = UnitConverterState.unitData[category] ?: emptyList()
        if (currentUnits.isNotEmpty()) {
            if (fromUnit !in currentUnits) {
                fromUnit = currentUnits.firstOrNull() ?: UnitItem("", 1.0)
            }
            if (toUnit !in currentUnits) {
                toUnit = currentUnits.getOrNull(1) ?: currentUnits.firstOrNull() ?: UnitItem("", 1.0)
            }
        }
    }

    LaunchedEffect(category, fromUnit, toUnit, inputValue) {
        updateConversion(inputValue)
    }

    fun handleKeyPress(char: String) {
        when (char) {
            "C" -> {
                inputValue = "0"
            }
            "⌫" -> {
                inputValue = if (inputValue.length > 1) {
                    inputValue.dropLast(1)
                } else {
                    "0"
                }
            }
            "±" -> {
                inputValue = if (inputValue.startsWith("-")) {
                    inputValue.substring(1)
                } else if (inputValue == "0") {
                    "-"
                } else {
                    "-$inputValue"
                }
            }
            "%" -> {
                val currentNum = inputValue.toDoubleOrNull()
                if (currentNum != null) {
                    val percentageVal = currentNum / 100.0
                    inputValue = if (percentageVal % 1.0 == 0.0) {
                        percentageVal.toLong().toString()
                    } else {
                        "%.6f".format(percentageVal).trimEnd('0').trimEnd('.')
                    }
                } else {
                    inputValue += "/100"
                }
            }
            "=" -> {
                val res = evaluateExpression(inputValue)
                if (res != null) {
                    inputValue = if (res % 1.0 == 0.0) {
                        res.toLong().toString()
                    } else {
                        "%.6f".format(res).trimEnd('0').trimEnd('.')
                    }
                }
            }
            "00" -> {
                if (inputValue != "0" && inputValue != "-") {
                    inputValue += "00"
                }
            }
            else -> {
                if (inputValue == "0" && char !in listOf("+", "-", "×", "÷", ".")) {
                    inputValue = char
                } else {
                    inputValue += char
                }
            }
        }
    }

    val theme = LocalThemeController.current
    val isDark = theme.isDarkTheme
    val cardColor = if (isDark) MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val innerCardColor = if (isDark) MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp) else MaterialTheme.colorScheme.surfaceVariant

    // --- DIALOGS ---
    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var unitsText by remember { mutableStateOf("") }
        var formulasText by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add Custom Quantity") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Create a custom category with custom units and mathematical conversion relations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { 
                            newCatName = it
                            errorMsg = ""
                        },
                        label = { Text("Quantity Name") },
                        placeholder = { Text("e.g. My Currency, Digital Miles") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = unitsText,
                        onValueChange = { 
                            unitsText = it
                            errorMsg = ""
                        },
                        label = { Text("Units (One per line)") },
                        placeholder = { Text("e.g.\nGold\nSilver\nBronze") },
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Real-time dynamic numbering mapping layout in structured dynamic lettering format
                    val lines = unitsText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Assigned Unit List (Dynamic a, b, c... numbering):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .fillMaxWidth()
                    ) {
                        if (lines.isEmpty()) {
                            Text(
                                text = "  (No units entered yet. Type some units above to see numbering)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        } else {
                            lines.forEachIndexed { idx, line ->
                                val letter = ('a' + idx)
                                val cleanedName = if (line.matches(Regex("^[a-z]\\s*[:\\-=].*"))) {
                                    line.substring(line.indexOfAny(charArrayOf(':', '-', '=')) + 1).trim()
                                } else {
                                    line
                                }
                                Text(
                                    text = "  ${letter}.  $cleanedName" + (if (idx == 0) " (Base Unit of Category)" else ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = formulasText,
                        onValueChange = { 
                            formulasText = it
                            errorMsg = ""
                        },
                        label = { Text("Conversion Formulas") },
                        placeholder = { Text("e.g.\n1 a = 5 b\n1 b = 10 c") },
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (errorMsg.isNotEmpty()) {
                        Text(
                            text = errorMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCatName.isBlank()) {
                        errorMsg = "Quantity name cannot be empty"
                        return@TextButton
                    }
                    
                    val list = unitsText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                    if (list.isEmpty()) {
                        errorMsg = "Please define at least one unit"
                        return@TextButton
                    }
                    
                    val parsedUnits = mutableListOf<Pair<Char, String>>()
                    list.forEachIndexed { idx, line ->
                        val charPrefix = ('a' + idx)
                        val cleanedName = if (line.matches(Regex("^[a-z]\\s*[:\\-=].*"))) {
                            line.substring(line.indexOfAny(charArrayOf(':', '-', '=')) + 1).trim()
                        } else {
                            line
                        }
                        parsedUnits.add(charPrefix to cleanedName)
                    }
                    
                    val multipliers = mutableMapOf<Char, Double>()
                    multipliers['a'] = 1.0 // 'a' is always base unit
                    
                    val equations = mutableListOf<Triple<Char, Double, Char>>()
                    formulasText.split("\n").map { it.trim() }.filter { it.isNotBlank() }.forEach { line ->
                        val parts = line.split("=")
                        if (parts.size == 2) {
                            val left = parts[0].trim()
                            val right = parts[1].trim()
                            
                            val leftMatch = Regex("""([0-9.]+)?\s*([a-z])""").find(left)
                            val rightMatch = Regex("""([0-9.]+)?\s*([a-z])""").find(right)
                            
                            if (leftMatch != null && rightMatch != null) {
                                val val1 = leftMatch.groupValues[1].toDoubleOrNull() ?: 1.0
                                val var1 = leftMatch.groupValues[2].first()
                                val val2 = rightMatch.groupValues[1].toDoubleOrNull() ?: 1.0
                                val var2 = rightMatch.groupValues[2].first()
                                
                                equations.add(Triple(var1, val2 / val1, var2))
                            }
                        }
                    }
                    
                    // Propagate ratio multipliers 15 times for transitivity resolver
                    repeat(15) {
                        for (eq in equations) {
                            val (v1, ratio, v2) = eq
                            if (multipliers.containsKey(v1) && !multipliers.containsKey(v2)) {
                                multipliers[v2] = multipliers[v1]!! / ratio
                            } else if (multipliers.containsKey(v2) && !multipliers.containsKey(v1)) {
                                multipliers[v1] = multipliers[v2]!! * ratio
                            }
                        }
                    }
                    
                    // Construct
                    val finalUnits = parsedUnits.map { (char, unitName) ->
                        val mult = multipliers[char] ?: 1.0
                        val rText = if (char == 'a') {
                            "Base unit of custom quantity ($newCatName)"
                        } else {
                            "1 $unitName = $mult × ${parsedUnits.first().second}"
                        }
                        UnitItem(unitName, mult, rText)
                    }
                    
                    UnitConverterState.addCustomCategory(newCatName.trim(), finalUnits)
                    category = UnitConverterState.categories.lastOrNull() ?: category
                    showAddCategoryDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddUnitDialog) {
        var unitName by remember { mutableStateOf("") }
        var relationValue by remember { mutableStateOf("") }
        var unitRelationDirection by remember { mutableStateOf(0) } // 0: 1 [New Unit] = X [Base Unit], 1: 1 [Base Unit] = X [New Unit]
        var unitRule by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf("") }
        
        // Find the base unit of the active category
        val activeUnits = UnitConverterState.unitData[category] ?: emptyList()
        val baseUnit = activeUnits.firstOrNull() ?: UnitItem("Base", 1.0)
        
        AlertDialog(
            onDismissRequest = { showAddUnitDialog = false },
            title = { Text("Add Custom Unit to ${category.title}") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Add a custom unit to this category. Define its mathematical relation structure to the base unit (${baseUnit.name}).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = unitName,
                        onValueChange = { 
                            unitName = it
                            errorMsg = ""
                        },
                        label = { Text("Unit Name") },
                        placeholder = { Text("e.g. Centimeter, Gram") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Define Conversion Relation Direction:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { unitRelationDirection = 0 }
                        ) {
                            RadioButton(
                                selected = unitRelationDirection == 0,
                                onClick = { unitRelationDirection = 0 }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "1 [New Unit] = X [${baseUnit.name}]\n(e.g. 1 km = 1000 m)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { unitRelationDirection = 1 }
                        ) {
                            RadioButton(
                                selected = unitRelationDirection == 1,
                                onClick = { unitRelationDirection = 1 }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "1 [${baseUnit.name}] = X [New Unit]\n(e.g. 1 m = 100 cm)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    OutlinedTextField(
                        value = relationValue,
                        onValueChange = { 
                            relationValue = it
                            errorMsg = ""
                        },
                        label = { Text("Relation Multiplier (Value of 'X')") },
                        placeholder = { Text("e.g. 1000, 12, 100") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = unitRule,
                        onValueChange = { unitRule = it },
                        label = { Text("Conversion Rule Description / Note") },
                        placeholder = { Text("e.g. 1 yard = 3 feet") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMsg.isNotEmpty()) {
                        Text(
                            text = errorMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (unitName.isBlank()) {
                        errorMsg = "Please enter a unit name."
                        return@TextButton
                    }
                    val xVal = relationValue.toDoubleOrNull()
                    if (xVal == null || xVal <= 0.0) {
                        errorMsg = "Please enter a valid positive multiplier X."
                        return@TextButton
                    }

                    val computedMultiplier = if (unitRelationDirection == 0) {
                        xVal * baseUnit.multiplier
                    } else {
                        baseUnit.multiplier / xVal
                    }

                    val formattedRule = if (unitRule.isNotBlank()) {
                        unitRule.trim()
                    } else {
                        if (unitRelationDirection == 0) {
                            "1 $unitName = $relationValue ${baseUnit.name}"
                        } else {
                            "1 ${baseUnit.name} = $relationValue $unitName"
                        }
                    }

                    UnitConverterState.addCustomUnit(category, unitName, computedMultiplier, formattedRule)
                    showAddUnitDialog = false
                }) {
                    Text("Add Unit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUnitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Mathematical formula construction
    val ratioVal = if (toUnit.multiplier != 0.0) fromUnit.multiplier / toUnit.multiplier else 1.0
    val formattedRatio = if (ratioVal % 1.0 == 0.0) {
        ratioVal.toLong().toString()
    } else {
        "%.6f".format(ratioVal).trimEnd('0').trimEnd('.')
    }

    val formulaText = if (category.id.equals("temperature", ignoreCase = true)) {
        when {
            fromUnit.name == "Celsius" && toUnit.name == "Fahrenheit" -> "°F = (°C × 1.8) + 32"
            fromUnit.name == "Fahrenheit" && toUnit.name == "Celsius" -> "°C = (°F - 32) ÷ 1.8"
            fromUnit.name == "Celsius" && toUnit.name == "Kelvin" -> "K = °C + 273.15"
            fromUnit.name == "Kelvin" && toUnit.name == "Celsius" -> "°C = K - 273.15"
            fromUnit.name == "Fahrenheit" && toUnit.name == "Kelvin" -> "K = (°F - 32) × 5/9 + 273.15"
            fromUnit.name == "Kelvin" && toUnit.name == "Fahrenheit" -> "°F = (K - 273.15) × 1.8 + 32"
            else -> "1 1 ${fromUnit.name} = 1 ${toUnit.name}"
        }
    } else {
        "1 ${fromUnit.name} = $formattedRatio ${toUnit.name}"
    }

    Scaffold(
        topBar = {
            com.scholarvault.ui.components.TopSearchBar(
                onOpenDrawer = onBack,
                isBackButton = true,
                title = "Unit Converter",
                showProfileIcon = false,
                showSearchBar = false,
                actions = {
                    IconButton(onClick = { showAddCategoryDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Custom Quantity")
                    }
                }
            )
        }
    ) { paddingValues ->
        var keypadVisible by remember { mutableStateOf(true) }
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isTablet = configuration.screenWidthDp >= 840

        if (!isTablet) {
            // --- MOBILE LAYOUT: SCROLL VIEW + BOTTOM LOCKED KEYPAD ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Category list Rows (Row 1 and Row 2)
                    CategoryRowsSection(
                        category = category,
                        onCategorySelect = { cat ->
                            category = cat
                            val newUnits = UnitConverterState.unitData[cat] ?: emptyList()
                            fromUnit = newUnits.firstOrNull() ?: UnitItem("", 1.0)
                            toUnit = newUnits.getOrNull(1) ?: newUnits.firstOrNull() ?: UnitItem("", 1.0)
                        }
                    )

                    // Title Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Convert ${category.title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = { showAddUnitDialog = true }) {
                            Text("+ Add Unit")
                        }
                    }

                    // Converter Card containing horizontal list unit selection dropdowns
                    ConverterPrimaryCard(
                        category = category,
                        fromUnit = fromUnit,
                        toUnit = toUnit,
                        inputValue = inputValue,
                        outputValue = outputValue,
                        cardColor = cardColor,
                        innerCardColor = innerCardColor,
                        fromDropdownExpanded = fromDropdownExpanded,
                        toDropdownExpanded = toDropdownExpanded,
                        onFromDropdownToggle = { fromDropdownExpanded = it },
                        onToDropdownToggle = { toDropdownExpanded = it },
                        onFromUnitSelected = { fromUnit = it },
                        onToUnitSelected = { toUnit = it },
                        onSwapUnits = {
                            val tempUnit = fromUnit
                            fromUnit = toUnit
                            toUnit = tempUnit
                            
                            val tempVal = inputValue
                            inputValue = if (outputValue == "—" || outputValue == "Invalid") "1" else outputValue
                            outputValue = tempVal
                        }
                    )

                    // Formula Card Section
                    FormulaCardSegment(
                        fromUnit = fromUnit,
                        toUnit = toUnit,
                        category = category,
                        currentExpression = inputValue
                    )
                }

                // Locked collapsible keyboard panel (Footer container layout)
                Card(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) {
                            MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Keypad grid is packed above the controls footer to cleanly position the 🔽 at the very bottom
                        if (keypadVisible) {
                            Spacer(modifier = Modifier.height(6.dp))
                            KeypadGridSection(
                                isDark = isDark,
                                onCharacterInput = { handleKeyPress(it) }
                            )
                        }

                        // Compact controls row with active arrow indicators placed at the absolute bottom
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { keypadVisible = !keypadVisible }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Interactive Keyboard Panel",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { keypadVisible = !keypadVisible },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (keypadVisible) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                    contentDescription = if (keypadVisible) "Hide Keyboard Panel" else "Show Keyboard Panel",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // --- TABLET / DESKTOP RESPONSIVE LAYOUT (2-COLUMN INTERFACE WIT FLOATING CALCULATOR) ---
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Column: Category rows & active converter Card (takes full visual weight)
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Category Selection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    CategoryRowsSection(
                        category = category,
                        onCategorySelect = { cat ->
                            category = cat
                            val newUnits = UnitConverterState.unitData[cat] ?: emptyList()
                            fromUnit = newUnits.firstOrNull() ?: UnitItem("", 1.0)
                            toUnit = newUnits.getOrNull(1) ?: newUnits.firstOrNull() ?: UnitItem("", 1.0)
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Convert ${category.title}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = { showAddUnitDialog = true }) {
                            Text("+ Add Unit")
                        }
                    }

                    ConverterPrimaryCard(
                        category = category,
                        fromUnit = fromUnit,
                        toUnit = toUnit,
                        inputValue = inputValue,
                        outputValue = outputValue,
                        cardColor = cardColor,
                        innerCardColor = innerCardColor,
                        fromDropdownExpanded = fromDropdownExpanded,
                        toDropdownExpanded = toDropdownExpanded,
                        onFromDropdownToggle = { fromDropdownExpanded = it },
                        onToDropdownToggle = { toDropdownExpanded = it },
                        onFromUnitSelected = { fromUnit = it },
                        onToUnitSelected = { toUnit = it },
                        onSwapUnits = {
                            val tempUnit = fromUnit
                            fromUnit = toUnit
                            toUnit = tempUnit
                            
                            val tempVal = inputValue
                            inputValue = if (outputValue == "—" || outputValue == "Invalid") "1" else outputValue
                            outputValue = tempVal
                        }
                    )
                }

                // Right Column: Formula Card & elegant compact "floating keyboard" calculator panel
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FormulaCardSegment(
                        fromUnit = fromUnit,
                        toUnit = toUnit,
                        category = category,
                        currentExpression = inputValue
                    )

                    // Floating style card keypad (Max width 360 to prevent large awkward keyboard layout stretching)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 360.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) {
                                MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Calculator Pad",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            
                            // Buttons grid
                            KeypadGridSection(
                                isDark = isDark,
                                onCharacterInput = { handleKeyPress(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryRowsSection(
    category: UnitCategory,
    onCategorySelect: (UnitCategory) -> Unit
) {
    val allCategories = UnitConverterState.categories
    val halfSize = (allCategories.size + 1) / 2
    val row1Categories = allCategories.take(halfSize)
    val row2Categories = allCategories.drop(halfSize)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row1Categories.forEach { cat ->
                val selected = category == cat
                val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                val textColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                
                Row(
                    modifier = Modifier
                        .background(containerColor, RoundedCornerShape(12.dp))
                        .clickable(onClickLabel = "Select ${cat.title} category") {
                            onCategorySelect(cat)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(cat.icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = textColor)
                    Spacer(Modifier.width(6.dp))
                    Text(cat.title, style = MaterialTheme.typography.bodyMedium, color = textColor)
                }
            }
        }
        
        // Row 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row2Categories.forEach { cat ->
                val selected = category == cat
                val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                val textColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                
                Row(
                    modifier = Modifier
                        .background(containerColor, RoundedCornerShape(12.dp))
                        .clickable(onClickLabel = "Select ${cat.title} category") {
                            onCategorySelect(cat)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(cat.icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = textColor)
                    Spacer(Modifier.width(6.dp))
                    Text(cat.title, style = MaterialTheme.typography.bodyMedium, color = textColor)
                }
            }
        }
    }
}

// Custom compact horizontal centered list dropdown selector container
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterPrimaryCard(
    category: UnitCategory,
    fromUnit: UnitItem,
    toUnit: UnitItem,
    inputValue: String,
    outputValue: String,
    cardColor: Color,
    innerCardColor: Color,
    fromDropdownExpanded: Boolean,
    toDropdownExpanded: Boolean,
    onFromDropdownToggle: (Boolean) -> Unit,
    onToDropdownToggle: (Boolean) -> Unit,
    onFromUnitSelected: (UnitItem) -> Unit,
    onToUnitSelected: (UnitItem) -> Unit,
    onSwapUnits: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ---- FROM UNIT SELECTION BLOCK ---
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "From", 
                        style = MaterialTheme.typography.labelLarge, 
                        color = MaterialTheme.colorScheme.primary, 
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    
                    Box {
                        OutlinedButton(
                            onClick = { onFromDropdownToggle(true) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = fromUnit.name, 
                                    maxLines = 1, 
                                    style = MaterialTheme.typography.bodyMedium, 
                                    overflow = TextOverflow.Ellipsis, 
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Compact horizontal unit selectable slider layout in a highly polished rectangular box
                        DropdownMenu(
                            expanded = fromDropdownExpanded,
                            onDismissRequest = { onFromDropdownToggle(false) },
                            offset = DpOffset(0.dp, 4.dp),
                            modifier = Modifier
                                .width(280.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(
                                    text = "Select From Unit (Scroll horizontally)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val currentUnits = UnitConverterState.unitData[category] ?: emptyList()
                                    currentUnits.forEach { unit ->
                                        val isSelected = fromUnit.name == unit.name
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), 
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .clickable {
                                                    onFromUnitSelected(unit)
                                                    onFromDropdownToggle(false)
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = unit.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        readOnly = true, // Feed driven strictly by interactive keyboard
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        )
                    )
                }

                // Swap Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(top = 28.dp)
                ) {
                    FilledIconButton(
                        onClick = onSwapUnits,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.SyncAlt, contentDescription = "Swap Units", modifier = Modifier.size(18.dp))
                    }
                }

                // ---- TO UNIT SELECTION BLOCK ---
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "To", 
                        style = MaterialTheme.typography.labelLarge, 
                        color = MaterialTheme.colorScheme.primary, 
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    
                    Box {
                        OutlinedButton(
                            onClick = { onToDropdownToggle(true) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = toUnit.name, 
                                    maxLines = 1, 
                                    style = MaterialTheme.typography.bodyMedium, 
                                    overflow = TextOverflow.Ellipsis, 
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Compact horizontal unit selectable slider layout in a highly polished rectangular box
                        // Offset by -120.dp to right-align the dropdown
                        DropdownMenu(
                            expanded = toDropdownExpanded,
                            onDismissRequest = { onToDropdownToggle(false) },
                            offset = DpOffset((-120).dp, 4.dp),
                            modifier = Modifier
                                .width(280.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(
                                    text = "Select To Unit (Scroll horizontally)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val currentUnits = UnitConverterState.unitData[category] ?: emptyList()
                                    currentUnits.forEach { unit ->
                                        val isSelected = toUnit.name == unit.name
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), 
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .clickable {
                                                    onToUnitSelected(unit)
                                                    onToDropdownToggle(false)
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = unit.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(innerCardColor, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = outputValue,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

fun getFraction(value: Double): Pair<Long, Long>? {
    if (value <= 0.0 || value > 1e6) return null
    val integerPart = Math.round(value)
    if (Math.abs(value - integerPart) < 1e-7) {
        return null
    }
    
    var number = value
    val maxDenominator = 1000L
    var m00 = 1L
    var m01 = 0L
    var m10 = 0L
    var m11 = 1L

    var x = number
    var ai = x.toLong()
    
    var t = m00 * ai + m01
    m01 = m00
    m00 = t
    
    t = m10 * ai + m11
    m11 = m10
    m10 = t
    
    while (x - ai > 1e-9 && m10 < maxDenominator) {
        x = 1.0 / (x - ai)
        ai = x.toLong()
        if (m10 * ai + m11 > maxDenominator) break
        
        t = m00 * ai + m01
        m01 = m00
        m00 = t
        
        t = m10 * ai + m11
        m11 = m10
        m10 = t
    }
    
    val approx = m00.toDouble() / m10.toDouble()
    if (Math.abs(value - approx) < 1e-4) {
        if (m10 == 1L) return null
        return Pair(m00, m10)
    }
    return null
}

@Composable
fun FractionLayout(num: String, den: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = num,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(1.5.dp)
                .background(MaterialTheme.colorScheme.outline)
        )
        Text(
            text = den,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FormulaDisplay(
    fromUnit: UnitItem,
    toUnit: UnitItem,
    category: UnitCategory
) {
    val isTemp = category.id.equals("temperature", ignoreCase = true)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isTemp) {
            when {
                fromUnit.name == "Celsius" && toUnit.name == "Fahrenheit" -> {
                    Text("°F = (", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                    Text("°C", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 20.sp)
                    Text(" × ", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                    FractionLayout(num = "9", den = "5")
                    Text(") + 32", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                }
                fromUnit.name == "Fahrenheit" && toUnit.name == "Celsius" -> {
                    Text("°C = (", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                    Text("°F", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 20.sp)
                    Text(" - 32) × ", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                    FractionLayout(num = "5", den = "9")
                }
                fromUnit.name == "Celsius" && toUnit.name == "Kelvin" -> {
                    Text("K = °C + 273.15", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                }
                fromUnit.name == "Kelvin" && toUnit.name == "Celsius" -> {
                    Text("°C = K - 273.15", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                }
                fromUnit.name == "Fahrenheit" && toUnit.name == "Kelvin" -> {
                    Text("K = (", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                    Text("°F", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 20.sp)
                    Text(" - 32) × ", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                    FractionLayout(num = "5", den = "9")
                    Text(" + 273.15", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                }
                fromUnit.name == "Kelvin" && toUnit.name == "Fahrenheit" -> {
                    Text("°F = (", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                    Text("K", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 20.sp)
                    Text(" - 273.15) × ", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                    FractionLayout(num = "9", den = "5")
                    Text(" + 32", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                }
                else -> {
                    Text("1 ${fromUnit.name} = 1 ${toUnit.name}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                }
            }
        } else {
            val ratioVal = if (toUnit.multiplier != 0.0) fromUnit.multiplier / toUnit.multiplier else 1.0
            val fraction = getFraction(ratioVal)
            
            Text("1 ", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.outline, fontSize = 18.sp)
            Text(fromUnit.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 18.sp)
            Text(" = ", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
            
            if (fraction != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FractionLayout(num = fraction.first.toString(), den = fraction.second.toString())
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(toUnit.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 18.sp)
                }
            } else {
                val formattedRatio = if (ratioVal % 1.0 == 0.0) {
                    ratioVal.toLong().toString()
                } else {
                    "%.6f".format(ratioVal).trimEnd('0').trimEnd('.')
                }
                Text("$formattedRatio ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                Text(toUnit.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun FormulaCardSegment(
    fromUnit: UnitItem,
    toUnit: UnitItem,
    category: UnitCategory,
    currentExpression: String
) {
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Mathematical Conversion Formula",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Current Input Expression
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Input / Expression",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (currentExpression.isBlank()) "0" else currentExpression,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                FormulaDisplay(
                    fromUnit = fromUnit,
                    toUnit = toUnit,
                    category = category
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "About ${fromUnit.name}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = fromUnit.rule.ifEmpty { "Standard unit for regional or global conversion." },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "About ${toUnit.name}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = toUnit.rule.ifEmpty { "Standard unit for regional or global conversion." },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun KeypadGridSection(
    isDark: Boolean,
    onCharacterInput: (String) -> Unit
) {
    val buttonSpacing = 6.dp
    val buttons = listOf(
        listOf("C", "±", "%", "⌫"),
        listOf("7", "8", "9", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("1", "2", "3", "-"),
        listOf("00", "0", ".", "+", "=")
    )

    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(buttonSpacing)
    ) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                row.forEach { char ->
                    val isOperator = char in listOf("÷", "×", "-", "+", "=")
                    val isAction = char in listOf("C", "±", "%", "⌫")
                    val buttonColor = when {
                        char == "=" -> MaterialTheme.colorScheme.primary
                        isOperator -> MaterialTheme.colorScheme.primaryContainer
                        isAction -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val contentColor = when {
                        char == "=" -> MaterialTheme.colorScheme.onPrimary
                        isOperator -> MaterialTheme.colorScheme.onPrimaryContainer
                        isAction -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Button(
                        onClick = { onCharacterInput(char) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor,
                            contentColor = contentColor
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
