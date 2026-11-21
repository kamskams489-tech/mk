package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stolen_vehicles",
    indices = [Index(value = ["plateNormalized"])]
)
data class StolenVehicleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val plateDisplay: String,
    val plateNormalized: String,
    val plateDigits: String,
    val makeModel: String,
    val color: String,
    val year: String,
    val caseNumber: String,
    val notes: String,
    val isRecovered: Boolean = false,
    val reportedAt: Long = System.currentTimeMillis()
)
