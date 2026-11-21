package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.StolenVehicleEntity
import com.example.ui.components.AddVehicleDialog
import com.example.ui.components.LicensePlateBadge
import com.example.ui.components.PlateBadgeStyle
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.AlertCrimsonContainer
import com.example.ui.theme.IceWhite
import com.example.ui.theme.ObsidianBase
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SafeEmeraldContainer
import com.example.ui.theme.SlateMuted
import com.example.viewmodel.VehicleFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DatabaseScreen(
    vehicles: List<StolenVehicleEntity>,
    totalStolenCount: Int,
    totalRecoveredCount: Int,
    searchQuery: String,
    activeFilter: VehicleFilter,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (VehicleFilter) -> Unit,
    onAddVehicle: (String, String, String, String, String, String) -> Unit,
    onToggleRecovered: (StolenVehicleEntity) -> Unit,
    onDeleteVehicle: (StolenVehicleEntity) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var vehiclePendingDelete by remember { mutableStateOf<StolenVehicleEntity?>(null) }

    if (showAddDialog) {
        AddVehicleDialog(
            onDismiss = { showAddDialog = false },
            onSave = onAddVehicle
        )
    }

    vehiclePendingDelete?.let { pending ->
        AlertDialog(
            onDismissRequest = { vehiclePendingDelete = null },
            containerColor = ObsidianSurface,
            title = { Text("حذف بلاغ المركبة") },
            text = { Text("هل تريد حذف اللوحة (${pending.plateDisplay}) من قاعدة البيانات المحلية؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteVehicle(pending)
                        vehiclePendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson),
                    modifier = Modifier.testTag("confirm_delete_vehicle_button")
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { vehiclePendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

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
                Column {
                    Text(
                        text = stringResource(R.string.db_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = IceWhite
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = null,
                            tint = RadarCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.db_local_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMuted
                        )
                    }
                }
            }

            // Stats Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.db_stat_stolen),
                    count = totalStolenCount,
                    accentColor = AlertCrimson,
                    containerColor = AlertCrimsonContainer.copy(alpha = 0.45f),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.db_stat_recovered),
                    count = totalRecoveredCount,
                    accentColor = SafeEmerald,
                    containerColor = SafeEmeraldContainer.copy(alpha = 0.45f),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.db_search_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = RadarCyan
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.action_cancel),
                                tint = SlateMuted
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface,
                    focusedBorderColor = RadarCyan,
                    unfocusedBorderColor = ObsidianOutline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("db_search_input")
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeFilter == VehicleFilter.ALL,
                    onClick = { onFilterChange(VehicleFilter.ALL) },
                    label = { Text(stringResource(R.string.db_filter_all)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RadarCyan.copy(alpha = 0.2f),
                        selectedLabelColor = RadarCyan
                    ),
                    modifier = Modifier.testTag("filter_all_chip")
                )
                FilterChip(
                    selected = activeFilter == VehicleFilter.STOLEN,
                    onClick = { onFilterChange(VehicleFilter.STOLEN) },
                    label = { Text(stringResource(R.string.db_filter_stolen)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AlertCrimson.copy(alpha = 0.22f),
                        selectedLabelColor = AlertCrimson
                    ),
                    modifier = Modifier.testTag("filter_stolen_chip")
                )
                FilterChip(
                    selected = activeFilter == VehicleFilter.RECOVERED,
                    onClick = { onFilterChange(VehicleFilter.RECOVERED) },
                    label = { Text(stringResource(R.string.db_filter_recovered)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SafeEmerald.copy(alpha = 0.22f),
                        selectedLabelColor = SafeEmerald
                    ),
                    modifier = Modifier.testTag("filter_recovered_chip")
                )
            }

            if (vehicles.isEmpty()) {
                EmptyDatabaseState(
                    hasQueryOrFilter = searchQuery.isNotBlank() || activeFilter != VehicleFilter.ALL,
                    onAddClick = { showAddDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 4.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(vehicles, key = { it.id }) { vehicle ->
                        StolenVehicleCard(
                            vehicle = vehicle,
                            onToggleRecovered = { onToggleRecovered(vehicle) },
                            onDeleteRequest = { vehiclePendingDelete = vehicle }
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.db_add_vehicle), fontWeight = FontWeight.Bold) },
            containerColor = RadarCyan,
            contentColor = ObsidianBase,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
                .testTag("add_stolen_vehicle_fab")
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    count: Int,
    accentColor: androidx.compose.ui.graphics.Color,
    containerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = containerColor
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = IceWhite.copy(alpha = 0.88f)
            )
        }
    }
}

@Composable
private fun StolenVehicleCard(
    vehicle: StolenVehicleEntity,
    onToggleRecovered: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.US) }
    val dateString = remember(vehicle.reportedAt) { dateFormatter.format(Date(vehicle.reportedAt)) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (vehicle.isRecovered) SafeEmerald.copy(alpha = 0.35f) else AlertCrimson.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("vehicle_card_${vehicle.id}"),
        shape = RoundedCornerShape(20.dp),
        color = ObsidianSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LicensePlateBadge(
                    plateText = vehicle.plateDisplay,
                    style = if (vehicle.isRecovered) PlateBadgeStyle.RECOVERED else PlateBadgeStyle.STOLEN,
                    fontSize = 18.sp
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (vehicle.isRecovered) {
                        SafeEmeraldContainer
                    } else {
                        AlertCrimsonContainer
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (vehicle.isRecovered) Icons.Filled.CheckCircle else Icons.Filled.GppMaybe,
                            contentDescription = null,
                            tint = if (vehicle.isRecovered) SafeEmerald else AlertCrimson,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(
                                if (vehicle.isRecovered) R.string.db_filter_recovered
                                else R.string.db_filter_stolen
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (vehicle.isRecovered) SafeEmerald else AlertCrimson,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Vehicle details
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        tint = RadarCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    val headline = listOfNotNull(
                        vehicle.makeModel.takeIf { it.isNotBlank() },
                        vehicle.color.takeIf { it.isNotBlank() },
                        vehicle.year.takeIf { it.isNotBlank() }
                    ).joinToString(" • ")
                    Text(
                        text = headline.ifBlank { "مركبة بدون تفاصيل" },
                        style = MaterialTheme.typography.titleMedium,
                        color = IceWhite
                    )
                }

                if (vehicle.caseNumber.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tag,
                            contentDescription = null,
                            tint = SlateMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "رقم البلاغ: ${vehicle.caseNumber}  •  $dateString",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted
                        )
                    }
                } else {
                    Text(
                        text = "تاريخ التسجيل: $dateString",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted
                    )
                }

                if (vehicle.notes.isNotBlank()) {
                    Text(
                        text = vehicle.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = IceWhite.copy(alpha = 0.8f)
                    )
                }
            }

            HorizontalDivider(color = ObsidianSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onToggleRecovered,
                    modifier = Modifier.testTag("toggle_recovered_button_${vehicle.id}")
                ) {
                    Icon(
                        imageVector = if (vehicle.isRecovered) Icons.Filled.GppMaybe else Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (vehicle.isRecovered) AlertCrimson else SafeEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            if (vehicle.isRecovered) R.string.action_mark_stolen
                            else R.string.action_mark_recovered
                        ),
                        color = if (vehicle.isRecovered) AlertCrimson else SafeEmerald
                    )
                }

                IconButton(
                    onClick = onDeleteRequest,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_vehicle_button_${vehicle.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = SlateMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDatabaseState(
    hasQueryOrFilter: Boolean,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!hasQueryOrFilter) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, ObsidianOutline, RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_radar_patrol_1795251495890),
                    contentDescription = stringResource(R.string.db_empty_title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        Text(
            text = if (hasQueryOrFilter) "لا توجد نتائج مطابقة للبحث" else stringResource(R.string.db_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = IceWhite,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (hasQueryOrFilter) {
                "جرّب تغيير عبارة البحث أو إزالة الفلتر."
            } else {
                stringResource(R.string.db_empty_desc)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = SlateMuted,
            textAlign = TextAlign.Center
        )

        if (!hasQueryOrFilter) {
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("empty_add_vehicle_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.db_add_vehicle))
            }
        }
    }
}
