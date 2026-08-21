package com.example.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.DayPeriod
import com.example.data.model.WeatherState
import kotlin.math.cos
import kotlin.math.sin

/**
 * Quintal Tamagotchi 2D rico (layout fixo).
 * Esquerda: árvore + balanço · Centro livre · Direita: banco, flores, arbustos.
 *
 * DayPeriod / WeatherState entram só como camadas de céu, luz e clima —
 * sem redesenhar cerca, gramado, móveis ou posição do pet.
 */
fun DrawScope.drawBackyardScene(
    w: Float,
    h: Float,
    phase: Float,
    pulse: Float,
    isSleeping: Boolean,
    dayPeriod: DayPeriod = DayPeriod.AFTERNOON,
    weather: WeatherState = WeatherState.CLEAR
) {
    // Horizonte alinhado ao gramado onde o pet fica (~70% no stage)
    val horizonY = h * 0.58f
    val leftW = w * 0.32f
    val rightX = w * 0.68f
    val theme = OutdoorAmbience.theme(dayPeriod, weather)

    // --- DayLightingLayer (céu / sol / lua / estrelas) ---
    drawBackyardDayLighting(w, horizonY, phase, pulse, dayPeriod, theme)

    // --- WeatherLayer (nuvens) ---
    drawBackyardClouds(w, horizonY, phase, theme)

    // --- QuintalBase (inalterado vs. design pré–ciclo de tempo) ---
    drawBackyardFence(w, horizonY, isSleeping)
    drawBackyardGrass(w, h, horizonY, isSleeping)
    drawBackyardStonePath(w, h, horizonY, leftW, rightX, isSleeping)

    drawBackyardLeftTreeAndSwing(leftW, horizonY, h, phase, isSleeping)
    drawBackyardRightGarden(w, rightX, horizonY, h, phase, isSleeping)
    drawBackyardPond(w, leftW, horizonY, h, phase, isSleeping)

    val showButterflies =
        !isSleeping &&
            !theme.showRain &&
            dayPeriod != DayPeriod.NIGHT &&
            weather == WeatherState.CLEAR
    if (showButterflies) {
        drawBackyardButterflies(w, horizonY, phase, isSleeping = false)
    }

    // --- WeatherLayer (chuva + poças leves) + escurecimento ambiente ---
    drawBackyardWeatherOverlay(w, h, horizonY, phase, theme)
}

// -------------------------------------------------------------------------------------------------
/** Camada de iluminação do dia: só céu e astros; posições alinhadas ao céu original. */
private fun DrawScope.drawBackyardDayLighting(
    w: Float,
    horizonY: Float,
    phase: Float,
    pulse: Float,
    dayPeriod: DayPeriod,
    theme: OutdoorAmbience.Theme
) {
    drawRect(
        brush = Brush.verticalGradient(theme.skyColors, startY = 0f, endY = horizonY),
        topLeft = Offset.Zero,
        size = Size(w, horizonY)
    )

    if (theme.showStars) {
        for (i in 0..8) {
            val sx = w * (0.08f + (i * 0.1f) % 0.75f)
            val sy = horizonY * (0.12f + (i % 4) * 0.12f)
            drawCircle(Color.White.copy(alpha = 0.4f + pulse * 0.4f), 1.6f, Offset(sx, sy))
        }
    }

    if (theme.showMoon) {
        // Mesma âncora da lua original do quintal
        drawCircle(Color(0xFFFEF08A), 15f, Offset(w * 0.82f, horizonY * 0.28f))
        drawCircle(
            Color(0xFF0F172A).copy(alpha = 0.55f),
            12f,
            Offset(w * 0.82f - 5f, horizonY * 0.28f - 2f)
        )
    }

    if (theme.showSun) {
        val sunPulse = (sin(phase * 2f) + 1f) / 2f
        // Original: alto à direita; entardecer/manhã: um pouco mais baixo no horizonte
        val sunY = if (theme.sunLow) horizonY * 0.42f else horizonY * 0.26f
        val sunC = Offset(w * 0.84f, sunY)
        val sunColor =
            if (dayPeriod == DayPeriod.EVENING) Color(0xFFFB923C) else Color(0xFFFBBF24)
        drawCircle(Color(0xFFFDE047).copy(alpha = 0.25f + 0.2f * sunPulse), 30f, sunC)
        drawCircle(sunColor, 18f, sunC)
    }
}

private fun DrawScope.drawBackyardClouds(
    w: Float,
    horizonY: Float,
    phase: Float,
    theme: OutdoorAmbience.Theme
) {
    if (!theme.showClouds) return
    val alpha = if (theme.showRain) 0.72f else 0.88f
    val gray = theme.showRain || (!theme.showSun && !theme.showMoon)
    // Nuvem 1 — drift lento (mesmas trajetórias do cenário original)
    val c1x = ((phase * 10f) % (w + 80f)) - 40f
    drawCloud(c1x, horizonY * 0.22f, 1f, alpha, gray)
    val c2x = ((phase * 6f + w * 0.4f) % (w + 60f)) - 30f
    drawCloud(c2x, horizonY * 0.38f, 0.75f, alpha * 0.85f, gray)
    val c3x = ((phase * 8f + w * 0.7f) % (w + 70f)) - 35f
    drawCloud(c3x, horizonY * 0.15f, 0.9f, alpha * 0.9f, gray)
    if (theme.showRain) {
        val c4x = ((phase * 12f + w * 0.2f) % (w + 90f)) - 45f
        drawCloud(c4x, horizonY * 0.30f, 1.15f, alpha * 0.8f, gray = true)
    }
}

/** Chuva, poças leves e vinheta de luminosidade — não move móveis. */
private fun DrawScope.drawBackyardWeatherOverlay(
    w: Float,
    h: Float,
    horizonY: Float,
    phase: Float,
    theme: OutdoorAmbience.Theme
) {
    if (theme.showRain) {
        for (i in 0..28) {
            val rx = w * ((i * 0.037f + phase * 0.08f) % 1f)
            val ry = (horizonY * 0.05f) + (h * 0.85f) * ((i * 0.11f + phase * 0.25f) % 1f)
            drawLine(
                Color(0xFFBAE6FD).copy(alpha = 0.55f),
                Offset(rx, ry),
                Offset(rx + 2.5f, ry + 14f),
                strokeWidth = 1.6f
            )
        }
        // Poças pequenas no gramado (decorativas, sem alterar layout)
        drawOval(
            Color(0xFF0369A1).copy(alpha = 0.28f),
            Offset(w * 0.48f, h * 0.82f),
            Size(28f, 8f)
        )
        drawOval(
            Color(0xFF0369A1).copy(alpha = 0.22f),
            Offset(w * 0.62f, h * 0.88f),
            Size(18f, 6f)
        )
    }

    if (theme.outdoorDim > 0.01f) {
        drawRect(
            color = Color.Black.copy(alpha = theme.outdoorDim),
            topLeft = Offset.Zero,
            size = Size(w, h)
        )
    }
}

private fun DrawScope.drawCloud(
    cx: Float,
    cy: Float,
    scale: Float,
    alpha: Float,
    gray: Boolean = false
) {
    val c = if (gray) {
        Color(0xFF94A3B8).copy(alpha = alpha)
    } else {
        Color.White.copy(alpha = alpha)
    }
    drawCircle(c, 12f * scale, Offset(cx, cy))
    drawCircle(c, 16f * scale, Offset(cx + 14f * scale, cy - 4f * scale))
    drawCircle(c, 13f * scale, Offset(cx + 28f * scale, cy))
    drawCircle(c, 10f * scale, Offset(cx + 18f * scale, cy + 4f * scale))
}

private fun DrawScope.drawBackyardFence(w: Float, horizonY: Float, isSleeping: Boolean) {
    val fenceH = 28f
    val fenceY = horizonY - fenceH
    val picketW = 14f
    val gap = 4f
    val wood = if (isSleeping) Color(0xFF44403C) else Color(0xFFA16207)
    val woodLight = if (isSleeping) Color(0xFF57534E) else Color(0xFFD97706)
    val post = if (isSleeping) Color(0xFF292524) else Color(0xFF78350F)

    // Sombra da cerca
    drawRect(
        color = Color.Black.copy(alpha = 0.08f),
        topLeft = Offset(0f, horizonY - 2f),
        size = Size(w, 6f)
    )

    // Traves
    drawRoundRect(
        color = wood,
        topLeft = Offset(0f, fenceY + 8f),
        size = Size(w, 5f),
        cornerRadius = CornerRadius(1f, 1f)
    )
    drawRoundRect(
        color = woodLight,
        topLeft = Offset(0f, fenceY + 18f),
        size = Size(w, 5f),
        cornerRadius = CornerRadius(1f, 1f)
    )

    var x = 0f
    while (x < w) {
        val path = Path().apply {
            moveTo(x, horizonY)
            lineTo(x, fenceY + 6f)
            lineTo(x + picketW * 0.5f, fenceY)
            lineTo(x + picketW, fenceY + 6f)
            lineTo(x + picketW, horizonY)
            close()
        }
        drawPath(path, woodLight)
        drawPath(path, post.copy(alpha = 0.35f), style = Stroke(width = 1f))
        x += picketW + gap
    }
}

private fun DrawScope.drawBackyardGrass(
    w: Float,
    h: Float,
    horizonY: Float,
    isSleeping: Boolean
) {
    val top = if (isSleeping) Color(0xFF064E3B) else Color(0xFF4ADE80)
    val mid = if (isSleeping) Color(0xFF022C22) else Color(0xFF22C55E)
    val bot = if (isSleeping) Color(0xFF011911) else Color(0xFF15803D)
    drawRect(
        brush = Brush.verticalGradient(listOf(top, mid, bot), startY = horizonY, endY = h),
        topLeft = Offset(0f, horizonY),
        size = Size(w, h - horizonY)
    )

    // Manchas de tom (profundidade / gramado irregular)
    val patchA = if (isSleeping) Color(0xFF065F46).copy(alpha = 0.35f) else Color(0xFF16A34A).copy(alpha = 0.35f)
    val patchB = if (isSleeping) Color(0xFF047857).copy(alpha = 0.25f) else Color(0xFF86EFAC).copy(alpha = 0.28f)
    drawOval(patchA, Offset(w * 0.05f, horizonY + 18f), Size(w * 0.28f, 36f))
    drawOval(patchB, Offset(w * 0.55f, horizonY + 40f), Size(w * 0.32f, 42f))
    drawOval(patchA, Offset(w * 0.35f, h - 50f), Size(w * 0.3f, 28f))

    // Fiapos de grama
    val blade = if (isSleeping) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFF166534).copy(alpha = 0.35f)
    for (i in 0..18) {
        val gx = (w / 18f) * i + 4f
        val gy = horizonY + 8f + (i % 5) * 10f
        drawLine(blade, Offset(gx, gy + 8f), Offset(gx - 1.5f, gy), 1.2f)
        drawLine(blade, Offset(gx + 3f, gy + 8f), Offset(gx + 4.5f, gy + 1f), 1.2f)
    }
}

private fun DrawScope.drawBackyardStonePath(
    w: Float,
    h: Float,
    horizonY: Float,
    leftW: Float,
    rightX: Float,
    isSleeping: Boolean
) {
    // Caminho de pedras no centro-inferior (não cobre o peito do pet; passa sob os pés)
    val pathY = horizonY + (h - horizonY) * 0.55f
    val stone = if (isSleeping) Color(0xFF57534E) else Color(0xFFD6D3D1)
    val stoneDark = if (isSleeping) Color(0xFF44403C) else Color(0xFFA8A29E)

    val startX = leftW * 0.85f
    val endX = rightX + 8f
    val steps = 7
    for (i in 0 until steps) {
        val t = i / (steps - 1f)
        val sx = startX + (endX - startX) * t
        val sy = pathY + sin(t * 3.1f) * 6f
        val sw = 18f + (i % 3) * 3f
        val sh = 10f + (i % 2) * 2f
        drawOval(
            color = Color.Black.copy(alpha = 0.1f),
            topLeft = Offset(sx - sw * 0.5f + 1f, sy + 2f),
            size = Size(sw, sh)
        )
        drawOval(
            color = if (i % 2 == 0) stone else stoneDark,
            topLeft = Offset(sx - sw * 0.5f, sy),
            size = Size(sw, sh)
        )
    }
}

private fun DrawScope.drawBackyardLeftTreeAndSwing(
    leftW: Float,
    horizonY: Float,
    h: Float,
    phase: Float,
    isSleeping: Boolean
) {
    val trunkX = leftW * 0.28f
    val trunkW = 28f
    val trunkH = 118f
    val trunkTopY = horizonY - trunkH + 8f
    val sway = sin(phase) * 3.5f

    // Sombra da árvore
    drawOval(
        color = Color.Black.copy(alpha = 0.14f),
        topLeft = Offset(trunkX - 18f, horizonY - 4f),
        size = Size(70f, 16f)
    )

    // Tronco detalhado
    val bark = if (isSleeping) Color(0xFF292524) else Color(0xFF78350F)
    val barkLight = if (isSleeping) Color(0xFF44403C) else Color(0xFFA16207)
    drawRoundRect(
        brush = Brush.horizontalGradient(listOf(bark, barkLight, bark)),
        topLeft = Offset(trunkX, trunkTopY),
        size = Size(trunkW, trunkH),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Textura
    drawLine(
        bark.copy(alpha = 0.5f),
        Offset(trunkX + 8f, trunkTopY + 20f),
        Offset(trunkX + 8f, trunkTopY + trunkH - 10f),
        2f
    )
    drawLine(
        barkLight.copy(alpha = 0.4f),
        Offset(trunkX + 18f, trunkTopY + 30f),
        Offset(trunkX + 16f, trunkTopY + trunkH - 20f),
        1.5f
    )

    // Galho do balanço
    drawRoundRect(
        color = bark,
        topLeft = Offset(trunkX + trunkW - 6f, trunkTopY + 28f),
        size = Size(72f, 11f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Copa maior / em camadas
    val leafDark = if (isSleeping) Color(0xFF064E3B) else Color(0xFF166534)
    val leafMid = if (isSleeping) Color(0xFF065F46) else Color(0xFF15803D)
    val leafLight = if (isSleeping) Color(0xFF047857) else Color(0xFF4ADE80)
    val crownY = trunkTopY + 8f
    drawCircle(leafDark, 52f, Offset(trunkX + 8f + sway, crownY - 8f))
    drawCircle(leafMid, 44f, Offset(trunkX + 40f + sway, crownY))
    drawCircle(leafLight, 40f, Offset(trunkX - 18f + sway, crownY + 6f))
    drawCircle(leafMid, 36f, Offset(trunkX + 22f + sway * 0.5f, crownY - 28f))
    drawCircle(leafLight.copy(alpha = 0.85f), 28f, Offset(trunkX + 50f + sway, crownY - 18f))

    // Balanço maior
    val ropeX1 = trunkX + trunkW + 18f
    val ropeX2 = ropeX1 + 28f
    val ropeTop = trunkTopY + 36f
    val swingSway = sin(phase * 1.4f) * 5f
    val seatY = horizonY - 28f
    drawLine(Color(0xFFD97706), Offset(ropeX1, ropeTop), Offset(ropeX1 + swingSway, seatY), 2.5f)
    drawLine(Color(0xFFD97706), Offset(ropeX2, ropeTop), Offset(ropeX2 + swingSway, seatY), 2.5f)
    // Assento
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.12f),
        topLeft = Offset(ropeX1 - 6f + swingSway, seatY + 3f),
        size = Size(40f, 6f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = Color(0xFFB45309),
        topLeft = Offset(ropeX1 - 6f + swingSway, seatY),
        size = Size(40f, 8f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawRoundRect(
        color = Color(0xFF92400E),
        topLeft = Offset(ropeX1 - 4f + swingSway, seatY + 2f),
        size = Size(36f, 3f),
        cornerRadius = CornerRadius(1f, 1f)
    )

    // Arbusto pequeno sob a árvore
    val bushY = horizonY - 6f
    drawOval(
        if (isSleeping) Color(0xFF065F46) else Color(0xFF16A34A),
        Offset(trunkX - 22f, bushY),
        Size(36f, 18f)
    )
    drawOval(
        if (isSleeping) Color(0xFF047857) else Color(0xFF4ADE80),
        Offset(trunkX - 14f, bushY - 4f),
        Size(28f, 14f)
    )
}

private fun DrawScope.drawBackyardRightGarden(
    w: Float,
    rightX: Float,
    horizonY: Float,
    h: Float,
    phase: Float,
    isSleeping: Boolean
) {
    val zoneW = w - rightX

    // Arbustos
    val bushBase = if (isSleeping) Color(0xFF064E3B) else Color(0xFF15803D)
    val bushLite = if (isSleeping) Color(0xFF047857) else Color(0xFF4ADE80)
    drawOval(bushBase, Offset(rightX + 4f, horizonY - 8f), Size(42f, 22f))
    drawOval(bushLite, Offset(rightX + 12f, horizonY - 14f), Size(34f, 18f))
    drawOval(bushBase, Offset(w - 48f, horizonY - 6f), Size(40f, 20f))
    drawOval(bushLite, Offset(w - 40f, horizonY - 12f), Size(30f, 16f))

    // Banco de jardim
    val benchW = (zoneW * 0.72f).coerceIn(56f, 95f)
    val benchH = 28f
    val benchX = rightX + (zoneW - benchW) * 0.35f
    val benchY = horizonY + 28f
    val wood = if (isSleeping) Color(0xFF44403C) else Color(0xFFB45309)
    val woodDark = if (isSleeping) Color(0xFF292524) else Color(0xFF78350F)

    drawOval(
        Color.Black.copy(alpha = 0.12f),
        Offset(benchX + 2f, benchY + benchH - 2f),
        Size(benchW, 10f)
    )
    // Pés
    drawRoundRect(woodDark, Offset(benchX + 6f, benchY + 10f), Size(5f, 16f), CornerRadius(1f, 1f))
    drawRoundRect(woodDark, Offset(benchX + benchW - 11f, benchY + 10f), Size(5f, 16f), CornerRadius(1f, 1f))
    // Assento
    drawRoundRect(wood, Offset(benchX, benchY + 8f), Size(benchW, 7f), CornerRadius(3f, 3f))
    // Encosto
    drawRoundRect(wood, Offset(benchX, benchY - 4f), Size(benchW, 6f), CornerRadius(2f, 2f))
    drawLine(woodDark, Offset(benchX + 8f, benchY + 2f), Offset(benchX + 8f, benchY + 10f), 3f)
    drawLine(woodDark, Offset(benchX + benchW - 8f, benchY + 2f), Offset(benchX + benchW - 8f, benchY + 10f), 3f)

    // Jardim de flores
    val flowerColors = listOf(
        Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFFEC4899),
        Color(0xFF8B5CF6), Color(0xFF38BDF8), Color(0xFFF472B6)
    )
    for (f in 0..10) {
        val fx = rightX + 10f + (f % 5) * 16f
        val fy = horizonY + 8f + (f / 5) * 14f + sin(phase + f) * 1.5f
        if (fx < w - 8f) {
            drawLine(Color(0xFF166534), Offset(fx, fy + 9f), Offset(fx, fy), 2f)
            drawCircle(flowerColors[f % flowerColors.size], 4.2f, Offset(fx, fy))
            drawCircle(Color(0xFFFEF08A), 1.6f, Offset(fx, fy))
        }
    }

    // Mais flores baixas perto do banco
    for (f in 0..4) {
        val fx = benchX + 8f + f * 14f
        val fy = benchY + 22f
        drawLine(Color(0xFF166534), Offset(fx, fy + 6f), Offset(fx, fy), 1.5f)
        drawCircle(flowerColors[(f + 2) % flowerColors.size], 3.5f, Offset(fx, fy))
    }
}

private fun DrawScope.drawBackyardPond(
    w: Float,
    leftW: Float,
    horizonY: Float,
    h: Float,
    phase: Float,
    isSleeping: Boolean
) {
    // Lago decorativo pequeno — canto inferior esquerdo (fora do centro do pet)
    val pondW = (leftW * 0.7f).coerceIn(48f, 85f)
    val pondH = 28f
    val pondX = 10f
    val pondY = h - pondH - 18f

    drawOval(
        Color.Black.copy(alpha = 0.12f),
        Offset(pondX + 2f, pondY + 3f),
        Size(pondW, pondH)
    )
    drawOval(
        if (isSleeping) Color(0xFF1E3A5F) else Color(0xFF0284C7),
        Offset(pondX, pondY),
        Size(pondW, pondH)
    )
    drawOval(
        if (isSleeping) Color(0xFF0EA5E9).copy(alpha = 0.35f) else Color(0xFF38BDF8).copy(alpha = 0.55f),
        Offset(pondX + 6f, pondY + 4f),
        Size(pondW * 0.55f, pondH * 0.45f)
    )
    // Brilho / ondulação
    val ripple = sin(phase * 2f) * 2f
    drawOval(
        Color.White.copy(alpha = 0.35f),
        Offset(pondX + 14f + ripple, pondY + 8f),
        Size(18f, 5f)
    )
    // Pedrinhas na borda
    drawCircle(Color(0xFF78716C), 3f, Offset(pondX + 4f, pondY + pondH - 4f))
    drawCircle(Color(0xFFA8A29E), 2.5f, Offset(pondX + pondW - 6f, pondY + pondH - 5f))
    drawCircle(Color(0xFF78716C), 2.8f, Offset(pondX + pondW * 0.45f, pondY + pondH - 2f))
}

private fun DrawScope.drawBackyardButterflies(
    w: Float,
    horizonY: Float,
    phase: Float,
    isSleeping: Boolean
) {
    if (isSleeping) return

    fun butterfly(bx: Float, by: Float, scale: Float, color: Color, flap: Float) {
        val wing = 5f * scale + flap
        drawCircle(color.copy(alpha = 0.85f), wing, Offset(bx - 4f * scale, by))
        drawCircle(color.copy(alpha = 0.85f), wing, Offset(bx + 4f * scale, by))
        drawCircle(Color(0xFF1E293B), 1.5f * scale, Offset(bx, by))
    }

    val f1 = sin(phase * 3f)
    val f2 = sin(phase * 2.4f + 1f)
    butterfly(
        w * 0.42f + cos(phase) * 18f,
        horizonY * 0.45f + sin(phase * 1.3f) * 12f,
        1f,
        Color(0xFFF472B6),
        f1 * 1.5f
    )
    butterfly(
        w * 0.58f + cos(phase * 0.8f + 2f) * 22f,
        horizonY * 0.55f + sin(phase * 1.1f + 1f) * 10f,
        0.85f,
        Color(0xFFFBBF24),
        f2 * 1.2f
    )
    butterfly(
        w * 0.28f + cos(phase * 1.2f) * 14f,
        horizonY * 0.35f + sin(phase + 0.5f) * 8f,
        0.7f,
        Color(0xFFA78BFA),
        sin(phase * 3.5f) * 1f
    )
}
