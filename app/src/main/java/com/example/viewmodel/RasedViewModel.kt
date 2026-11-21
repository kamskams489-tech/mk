package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DetectionSource
import com.example.data.RasedDatabase
import com.example.data.RasedRepository
import com.example.data.SightingAlertEntity
import com.example.data.StolenVehicleEntity
import com.example.notifications.StolenAlertNotifier
import com.example.plate.PlateImageAnalyzer
import com.example.plate.PlateNormalizer
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class VehicleFilter {
    ALL,
    STOLEN,
    RECOVERED
}

data class ActiveMatchAlert(
    val vehicle: StolenVehicleEntity,
    val detectedText: String,
    val source: DetectionSource,
    val detectedAt: Long = System.currentTimeMillis()
)

data class ScannerUiState(
    val isScanningActive: Boolean = true,
    val isTorchEnabled: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val zoomRatio: Float = 1.0f,
    val isAnalyzingPhoto: Boolean = false,
    val isCapturingFrame: Boolean = false,
    val framesAnalyzed: Long = 0L,
    val lastRawOcrSnippet: String? = null,
    val lastDetectedPlate: String? = null,
    val lastScanMatched: Boolean = false,
    val activeMatchAlert: ActiveMatchAlert? = null,
    val transientMessage: String? = null
)

class RasedViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RasedRepository(RasedDatabase.getInstance(application).rasedDao())
    val notifier = StolenAlertNotifier(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _vehicleFilter = MutableStateFlow(VehicleFilter.ALL)
    val vehicleFilter: StateFlow<VehicleFilter> = _vehicleFilter.asStateFlow()

    private val _scannerState = MutableStateFlow(ScannerUiState())
    val scannerState: StateFlow<ScannerUiState> = _scannerState.asStateFlow()

    val allVehicles: StateFlow<List<StolenVehicleEntity>> = repository.allVehicles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeStolenVehicles: StateFlow<List<StolenVehicleEntity>> = repository.activeStolenVehicles
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredVehicles: StateFlow<List<StolenVehicleEntity>> = combine(
        allVehicles,
        _searchQuery,
        _vehicleFilter
    ) { vehicles, query, filter ->
        val normalizedQuery = PlateNormalizer.normalizePlate(query)
        val trimmedQuery = query.trim().lowercase()
        vehicles.filter { vehicle ->
            val matchesFilter = when (filter) {
                VehicleFilter.ALL -> true
                VehicleFilter.STOLEN -> !vehicle.isRecovered
                VehicleFilter.RECOVERED -> vehicle.isRecovered
            }
            val matchesSearch = if (trimmedQuery.isEmpty()) {
                true
            } else {
                vehicle.plateDisplay.lowercase().contains(trimmedQuery) ||
                    (normalizedQuery.isNotEmpty() && vehicle.plateNormalized.contains(normalizedQuery)) ||
                    vehicle.makeModel.lowercase().contains(trimmedQuery) ||
                    vehicle.color.lowercase().contains(trimmedQuery) ||
                    vehicle.caseNumber.lowercase().contains(trimmedQuery)
            }
            matchesFilter && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlerts: StateFlow<List<SightingAlertEntity>> = repository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadAlertCount: StateFlow<Int> = repository.unreadAlertCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val lastCameraAlertByVehicleId = ConcurrentHashMap<Long, Long>()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateVehicleFilter(filter: VehicleFilter) {
        _vehicleFilter.value = filter
    }

    fun toggleScanning() {
        _scannerState.update { it.copy(isScanningActive = !it.isScanningActive) }
    }

    fun toggleTorch() {
        _scannerState.update { it.copy(isTorchEnabled = !it.isTorchEnabled) }
    }

    fun toggleSound() {
        _scannerState.update { it.copy(isSoundEnabled = !it.isSoundEnabled) }
    }

    fun toggleCameraLens() {
        _scannerState.update { state ->
            val nextLens = if (state.lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            state.copy(
                lensFacing = nextLens,
                isTorchEnabled = false,
                zoomRatio = 1.0f
            )
        }
    }

    fun setZoomRatio(ratio: Float) {
        _scannerState.update { it.copy(zoomRatio = ratio.coerceIn(1.0f, 4.0f)) }
    }

    fun onFrameAnalyzed() {
        _scannerState.update { it.copy(framesAnalyzed = it.framesAnalyzed + 1L) }
    }

    fun onCaptureStarted() {
        _scannerState.update { it.copy(isCapturingFrame = true) }
    }

    fun onCaptureFailed(message: String) {
        _scannerState.update {
            it.copy(
                isCapturingFrame = false,
                transientMessage = message
            )
        }
    }

    fun dismissActiveMatchAlert() {
        _scannerState.update { it.copy(activeMatchAlert = null) }
    }

    fun clearTransientMessage() {
        _scannerState.update { it.copy(transientMessage = null) }
    }

    fun addStolenVehicle(
        plateRaw: String,
        makeModel: String,
        color: String,
        year: String,
        caseNumber: String,
        notes: String
    ) {
        val normalized = PlateNormalizer.normalizePlate(plateRaw)
        if (normalized.length < 2) {
            _scannerState.update { it.copy(transientMessage = "يرجى إدخال رقم لوحة صحيح") }
            return
        }
        viewModelScope.launch {
            repository.addVehicle(
                plateRaw = plateRaw,
                makeModel = makeModel,
                color = color,
                year = year,
                caseNumber = caseNumber,
                notes = notes
            )
            _scannerState.update {
                it.copy(transientMessage = "تم حفظ البلاغ للوحة ${PlateNormalizer.formatForDisplay(plateRaw)}")
            }
        }
    }

    fun toggleVehicleRecovered(vehicle: StolenVehicleEntity) {
        viewModelScope.launch {
            repository.setVehicleRecovered(vehicle.id, !vehicle.isRecovered)
        }
    }

    fun deleteVehicle(vehicle: StolenVehicleEntity) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle.id)
        }
    }

    fun acknowledgeAlert(alertId: Long) {
        viewModelScope.launch {
            repository.acknowledgeAlert(alertId)
        }
    }

    fun acknowledgeAllAlerts() {
        viewModelScope.launch {
            repository.acknowledgeAllAlerts()
        }
    }

    fun clearAllAlerts() {
        viewModelScope.launch {
            repository.clearAllAlerts()
        }
    }

    fun sendTestNotification() {
        val sent = notifier.sendTestNotification()
        _scannerState.update {
            it.copy(
                transientMessage = if (sent) {
                    "تم إرسال إشعار تجريبي فوري بنجاح"
                } else {
                    "يرجى منح إذن الإشعارات أولاً لتجربة التنبيهات"
                }
            )
        }
    }

    /**
     * Called from CameraX ML Kit analyzer on live frames.
     */
    fun onCameraTextRecognized(rawText: String) {
        val snippet = rawText
            .replace(Regex("[\\r\\n]+"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
            .take(32)

        val stolenList = activeStolenVehicles.value
        val matchResult = PlateNormalizer.findMatchingVehicle(rawText, stolenList)

        if (matchResult != null) {
            val (vehicle, matchedToken) = matchResult
            val now = System.currentTimeMillis()
            val lastFired = lastCameraAlertByVehicleId[vehicle.id] ?: 0L
            _scannerState.update {
                it.copy(
                    lastRawOcrSnippet = snippet.ifBlank { null },
                    lastDetectedPlate = vehicle.plateDisplay,
                    lastScanMatched = true
                )
            }
            if (now - lastFired >= CAMERA_ALERT_COOLDOWN_MS) {
                lastCameraAlertByVehicleId[vehicle.id] = now
                triggerMatchAlert(vehicle, matchedToken, DetectionSource.CAMERA)
            }
            return
        }

        val candidates = PlateNormalizer.extractCandidates(rawText)
        val bestCandidate = candidates.firstOrNull()
        _scannerState.update { state ->
            state.copy(
                lastRawOcrSnippet = snippet.ifBlank { state.lastRawOcrSnippet },
                lastDetectedPlate = bestCandidate ?: state.lastDetectedPlate,
                lastScanMatched = if (bestCandidate != null) false else state.lastScanMatched
            )
        }
    }

    /**
     * Called when user taps the live shutter button to capture and inspect a single full frame.
     */
    fun onSingleShotCapturedText(rawText: String) {
        val snippet = rawText
            .replace(Regex("[\\r\\n]+"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
            .take(36)

        if (rawText.isBlank()) {
            _scannerState.update {
                it.copy(
                    isCapturingFrame = false,
                    transientMessage = "تم التقاط الإطار — لم يُرصد نص واضح للوحة أمام الكاميرا"
                )
            }
            return
        }

        val stolenList = activeStolenVehicles.value
        val matchResult = PlateNormalizer.findMatchingVehicle(rawText, stolenList)
        if (matchResult != null) {
            val (vehicle, matchedToken) = matchResult
            _scannerState.update {
                it.copy(
                    isCapturingFrame = false,
                    lastRawOcrSnippet = snippet,
                    lastDetectedPlate = vehicle.plateDisplay,
                    lastScanMatched = true
                )
            }
            triggerMatchAlert(vehicle, matchedToken, DetectionSource.CAMERA)
        } else {
            val bestCandidate = PlateNormalizer.extractCandidates(rawText).firstOrNull()
                ?: PlateNormalizer.normalizePlate(rawText).take(10).ifBlank { null }
            _scannerState.update {
                it.copy(
                    isCapturingFrame = false,
                    lastRawOcrSnippet = snippet,
                    lastDetectedPlate = bestCandidate ?: it.lastDetectedPlate,
                    lastScanMatched = false,
                    transientMessage = if (bestCandidate != null) {
                        "تم فحص ($bestCandidate) — غير مسجلة كمسروقة"
                    } else {
                        "القراءة المرصودة: $snippet (لا تطابق مركبة مسروقة)"
                    }
                )
            }
        }
    }

    /**
     * Scans a user-picked image from Android Photo Picker using on-device ML Kit OCR.
     */
    fun scanPhotoUri(uri: Uri) {
        viewModelScope.launch {
            _scannerState.update { it.copy(isAnalyzingPhoto = true) }
            try {
                val rawText = withContext(Dispatchers.IO) {
                    PlateImageAnalyzer.recognizeTextFromUri(getApplication(), uri)
                }
                if (rawText.isBlank()) {
                    _scannerState.update {
                        it.copy(
                            isAnalyzingPhoto = false,
                            transientMessage = "لم يتم العثور على نص واضح للوحة في هذه الصورة"
                        )
                    }
                    return@launch
                }

                val stolenList = activeStolenVehicles.value
                val matchResult = PlateNormalizer.findMatchingVehicle(rawText, stolenList)
                if (matchResult != null) {
                    val (vehicle, matchedToken) = matchResult
                    _scannerState.update {
                        it.copy(
                            isAnalyzingPhoto = false,
                            lastDetectedPlate = vehicle.plateDisplay,
                            lastScanMatched = true
                        )
                    }
                    triggerMatchAlert(vehicle, matchedToken, DetectionSource.PHOTO)
                } else {
                    val bestCandidate = PlateNormalizer.extractCandidates(rawText).firstOrNull()
                        ?: PlateNormalizer.normalizePlate(rawText).take(10).ifBlank { null }
                    _scannerState.update {
                        it.copy(
                            isAnalyzingPhoto = false,
                            lastDetectedPlate = bestCandidate,
                            lastScanMatched = false,
                            transientMessage = if (bestCandidate != null) {
                                "تم التعرف على ($bestCandidate) — غير مسجلة كمسروقة"
                            } else {
                                "لم يتم مطابقة أي لوحة مسروقة في الصورة"
                            }
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Photo OCR failed", e)
                _scannerState.update {
                    it.copy(
                        isAnalyzingPhoto = false,
                        transientMessage = "تعذر تحليل الصورة المختارة"
                    )
                }
            }
        }
    }

    /**
     * Runs the exact same normalization & alert pipeline for a manually entered plate number.
     */
    fun checkPlateManually(input: String) {
        val display = PlateNormalizer.formatForDisplay(input)
        val normalized = PlateNormalizer.normalizePlate(input)
        if (normalized.length < 2) {
            _scannerState.update { it.copy(transientMessage = "أدخل رقم لوحة صحيح للفحص") }
            return
        }

        val stolenList = activeStolenVehicles.value
        val matchResult = PlateNormalizer.findMatchingVehicle(input, stolenList)
        if (matchResult != null) {
            val (vehicle, _) = matchResult
            _scannerState.update {
                it.copy(
                    lastDetectedPlate = vehicle.plateDisplay,
                    lastScanMatched = true
                )
            }
            triggerMatchAlert(vehicle, display, DetectionSource.MANUAL)
        } else {
            _scannerState.update {
                it.copy(
                    lastDetectedPlate = display,
                    lastScanMatched = false,
                    transientMessage = "اللوحة ($display) سليمة وغير مسجلة في قائمة المسروقات"
                )
            }
        }
    }

    private fun triggerMatchAlert(
        vehicle: StolenVehicleEntity,
        detectedText: String,
        source: DetectionSource
    ) {
        val playSound = _scannerState.value.isSoundEnabled
        notifier.notifyStolenMatch(
            vehicle = vehicle,
            source = source,
            playAudioAlert = playSound
        )
        _scannerState.update {
            it.copy(
                activeMatchAlert = ActiveMatchAlert(
                    vehicle = vehicle,
                    detectedText = detectedText,
                    source = source
                )
            )
        }
        viewModelScope.launch {
            repository.logSightingAlert(
                vehicle = vehicle,
                detectedRawText = detectedText,
                source = source
            )
        }
    }

    companion object {
        private const val TAG = "RasedViewModel"
        private const val CAMERA_ALERT_COOLDOWN_MS = 12_000L
    }
}
