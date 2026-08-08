package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AnalysisResult
import com.example.data.model.DataQuality
import com.example.data.model.SignalType
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.SignalDownBg
import com.example.ui.theme.SignalDownRed
import com.example.ui.theme.SignalUpBg
import com.example.ui.theme.SignalUpGreen
import com.example.ui.theme.SignalWaitBg
import com.example.ui.theme.SignalWaitGray
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    currentResult: AnalysisResult?,
    isCaptureActive: Boolean,
    isAnalysisPaused: Boolean,
    statusMessage: String,
    onRequestStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Status Header Bar
        CaptureStatusHeader(
            isCaptureActive = isCaptureActive,
            isPaused = isAnalysisPaused,
            statusMessage = statusMessage
        )

        // Hero Graphic Banner
        HeroBannerCard()

        // Main Signal Card
        MainSignalCard(
            result = currentResult,
            isCaptureActive = isCaptureActive
        )

        // Control Action Buttons
        ActionControlsCard(
            isCaptureActive = isCaptureActive,
            isPaused = isAnalysisPaused,
            onRequestStartCapture = onRequestStartCapture,
            onStopCapture = onStopCapture,
            onTogglePause = onTogglePause
        )

        // Safety Disclaimer Card
        SafetyDisclaimerCard()

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun CaptureStatusHeader(
    isCaptureActive: Boolean,
    isPaused: Boolean,
    statusMessage: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Qtex AI Signal Analyzer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Active / Stopped Badge
            val (badgeBg, badgeText, badgeColor) = when {
                isPaused -> Triple(SignalWaitBg, "PAUSED", SignalWaitGray)
                isCaptureActive -> Triple(SignalUpBg, "ACTIVE", SignalUpGreen)
                else -> Triple(SignalWaitBg, "STOPPED", SignalWaitGray)
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(badgeBg)
                    .border(1.dp, badgeColor.copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }
    }
}

@Composable
fun HeroBannerCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.qtex_hero_banner_1786193810850),
                contentDescription = "Qtex AI Signal Analyzer Banner",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.30f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = "PRECISION SIGNAL ENGINE",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Computer Vision & EMA Multi-Confluence",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun MainSignalCard(
    result: AnalysisResult?,
    isCaptureActive: Boolean
) {
    val res = result ?: AnalysisResult(
        asset = "EUR/USD",
        price = 1.17342,
        timeframe = "1 MIN",
        signalType = SignalType.WAIT,
        confidence = 0,
        dataQuality = DataQuality.LOW,
        confirmations = emptyList(),
        reason = "Tap Start Screen Capture to begin chart analysis",
        timestamp = "--:--:--"
    )

    val (signalBg, signalTextColor, signalIcon) = when (res.signalType) {
        SignalType.UP -> Triple(SignalUpBg, SignalUpGreen, "🟢 UP")
        SignalType.DOWN -> Triple(SignalDownBg, SignalDownRed, "🔴 DOWN")
        SignalType.WAIT -> Triple(SignalWaitBg, SignalWaitGray, "⚪ WAIT")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, signalTextColor.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pair & Price Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = res.asset,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Timeframe: ${res.timeframe}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "%.5f".format(res.price),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold
                    )
                    Text(
                        text = "Detected Price",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // Big Signal Indicator Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(signalBg)
                    .border(1.dp, signalTextColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = signalIcon,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = signalTextColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Confidence: ${res.confidence}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Confidence Progress Bar
                    LinearProgressIndicator(
                        progress = { res.confidence / 100f },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = signalTextColor,
                        trackColor = DarkSurfaceVariant,
                    )
                }
            }

            // Data Quality Indicator Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Data Quality:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                val qualityColor = when (res.dataQuality) {
                    DataQuality.HIGH -> SignalUpGreen
                    DataQuality.MEDIUM -> AccentGold
                    DataQuality.LOW -> SignalDownRed
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(qualityColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = res.dataQuality.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = qualityColor
                    )
                }
            }

            // Reason Line
            Text(
                text = res.reason,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (res.dataQuality == DataQuality.LOW) SignalDownRed else TextPrimary
            )

            // Technical Confirmations List
            if (res.confirmations.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Technical Confirmations:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )

                    res.confirmations.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isBullish) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (item.isBullish) SignalUpGreen else SignalWaitGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${item.name}: ${item.description}",
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Signal generated: ${res.timestamp}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun ActionControlsCard(
    isCaptureActive: Boolean,
    isPaused: Boolean,
    onRequestStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onTogglePause: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isCaptureActive) {
                Button(
                    onClick = onRequestStartCapture,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("start_capture_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SignalUpGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenShare,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START SCREEN CAPTURE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onStopCapture,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("stop_capture_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SignalDownRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.StopScreenShare,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STOP SCREEN CAPTURE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            OutlinedButton(
                onClick = onTogglePause,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pause_analysis_button"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPaused) "RESUME ANALYSIS" else "PAUSE ANALYSIS",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun SafetyDisclaimerCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = "Analysis Only Guarantee",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "This app NEVER executes trades, clicks buttons inside Qtex, or accesses account credentials. Every trading decision is made manually by you.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
