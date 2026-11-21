package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.components.MatchAlertDialog
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.DatabaseScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.IceWhite
import com.example.ui.theme.ObsidianBase
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarCyanContainer
import com.example.ui.theme.SlateMuted
import com.example.viewmodel.RasedViewModel

enum class AppDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    SCANNER(
        route = "scanner",
        labelRes = R.string.nav_scanner,
        selectedIcon = Icons.Filled.Radar,
        unselectedIcon = Icons.Outlined.Radar,
        tag = "nav_tab_scanner"
    ),
    DATABASE(
        route = "database",
        labelRes = R.string.nav_database,
        selectedIcon = Icons.Filled.DirectionsCar,
        unselectedIcon = Icons.Outlined.DirectionsCar,
        tag = "nav_tab_database"
    ),
    ALERTS(
        route = "alerts",
        labelRes = R.string.nav_alerts,
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications,
        tag = "nav_tab_alerts"
    )
}

@Composable
fun RasedApp(
    initialDestination: AppDestination = AppDestination.SCANNER,
    externalDestinationTrigger: Int = 0,
    viewModel: RasedViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentDestination by rememberSaveable { mutableStateOf(initialDestination) }
    val snackbarHostState = remember { SnackbarHostState() }

    val allVehicles by viewModel.allVehicles.collectAsStateWithLifecycle()
    val filteredVehicles by viewModel.filteredVehicles.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeFilter by viewModel.vehicleFilter.collectAsStateWithLifecycle()
    val allAlerts by viewModel.allAlerts.collectAsStateWithLifecycle()
    val unreadAlertCount by viewModel.unreadAlertCount.collectAsStateWithLifecycle()
    val scannerState by viewModel.scannerState.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(viewModel.notifier.hasNotificationPermission())
    }
    var requestedInitialPermissions by rememberSaveable { mutableStateOf(false) }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCameraPermission = results[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        hasNotificationPermission = viewModel.notifier.hasNotificationPermission()
    }

    val cameraOnlyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    val notificationOnlyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted || viewModel.notifier.hasNotificationPermission()
    }

    // Re-check permissions immediately when returning from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission =
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                hasNotificationPermission = viewModel.notifier.hasNotificationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Auto-request CAMERA (and POST_NOTIFICATIONS on Android 13+) right on launch so the real camera opens immediately
    LaunchedEffect(Unit) {
        if (!requestedInitialPermissions) {
            requestedInitialPermissions = true
            val missing = buildList {
                if (!hasCameraPermission) add(Manifest.permission.CAMERA)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            if (missing.isNotEmpty()) {
                multiplePermissionsLauncher.launch(missing.toTypedArray())
            }
        }
    }

    LaunchedEffect(externalDestinationTrigger) {
        if (externalDestinationTrigger > 0) {
            currentDestination = initialDestination
        }
    }

    LaunchedEffect(scannerState.transientMessage) {
        val message = scannerState.transientMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearTransientMessage()
        }
    }

    // Back press on secondary screens returns smoothly to the Scanner tab
    BackHandler(enabled = currentDestination != AppDestination.SCANNER) {
        currentDestination = AppDestination.SCANNER
    }

    val totalStolenCount = remember(allVehicles) { allVehicles.count { !it.isRecovered } }
    val totalRecoveredCount = remember(allVehicles) { allVehicles.count { it.isRecovered } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        scannerState.activeMatchAlert?.let { activeAlert ->
            MatchAlertDialog(
                alert = activeAlert,
                onDismiss = viewModel::dismissActiveMatchAlert
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = ObsidianBase,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            containerColor = ObsidianSurface,
                            tonalElevation = 0.dp
                        ) {
                            AppDestination.entries.forEach { dest ->
                                val selected = currentDestination == dest
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentDestination = dest },
                                    modifier = Modifier.testTag(dest.tag),
                                    icon = {
                                        DestinationIcon(
                                            destination = dest,
                                            selected = selected,
                                            unreadAlerts = unreadAlertCount
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = RadarCyan,
                                        selectedTextColor = RadarCyan,
                                        indicatorColor = RadarCyanContainer,
                                        unselectedIconColor = SlateMuted,
                                        unselectedTextColor = SlateMuted
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = ObsidianSurface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            AppDestination.entries.forEach { dest ->
                                val selected = currentDestination == dest
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentDestination = dest },
                                    modifier = Modifier.testTag(dest.tag),
                                    icon = {
                                        DestinationIcon(
                                            destination = dest,
                                            selected = selected,
                                            unreadAlerts = unreadAlertCount
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = RadarCyan,
                                        selectedTextColor = RadarCyan,
                                        indicatorColor = RadarCyanContainer,
                                        unselectedIconColor = SlateMuted,
                                        unselectedTextColor = SlateMuted
                                    )
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        when (currentDestination) {
                            AppDestination.SCANNER -> {
                                ScannerScreen(
                                    scannerState = scannerState,
                                    hasCameraPermission = hasCameraPermission,
                                    activeStolenCount = totalStolenCount,
                                    onRequestCameraPermission = {
                                        cameraOnlyLauncher.launch(Manifest.permission.CAMERA)
                                    },
                                    onToggleScan = viewModel::toggleScanning,
                                    onToggleTorch = viewModel::toggleTorch,
                                    onToggleSound = viewModel::toggleSound,
                                    onSwitchCameraLens = viewModel::toggleCameraLens,
                                    onSelectZoom = viewModel::setZoomRatio,
                                    onFrameAnalyzed = viewModel::onFrameAnalyzed,
                                    onCameraTextRecognized = viewModel::onCameraTextRecognized,
                                    onCaptureStarted = viewModel::onCaptureStarted,
                                    onCaptureRecognized = viewModel::onSingleShotCapturedText,
                                    onCaptureFailed = viewModel::onCaptureFailed,
                                    onScanPhotoUri = viewModel::scanPhotoUri,
                                    onCheckPlateManually = viewModel::checkPlateManually,
                                    onOpenDatabase = { currentDestination = AppDestination.DATABASE }
                                )
                            }

                            AppDestination.DATABASE -> {
                                DatabaseScreen(
                                    vehicles = filteredVehicles,
                                    totalStolenCount = totalStolenCount,
                                    totalRecoveredCount = totalRecoveredCount,
                                    searchQuery = searchQuery,
                                    activeFilter = activeFilter,
                                    onSearchQueryChange = viewModel::updateSearchQuery,
                                    onFilterChange = viewModel::updateVehicleFilter,
                                    onAddVehicle = viewModel::addStolenVehicle,
                                    onToggleRecovered = viewModel::toggleVehicleRecovered,
                                    onDeleteVehicle = viewModel::deleteVehicle
                                )
                            }

                            AppDestination.ALERTS -> {
                                AlertsScreen(
                                    alerts = allAlerts,
                                    hasNotificationPermission = hasNotificationPermission,
                                    onRequestNotificationPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationOnlyLauncher.launch(
                                                Manifest.permission.POST_NOTIFICATIONS
                                            )
                                        }
                                    },
                                    onSendTestNotification = viewModel::sendTestNotification,
                                    onAcknowledgeAlert = viewModel::acknowledgeAlert,
                                    onAcknowledgeAll = viewModel::acknowledgeAllAlerts,
                                    onClearAll = viewModel::clearAllAlerts
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationIcon(
    destination: AppDestination,
    selected: Boolean,
    unreadAlerts: Int
) {
    val icon = if (selected) destination.selectedIcon else destination.unselectedIcon
    if (destination == AppDestination.ALERTS && unreadAlerts > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = AlertCrimson,
                    contentColor = IceWhite
                ) {
                    Text(if (unreadAlerts > 99) "99+" else unreadAlerts.toString())
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(destination.labelRes)
            )
        }
    } else {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(destination.labelRes)
        )
    }
}
