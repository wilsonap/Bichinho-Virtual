package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PetEntity
import com.example.data.model.PetDisease
import com.example.data.model.PetHealthRules
import com.example.data.model.PetHealthState
import com.example.data.model.PetStage
import com.example.data.model.Rarity

/** Doença nomeada OU estado Doente/Crítico (comunicação UI). */
fun shouldShowHealthAlert(pet: PetEntity): Boolean {
    val disease = PetDisease.fromString(pet.disease)
    if (disease != PetDisease.NONE) return true
    val state = PetHealthRules.getHealthState(pet.health)
    return state == PetHealthState.DOENTE || state == PetHealthState.CRITICO
}

/** Sufixo da barra de Saúde — prioriza diagnóstico nomeado (ex.: Indigestão a 100%). */
fun healthBarStatusSuffix(pet: PetEntity): String? {
    val disease = PetDisease.fromString(pet.disease)
    if (disease != PetDisease.NONE) {
        return "${disease.iconEmoji} ${disease.displayName}"
    }
    return when (val state = PetHealthRules.getHealthState(pet.health)) {
        PetHealthState.DOENTE, PetHealthState.CRITICO, PetHealthState.INDISPOSTO -> state.displayName
        else -> null
    }
}

@Composable
fun StatBar(
    label: String,
    value: Int, // 0 - 100
    icon: ImageVector,
    modifier: Modifier = Modifier,
    statusSuffix: String? = null
) {
    val clamped = value.coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped / 100f,
        animationSpec = tween(500),
        label = "stat_progress"
    )

    val barColor = when {
        clamped > 60 -> Color(0xFF10B981)
        clamped > 30 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    val animatedColor by animateColorAsState(targetValue = barColor, label = "bar_color")
    val valueLabel = if (statusSuffix != null) {
        "$clamped/100 • $statusSuffix"
    } else {
        "$clamped%"
    }

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
                text = valueLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = animatedColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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

/**
 * Card persistente de alerta de saúde (abaixo dos cômodos, acima do pet).
 * Pulso lento na borda/ícone — sem piscar rápido.
 */
@Composable
fun HealthAlertCard(
    pet: PetEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!shouldShowHealthAlert(pet)) return

    val healthState = PetHealthRules.getHealthState(pet.health)
    val disease = PetDisease.fromString(pet.disease)
    val isCritical = healthState == PetHealthState.CRITICO
    val petName = pet.name.ifBlank { "Seu bichinho" }

    val (title, subtitle) = if (isCritical) {
        "🚨 Saúde crítica" to "$petName precisa de cuidados agora"
    } else {
        val second = if (disease != PetDisease.NONE) {
            "${disease.displayName} • Toque para cuidar"
        } else {
            "Toque para cuidar"
        }
        "🤒 $petName está doente" to second
    }

    val bg = if (isCritical) Color(0xFFFEE2E2) else Color(0xFFFFEDD5)
    val borderBase = if (isCritical) Color(0xFFDC2626) else Color(0xFFEA580C)
    val titleColor = if (isCritical) Color(0xFF991B1B) else Color(0xFF9A3412)

    val pulse = rememberInfiniteTransition(label = "health_alert_pulse")
    val borderAlpha by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "health_border_alpha"
    )
    val iconScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "health_icon_scale"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(2.dp, borderBase.copy(alpha = borderAlpha)),
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("health_alert_card")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isCritical) "🚨" else "🤒",
                fontSize = 22.sp,
                modifier = Modifier.scale(iconScale)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = titleColor.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Rounded.LocalHospital,
                contentDescription = "Abrir clínica",
                tint = borderBase,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun HealthConditionBadge(
    healthState: PetHealthState,
    modifier: Modifier = Modifier
) {
    if (healthState != PetHealthState.DOENTE && healthState != PetHealthState.CRITICO) {
        return
    }
    val bg = if (healthState == PetHealthState.CRITICO) Color(0xFFFEE2E2) else Color(0xFFFFEDD5)
    val fg = if (healthState == PetHealthState.CRITICO) Color(0xFFB91C1C) else Color(0xFFC2410C)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f)),
        shadowElevation = 2.dp,
        modifier = modifier.testTag("health_condition_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(text = healthState.icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = healthState.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = fg
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
        border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
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
        border = BorderStroke(1.dp, bgColor),
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
        PetStage.IDOSO -> Color(0xFFF59E0B)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, color),
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
