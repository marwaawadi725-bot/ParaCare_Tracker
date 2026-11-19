package com.paracare.tracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserItemDao {
    @Insert suspend fun insert(i: UserItem): Long
    @Update suspend fun update(i: UserItem)
    @Delete suspend fun delete(i: UserItem)

    @Query("SELECT * FROM user_items WHERE userId = :uid ORDER BY id DESC")
    fun itemsFlow(uid: Long): Flow<List<UserItem>>

    @Query("SELECT * FROM user_items WHERE userId = :uid")
    suspend fun getForUser(uid: Long): List<UserItem>
}
