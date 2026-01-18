package com.example.contactapp.Dao

import androidx.lifecycle.LiveData
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.contactapp.RoomDatabase.Contact

interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contactDao: Contact)

    @Query("select *from contact order by name asc")
    fun getAllContacts(): LiveData<List<Contact>>

    @Delete()
    suspend fun delete(contact : Contact)

}