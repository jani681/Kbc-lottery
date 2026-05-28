package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kbc_pranks")
data class KbcPrank(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val friendName: String,
    val friendNumber: String,
    val friendAddress: String,
    val senderName: String,
    val generatedUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)
