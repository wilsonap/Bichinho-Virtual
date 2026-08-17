package com.example.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

// =================================================================================================
// 1. SALA DE ESTAR (LIVING ROOM) - AMBIENTE PRINCIPAL
// Sofá, Televisão, Mesa de centro, Estante, Quadros, Plantas, Tapete, Janela
// =================================================================================================
fun DrawScope.drawLivingRoomScene(
    w: Float,
    h: Float,
    phase: Float,
    pulse: Float,
    isSleeping: Boolean
) {
    val floorY = h * 0.70f

    // Parede
    val wallGradient = if (isSleeping) {
        listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
    } else {
        listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A), Color(0xFFF3E8FF))
    }
    drawRect(
        brush = Brush.verticalGradient(wallGradient, startY = 0f, endY = floorY),
        topLeft = Offset(0f, 0f),
        size = Size(w, floorY)
    )

    // Rodapé
    drawRect(
        color = if (isSleeping) Color(0xFF1E293B) else Color(0xFFE2E8F0),
        topLeft = Offset(0f, floorY - 12f),
        size = Size(w, 12f)
    )

    // Piso de Madeira Aconchegante
    val floorTop = if (isSleeping) Color(0xFF292524) else Color(0xFFC49A6C)
    val floorBot = if (isSleeping) Color(0xFF0C0A09) else Color(0xFF854D0E)
    drawRect(
        brush = Brush.verticalGradient(listOf(floorTop, floorBot), startY = floorY, endY = h),
        topLeft = Offset(0f, floorY),
        size = Size(w, h - floorY)
    )
    for (i in 0..7) {
        val px = (w / 7f) * i
        drawLine(
            color = if (isSleeping) Color(0xFF0C0A09).copy(alpha = 0.4f) else Color(0xFF713F12).copy(alpha = 0.25f),
            start = Offset(px, floorY),
            end = Offset(px, h),
            strokeWidth = 1.5f
        )
    }

    // 1. Janela da Sala (Centro-Esquerda)
    val winW = (w * 0.24f).coerceIn(80f, 130f)
    val winH = winW * 1.1f
    val winX = w * 0.08f
    val winY = (h * 0.08f).coerceIn(15f, 40f)

    drawRoundRect(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFFFFFFF),
        topLeft = Offset(winX - 4f, winY - 4f),
        size = Size(winW + 8f, winH + 8f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    val skyBrush = if (isSleeping) {
        Brush.verticalGradient(listOf(Color(0xFF020617), Color(0xFF1E293B)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF60A5FA), Color(0xFF93C5FD), Color(0xFFFEF3C7)))
    }
    drawRoundRect(
        brush = skyBrush,
        topLeft = Offset(winX, winY),
        size = Size(winW, winH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Divisórias da janela
    drawLine(color = Color.White.copy(alpha = 0.7f), start = Offset(winX + winW / 2f, winY), end = Offset(winX + winW / 2f, winY + winH), strokeWidth = 2f)
    drawLine(color = Color.White.copy(alpha = 0.7f), start = Offset(winX, winY + winH / 2f), end = Offset(winX + winW, winY + winH / 2f), strokeWidth = 2f)

    // 2. Quadros Decorativos (Wall art)
    val qx = w * 0.40f
    val qy = winY + 8f
    drawRoundRect(
        color = Color(0xFF78350F),
        topLeft = Offset(qx, qy),
        size = Size(36f, 44f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawRoundRect(
        color = Color(0xFFFEF08A),
        topLeft = Offset(qx + 3f, qy + 3f),
        size = Size(30f, 38f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawCircle(Color(0xFFF59E0B), radius = 6f, center = Offset(qx + 18f, qy + 20f))

    // 3. Televisão & Painel de TV (Lado Direito)
    val tvW = (w * 0.28f).coerceIn(90f, 150f)
    val tvH = tvW * 0.62f
    val tvX = w * 0.68f
    val tvY = floorY - tvH - 45f

    // Painel / Hack da TV
    drawRoundRect(
        color = if (isSleeping) Color(0xFF1E293B) else Color(0xFF451A03),
        topLeft = Offset(tvX - 8f, floorY - 40f),
        size = Size(tvW + 16f, 38f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFF78350F),
        topLeft = Offset(tvX - 4f, floorY - 36f),
        size = Size(tvW + 8f, 15f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Tela de TV
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(tvX - 2f, tvY - 2f),
        size = Size(tvW + 4f, tvH + 4f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    val tvScreenColor = if (isSleeping) Color(0xFF020617) else Color(0xFF1E1B4B)
    drawRoundRect(
        color = tvScreenColor,
        topLeft = Offset(tvX, tvY),
        size = Size(tvW, tvH),
        cornerRadius = CornerRadius(3f, 3f)
    )
    if (!isSleeping) {
        // Brilho suave animado na tela
        val screenGlow = (sin(phase * 3f) + 1f) / 2f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF38BDF8).copy(alpha = 0.35f * screenGlow), Color.Transparent),
                center = Offset(tvX + tvW / 2f, tvY + tvH / 2f),
                radius = tvW * 0.6f
            ),
            radius = tvW * 0.6f,
            center = Offset(tvX + tvW / 2f, tvY + tvH / 2f)
        )
    }

    // Suporte da TV
    drawRect(color = Color(0xFF334155), topLeft = Offset(tvX + tvW / 2f - 4f, tvY + tvH), size = Size(8f, 12f))
    drawRoundRect(color = Color(0xFF475569), topLeft = Offset(tvX + tvW / 2f - 16f, tvY + tvH + 10f), size = Size(32f, 4f), cornerRadius = CornerRadius(2f, 2f))

    // 4. Sofá Confortável (Lado Esquerdo da Sala)
    val sofaW = (w * 0.32f).coerceIn(110f, 170f)
    val sofaH = (h * 0.22f).coerceIn(60f, 95f)
    val sofaX = w * 0.05f
    val sofaY = floorY - sofaH * 0.85f

    val sofaColor = if (isSleeping) Color(0xFF1E3A8A) else Color(0xFF0284C7)
    val sofaDark = if (isSleeping) Color(0xFF172554) else Color(0xFF0369A1)
    val sofaCushion = if (isSleeping) Color(0xFF3B82F6) else Color(0xFF38BDF8)

    // Encosto do Sofá
    drawRoundRect(
        color = sofaDark,
        topLeft = Offset(sofaX, sofaY),
        size = Size(sofaW, sofaH * 0.65f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    // Braço esquerdo
    drawRoundRect(
        color = sofaColor,
        topLeft = Offset(sofaX - 6f, sofaY + sofaH * 0.2f),
        size = Size(sofaW * 0.22f, sofaH * 0.65f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Braço direito
    drawRoundRect(
        color = sofaColor,
        topLeft = Offset(sofaX + sofaW - sofaW * 0.18f, sofaY + sofaH * 0.2f),
        size = Size(sofaW * 0.22f, sofaH * 0.65f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Assento / Almofadas do Sofá
    drawRoundRect(
        color = sofaCushion,
        topLeft = Offset(sofaX + sofaW * 0.12f, sofaY + sofaH * 0.35f),
        size = Size(sofaW * 0.76f, sofaH * 0.45f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Almofada decorativa amarela
    drawRoundRect(
        color = Color(0xFFFBBF24),
        topLeft = Offset(sofaX + sofaW * 0.16f, sofaY + sofaH * 0.28f),
        size = Size(20f, 20f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // 5. Planta em Vaso (Ao lado do sofá)
    val potX = sofaX + sofaW + 12f
    val potY = floorY - 24f
    drawRoundRect(color = Color(0xFFEA580C), topLeft = Offset(potX, potY), size = Size(16f, 24f), cornerRadius = CornerRadius(3f, 3f))
    val plantGreen = if (isSleeping) Color(0xFF065F46) else Color(0xFF16A34A)
    drawCircle(plantGreen, radius = 9f, center = Offset(potX + 8f, potY - 8f))
    drawCircle(plantGreen, radius = 7f, center = Offset(potX + 2f, potY - 4f))
    drawCircle(plantGreen, radius = 8f, center = Offset(potX + 14f, potY - 4f))

    // 6. Tapete Central da Sala (Onde o bichinho fica)
    val rugW = (w * 0.50f).coerceIn(160f, 300f)
    val rugH = (h * 0.16f).coerceIn(40f, 75f)
    val rugCenter = Offset(w * 0.50f, floorY + rugH * 0.30f)

    drawOval(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFFDE68A),
        topLeft = Offset(rugCenter.x - rugW / 2f, rugCenter.y - rugH / 2f),
        size = Size(rugW, rugH)
    )
    drawOval(
        color = if (isSleeping) Color(0xFF1E293B) else Color(0xFFF59E0B),
        topLeft = Offset(rugCenter.x - rugW * 0.42f, rugCenter.y - rugH * 0.42f),
        size = Size(rugW * 0.84f, rugH * 0.84f)
    )
    drawOval(
        color = if (isSleeping) Color(0xFF0F172A) else Color(0xFFFEF3C7),
        topLeft = Offset(rugCenter.x - rugW * 0.32f, rugCenter.y - rugH * 0.32f),
        size = Size(rugW * 0.64f, rugH * 0.64f)
    )

    // 7. Mesa de Centro com Vaso (Coffee Table no canto frontal)
    val tableW = 48f
    val tableH = 18f
    val tableX = w * 0.70f
    val tableY = floorY + 12f
    drawRoundRect(color = Color(0xFF78350F), topLeft = Offset(tableX, tableY), size = Size(tableW, 6f), cornerRadius = CornerRadius(2f, 2f))
    drawLine(color = Color(0xFF451A03), start = Offset(tableX + 4f, tableY + 6f), end = Offset(tableX + 4f, tableY + tableH), strokeWidth = 2.5f)
    drawLine(color = Color(0xFF451A03), start = Offset(tableX + tableW - 4f, tableY + 6f), end = Offset(tableX + tableW - 4f, tableY + tableH), strokeWidth = 2.5f)
}

// =================================================================================================
// 2. COZINHA (KITCHEN) - AMBIENTE DE ALIMENTAÇÃO
// Geladeira, Fogão, Armários, Bancada, Mesa, Cadeiras, Pratos e Utensílios
// =================================================================================================
fun DrawScope.drawKitchenScene(
    w: Float,
    h: Float,
    phase: Float,
    pulse: Float,
    isSleeping: Boolean
) {
    val floorY = h * 0.70f

    // Parede de Azulejos de Cozinha
    val wallGradient = if (isSleeping) {
        listOf(Color(0xFF0F172A), Color(0xFF1E293B))
    } else {
        listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD), Color(0xFFF0FDF4))
    }
    drawRect(
        brush = Brush.verticalGradient(wallGradient, startY = 0f, endY = floorY),
        topLeft = Offset(0f, 0f),
        size = Size(w, floorY)
    )
    // Grade de azulejos brancos / backsplash
    val tileW = 20f
    val tileH = 12f
    for (ty in 0 until (floorY / tileH).toInt()) {
        for (tx in 0 until (w / tileW).toInt()) {
            drawRect(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = Offset(tx * tileW + (if (ty % 2 == 0) 0f else tileW / 2f), ty * tileH),
                size = Size(tileW - 2f, tileH - 2f)
            )
        }
    }

    // Piso de Cozinha (Cerâmica Xadrez / Modern Tiles)
    val floorColor1 = if (isSleeping) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val floorColor2 = if (isSleeping) Color(0xFF0F172A) else Color(0xFFCBD5E1)
    drawRect(color = floorColor1, topLeft = Offset(0f, floorY), size = Size(w, h - floorY))
    for (i in 0..10) {
        val px = (w / 10f) * i
        drawLine(color = floorColor2, start = Offset(px, floorY), end = Offset(px, h), strokeWidth = 2f)
    }

    // 1. Geladeira Moderna (Lado Esquerdo)
    val fridgeW = (w * 0.18f).coerceIn(60f, 95f)
    val fridgeH = (h * 0.48f).coerceIn(120f, 190f)
    val fridgeX = w * 0.04f
    val fridgeY = floorY - fridgeH

    drawRoundRect(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFE2E8F0),
        topLeft = Offset(fridgeX, fridgeY),
        size = Size(fridgeW, fridgeH),
        cornerRadius = CornerRadius(6f, 6f)
    )
    // Divisão do freezer / refrigerador
    drawLine(
        color = if (isSleeping) Color(0xFF1E293B) else Color(0xFF94A3B8),
        start = Offset(fridgeX + 2f, fridgeY + fridgeH * 0.35f),
        end = Offset(fridgeX + fridgeW - 2f, fridgeY + fridgeH * 0.35f),
        strokeWidth = 2f
    )
    // Puxadores
    drawRoundRect(color = Color(0xFF64748B), topLeft = Offset(fridgeX + fridgeW - 8f, fridgeY + fridgeH * 0.12f), size = Size(3f, 22f), cornerRadius = CornerRadius(1.5f, 1.5f))
    drawRoundRect(color = Color(0xFF64748B), topLeft = Offset(fridgeX + fridgeW - 8f, fridgeY + fridgeH * 0.45f), size = Size(3f, 28f), cornerRadius = CornerRadius(1.5f, 1.5f))
    // Ímãs de geladeira coloridos
    drawCircle(Color(0xFFEF4444), radius = 3f, center = Offset(fridgeX + fridgeW * 0.3f, fridgeY + fridgeH * 0.15f))
    drawCircle(Color(0xFFF59E0B), radius = 3f, center = Offset(fridgeX + fridgeW * 0.6f, fridgeY + fridgeH * 0.20f))

    // 2. Bancada & Armários Superiores (Ao lado da geladeira)
    val counterX = fridgeX + fridgeW + 6f
    val counterW = (w * 0.40f).coerceIn(120f, 200f)
    val counterH = (h * 0.24f).coerceIn(60f, 95f)
    val counterY = floorY - counterH

    // Armário Superior
    drawRoundRect(
        color = if (isSleeping) Color(0xFF1E293B) else Color(0xFF0284C7),
        topLeft = Offset(counterX, fridgeY + 10f),
        size = Size(counterW, 36f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawLine(color = Color.White.copy(alpha = 0.5f), start = Offset(counterX + counterW / 2f, fridgeY + 10f), end = Offset(counterX + counterW / 2f, fridgeY + 46f), strokeWidth = 1.5f)

    // Bancada Inferior
    drawRoundRect(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFF0369A1),
        topLeft = Offset(counterX, counterY),
        size = Size(counterW, counterH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Tampo de Mármore
    drawRoundRect(
        color = if (isSleeping) Color(0xFF475569) else Color(0xFFF8FAFC),
        topLeft = Offset(counterX - 2f, counterY - 4f),
        size = Size(counterW + 4f, 6f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // 3. Fogão com Forno e Chaleira
    val stoveX = counterX + 10f
    val stoveW = 45f
    drawRoundRect(
        color = if (isSleeping) Color(0xFF1E293B) else Color(0xFF334155),
        topLeft = Offset(stoveX, counterY + 4f),
        size = Size(stoveW, counterH - 6f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    // Vidro do forno
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(stoveX + 6f, counterY + 28f),
        size = Size(stoveW - 12f, 26f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    // Chaleira com vapor na bancada
    val kettleX = stoveX + 16f
    val kettleY = counterY - 14f
    drawRoundRect(color = Color(0xFFEF4444), topLeft = Offset(kettleX, kettleY), size = Size(14f, 10f), cornerRadius = CornerRadius(4f, 4f))
    drawArc(color = Color(0xFFEF4444), startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(kettleX - 2f, kettleY - 6f), size = Size(18f, 12f), style = Stroke(width = 2f))
    if (!isSleeping) {
        val steamSway = sin(phase * 4f) * 2f
        drawLine(color = Color.White.copy(alpha = 0.6f), start = Offset(kettleX + 7f, kettleY - 3f), end = Offset(kettleX + 7f + steamSway, kettleY - 12f), strokeWidth = 1.5f)
    }

    // 4. Mesa de Refeições & Pratinho do Bichinho (Lado Direito)
    val tableX = w * 0.70f
    val tableW = (w * 0.26f).coerceIn(80f, 130f)
    val tableH = 45f
    val tableY = floorY - tableH

    // Cadeiras
    drawRoundRect(color = Color(0xFFB45309), topLeft = Offset(tableX - 16f, tableY - 15f), size = Size(14f, 40f), cornerRadius = CornerRadius(3f, 3f))
    drawRoundRect(color = Color(0xFFB45309), topLeft = Offset(tableX + tableW + 2f, tableY - 15f), size = Size(14f, 40f), cornerRadius = CornerRadius(3f, 3f))

    // Tampo da Mesa com Toalha
    drawRoundRect(
        color = Color(0xFF78350F),
        topLeft = Offset(tableX, tableY),
        size = Size(tableW, 8f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    // Toalha xadrez vermelha
    drawRoundRect(
        color = Color(0xFFEF4444),
        topLeft = Offset(tableX + 8f, tableY - 1f),
        size = Size(tableW - 16f, 10f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    // Pés da Mesa
    drawLine(color = Color(0xFF451A03), start = Offset(tableX + 6f, tableY + 8f), end = Offset(tableX + 6f, floorY), strokeWidth = 4f)
    drawLine(color = Color(0xFF451A03), start = Offset(tableX + tableW - 6f, tableY + 8f), end = Offset(tableX + tableW - 6f, floorY), strokeWidth = 4f)

    // Pratinho de Comida / Tigela no Centro do Chão
    val bowlX = w * 0.50f - 18f
    val bowlY = floorY + 4f
    drawOval(color = Color(0x30000000), topLeft = Offset(bowlX - 2f, bowlY + 8f), size = Size(40f, 8f))
    drawRoundRect(color = Color(0xFFF97316), topLeft = Offset(bowlX, bowlY), size = Size(36f, 12f), cornerRadius = CornerRadius(6f, 6f))
    drawOval(color = Color(0xFFFEF08A), topLeft = Offset(bowlX + 4f, bowlY + 1f), size = Size(28f, 5f))
}

// =================================================================================================
// 3. BANHEIRO (BATHROOM) - AMBIENTE DE HIGIENE E BANHO
// Banheira / Box, Chuveiro, Pia, Espelho, Toalhas, Armário, Tapete
// =================================================================================================
fun DrawScope.drawBathroomScene(
    w: Float,
    h: Float,
    phase: Float,
    pulse: Float,
    isSleeping: Boolean
) {
    val floorY = h * 0.70f

    // Parede com Azulejos Azul Turquesa
    val wallGradient = if (isSleeping) {
        listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0E7490))
    } else {
        listOf(Color(0xFFE0F7FA), Color(0xFFB2EBF2), Color(0xFFE0F2FE))
    }
    drawRect(
        brush = Brush.verticalGradient(wallGradient, startY = 0f, endY = floorY),
        topLeft = Offset(0f, 0f),
        size = Size(w, floorY)
    )

    // Piso impermeável de banheiro
    val floorColor = if (isSleeping) Color(0xFF1E293B) else Color(0xFFE0E7FF)
    drawRect(color = floorColor, topLeft = Offset(0f, floorY), size = Size(w, h - floorY))
    for (i in 0..8) {
        val px = (w / 8f) * i
        drawLine(color = Color(0xFF93C5FD).copy(alpha = 0.4f), start = Offset(px, floorY), end = Offset(px, h), strokeWidth = 1.5f)
    }

    // 1. Pia com Espelho e Torneira (Lado Direito)
    val sinkW = (w * 0.22f).coerceIn(75f, 115f)
    val sinkX = w * 0.72f
    val sinkH = 45f
    val sinkY = floorY - sinkH

    // Espelho Oval Acima da Pia
    val mirrorW = sinkW * 0.75f
    val mirrorH = mirrorW * 1.35f
    val mirrorX = sinkX + (sinkW - mirrorW) / 2f
    val mirrorY = sinkY - mirrorH - 12f

    drawOval(color = Color(0xFFCBD5E1), topLeft = Offset(mirrorX - 3f, mirrorY - 3f), size = Size(mirrorW + 6f, mirrorH + 6f))
    drawOval(
        brush = Brush.linearGradient(listOf(Color(0xFFBAE6FD), Color(0xFFE0F2FE), Color(0xFFFFFFFF))),
        topLeft = Offset(mirrorX, mirrorY),
        size = Size(mirrorW, mirrorH)
    )
    // Reflexo diagonal no espelho
    drawLine(color = Color.White.copy(alpha = 0.6f), start = Offset(mirrorX + 8f, mirrorY + 12f), end = Offset(mirrorX + mirrorW - 8f, mirrorY + mirrorH - 16f), strokeWidth = 3f)

    // Gabinete / Pia
    drawRoundRect(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFF0284C7),
        topLeft = Offset(sinkX, sinkY),
        size = Size(sinkW, sinkH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Cuba da pia
    drawRoundRect(
        color = Color(0xFFF8FAFC),
        topLeft = Offset(sinkX - 2f, sinkY - 6f),
        size = Size(sinkW + 4f, 10f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Torneira prateada com gotas de água
    drawRoundRect(color = Color(0xFF94A3B8), topLeft = Offset(sinkX + sinkW / 2f - 2f, sinkY - 18f), size = Size(4f, 14f), cornerRadius = CornerRadius(2f, 2f))
    drawRoundRect(color = Color(0xFF94A3B8), topLeft = Offset(sinkX + sinkW / 2f - 6f, sinkY - 20f), size = Size(10f, 4f), cornerRadius = CornerRadius(2f, 2f))

    // 2. Toalheiro com Toalhas Macias (Ao lado da pia)
    val towelX = sinkX - 24f
    val towelY = sinkY - 20f
    drawRoundRect(color = Color(0xFF94A3B8), topLeft = Offset(towelX, towelY), size = Size(18f, 3f), cornerRadius = CornerRadius(1f, 1f))
    drawRoundRect(color = Color(0xFFEC4899), topLeft = Offset(towelX + 2f, towelY + 3f), size = Size(14f, 28f), cornerRadius = CornerRadius(3f, 3f))

    // 3. Grande Banheira Luxuosa de Espuma (Lado Esquerdo / Centro)
    val tubW = (w * 0.46f).coerceIn(155f, 260f)
    val tubH = (h * 0.28f).coerceIn(75f, 120f)
    val tubX = w * 0.08f
    val tubY = floorY - tubH * 0.70f

    // Sombra da Banheira
    drawOval(color = Color(0x30000000), topLeft = Offset(tubX - 4f, floorY - 4f), size = Size(tubW + 8f, 14f))

    // Pés dourados clássicos da banheira (Clawfoot tub)
    drawCircle(Color(0xFFF59E0B), radius = 5f, center = Offset(tubX + 16f, floorY + 2f))
    drawCircle(Color(0xFFF59E0B), radius = 5f, center = Offset(tubX + tubW - 16f, floorY + 2f))

    // Corpo Externo da Banheira
    drawRoundRect(
        color = if (isSleeping) Color(0xFF334155) else Color(0xFFF8FAFC),
        topLeft = Offset(tubX, tubY),
        size = Size(tubW, tubH * 0.85f),
        cornerRadius = CornerRadius(24f, 24f)
    )
    // Borda superior esmaltada
    drawRoundRect(
        color = if (isSleeping) Color(0xFF475569) else Color(0xFFE2E8F0),
        topLeft = Offset(tubX - 4f, tubY - 4f),
        size = Size(tubW + 8f, 10f),
        cornerRadius = CornerRadius(5f, 5f)
    )

    // Água Morna Azulada dentro da banheira
    val waterPath = Path().apply {
        moveTo(tubX + 8f, tubY + 8f)
        lineTo(tubX + tubW - 8f, tubY + 8f)
        quadraticBezierTo(tubX + tubW - 8f, tubY + tubH * 0.70f, tubX + tubW / 2f, tubY + tubH * 0.70f)
        quadraticBezierTo(tubX + 8f, tubY + tubH * 0.70f, tubX + 8f, tubY + 8f)
        close()
    }
    drawPath(path = waterPath, color = Color(0xFF38BDF8).copy(alpha = 0.75f))

    // Espuma fofinha e bolhas na banheira
    val bubblePhase = sin(phase * 2f) * 3f
    drawCircle(Color.White, radius = 9f, center = Offset(tubX + 26f, tubY + 4f))
    drawCircle(Color.White, radius = 12f, center = Offset(tubX + 44f, tubY + 2f))
    drawCircle(Color.White, radius = 10f, center = Offset(tubX + 68f, tubY + 5f))
    drawCircle(Color.White, radius = 11f, center = Offset(tubX + 90f, tubY + 3f))
    drawCircle(Color.White, radius = 8f, center = Offset(tubX + tubW - 28f, tubY + 6f))

    // Bolhinhas de sabão animadas flutuando no ar
    for (b in 0..4) {
        val bx = tubX + 20f + (b * 24f) + bubblePhase
        val by = tubY - 12f - (b * 8f) + sin(phase + b) * 5f
        drawCircle(Color(0xFFBAE6FD).copy(alpha = 0.75f), radius = 4f + (b % 3), center = Offset(bx, by), style = Stroke(width = 1.5f))
        drawCircle(Color.White.copy(alpha = 0.85f), radius = 1.2f, center = Offset(bx - 1f, by - 1f))
    }

    // 4. Chuveiro de Inox acima da Banheira
    val showerX = tubX + 16f
    val showerY = tubY - 40f
    drawLine(color = Color(0xFF94A3B8), start = Offset(showerX, floorY), end = Offset(showerX, showerY), strokeWidth = 3f)
    drawLine(color = Color(0xFF94A3B8), start = Offset(showerX, showerY), end = Offset(showerX + 24f, showerY), strokeWidth = 3f)
    drawArc(color = Color(0xFF64748B), startAngle = 0f, sweepAngle = 180f, useCenter = true, topLeft = Offset(showerX + 16f, showerY), size = Size(16f, 10f))

    // 5. Tapete de Banheiro Felpudo Antiderrapante
    val matW = (w * 0.36f).coerceIn(120f, 200f)
    val matH = 24f
    val matX = w * 0.48f
    val matY = floorY + 10f
    drawRoundRect(
        color = Color(0xFF06B6D4),
        topLeft = Offset(matX, matY),
        size = Size(matW, matH),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = Color(0xFF67E8F9),
        topLeft = Offset(matX + 4f, matY + 4f),
        size = Size(matW - 8f, matH - 8f),
        cornerRadius = CornerRadius(6f, 6f)
    )
}

// =================================================================================================
// 4. QUINTAL (BACKYARD) - AMBIENTE DE BRINCADEIRAS AO AR LIVRE
// Gramado, Árvores, Flores, Balanço, Jardim, Cerca, Brinquedos externos
// =================================================================================================
fun DrawScope.drawBackyardScene(
    w: Float,
    h: Float,
    phase: Float,
    pulse: Float,
    isSleeping: Boolean
) {
    val horizonY = h * 0.58f

    // Céu Aberto e Ensolarado / Estrelado
    val skyBrush = if (isSleeping) {
        Brush.verticalGradient(listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E3A8A)), startY = 0f, endY = horizonY)
    } else {
        Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF7DD3FC), Color(0xFFBAE6FD), Color(0xFFFEF3C7)), startY = 0f, endY = horizonY)
    }
    drawRect(brush = skyBrush, topLeft = Offset(0f, 0f), size = Size(w, horizonY))

    // Sol / Lua
    if (isSleeping) {
        drawCircle(Color(0xFFFEF08A), radius = 16f, center = Offset(w * 0.80f, horizonY * 0.35f))
    } else {
        val sunPulse = (sin(phase * 2f) + 1f) / 2f
        drawCircle(Color(0xFFFDE047).copy(alpha = 0.3f * sunPulse), radius = 28f, center = Offset(w * 0.82f, horizonY * 0.30f))
        drawCircle(Color(0xFFFBBF24), radius = 20f, center = Offset(w * 0.82f, horizonY * 0.30f))
    }

    // Nuvens no céu
    val cloudShift = (phase * 12f) % (w * 0.6f)
    val cx = w * 0.15f + cloudShift
    val cy = horizonY * 0.30f
    drawCircle(Color.White.copy(alpha = 0.85f), radius = 14f, center = Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.85f), radius = 18f, center = Offset(cx + 12f, cy - 4f))
    drawCircle(Color.White.copy(alpha = 0.85f), radius = 14f, center = Offset(cx + 26f, cy))

    // Cerquinha de Madeira Branca ao Fundo (Picket Fence)
    val picketCount = (w / 18f).toInt() + 1
    val fenceY = horizonY - 22f
    for (p in 0 until picketCount) {
        val px = p * 18f
        val picketPath = Path().apply {
            moveTo(px, horizonY)
            lineTo(px, fenceY + 6f)
            lineTo(px + 6f, fenceY)
            lineTo(px + 12f, fenceY + 6f)
            lineTo(px + 12f, horizonY)
            close()
        }
        drawPath(path = picketPath, color = if (isSleeping) Color(0xFF334155) else Color(0xFFF8FAFC))
    }
    // Traves horizontais da cerca
    drawRect(color = if (isSleeping) Color(0xFF1E293B) else Color(0xFFE2E8F0), topLeft = Offset(0f, fenceY + 8f), size = Size(w, 4f))
    drawRect(color = if (isSleeping) Color(0xFF1E293B) else Color(0xFFE2E8F0), topLeft = Offset(0f, fenceY + 16f), size = Size(w, 4f))

    // Gramado Verdejante (Lush Lawn)
    val grassBrush = if (isSleeping) {
        Brush.verticalGradient(listOf(Color(0xFF064E3B), Color(0xFF022C22)), startY = horizonY, endY = h)
    } else {
        Brush.verticalGradient(listOf(Color(0xFF22C55E), Color(0xFF16A34A), Color(0xFF15803D)), startY = horizonY, endY = h)
    }
    drawRect(brush = grassBrush, topLeft = Offset(0f, horizonY), size = Size(w, h - horizonY))

    // 1. Árvore com Balanço (Lado Esquerdo)
    val trunkX = w * 0.12f
    val trunkTop = Offset(trunkX + 24f, horizonY - 95f)
    drawRoundRect(
        color = if (isSleeping) Color(0xFF292524) else Color(0xFF78350F),
        topLeft = Offset(trunkX, horizonY - 95f),
        size = Size(24f, 105f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    // Galho horizontal para o balanço
    drawRoundRect(
        color = if (isSleeping) Color(0xFF292524) else Color(0xFF78350F),
        topLeft = Offset(trunkX + 16f, horizonY - 80f),
        size = Size(65f, 12f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Copa da Árvore
    val sway = sin(phase) * 4f
    val leafColor = if (isSleeping) Color(0xFF065F46) else Color(0xFF15803D)
    val leafColorLight = if (isSleeping) Color(0xFF047857) else Color(0xFF4ADE80)
    drawCircle(leafColor, radius = 48f, center = Offset(trunkX + 10f + sway, horizonY - 110f))
    drawCircle(leafColorLight, radius = 38f, center = Offset(trunkX + 45f + sway, horizonY - 100f))
    drawCircle(leafColor, radius = 42f, center = Offset(trunkX - 15f + sway, horizonY - 95f))

    // Balanço de Corda e Madeira (Tree Swing)
    val ropeX1 = trunkX + 50f
    val ropeX2 = trunkX + 70f
    val swingY = horizonY - 20f
    drawLine(color = Color(0xFFD97706), start = Offset(ropeX1, horizonY - 72f), end = Offset(ropeX1, swingY), strokeWidth = 2f)
    drawLine(color = Color(0xFFD97706), start = Offset(ropeX2, horizonY - 72f), end = Offset(ropeX2, swingY), strokeWidth = 2f)
    // Assento de madeira do balanço
    drawRoundRect(color = Color(0xFFB45309), topLeft = Offset(ropeX1 - 4f, swingY), size = Size(28f, 6f), cornerRadius = CornerRadius(2f, 2f))

    // 2. Canteiro de Flores Coloridas (Jardim ao fundo)
    val flowerColors = listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF38BDF8))
    for (f in 0..12) {
        val fx = (w * 0.45f) + (f * 18f)
        val fy = horizonY + 12f + ((f * 13) % 16)
        if (fx < w - 10f) {
            drawLine(color = Color(0xFF166534), start = Offset(fx, fy + 8f), end = Offset(fx, fy), strokeWidth = 2f)
            drawCircle(flowerColors[f % flowerColors.size], radius = 4.5f, center = Offset(fx, fy))
            drawCircle(Color(0xFFFEF08A), radius = 1.8f, center = Offset(fx, fy))
        }
    }

    // 3. Brinquedos de Quintal (Bola de Futebol / Pipa)
    val ballX = w * 0.82f
    val ballY = horizonY + 36f
    drawCircle(Color(0x25000000), radius = 12f, center = Offset(ballX + 2f, ballY + 4f))
    drawCircle(Color.White, radius = 12f, center = Offset(ballX, ballY))
    drawCircle(Color(0xFF0F172A), radius = 4f, center = Offset(ballX, ballY))
    for (p in 0..4) {
        val pa = p * 1.25f
        drawCircle(Color(0xFF0F172A), radius = 2.5f, center = Offset(ballX + cos(pa) * 8f, ballY + sin(pa) * 8f))
    }
}

// =================================================================================================
// 5. GARAGEM (GARAGE) - AMBIENTE DE ATIVIDADES E MINIJOGOS
// Bicicleta, Skate, Ferramentas, Prateleiras, Caixas, Elementos Recreativos
// =================================================================================================
fun DrawScope.drawGarageScene(
    w: Float,
    h: Float,
    phase: Float,
    pulse: Float,
    isSleeping: Boolean
) {
    val floorY = h * 0.70f

    // Parede de Tijolinhos e Concreto Industrial
    val wallGradient = if (isSleeping) {
        listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
    } else {
        listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFF94A3B8))
    }
    drawRect(
        brush = Brush.verticalGradient(wallGradient, startY = 0f, endY = floorY),
        topLeft = Offset(0f, 0f),
        size = Size(w, floorY)
    )

    // Piso de Cimento Queimado / Epóxi
    val floorTop = if (isSleeping) Color(0xFF1E293B) else Color(0xFF64748B)
    val floorBot = if (isSleeping) Color(0xFF0F172A) else Color(0xFF475569)
    drawRect(
        brush = Brush.verticalGradient(listOf(floorTop, floorBot), startY = floorY, endY = h),
        topLeft = Offset(0f, floorY),
        size = Size(w, h - floorY)
    )
    // Linha amarela de oficina no chão
    drawLine(
        color = Color(0xFFFBBF24).copy(alpha = 0.5f),
        start = Offset(0f, floorY + 12f),
        end = Offset(w, floorY + 12f),
        strokeWidth = 3f
    )

    // 1. Painel de Ferramentas (Tool Pegboard no fundo)
    val pegW = (w * 0.28f).coerceIn(90f, 150f)
    val pegH = 50f
    val pegX = w * 0.36f
    val pegY = floorY - pegH - 40f

    drawRoundRect(
        color = Color(0xFF334155),
        topLeft = Offset(pegX, pegY),
        size = Size(pegW, pegH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Ferramentas penduradas: Martelo, Chave de fenda, Chave inglesa
    // Martelo
    drawLine(color = Color(0xFFB45309), start = Offset(pegX + 16f, pegY + 10f), end = Offset(pegX + 16f, pegY + 38f), strokeWidth = 3f)
    drawRect(color = Color(0xFF94A3B8), topLeft = Offset(pegX + 10f, pegY + 8f), size = Size(12f, 8f))
    // Chave inglesa
    drawLine(color = Color(0xFF94A3B8), start = Offset(pegX + 38f, pegY + 12f), end = Offset(pegX + 38f, pegY + 36f), strokeWidth = 3.5f)
    drawCircle(Color(0xFF94A3B8), radius = 5f, center = Offset(pegX + 38f, pegY + 12f), style = Stroke(width = 2f))
    // Chave de fenda amarela
    drawLine(color = Color(0xFFF59E0B), start = Offset(pegX + 60f, pegY + 10f), end = Offset(pegX + 60f, pegY + 24f), strokeWidth = 4f)
    drawLine(color = Color(0xFFCBD5E1), start = Offset(pegX + 60f, pegY + 24f), end = Offset(pegX + 60f, pegY + 38f), strokeWidth = 2f)

    // 2. Prateleira de Oficina com Caixas Organizadoras (Lado Direito)
    val shelfX = w * 0.72f
    val shelfW = (w * 0.24f).coerceIn(75f, 120f)
    val shelfH = (h * 0.45f).coerceIn(120f, 180f)
    val shelfY = floorY - shelfH

    // Postes de aço da estante
    drawRect(color = Color(0xFF1E293B), topLeft = Offset(shelfX, shelfY), size = Size(4f, shelfH))
    drawRect(color = Color(0xFF1E293B), topLeft = Offset(shelfX + shelfW - 4f, shelfY), size = Size(4f, shelfH))
    // 3 Prateleiras de metal
    for (s in 0..2) {
        val sy = shelfY + (shelfH / 3f) * (s + 1)
        drawRect(color = Color(0xFF475569), topLeft = Offset(shelfX, sy - 3f), size = Size(shelfW, 4f))
        // Caixas de armazenamento coloridas
        drawRoundRect(color = Color(0xFFEF4444), topLeft = Offset(shelfX + 6f, sy - 22f), size = Size(24f, 18f), cornerRadius = CornerRadius(2f, 2f))
        drawRoundRect(color = Color(0xFF3B82F6), topLeft = Offset(shelfX + 34f, sy - 20f), size = Size(26f, 16f), cornerRadius = CornerRadius(2f, 2f))
    }

    // 3. Bicicleta Esportiva (Lado Esquerdo encostada)
    val bikeX = w * 0.08f
    val bikeY = floorY - 38f
    val wheelR = 18f
    // Roda Traseira
    drawCircle(Color(0xFF0F172A), radius = wheelR, center = Offset(bikeX, bikeY), style = Stroke(width = 3f))
    drawCircle(Color(0xFF94A3B8), radius = 3f, center = Offset(bikeX, bikeY))
    // Roda Dianteira
    drawCircle(Color(0xFF0F172A), radius = wheelR, center = Offset(bikeX + 56f, bikeY), style = Stroke(width = 3f))
    drawCircle(Color(0xFF94A3B8), radius = 3f, center = Offset(bikeX + 56f, bikeY))
    // Quadro da Bicicleta (Vermelho vibrante)
    val frameRed = Color(0xFFEF4444)
    drawLine(color = frameRed, start = Offset(bikeX, bikeY), end = Offset(bikeX + 24f, bikeY - 24f), strokeWidth = 3f)
    drawLine(color = frameRed, start = Offset(bikeX + 24f, bikeY - 24f), end = Offset(bikeX + 48f, bikeY - 24f), strokeWidth = 3f)
    drawLine(color = frameRed, start = Offset(bikeX + 48f, bikeY - 24f), end = Offset(bikeX + 56f, bikeY), strokeWidth = 3f)
    drawLine(color = frameRed, start = Offset(bikeX, bikeY), end = Offset(bikeX + 32f, bikeY), strokeWidth = 3f)
    drawLine(color = frameRed, start = Offset(bikeX + 32f, bikeY), end = Offset(bikeX + 24f, bikeY - 24f), strokeWidth = 3f)
    // Guidão & Selim
    drawLine(color = Color(0xFF0F172A), start = Offset(bikeX + 46f, bikeY - 32f), end = Offset(bikeX + 52f, bikeY - 32f), strokeWidth = 3f)
    drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(bikeX + 20f, bikeY - 28f), size = Size(12f, 4f), cornerRadius = CornerRadius(2f, 2f))

    // 4. Skate Estiloso (No chão, próximo ao centro)
    val skateX = w * 0.42f
    val skateY = floorY + 16f
    val skateW = 44f
    drawRoundRect(
        color = Color(0xFFF59E0B),
        topLeft = Offset(skateX, skateY),
        size = Size(skateW, 6f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    // Rodinhas de skate azuis
    drawCircle(Color(0xFF06B6D4), radius = 3.5f, center = Offset(skateX + 8f, skateY + 8f))
    drawCircle(Color(0xFF06B6D4), radius = 3.5f, center = Offset(skateX + skateW - 8f, skateY + 8f))
}
