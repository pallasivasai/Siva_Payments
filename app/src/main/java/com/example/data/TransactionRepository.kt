package com.example.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val userAccountDao: UserAccountDao
) {
    fun getAllTransactionsForUser(username: String): Flow<List<Transaction>> {
        return transactionDao.getAllTransactionsForUser(username)
    }

    fun getUserAccountFlow(username: String): Flow<UserAccount?> {
        return userAccountDao.getUserAccountFlow(username)
    }

    suspend fun getAccount(username: String): UserAccount? {
        return userAccountDao.getUserAccount(username)
    }

    suspend fun updateAccount(account: UserAccount) {
        userAccountDao.insertOrUpdateAccount(account)
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun getTransactionById(id: Int): Transaction? {
        return transactionDao.getTransactionById(id)
    }

    suspend fun getAnyTransaction(): Transaction? {
        return transactionDao.getAnyTransaction()
    }
}
