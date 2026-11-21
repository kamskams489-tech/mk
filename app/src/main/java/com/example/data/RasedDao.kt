package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RasedDao {

    @Query("SELECT * FROM stolen_vehicles ORDER BY isRecovered ASC, reportedAt DESC")
    fun observeAllVehicles(): Flow<List<StolenVehicleEntity>>

    @Query("SELECT * FROM stolen_vehicles WHERE isRecovered = 0 ORDER BY reportedAt DESC")
    fun observeActiveStolenVehicles(): Flow<List<StolenVehicleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: StolenVehicleEntity): Long

    @Query("UPDATE stolen_vehicles SET isRecovered = :recovered WHERE id = :vehicleId")
    suspend fun setVehicleRecovered(vehicleId: Long, recovered: Boolean)

    @Query("DELETE FROM stolen_vehicles WHERE id = :vehicleId")
    suspend fun deleteVehicle(vehicleId: Long)

    @Query("SELECT * FROM sighting_alerts ORDER BY detectedAt DESC")
    fun observeAllAlerts(): Flow<List<SightingAlertEntity>>

    @Query("SELECT COUNT(*) FROM sighting_alerts WHERE isAcknowledged = 0")
    fun observeUnreadAlertCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: SightingAlertEntity): Long

    @Query("UPDATE sighting_alerts SET isAcknowledged = 1 WHERE id = :alertId")
    suspend fun acknowledgeAlert(alertId: Long)

    @Query("UPDATE sighting_alerts SET isAcknowledged = 1")
    suspend fun acknowledgeAllAlerts()

    @Query("DELETE FROM sighting_alerts")
    suspend fun clearAllAlerts()
}
