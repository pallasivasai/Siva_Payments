package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.TransactionRepository
import com.example.ui.HomeScreen
import com.example.ui.PaymentViewModel
import com.example.ui.ViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialise local SQLite database
        val database = AppDatabase.getDatabase(this)

        // 2. Set up transaction repository
        val repository = TransactionRepository(
            transactionDao = database.transactionDao,
            userAccountDao = database.userAccountDao
        )

        // 3. Instantiate PaymentViewModel using factory
        val factory = ViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[PaymentViewModel::class.java]

        setContent {
            MyApplicationTheme {
                HomeScreen(viewModel = viewModel)
            }
        }
    }
}
