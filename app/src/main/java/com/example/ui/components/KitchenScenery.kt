package com.example.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.DayPeriod
import com.example.data.model.HouseRoom
import com.example.data.model.WeatherState
import kotlin.math.sin

/**
 * Cozinha redesenhada (Tamagotchi 2D moderno).
 * Layout: 30% esquerda | centro livre para o pet | 30% direita.
 *
 * Camadas Canvas (z-order de desenho):
 * 1) parede + piso  2) janela  3) móveis/objetos
 * (Pet e balão são Compose acima do Canvas.)
 */
fun DrawScope.drawKitchenScene(
    w: Float,
    h: Float,
    phase: Float,
    pulse: Float,
    isSleeping: Boolean,
    dayPeriod: DayPeriod = DayPeriod.AFTERNOON,
    weather: WeatherState = WeatherState.CLEAR
) {
    val floorY = h * 0.70f
    val leftW = w * 0.30f
    val rightX = w * 0.70f

    // Camada 1 — parede e piso
    drawKitchenWallAndTiles(w, floorY, isSleeping)
    drawKitchenCeramicFloor(w, h, floorY, isSleeping)

    // Camada 2 — janela (canto superior esquerdo; fora da faixa do balão central)
    drawKitchenWindow(w, floorY, phase, pulse, isSleeping, dayPeriod, weather)

    // Camada 3 — móveis e objetos (esquerda / direita; centro livre)
    drawKitchenLeftFurniture(leftW, floorY, phase, isSleeping)
    drawKitchenRightFurniture(w, rightX, floorY, phase, isSleeping)
    drawKitchenSideFloorDetails(w, leftW, rightX, floorY, isSleeping)
    with(OutdoorAmbience) {
        drawIndoorWarmOverlay(
            w, h, dayPeriod, weather,
            apply = IndoorLighting.shouldApplyWarmGlow(HouseRoom.KITCHEN, dayPeriod, isSleeping)
        )
    }
}

// -------------------------------------------------------------------------------------------------
private fun DrawScope.drawKitchenWallAndTiles(w: Float, floorY: Float, isSleeping: Boolean) {
    val wallBrush = if (isSleeping) {
        Brush.verticalGradient(
            listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A)),
            startY = 0f,
            endY = floorY
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7), Color(0xFFE0F2FE)),
            startY = 0f,
            endY = floorY
        )
    }
    drawRect(brush = wallBrush, topLeft = Offset.Zero, size = Size(w, floorY))

    // Revestimento / backsplash (azulejos brancos com junta suave)
    val backsplashTop = floorY * 0.42f
    val tileW = 22f
    val tileH = 14f
    val tileBase = if (isSleeping) Color(0xFF334155) else Color(0xFFF8FAFC)
    val grout = if (isSleeping) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    drawRect(
        color = grout,
        topLeft = Offset(0f, backsplashTop),
        size = Size(w, floorY - backsplashTop - 10f)
    )

    var ty = backsplashTop
    var row = 0
    while (ty < floorY - 12f) {
        var tx = if (row % 2 == 0) 0f else -tileW * 0.5f
        while (tx < w) {
            drawRoundRect(
                color = tileBase.copy(alpha = if (isSleeping) 0.85f else 1f),
                topLeft = Offset(tx + 1.5f, ty + 1.2f),
                size = Size(tileW - 3f, tileH - 2.4f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            // Brilho sutil no azulejo
            if (!isSleeping) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(tx + 3f, ty + 2.5f),
                    size = Size(tileW * 0.35f, 3f),
                    cornerRadius = CornerRadius(1f, 1f)
                )
            }
            tx += tileW
        }
        ty += tileH
        row++
    }

    // Rodapé
    drawRect(
        color = if (isSleeping) Color(0xFF1E293B) else Color(0xFFD6D3D1),
        topLeft = Offset(0f, floorY - 10f),
        size = Size(w, 10f)
    )
}

private fun DrawScope.drawKitchenCeramicFloor(
    w: Float,
    h: Float,
    floorY: Float,
    isSleeping: Boolean
) {
    val light = if (isSleeping) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val dark = if (isSleeping) Color(0xFF0F172A) else Color(0xFFE2E8F0)
    val grout = if (isSleeping) Color(0xFF020617) else Color(0xFFCBD5E1)

    drawRect(color = light, topLeft = Offset(0f, floorY), size = Size(w, h - floorY))

    val tile = 28f
    var row = 0
    var y = floorY
    while (y < h) {
        var col = 0
        var x = 0f
        while (x < w) {
            val checker = (row + col) % 2 == 0
            drawRect(
                color = if (checker) light else dark,
                topLeft = Offset(x, y),
                size = Size(tile, tile)
            )
            drawRect(
                color = grout.copy(alpha = 0.45f),
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

    // Sombra suave do rodapé no piso
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = if (isSleeping) 0.25f else 0.08f), Color.Transparent)
        ),
        topLeft = Offset(0f, floorY),
        size = Size(w, 14f)
    )
}

private fun DrawScope.drawKitchenWindow(
    w: Float,
    floorY: Float,
    phase: Float,
    pulse: Float,
    isSleeping: Boolean,
    dayPeriod: DayPeriod,
    weather: WeatherState
) {
    // Canto superior esquerdo — acima da pia (à direita da geladeira), fora do balão central
    val winW = (w * 0.16f).coerceIn(52f, 88f)
    val winH = (winW * 0.88f).coerceIn(46f, 78f)
    val leftW = w * 0.30f
    val fridgeApproxW = (leftW * 0.42f).coerceIn(48f, 78f)
    val winX = (leftW * 0.06f + fridgeApproxW + 4f)
        .coerceAtMost(leftW - winW - 4f)
        .coerceAtLeast((w * 0.04f).coerceIn(8f, 18f))
    val winY = (floorY * 0.045f).coerceIn(8f, 20f)

    // Moldura com sombra
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.12f),
        topLeft = Offset(winX - 2f, winY + 3f),
        size = Size(winW + 10f, winH + 10f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFFFF7ED),
        topLeft = Offset(winX - 5f, winY - 5f),
        size = Size(winW + 10f, winH + 10f),
        cornerRadius = CornerRadius(8f, 8f)
    )

    with(OutdoorAmbience) {
        drawWindowExterior(winX, winY, winW, winH, dayPeriod, weather, phase, pulse)
    }

    // Divisórias
    val frame = if (isSleeping) Color(0xFF64748B) else Color.White.copy(alpha = 0.85f)
    drawLine(frame, Offset(winX + winW / 2f, winY), Offset(winX + winW / 2f, winY + winH), 2f)
    drawLine(frame, Offset(winX, winY + winH / 2f), Offset(winX + winW, winY + winH / 2f), 2f)

    // Cortina leve (lados)
    val curtain = if (isSleeping) Color(0xFF475569) else Color(0xFFFDA4AF)
    val sway = sin(phase * 2f) * 1.2f
    drawRoundRect(
        color = curtain.copy(alpha = 0.75f),
        topLeft = Offset(winX - 6f + sway, winY - 2f),
        size = Size(9f, winH + 5f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawRoundRect(
        color = curtain.copy(alpha = 0.75f),
        topLeft = Offset(winX + winW - 3f - sway, winY - 2f),
        size = Size(9f, winH + 5f),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // Feixe de luz na zona esquerda (não invade o centro do pet)
    if (!isSleeping && dayPeriod != DayPeriod.NIGHT && weather != WeatherState.RAIN) {
        val lightAlpha = 0.07f + pulse * 0.03f
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFFFFFBEB).copy(alpha = lightAlpha), Color.Transparent)
            ),
            topLeft = Offset(winX - 6f, winY + winH),
            size = Size(winW + 12f, (floorY - (winY + winH)) * 0.55f)
        )
    }
}

private fun DrawScope.drawKitchenLeftFurniture(
    leftW: Float,
    floorY: Float,
    phase: Float,
    isSleeping: Boolean
) {
    val margin = leftW * 0.06f
    val fridgeW = (leftW * 0.42f).coerceIn(48f, 78f)
    val fridgeH = (floorY * 0.62f).coerceIn(110f, 175f)
    val fridgeX = margin
    val fridgeY = floorY - fridgeH

    // Sombra da geladeira
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.14f),
        topLeft = Offset(fridgeX + 4f, floorY - 6f),
        size = Size(fridgeW, 10f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Corpo geladeira
    val fridgeBody = if (isSleeping) Color(0xFF334155) else Color(0xFFF1F5F9)
    val fridgeEdge = if (isSleeping) Color(0xFF1E293B) else Color(0xFF94A3B8)
    drawRoundRect(
        brush = Brush.horizontalGradient(
            listOf(fridgeBody, if (isSleeping) Color(0xFF475569) else Color(0xFFE2E8F0))
        ),
        topLeft = Offset(fridgeX, fridgeY),
        size = Size(fridgeW, fridgeH),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = fridgeEdge.copy(alpha = 0.5f),
        topLeft = Offset(fridgeX, fridgeY),
        size = Size(fridgeW, fridgeH),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = 1.5f)
    )

    // Freezer / refrigerador
    val splitY = fridgeY + fridgeH * 0.32f
    drawLine(fridgeEdge, Offset(fridgeX + 4f, splitY), Offset(fridgeX + fridgeW - 4f, splitY), 2.5f)

    // Display / LED
    if (!isSleeping) {
        drawRoundRect(
            color = Color(0xFF22C55E).copy(alpha = 0.85f),
            topLeft = Offset(fridgeX + 8f, fridgeY + 8f),
            size = Size(10f, 4f),
            cornerRadius = CornerRadius(1f, 1f)
        )
    }

    // Puxadores
    drawRoundRect(
        color = Color(0xFF64748B),
        topLeft = Offset(fridgeX + fridgeW - 10f, fridgeY + fridgeH * 0.12f),
        size = Size(4f, 20f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = Color(0xFF64748B),
        topLeft = Offset(fridgeX + fridgeW - 10f, fridgeY + fridgeH * 0.42f),
        size = Size(4f, 32f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Ímãs
    drawCircle(Color(0xFFEF4444), 3.5f, Offset(fridgeX + fridgeW * 0.28f, fridgeY + fridgeH * 0.14f))
    drawCircle(Color(0xFFF59E0B), 3f, Offset(fridgeX + fridgeW * 0.55f, fridgeY + fridgeH * 0.18f))
    drawCircle(Color(0xFF3B82F6), 2.8f, Offset(fridgeX + fridgeW * 0.4f, fridgeY + fridgeH * 0.22f))

    // Bancada + armários (resto do 30% esquerdo)
    val counterX = fridgeX + fridgeW + 6f
    val counterW = (leftW - counterX - margin).coerceAtLeast(40f)
    val counterH = (floorY * 0.28f).coerceIn(55f, 88f)
    val counterY = floorY - counterH

    // Sem armário superior esquerdo sobre a pia: a janela ocupa esse espaço
    // (armário superior só se couber abaixo da janela — omitido de propósito)

    // Armários inferiores / base
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.12f),
        topLeft = Offset(counterX + 3f, floorY - 5f),
        size = Size(counterW, 8f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    val lowerBlue = if (isSleeping) Color(0xFF1E40AF) else Color(0xFF0369A1)
    drawRoundRect(
        color = lowerBlue,
        topLeft = Offset(counterX, counterY),
        size = Size(counterW, counterH),
        cornerRadius = CornerRadius(5f, 5f)
    )
    drawLine(
        Color.White.copy(alpha = 0.3f),
        Offset(counterX + counterW * 0.5f, counterY + 8f),
        Offset(counterX + counterW * 0.5f, floorY - 8f),
        1.5f
    )

    // Tampo
    drawRoundRect(
        color = if (isSleeping) Color(0xFF64748B) else Color(0xFFF8FAFC),
        topLeft = Offset(counterX - 2f, counterY - 5f),
        size = Size(counterW + 4f, 7f),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // Pia + torneira (sobre a bancada esquerda)
    val sinkW = (counterW * 0.55f).coerceIn(28f, 48f)
    val sinkH = 10f
    val sinkX = counterX + (counterW - sinkW) * 0.45f
    val sinkY = counterY - 3f
    drawRoundRect(
        color = if (isSleeping) Color(0xFF475569) else Color(0xFFCBD5E1),
        topLeft = Offset(sinkX, sinkY),
        size = Size(sinkW, sinkH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = if (isSleeping) Color(0xFF1E293B) else Color(0xFF94A3B8),
        topLeft = Offset(sinkX + 3f, sinkY + 2f),
        size = Size(sinkW - 6f, sinkH - 4f),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // Torneira
    val tapX = sinkX + sinkW * 0.5f
    val tapBaseY = sinkY - 2f
    drawRoundRect(
        color = Color(0xFF64748B),
        topLeft = Offset(tapX - 3f, tapBaseY - 16f),
        size = Size(6f, 16f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = Color(0xFF94A3B8),
        topLeft = Offset(tapX - 8f, tapBaseY - 18f),
        size = Size(16f, 5f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    if (!isSleeping) {
        val drip = sin(phase * 5f)
        if (drip > 0.3f) {
            drawLine(
                Color(0xFF7DD3FC).copy(alpha = 0.55f),
                Offset(tapX, tapBaseY - 12f),
                Offset(tapX, sinkY + 4f),
                1.2f
            )
        }
    }

    // Esponja / detalhe
    drawRoundRect(
        color = Color(0xFFF472B6),
        topLeft = Offset(sinkX + sinkW - 10f, sinkY - 5f),
        size = Size(8f, 5f),
        cornerRadius = CornerRadius(1.5f, 1.5f)
    )
}

private fun DrawScope.drawKitchenRightFurniture(
    w: Float,
    rightX: Float,
    floorY: Float,
    phase: Float,
    isSleeping: Boolean
) {
    val zoneW = w - rightX
    val margin = zoneW * 0.08f

    // Fogão com forno
    val stoveW = (zoneW * 0.42f).coerceIn(48f, 72f)
    val stoveH = (floorY * 0.30f).coerceIn(58f, 92f)
    val stoveX = rightX + margin
    val stoveY = floorY - stoveH

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.14f),
        topLeft = Offset(stoveX + 3f, floorY - 5f),
        size = Size(stoveW, 8f),
        cornerRadius = CornerRadius(3f, 3f)
    )

    val stoveBody = if (isSleeping) Color(0xFF1E293B) else Color(0xFF334155)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF475569), stoveBody)),
        topLeft = Offset(stoveX, stoveY),
        size = Size(stoveW, stoveH),
        cornerRadius = CornerRadius(6f, 6f)
    )

    // Cooktop (4 bocas)
    val burnerR = 5.5f
    val bx1 = stoveX + stoveW * 0.30f
    val bx2 = stoveX + stoveW * 0.70f
    val by1 = stoveY + 12f
    val by2 = stoveY + 26f
    listOf(
        Offset(bx1, by1), Offset(bx2, by1),
        Offset(bx1, by2), Offset(bx2, by2)
    ).forEach { c ->
        drawCircle(Color(0xFF0F172A), burnerR + 1.5f, c)
        drawCircle(Color(0xFF64748B), burnerR, c, style = Stroke(1.5f))
        if (!isSleeping) {
            drawCircle(Color(0xFFF97316).copy(alpha = 0.35f + sin(phase * 3f) * 0.1f), 2.2f, c)
        }
    }

    // Painel de botões
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(stoveX + 6f, stoveY + 36f),
        size = Size(stoveW - 12f, 8f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    for (i in 0..3) {
        drawCircle(
            Color(0xFF94A3B8),
            2f,
            Offset(stoveX + 14f + i * ((stoveW - 28f) / 3f), stoveY + 40f)
        )
    }

    // Forno (vidro)
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(stoveX + 8f, stoveY + 48f),
        size = Size(stoveW - 16f, stoveH - 56f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    if (!isSleeping) {
        drawRoundRect(
            color = Color(0xFFF59E0B).copy(alpha = 0.25f + pulseGlow(phase) * 0.15f),
            topLeft = Offset(stoveX + 10f, stoveY + 50f),
            size = Size(stoveW - 20f, stoveH - 60f),
            cornerRadius = CornerRadius(3f, 3f)
        )
    }
    // Puxador do forno
    drawRoundRect(
        color = Color(0xFF94A3B8),
        topLeft = Offset(stoveX + stoveW * 0.25f, stoveY + 50f),
        size = Size(stoveW * 0.5f, 3f),
        cornerRadius = CornerRadius(1.5f, 1.5f)
    )

    // Panela no fogão
    val potX = bx2 - 7f
    val potY = by1 - 10f
    drawRoundRect(
        color = Color(0xFFEF4444),
        topLeft = Offset(potX, potY),
        size = Size(14f, 9f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawArc(
        color = Color(0xFFEF4444),
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(potX - 1f, potY - 5f),
        size = Size(16f, 10f),
        style = Stroke(width = 2f)
    )
    if (!isSleeping) {
        val steam = sin(phase * 4f) * 2f
        drawLine(
            Color.White.copy(alpha = 0.55f),
            Offset(potX + 7f, potY - 2f),
            Offset(potX + 7f + steam, potY - 12f),
            1.4f
        )
    }

    // Mesa pequena + fruteira (lado direito, sem invadir centro)
    val tableW = (zoneW * 0.48f).coerceIn(52f, 88f)
    val tableH = 36f
    val tableX = (w - margin - tableW).coerceAtLeast(rightX + stoveW + margin + 4f)
    val tableY = floorY - tableH

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.12f),
        topLeft = Offset(tableX + 2f, floorY - 4f),
        size = Size(tableW, 7f),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // Pés
    drawLine(Color(0xFF451A03), Offset(tableX + 6f, tableY + 6f), Offset(tableX + 6f, floorY), 3.5f)
    drawLine(Color(0xFF451A03), Offset(tableX + tableW - 6f, tableY + 6f), Offset(tableX + tableW - 6f, floorY), 3.5f)

    // Tampo
    drawRoundRect(
        color = Color(0xFF92400E),
        topLeft = Offset(tableX, tableY),
        size = Size(tableW, 7f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    // Toalhinha
    drawRoundRect(
        color = Color(0xFFFB7185).copy(alpha = 0.9f),
        topLeft = Offset(tableX + 8f, tableY - 1f),
        size = Size(tableW - 16f, 8f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Fruteira
    val bowlX = tableX + tableW * 0.5f - 14f
    val bowlY = tableY - 12f
    drawOval(
        color = Color(0xFF78350F),
        topLeft = Offset(bowlX, bowlY + 6f),
        size = Size(28f, 8f)
    )
    drawOval(
        color = Color(0xFFA16207),
        topLeft = Offset(bowlX + 2f, bowlY + 5f),
        size = Size(24f, 6f)
    )
    // Frutas
    drawCircle(Color(0xFFEF4444), 5f, Offset(bowlX + 10f, bowlY + 3f))
    drawCircle(Color(0xFFFBBF24), 4.5f, Offset(bowlX + 18f, bowlY + 2f))
    drawCircle(Color(0xFF22C55E), 4f, Offset(bowlX + 14f, bowlY - 1f))

    // Sem armário superior direito: a janela ocupa o canto superior direito
}

private fun DrawScope.drawKitchenSideFloorDetails(
    w: Float,
    leftW: Float,
    rightX: Float,
    floorY: Float,
    isSleeping: Boolean
) {
    // Tapetinho só na esquerda (não no centro)
    val matW = leftW * 0.55f
    val matH = 10f
    val matX = leftW * 0.2f
    drawOval(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFFDA4AF).copy(alpha = 0.55f),
        topLeft = Offset(matX, floorY + 6f),
        size = Size(matW, matH)
    )

    // Tigela do pet encostada na zona direita (fora do centro 30–70%)
    val bowlX = rightX + 8f
    val bowlY = floorY + 4f
    drawOval(Color(0x30000000), Offset(bowlX - 1f, bowlY + 7f), Size(28f, 6f))
    drawRoundRect(
        color = Color(0xFFF97316),
        topLeft = Offset(bowlX, bowlY),
        size = Size(26f, 10f),
        cornerRadius = CornerRadius(5f, 5f)
    )
    drawOval(Color(0xFFFEF08A), Offset(bowlX + 4f, bowlY + 1f), Size(18f, 4f))

    // Planta minúscula no canto esquerdo (decor)
    val plantX = 10f
    val plantY = floorY - 18f
    drawRoundRect(
        color = Color(0xFF78350F),
        topLeft = Offset(plantX, plantY + 8f),
        size = Size(12f, 10f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawCircle(Color(0xFF22C55E).copy(alpha = if (isSleeping) 0.4f else 0.9f), 6f, Offset(plantX + 6f, plantY + 4f))
    drawCircle(Color(0xFF16A34A).copy(alpha = if (isSleeping) 0.35f else 0.85f), 4.5f, Offset(plantX + 2f, plantY + 6f))
    drawCircle(Color(0xFF4ADE80).copy(alpha = if (isSleeping) 0.35f else 0.85f), 4f, Offset(plantX + 10f, plantY + 6f))
}

private fun pulseGlow(phase: Float): Float = (sin(phase) + 1f) * 0.5f
