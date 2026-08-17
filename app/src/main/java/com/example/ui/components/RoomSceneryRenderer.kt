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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders rich, animated thematic room environments with layered scenery
 * for Bedroom (Quarto Aconchegante), Forest (Floresta Mágica), Beach (Praia Tropical),
 * and Space (Espaço Sideral).
 *
 * Scenery layers strictly sit inside the pet living stage boundaries,
 * keeping the furniture perfectly framed and completely visible above all status panels.
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
// 1. QUARTO ACONCHEGANTE (Cozy Bedroom) - AUDITADO E REFORMULADO
// -------------------------------------------------------------------------------------------------
private fun DrawScope.drawCozyBedroomScene(
    w: Float,
    h: Float,
    phase: Float,
    pulse: Float,
    isSleeping: Boolean
) {
    // 1. Linha de piso visual bem delineada (70% da altura para ancorar perfeitamente o bichinho e móveis)
    val floorY = h * 0.70f

    // ---------------------------------------------------------------------------------------------
    // PAREDE & PAPEL DE PAREDE
    // ---------------------------------------------------------------------------------------------
    val wallGradient = if (isSleeping) {
        listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
    } else {
        listOf(Color(0xFFFFF7ED), Color(0xFFFDF2F8), Color(0xFFFCE7F3))
    }
    drawRect(
        brush = Brush.verticalGradient(wallGradient, startY = 0f, endY = floorY),
        topLeft = Offset(0f, 0f),
        size = Size(w, floorY)
    )

    // Listras decorativas sutis na parede (painel clássico)
    val stripeCount = 12
    val stripeWidth = w / stripeCount
    val stripeColor = if (isSleeping) Color(0xFF1E293B).copy(alpha = 0.35f) else Color(0xFFFBCFE8).copy(alpha = 0.25f)
    for (i in 0 until stripeCount step 2) {
        drawRect(
            color = stripeColor,
            topLeft = Offset(i * stripeWidth, 0f),
            size = Size(stripeWidth, floorY)
        )
    }

    // Moldura superior de teto (Crown Moulding)
    drawRect(
        color = if (isSleeping) Color(0xFF1E293B) else Color(0xFFFFFFFF),
        topLeft = Offset(0f, 0f),
        size = Size(w, 8f)
    )
    drawLine(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFE2E8F0),
        start = Offset(0f, 8f),
        end = Offset(w, 8f),
        strokeWidth = 2f
    )

    // Rodapé clássico detalhado (Baseboard)
    drawRect(
        color = if (isSleeping) Color(0xFF1E293B) else Color(0xFFF8FAFC),
        topLeft = Offset(0f, floorY - 14f),
        size = Size(w, 14f)
    )
    drawRect(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFFFFFFF),
        topLeft = Offset(0f, floorY - 14f),
        size = Size(w, 3f)
    )
    drawLine(
        color = if (isSleeping) Color(0xFF0F172A) else Color(0xFFCBD5E1),
        start = Offset(0f, floorY),
        end = Offset(w, floorY),
        strokeWidth = 2.5f
    )

    // ---------------------------------------------------------------------------------------------
    // PISO DE MADEIRA (Hardwood parquet floor)
    // ---------------------------------------------------------------------------------------------
    val floorTop = if (isSleeping) Color(0xFF292524) else Color(0xFFDEB887)
    val floorMid = if (isSleeping) Color(0xFF1C1917) else Color(0xFFC49A6C)
    val floorBot = if (isSleeping) Color(0xFF0C0A09) else Color(0xFF9A744B)
    drawRect(
        brush = Brush.verticalGradient(listOf(floorTop, floorMid, floorBot), startY = floorY, endY = h),
        topLeft = Offset(0f, floorY),
        size = Size(w, h - floorY)
    )

    // Tábuas de madeira com perspectiva realista
    val plankCount = 8
    for (i in 0..plankCount) {
        val pxTop = (w / plankCount) * i
        val pxBottom = (pxTop - w * 0.5f) * 1.18f + w * 0.5f
        drawLine(
            color = if (isSleeping) Color(0xFF0C0A09).copy(alpha = 0.5f) else Color(0xFF78350F).copy(alpha = 0.30f),
            start = Offset(pxTop, floorY),
            end = Offset(pxBottom, h),
            strokeWidth = 2f
        )
    }

    // ---------------------------------------------------------------------------------------------
    // JANELA AMPLIADA (Top-Left / Center-Left acima da cama)
    // ---------------------------------------------------------------------------------------------
    val winW = (w * 0.28f).coerceIn(90f, 150f)
    val winH = winW * 1.15f
    val winX = w * 0.07f
    val winY = (h * 0.07f).coerceIn(12f, 36f)

    // Sombra da janela na parede
    drawRoundRect(
        color = Color(0x1A000000),
        topLeft = Offset(winX - 3f, winY - 3f),
        size = Size(winW + 6f, winH + 10f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Moldura externa da janela
    drawRoundRect(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFFFFFFF),
        topLeft = Offset(winX - 5f, winY - 5f),
        size = Size(winW + 10f, winH + 10f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    // Parapeito da janela
    drawRoundRect(
        color = if (isSleeping) Color(0xFF475569) else Color(0xFFF1F5F9),
        topLeft = Offset(winX - 10f, winY + winH - 2f),
        size = Size(winW + 20f, 8f),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // Vidro da janela e Paisagem Externa
    val skyBrush = if (isSleeping) {
        Brush.verticalGradient(listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E293B)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF93C5FD), Color(0xFFFEF3C7)))
    }
    drawRoundRect(
        brush = skyBrush,
        topLeft = Offset(winX, winY),
        size = Size(winW, winH),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Elementos do Céu dentro da janela
    if (isSleeping) {
        // Céu Noturno: Estrelas cintilantes
        val starAlpha1 = 0.5f + (pulse * 0.5f)
        val starAlpha2 = 0.9f - (pulse * 0.4f)
        drawCircle(Color.White.copy(alpha = starAlpha1), radius = 2f, center = Offset(winX + winW * 0.20f, winY + winH * 0.22f))
        drawCircle(Color(0xFFFEF08A).copy(alpha = starAlpha2), radius = 2.5f, center = Offset(winX + winW * 0.45f, winY + winH * 0.15f))
        drawCircle(Color.White.copy(alpha = starAlpha1), radius = 1.8f, center = Offset(winX + winW * 0.82f, winY + winH * 0.28f))
        drawCircle(Color.White.copy(alpha = starAlpha2), radius = 2f, center = Offset(winX + winW * 0.28f, winY + winH * 0.48f))
        drawCircle(Color(0xFFBAE6FD).copy(alpha = starAlpha1), radius = 1.5f, center = Offset(winX + winW * 0.60f, winY + winH * 0.40f))

        // Lua Crescente brilhante com halo
        val moonX = winX + winW * 0.70f
        val moonY = winY + winH * 0.32f
        val moonR = winW * 0.18f
        // Halo suave
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFEF08A).copy(alpha = 0.25f), Color.Transparent),
                center = Offset(moonX, moonY),
                radius = moonR * 2.2f
            ),
            radius = moonR * 2.2f,
            center = Offset(moonX, moonY)
        )
        // Lua
        drawCircle(Color(0xFFFEF08A), radius = moonR, center = Offset(moonX, moonY))
        drawCircle(Color(0xFF0F172A), radius = moonR * 0.85f, center = Offset(moonX - moonR * 0.35f, moonY - moonR * 0.15f))
    } else {
        // Céu Diurno: Colinas verdes ao fundo
        val hillPath = Path().apply {
            moveTo(winX, winY + winH)
            lineTo(winX, winY + winH * 0.70f)
            quadraticTo(winX + winW * 0.45f, winY + winH * 0.55f, winX + winW, winY + winH * 0.68f)
            lineTo(winX + winW, winY + winH)
            close()
        }
        drawPath(hillPath, color = Color(0xFF4ADE80))

        // Sol radiante com corona
        val sunX = winX + winW * 0.74f
        val sunY = winY + winH * 0.30f
        val sunR = winW * 0.18f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFDE047).copy(alpha = 0.4f), Color.Transparent),
                center = Offset(sunX, sunY),
                radius = sunR * 1.8f
            ),
            radius = sunR * 1.8f,
            center = Offset(sunX, sunY)
        )
        drawCircle(Color(0xFFFBBF24), radius = sunR, center = Offset(sunX, sunY))
        drawCircle(Color(0xFFFDE047), radius = sunR * 0.75f, center = Offset(sunX - 2f, sunY - 2f))

        // Nuvens fofinhas animadas flutuando suavemente
        val cloudShift = (phase * 16f) % (winW * 0.5f)
        val cloudX = winX + winW * 0.18f + cloudShift
        val cloudY = winY + winH * 0.38f
        drawCircle(Color.White.copy(alpha = 0.90f), radius = winW * 0.10f, center = Offset(cloudX, cloudY))
        drawCircle(Color.White.copy(alpha = 0.90f), radius = winW * 0.13f, center = Offset(cloudX + winW * 0.10f, cloudY - 3f))
        drawCircle(Color.White.copy(alpha = 0.90f), radius = winW * 0.09f, center = Offset(cloudX + winW * 0.20f, cloudY + 1f))
    }

    // Travessas da janela (4 painéis)
    drawLine(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFE2E8F0),
        start = Offset(winX + winW / 2, winY),
        end = Offset(winX + winW / 2, winY + winH),
        strokeWidth = 3f
    )
    drawLine(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFE2E8F0),
        start = Offset(winX, winY + winH * 0.45f),
        end = Offset(winX + winW, winY + winH * 0.45f),
        strokeWidth = 3f
    )

    // Cortinas drapeadas nas laterais
    val curtainColor = if (isSleeping) Color(0xFF475569) else Color(0xFFFBCFE8)
    val curtainTieColor = if (isSleeping) Color(0xFF64748B) else Color(0xFFF472B6)
    // Cortina esquerda
    val leftCurtain = Path().apply {
        moveTo(winX - 6f, winY - 4f)
        lineTo(winX + winW * 0.20f, winY - 4f)
        quadraticTo(winX + winW * 0.08f, winY + winH * 0.50f, winX + winW * 0.18f, winY + winH + 4f)
        lineTo(winX - 6f, winY + winH + 4f)
        close()
    }
    drawPath(leftCurtain, color = curtainColor)
    drawRoundRect(
        color = curtainTieColor,
        topLeft = Offset(winX - 4f, winY + winH * 0.48f),
        size = Size(winW * 0.14f, 5f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Cortina direita
    val rightCurtain = Path().apply {
        moveTo(winX + winW + 6f, winY - 4f)
        lineTo(winX + winW - winW * 0.20f, winY - 4f)
        quadraticTo(winX + winW - winW * 0.08f, winY + winH * 0.50f, winX + winW - winW * 0.18f, winY + winH + 4f)
        lineTo(winX + winW + 6f, winY + winH + 4f)
        close()
    }
    drawPath(rightCurtain, color = curtainColor)
    drawRoundRect(
        color = curtainTieColor,
        topLeft = Offset(winX + winW - winW * 0.10f, winY + winH * 0.48f),
        size = Size(winW * 0.14f, 5f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // ---------------------------------------------------------------------------------------------
    // QUADRO DECORATIVO NA PAREDE (Entre janela e estante)
    // ---------------------------------------------------------------------------------------------
    val frameX = w * 0.40f
    val frameY = winY + 6f
    val frameW = (w * 0.12f).coerceIn(38f, 65f)
    val frameH = frameW * 1.1f
    drawRoundRect(
        color = if (isSleeping) Color(0xFF475569) else Color(0xFFB45309),
        topLeft = Offset(frameX, frameY),
        size = Size(frameW, frameH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = if (isSleeping) Color(0xFF1E293B) else Color(0xFFFFFBEB),
        topLeft = Offset(frameX + 3f, frameY + 3f),
        size = Size(frameW - 6f, frameH - 6f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    // Mini ilustração de coração/pata no quadro
    drawCircle(
        color = if (isSleeping) Color(0xFF38BDF8) else Color(0xFFF43F5E),
        radius = frameW * 0.18f,
        center = Offset(frameX + frameW / 2, frameY + frameH / 2)
    )

    // ---------------------------------------------------------------------------------------------
    // 2. CAMA ACONCHEGANTE (Aumentada em 200%, Lateral Esquerda)
    // ---------------------------------------------------------------------------------------------
    val bedW = (w * 0.38f).coerceIn(135f, 230f)
    val bedH = (h * 0.30f).coerceIn(85f, 140f)
    val bedX = w * 0.02f
    val bedY = floorY - bedH * 0.72f

    // Sombra da cama no piso
    drawOval(
        color = Color(0x35000000),
        topLeft = Offset(bedX - 4f, floorY - 6f),
        size = Size(bedW + 12f, 16f)
    )

    // Cabeceira da cama de madeira nobre (Headboard)
    val headboardW = bedW * 0.20f
    val headboardH = bedH * 1.05f
    val woodDark = if (isSleeping) Color(0xFF292524) else Color(0xFF78350F)
    val woodMedium = if (isSleeping) Color(0xFF44403C) else Color(0xFF92400E)
    val woodLight = if (isSleeping) Color(0xFF57534E) else Color(0xFFB45309)

    // Postes da cabeceira
    drawRoundRect(
        color = woodDark,
        topLeft = Offset(bedX, bedY - headboardH * 0.25f),
        size = Size(10f, headboardH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawCircle(
        color = woodLight,
        radius = 7f,
        center = Offset(bedX + 5f, bedY - headboardH * 0.25f)
    )

    // Painel principal da cabeceira
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(woodLight, woodMedium, woodDark)),
        topLeft = Offset(bedX + 4f, bedY - headboardH * 0.20f),
        size = Size(headboardW, headboardH * 0.85f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    // Detalhes entalhados da cabeceira
    for (panel in 1..2) {
        val px = bedX + 4f + (headboardW / 3f) * panel
        drawLine(
            color = woodDark,
            start = Offset(px, bedY - headboardH * 0.18f),
            end = Offset(px, bedY + headboardH * 0.50f),
            strokeWidth = 2f
        )
    }

    // Estrutura / Base de madeira da cama
    drawRoundRect(
        color = woodMedium,
        topLeft = Offset(bedX + 6f, bedY + bedH * 0.35f),
        size = Size(bedW - 6f, bedH * 0.45f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    // Pés de apoio da cama
    drawRoundRect(
        color = woodDark,
        topLeft = Offset(bedX + 8f, floorY - 12f),
        size = Size(8f, 14f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = woodDark,
        topLeft = Offset(bedX + bedW - 14f, floorY - 12f),
        size = Size(8f, 14f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Colchão espesso e macio (Plush White Mattress)
    val mattressX = bedX + headboardW * 0.5f
    val mattressW = bedW - (headboardW * 0.5f)
    val mattressY = bedY + bedH * 0.18f
    val mattressH = bedH * 0.48f

    drawRoundRect(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFF8FAFC),
        topLeft = Offset(mattressX, mattressY),
        size = Size(mattressW, mattressH),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Borda/costura do colchão
    drawLine(
        color = if (isSleeping) Color(0xFF475569) else Color(0xFFE2E8F0),
        start = Offset(mattressX + 4f, mattressY + mattressH * 0.35f),
        end = Offset(mattressX + mattressW - 4f, mattressY + mattressH * 0.35f),
        strokeWidth = 2f
    )

    // Travesseiro Duplo Macio (Double Fluffy Pillows)
    val pillowW = mattressW * 0.28f
    val pillowH = mattressH * 0.52f
    val pillowX = mattressX + 6f
    val pillowY = mattressY + 4f

    // Travesseiro de trás (sombra)
    drawRoundRect(
        color = if (isSleeping) Color(0xFF1E293B) else Color(0xFFE2E8F0),
        topLeft = Offset(pillowX + 2f, pillowY - 4f),
        size = Size(pillowW, pillowH),
        cornerRadius = CornerRadius(6f, 6f)
    )
    // Travesseiro frontal
    drawRoundRect(
        color = if (isSleeping) Color(0xFF475569) else Color(0xFFFFFFFF),
        topLeft = Offset(pillowX, pillowY),
        size = Size(pillowW, pillowH),
        cornerRadius = CornerRadius(7f, 7f)
    )
    // Dobra/afundamento central do travesseiro
    drawOval(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFF1F5F9),
        topLeft = Offset(pillowX + pillowW * 0.30f, pillowY + pillowH * 0.30f),
        size = Size(pillowW * 0.40f, pillowH * 0.40f)
    )

    // Edredom / Cobertor Aconchegante (Quilt / Duvet)
    val quiltX = mattressX + pillowW * 0.65f
    val quiltW = mattressW - (pillowW * 0.65f) + 4f
    val quiltY = mattressY + 2f
    val quiltH = mattressH + 2f
    val quiltColor = if (isSleeping) Color(0xFF1E3A8A) else Color(0xFF818CF8)
    val quiltDark = if (isSleeping) Color(0xFF172554) else Color(0xFF6366F1)
    val quiltFoldColor = if (isSleeping) Color(0xFF38BDF8) else Color(0xFFC7D2FE)

    // Corpo do edredom
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(quiltColor, quiltDark)),
        topLeft = Offset(quiltX, quiltY),
        size = Size(quiltW, quiltH),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Dobra do lençol no topo do cobertor
    drawRoundRect(
        color = quiltFoldColor,
        topLeft = Offset(quiltX - 2f, quiltY),
        size = Size(12f, quiltH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Detalhes acolchoados / pespontos diagonais no edredom
    drawLine(
        color = quiltFoldColor.copy(alpha = 0.35f),
        start = Offset(quiltX + quiltW * 0.35f, quiltY + 4f),
        end = Offset(quiltX + quiltW * 0.85f, quiltY + quiltH - 4f),
        strokeWidth = 2f
    )
    drawLine(
        color = quiltFoldColor.copy(alpha = 0.35f),
        start = Offset(quiltX + quiltW * 0.60f, quiltY + 4f),
        end = Offset(quiltX + quiltW * 0.98f, quiltY + quiltH * 0.70f),
        strokeWidth = 2f
    )

    // Criado-Mudo com reloginho despertador (À esquerda da cabeceira)
    val woodStandColor = if (isSleeping) Color(0xFF1C1917) else Color(0xFF92400E)
    drawRoundRect(
        color = woodStandColor,
        topLeft = Offset(bedX + 2f, floorY - 24f),
        size = Size(16f, 24f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    // Mini relógio despertador
    drawCircle(
        color = if (isSleeping) Color(0xFF38BDF8) else Color(0xFFEF4444),
        radius = 4.5f,
        center = Offset(bedX + 10f, floorY - 28f)
    )

    // ---------------------------------------------------------------------------------------------
    // 3. TAPETE CENTRALIZADO (Plush Rug sob o bichinho)
    // ---------------------------------------------------------------------------------------------
    val rugWidth = (w * 0.46f).coerceIn(150f, 290f)
    val rugHeight = (h * 0.16f).coerceIn(40f, 72f)
    val rugCenter = Offset(w * 0.5f, floorY + rugHeight * 0.30f)

    // Sombra do tapete no chão
    drawOval(
        color = Color(0x38000000),
        topLeft = Offset(rugCenter.x - rugWidth / 2 - 4f, rugCenter.y - rugHeight / 2 + 3f),
        size = Size(rugWidth + 8f, rugHeight + 4f)
    )
    // Camada externa do tapete
    drawOval(
        color = if (isSleeping) Color(0xFF475569) else Color(0xFFFBCFE8),
        topLeft = Offset(rugCenter.x - rugWidth / 2, rugCenter.y - rugHeight / 2),
        size = Size(rugWidth, rugHeight)
    )
    // Camada intermediária com tom rosa suave/índigo
    drawOval(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFF472B6),
        topLeft = Offset(rugCenter.x - rugWidth * 0.42f, rugCenter.y - rugHeight * 0.42f),
        size = Size(rugWidth * 0.84f, rugHeight * 0.84f)
    )
    // Camada interna macia
    drawOval(
        color = if (isSleeping) Color(0xFF1E293B) else Color(0xFFFFF1F2),
        topLeft = Offset(rugCenter.x - rugWidth * 0.32f, rugCenter.y - rugHeight * 0.32f),
        size = Size(rugWidth * 0.64f, rugHeight * 0.64f)
    )
    // Detalhes pontilhados de franja nas bordas
    val fringeCount = 14
    for (f in 0 until fringeCount) {
        val angle = (f / fringeCount.toFloat()) * 6.28318f
        val fx = rugCenter.x + cos(angle) * (rugWidth * 0.48f)
        val fy = rugCenter.y + sin(angle) * (rugHeight * 0.48f)
        drawCircle(
            color = if (isSleeping) Color(0xFF64748B) else Color(0xFFFDA4AF),
            radius = 2f,
            center = Offset(fx, fy)
        )
    }

    // ---------------------------------------------------------------------------------------------
    // 4. ESTANTE DE LIVROS AMPLIADA (Bookshelf na Extrema Direita)
    // ---------------------------------------------------------------------------------------------
    val shelfX = w * 0.79f
    val shelfW = (w * 0.19f).coerceIn(65f, 105f)
    val shelfH = (h * 0.45f).coerceIn(120f, 190f)
    val shelfY = floorY - shelfH

    // Sombra da estante no chão
    drawOval(
        color = Color(0x30000000),
        topLeft = Offset(shelfX - 3f, floorY - 4f),
        size = Size(shelfW + 6f, 10f)
    )
    // Estrutura principal da estante (Corpo de madeira nobre)
    drawRoundRect(
        color = if (isSleeping) Color(0xFF1C1917) else Color(0xFF78350F),
        topLeft = Offset(shelfX, shelfY),
        size = Size(shelfW, shelfH),
        cornerRadius = CornerRadius(5f, 5f)
    )
    drawRoundRect(
        color = if (isSleeping) Color(0xFF292524) else Color(0xFF92400E),
        topLeft = Offset(shelfX + 3f, shelfY + 3f),
        size = Size(shelfW - 6f, shelfH - 6f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Prateleiras internas (4 níveis)
    val shelfLevels = 4
    val bookPalette = listOf(
        Color(0xFFEF4444), // Vermelho
        Color(0xFF3B82F6), // Azul
        Color(0xFF10B981), // Verde
        Color(0xFFF59E0B), // Âmbar
        Color(0xFF8B5CF6), // Roxo
        Color(0xFFEC4899), // Rosa
        Color(0xFF14B8A6)  // Turquesa
    )

    for (s in 1 until shelfLevels) {
        val sy = shelfY + (shelfH / shelfLevels) * s
        // Prancha de madeira da prateleira
        drawRect(
            color = if (isSleeping) Color(0xFF44403C) else Color(0xFFB45309),
            topLeft = Offset(shelfX + 2f, sy),
            size = Size(shelfW - 4f, 4f)
        )

        // Livros enfileirados em cada prateleira
        val booksInShelf = 3 + (s % 2)
        val maxBookHeight = (shelfH / shelfLevels) * 0.70f

        for (b in 0 until booksInShelf) {
            val bookW = 6.5f
            val bx = shelfX + 6f + (b * (bookW + 2f))
            val bh = maxBookHeight * (0.75f + ((b * 37) % 30) / 100f)
            val bColor = bookPalette[(s * 3 + b) % bookPalette.size]

            if (bx + bookW < shelfX + shelfW - 4f) {
                // Livro com lombada e título dourado/claro
                drawRoundRect(
                    color = if (isSleeping) bColor.copy(alpha = 0.55f) else bColor,
                    topLeft = Offset(bx, sy - bh),
                    size = Size(bookW, bh),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
                drawLine(
                    color = if (isSleeping) Color(0xFF94A3B8) else Color(0xFFFEF08A),
                    start = Offset(bx + 1.5f, sy - bh + 3f),
                    end = Offset(bx + bookW - 1.5f, sy - bh + 3f),
                    strokeWidth = 1f
                )
            }
        }
    }

    // Topo da Estante: Vasinho com Planta Pendente (Jiboia) & Troféu Dourado
    val potX = shelfX + 8f
    val potY = shelfY - 12f
    // Vaso de cerâmica
    drawRoundRect(
        color = if (isSleeping) Color(0xFF475569) else Color(0xFFEA580C),
        topLeft = Offset(potX, potY),
        size = Size(14f, 12f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    // Folhinhas verdes pendentes caindo pela lateral
    val plantGreen = if (isSleeping) Color(0xFF065F46) else Color(0xFF22C55E)
    drawCircle(plantGreen, radius = 5f, center = Offset(potX + 4f, potY - 2f))
    drawCircle(plantGreen, radius = 6f, center = Offset(potX + 11f, potY - 3f))
    drawCircle(plantGreen, radius = 4f, center = Offset(potX - 3f, potY + 6f))
    drawCircle(plantGreen, radius = 3.5f, center = Offset(potX - 4f, potY + 14f))

    // Mini Troféu de ouro no topo direito da estante
    val trophyX = shelfX + shelfW - 16f
    drawRoundRect(
        color = if (isSleeping) Color(0xFF94A3B8) else Color(0xFFFBBF24),
        topLeft = Offset(trophyX, shelfY - 14f),
        size = Size(10f, 14f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // ---------------------------------------------------------------------------------------------
    // 5. ESCRIVANINHA AMPLIADA + CADEIRA + LIVROS + ABAJUR (Entre Tapete e Estante)
    // ---------------------------------------------------------------------------------------------
    val deskW = (w * 0.22f).coerceIn(75f, 120f)
    val deskH = (h * 0.24f).coerceIn(65f, 98f)
    val deskX = shelfX - deskW - (w * 0.02f)
    val deskY = floorY - deskH

    // Sombra da escrivaninha e cadeira no chão
    drawOval(
        color = Color(0x32000000),
        topLeft = Offset(deskX - 4f, floorY - 5f),
        size = Size(deskW + 10f, 12f)
    )

    // Tampo reforçado da escrivaninha
    drawRoundRect(
        color = if (isSleeping) Color(0xFF292524) else Color(0xFFB45309),
        topLeft = Offset(deskX, deskY),
        size = Size(deskW, 10f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = if (isSleeping) Color(0xFF44403C) else Color(0xFFD97706),
        topLeft = Offset(deskX + 2f, deskY + 1f),
        size = Size(deskW - 4f, 4f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Pés de suporte da escrivaninha (lado esquerdo)
    drawLine(
        color = if (isSleeping) Color(0xFF1C1917) else Color(0xFF78350F),
        start = Offset(deskX + 5f, deskY + 10f),
        end = Offset(deskX + 5f, floorY),
        strokeWidth = 4f
    )

    // Gaveteiro com 2 gavetas (lado direito da mesa)
    val drawerW = deskW * 0.38f
    val drawerX = deskX + deskW - drawerW - 3f
    val drawerH = deskH * 0.70f
    drawRoundRect(
        color = if (isSleeping) Color(0xFF292524) else Color(0xFF92400E),
        topLeft = Offset(drawerX, deskY + 10f),
        size = Size(drawerW, drawerH),
        cornerRadius = CornerRadius(3f, 3f)
    )
    // 2 Gavetas com puxadores de metal dourado
    for (d in 0..1) {
        val dy = deskY + 12f + (d * (drawerH * 0.48f))
        drawRoundRect(
            color = if (isSleeping) Color(0xFF44403C) else Color(0xFFB45309),
            topLeft = Offset(drawerX + 2f, dy),
            size = Size(drawerW - 4f, drawerH * 0.42f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        // Puxador de latão
        drawCircle(
            color = if (isSleeping) Color(0xFF94A3B8) else Color(0xFFF59E0B),
            radius = 2.5f,
            center = Offset(drawerX + drawerW / 2, dy + drawerH * 0.21f)
        )
    }

    // Cadeira de Estudo (Study Chair posicionada na frente da escrivaninha)
    val chairX = deskX + deskW * 0.10f
    val chairW = deskW * 0.42f
    val chairSeatY = floorY - deskH * 0.48f

    // Pés da cadeira
    drawLine(
        color = if (isSleeping) Color(0xFF1C1917) else Color(0xFF78350F),
        start = Offset(chairX + 4f, chairSeatY + 6f),
        end = Offset(chairX + 4f, floorY),
        strokeWidth = 3f
    )
    drawLine(
        color = if (isSleeping) Color(0xFF1C1917) else Color(0xFF78350F),
        start = Offset(chairX + chairW - 4f, chairSeatY + 6f),
        end = Offset(chairX + chairW - 4f, floorY),
        strokeWidth = 3f
    )
    // Assento estofado da cadeira
    drawRoundRect(
        color = if (isSleeping) Color(0xFF475569) else Color(0xFFF43F5E),
        topLeft = Offset(chairX, chairSeatY),
        size = Size(chairW, 7f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    // Encosto de madeira da cadeira
    drawRoundRect(
        color = if (isSleeping) Color(0xFF292524) else Color(0xFF92400E),
        topLeft = Offset(chairX + 2f, chairSeatY - deskH * 0.35f),
        size = Size(chairW - 4f, 6f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    // Barras verticais do encosto
    for (spindle in 1..2) {
        val spX = chairX + 2f + ((chairW - 4f) / 3f) * spindle
        drawLine(
            color = if (isSleeping) Color(0xFF292524) else Color(0xFF92400E),
            start = Offset(spX, chairSeatY - deskH * 0.35f + 6f),
            end = Offset(spX, chairSeatY),
            strokeWidth = 2f
        )
    }

    // Acessórios na mesa: Notebook aberto + Caneca de café fumegante
    val laptopX = deskX + 8f
    val laptopY = deskY - 14f
    // Base do notebook
    drawRoundRect(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFF94A3B8),
        topLeft = Offset(laptopX, deskY - 3f),
        size = Size(18f, 3f),
        cornerRadius = CornerRadius(1f, 1f)
    )
    // Tela iluminada
    drawRoundRect(
        color = if (isSleeping) Color(0xFF1E293B) else Color(0xFF38BDF8),
        topLeft = Offset(laptopX + 2f, laptopY),
        size = Size(14f, 11f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Caneca com vapor
    val mugX = deskX + 30f
    val mugY = deskY - 9f
    drawRoundRect(
        color = if (isSleeping) Color(0xFF475569) else Color(0xFFEF4444),
        topLeft = Offset(mugX, mugY),
        size = Size(7f, 9f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    // Vapor subindo da caneca
    if (!isSleeping) {
        val steamAlpha = 0.4f + (pulse * 0.4f)
        drawLine(
            color = Color.White.copy(alpha = steamAlpha),
            start = Offset(mugX + 3.5f, mugY - 2f),
            end = Offset(mugX + 5f, mugY - 7f),
            strokeWidth = 1.5f
        )
    }

    // Abajur Clássico (Desk Lamp)
    val lampX = deskX + deskW * 0.72f
    val lampBaseY = deskY
    val lampTopY = deskY - 24f

    // Base redonda do abajur
    drawRoundRect(
        color = if (isSleeping) Color(0xFF64748B) else Color(0xFFD97706),
        topLeft = Offset(lampX - 7f, lampBaseY - 3f),
        size = Size(14f, 3f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    // Haste metálica
    drawLine(
        color = if (isSleeping) Color(0xFF94A3B8) else Color(0xFFF59E0B),
        start = Offset(lampX, lampBaseY - 3f),
        end = Offset(lampX, lampTopY + 8f),
        strokeWidth = 3f
    )

    // Cúpula cônica do abajur (Shade)
    val shadePath = Path().apply {
        moveTo(lampX - 11f, lampTopY + 9f)
        lineTo(lampX + 11f, lampTopY + 9f)
        lineTo(lampX + 6f, lampTopY)
        lineTo(lampX - 6f, lampTopY)
        close()
    }
    drawPath(
        path = shadePath,
        color = if (isSleeping) Color(0xFFF59E0B).copy(alpha = 0.9f) else Color(0xFFF59E0B)
    )

    // Iluminação radiante aconchegante do Abajur (Luz Dourada suave com pulsação)
    val glowAlpha = if (isSleeping) 0.32f + (pulse * 0.12f) else 0.22f + (pulse * 0.06f)
    val glowRadius = if (isSleeping) 65f else 50f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFEF08A).copy(alpha = glowAlpha), Color(0xFFFDE047).copy(alpha = glowAlpha * 0.4f), Color.Transparent),
            center = Offset(lampX, lampTopY + 8f),
            radius = glowRadius
        ),
        radius = glowRadius,
        center = Offset(lampX, lampTopY + 8f)
    )

    // ---------------------------------------------------------------------------------------------
    // ILUMINAÇÃO NOTURNA GERAL (Quando isSleeping == true)
    // ---------------------------------------------------------------------------------------------
    if (isSleeping) {
        // Suave vinheta azulada noturna no quarto, mantendo o aconchego
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0x22020617), Color(0x350F172A), Color(0x450F172A)),
                startY = 0f,
                endY = h
            ),
            topLeft = Offset(0f, 0f),
            size = Size(w, h)
        )
    }
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
    val groundY = h * 0.70f

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
        moveTo(0f, groundY - 15f)
        quadraticBezierTo(w * 0.25f, groundY - 45f, w * 0.5f, groundY - 20f)
        quadraticBezierTo(w * 0.75f, groundY - 45f, w, groundY - 15f)
        lineTo(w, groundY)
        lineTo(0f, groundY)
        close()
    }
    drawPath(
        path = hillPath,
        color = if (isSleeping) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFF34D399).copy(alpha = 0.5f)
    )

    // Left Big Tree
    val trunkLeftX = w * 0.06f
    drawRoundRect(
        color = if (isSleeping) Color(0xFF1C1917) else Color(0xFF78350F),
        topLeft = Offset(trunkLeftX, groundY - 110f),
        size = Size(20f, 120f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    val swayLeft = sin(phase) * 5f
    drawCircle(
        color = if (isSleeping) Color(0xFF065F46) else Color(0xFF059669),
        radius = 42f,
        center = Offset(trunkLeftX + 10f + swayLeft, groundY - 110f)
    )
    drawCircle(
        color = if (isSleeping) Color(0xFF047857) else Color(0xFF10B981),
        radius = 35f,
        center = Offset(trunkLeftX - 14f + swayLeft, groundY - 95f)
    )

    // Right Big Tree
    val trunkRightX = w * 0.86f
    drawRoundRect(
        color = if (isSleeping) Color(0xFF1C1917) else Color(0xFF78350F),
        topLeft = Offset(trunkRightX, groundY - 120f),
        size = Size(22f, 130f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    val swayRight = cos(phase) * 5f
    drawCircle(
        color = if (isSleeping) Color(0xFF065F46) else Color(0xFF059669),
        radius = 45f,
        center = Offset(trunkRightX + 11f + swayRight, groundY - 120f)
    )
    drawCircle(
        color = if (isSleeping) Color(0xFF047857) else Color(0xFF10B981),
        radius = 38f,
        center = Offset(trunkRightX + 28f + swayRight, groundY - 100f)
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
    for (i in 0..10) {
        val gx = (w / 10f) * i + (sin(i.toFloat()) * 10f)
        val gy = groundY + (i % 3) * 4f
        val grassColor = if (isSleeping) Color(0xFF34D399).copy(alpha = 0.4f) else Color(0xFF6EE7B7)
        drawLine(
            color = grassColor,
            start = Offset(gx, gy),
            end = Offset(gx - 3f, gy - 8f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }

    // Magic Glowing Mushrooms (Left Floor)
    val mush1X = w * 0.16f
    val mush1Y = groundY + (h * 0.12f)
    drawRoundRect(
        color = Color(0xFFF1F5F9),
        topLeft = Offset(mush1X - 3f, mush1Y - 10f),
        size = Size(6f, 12f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawArc(
        color = Color(0xFFEF4444),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(mush1X - 12f, mush1Y - 20f),
        size = Size(24f, 20f)
    )
    drawCircle(color = Color.White, radius = 2f, center = Offset(mush1X - 4f, mush1Y - 14f))
    drawCircle(color = Color.White, radius = 2f, center = Offset(mush1X + 4f, mush1Y - 15f))
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFCA5A5).copy(alpha = 0.4f * pulse), Color.Transparent),
            center = Offset(mush1X, mush1Y - 12f),
            radius = 26f
        ),
        radius = 26f,
        center = Offset(mush1X, mush1Y - 12f)
    )

    // Smaller Cyan Magic Mushroom (Right Floor)
    val mush2X = w * 0.80f
    val mush2Y = groundY + (h * 0.14f)
    drawRoundRect(
        color = Color(0xFFF1F5F9),
        topLeft = Offset(mush2X - 2.5f, mush2Y - 8f),
        size = Size(5f, 10f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawArc(
        color = Color(0xFF06B6D4),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(mush2X - 9f, mush2Y - 16f),
        size = Size(18f, 16f)
    )
    drawCircle(color = Color.White, radius = 1.5f, center = Offset(mush2X - 2f, mush2Y - 11f))
    drawCircle(color = Color.White, radius = 1.5f, center = Offset(mush2X + 3f, mush2Y - 12f))
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF67E8F9).copy(alpha = 0.45f * pulse), Color.Transparent),
            center = Offset(mush2X, mush2Y - 10f),
            radius = 22f
        ),
        radius = 22f,
        center = Offset(mush2X, mush2Y - 10f)
    )

    // Animated Floating Fireflies
    val fireflyCount = 7
    for (i in 0 until fireflyCount) {
        val fx = ((w * (0.1f + (i * 0.12f))) + sin(phase + i * 1.2f) * 16f) % w
        val fy = ((h * (0.25f + ((i % 4) * 0.12f))) + cos(phase + i * 0.9f) * 12f)
        val fireflyGlow = (sin(phase * 2f + i) + 1f) / 2f

        drawCircle(
            color = Color(0xFFFEF08A).copy(alpha = 0.85f * fireflyGlow),
            radius = 2.5f,
            center = Offset(fx, fy)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFDE047).copy(alpha = 0.4f * fireflyGlow), Color.Transparent),
                center = Offset(fx, fy),
                radius = 12f
            ),
            radius = 12f,
            center = Offset(fx, fy)
        )
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
    val shoreY = h * 0.62f

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
            radius = 18f,
            center = Offset(w * 0.75f, horizonY * 0.55f)
        )
    } else {
        drawCircle(
            color = Color(0xFFFBBF24),
            radius = 24f,
            center = Offset(w * 0.25f, horizonY * 0.50f)
        )
    }

    // Ocean Water
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
    val waveOffset1 = sin(phase) * 6f
    val wavePath1 = Path().apply {
        moveTo(0f, shoreY - 10f + waveOffset1)
        quadraticBezierTo(w * 0.25f, shoreY - 18f - waveOffset1, w * 0.5f, shoreY - 10f + waveOffset1)
        quadraticBezierTo(w * 0.75f, shoreY - 2f - waveOffset1, w, shoreY - 10f + waveOffset1)
        lineTo(w, shoreY)
        lineTo(0f, shoreY)
        close()
    }
    drawPath(path = wavePath1, color = Color(0xFF7DD3FC).copy(alpha = 0.5f))

    // Golden Sand Beach
    val sandBrush = if (isSleeping) {
        Brush.verticalGradient(listOf(Color(0xFF44403C), Color(0xFF292524), Color(0xFF1C1917)), startY = shoreY, endY = h)
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFDE68A), Color(0xFFFCD34D), Color(0xFFF59E0B)), startY = shoreY, endY = h)
    }
    drawRect(
        brush = sandBrush,
        topLeft = Offset(0f, shoreY),
        size = Size(w, h - shoreY)
    )

    // Beach Umbrella (Right Side)
    val umbX = w * 0.82f
    val umbY = shoreY + (h * 0.04f)
    val umbW = (w * 0.18f).coerceIn(55f, 85f)
    val umbH = umbW * 0.65f

    drawLine(
        color = if (isSleeping) Color(0xFF94A3B8) else Color(0xFFCBD5E1),
        start = Offset(umbX, umbY),
        end = Offset(umbX - 6f, umbY + (h * 0.18f)),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
    drawArc(
        color = Color(0xFFEF4444),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(umbX - umbW / 2, umbY - umbH),
        size = Size(umbW, umbH * 2)
    )
    drawArc(
        color = Color.White,
        startAngle = 220f,
        sweepAngle = 100f,
        useCenter = true,
        topLeft = Offset(umbX - umbW * 0.35f, umbY - umbH),
        size = Size(umbW * 0.7f, umbH * 2)
    )

    // Palm Tree (Left Side)
    val palmX = w * 0.07f
    val palmY = shoreY + (h * 0.14f)
    val palmTop = Offset(palmX + (w * 0.07f), shoreY - (h * 0.18f))

    val trunkPath = Path().apply {
        moveTo(palmX, palmY)
        quadraticBezierTo(palmX + (w * 0.015f), palmY - (h * 0.14f), palmTop.x, palmTop.y)
    }
    drawPath(
        path = trunkPath,
        color = if (isSleeping) Color(0xFF292524) else Color(0xFF78350F),
        style = Stroke(width = 12f, cap = StrokeCap.Round)
    )

    val palmSway = sin(phase) * 4f
    val frondAngles = listOf(-60f, -20f, 20f, 60f, 100f, 140f)
    for (angle in frondAngles) {
        rotate(degrees = angle + palmSway, pivot = palmTop) {
            val frondPath = Path().apply {
                moveTo(palmTop.x, palmTop.y)
                quadraticBezierTo(palmTop.x + 30f, palmTop.y - 14f, palmTop.x + 55f, palmTop.y + 10f)
                quadraticBezierTo(palmTop.x + 25f, palmTop.y + 3f, palmTop.x, palmTop.y)
                close()
            }
            drawPath(
                path = frondPath,
                color = if (isSleeping) Color(0xFF065F46) else Color(0xFF16A34A)
            )
        }
    }
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

    // Star Field
    val starCoords = listOf(
        Pair(0.08f, 0.12f), Pair(0.18f, 0.06f), Pair(0.32f, 0.18f),
        Pair(0.48f, 0.08f), Pair(0.62f, 0.14f), Pair(0.78f, 0.05f),
        Pair(0.92f, 0.18f), Pair(0.12f, 0.42f), Pair(0.88f, 0.38f),
        Pair(0.15f, 0.72f), Pair(0.85f, 0.78f)
    )

    for ((idx, coord) in starCoords.withIndex()) {
        val sx = w * coord.first
        val sy = h * coord.second
        val starPulse = (sin(phase * 2.5f + idx * 0.9f) + 1f) / 2f
        val starRadius = if (idx % 3 == 0) 2.5f + (starPulse * 1.2f) else 1.5f + (starPulse * 0.6f)

        drawCircle(
            color = Color.White.copy(alpha = 0.6f + (starPulse * 0.4f)),
            radius = starRadius,
            center = Offset(sx, sy)
        )
    }

    // Giant Ringed Planet (Top Left)
    val planet1X = w * 0.16f
    val planet1Y = h * 0.16f
    val planet1R = (w * 0.07f).coerceIn(22f, 36f)

    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFFB45309)),
            center = Offset(planet1X - planet1R * 0.3f, planet1Y - planet1R * 0.3f),
            radius = planet1R * 1.3f
        ),
        radius = planet1R,
        center = Offset(planet1X, planet1Y)
    )
    rotate(degrees = -25f, pivot = Offset(planet1X, planet1Y)) {
        drawOval(
            color = Color(0xFFFDE68A).copy(alpha = 0.8f),
            topLeft = Offset(planet1X - planet1R * 1.8f, planet1Y - planet1R * 0.35f),
            size = Size(planet1R * 3.6f, planet1R * 0.7f),
            style = Stroke(width = 4f)
        )
    }

    // Lunar/Asteroid Surface Platform (Bottom Stage for the pet)
    val floorY = h * 0.74f
    val surfacePath = Path().apply {
        moveTo(0f, floorY + 8f)
        quadraticBezierTo(w * 0.25f, floorY - 10f, w * 0.5f, floorY)
        quadraticBezierTo(w * 0.75f, floorY + 10f, w, floorY - 4f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    val surfaceBrush = Brush.verticalGradient(
        listOf(Color(0xFF475569), Color(0xFF334155), Color(0xFF1E293B)),
        startY = floorY - 10f,
        endY = h
    )
    drawPath(path = surfacePath, brush = surfaceBrush)
}
