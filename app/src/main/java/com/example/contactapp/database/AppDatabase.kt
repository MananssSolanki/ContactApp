package com.example.contactapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BlockedNumberEntity::class,
          // ← ADD your other entities here
    ],
    version = 4,                      // ← bumped because blocked_numbers table is new
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun blockedNumberDao(): BlockedNumberDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS deleted_contacts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        contactId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        phoneNumber TEXT NOT NULL,
                        email TEXT,
                        photoUri TEXT,
                        accountName TEXT,
                        accountType TEXT,
                        photoBytesBase64 TEXT,
                        deletedAt INTEGER NOT NULL,
                        vCardData TEXT NOT NULL DEFAULT ''
                    )"""
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE deleted_contacts ADD COLUMN accountName TEXT")
                database.execSQL("ALTER TABLE deleted_contacts ADD COLUMN accountType TEXT")
                database.execSQL("ALTER TABLE deleted_contacts ADD COLUMN photoBytesBase64 TEXT")
            }
        }

        // ← NEW: creates the blocked_numbers table for existing users upgrading from v3
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS blocked_numbers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        phone_number TEXT NOT NULL
                    )"""
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "contact_app_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4) // ← added MIGRATION_3_4
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}