package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GameAudioManager
import com.example.audio.SoundEffect
import com.example.game.runner.RunnerGameEngine
import com.example.game.runner.RunnerGameEvent
import kotlinx.coroutines.isActive

private const val HUD_UPDATE_INTERVAL_SEC = 0.1f // ~10 Hz

/**
 * Cache de Brush do chão: só recria quando a altura do canvas muda.
 * Evita alocação por frame sem escrever estado Compose no draw.
 */
private class GroundBrushCache {
    private var lastGround = Float.NaN
    private var lastH = Float.NaN
    private var brush: Brush? = null

    fun get(groundLevel: Float, h: Float): Brush {
        if (brush == null || groundLevel != lastGround || h != lastH) {
            lastGround = groundLevel
            lastH = h
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF22C55E), Color(0xFF16A34A), Color(0xFF78350F)),
                startY = groundLevel,
                endY = h
            )
        }
        return brush!!
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunnerMinigameScreen(
    onBack: () -> Unit,
    onFinishGame: (score: Int, coins: Int) -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { GameAudioManager.getInstance(context) }
    val engine = remember { RunnerGameEngine() }
    val groundBrushCache = remember { GroundBrushCache() }

    val skyBrush = remember {
        Brush.verticalGradient(
            listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD), Color(0xFF7DD3FC))
        )
    }
    val cloudColorA = remember { Color.White.copy(alpha = 0.8f) }
    val cloudColorB = remember { Color.White.copy(alpha = 0.7f) }
    val petBodyColor = remember { Color(0xFFF59E0B) }
    val petEyeColor = remember { Color(0xFF1E293B) }
    val petCheekColor = remember { Color(0xFFF472B6) }
    val obstacleColor = remember { Color(0xFFDC2626) }
    val obstacleStripe = remember { Color(0xFFFEF2F2) }
    val coinOuter = remember { Color(0xFFF59E0B) }
    val coinInner = remember { Color(0xFFFDE047) }
    val groundLineColor = remember { Color(0xFF15803D) }

    // Só o Canvas observa este contador → redraw sem recompor HUD.
    var frameEpoch by remember { mutableIntStateOf(0) }

    // HUD throttled (~10 Hz)
    var hudDistance by remember { mutableIntStateOf(0) }
    var hudCoins by remember { mutableIntStateOf(0) }

    var isGameOverUi by remember { mutableStateOf(false) }
    var finishReported by remember { mutableStateOf(false) }

    fun restartGame() {
        engine.reset()
        hudDistance = 0
        hudCoins = 0
        isGameOverUi = false
        finishReported = false
        frameEpoch++
    }

    fun tryJump() {
        if (engine.jump()) {
            audioManager.playSfx(SoundEffect.TAP)
        }
    }

    // Um único loop por ciclo de vida da tela (sem relançar no restart).
    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L
        var hudAccum = 0f

        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameNanos == 0L) {
                    lastFrameNanos = frameTimeNanos
                    return@withFrameNanos
                }

                val rawDt = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
                lastFrameNanos = frameTimeNanos
                val dt = rawDt.coerceIn(0f, RunnerGameEngine.MAX_DT_SECONDS)

                if (!engine.isGameOver) {
                    val events = engine.update(dt)
                    for (event in events) {
                        when (event) {
                            RunnerGameEvent.COIN_COLLECTED ->
                                audioManager.playSfx(SoundEffect.COIN)
                            RunnerGameEvent.COLLISION ->
                                audioManager.playSfx(SoundEffect.PET_SAD)
                        }
                    }
                    frameEpoch++

                    hudAccum += dt
                    if (hudAccum >= HUD_UPDATE_INTERVAL_SEC) {
                        hudAccum = 0f
                        hudDistance = engine.distanceScoreInt()
                        hudCoins = engine.coinsCollected
                    }

                    if (engine.isGameOver && !finishReported) {
                        finishReported = true
                        isGameOverUi = true
                        hudDistance = engine.distanceScoreInt()
                        hudCoins = engine.coinsCollected
                        onFinishGame(engine.distanceScoreInt(), engine.totalCoinsEarned())
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Corrida do Bichinho", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { restartGame() },
                        modifier = Modifier.testTag("restart_runner_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Reiniciar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(skyBrush)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = "Distância: ${hudDistance}m",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0369A1),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF3C7)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🪙", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$hudCoins",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { tryJump() })
                    }
                    .testTag("runner_canvas_container")
            ) {
                Canvas(modifier = Modifier.fillMaxSize().testTag("runner_canvas")) {
                    // Dispara redraw explícito sem depender do score.
                    @Suppress("UNUSED_EXPRESSION")
                    frameEpoch

                    val w = size.width
                    val h = size.height
                    val groundLevel = h * 0.72f

                    drawCircle(cloudColorA, radius = 30.dp.toPx(), center = Offset(w * 0.2f, h * 0.18f))
                    drawCircle(cloudColorA, radius = 42.dp.toPx(), center = Offset(w * 0.28f, h * 0.16f))
                    drawCircle(cloudColorA, radius = 28.dp.toPx(), center = Offset(w * 0.36f, h * 0.19f))
                    drawCircle(cloudColorB, radius = 36.dp.toPx(), center = Offset(w * 0.75f, h * 0.24f))
                    drawCircle(cloudColorB, radius = 24.dp.toPx(), center = Offset(w * 0.82f, h * 0.25f))

                    drawRect(
                        brush = groundBrushCache.get(groundLevel, h),
                        topLeft = Offset(0f, groundLevel),
                        size = Size(w, h - groundLevel)
                    )
                    drawLine(
                        color = groundLineColor,
                        start = Offset(0f, groundLevel),
                        end = Offset(w, groundLevel),
                        strokeWidth = 6.dp.toPx()
                    )

                    val petBaseX = 140f
                    val currentPetY = groundLevel + engine.petY
                    val petBodyR = 28.dp.toPx()
                    val petEyeR = 4.dp.toPx()
                    val petShineR = 1.5.dp.toPx()
                    val petCheekR = 4.dp.toPx()

                    drawCircle(
                        color = petBodyColor,
                        radius = petBodyR,
                        center = Offset(petBaseX, currentPetY - 30.dp.toPx())
                    )
                    drawCircle(
                        petEyeColor,
                        radius = petEyeR,
                        center = Offset(petBaseX + 8.dp.toPx(), currentPetY - 34.dp.toPx())
                    )
                    drawCircle(
                        Color.White,
                        radius = petShineR,
                        center = Offset(petBaseX + 7.dp.toPx(), currentPetY - 35.dp.toPx())
                    )
                    drawCircle(
                        petCheekColor,
                        radius = petCheekR,
                        center = Offset(petBaseX + 16.dp.toPx(), currentPetY - 26.dp.toPx())
                    )

                    val obstacles = engine.obstacles
                    for (obsIndex in obstacles.indices) {
                        val obs = obstacles[obsIndex]
                        val obsX = (obs.x / RunnerGameEngine.LOGICAL_WIDTH) * w
                        val obsH = obs.height.dp.toPx()
                        val obsW = obs.width.dp.toPx()

                        drawRoundRect(
                            color = obstacleColor,
                            topLeft = Offset(obsX, groundLevel - obsH),
                            size = Size(obsW, obsH),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                        drawRoundRect(
                            color = obstacleStripe,
                            topLeft = Offset(obsX + 4.dp.toPx(), groundLevel - obsH + 4.dp.toPx()),
                            size = Size(obsW - 8.dp.toPx(), 8.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }

                    val coins = engine.collectibleCoins
                    for (coinIndex in coins.indices) {
                        val coin = coins[coinIndex]
                        if (coin.isCollected) continue
                        val coinX = (coin.x / RunnerGameEngine.LOGICAL_WIDTH) * w
                        val coinY = groundLevel + coin.y.dp.toPx()
                        drawCircle(color = coinOuter, radius = 12.dp.toPx(), center = Offset(coinX, coinY))
                        drawCircle(color = coinInner, radius = 9.dp.toPx(), center = Offset(coinX, coinY))
                    }
                }

                if (!isGameOverUi) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "👆 Toque na tela para PULAR obstáculos!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                if (isGameOverUi) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(10.dp),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.85f)
                            .padding(16.dp)
                            .testTag("runner_game_over_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "💥 Fim de Corrida!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Distância Percorrida: ${hudDistance}m",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Moedas Coletadas: +$hudCoins 🪙",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { restartGame() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Correr Novamente", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
