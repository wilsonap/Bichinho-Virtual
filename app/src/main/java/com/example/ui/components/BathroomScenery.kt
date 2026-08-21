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
import com.example.data.model.HouseRoom
import com.example.data.model.WeatherState
import kotlin.math.sin

/**
 * Banheiro redesenhado (Tamagotchi 2D aconchegante).
 * Esquerda: banheira · Centro livre para o pet · Direita: pia/espelho/armário.
 * Apenas visual — sem alterar mecânicas, banho ou posição do pet.
 *
 * Camadas: 1 parede/piso · 2 janela · 3 móveis/objetos
 */
fun DrawScope.drawBathroomScene(
    w: Float,
    h: Float,
    phase: Float,
    pulse: Float,
    isSleeping: Boolean,
    dayPeriod: DayPeriod = DayPeriod.AFTERNOON,
    weather: WeatherState = WeatherState.CLEAR
) {
    val floorY = h * 0.70f
    val leftW = w * 0.32f
    val rightX = w * 0.68f

    drawBathroomWallAndTiles(w, floorY, isSleeping)
    drawBathroomCeramicFloor(w, h, floorY, isSleeping)

    drawBathroomLeftBath(leftW, floorY, phase, isSleeping)
    drawBathroomRightVanity(w, rightX, floorY, phase, isSleeping)
    drawBathroomCenterDetails(w, leftW, rightX, floorY, isSleeping)

    // Janela por último no Canvas para ficar sempre visível (acima de azulejos/chuveiro)
    drawBathroomVentWindow(w, floorY, phase, pulse, isSleeping, dayPeriod, weather)
    with(OutdoorAmbience) {
        drawIndoorWarmOverlay(
            w, h, dayPeriod, weather,
            apply = IndoorLighting.shouldApplyWarmGlow(HouseRoom.BATHROOM, dayPeriod, isSleeping)
        )
    }
}

// -------------------------------------------------------------------------------------------------
private fun DrawScope.drawBathroomWallAndTiles(w: Float, floorY: Float, isSleeping: Boolean) {
    val wallBrush = if (isSleeping) {
        Brush.verticalGradient(
            listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF164E63)),
            startY = 0f,
            endY = floorY
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFFF0FDFA), Color(0xFFCCFBF1), Color(0xFFE0F2FE)),
            startY = 0f,
            endY = floorY
        )
    }
    drawRect(brush = wallBrush, topLeft = Offset.Zero, size = Size(w, floorY))

    // Azulejos (backsplash / parede)
    val tileTop = floorY * 0.28f
    val tileW = 20f
    val tileH = 14f
    val tileBase = if (isSleeping) Color(0xFF334155) else Color(0xFFF8FAFC)
    val accent = if (isSleeping) Color(0xFF0E7490) else Color(0xFFA5F3FC)
    val grout = if (isSleeping) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    drawRect(
        color = grout,
        topLeft = Offset(0f, tileTop),
        size = Size(w, floorY - tileTop - 10f)
    )

    var ty = tileTop
    var row = 0
    while (ty < floorY - 12f) {
        var tx = if (row % 2 == 0) 0f else -tileW * 0.5f
        var col = 0
        while (tx < w) {
            val isAccent = (row + col) % 7 == 0
            drawRoundRect(
                color = if (isAccent) accent.copy(alpha = 0.85f) else tileBase,
                topLeft = Offset(tx + 1.2f, ty + 1.1f),
                size = Size(tileW - 2.4f, tileH - 2.2f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            if (!isSleeping && !isAccent) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.4f),
                    topLeft = Offset(tx + 3f, ty + 2.2f),
                    size = Size(tileW * 0.32f, 2.5f),
                    cornerRadius = CornerRadius(1f, 1f)
                )
            }
            tx += tileW
            col++
        }
        ty += tileH
        row++
    }

    // Rodapé
    drawRect(
        color = if (isSleeping) Color(0xFF1E293B) else Color(0xFFCBD5E1),
        topLeft = Offset(0f, floorY - 10f),
        size = Size(w, 10f)
    )
}

private fun DrawScope.drawBathroomCeramicFloor(
    w: Float,
    h: Float,
    floorY: Float,
    isSleeping: Boolean
) {
    val light = if (isSleeping) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val dark = if (isSleeping) Color(0xFF0F172A) else Color(0xFFE2E8F0)
    val grout = if (isSleeping) Color(0xFF020617) else Color(0xFFCBD5E1)

    drawRect(color = light, topLeft = Offset(0f, floorY), size = Size(w, h - floorY))

    val tile = 26f
    var row = 0
    var y = floorY
    while (y < h) {
        var col = 0
        var x = 0f
        while (x < w) {
            drawRect(
                color = if ((row + col) % 2 == 0) light else dark,
                topLeft = Offset(x, y),
                size = Size(tile, tile)
            )
            drawRect(
                color = grout.copy(alpha = 0.4f),
                topLeft = Offset(x, y),
                size = Size(tile, tile),
                style = Stroke(width = 1f)
            )
            x += tile
            col++
        }
        y += tile
        row++
    }

    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = if (isSleeping) 0.22f else 0.07f), Color.Transparent)
        ),
        topLeft = Offset(0f, floorY),
        size = Size(w, 12f)
    )
}

private fun DrawScope.drawBathroomVentWindow(
    w: Float,
    floorY: Float,
    phase: Float,
    pulse: Float,
    isSleeping: Boolean,
    dayPeriod: DayPeriod,
    weather: WeatherState
) {
    // Janela de ventilação no canto superior esquerdo (visível; fora do pill/balão e da coluna do chuveiro)
    val winW = (w * 0.20f).coerceIn(64f, 108f)
    val winH = (winW * 0.85f).coerceIn(52f, 90f)
    val winX = (w * 0.10f).coerceIn(36f, 52f) // deslocada da coluna do chuveiro
    val winY = (floorY * 0.05f).coerceIn(8f, 22f)

    // Sombra + moldura
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.14f),
        topLeft = Offset(winX + 2f, winY + 4f),
        size = Size(winW + 8f, winH + 8f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFFFFFFF),
        topLeft = Offset(winX - 5f, winY - 5f),
        size = Size(winW + 10f, winH + 10f),
        cornerRadius = CornerRadius(8f, 8f)
    )

    with(OutdoorAmbience) {
        drawWindowExterior(winX, winY, winW, winH, dayPeriod, weather, phase, pulse)
    }

    // Divisórias (cruz)
    val frame = if (isSleeping) Color(0xFF64748B) else Color.White.copy(alpha = 0.9f)
    drawLine(frame, Offset(winX + winW / 2f, winY), Offset(winX + winW / 2f, winY + winH), 2.2f)
    drawLine(frame, Offset(winX, winY + winH / 2f), Offset(winX + winW, winY + winH / 2f), 2.2f)

    // Cortininha / veneziana leve
    val curtain = if (isSleeping) Color(0xFF475569) else Color(0xFF67E8F9)
    val sway = sin(phase * 1.8f) * 1.2f
    drawRoundRect(
        color = curtain.copy(alpha = 0.55f),
        topLeft = Offset(winX - 4f + sway, winY - 1f),
        size = Size(8f, winH + 4f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawRoundRect(
        color = curtain.copy(alpha = 0.55f),
        topLeft = Offset(winX + winW - 4f - sway, winY - 1f),
        size = Size(8f, winH + 4f),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // Peitoril
    drawRoundRect(
        color = if (isSleeping) Color(0xFF475569) else Color(0xFFE2E8F0),
        topLeft = Offset(winX - 6f, winY + winH + 2f),
        size = Size(winW + 12f, 5f),
        cornerRadius = CornerRadius(2f, 2f)
    )
}

private fun DrawScope.drawBathroomLeftBath(
    leftW: Float,
    floorY: Float,
    phase: Float,
    isSleeping: Boolean
) {
    val margin = leftW * 0.06f
    val tubW = (leftW * 0.88f).coerceIn(95f, 150f)
    val tubH = (floorY * 0.38f).coerceIn(78f, 125f)
    val tubX = margin
    val tubY = floorY - tubH * 0.78f

    // Sombra
    drawOval(
        color = Color.Black.copy(alpha = 0.14f),
        topLeft = Offset(tubX - 2f, floorY - 5f),
        size = Size(tubW + 8f, 12f)
    )

    // Pés da banheira
    drawCircle(Color(0xFFD97706), 5f, Offset(tubX + 14f, floorY + 1f))
    drawCircle(Color(0xFFD97706), 5f, Offset(tubX + tubW - 14f, floorY + 1f))

    // Corpo
    val tubBody = if (isSleeping) Color(0xFF334155) else Color(0xFFF8FAFC)
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(tubBody, if (isSleeping) Color(0xFF1E293B) else Color(0xFFE2E8F0))
        ),
        topLeft = Offset(tubX, tubY),
        size = Size(tubW, tubH * 0.88f),
        cornerRadius = CornerRadius(22f, 22f)
    )
    // Borda
    drawRoundRect(
        color = if (isSleeping) Color(0xFF475569) else Color(0xFFFFFFFF),
        topLeft = Offset(tubX - 3f, tubY - 4f),
        size = Size(tubW + 6f, 9f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Água
    val waterPath = Path().apply {
        moveTo(tubX + 8f, tubY + 8f)
        lineTo(tubX + tubW - 8f, tubY + 8f)
        quadraticBezierTo(
            tubX + tubW - 8f, tubY + tubH * 0.68f,
            tubX + tubW / 2f, tubY + tubH * 0.68f
        )
        quadraticBezierTo(
            tubX + 8f, tubY + tubH * 0.68f,
            tubX + 8f, tubY + 8f
        )
        close()
    }
    drawPath(
        path = waterPath,
        color = Color(0xFF38BDF8).copy(alpha = if (isSleeping) 0.35f else 0.72f)
    )

    // Espuma
    if (!isSleeping) {
        listOf(0.18f, 0.32f, 0.48f, 0.62f, 0.78f).forEachIndexed { i, t ->
            drawCircle(
                Color.White,
                radius = 7f + (i % 3),
                center = Offset(tubX + tubW * t, tubY + 3f + (i % 2))
            )
        }
        val bubblePhase = sin(phase * 2f) * 2.5f
        for (b in 0..4) {
            val bx = tubX + 16f + b * 18f + bubblePhase
            val by = tubY - 10f - b * 6f + sin(phase + b) * 4f
            drawCircle(
                Color(0xFFBAE6FD).copy(alpha = 0.7f),
                radius = 3.5f + (b % 2),
                center = Offset(bx, by),
                style = Stroke(width = 1.3f)
            )
            drawCircle(Color.White.copy(alpha = 0.8f), 1f, Offset(bx - 1f, by - 1f))
        }
    }

    // Chuveiro (acima da banheira, coluna na borda esquerda)
    val showerX = tubX + 8f
    val showerTop = (tubY - 40f).coerceAtLeast(18f)
    drawLine(Color(0xFF94A3B8), Offset(showerX, floorY - 4f), Offset(showerX, showerTop), 3f)
    drawLine(Color(0xFF94A3B8), Offset(showerX, showerTop), Offset(showerX + 22f, showerTop), 3f)
    drawArc(
        color = Color(0xFF64748B),
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(showerX + 14f, showerTop),
        size = Size(16f, 9f)
    )
    if (!isSleeping) {
        for (d in 0..3) {
            val dx = showerX + 18f + d * 3f
            drawLine(
                Color(0xFF7DD3FC).copy(alpha = 0.45f),
                Offset(dx, showerTop + 8f),
                Offset(dx + sin(phase + d) * 1.5f, showerTop + 22f + d * 2f),
                1.1f
            )
        }
    }

    // Saboneteira na borda
    drawRoundRect(
        color = Color(0xFFF9A8D4),
        topLeft = Offset(tubX + tubW - 22f, tubY - 2f),
        size = Size(14f, 6f),
        cornerRadius = CornerRadius(2f, 2f)
    )
}

private fun DrawScope.drawBathroomRightVanity(
    w: Float,
    rightX: Float,
    floorY: Float,
    phase: Float,
    isSleeping: Boolean
) {
    val zoneW = w - rightX
    val margin = zoneW * 0.08f

    // Armário de higiene (alto)
    val cabW = (zoneW * 0.38f).coerceIn(36f, 58f)
    val cabH = (floorY * 0.42f).coerceIn(90f, 140f)
    val cabX = rightX + margin
    val cabY = floorY - cabH

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.12f),
        topLeft = Offset(cabX + 2f, floorY - 4f),
        size = Size(cabW, 7f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    val cabColor = if (isSleeping) Color(0xFF1E3A5F) else Color(0xFF0EA5E9)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(cabColor, cabColor.copy(alpha = 0.85f))),
        topLeft = Offset(cabX, cabY),
        size = Size(cabW, cabH),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawLine(
        Color.White.copy(alpha = 0.4f),
        Offset(cabX + 4f, cabY + cabH * 0.45f),
        Offset(cabX + cabW - 4f, cabY + cabH * 0.45f),
        1.5f
    )
    // Puxadores
    drawCircle(Color(0xFFE2E8F0), 2.2f, Offset(cabX + cabW * 0.5f, cabY + cabH * 0.28f))
    drawCircle(Color(0xFFE2E8F0), 2.2f, Offset(cabX + cabW * 0.5f, cabY + cabH * 0.68f))
    // Frascos no topo do armário
    drawRoundRect(
        color = Color(0xFFA78BFA),
        topLeft = Offset(cabX + 6f, cabY - 14f),
        size = Size(8f, 14f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = Color(0xFF34D399),
        topLeft = Offset(cabX + 18f, cabY - 11f),
        size = Size(7f, 11f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Pia + espelho
    val sinkW = (zoneW * 0.48f).coerceIn(52f, 86f)
    val sinkH = (floorY * 0.26f).coerceIn(48f, 78f)
    val sinkX = (w - margin - sinkW).coerceAtLeast(cabX + cabW + 6f)
    val sinkY = floorY - sinkH

    // Espelho
    val mirrorW = sinkW * 0.72f
    val mirrorH = mirrorW * 1.2f
    val mirrorX = sinkX + (sinkW - mirrorW) / 2f
    val mirrorY = (sinkY - mirrorH - 10f).coerceAtLeast(22f)

    drawOval(
        color = Color.Black.copy(alpha = 0.1f),
        topLeft = Offset(mirrorX + 2f, mirrorY + 3f),
        size = Size(mirrorW + 4f, mirrorH + 4f)
    )
    drawOval(
        color = if (isSleeping) Color(0xFF475569) else Color(0xFFCBD5E1),
        topLeft = Offset(mirrorX - 3f, mirrorY - 3f),
        size = Size(mirrorW + 6f, mirrorH + 6f)
    )
    drawOval(
        brush = Brush.linearGradient(
            listOf(Color(0xFFBAE6FD), Color(0xFFE0F2FE), Color(0xFFFFFFFF))
        ),
        topLeft = Offset(mirrorX, mirrorY),
        size = Size(mirrorW, mirrorH)
    )
    drawLine(
        Color.White.copy(alpha = 0.55f),
        Offset(mirrorX + 6f, mirrorY + 10f),
        Offset(mirrorX + mirrorW - 8f, mirrorY + mirrorH - 14f),
        2.5f
    )

    // Gabinete da pia
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.12f),
        topLeft = Offset(sinkX + 2f, floorY - 4f),
        size = Size(sinkW, 7f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawRoundRect(
        color = if (isSleeping) Color(0xFF1E40AF) else Color(0xFF0284C7),
        topLeft = Offset(sinkX, sinkY),
        size = Size(sinkW, sinkH),
        cornerRadius = CornerRadius(5f, 5f)
    )
    // Tampo / cuba
    drawRoundRect(
        color = if (isSleeping) Color(0xFF64748B) else Color(0xFFF8FAFC),
        topLeft = Offset(sinkX - 2f, sinkY - 5f),
        size = Size(sinkW + 4f, 8f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawRoundRect(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFE0F2FE),
        topLeft = Offset(sinkX + 8f, sinkY - 3f),
        size = Size(sinkW - 16f, 5f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Torneira
    val tapX = sinkX + sinkW * 0.5f
    drawRoundRect(
        color = Color(0xFF94A3B8),
        topLeft = Offset(tapX - 2.5f, sinkY - 18f),
        size = Size(5f, 14f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = Color(0xFFCBD5E1),
        topLeft = Offset(tapX - 7f, sinkY - 20f),
        size = Size(14f, 4f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    if (!isSleeping && sin(phase * 4f) > 0.4f) {
        drawLine(
            Color(0xFF7DD3FC).copy(alpha = 0.5f),
            Offset(tapX, sinkY - 14f),
            Offset(tapX, sinkY + 1f),
            1.2f
        )
    }

    // Toalheiro
    val towelX = sinkX - 20f
    val towelY = sinkY - 8f
    if (towelX > cabX + cabW) {
        drawRoundRect(
            color = Color(0xFF94A3B8),
            topLeft = Offset(towelX, towelY),
            size = Size(16f, 3f),
            cornerRadius = CornerRadius(1f, 1f)
        )
        drawRoundRect(
            color = if (isSleeping) Color(0xFF9F1239) else Color(0xFFEC4899),
            topLeft = Offset(towelX + 2f, towelY + 3f),
            size = Size(12f, 26f),
            cornerRadius = CornerRadius(3f, 3f)
        )
        drawRoundRect(
            color = Color(0xFFF9A8D4).copy(alpha = 0.8f),
            topLeft = Offset(towelX + 4f, towelY + 5f),
            size = Size(8f, 10f),
            cornerRadius = CornerRadius(2f, 2f)
        )
    }

    // Porta-escovas
    val brushX = sinkX + sinkW - 16f
    val brushY = sinkY - 16f
    drawRoundRect(
        color = Color(0xFF67E8F9),
        topLeft = Offset(brushX, brushY + 6f),
        size = Size(12f, 10f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawLine(Color(0xFFEC4899), Offset(brushX + 4f, brushY), Offset(brushX + 4f, brushY + 8f), 2f)
    drawLine(Color(0xFF3B82F6), Offset(brushX + 8f, brushY + 1f), Offset(brushX + 8f, brushY + 8f), 2f)

    // Prateleira na parede (acima da pia, à esquerda do espelho se couber)
    val shelfY = mirrorY + 4f
    val shelfX = cabX + cabW + 4f
    val shelfW = (sinkX - shelfX - 4f).coerceAtLeast(0f)
    if (shelfW > 20f) {
        drawRoundRect(
            color = if (isSleeping) Color(0xFF475569) else Color(0xFFFDBA74),
            topLeft = Offset(shelfX, shelfY),
            size = Size(shelfW, 4f),
            cornerRadius = CornerRadius(1f, 1f)
        )
        drawRoundRect(
            color = Color(0xFFF472B6),
            topLeft = Offset(shelfX + 4f, shelfY - 10f),
            size = Size(7f, 10f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawRoundRect(
            color = Color(0xFF38BDF8),
            topLeft = Offset(shelfX + 14f, shelfY - 8f),
            size = Size(6f, 8f),
            cornerRadius = CornerRadius(2f, 2f)
        )
    }
}

private fun DrawScope.drawBathroomCenterDetails(
    w: Float,
    leftW: Float,
    rightX: Float,
    floorY: Float,
    isSleeping: Boolean
) {
    // Tapete antiderrapante — entre banheira e pia, sem cobrir o centro do pet
    val matW = ((rightX - leftW) * 0.55f).coerceIn(50f, 110f)
    val matH = 14f
    val matX = leftW + ((rightX - leftW) - matW) * 0.5f
    val matY = floorY + 8f
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.08f),
        topLeft = Offset(matX + 1f, matY + 2f),
        size = Size(matW, matH),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = if (isSleeping) Color(0xFF0E7490) else Color(0xFF06B6D4),
        topLeft = Offset(matX, matY),
        size = Size(matW, matH),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = if (isSleeping) Color(0xFF155E75) else Color(0xFF67E8F9),
        topLeft = Offset(matX + 3f, matY + 3f),
        size = Size(matW - 6f, matH - 6f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Planta decorativa (canto esquerdo, junto à banheira)
    val plantX = 8f
    val plantY = floorY - 22f
    drawRoundRect(
        color = Color(0xFF78716C),
        topLeft = Offset(plantX, plantY + 10f),
        size = Size(14f, 12f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    val leafA = if (isSleeping) 0.35f else 0.9f
    drawCircle(Color(0xFF22C55E).copy(alpha = leafA), 7f, Offset(plantX + 7f, plantY + 4f))
    drawCircle(Color(0xFF4ADE80).copy(alpha = leafA), 5f, Offset(plantX + 2f, plantY + 7f))
    drawCircle(Color(0xFF16A34A).copy(alpha = leafA), 5f, Offset(plantX + 12f, plantY + 7f))

    // Prateleira baixa decorativa no lado direito do centro (não cobre o pet)
    val shelfX = rightX - 36f
    if (shelfX > leftW + 20f) {
        drawRoundRect(
            color = if (isSleeping) Color(0xFF334155) else Color(0xFFFDE68A),
            topLeft = Offset(shelfX, floorY - 38f),
            size = Size(28f, 4f),
            cornerRadius = CornerRadius(1f, 1f)
        )
        drawRoundRect(
            color = Color(0xFFFB7185),
            topLeft = Offset(shelfX + 4f, floorY - 48f),
            size = Size(8f, 10f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawRoundRect(
            color = Color(0xFF2DD4BF),
            topLeft = Offset(shelfX + 15f, floorY - 46f),
            size = Size(7f, 8f),
            cornerRadius = CornerRadius(2f, 2f)
        )
    }
}
