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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.db.SignalEntity
import com.example.ui.theme.AccentGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.SignalDownRed
import com.example.ui.theme.SignalUpGreen
import com.example.ui.theme.SignalWaitGray
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SignalHistoryScreen(
    signals: List<SignalEntity>,
    winCount: Int,
    lossCount: Int,
    totalCount: Int,
    onMarkResult: (Long, String) -> Unit,
    onDeleteSignal: (Long) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val evaluatedCount = winCount + lossCount
    val winRate = if (evaluatedCount > 0) (winCount.toDouble() / evaluatedCount * 100).toInt() else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Title & Clear All
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Signal History",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (signals.isNotEmpty()) {
                IconButton(
                    onClick = onClearAll,
                    modifier = Modifier.testTag("clear_history_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = SignalDownRed)
                }
            }
        }

        // Win Rate & Performance Stats Card
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
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatColumn(label = "Win Rate", value = "$winRate%", color = SignalUpGreen)
                StatColumn(label = "Wins", value = "$winCount", color = SignalUpGreen)
                StatColumn(label = "Losses", value = "$lossCount", color = SignalDownRed)
                StatColumn(label = "Total Logs", value = "$totalCount", color = TextPrimary)
            }
        }

        if (signals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No saved signals yet.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(signals) { item ->
                    SignalItemCard(
                        item = item,
                        onMarkResult = { outcome -> onMarkResult(item.id, outcome) },
                        onDelete = { onDeleteSignal(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun StatColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
fun SignalItemCard(
    item: SignalEntity,
    onMarkResult: (String) -> Unit,
    onDelete: () -> Unit
) {
    val (signalColor, signalText) = when (item.signalType) {
        "UP" -> Pair(SignalUpGreen, "🟢 UP")
        "DOWN" -> Pair(SignalDownRed, "🔴 DOWN")
        else -> Pair(SignalWaitGray, "⚪ WAIT")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    Text(text = item.asset, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(signalColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = signalText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = signalColor)
                    }
                }

                Text(text = item.timestamp, fontSize = 11.sp, color = TextSecondary)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Price: %.5f".format(item.price), fontSize = 12.sp, color = AccentGold)
                Text(text = "Confidence: ${item.confidence}%", fontSize = 12.sp, color = TextPrimary)
            }

            Text(text = item.reason, fontSize = 11.sp, color = TextSecondary, maxLines = 1)

            // Win / Loss Outcome Marking Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (resultText, resultColor) = when (item.userResult) {
                    "WIN" -> Pair("WIN ✓", SignalUpGreen)
                    "LOSS" -> Pair("LOSS ✗", SignalDownRed)
                    else -> Pair("PENDING", SignalWaitGray)
                }

                Text(
                    text = "Result: $resultText",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = resultColor
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { onMarkResult("WIN") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SignalUpGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("win_btn_${item.id}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WIN", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onMarkResult("LOSS") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SignalDownRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("loss_btn_${item.id}")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LOSS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
