package com.paracare.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Insert suspend fun insert(o: Order): Long
    @Update suspend fun update(o: Order)

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun allFlow(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE clientId = :id ORDER BY createdAt DESC")
    fun forClientFlow(id: Long): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE clientId = :id")
    suspend fun forClient(id: Long): List<Order>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): Order?
}
