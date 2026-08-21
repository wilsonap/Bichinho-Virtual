package com.example.ui.screens.fishing

import android.graphics.Paint as AndroidPaint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.example.game.fishing.FishingCatchType
import com.example.game.fishing.SwimEntity
import kotlin.math.abs

/**
 * Desenho dos peixes/lixo no Canvas.
 * Hoje usa emoji + corpo/glow; [spriteKey] prepara troca futura por sprites.
 */
object FishingCatchRenderer {

    fun spriteKey(type: FishingCatchType): String = "catch_${type.name.lowercase()}"

    fun DrawScope.drawCatchables(
        entities: List<SwimEntity>,
        attachedId: Long?,
        w: Float,
        h: Float,
        emojiPaint: AndroidPaint,
        baseEmojiSizePx: Float,
        phase: Float
    ) {
        emojiPaint.textSize = baseEmojiSizePx
        for (i in entities.indices) {
            val e = entities[i]
            if (attachedId != null && e.id == attachedId) continue
            drawOne(e, w, h, emojiPaint, baseEmojiSizePx, phase, attached = false)
        }
    }

    fun DrawScope.drawAttached(
        entity: SwimEntity?,
        w: Float,
        h: Float,
        emojiPaint: AndroidPaint,
        baseEmojiSizePx: Float,
        phase: Float
    ) {
        if (entity == null) return
        drawOne(entity, w, h, emojiPaint, baseEmojiSizePx, phase, attached = true)
    }

    private fun DrawScope.drawOne(
        e: SwimEntity,
        w: Float,
        h: Float,
        emojiPaint: AndroidPaint,
        baseEmojiSizePx: Float,
        phase: Float,
        attached: Boolean
    ) {
        val cx = e.x * w
        val cy = e.y * h
        val facingRight = e.vx >= 0f || attached
        val scale = when {
            e.type.isJunk -> 0.95f
            e.type.isRare -> 1.25f
            e.type.points >= 30 -> 1.12f
            else -> 1f
        }
        val bob = if (attached) 0f else sinCompat(phase * 2.2f + e.id) * 2.5f
        val drawY = cy + bob

        // Sombra / “corpo” sob a água — evita emoji solto
        val bodyW = 22f * scale
        val bodyH = 12f * scale
        val bodyColor = bodyTint(e.type)
        drawOval(
            bodyColor.copy(alpha = 0.55f),
            Offset(cx - bodyW * 0.5f, drawY - bodyH * 0.35f),
            Size(bodyW, bodyH)
        )

        if (e.type.isRare) {
            val pulse = 0.35f + 0.35f * abs(sinCompat(phase * 3f))
            drawCircle(Color(0xFFFDE047).copy(alpha = pulse), 18f * scale, Offset(cx, drawY))
            drawCircle(
                Color(0xFFFBBF24).copy(alpha = pulse * 0.5f),
                24f * scale,
                Offset(cx, drawY),
                style = Stroke(width = 2f)
            )
        } else if (!e.type.isJunk && e.type.points >= 30) {
            drawCircle(Color(0xFFA78BFA).copy(alpha = 0.25f), 16f * scale, Offset(cx, drawY))
        }

        emojiPaint.textSize = baseEmojiSizePx * scale
        // Espelhar visualmente peixes que nadam para a esquerda: desenha um pouco deslocado
        val emojiX = if (facingRight) cx else cx
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(e.type.emoji, emojiX, drawY + baseEmojiSizePx * 0.28f * scale, emojiPaint)
        }
    }

    private fun bodyTint(type: FishingCatchType): Color = when (type) {
        FishingCatchType.SMALL_FISH -> Color(0xFF38BDF8)
        FishingCatchType.COLORFUL_FISH -> Color(0xFFF472B6)
        FishingCatchType.PUFFER -> Color(0xFFFBBF24)
        FishingCatchType.SHRIMP -> Color(0xFFFB7185)
        FishingCatchType.CRAB -> Color(0xFFEF4444)
        FishingCatchType.STARFISH -> Color(0xFFFDE047)
        FishingCatchType.OLD_BOOT -> Color(0xFF78716C)
        FishingCatchType.CAN -> Color(0xFF94A3B8)
        FishingCatchType.TRASH -> Color(0xFF64748B)
    }

    private fun sinCompat(v: Float): Float = kotlin.math.sin(v.toDouble()).toFloat()
}
