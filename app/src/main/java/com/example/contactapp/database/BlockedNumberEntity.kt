package com.example.contactapp.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_numbers")
data class BlockedNumberEntity(
    @PrimaryKey
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String
)
