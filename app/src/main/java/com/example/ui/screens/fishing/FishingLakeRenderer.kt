package com.example.ui.screens.fishing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.DayPeriod
import com.example.data.model.WeatherState
import com.example.game.fishing.FishingGameEngine
import com.example.ui.components.OutdoorAmbience
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cenário estático/animado do lago — só desenho, sem lógica de jogo.
 * Cores de céu/astro via [OutdoorAmbience.theme] (DayPeriod + Weather).
 */
object FishingLakeRenderer {

    fun DrawScope.drawLakeScene(
        w: Float,
        h: Float,
        phase: Float,
        period: DayPeriod,
        weather: WeatherState
    ) {
        val theme = OutdoorAmbience.theme(period, weather)
        val surfaceY = FishingGameEngine.SURFACE_Y * h

        drawSky(w, surfaceY, phase, period, theme)
        drawDistantTree(w * 0.08f, surfaceY - h * 0.02f, h * 0.12f)
        drawShore(w, h, surfaceY)
        drawReeds(w, surfaceY, phase)
        drawWater(w, h, surfaceY, phase, theme)
        drawUnderwaterDecor(w, h, surfaceY)
    }

    private fun DrawScope.drawSky(
        w: Float,
        surfaceY: Float,
        phase: Float,
        period: DayPeriod,
        theme: OutdoorAmbience.Theme
    ) {
        drawRect(
            brush = Brush.verticalGradient(theme.skyColors, startY = 0f, endY = surfaceY),
            topLeft = Offset.Zero,
            size = Size(w, surfaceY)
        )

        if (theme.showStars) {
            for (i in 0..10) {
                val sx = w * (0.06f + (i * 0.09f) % 0.88f)
                val sy = surfaceY * (0.10f + (i % 5) * 0.12f)
                val twinkle = 0.35f + 0.45f * ((sin(phase * 2f + i) + 1f) * 0.5f)
                drawCircle(Color.White.copy(alpha = twinkle), 1.8f, Offset(sx, sy))
            }
        }

        if (theme.showMoon) {
            val mx = w * 0.82f
            val my = surfaceY * 0.28f
            drawCircle(Color(0xFFFEF08A), 16f, Offset(mx, my))
            drawCircle(Color(0xFF0F172A).copy(alpha = 0.45f), 13f, Offset(mx - 5f, my - 2f))
        }

        if (theme.showSun) {
            val sunY = if (theme.sunLow) surfaceY * 0.55f else surfaceY * 0.28f
            val sunC = Offset(w * 0.84f, sunY)
            val sunCol =
                if (period == DayPeriod.EVENING) Color(0xFFFB923C) else Color(0xFFFBBF24)
            drawCircle(Color(0xFFFDE047).copy(alpha = 0.28f), 28f, sunC)
            drawCircle(sunCol, 16f, sunC)
        }

        if (theme.showClouds) {
            val gray = theme.showRain || (!theme.showSun && !theme.showMoon)
            val c = if (gray) Color(0xFF94A3B8).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.88f)
            val c1 = ((phase * 8f) % (w + 70f)) - 35f
            drawCloudBlob(c1, surfaceY * 0.22f, 1f, c)
            val c2 = ((phase * 5f + w * 0.45f) % (w + 60f)) - 30f
            drawCloudBlob(c2, surfaceY * 0.38f, 0.75f, c.copy(alpha = c.alpha * 0.85f))
        }

        if (theme.showRain) {
            for (i in 0..16) {
                val rx = w * ((i * 0.06f + phase * 0.08f) % 1f)
                val ry = surfaceY * ((i * 0.11f + phase * 0.2f) % 0.95f)
                drawLine(
                    Color(0xFFBAE6FD).copy(alpha = 0.45f),
                    Offset(rx, ry),
                    Offset(rx + 2f, ry + 12f),
                    strokeWidth = 1.4f
                )
            }
        }
    }

    private fun DrawScope.drawCloudBlob(cx: Float, cy: Float, scale: Float, color: Color) {
        drawCircle(color, 11f * scale, Offset(cx, cy))
        drawCircle(color, 15f * scale, Offset(cx + 13f * scale, cy - 3f * scale))
        drawCircle(color, 12f * scale, Offset(cx + 26f * scale, cy))
    }

    private fun DrawScope.drawDistantTree(x: Float, baseY: Float, height: Float) {
        drawRect(Color(0xFF78350F), Offset(x - 4f, baseY - height * 0.45f), Size(8f, height * 0.45f))
        drawCircle(Color(0xFF166534), height * 0.28f, Offset(x, baseY - height * 0.55f))
        drawCircle(Color(0xFF15803D), height * 0.22f, Offset(x - height * 0.12f, baseY - height * 0.42f))
        drawCircle(Color(0xFF22C55E), height * 0.20f, Offset(x + height * 0.12f, baseY - height * 0.42f))
    }

    private fun DrawScope.drawShore(w: Float, h: Float, surfaceY: Float) {
        val bankTop = surfaceY - h * 0.10f
        // Grama
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF86EFAC), Color(0xFF4ADE80), Color(0xFF22C55E)),
                startY = bankTop,
                endY = surfaceY
            ),
            topLeft = Offset(0f, bankTop),
            size = Size(w, surfaceY - bankTop)
        )
        // Barranco / terra
        drawRect(
            color = Color(0xFFA16207).copy(alpha = 0.55f),
            topLeft = Offset(0f, surfaceY - h * 0.018f),
            size = Size(w, h * 0.018f)
        )
        // Pedras na margem
        drawCircle(Color(0xFF78716C), 7f, Offset(w * 0.42f, surfaceY - 6f))
        drawCircle(Color(0xFFA8A29E), 5f, Offset(w * 0.46f, surfaceY - 4f))
        drawCircle(Color(0xFF57534E), 6f, Offset(w * 0.78f, surfaceY - 5f))
        // Plantinhas
        for (i in 0..5) {
            val px = w * (0.55f + i * 0.06f)
            drawLine(Color(0xFF166534), Offset(px, surfaceY - 2f), Offset(px, surfaceY - 14f), 2f)
            drawCircle(Color(0xFF4ADE80), 3f, Offset(px, surfaceY - 15f))
        }
    }

    private fun DrawScope.drawReeds(w: Float, surfaceY: Float, phase: Float) {
        val sway = sin(phase) * 3f
        for (i in 0..4) {
            val rx = w * (0.62f + i * 0.05f)
            val tip = Offset(rx + sway * (0.5f + i * 0.1f), surfaceY - 28f - i * 2f)
            drawLine(Color(0xFF365314), Offset(rx, surfaceY + 2f), tip, 2.5f)
            drawOval(Color(0xFF3F6212), Offset(tip.x - 4f, tip.y - 8f), Size(10f, 14f))
        }
        for (i in 0..2) {
            val rx = w * (0.08f + i * 0.04f)
            drawLine(Color(0xFF365314), Offset(rx, surfaceY + 2f), Offset(rx + sway, surfaceY - 22f), 2.2f)
        }
    }

    private fun DrawScope.drawWater(
        w: Float,
        h: Float,
        surfaceY: Float,
        phase: Float,
        theme: OutdoorAmbience.Theme
    ) {
        val deep = if (theme.showMoon || theme.outdoorDim > 0.1f) {
            listOf(Color(0xFF075985), Color(0xFF0C4A6E), Color(0xFF082F49))
        } else {
            listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0C4A6E))
        }
        drawRect(
            brush = Brush.verticalGradient(deep, startY = surfaceY, endY = h),
            topLeft = Offset(0f, surfaceY),
            size = Size(w, h - surfaceY)
        )
        // Linha da superfície
        drawLine(
            Color.White.copy(alpha = 0.45f),
            Offset(0f, surfaceY),
            Offset(w, surfaceY),
            strokeWidth = 2.5f
        )
        // Ondas
        val wavePath = Path()
        wavePath.moveTo(0f, surfaceY + 4f)
        var x = 0f
        while (x <= w) {
            val y = surfaceY + 4f + sin(phase * 2f + x * 0.04f) * 3.5f
            wavePath.lineTo(x, y)
            x += 8f
        }
        drawPath(wavePath, Color.White.copy(alpha = 0.28f), style = Stroke(width = 2f))
        // Bolhas
        for (i in 0..5) {
            val bx = w * (0.15f + (i * 0.14f) % 0.7f)
            val by = surfaceY + (h - surfaceY) * (0.25f + (i % 3) * 0.2f) + sin(phase + i) * 4f
            drawCircle(Color.White.copy(alpha = 0.22f), 3f + (i % 2), Offset(bx, by), style = Stroke(1.5f))
        }
    }

    private fun DrawScope.drawUnderwaterDecor(w: Float, h: Float, surfaceY: Float) {
        val floorY = h * 0.92f
        drawCircle(Color(0xFF57534E).copy(alpha = 0.55f), 10f, Offset(w * 0.18f, floorY))
        drawCircle(Color(0xFF78716C).copy(alpha = 0.5f), 7f, Offset(w * 0.22f, floorY + 4f))
        drawCircle(Color(0xFF57534E).copy(alpha = 0.5f), 9f, Offset(w * 0.72f, floorY - 2f))
        // Plantas aquáticas
        for (i in 0..3) {
            val px = w * (0.30f + i * 0.12f)
            val path = Path().apply {
                moveTo(px, floorY)
                quadraticTo(px + cos(i.toFloat()) * 8f, (surfaceY + floorY) * 0.55f, px + 4f, surfaceY + 40f)
            }
            drawPath(path, Color(0xFF14532D).copy(alpha = 0.45f), style = Stroke(width = 3f))
        }
    }

    /** Splash curto na superfície (feedback visual de captura). */
    fun DrawScope.drawSplash(w: Float, h: Float, hookYNorm: Float, timer: Float) {
        if (timer <= 0f) return
        val cx = w * FishingGameEngine.HOOK_X
        val cy = hookYNorm * h
        val t = (timer / 0.45f).coerceIn(0f, 1f)
        val alpha = t * 0.7f
        drawCircle(Color.White.copy(alpha = alpha * 0.5f), 10f + (1f - t) * 22f, Offset(cx, cy))
        for (i in 0..5) {
            val ang = i * 1.05f
            val dist = (1f - t) * 28f
            drawCircle(
                Color(0xFFBAE6FD).copy(alpha = alpha),
                3f,
                Offset(cx + cos(ang) * dist, cy + sin(ang) * dist * 0.6f)
            )
        }
    }
}
