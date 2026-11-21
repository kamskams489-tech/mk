package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlateMetalInk
import com.example.ui.theme.PlateMetalLight
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafeEmerald

enum class PlateBadgeStyle {
    STOLEN,
    RECOVERED,
    NEUTRAL
}

@Composable
fun LicensePlateBadge(
    plateText: String,
    modifier: Modifier = Modifier,
    style: PlateBadgeStyle = PlateBadgeStyle.NEUTRAL,
    fontSize: TextUnit = 18.sp
) {
    val stripeColor = when (style) {
        PlateBadgeStyle.STOLEN -> AlertCrimson
        PlateBadgeStyle.RECOVERED -> SafeEmerald
        PlateBadgeStyle.NEUTRAL -> RadarCyan
    }
    val borderColor = when (style) {
        PlateBadgeStyle.STOLEN -> AlertCrimson
        PlateBadgeStyle.RECOVERED -> SafeEmerald.copy(alpha = 0.8f)
        PlateBadgeStyle.NEUTRAL -> Color(0xFF475569)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF8FAFC), PlateMetalLight)
                    )
                )
                .border(
                    width = 1.75.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(10.dp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left tactical stripe
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(42.dp)
                    .background(stripeColor)
            )
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = plateText.ifBlank { "---" },
                        color = PlateMetalInk,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        letterSpacing = 1.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}
