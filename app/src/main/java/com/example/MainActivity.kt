package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.ScreenCaptureService
import com.example.ui.MainViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ForexPairsScreen
import com.example.ui.screens.QtexScreenMode
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignalHistoryScreen
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.QtexTheme
import com.example.ui.theme.SignalUpGreen
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                    as MediaProjectionManager

        setContent {

            QtexTheme {

                var selectedTab by remember {
                    mutableStateOf(0)
                }

                val isCaptureActive by
                    viewModel.isCaptureActive.collectAsStateWithLifecycle()

                val isAnalysisPaused by
                    viewModel.isAnalysisPaused.collectAsStateWithLifecycle()

                val currentResult by
                    viewModel.currentResult.collectAsStateWithLifecycle()

                val statusMessage by
                    viewModel.statusMessage.collectAsStateWithLifecycle()

                val analysisIntervalMs by
                    viewModel.analysisIntervalMs.collectAsStateWithLifecycle()

                val minConfidenceThreshold by
                    viewModel.minConfidenceThreshold.collectAsStateWithLifecycle()

                val selectedPair by
                    viewModel.selectedPair.collectAsStateWithLifecycle()

                val historyList by
                    viewModel.historySignals.collectAsStateWithLifecycle()

                val winCount by
                    viewModel.winCount.collectAsStateWithLifecycle()

                val lossCount by
                    viewModel.lossCount.collectAsStateWithLifecycle()

                val totalCount by
                    viewModel.totalCount.collectAsStateWithLifecycle()


                /*
                 * Android 13+ Notification Permission
                 */
                val notificationPermissionLauncher =
                    rememberLauncherForActivityResult(
                        contract =
                            ActivityResultContracts.RequestPermission()
                    ) {
                        // Permission result handled automatically.
                    }


                /*
                 * Screen Capture Permission
                 */
                val screenCaptureLauncher =
                    rememberLauncherForActivityResult(
                        contract =
                            ActivityResultContracts.StartActivityForResult()
                    ) { result ->

                        if (
                            result.resultCode == Activity.RESULT_OK &&
                            result.data != null
                        ) {

                            val serviceIntent =
                                Intent(
                                    this@MainActivity,
                                    ScreenCaptureService::class.java
                                ).apply {

                                    action =
                                        ScreenCaptureService.ACTION_START

                                    putExtra(
                                        ScreenCaptureService.EXTRA_RESULT_CODE,
                                        result.resultCode
                                    )

                                    putExtra(
                                        ScreenCaptureService.EXTRA_RESULT_DATA,
                                        result.data
                                    )
                                }


                            if (
                                Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.O
                            ) {

                                startForegroundService(serviceIntent)

                            } else {

                                startService(serviceIntent)
                            }
                        }
                    }


                /*
                 * Start Screen Capture
                 */
                val onStartCapture: () -> Unit = {

                    /*
                     * Ask notification permission first
                     */
                    if (
                        Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.TIRAMISU
                    ) {

                        val notificationGranted =
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED

                        if (!notificationGranted) {

                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                    }


                    /*
                     * Open Android's official
                     * Screen Capture permission dialog.
                     */
                    val captureIntent =
                        projectionManager
                            .createScreenCaptureIntent()

                    screenCaptureLauncher.launch(captureIntent)
                }


                /*
                 * Stop Screen Capture
                 */
                val onStopCapture: () -> Unit = {

                    val stopIntent =
                        Intent(
                            this@MainActivity,
                            ScreenCaptureService::class.java
                        ).apply {
                            action =
                                ScreenCaptureService.ACTION_STOP
                        }

                    startService(stopIntent)
                }


                Scaffold(

                    modifier = Modifier.fillMaxSize(),

                    bottomBar = {

                        NavigationBar(

                            modifier =
                                Modifier.windowInsetsPadding(
                                    WindowInsets.navigationBars
                                ),

                            containerColor = DarkSurface,
                            tonalElevation = 3.dp

                        ) {

                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = {
                                    selectedTab = 0
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.Dashboard,
                                        contentDescription = "Dashboard"
                                    )
                                },
                                label = {
                                    Text(
                                        "Dashboard",
                                        fontSize = 10.sp
                                    )
                                },
                                colors =
                                    NavigationBarItemDefaults.colors(
                                        selectedIconColor =
                                            SignalUpGreen,
                                        selectedTextColor =
                                            SignalUpGreen,
                                        unselectedIconColor =
                                            TextSecondary,
                                        unselectedTextColor =
                                            TextSecondary,
                                        indicatorColor =
                                            DarkBorder
                                    ),
                                modifier =
                                    Modifier.testTag(
                                        "nav_tab_dashboard"
                                    )
                            )


                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = {
                                    selectedTab = 1
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.Analytics,
                                        contentDescription = "Qtex Mode"
                                    )
                                },
                                label = {
                                    Text(
                                        "Qtex",
                                        fontSize = 10.sp
                                    )
                                },
                                colors =
                                    NavigationBarItemDefaults.colors(
                                        selectedIconColor =
                                            SignalUpGreen,
                                        selectedTextColor =
                                            SignalUpGreen,
                                        unselectedIconColor =
                                            TextSecondary,
                                        unselectedTextColor =
                                            TextSecondary,
                                        indicatorColor =
                                            DarkBorder
                                    ),
                                modifier =
                                    Modifier.testTag(
                                        "nav_tab_qtex"
                                    )
                            )


                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = {
                                    selectedTab = 2
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.ShowChart,
                                        contentDescription = "Forex Pairs"
                                    )
                                },
                                label = {
                                    Text(
                                        "Forex",
                                        fontSize = 10.sp
                                    )
                                },
                                colors =
                                    NavigationBarItemDefaults.colors(
                                        selectedIconColor =
                                            SignalUpGreen,
                                        selectedTextColor =
                                            SignalUpGreen,
                                        unselectedIconColor =
                                            TextSecondary,
                                        unselectedTextColor =
                                            TextSecondary,
                                        indicatorColor =
                                            DarkBorder
                                    ),
                                modifier =
                                    Modifier.testTag(
                                        "nav_tab_forex"
                                    )
                            )


                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = {
                                    selectedTab = 3
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = "History"
                                    )
                                },
                                label = {
                                    Text(
                                        "History",
                                        fontSize = 10.sp
                                    )
                                },
                                colors =
                                    NavigationBarItemDefaults.colors(
                                        selectedIconColor =
                                            SignalUpGreen,
                                        selectedTextColor =
                                            SignalUpGreen,
                                        unselectedIconColor =
                                            TextSecondary,
                                        unselectedTextColor =
                                            TextSecondary,
                                        indicatorColor =
                                            DarkBorder
                                    ),
                                modifier =
                                    Modifier.testTag(
                                        "nav_tab_history"
                                    )
                            )


                            NavigationBarItem(
                                selected = selectedTab == 4,
                                onClick = {
                                    selectedTab = 4
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Settings"
                                    )
                                },
                                label = {
                                    Text(
                                        "Settings",
                                        fontSize = 10.sp
                                    )
                                },
                                colors =
                                    NavigationBarItemDefaults.colors(
                                        selectedIconColor =
                                            SignalUpGreen,
                                        selectedTextColor =
                                            SignalUpGreen,
                                        unselectedIconColor =
                                            TextSecondary,
                                        unselectedTextColor =
                                            TextSecondary,
                                        indicatorColor =
                                            DarkBorder
                                    ),
                                modifier =
                                    Modifier.testTag(
                                        "nav_tab_settings"
                                    )
                            )
                        }
                    }

                ) { innerPadding ->

                    when (selectedTab) {

                        0 -> DashboardScreen(
                            currentResult = currentResult,
                            isCaptureActive = isCaptureActive,
                            isAnalysisPaused = isAnalysisPaused,
                            statusMessage = statusMessage,
                            onRequestStartCapture = onStartCapture,
                            onStopCapture = onStopCapture,
                            onTogglePause = {
                                viewModel.toggleAnalysisPause()
                            },
                            modifier =
                                Modifier.padding(innerPadding)
                        )


                        1 -> QtexScreenMode(
                            isCaptureActive = isCaptureActive,
                            currentResult = currentResult,
                            onRequestStartCapture = onStartCapture,
                            onStopCapture = onStopCapture,
                            modifier =
                                Modifier.padding(innerPadding)
                        )


                        2 -> ForexPairsScreen(
                            pairs = viewModel.supportedPairs,
                            selectedPair = selectedPair,
                            onSelectPair = {
                                viewModel.setSelectedPair(it)
                            },
                            modifier =
                                Modifier.padding(innerPadding)
                        )


                        3 -> SignalHistoryScreen(
                            signals = historyList,
                            winCount = winCount,
                            lossCount = lossCount,
                            totalCount = totalCount,
                            onMarkResult = {
                                    id,
                                    outcome ->
                                viewModel.markSignalOutcome(
                                    id,
                                    outcome
                                )
                            },
                            onDeleteSignal = {
                                viewModel.deleteSignal(it)
                            },
                            onClearAll = {
                                viewModel.clearSignalHistory()
                            },
                            modifier =
                                Modifier.padding(innerPadding)
                        )


                        4 -> SettingsScreen(
                            analysisIntervalMs =
                                analysisIntervalMs,
                            minConfidenceThreshold =
                                minConfidenceThreshold,
                            onSetInterval = {
                                viewModel.setAnalysisInterval(it)
                            },
                            onSetConfidence = {
                                viewModel.setMinConfidenceThreshold(it)
                            },
                            modifier =
                                Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
