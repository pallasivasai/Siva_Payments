package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Transaction
import com.example.data.TransactionRepository
import com.example.data.UserAccount
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    Login,
    SignUp,
    Home
}

class PaymentViewModel(private val repository: TransactionRepository) : ViewModel() {
    
    private val _currentScreen = MutableStateFlow(AppScreen.Login)
    val currentScreen = _currentScreen.asStateFlow()

    private val _loggedInUsername = MutableStateFlow<String?>(null)
    val loggedInUsername = _loggedInUsername.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userAccount: StateFlow<UserAccount?> = _loggedInUsername
        .flatMapLatest { username ->
            if (username == null) {
                flowOf(null)
            } else {
                repository.getUserAccountFlow(username)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<Transaction>> = _loggedInUsername
        .flatMapLatest { username ->
            if (username == null) {
                flowOf(emptyList())
            } else {
                repository.getAllTransactionsForUser(username)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTime = MutableStateFlow(System.currentTimeMillis())
    val currentTime = _currentTime.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage = _successMessage.asStateFlow()

    init {
        // Automatically seed the fallback 'test' user with 1234 credentials
        viewModelScope.launch {
            val testUser = repository.getAccount("test")
            if (testUser == null) {
                repository.updateAccount(
                    UserAccount(
                        username = "test",
                        password = "1234",
                        balance = 7500.0,
                        isTestAccount = true
                    )
                )
                // Seed initial transaction items to demonstrate the interactive countdown out of the box
                repository.insertTransaction(
                    Transaction(
                        username = "test",
                        amount = 4500.0,
                        recipientName = "Aria Chen",
                        recipientUpi = "ariachen@axl",
                        timestamp = System.currentTimeMillis() - 2 * 60 * 60 * 1000, // 2 hours ago (completed)
                        status = "COMPLETED",
                        isIncoming = true
                    )
                )
                repository.insertTransaction(
                    Transaction(
                        username = "test",
                        amount = 1250.0,
                        recipientName = "Star Cafe & Bakery",
                        recipientUpi = "starcafe@okicici",
                        timestamp = System.currentTimeMillis() - 2 * 60 * 1000, // 2 mins ago (frozen)
                        status = "FROZEN",
                        isIncoming = false
                    )
                )
            }
        }

        // Live automatic escrow transitions
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val now = System.currentTimeMillis()
                _currentTime.value = now
                checkAndExpireTransactions(now)
            }
        }
    }

    private suspend fun checkAndExpireTransactions(now: Long) {
        val activeTransactions = transactions.value
        activeTransactions.filter { it.status == "FROZEN" }.forEach { tx ->
            val elapsed = now - tx.timestamp
            val fifteenMinutes = 15 * 60 * 1000
            if (elapsed >= fifteenMinutes) {
                val updatedTx = tx.copy(status = "COMPLETED")
                repository.updateTransaction(updatedTx)
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun login(usernameInput: String, passwordInput: String) {
        if (usernameInput.isBlank()) {
            _errorMessage.value = "Username cannot be empty!"
            return
        }
        if (passwordInput.isBlank()) {
            _errorMessage.value = "Password cannot be empty!"
            return
        }
        val trimmed = usernameInput.trim().lowercase()
        viewModelScope.launch {
            val account = repository.getAccount(trimmed)
            if (account == null) {
                _errorMessage.value = "Account does not exist. Please Sign Up!"
            } else if (account.password != passwordInput) {
                _errorMessage.value = "Incorrect password! Try again."
            } else {
                _loggedInUsername.value = trimmed
                _currentScreen.value = AppScreen.Home
                _successMessage.value = "Logged in successfully as $trimmed!"
            }
        }
    }

    fun signUp(usernameInput: String, passwordInput: String, confirmPasswordInput: String) {
        if (usernameInput.isBlank()) {
            _errorMessage.value = "Username cannot be empty!"
            return
        }
        if (passwordInput.length < 4) {
            _errorMessage.value = "Password must be at least 4 characters."
            return
        }
        if (passwordInput != confirmPasswordInput) {
            _errorMessage.value = "Passwords do not match!"
            return
        }
        val trimmed = usernameInput.trim().lowercase()
        viewModelScope.launch {
            val existing = repository.getAccount(trimmed)
            if (existing != null) {
                _errorMessage.value = "Username is already taken!"
                return@launch
            }
            
            // New user account starts with 0.0 balance per core specification
            val newAccount = UserAccount(
                username = trimmed,
                password = passwordInput,
                balance = 0.0,
                isTestAccount = trimmed == "test"
            )
            repository.updateAccount(newAccount)
            _loggedInUsername.value = trimmed
            _currentScreen.value = AppScreen.Home
            _successMessage.value = "Registered successfully! Balance starts at ₹0.00."
        }
    }

    fun logout() {
        _loggedInUsername.value = null
        _currentScreen.value = AppScreen.Login
        _successMessage.value = "Logged out successfully."
    }

    fun navigateToSignUp() {
        _currentScreen.value = AppScreen.SignUp
    }

    fun navigateToLogin() {
        _currentScreen.value = AppScreen.Login
    }

    fun makePayment(name: String, upiId: String, amount: Double) {
        val user = _loggedInUsername.value ?: return
        if (name.isBlank() || upiId.isBlank() || amount <= 0) {
            _errorMessage.value = "Enter a valid recipient name, UPI ID, and amount."
            return
        }

        viewModelScope.launch {
            val account = repository.getAccount(user) ?: return@launch
            if (account.balance < amount) {
                _errorMessage.value = "Insufficient bank balance! Please add funds to proceed."
                return@launch
            }

            // Deduct immediately on payment initiation
            val newAccount = account.copy(balance = account.balance - amount)
            repository.updateAccount(newAccount)

            // Save the transaction as FROZEN (holding window of 15 minutes)
            val newTx = Transaction(
                username = user,
                amount = amount,
                recipientName = name,
                recipientUpi = upiId,
                timestamp = System.currentTimeMillis(),
                status = "FROZEN",
                isIncoming = false
            )
            repository.insertTransaction(newTx)
            _successMessage.value = "Transaction initiated securely! ₹$amount is frozen in Escrow."
        }
    }

    fun receiveDummyPayment(senderName: String, senderUpi: String, amount: Double) {
        val user = _loggedInUsername.value ?: return
        if (amount <= 0) return
        viewModelScope.launch {
            val account = repository.getAccount(user) ?: return@launch
            val newAccount = account.copy(balance = account.balance + amount)
            repository.updateAccount(newAccount)

            val newTx = Transaction(
                username = user,
                amount = amount,
                recipientName = senderName,
                recipientUpi = senderUpi,
                timestamp = System.currentTimeMillis(),
                status = "COMPLETED",
                isIncoming = true
            )
            repository.insertTransaction(newTx)
            _successMessage.value = "Successfully received ₹$amount from $senderName!"
        }
    }

    fun reversePayment(tx: Transaction) {
        if (tx.status != "FROZEN") return
        val user = _loggedInUsername.value ?: return
        viewModelScope.launch {
            // Update transaction table status to REVERSED
            val updatedTx = tx.copy(status = "REVERSED")
            repository.updateTransaction(updatedTx)

            // Add the money back seamlessly to user balance
            val account = repository.getAccount(user) ?: return@launch
            val newAccount = account.copy(balance = account.balance + tx.amount)
            repository.updateAccount(newAccount)

            _successMessage.value = "Mistake corrected! ₹${tx.amount} returned immediately to your account."
        }
    }

    fun depositFunds(amount: Double) {
        val user = _loggedInUsername.value ?: return
        if (amount <= 0) return
        viewModelScope.launch {
            val account = repository.getAccount(user) ?: return@launch
            val newAccount = account.copy(balance = account.balance + amount)
            repository.updateAccount(newAccount)

            val newTx = Transaction(
                username = user,
                amount = amount,
                recipientName = "Deposit from Linked Bank",
                recipientUpi = "bank@deposit",
                timestamp = System.currentTimeMillis(),
                status = "COMPLETED",
                isIncoming = true
            )
            repository.insertTransaction(newTx)
            _successMessage.value = "Successfully deposited ₹$amount from Linked Bank!"
        }
    }

    fun withdrawFunds(amount: Double) {
        val user = _loggedInUsername.value ?: return
        if (amount <= 0) return
        viewModelScope.launch {
            val account = repository.getAccount(user) ?: return@launch
            if (account.balance < amount) {
                _errorMessage.value = "Insufficient balance to withdraw ₹$amount!"
                return@launch
            }
            val newAccount = account.copy(balance = account.balance - amount)
            repository.updateAccount(newAccount)

            val newTx = Transaction(
                username = user,
                amount = amount,
                recipientName = "Withdrawal to Bank Account",
                recipientUpi = "bank@withdrawal",
                timestamp = System.currentTimeMillis(),
                status = "COMPLETED",
                isIncoming = false
            )
            repository.insertTransaction(newTx)
            _successMessage.value = "Successfully withdrew ₹$amount to Linked Bank!"
        }
    }

    fun fastForwardTime(minutes: Int) {
        viewModelScope.launch {
            val offsetMs = minutes * 60 * 1000L
            val activeTransactions = transactions.value
            activeTransactions.filter { it.status == "FROZEN" }.forEach { tx ->
                val adjustedTx = tx.copy(timestamp = tx.timestamp - offsetMs)
                repository.updateTransaction(adjustedTx)
            }
            // Trigger instant evaluation on adjusted timestamps
            val nowUpdated = System.currentTimeMillis()
            _currentTime.value = nowUpdated
            checkAndExpireTransactions(nowUpdated)
        }
    }

    fun forceFinishPayment(tx: Transaction) {
        if (tx.status != "FROZEN") return
        viewModelScope.launch {
            val updatedTx = tx.copy(status = "COMPLETED")
            repository.updateTransaction(updatedTx)
            _successMessage.value = "Transaction cleared: Settled successfully!"
        }
    }

    fun resetDemoState() {
        val user = _loggedInUsername.value ?: return
        viewModelScope.launch {
            val account = repository.getAccount(user) ?: return@launch
            val defaultBalance = if (account.isTestAccount) 7500.0 else 0.0
            repository.updateAccount(account.copy(balance = defaultBalance))
            _successMessage.value = "Account balance reset to ₹$defaultBalance."
        }
    }
}

class ViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaymentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PaymentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
