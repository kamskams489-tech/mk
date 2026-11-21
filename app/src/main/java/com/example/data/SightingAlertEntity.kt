package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DetectionSource {
    CAMERA,
    PHOTO,
    MANUAL
}

@Entity(tableName = "sighting_alerts")
data class SightingAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val stolenVehicleId: Long,
    val matchedPlate: String,
    val detectedRawText: String,
    val vehicleSummary: String,
    val caseNumber: String,
    val source: String = DetectionSource.CAMERA.name,
    val detectedAt: Long = System.currentTimeMillis(),
    val isAcknowledged: Boolean = false
)
