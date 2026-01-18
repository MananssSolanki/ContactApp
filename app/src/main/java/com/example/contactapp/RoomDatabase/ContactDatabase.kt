package com.example.contactapp.RoomDatabase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.contactapp.Dao.ContactDao

@Database(entities = [Contact::class] , version = 1)
abstract class ContactDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao

    companion object{
        @Volatile private var INSTANCE : ContactDatabase?= null

        fun getDatabase(context: Context) : ContactDatabase{
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    ContactDatabase::class.java,
                    "contact_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}