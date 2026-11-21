package com.example.data

import com.example.plate.PlateNormalizer
import kotlinx.coroutines.flow.Flow

class RasedRepository(private val dao: RasedDao) {

    val allVehicles: Flow<List<StolenVehicleEntity>> = dao.observeAllVehicles()
    val activeStolenVehicles: Flow<List<StolenVehicleEntity>> = dao.observeActiveStolenVehicles()
    val allAlerts: Flow<List<SightingAlertEntity>> = dao.observeAllAlerts()
    val unreadAlertCount: Flow<Int> = dao.observeUnreadAlertCount()

    suspend fun addVehicle(
        plateRaw: String,
        makeModel: String,
        color: String,
        year: String,
        caseNumber: String,
        notes: String
    ): Long {
        val display = PlateNormalizer.formatForDisplay(plateRaw)
        val normalized = PlateNormalizer.normalizePlate(plateRaw)
        val digits = PlateNormalizer.extractDigitsOnly(plateRaw)
        return dao.insertVehicle(
            StolenVehicleEntity(
                plateDisplay = display,
                plateNormalized = normalized,
                plateDigits = digits,
                makeModel = makeModel.trim(),
                color = color.trim(),
                year = PlateNormalizer.normalizeDigits(year.trim()),
                caseNumber = PlateNormalizer.normalizeDigits(caseNumber.trim()),
                notes = notes.trim(),
                isRecovered = false,
                reportedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun setVehicleRecovered(vehicleId: Long, recovered: Boolean) {
        dao.setVehicleRecovered(vehicleId, recovered)
    }

    suspend fun deleteVehicle(vehicleId: Long) {
        dao.deleteVehicle(vehicleId)
    }

    suspend fun logSightingAlert(
        vehicle: StolenVehicleEntity,
        detectedRawText: String,
        source: DetectionSource
    ): Long {
        val summaryParts = listOfNotNull(
            vehicle.makeModel.takeIf { it.isNotBlank() },
            vehicle.color.takeIf { it.isNotBlank() },
            vehicle.year.takeIf { it.isNotBlank() }
        )
        val summary = summaryParts.joinToString(" • ").ifBlank { "مركبة مسجلة" }
        return dao.insertAlert(
            SightingAlertEntity(
                stolenVehicleId = vehicle.id,
                matchedPlate = vehicle.plateDisplay,
                detectedRawText = detectedRawText.take(64),
                vehicleSummary = summary,
                caseNumber = vehicle.caseNumber,
                source = source.name,
                detectedAt = System.currentTimeMillis(),
                isAcknowledged = false
            )
        )
    }

    suspend fun acknowledgeAlert(alertId: Long) = dao.acknowledgeAlert(alertId)

    suspend fun acknowledgeAllAlerts() = dao.acknowledgeAllAlerts()

    suspend fun clearAllAlerts() = dao.clearAllAlerts()
}
