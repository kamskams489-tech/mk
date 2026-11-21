package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.DetectionSource
import com.example.data.StolenVehicleEntity

class StolenAlertNotifier(private val context: Context) {

    init {
        ensureNotificationChannel()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing != null) return

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 280L, 140L, 380L)
                enableLights(true)
                setSound(soundUri, audioAttributes)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun notifyStolenMatch(
        vehicle: StolenVehicleEntity,
        source: DetectionSource,
        playAudioAlert: Boolean
    ) {
        triggerHapticAlarm()
        if (playAudioAlert) {
            playBeepAlert()
        }

        if (!hasNotificationPermission()) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted; skipping system notification")
            return
        }

        val sourceLabel = when (source) {
            DetectionSource.CAMERA -> context.getString(R.string.source_camera)
            DetectionSource.PHOTO -> context.getString(R.string.source_photo)
            DetectionSource.MANUAL -> context.getString(R.string.source_manual)
        }

        val vehicleDetails = listOfNotNull(
            vehicle.makeModel.takeIf { it.isNotBlank() },
            vehicle.color.takeIf { it.isNotBlank() },
            vehicle.year.takeIf { it.isNotBlank() }
        ).joinToString(" • ")

        val bodyText = buildString {
            append("اللوحة: ${vehicle.plateDisplay}")
            if (vehicleDetails.isNotBlank()) {
                append("\nالمركبة: $vehicleDetails")
            }
            if (vehicle.caseNumber.isNotBlank()) {
                append("\nرقم البلاغ: ${vehicle.caseNumber}")
            }
            append("\nالمصدر: $sourceLabel")
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TAB, TAB_ALERTS)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            vehicle.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notification_match_title))
            .setContentText("تطابق اللوحة ${vehicle.plateDisplay} — $vehicleDetails")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0L, 280L, 140L, 380L))
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context)
                .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        }.onFailure { e ->
            Log.e(TAG, "Failed to post stolen vehicle notification", e)
        }
    }

    fun sendTestNotification(): Boolean {
        if (!hasNotificationPermission()) return false
        triggerHapticAlarm()

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TAB, TAB_ALERTS)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9991,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("راصد المركبات — نظام الإشعارات جاهز")
            .setContentText("ستصلك تنبيهات فورية عالية الأولوية فور تطابق أي لوحة مسروقة.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return runCatching {
            NotificationManagerCompat.from(context).notify(9991, notification)
            true
        }.getOrDefault(false)
    }

    private fun triggerHapticAlarm() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0L, 220L, 100L, 320L)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0L, 220L, 100L, 320L), -1)
            }
        }
    }

    private fun playBeepAlert() {
        runCatching {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95)
            toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 320)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                { runCatching { toneGen.release() } },
                500L
            )
        }
    }

    companion object {
        private const val TAG = "StolenAlertNotifier"
        const val CHANNEL_ID = "stolen_vehicle_alerts_v1"
        const val EXTRA_OPEN_TAB = "extra_open_tab"
        const val TAB_ALERTS = "alerts"
    }
}
