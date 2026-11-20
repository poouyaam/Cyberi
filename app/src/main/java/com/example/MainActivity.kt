package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import com.example.ui.theme.CyberiTheme
import com.example.ui.viewmodel.CyberiViewModel
import com.example.util.DeviceCompatibilityManager
import com.example.util.SupportActions
import kotlinx.coroutines.launch

sealed class CyberiDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : CyberiDestination("home", "خانه", Icons.Rounded.Home)
    data object Recordings : CyberiDestination("recordings", "تاریخچه", Icons.Rounded.History)
    data object Settings : CyberiDestination("settings", "تنظیمات", Icons.Rounded.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDark = isSystemInDarkTheme()
            LaunchedEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.Transparent.toArgb(),
                        Color.Transparent.toArgb()
                    ) { isDark },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.Transparent.toArgb(),
                        Color.Transparent.toArgb()
                    ) { isDark }
                )
            }
            CyberiTheme(darkTheme = isDark) {
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
    val coroutineScope = rememberCoroutineScope()
    val colors = CyberiTheme.colors

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val liveState by viewModel.liveRecorderState.collectAsStateWithLifecycle()
    val deviceStatus by viewModel.deviceStatus.collectAsStateWithLifecycle()
    val permissionSummary by viewModel.permissionSummary.collectAsStateWithLifecycle()
    val allRecordings by viewModel.allRecordings.collectAsStateWithLifecycle()
    val totalStorageBytes by viewModel.totalStorageUsedBytes.collectAsStateWithLifecycle()
    val filteredRecordings by viewModel.filteredRecordings.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    var autoRequestedOnLaunch by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshDeviceStatusAndSyncService()
    }

    val requestMissingPermissions: () -> Unit = {
        val missing = viewModel.permissionService.getMissingPermissions()
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing)
        } else {
            viewModel.refreshDeviceStatusAndSyncService()
        }
    }

    LaunchedEffect(Unit) {
        if (!autoRequestedOnLaunch) {
            autoRequestedOnLaunch = true
            val missing = viewModel.permissionService.getMissingPermissions()
            if (missing.isNotEmpty()) {
                permissionLauncher.launch(missing)
            } else {
                viewModel.refreshDeviceStatusAndSyncService()
            }
        }
    }

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

    val shareAppAction: () -> Unit = {
        coroutineScope.launch {
            SupportActions.shareCyberiApp(context)
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

                            FloatingGlassBottomBar(
                                destinations = destinations,
                                currentRoute = currentRoute,
                                onNavigate = navigateTo
                            )
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
                            containerColor = colors.surface.copy(alpha = 0.88f)
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
                                        selectedIconColor = Color.White,
                                        selectedTextColor = colors.cyan,
                                        indicatorColor = colors.cyan,
                                        unselectedIconColor = colors.textSecondary,
                                        unselectedTextColor = colors.textPrimary
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
                                    permissionSummary = permissionSummary,
                                    totalStorageBytes = totalStorageBytes,
                                    recentRecordings = allRecordings,
                                    contacts = contacts,
                                    playbackState = playbackState,
                                    onRequestPermissions = requestMissingPermissions,
                                    onOpenBatterySettings = {
                                        DeviceCompatibilityManager.openBatteryOptimizationSettings(context)
                                    },
                                    onToggleAutoRecord = viewModel::toggleAutoRecord,
                                    onToggleManualRecord = viewModel::toggleManualRecording,
                                    onShareApp = shareAppAction,
                                    onTogglePlay = viewModel::togglePlay,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onLoadContacts = viewModel::loadContacts,
                                    onAssignContact = viewModel::assignContactToRecording,
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
                                    allCount = allRecordings.size,
                                    totalStorageBytes = totalStorageBytes,
                                    settings = settings,
                                    hasContactsPermission = permissionSummary.hasReadContacts,
                                    contacts = contacts,
                                    searchQuery = searchQuery,
                                    selectedFilter = selectedFilter,
                                    playbackState = playbackState,
                                    onSearchQueryChange = viewModel::setSearchQuery,
                                    onFilterChange = viewModel::setFilter,
                                    onTogglePlay = viewModel::togglePlay,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onLoadContacts = viewModel::loadContacts,
                                    onAssignContact = viewModel::assignContactToRecording,
                                    onRequestContactsPermission = requestMissingPermissions,
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
                                    totalStorageBytes = totalStorageBytes,
                                    deviceStatus = deviceStatus,
                                    permissionSummary = permissionSummary,
                                    onUpdateSettings = viewModel::updateSettings,
                                    onSelectFormat = viewModel::setAudioFormat,
                                    onSelectSource = viewModel::setAudioSource,
                                    onSelectStorageLimit = viewModel::setStorageLimit,
                                    onCleanupStorageNow = viewModel::cleanupStorageNow,
                                    onRequestPermissions = requestMissingPermissions,
                                    onOpenBatterySettings = {
                                        DeviceCompatibilityManager.openBatteryOptimizationSettings(context)
                                    },
                                    onOpenSystemAppSettings = {
                                        DeviceCompatibilityManager.openAppPermissionSettings(context)
                                    },
                                    onOpenSupportEmail = {
                                        SupportActions.openGmailSupport(context)
                                    },
                                    onOpenSupportDialer = {
                                        SupportActions.openDialer(context)
                                    },
                                    onShareApp = shareAppAction
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
private fun FloatingGlassBottomBar(
    destinations: List<CyberiDestination>,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val colors = CyberiTheme.colors
    val shape = RoundedCornerShape(28.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .then(
                if (!colors.isDark) {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = shape,
                        ambientColor = Color.Black.copy(alpha = 0.10f)
                    )
                } else Modifier
            ),
        shape = shape,
        color = colors.glassFillBottom,
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                colors = listOf(
                    colors.glassBorderSubtle,
                    colors.indigo.copy(alpha = 0.30f),
                    colors.cyan.copy(alpha = 0.25f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEach { dest ->
                val selected = currentRoute == dest.route
                val itemModifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .then(
                        if (selected) {
                            Modifier.background(
                                Brush.horizontalGradient(
                                    colors = listOf(colors.cyan, colors.blue)
                                )
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onNavigate(dest.route) }
                    .testTag("nav_${dest.route}")

                Row(
                    modifier = itemModifier,
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = dest.label,
                        tint = if (selected) Color.White else colors.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dest.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Color.White else colors.textSecondary
                    )
                }
            }
        }
    }
}
