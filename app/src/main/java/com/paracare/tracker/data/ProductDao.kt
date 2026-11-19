package com.paracare.tracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert suspend fun insert(p: Product): Long
    @Insert suspend fun insertAll(list: List<Product>)
    @Update suspend fun update(p: Product)
    @Delete suspend fun delete(p: Product)

    @Query("SELECT * FROM products ORDER BY expiryDate ASC")
    fun allFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products")
    suspend fun getAll(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): Product?

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int
}
