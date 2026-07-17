package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.platform.AiPlatform
import com.example.ui.theme.*

@Composable
fun PlatformCard(
    platform: AiPlatform,
    statusText: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isActive) AccentGreen.copy(alpha = 0.4f) else BorderDefault),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val bgColor = if (isActive) AccentGreen.copy(alpha = 0.15f) else Color(0xFF21262D)
            val fgColor = if (isActive) AccentGreen else TextSecondary

            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(platform.name.first().toString(), color = fgColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(platform.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            val dotColor = when {
                isActive -> AccentGreen
                statusText == "未获取" -> TextSecondary
                else -> AccentRed
            }
            Text("● $statusText", color = dotColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
