package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DetectionSource
import com.example.data.SightingAlertEntity
import com.example.ui.components.LicensePlateBadge
import com.example.ui.components.PlateBadgeStyle
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.IceWhite
import com.example.ui.theme.ObsidianBase
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalAmberContainer
import com.example.ui.theme.SlateMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlertsScreen(
    alerts: List<SightingAlertEntity>,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onSendTestNotification: () -> Unit,
    onAcknowledgeAlert: (Long) -> Unit,
    onAcknowledgeAll: () -> Unit,
    onClearAll: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBase)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .widthIn(max = 720.dp)
                .align(Alignment.TopCenter)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.alerts_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = IceWhite
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = onSendTestNotification,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("test_notification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_test_notification),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    if (alerts.any { !it.isAcknowledged }) {
                        IconButton(
                            onClick = onAcknowledgeAll,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("acknowledge_all_alerts_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                contentDescription = "تحديد الكل كمقروء",
                                tint = RadarCyan
                            )
                        }
                    }

                    if (alerts.isNotEmpty()) {
                        IconButton(
                            onClick = onClearAll,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("clear_all_alerts_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = stringResource(R.string.alerts_clear_all),
                                tint = SlateMuted
                            )
                        }
                    }
                }
            }

            // Notification permission prompt banner on Android 13+
            if (!hasNotificationPermission) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .border(1.dp, SignalAmber.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    color = SignalAmberContainer
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsOff,
                                contentDescription = null,
                                tint = SignalAmber
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.notification_permission_banner_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = IceWhite
                                )
                                Text(
                                    text = stringResource(R.string.notification_permission_banner_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IceWhite.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Button(
                            onClick = onRequestNotificationPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SignalAmber,
                                contentColor = ObsidianBase
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("grant_notifications_button")
                        ) {
                            Text(
                                text = stringResource(R.string.action_grant_notifications),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (alerts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(ObsidianSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = RadarCyan,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = stringResource(R.string.alerts_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = IceWhite,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.alerts_empty_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateMuted,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alerts, key = { it.id }) { alert ->
                        SightingAlertCard(
                            alert = alert,
                            onClick = { onAcknowledgeAlert(alert.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SightingAlertCard(
    alert: SightingAlertEntity,
    onClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy • HH:mm:ss", Locale.US) }
    val timeText = remember(alert.detectedAt) { formatter.format(Date(alert.detectedAt)) }

    val sourceEnum = remember(alert.source) {
        DetectionSource.entries.firstOrNull { it.name == alert.source } ?: DetectionSource.CAMERA
    }
    val (sourceIcon, sourceLabel) = when (sourceEnum) {
        DetectionSource.CAMERA -> Icons.Filled.CameraAlt to stringResource(R.string.source_camera)
        DetectionSource.PHOTO -> Icons.Filled.ImageSearch to stringResource(R.string.source_photo)
        DetectionSource.MANUAL -> Icons.Filled.Keyboard to stringResource(R.string.source_manual)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (alert.isAcknowledged) 1.dp else 1.5.dp,
                color = if (alert.isAcknowledged) ObsidianSurfaceVariant else AlertCrimson.copy(alpha = 0.75f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .testTag("alert_card_${alert.id}"),
        shape = RoundedCornerShape(20.dp),
        color = ObsidianSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!alert.isAcknowledged) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(AlertCrimson)
                        )
                    }
                    LicensePlateBadge(
                        plateText = alert.matchedPlate,
                        style = PlateBadgeStyle.STOLEN,
                        fontSize = 17.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = ObsidianSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = sourceIcon,
                            contentDescription = null,
                            tint = RadarCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = sourceLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = IceWhite
                        )
                    }
                }
            }

            Text(
                text = alert.vehicleSummary,
                style = MaterialTheme.typography.titleMedium,
                color = IceWhite
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (alert.caseNumber.isNotBlank()) "بلاغ #${alert.caseNumber}" else "تطابق مباشر",
                    style = MaterialTheme.typography.bodySmall,
                    color = AlertCrimson
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateMuted
                )
            }
        }
    }
}
