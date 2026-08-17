package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders rich, animated thematic room environments with layered scenery
 * for Bedroom (Quarto Aconchegante), Forest (Floresta Mágica), Beach (Praia Tropical),
 * and Space (Espaço Sideral).
 *
 * Scenery layers strictly sit behind the pet to keep the pet fully visible and centered.
 */
@Composable
fun RoomSceneryRenderer(
    themeId: String,
    isSleeping: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scenery_ambient")

    // Ambient floating / wave / leaf animation phase (0 to 2*PI)
    val ambientPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scenery_phase"
    )

    // Twinkle / firefly pulse (0 to 1)
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scenery_pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (themeId) {
            "decor_forest" -> drawMagicForestScene(w, h, ambientPhase, pulseProgress, isSleeping)
            "decor_beach" -> drawTropicalBeachScene(w, h, ambientPhase, pulseProgress, isSleeping)
            "decor_space" -> drawOuterSpaceScene(w, h, ambientPhase, pulseProgress, isSleeping)
            else -> drawCozyBedroomScene(w, h, ambientPhase, pulseProgress, isSleeping)
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 1. QUARTO ACONCHEGANTE (Cozy Bedroom)
// -------------------------------------------------------------------------------------------------
private fun DrawScope.drawCozyBedroomScene(
    w: Float,
    h: Float,
    phase: Float,
    pulse: Float,
    isSleeping: Boolean
) {
    val floorY = h * 0.68f

    // Wall Background
    val wallGradient = if (isSleeping) {
        listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
    } else {
        listOf(Color(0xFFFDF2F8), Color(0xFFFCE7F3), Color(0xFFF5D0FE))
    }
    drawRect(
        brush = Brush.verticalGradient(wallGradient, startY = 0f, endY = floorY),
        topLeft = Offset(0f, 0f),
        size = Size(w, floorY)
    )

    // Baseboard moulding strip
    drawRect(
        color = if (isSleeping) Color(0xFF1E293B) else Color(0xFFF1F5F9),
        topLeft = Offset(0f, floorY - 14f),
        size = Size(w, 14f)
    )
    drawLine(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFCBD5E1),
        start = Offset(0f, floorY - 14f),
        end = Offset(w, floorY - 14f),
        strokeWidth = 2f
    )

    // Hardwood Floor with planks
    val floorColorTop = if (isSleeping) Color(0xFF292524) else Color(0xFFDEB887)
    val floorColorBottom = if (isSleeping) Color(0xFF1C1917) else Color(0xFFC49A6C)
    drawRect(
        brush = Brush.verticalGradient(listOf(floorColorTop, floorColorBottom), startY = floorY, endY = h),
        topLeft = Offset(0f, floorY),
        size = Size(w, h - floorY)
    )

    // Floor wood plank lines (perspective)
    val plankCount = 7
    for (i in 0..plankCount) {
        val px = (w / plankCount) * i
        drawLine(
            color = if (isSleeping) Color(0xFF0C0A09).copy(alpha = 0.5f) else Color(0xFF8B5A2B).copy(alpha = 0.35f),
            start = Offset(px, floorY),
            end = Offset(px * 1.08f - (w * 0.04f), h),
            strokeWidth = 2.5f
        )
    }

    // Cozy Plush Rug in center under pet stage
    val rugWidth = (w * 0.62f).coerceIn(240f, 480f)
    val rugHeight = (h * 0.22f).coerceIn(90f, 160f)
    val rugCenter = Offset(w * 0.5f, floorY + rugHeight * 0.45f)

    // Rug shadow
    drawOval(
        color = Color(0x33000000),
        topLeft = Offset(rugCenter.x - rugWidth / 2 - 4f, rugCenter.y - rugHeight / 2 + 6f),
        size = Size(rugWidth + 8f, rugHeight + 4f)
    )
    // Outer rug
    drawOval(
        color = if (isSleeping) Color(0xFF475569) else Color(0xFFFBCFE8),
        topLeft = Offset(rugCenter.x - rugWidth / 2, rugCenter.y - rugHeight / 2),
        size = Size(rugWidth, rugHeight)
    )
    // Inner pattern rug
    drawOval(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFF472B6),
        topLeft = Offset(rugCenter.x - rugWidth * 0.42f, rugCenter.y - rugHeight * 0.42f),
        size = Size(rugWidth * 0.84f, rugHeight * 0.84f)
    )
    drawOval(
        color = if (isSleeping) Color(0xFF1E293B) else Color(0xFFFFF1F2),
        topLeft = Offset(rugCenter.x - rugWidth * 0.34f, rugCenter.y - rugHeight * 0.34f),
        size = Size(rugWidth * 0.68f, rugHeight * 0.68f)
    )

    // Window with scenery outside (Top Left)
    val winWidth = (w * 0.22f).coerceIn(80f, 130f)
    val winHeight = (h * 0.24f).coerceIn(90f, 140f)
    val winX = w * 0.06f
    val winY = h * 0.10f

    // Window frame
    drawRoundRect(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFE2E8F0),
        topLeft = Offset(winX - 6f, winY - 6f),
        size = Size(winWidth + 12f, winHeight + 12f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Outside sky glass
    val glassBrush = if (isSleeping) {
        Brush.verticalGradient(listOf(Color(0xFF090D16), Color(0xFF1E293B)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF60A5FA), Color(0xFFBAE6FD), Color(0xFFFEF3C7)))
    }
    drawRoundRect(
        brush = glassBrush,
        topLeft = Offset(winX, winY),
        size = Size(winWidth, winHeight),
        cornerRadius = CornerRadius(6f, 6f)
    )
    // Sun or Moon in window
    if (isSleeping) {
        // Crescent moon
        drawCircle(
            color = Color(0xFFFEF08A),
            radius = winWidth * 0.16f,
            center = Offset(winX + winWidth * 0.72f, winY + winHeight * 0.35f)
        )
        drawCircle(
            color = Color(0xFF090D16),
            radius = winWidth * 0.14f,
            center = Offset(winX + winWidth * 0.66f, winY + winHeight * 0.32f)
        )
    } else {
        // Warm sun
        drawCircle(
            color = Color(0xFFFBBF24),
            radius = winWidth * 0.18f,
            center = Offset(winX + winWidth * 0.75f, winY + winHeight * 0.32f)
        )
        // Little cloud
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = winWidth * 0.12f,
            center = Offset(winX + winWidth * 0.35f, winY + winHeight * 0.55f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = winWidth * 0.15f,
            center = Offset(winX + winWidth * 0.50f, winY + winHeight * 0.52f)
        )
    }
    // Window mullions (cross panes)
    drawLine(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFCBD5E1),
        start = Offset(winX + winWidth / 2, winY),
        end = Offset(winX + winWidth / 2, winY + winHeight),
        strokeWidth = 3f
    )
    drawLine(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFCBD5E1),
        start = Offset(winX, winY + winHeight / 2),
        end = Offset(winX + winWidth, winY + winHeight / 2),
        strokeWidth = 3f
    )

    // Wall Art (Picture Frames on Top Right)
    val frameX = w * 0.74f
    val frameY = h * 0.11f
    val frameW = (w * 0.18f).coerceIn(60f, 100f)
    val frameH = frameW * 0.8f
    // Frame border
    drawRoundRect(
        color = if (isSleeping) Color(0xFF475569) else Color(0xFFB45309),
        topLeft = Offset(frameX, frameY),
        size = Size(frameW, frameH),
        cornerRadius = CornerRadius(6f, 6f)
    )
    // Canvas inside frame
    drawRoundRect(
        color = if (isSleeping) Color(0xFF1E293B) else Color(0xFFFEF3C7),
        topLeft = Offset(frameX + 4f, frameY + 4f),
        size = Size(frameW - 8f, frameH - 8f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Heart/Star inside picture
    drawCircle(
        color = if (isSleeping) Color(0xFF38BDF8) else Color(0xFFF43F5E),
        radius = frameW * 0.16f,
        center = Offset(frameX + frameW / 2, frameY + frameH / 2)
    )

    // Bookshelf (Right Wall)
    val shelfX = w * 0.82f
    val shelfY = floorY - (h * 0.28f).coerceIn(110f, 170f)
    val shelfW = (w * 0.14f).coerceIn(50f, 85f)
    val shelfH = floorY - shelfY
    // Shelf frame
    drawRoundRect(
        color = if (isSleeping) Color(0xFF292524) else Color(0xFF92400E),
        topLeft = Offset(shelfX, shelfY),
        size = Size(shelfW, shelfH),
        cornerRadius = CornerRadius(6f, 6f)
    )
    // Shelf planks & books
    val shelfLevels = 3
    for (s in 1 until shelfLevels) {
        val sy = shelfY + (shelfH / shelfLevels) * s
        drawLine(
            color = if (isSleeping) Color(0xFF44403C) else Color(0xFFB45309),
            start = Offset(shelfX + 4f, sy),
            end = Offset(shelfX + shelfW - 4f, sy),
            strokeWidth = 3f
        )
        // Colorful book spines
        val bookColors = listOf(Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFF8B5CF6))
        for (b in 0..3) {
            val bx = shelfX + 8f + (b * 9f)
            val bh = (shelfH / shelfLevels) * 0.65f
            if (bx + 7f < shelfX + shelfW - 4f) {
                drawRoundRect(
                    color = if (isSleeping) bookColors[b % bookColors.size].copy(alpha = 0.5f) else bookColors[b % bookColors.size],
                    topLeft = Offset(bx, sy - bh),
                    size = Size(7f, bh),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }
    }

    // Cozy Bed with Pillow and Blanket (Left Background)
    val bedW = (w * 0.28f).coerceIn(95f, 160f)
    val bedH = (h * 0.20f).coerceIn(75f, 120f)
    val bedX = w * 0.04f
    val bedY = floorY - bedH * 0.70f

    // Bed shadow
    drawRoundRect(
        color = Color(0x2A000000),
        topLeft = Offset(bedX - 2f, floorY - 6f),
        size = Size(bedW + 8f, 16f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Headboard
    drawRoundRect(
        color = if (isSleeping) Color(0xFF44403C) else Color(0xFFB45309),
        topLeft = Offset(bedX, bedY - 18f),
        size = Size(bedW * 0.22f, bedH + 18f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    // Mattress Base
    drawRoundRect(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFF8FAFC),
        topLeft = Offset(bedX + 8f, bedY + bedH * 0.2f),
        size = Size(bedW - 8f, bedH * 0.65f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Quilt / Blanket
    drawRoundRect(
        color = if (isSleeping) Color(0xFF1E3A8A) else Color(0xFF818CF8),
        topLeft = Offset(bedX + bedW * 0.32f, bedY + bedH * 0.2f),
        size = Size(bedW * 0.68f - 4f, bedH * 0.65f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Fluffy Pillow
    drawRoundRect(
        color = if (isSleeping) Color(0xFFE2E8F0).copy(alpha = 0.6f) else Color.White,
        topLeft = Offset(bedX + bedW * 0.12f, bedY + bedH * 0.15f),
        size = Size(bedW * 0.24f, bedH * 0.38f),
        cornerRadius = CornerRadius(6f, 6f)
    )

    // Desk + Desk Lamp (Right-Middle)
    val deskX = w * 0.68f
    val deskY = floorY - (h * 0.15f).coerceIn(60f, 95f)
    val deskW = (w * 0.15f).coerceIn(55f, 85f)
    val deskH = floorY - deskY
    // Desk tabletop
    drawRoundRect(
        color = if (isSleeping) Color(0xFF3E2723) else Color(0xFFD97706),
        topLeft = Offset(deskX, deskY),
        size = Size(deskW, 10f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Desk legs
    drawLine(
        color = if (isSleeping) Color(0xFF292524) else Color(0xFFB45309),
        start = Offset(deskX + 6f, deskY + 10f),
        end = Offset(deskX + 6f, floorY),
        strokeWidth = 4f
    )
    drawLine(
        color = if (isSleeping) Color(0xFF292524) else Color(0xFFB45309),
        start = Offset(deskX + deskW - 6f, deskY + 10f),
        end = Offset(deskX + deskW - 6f, floorY),
        strokeWidth = 4f
    )
    // Lamp base + pole
    val lampX = deskX + deskW * 0.5f
    val lampY = deskY - 26f
    drawLine(
        color = if (isSleeping) Color(0xFF64748B) else Color(0xFF475569),
        start = Offset(lampX, deskY),
        end = Offset(lampX, lampY),
        strokeWidth = 3f
    )
    // Lamp Shade
    val shadePath = Path().apply {
        moveTo(lampX - 12f, lampY + 10f)
        lineTo(lampX + 12f, lampY + 10f)
        lineTo(lampX + 6f, lampY)
        lineTo(lampX - 6f, lampY)
        close()
    }
    drawPath(
        path = shadePath,
        color = if (isSleeping) Color(0xFFF59E0B).copy(alpha = 0.7f) else Color(0xFFF59E0B)
    )
    // Warm Lamp Light glow
    val lampGlowAlpha = if (isSleeping) 0.18f + (pulse * 0.08f) else 0.35f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFEF08A).copy(alpha = lampGlowAlpha), Color.Transparent),
            center = Offset(lampX, lampY + 8f),
            radius = 65f
        ),
        radius = 65f,
        center = Offset(lampX, lampY + 8f)
    )
}

// -------------------------------------------------------------------------------------------------
// 2. FLORESTA MÁGICA (Magic Forest)
// -------------------------------------------------------------------------------------------------
private fun DrawScope.drawMagicForestScene(
    w: Float,
    h: Float,
    phase: Float,
    pulse: Float,
    isSleeping: Boolean
) {
    val groundY = h * 0.65f

    // Enchanted Forest Sky
    val forestSky = if (isSleeping) {
        listOf(Color(0xFF022C22), Color(0xFF064E3B), Color(0xFF065F46))
    } else {
        listOf(Color(0xFFD1FAE5), Color(0xFFA7F3D0), Color(0xFF6EE7B7))
    }
    drawRect(
        brush = Brush.verticalGradient(forestSky, startY = 0f, endY = groundY),
        topLeft = Offset(0f, 0f),
        size = Size(w, groundY)
    )

    // Sun rays / Aurora light beams streaming down
    val rayAlpha = if (isSleeping) 0.08f + (pulse * 0.05f) else 0.16f
    val rayPath = Path().apply {
        moveTo(w * 0.1f, 0f)
        lineTo(w * 0.35f, 0f)
        lineTo(w * 0.75f, groundY)
        lineTo(w * 0.40f, groundY)
        close()
    }
    drawPath(
        path = rayPath,
        brush = Brush.verticalGradient(
            listOf(Color(0xFFFEF08A).copy(alpha = rayAlpha), Color.Transparent),
            startY = 0f,
            endY = groundY
        )
    )

    // Distant tree silhouettes / hills
    val hillPath = Path().apply {
        moveTo(0f, groundY - 20f)
        quadraticBezierTo(w * 0.25f, groundY - 70f, w * 0.5f, groundY - 30f)
        quadraticBezierTo(w * 0.75f, groundY - 65f, w, groundY - 25f)
        lineTo(w, groundY)
        lineTo(0f, groundY)
        close()
    }
    drawPath(
        path = hillPath,
        color = if (isSleeping) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFF34D399).copy(alpha = 0.5f)
    )

    // Magical Forest Canopy Trees (Left & Right background)
    // Left Big Tree
    val trunkLeftX = w * 0.08f
    drawRoundRect(
        color = if (isSleeping) Color(0xFF1C1917) else Color(0xFF78350F),
        topLeft = Offset(trunkLeftX, groundY - 140f),
        size = Size(26f, 150f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Left Tree Foliage clusters (Animated sway)
    val swayLeft = sin(phase) * 6f
    drawCircle(
        color = if (isSleeping) Color(0xFF065F46) else Color(0xFF059669),
        radius = 55f,
        center = Offset(trunkLeftX + 13f + swayLeft, groundY - 140f)
    )
    drawCircle(
        color = if (isSleeping) Color(0xFF047857) else Color(0xFF10B981),
        radius = 45f,
        center = Offset(trunkLeftX - 18f + swayLeft, groundY - 120f)
    )
    drawCircle(
        color = if (isSleeping) Color(0xFF0F766E) else Color(0xFF34D399),
        radius = 48f,
        center = Offset(trunkLeftX + 35f + swayLeft, groundY - 110f)
    )

    // Right Big Tree
    val trunkRightX = w * 0.84f
    drawRoundRect(
        color = if (isSleeping) Color(0xFF1C1917) else Color(0xFF78350F),
        topLeft = Offset(trunkRightX, groundY - 160f),
        size = Size(30f, 170f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    val swayRight = cos(phase) * 6f
    drawCircle(
        color = if (isSleeping) Color(0xFF065F46) else Color(0xFF059669),
        radius = 60f,
        center = Offset(trunkRightX + 15f + swayRight, groundY - 160f)
    )
    drawCircle(
        color = if (isSleeping) Color(0xFF047857) else Color(0xFF10B981),
        radius = 50f,
        center = Offset(trunkRightX + 40f + swayRight, groundY - 135f)
    )
    drawCircle(
        color = if (isSleeping) Color(0xFF0F766E) else Color(0xFF34D399),
        radius = 52f,
        center = Offset(trunkRightX - 22f + swayRight, groundY - 130f)
    )

    // Forest Mossy Ground
    val groundBrush = if (isSleeping) {
        Brush.verticalGradient(listOf(Color(0xFF064E3B), Color(0xFF022C22), Color(0xFF021B15)), startY = groundY, endY = h)
    } else {
        Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF047857)), startY = groundY, endY = h)
    }
    drawRect(
        brush = groundBrush,
        topLeft = Offset(0f, groundY),
        size = Size(w, h - groundY)
    )

    // Grass Tuft Details along the floor
    for (i in 0..12) {
        val gx = (w / 12f) * i + (sin(i.toFloat()) * 14f)
        val gy = groundY + (i % 3) * 6f
        val grassColor = if (isSleeping) Color(0xFF34D399).copy(alpha = 0.4f) else Color(0xFF6EE7B7)
        drawLine(
            color = grassColor,
            start = Offset(gx, gy),
            end = Offset(gx - 4f, gy - 12f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = grassColor,
            start = Offset(gx + 3f, gy),
            end = Offset(gx + 5f, gy - 14f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
    }

    // Magic Glowing Mushrooms (Left Floor)
    val mush1X = w * 0.18f
    val mush1Y = groundY + (h * 0.08f)
    // Stem
    drawRoundRect(
        color = Color(0xFFF1F5F9),
        topLeft = Offset(mush1X - 4f, mush1Y - 14f),
        size = Size(8f, 16f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    // Mushroom Cap
    drawArc(
        color = Color(0xFFEF4444),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(mush1X - 16f, mush1Y - 26f),
        size = Size(32f, 26f)
    )
    // White polka dots
    drawCircle(color = Color.White, radius = 2.5f, center = Offset(mush1X - 6f, mush1Y - 18f))
    drawCircle(color = Color.White, radius = 2.5f, center = Offset(mush1X + 5f, mush1Y - 19f))
    drawCircle(color = Color.White, radius = 2.5f, center = Offset(mush1X, mush1Y - 23f))
    // Mushroom glow aura
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFCA5A5).copy(alpha = 0.4f * pulse), Color.Transparent),
            center = Offset(mush1X, mush1Y - 16f),
            radius = 35f
        ),
        radius = 35f,
        center = Offset(mush1X, mush1Y - 16f)
    )

    // Smaller Cyan Magic Mushroom (Right Floor)
    val mush2X = w * 0.78f
    val mush2Y = groundY + (h * 0.12f)
    drawRoundRect(
        color = Color(0xFFF1F5F9),
        topLeft = Offset(mush2X - 3f, mush2Y - 10f),
        size = Size(6f, 12f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawArc(
        color = Color(0xFF06B6D4),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(mush2X - 12f, mush2Y - 20f),
        size = Size(24f, 20f)
    )
    drawCircle(color = Color.White, radius = 2f, center = Offset(mush2X - 3f, mush2Y - 14f))
    drawCircle(color = Color.White, radius = 2f, center = Offset(mush2X + 4f, mush2Y - 15f))
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF67E8F9).copy(alpha = 0.45f * pulse), Color.Transparent),
            center = Offset(mush2X, mush2Y - 12f),
            radius = 30f
        ),
        radius = 30f,
        center = Offset(mush2X, mush2Y - 12f)
    )

    // Forest Wildflowers (Yellow & Purple)
    val flowerColors = listOf(Color(0xFFFBBF24), Color(0xFFA855F7), Color(0xFFF472B6))
    for (f in 0..4) {
        val fx = w * (0.28f + (f * 0.12f))
        val fy = groundY + 20f + ((f % 2) * 18f)
        val col = flowerColors[f % flowerColors.size]
        drawCircle(color = col, radius = 4f, center = Offset(fx, fy))
        drawCircle(color = Color(0xFFFEF08A), radius = 1.5f, center = Offset(fx, fy))
    }

    // Animated Floating Fireflies / Magic Spores
    val fireflyCount = 9
    for (i in 0 until fireflyCount) {
        val fx = ((w * (0.1f + (i * 0.09f))) + sin(phase + i * 1.2f) * 22f) % w
        val fy = ((h * (0.25f + ((i % 5) * 0.09f))) + cos(phase + i * 0.9f) * 16f)
        val fireflyGlow = (sin(phase * 2f + i) + 1f) / 2f

        drawCircle(
            color = Color(0xFFFEF08A).copy(alpha = 0.85f * fireflyGlow),
            radius = 3f,
            center = Offset(fx, fy)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFDE047).copy(alpha = 0.4f * fireflyGlow), Color.Transparent),
                center = Offset(fx, fy),
                radius = 16f
            ),
            radius = 16f,
            center = Offset(fx, fy)
        )
    }

    // Drifting animated leaves
    for (l in 0..2) {
        val lx = (w * (0.3f + l * 0.25f) + cos(phase + l) * 28f)
        val ly = (h * (0.15f + l * 0.18f) + sin(phase + l * 1.5f) * 15f)
        rotate(degrees = (phase * 40f + l * 60f) % 360f, pivot = Offset(lx, ly)) {
            val leafPath = Path().apply {
                moveTo(lx, ly - 8f)
                quadraticBezierTo(lx + 6f, ly, lx, ly + 8f)
                quadraticBezierTo(lx - 6f, ly, lx, ly - 8f)
                close()
            }
            drawPath(path = leafPath, color = Color(0xFF34D399).copy(alpha = 0.65f))
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 3. PRAIA TROPICAL (Tropical Beach)
// -------------------------------------------------------------------------------------------------
private fun DrawScope.drawTropicalBeachScene(
    w: Float,
    h: Float,
    phase: Float,
    pulse: Float,
    isSleeping: Boolean
) {
    val horizonY = h * 0.38f
    val shoreY = h * 0.58f

    // Tropical Sky
    val skyBrush = if (isSleeping) {
        Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF1E3A8A)), startY = 0f, endY = horizonY)
    } else {
        Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF7DD3FC), Color(0xFFBAE6FD), Color(0xFFFEF3C7)), startY = 0f, endY = horizonY)
    }
    drawRect(
        brush = skyBrush,
        topLeft = Offset(0f, 0f),
        size = Size(w, horizonY)
    )

    // Sun / Moon on Horizon
    if (isSleeping) {
        drawCircle(
            color = Color(0xFFFEF08A),
            radius = 24f,
            center = Offset(w * 0.75f, horizonY * 0.55f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFEF08A).copy(alpha = 0.25f), Color.Transparent),
                center = Offset(w * 0.75f, horizonY * 0.55f),
                radius = 60f
            ),
            radius = 60f,
            center = Offset(w * 0.75f, horizonY * 0.55f)
        )
    } else {
        // Bright tropical sun with radiating lens
        drawCircle(
            color = Color(0xFFFBBF24),
            radius = 32f,
            center = Offset(w * 0.25f, horizonY * 0.50f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFEF08A).copy(alpha = 0.45f), Color.Transparent),
                center = Offset(w * 0.25f, horizonY * 0.50f),
                radius = 80f
            ),
            radius = 80f,
            center = Offset(w * 0.25f, horizonY * 0.50f)
        )
    }

    // Ocean Water (Horizon to Shore)
    val seaBrush = if (isSleeping) {
        Brush.verticalGradient(listOf(Color(0xFF1E3A8A), Color(0xFF172554), Color(0xFF0F172A)), startY = horizonY, endY = shoreY)
    } else {
        Brush.verticalGradient(listOf(Color(0xFF0284C7), Color(0xFF0EA5E9), Color(0xFF38BDF8)), startY = horizonY, endY = shoreY)
    }
    drawRect(
        brush = seaBrush,
        topLeft = Offset(0f, horizonY),
        size = Size(w, shoreY - horizonY)
    )

    // Animated Ocean Waves
    val waveOffset1 = sin(phase) * 8f
    val waveOffset2 = cos(phase) * 10f

    val wavePath1 = Path().apply {
        moveTo(0f, shoreY - 14f + waveOffset1)
        quadraticBezierTo(w * 0.25f, shoreY - 24f - waveOffset1, w * 0.5f, shoreY - 14f + waveOffset1)
        quadraticBezierTo(w * 0.75f, shoreY - 4f - waveOffset1, w, shoreY - 14f + waveOffset1)
        lineTo(w, shoreY)
        lineTo(0f, shoreY)
        close()
    }
    drawPath(path = wavePath1, color = Color(0xFF7DD3FC).copy(alpha = 0.5f))

    // Wave Foam / Crest (Animated shoreline)
    val foamPath = Path().apply {
        moveTo(0f, shoreY + waveOffset2)
        quadraticBezierTo(w * 0.3f, shoreY - 10f + waveOffset2, w * 0.6f, shoreY + 4f + waveOffset2)
        quadraticBezierTo(w * 0.85f, shoreY - 8f + waveOffset2, w, shoreY + waveOffset2)
        lineTo(w, shoreY + 12f)
        lineTo(0f, shoreY + 12f)
        close()
    }
    drawPath(path = foamPath, color = Color.White.copy(alpha = 0.75f))

    // Golden Sand Beach (Shore to bottom)
    val sandBrush = if (isSleeping) {
        Brush.verticalGradient(listOf(Color(0xFF44403C), Color(0xFF292524), Color(0xFF1C1917)), startY = shoreY, endY = h)
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFDE68A), Color(0xFFFCD34D), Color(0xFFF59E0B)), startY = shoreY, endY = h)
    }
    drawRect(
        brush = sandBrush,
        topLeft = Offset(0f, shoreY + 8f),
        size = Size(w, h - (shoreY + 8f))
    )

    // Beach Umbrella (Right Side Background)
    val umbX = w * 0.82f
    val umbY = shoreY + (h * 0.06f)
    val umbW = (w * 0.20f).coerceIn(70f, 110f)
    val umbH = umbW * 0.7f

    // Umbrella pole
    drawLine(
        color = if (isSleeping) Color(0xFF94A3B8) else Color(0xFFCBD5E1),
        start = Offset(umbX, umbY),
        end = Offset(umbX - 8f, umbY + (h * 0.18f)),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
    // Umbrella Canopy (Red & White stripes)
    drawArc(
        color = Color(0xFFEF4444),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(umbX - umbW / 2, umbY - umbH),
        size = Size(umbW, umbH * 2)
    )
    // White middle stripe
    drawArc(
        color = Color.White,
        startAngle = 220f,
        sweepAngle = 100f,
        useCenter = true,
        topLeft = Offset(umbX - umbW * 0.35f, umbY - umbH),
        size = Size(umbW * 0.7f, umbH * 2)
    )
    // Red core
    drawArc(
        color = Color(0xFFEF4444),
        startAngle = 250f,
        sweepAngle = 40f,
        useCenter = true,
        topLeft = Offset(umbX - umbW * 0.18f, umbY - umbH),
        size = Size(umbW * 0.36f, umbH * 2)
    )

    // Palm Tree (Left Side Background)
    val palmX = w * 0.08f
    val palmY = shoreY + (h * 0.14f)
    val palmTop = Offset(palmX + (w * 0.08f), shoreY - (h * 0.22f))

    // Curved Trunk
    val trunkPath = Path().apply {
        moveTo(palmX, palmY)
        quadraticBezierTo(palmX + (w * 0.02f), palmY - (h * 0.18f), palmTop.x, palmTop.y)
    }
    drawPath(
        path = trunkPath,
        color = if (isSleeping) Color(0xFF292524) else Color(0xFF78350F),
        style = Stroke(width = 16f, cap = StrokeCap.Round)
    )
    // Trunk rings
    for (r in 1..5) {
        val rx = palmX + (palmTop.x - palmX) * (r / 6f)
        val ry = palmY + (palmTop.y - palmY) * (r / 6f)
        drawCircle(
            color = if (isSleeping) Color(0xFF44403C) else Color(0xFF92400E),
            radius = 7f,
            center = Offset(rx, ry)
        )
    }

    // Palm Fronds / Leaves (Animated sway with wind)
    val palmSway = sin(phase) * 5f
    val frondAngles = listOf(-60f, -20f, 20f, 60f, 100f, 140f)
    for (angle in frondAngles) {
        rotate(degrees = angle + palmSway, pivot = palmTop) {
            val frondPath = Path().apply {
                moveTo(palmTop.x, palmTop.y)
                quadraticBezierTo(palmTop.x + 40f, palmTop.y - 18f, palmTop.x + 75f, palmTop.y + 12f)
                quadraticBezierTo(palmTop.x + 35f, palmTop.y + 4f, palmTop.x, palmTop.y)
                close()
            }
            drawPath(
                path = frondPath,
                color = if (isSleeping) Color(0xFF065F46) else Color(0xFF16A34A)
            )
        }
    }

    // Seashells and Starfish on the Sand (Floor)
    // Pink Starfish (Left)
    val starX = w * 0.25f
    val starY = shoreY + (h * 0.22f)
    drawCircle(color = Color(0xFFF43F5E), radius = 6f, center = Offset(starX, starY))
    for (a in 0..4) {
        val rad = Math.toRadians((a * 72.0) - 90.0)
        val armX = starX + (cos(rad) * 11.0).toFloat()
        val armY = starY + (sin(rad) * 11.0).toFloat()
        drawLine(
            color = Color(0xFFF43F5E),
            start = Offset(starX, starY),
            end = Offset(armX, armY),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
    }

    // White / Gold Seashell (Right)
    val shellX = w * 0.65f
    val shellY = shoreY + (h * 0.26f)
    drawArc(
        color = Color(0xFFFFFBEB),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(shellX - 8f, shellY - 12f),
        size = Size(16f, 14f)
    )
    drawArc(
        color = Color(0xFFD97706),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(shellX - 8f, shellY - 12f),
        size = Size(16f, 14f),
        style = Stroke(width = 1.5f)
    )
}

// -------------------------------------------------------------------------------------------------
// 4. ESPAÇO SIDERAL (Outer Space)
// -------------------------------------------------------------------------------------------------
private fun DrawScope.drawOuterSpaceScene(
    w: Float,
    h: Float,
    phase: Float,
    pulse: Float,
    isSleeping: Boolean
) {
    // Deep Space Background
    val spaceBrush = Brush.verticalGradient(
        listOf(Color(0xFF030712), Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF311042)),
        startY = 0f,
        endY = h
    )
    drawRect(
        brush = spaceBrush,
        topLeft = Offset(0f, 0f),
        size = Size(w, h)
    )

    // Swirling Nebula Dust Clouds (Purple & Cyan)
    val nebAlpha = 0.22f + (pulse * 0.08f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFC084FC).copy(alpha = nebAlpha), Color.Transparent),
            center = Offset(w * 0.25f, h * 0.30f),
            radius = w * 0.45f
        ),
        radius = w * 0.45f,
        center = Offset(w * 0.25f, h * 0.30f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF38BDF8).copy(alpha = nebAlpha * 0.8f), Color.Transparent),
            center = Offset(w * 0.75f, h * 0.65f),
            radius = w * 0.40f
        ),
        radius = w * 0.40f,
        center = Offset(w * 0.75f, h * 0.65f)
    )

    // Star Field (Twinkling stars with procedural positions)
    val starCoords = listOf(
        Pair(0.08f, 0.12f), Pair(0.18f, 0.06f), Pair(0.32f, 0.18f),
        Pair(0.48f, 0.08f), Pair(0.62f, 0.14f), Pair(0.78f, 0.05f),
        Pair(0.92f, 0.18f), Pair(0.12f, 0.42f), Pair(0.88f, 0.38f),
        Pair(0.15f, 0.72f), Pair(0.85f, 0.78f), Pair(0.06f, 0.88f),
        Pair(0.35f, 0.85f), Pair(0.68f, 0.88f), Pair(0.94f, 0.92f)
    )

    for ((idx, coord) in starCoords.withIndex()) {
        val sx = w * coord.first
        val sy = h * coord.second
        val starPulse = (sin(phase * 2.5f + idx * 0.9f) + 1f) / 2f
        val starRadius = if (idx % 3 == 0) 3f + (starPulse * 1.5f) else 1.8f + (starPulse * 0.8f)

        drawCircle(
            color = Color.White.copy(alpha = 0.6f + (starPulse * 0.4f)),
            radius = starRadius,
            center = Offset(sx, sy)
        )
        // 4-point twinkle sparkle on larger stars
        if (idx % 4 == 0) {
            val sparkLen = 7f + (starPulse * 4f)
            drawLine(
                color = Color.White.copy(alpha = 0.7f * starPulse),
                start = Offset(sx - sparkLen, sy),
                end = Offset(sx + sparkLen, sy),
                strokeWidth = 1.5f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f * starPulse),
                start = Offset(sx, sy - sparkLen),
                end = Offset(sx, sy + sparkLen),
                strokeWidth = 1.5f
            )
        }
    }

    // Giant Ringed Planet (Saturn-like in Top Left)
    val planet1X = w * 0.18f
    val planet1Y = h * 0.18f
    val planet1R = (w * 0.08f).coerceIn(28f, 44f)

    // Planet body
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFFB45309)),
            center = Offset(planet1X - planet1R * 0.3f, planet1Y - planet1R * 0.3f),
            radius = planet1R * 1.3f
        ),
        radius = planet1R,
        center = Offset(planet1X, planet1Y)
    )
    // Planet Planetary Ring
    rotate(degrees = -25f, pivot = Offset(planet1X, planet1Y)) {
        drawOval(
            color = Color(0xFFFDE68A).copy(alpha = 0.8f),
            topLeft = Offset(planet1X - planet1R * 1.8f, planet1Y - planet1R * 0.35f),
            size = Size(planet1R * 3.6f, planet1R * 0.7f),
            style = Stroke(width = 5f)
        )
        drawOval(
            color = Color(0xFFF59E0B).copy(alpha = 0.5f),
            topLeft = Offset(planet1X - planet1R * 2.1f, planet1Y - planet1R * 0.45f),
            size = Size(planet1R * 4.2f, planet1R * 0.9f),
            style = Stroke(width = 3f)
        )
    }

    // Moon / Terra Planet (Top Right)
    val planet2X = w * 0.82f
    val planet2Y = h * 0.22f
    val planet2R = (w * 0.06f).coerceIn(20f, 32f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF67E8F9), Color(0xFF0284C7), Color(0xFF0F172A)),
            center = Offset(planet2X - planet2R * 0.25f, planet2Y - planet2R * 0.25f),
            radius = planet2R * 1.2f
        ),
        radius = planet2R,
        center = Offset(planet2X, planet2Y)
    )

    // Animated Floating Satellite / Space Station (Drifting slowly)
    val satX = (w * 0.76f) + (cos(phase * 0.7f) * 15f)
    val satY = (h * 0.42f) + (sin(phase * 0.7f) * 10f)

    // Satellite Core
    drawRoundRect(
        color = Color(0xFFE2E8F0),
        topLeft = Offset(satX - 7f, satY - 7f),
        size = Size(14f, 14f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    // Solar Panels (Left & Right)
    drawRoundRect(
        color = Color(0xFF38BDF8),
        topLeft = Offset(satX - 26f, satY - 5f),
        size = Size(16f, 10f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = Color(0xFF38BDF8),
        topLeft = Offset(satX + 10f, satY - 5f),
        size = Size(16f, 10f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    // Antenna blinking red LED
    val ledAlpha = if (pulse > 0.6f) 1f else 0.2f
    drawCircle(color = Color(0xFFEF4444).copy(alpha = ledAlpha), radius = 2.5f, center = Offset(satX, satY - 10f))

    // Animated Shooting Star / Comet across the sky
    val cometProgress = (phase / (2f * Math.PI.toFloat()))
    val cometStartX = w * 0.9f - (cometProgress * w * 1.2f)
    val cometStartY = h * 0.05f + (cometProgress * h * 0.35f)
    val cometLen = 45f

    if (cometProgress in 0.1f..0.85f) {
        drawLine(
            brush = Brush.linearGradient(
                listOf(Color.White, Color(0xFF67E8F9), Color.Transparent),
                start = Offset(cometStartX, cometStartY),
                end = Offset(cometStartX + cometLen, cometStartY - (cometLen * 0.45f))
            ),
            start = Offset(cometStartX, cometStartY),
            end = Offset(cometStartX + cometLen, cometStartY - (cometLen * 0.45f)),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }

    // Lunar/Asteroid Surface Platform (Bottom Stage for the pet)
    val floorY = h * 0.70f
    val surfacePath = Path().apply {
        moveTo(0f, floorY + 10f)
        quadraticBezierTo(w * 0.25f, floorY - 12f, w * 0.5f, floorY)
        quadraticBezierTo(w * 0.75f, floorY + 12f, w, floorY - 5f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    val surfaceBrush = Brush.verticalGradient(
        listOf(Color(0xFF475569), Color(0xFF334155), Color(0xFF1E293B)),
        startY = floorY - 12f,
        endY = h
    )
    drawPath(path = surfacePath, brush = surfaceBrush)

    // Lunar Craters
    val craters = listOf(
        Pair(0.18f, floorY + 35f),
        Pair(0.42f, floorY + 55f),
        Pair(0.72f, floorY + 30f),
        Pair(0.88f, floorY + 60f)
    )
    for ((cxRel, cy) in craters) {
        val cx = w * cxRel
        drawOval(
            color = Color(0xFF1E293B),
            topLeft = Offset(cx - 16f, cy - 8f),
            size = Size(32f, 16f)
        )
        drawOval(
            color = Color(0xFF64748B).copy(alpha = 0.6f),
            topLeft = Offset(cx - 14f, cy - 6f),
            size = Size(28f, 12f),
            style = Stroke(width = 2f)
        )
    }
}
