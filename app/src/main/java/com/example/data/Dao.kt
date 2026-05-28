package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE username = :username ORDER BY timestamp DESC")
    fun getAllTransactionsForUser(username: String): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?

    @Query("SELECT * FROM transactions LIMIT 1")
    suspend fun getAnyTransaction(): Transaction?
}

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_account WHERE username = :username")
    fun getUserAccountFlow(username: String): Flow<UserAccount?>

    @Query("SELECT * FROM user_account WHERE username = :username")
    suspend fun getUserAccount(username: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAccount(account: UserAccount)
}
