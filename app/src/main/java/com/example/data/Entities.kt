package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String = "test",
    val amount: Double,
    val recipientName: String,
    val recipientUpi: String,
    val timestamp: Long,
    val status: String, // "FROZEN", "COMPLETED", "REVERSED"
    val isIncoming: Boolean = false
)

@Entity(tableName = "user_account")
data class UserAccount(
    @PrimaryKey val username: String,
    val password: String,
    val balance: Double,
    val isTestAccount: Boolean = false
)
