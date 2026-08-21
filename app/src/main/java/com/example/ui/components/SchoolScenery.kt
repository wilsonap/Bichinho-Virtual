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
import kotlin.math.sin

/**
 * Cenário simples da sala de aula: quadro, carteira, livros e mochila.
 */
fun DrawScope.drawSchoolScene(
    w: Float,
    h: Float,
    phase: Float,
    pulse: Float,
    isSleeping: Boolean,
    dayPeriod: DayPeriod = DayPeriod.AFTERNOON,
    weather: WeatherState = WeatherState.CLEAR
) {
    val floorY = h * 0.68f
    // Escola: não escurece pelo DayPeriod.NIGHT — janela mostra o exterior
    val dim = isSleeping

    // Parede bege/sala
    drawRect(
        brush = Brush.verticalGradient(
            if (dim) listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF475569))
            else listOf(Color(0xFFFFF7ED), Color(0xFFFDE68A), Color(0xFFFCD34D)),
            startY = 0f,
            endY = floorY
        ),
        topLeft = Offset(0f, 0f),
        size = Size(w, floorY)
    )

    // Janela lateral da escola
    val sWinW = (w * 0.18f).coerceIn(55f, 90f)
    val sWinH = sWinW * 1.1f
    val sWinX = w * 0.78f
    val sWinY = h * 0.08f
    drawRoundRect(
        color = if (dim) Color(0xFF334155) else Color(0xFFFFFFFF),
        topLeft = Offset(sWinX - 3f, sWinY - 3f),
        size = Size(sWinW + 6f, sWinH + 6f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    with(OutdoorAmbience) {
        drawWindowExterior(sWinX, sWinY, sWinW, sWinH, dayPeriod, weather, phase, pulse)
    }

    // Piso madeira
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFFD6A36A), Color(0xFFB45309), Color(0xFF92400E)),
            startY = floorY,
            endY = h
        ),
        topLeft = Offset(0f, floorY),
        size = Size(w, h - floorY)
    )

    // Quadro negro
    val boardW = w * 0.55f
    val boardH = h * 0.28f
    val boardX = (w - boardW) / 2f
    val boardY = h * 0.08f
    drawRoundRect(
        color = Color(0xFF78350F),
        topLeft = Offset(boardX - 8f, boardY - 8f),
        size = Size(boardW + 16f, boardH + 16f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = Color(0xFF14532D),
        topLeft = Offset(boardX, boardY),
        size = Size(boardW, boardH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Giz "ABC"
    drawLine(
        color = Color.White.copy(alpha = 0.85f),
        start = Offset(boardX + boardW * 0.15f, boardY + boardH * 0.45f),
        end = Offset(boardX + boardW * 0.35f, boardY + boardH * 0.45f),
        strokeWidth = 3f
    )
    drawLine(
        color = Color.White.copy(alpha = 0.7f),
        start = Offset(boardX + boardW * 0.40f, boardY + boardH * 0.55f),
        end = Offset(boardX + boardW * 0.70f, boardY + boardH * 0.55f),
        strokeWidth = 2.5f
    )
    drawCircle(
        color = Color(0xFFFDE047).copy(alpha = 0.35f + pulse * 0.25f),
        radius = 6f,
        center = Offset(boardX + boardW * 0.82f, boardY + boardH * 0.35f)
    )

    // Carteira
    val deskW = w * 0.42f
    val deskH = h * 0.10f
    val deskX = (w - deskW) / 2f
    val deskY = floorY - deskH - h * 0.02f
    drawRoundRect(
        color = Color(0xFF92400E),
        topLeft = Offset(deskX, deskY),
        size = Size(deskW, deskH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = Color(0xFFB45309),
        topLeft = Offset(deskX + 4f, deskY + 3f),
        size = Size(deskW - 8f, deskH * 0.35f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    // Pernas
    drawLine(Color(0xFF78350F), Offset(deskX + 10f, deskY + deskH), Offset(deskX + 10f, floorY), 4f)
    drawLine(Color(0xFF78350F), Offset(deskX + deskW - 10f, deskY + deskH), Offset(deskX + deskW - 10f, floorY), 4f)

    // Livros na carteira
    val bookColors = listOf(Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFF10B981))
    bookColors.forEachIndexed { i, c ->
        drawRoundRect(
            color = c,
            topLeft = Offset(deskX + deskW * 0.12f + i * 18f, deskY - 14f),
            size = Size(14f, 14f),
            cornerRadius = CornerRadius(2f, 2f)
        )
    }

    // Mochila à esquerda
    val bagX = w * 0.12f
    val bagY = floorY - h * 0.16f
    drawRoundRect(
        color = Color(0xFF2563EB),
        topLeft = Offset(bagX, bagY),
        size = Size(w * 0.14f, h * 0.14f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = Color(0xFFFBBF24),
        topLeft = Offset(bagX + w * 0.03f, bagY + h * 0.04f),
        size = Size(w * 0.08f, h * 0.06f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Alça
    drawPath(
        Path().apply {
            moveTo(bagX + 8f, bagY + 6f)
            quadraticTo(bagX - 6f, bagY - 10f, bagX + w * 0.07f, bagY + 4f)
        },
        color = Color(0xFF1D4ED8),
        style = Stroke(width = 4f)
    )

    // Lousa lateral / estante de livros à direita
    val shelfX = w * 0.78f
    drawRoundRect(
        color = Color(0xFF78350F),
        topLeft = Offset(shelfX, floorY - h * 0.32f),
        size = Size(w * 0.16f, h * 0.32f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    for (row in 0..2) {
        val sy = floorY - h * 0.32f + (row + 1) * (h * 0.09f)
        drawRect(Color(0xFFB45309), Offset(shelfX + 4f, sy), Size(w * 0.14f, 3f))
        drawRoundRect(
            color = listOf(Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFFEC4899))[row],
            topLeft = Offset(shelfX + 10f, sy - 18f),
            size = Size(12f, 18f),
            cornerRadius = CornerRadius(2f, 2f)
        )
    }

    // Partículas de "estudo" (pontinhos flutuando)
    val bob = sin(phase) * 6f
    drawCircle(
        Color(0xFF38BDF8).copy(alpha = 0.5f + pulse * 0.3f),
        radius = 4f,
        center = Offset(w * 0.5f, deskY - 28f + bob)
    )
    drawCircle(
        Color(0xFFF472B6).copy(alpha = 0.45f),
        radius = 3f,
        center = Offset(w * 0.58f, deskY - 40f - bob)
    )

    if (isSleeping) {
        drawRect(Color(0x330F172A), Offset(0f, 0f), Size(w, h))
    }
    with(OutdoorAmbience) {
        drawIndoorWarmOverlay(
            w, h, dayPeriod, weather,
            apply = !isSleeping && (dayPeriod == DayPeriod.EVENING || dayPeriod == DayPeriod.NIGHT)
        )
    }
}
