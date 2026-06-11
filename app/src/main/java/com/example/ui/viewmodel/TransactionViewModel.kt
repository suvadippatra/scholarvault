package com.scholarvault.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scholarvault.data.AppDatabase
import com.scholarvault.data.model.TransactionAccount
import com.scholarvault.data.model.TransactionItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TransactionViewModel(private val db: AppDatabase) : ViewModel() {
    private val dao = db.transactionLedgerDao()

    val accounts: StateFlow<List<TransactionAccount>> = dao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedAccountId = MutableStateFlow<Int?>(null)
    val selectedAccountId: StateFlow<Int?> = _selectedAccountId.asStateFlow()

    val currentAccount: StateFlow<TransactionAccount?> = combine(accounts, selectedAccountId) { accs, id ->
        if (id == null) accs.firstOrNull() else accs.find { it.id == id } ?: accs.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val transactions: StateFlow<List<TransactionItem>> = currentAccount.flatMapLatest { account ->
        if (account == null) flowOf(emptyList())
        else dao.getTransactionsForAccount(account.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectAccount(id: Int) {
        _selectedAccountId.value = id
    }

    fun addAccount(name: String, initialBalance: Double) {
        viewModelScope.launch {
            val account = TransactionAccount(name = name, initialBalance = initialBalance, currentBalance = initialBalance)
            val id = dao.insertAccount(account)
            if (_selectedAccountId.value == null) {
                _selectedAccountId.value = id.toInt()
            }
        }
    }

    fun deleteAccount(account: TransactionAccount) {
        viewModelScope.launch {
            dao.deleteAccount(account)
            if (_selectedAccountId.value == account.id) {
                _selectedAccountId.value = null
            }
        }
    }

    fun addTransaction(item: TransactionItem) {
        viewModelScope.launch {
            val account = currentAccount.value ?: return@launch
            dao.insertTransactionAndUpdateBalance(item, account)
        }
    }
}
