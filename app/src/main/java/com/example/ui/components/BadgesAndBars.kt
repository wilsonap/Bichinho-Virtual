package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PetStage
import com.example.data.model.Rarity

@Composable
fun StatBar(
    label: String,
    value: Int, // 0 - 100
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val clamped = value.coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped / 100f,
        animationSpec = tween(500),
        label = "stat_progress"
    )

    val barColor = when {
        clamped > 60 -> Color(0xFF10B981) // Emerald Green
        clamped > 30 -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFEF4444) // Red alert
    }

    val animatedColor by animateColorAsState(targetValue = barColor, label = "bar_color")

    Column(
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("stat_bar_$label")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = animatedColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$clamped%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = animatedColor
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(animatedColor)
            )
        }
    }
}

@Composable
fun CoinBadge(
    coins: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFEF3C7),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)),
        shadowElevation = 2.dp,
        modifier = modifier.testTag("coin_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "🪙", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$coins",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF78350F)
            )
        }
    }
}

@Composable
fun RarityBadge(
    rarity: Rarity,
    modifier: Modifier = Modifier
) {
    val bgColor = Color(rarity.colorHex)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, bgColor),
        modifier = modifier.testTag("rarity_badge_${rarity.name}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (rarity == Rarity.LENDARIA || rarity == Rarity.EPICA) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = bgColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = rarity.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = bgColor
            )
        }
    }
}

@Composable
fun StageBadge(
    stage: PetStage,
    modifier: Modifier = Modifier
) {
    val color = when (stage) {
        PetStage.OVO -> Color(0xFFE2E8F0)
        PetStage.FILHOTE -> Color(0xFF93C5FD)
        PetStage.JOVEM -> Color(0xFFA78BFA)
        PetStage.ADULTO -> Color(0xFFF472B6)
        PetStage.IDOSO -> Color(0xFFF59E0B) // Amber Gold for Senior / Wise Elder
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        modifier = modifier.testTag("stage_badge_${stage.name}")
    ) {
        Text(
            text = "Fase: ${stage.displayName}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
