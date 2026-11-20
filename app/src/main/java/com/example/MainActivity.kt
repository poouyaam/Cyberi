package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.CyberGlassBackground
import com.example.ui.components.MiniAudioPlayerBar
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RecordingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberDeepNavy
import com.example.ui.theme.CyberVoid
import com.example.ui.theme.CyberiTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CyberiViewModel
import com.example.util.DeviceCompatibilityManager

sealed class CyberiDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : CyberiDestination("home", "خانه", Icons.Rounded.Home)
    data object Recordings : CyberiDestination("recordings", "مکالمات", Icons.Rounded.Folder)
    data object Settings : CyberiDestination("settings", "تنظیمات", Icons.Rounded.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CyberiTheme {
                CyberiAppRoot()
            }
        }
    }
}

@Composable
fun CyberiAppRoot(
    viewModel: CyberiViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val liveState by viewModel.liveRecorderState.collectAsStateWithLifecycle()
    val deviceStatus by viewModel.deviceStatus.collectAsStateWithLifecycle()
    val allRecordings by viewModel.allRecordings.collectAsStateWithLifecycle()
    val filteredRecordings by viewModel.filteredRecordings.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    var autoRequestedOnLaunch by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshDeviceStatusAndSyncService()
    }

    val requestAllPermissions: () -> Unit = {
        permissionLauncher.launch(DeviceCompatibilityManager.requiredPermissions())
    }

    // Automatically trigger smart permission matching on first install/launch
    LaunchedEffect(Unit) {
        if (!autoRequestedOnLaunch) {
            autoRequestedOnLaunch = true
            if (!deviceStatus.allRecommendedReady) {
                requestAllPermissions()
            } else {
                viewModel.refreshDeviceStatusAndSyncService()
            }
        }
    }

    // Re-sync device status & monitoring service whenever returning from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDeviceStatusAndSyncService()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: CyberiDestination.Home.route
    val destinations = listOf(
        CyberiDestination.Home,
        CyberiDestination.Recordings,
        CyberiDestination.Settings
    )

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    CyberGlassBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 700.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!useNavigationRail) {
                        Column(
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            MiniAudioPlayerBar(
                                playbackState = playbackState,
                                onTogglePlay = {
                                    allRecordings.firstOrNull { it.id == playbackState.activeRecordingId }?.let {
                                        viewModel.togglePlay(it)
                                    }
                                },
                                onSeek = viewModel::seekTo,
                                onCycleSpeed = viewModel::cyclePlaybackSpeed,
                                onClose = viewModel::stopPlayback
                            )
                            NavigationBar(
                                containerColor = CyberDeepNavy.copy(alpha = 0.88f),
                                tonalElevation = 0.dp
                            ) {
                                destinations.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTo(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = dest.icon,
                                                contentDescription = dest.label
                                            )
                                        },
                                        label = { Text(dest.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = CyberVoid,
                                            selectedTextColor = CyberCyan,
                                            indicatorColor = CyberCyan,
                                            unselectedIconColor = TextSecondary,
                                            unselectedTextColor = TextSecondary
                                        ),
                                        modifier = Modifier.testTag("nav_${dest.route}")
                                    )
                                }
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
                            containerColor = CyberDeepNavy.copy(alpha = 0.75f)
                        ) {
                            destinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = dest.icon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = CyberVoid,
                                        selectedTextColor = CyberCyan,
                                        indicatorColor = CyberCyan,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.testTag("rail_${dest.route}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = CyberiDestination.Home.route
                        ) {
                            composable(CyberiDestination.Home.route) {
                                HomeScreen(
                                    settings = settings,
                                    liveState = liveState,
                                    deviceStatus = deviceStatus,
                                    recentRecordings = allRecordings,
                                    playbackState = playbackState,
                                    onRequestPermissions = requestAllPermissions,
                                    onOpenBatterySettings = {
                                        DeviceCompatibilityManager.openBatteryOptimizationSettings(context)
                                    },
                                    onToggleAutoRecord = viewModel::toggleAutoRecord,
                                    onToggleManualRecord = viewModel::toggleManualRecording,
                                    onTogglePlay = viewModel::togglePlay,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onRenameRecording = viewModel::renameRecording,
                                    onShareRecording = viewModel::shareRecording,
                                    onExportRecording = viewModel::exportToMusic,
                                    onDeleteRecording = viewModel::deleteRecording,
                                    onOpenAllRecordings = { navigateTo(CyberiDestination.Recordings.route) },
                                    onOpenSettings = { navigateTo(CyberiDestination.Settings.route) }
                                )
                            }

                            composable(CyberiDestination.Recordings.route) {
                                RecordingsScreen(
                                    recordings = filteredRecordings,
                                    searchQuery = searchQuery,
                                    selectedFilter = selectedFilter,
                                    playbackState = playbackState,
                                    onSearchQueryChange = viewModel::setSearchQuery,
                                    onFilterChange = viewModel::setFilter,
                                    onTogglePlay = viewModel::togglePlay,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onRenameRecording = viewModel::renameRecording,
                                    onShareRecording = viewModel::shareRecording,
                                    onExportRecording = viewModel::exportToMusic,
                                    onDeleteRecording = viewModel::deleteRecording,
                                    onDeleteAllRecordings = viewModel::deleteAllRecordings
                                )
                            }

                            composable(CyberiDestination.Settings.route) {
                                SettingsScreen(
                                    settings = settings,
                                    deviceStatus = deviceStatus,
                                    onUpdateSettings = viewModel::updateSettings,
                                    onSelectFormat = viewModel::setAudioFormat,
                                    onSelectSource = viewModel::setAudioSource,
                                    onRequestPermissions = requestAllPermissions,
                                    onOpenBatterySettings = {
                                        DeviceCompatibilityManager.openBatteryOptimizationSettings(context)
                                    },
                                    onOpenSystemAppSettings = {
                                        DeviceCompatibilityManager.openAppPermissionSettings(context)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
