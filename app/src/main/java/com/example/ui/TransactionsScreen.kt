package com.scholarvault.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.scholarvault.data.model.TransactionAccount
import com.scholarvault.data.model.TransactionItem
import com.scholarvault.ui.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    navController: NavController,
    viewModel: TransactionViewModel
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val currentAccount by viewModel.currentAccount.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showAccountManager by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (accounts.isEmpty()) {
                        Text("Transactions")
                    } else {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = currentAccount?.name ?: "Select Account",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                shape = RoundedCornerShape(24.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                accounts.forEach { account ->
                                    DropdownMenuItem(
                                        text = { Text(account.name) },
                                        onClick = {
                                            viewModel.selectAccount(account.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAccountManager = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Manage Accounts")
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentAccount != null) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            val isTablet = maxWidth > 600.dp
            
            if (currentAccount == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No Accounts Found", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showAccountManager = true }) {
                            Text("Create Account")
                        }
                    }
                }
            } else {
                val income = transactions.filter { !it.isExpense }.sumOf { it.amount }
                val expense = transactions.filter { it.isExpense }.sumOf { it.amount }

                if (isTablet) {
                    Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            SummaryCards(currentAccount!!, income, expense, true)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(2f)) {
                            TransactionList(transactions)
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        SummaryCards(currentAccount!!, income, expense, false)
                        Spacer(modifier = Modifier.height(16.dp))
                        TransactionList(transactions)
                    }
                }
            }
        }
    }

    if (showAddDialog && currentAccount != null) {
        AddTransactionModal(
            account = currentAccount!!,
            onDismiss = { showAddDialog = false },
            onAdd = { viewModel.addTransaction(it); showAddDialog = false }
        )
    }

    if (showAccountManager) {
        AccountManagerModal(
            accounts = accounts,
            onDismiss = { showAccountManager = false },
            onAddAccount = { name, initial -> viewModel.addAccount(name, initial) },
            onDeleteAccount = { viewModel.deleteAccount(it) }
        )
    }
}

@Composable
fun SummaryCards(account: TransactionAccount, income: Double, expense: Double, isTablet: Boolean) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    if (isTablet) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SummaryCard("Total Balance", account.currentBalance, containerColor, contentColor, Modifier.fillMaxWidth().padding(bottom = 8.dp))
            SummaryCard("Total Income", income, containerColor, contentColor, Modifier.fillMaxWidth().padding(bottom = 8.dp))
            SummaryCard("Total Expense", expense, containerColor, contentColor, Modifier.fillMaxWidth().padding(bottom = 8.dp))
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Income", income, containerColor, contentColor, Modifier.weight(1f))
            SummaryCard("Expense", expense, containerColor, contentColor, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        SummaryCard("Total Balance", account.currentBalance, containerColor, contentColor, Modifier.fillMaxWidth())
    }
}

@Composable
fun SummaryCard(title: String, amount: Double, containerColor: Color, contentColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(String.format("$%.2f", amount), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagerModal(
    accounts: List<TransactionAccount>,
    onDismiss: () -> Unit,
    onAddAccount: (String, Double) -> Unit,
    onDeleteAccount: (TransactionAccount) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        var name by remember { mutableStateOf("") }
        var initialBalance by remember { mutableStateOf("") }

        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Text("Manage Accounts", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Account Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = initialBalance,
                onValueChange = { initialBalance = it },
                label = { Text("Initial Balance") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAddAccount(name, initialBalance.toDoubleOrNull() ?: 0.0)
                        name = ""
                        initialBalance = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Account")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Existing Accounts", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            accounts.forEach { account ->
                var showDeleteConfirm by remember { mutableStateOf(false) }
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(account.name, fontWeight = FontWeight.Bold)
                                Text("Balance: $${account.currentBalance}", fontSize = 14.sp)
                            }
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                        if (showDeleteConfirm) {
                            var deleteText by remember { mutableStateOf("") }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("This will permanently wipe all transactions. Type 'DeLeTe' to confirm.", color = Color.Red, fontSize = 12.sp)
                            OutlinedTextField(
                                value = deleteText,
                                onValueChange = { deleteText = it },
                                label = { Text("Type DeLeTe") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                                if (deleteText == "DeLeTe") {
                                    Button(
                                        onClick = {
                                            onDeleteAccount(account)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                    ) {
                                        Text("Confirm Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun TransactionList(transactions: List<TransactionItem>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(transactions) { item ->
            TransactionRow(item)
        }
    }
}

@Composable
fun TransactionRow(item: TransactionItem) {
    var showDetails by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { showDetails = true },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val icon = if (item.category == "Electricity") Icons.Default.Bolt else Icons.Default.Receipt
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.category, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(item.details, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(item.timestamp)),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            val amountColor = if (item.isExpense) Color.Red else Color(0xFF388E3C)
            val sign = if (item.isExpense) "-" else "+"
            Text("$sign$${String.format("%.2f", item.amount)}", fontWeight = FontWeight.Bold, color = amountColor, fontSize = 16.sp)
            
            if (item.attachmentDocId != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.AttachFile, contentDescription = "Attachment", modifier = Modifier.size(16.dp))
            }
        }
    }

    if (showDetails) {
        TransactionDetailDialog(item, onDismiss = { showDetails = false })
    }
}

@Composable
fun TransactionDetailDialog(item: TransactionItem, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (item.isExpense) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (item.isExpense) Color.Red else Color(0xFF388E3C),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                val sign = if (item.isExpense) "-" else "+"
                Text("$sign$${String.format("%.2f", item.amount)}", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(if (item.isExpense) "Paid Successfully" else "Received Safely", color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Date & Time", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(item.timestamp)))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Category", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.category)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Details", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.details)
                }
                
                if (item.category == "Electricity" && item.meterReadingStart != null && item.meterReadingEnd != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Units Consumed", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${item.meterReadingEnd - item.meterReadingStart} kWh")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionModal(
    account: TransactionAccount,
    onDismiss: () -> Unit,
    onAdd: (TransactionItem) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        var isExpense by remember { mutableStateOf(true) }
        var category by remember { mutableStateOf("General") }
        var details by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }
        
        var startReadingStr by remember { mutableStateOf("") }
        var endReadingStr by remember { mutableStateOf("") }
        var perUnitChargeStr by remember { mutableStateOf("0.15") }
        
        val categories = listOf("General", "Electricity", "Groceries", "Salary", "Education")

        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Text("Add Transaction", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                FilterChip(
                    selected = !isExpense,
                    onClick = { isExpense = false },
                    label = { Text("Income") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC8E6C9))
                )
                Spacer(modifier = Modifier.width(16.dp))
                FilterChip(
                    selected = isExpense,
                    onClick = { isExpense = true },
                    label = { Text("Expense") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFCDD2))
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = { category = cat; expanded = false }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (category == "Electricity") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Electric Meter Utility", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = startReadingStr,
                                onValueChange = { startReadingStr = it },
                                label = { Text("Start Reading") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = endReadingStr,
                                onValueChange = { endReadingStr = it },
                                label = { Text("End Reading") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = perUnitChargeStr,
                            onValueChange = { perUnitChargeStr = it },
                            label = { Text("Flat Rate / Tier") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val startR = startReadingStr.toDoubleOrNull() ?: 0.0
                        val endR = endReadingStr.toDoubleOrNull() ?: 0.0
                        val rate = perUnitChargeStr.toDoubleOrNull() ?: 0.0
                        val consumed = (endR - startR).coerceAtLeast(0.0)
                        val calcTotal = consumed * rate
                        
                        Text("Consumed: $consumed units @ $$rate = $$calcTotal", color = MaterialTheme.colorScheme.primary)
                        
                        LaunchedEffect(calcTotal) {
                            if (calcTotal > 0) {
                                amountStr = calcTotal.toString()
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                label = { Text("Details / Note") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        val startR = startReadingStr.toDoubleOrNull()
                        val endR = endReadingStr.toDoubleOrNull()
                        val item = TransactionItem(
                            accountId = account.id,
                            timestamp = System.currentTimeMillis(),
                            category = category,
                            details = details,
                            amount = amt,
                            isExpense = isExpense,
                            attachmentDocId = null,
                            meterReadingStart = if (category == "Electricity") startR else null,
                            meterReadingEnd = if (category == "Electricity") endR else null,
                            meterRatesJson = null
                        )
                        onAdd(item)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Transaction")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
