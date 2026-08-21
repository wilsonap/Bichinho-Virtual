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
 * Paleta e desenho de céu/janela compartilhados — evita 4 cópias de cenário.
 * Móveis ficam nos renderizadores; só céu/luz/sol/lua/estrelas/chuva mudam aqui.
 */
object OutdoorAmbience {

    data class Theme(
        val skyColors: List<Color>,
        val showSun: Boolean,
        val sunLow: Boolean,
        val showMoon: Boolean,
        val showStars: Boolean,
        val showClouds: Boolean,
        val showRain: Boolean,
        val outdoorDim: Float,
        val indoorWarmGlow: Float,
        val shadowAlpha: Float
    )

    fun theme(period: DayPeriod, weather: WeatherState): Theme {
        val raining = weather == WeatherState.RAIN
        val cloudy = weather == WeatherState.CLOUDY || raining

        val base = when (period) {
            DayPeriod.MORNING -> Theme(
                skyColors = listOf(Color(0xFF7DD3FC), Color(0xFFBAE6FD), Color(0xFFFEF9C3)),
                showSun = true,
                sunLow = true,
                showMoon = false,
                showStars = false,
                showClouds = true,
                showRain = false,
                outdoorDim = 0f,
                indoorWarmGlow = 0.05f,
                shadowAlpha = 0.12f
            )
            DayPeriod.AFTERNOON -> Theme(
                // Próximo do céu diurno original do Quintal (claro + base quente no horizonte)
                skyColors = listOf(
                    Color(0xFF38BDF8),
                    Color(0xFF7DD3FC),
                    Color(0xFFBAE6FD),
                    Color(0xFFFEF3C7)
                ),
                showSun = true,
                sunLow = false,
                showMoon = false,
                showStars = false,
                showClouds = true,
                showRain = false,
                outdoorDim = 0f,
                indoorWarmGlow = 0f,
                shadowAlpha = 0.22f
            )
            DayPeriod.EVENING -> Theme(
                skyColors = listOf(Color(0xFFFB923C), Color(0xFFF472B6), Color(0xFF7C3AED)),
                showSun = true,
                sunLow = true,
                showMoon = false,
                showStars = false,
                showClouds = true,
                showRain = false,
                outdoorDim = 0.08f,
                // Brilho quente leve — não escurece o cômodo
                indoorWarmGlow = 0.14f,
                shadowAlpha = 0.32f
            )
            DayPeriod.NIGHT -> Theme(
                skyColors = listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E3A8A)),
                showSun = false,
                sunLow = false,
                showMoon = true,
                showStars = true,
                showClouds = false,
                showRain = false,
                outdoorDim = 0.15f,
                indoorWarmGlow = 0.18f,
                shadowAlpha = 0.40f
            )
        }

        if (!raining && !cloudy) return base

        if (raining) {
            return base.copy(
                skyColors = listOf(Color(0xFF64748B), Color(0xFF475569), Color(0xFF334155)),
                showSun = false,
                showMoon = period == DayPeriod.NIGHT,
                showStars = false,
                showClouds = true,
                showRain = true,
                outdoorDim = 0.25f,
                indoorWarmGlow = (base.indoorWarmGlow + 0.06f).coerceAtMost(0.24f)
            )
        }

        // CLOUDY
        return base.copy(
            skyColors = listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1), Color(0xFFE2E8F0)),
            showSun = false,
            showClouds = true,
            outdoorDim = 0.12f
        )
    }

    /** Desenha o conteúdo do vidro da janela (céu + astros + chuva). */
    fun DrawScope.drawWindowExterior(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        period: DayPeriod,
        weather: WeatherState,
        phase: Float,
        pulse: Float
    ) {
        val t = theme(period, weather)
        drawRoundRect(
            brush = Brush.verticalGradient(t.skyColors, startY = y, endY = y + h),
            topLeft = Offset(x, y),
            size = Size(w, h),
            cornerRadius = CornerRadius(4f, 4f)
        )

        if (t.showStars) {
            for (i in 0..5) {
                val sx = x + w * (0.15f + (i * 0.14f) % 0.7f)
                val sy = y + h * (0.18f + (i % 3) * 0.22f)
                drawCircle(
                    Color.White.copy(alpha = 0.45f + pulse * 0.4f),
                    1.6f,
                    Offset(sx, sy)
                )
            }
        }

        if (t.showMoon) {
            val mx = x + w * 0.72f
            val my = y + h * 0.32f
            val r = w * 0.12f
            drawCircle(Color(0xFFFEF08A), r, Offset(mx, my))
            drawCircle(Color(0xFF0F172A), r * 0.85f, Offset(mx - r * 0.35f, my - r * 0.1f))
        }

        if (t.showSun) {
            val sunY = if (t.sunLow) y + h * 0.62f else y + h * 0.28f
            val sunX = x + w * (if (t.sunLow) 0.78f else 0.70f)
            val sunR = w * (if (t.sunLow) 0.14f else 0.16f)
            drawCircle(Color(0xFFFDE047).copy(alpha = 0.35f), sunR * 1.6f, Offset(sunX, sunY))
            drawCircle(
                if (period == DayPeriod.EVENING) Color(0xFFFB923C) else Color(0xFFFBBF24),
                sunR,
                Offset(sunX, sunY)
            )
        }

        if (t.showClouds) {
            val shift = (phase * 10f) % (w * 0.4f)
            val cy = y + h * 0.35f
            drawCircle(Color.White.copy(alpha = 0.55f), w * 0.10f, Offset(x + w * 0.2f + shift * 0.3f, cy))
            drawCircle(Color.White.copy(alpha = 0.5f), w * 0.12f, Offset(x + w * 0.32f + shift * 0.3f, cy - 4f))
        }

        if (t.showRain) {
            for (i in 0..10) {
                val rx = x + w * ((i * 0.09f + phase * 0.05f) % 0.95f)
                val ry = y + h * ((i * 0.13f + phase * 0.2f) % 0.9f)
                drawLine(
                    Color(0xFFBAE6FD).copy(alpha = 0.65f),
                    Offset(rx, ry),
                    Offset(rx + 2f, ry + h * 0.12f),
                    strokeWidth = 1.5f
                )
            }
        }

        if (t.outdoorDim > 0f) {
            drawRoundRect(
                color = Color.Black.copy(alpha = t.outdoorDim),
                topLeft = Offset(x, y),
                size = Size(w, h),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }

    /** Overlay quente interno (lâmpadas) — só quando [apply] for true. */
    fun DrawScope.drawIndoorWarmOverlay(
        w: Float,
        h: Float,
        period: DayPeriod,
        weather: WeatherState,
        apply: Boolean = true
    ) {
        if (!apply) return
        val glow = theme(period, weather).indoorWarmGlow
        if (glow <= 0.01f) return
        drawRect(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFBBF24).copy(alpha = glow * 0.28f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.45f),
                radius = w * 0.75f
            ),
            topLeft = Offset.Zero,
            size = Size(w, h)
        )
    }

    /**
     * @deprecated Preferir [IndoorLighting.useDarkSleepPalette].
     * Nunca use DayPeriod.NIGHT para escurecer a casa inteira.
     */
    fun dimInterior(period: DayPeriod, isSleeping: Boolean): Boolean = isSleeping
}
