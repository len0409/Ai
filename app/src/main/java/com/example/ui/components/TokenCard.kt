package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.TokenEntity
import com.example.data.health.TokenHealthStatus
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TokenCard(
    token: TokenEntity,
    healthStatus: TokenHealthStatus? = null,
    onCopy: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when {
                        healthStatus != null && !healthStatus.isHealthy -> AccentRed
                        token.status == "active" -> AccentGreen
                        else -> AccentRed
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(token.platformId, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    if (healthStatus != null) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (healthStatus.isHealthy) AccentGreen.copy(alpha = 0.12f)
                                    else AccentRed.copy(alpha = 0.12f)
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                if (healthStatus.isHealthy) "有效" else "失效",
                                color = if (healthStatus.isHealthy) AccentGreen else AccentRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))
                    Text(token.label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Row {
                    IconButton(
                        onClick = { onCopy(token.tokenValue) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, tint = AccentRed.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                token.tokenValue.take(30) + "..." + token.tokenValue.takeLast(10),
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "获取: ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(token.createdAt))}",
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
                if (token.lastUsedAt > 0) {
                    Text(
                        "最后使用: ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(token.lastUsedAt))}",
                        color = TextSecondary.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
