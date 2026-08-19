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
// 2. COZINHA (KITCHEN) — implementada em KitchenScenery.kt
// =================================================================================================

// =================================================================================================
// 3. BANHEIRO (BATHROOM) — implementado em BathroomScenery.kt
// =================================================================================================

// =================================================================================================
// 4. QUINTAL (BACKYARD) — implementado em BackyardScenery.kt
// =================================================================================================

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
