package com.example.contactapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BlockedNumberDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: BlockedNumberEntity): Long

    @Query("DELETE FROM blocked_numbers WHERE phone_number = :phoneNumber")
    suspend fun deleteByPhoneNumber(phoneNumber: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE phone_number = :phoneNumber)")
    suspend fun isBlocked(phoneNumber: String): Boolean
}
