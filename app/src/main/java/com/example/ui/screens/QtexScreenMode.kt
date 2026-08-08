package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnalysisResult
import com.example.data.model.DataQuality
import com.example.data.model.SignalType
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.SignalDownRed
import com.example.ui.theme.SignalUpBg
import com.example.ui.theme.SignalUpGreen
import com.example.ui.theme.SignalWaitBg
import com.example.ui.theme.SignalWaitGray
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun QtexScreenMode(
    isCaptureActive: Boolean,
    currentResult: AnalysisResult?,
    onRequestStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val res = currentResult ?: AnalysisResult(
        asset = "EUR/USD",
        price = 1.17342,
        timeframe = "1 MIN",
        signalType = SignalType.WAIT,
        confidence = 0,
        dataQuality = DataQuality.LOW,
        confirmations = emptyList(),
        reason = "Screen capture stopped",
        timestamp = "--:--:--"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Qtex Screen Analysis",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // Capture Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Capture Status",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = if (isCaptureActive) "ACTIVE" else "STOPPED",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCaptureActive) SignalUpGreen else SignalWaitGray
                    )
                }

                if (!isCaptureActive) {
                    Button(
                        onClick = onRequestStartCapture,
                        colors = ButtonDefaults.buttonColors(containerColor = SignalUpGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("qtex_start_capture_btn")
                    ) {
                        Icon(Icons.Default.ScreenShare, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("START", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onStopCapture,
                        colors = ButtonDefaults.buttonColors(containerColor = SignalDownRed, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("qtex_stop_capture_btn")
                    ) {
                        Icon(Icons.Default.StopScreenShare, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("STOP", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Data Quality Status Card
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = AccentCyan)
                        Text(
                            text = "Frame Data Quality",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    val qualityColor = when (res.dataQuality) {
                        DataQuality.HIGH -> SignalUpGreen
                        DataQuality.MEDIUM -> AccentGold
                        DataQuality.LOW -> SignalDownRed
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(qualityColor.copy(alpha = 0.2f))
                            .border(1.dp, qualityColor, CircleShape)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = res.dataQuality.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = qualityColor
                        )
                    }
                }

                if (res.dataQuality == DataQuality.LOW) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = SignalWaitBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SignalDownRed.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = SignalDownRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "WAIT — DATA QUALITY TOO LOW. Please bring the Qtex candlestick chart into clear view on screen.",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Live Technical Indicator Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Screen Analysis Indicators",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                IndicatorMetricRow("EMA 20", "%.5f".format(res.ema20))
                IndicatorMetricRow("EMA 50", "%.5f".format(res.ema50))
                IndicatorMetricRow("RSI (14)", "%.1f".format(res.rsi14))
                IndicatorMetricRow("MACD Histogram", "%.5f".format(res.macdValue))
                IndicatorMetricRow("Supertrend", res.supertrend)
            }
        }
    }
}

@Composable
fun IndicatorMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
