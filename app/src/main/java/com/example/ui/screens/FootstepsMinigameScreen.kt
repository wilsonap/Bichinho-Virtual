package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GameAudioManager
import com.example.audio.SoundEffect
import com.example.data.local.PetEntity
import com.example.data.model.DayPeriod
import com.example.data.model.PetBehaviorState
import com.example.data.model.WeatherState
import com.example.game.footsteps.FootstepsEvent
import com.example.game.footsteps.FootstepsGameEngine
import com.example.game.footsteps.FootstepsPhase
import com.example.ui.components.OutdoorAmbience
import com.example.ui.components.PetCanvasRenderer
import kotlinx.coroutines.isActive
import kotlin.math.hypot
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootstepsMinigameScreen(
    pet: PetEntity?,
    dayPeriod: DayPeriod = DayPeriod.AFTERNOON,
    weather: WeatherState = WeatherState.CLEAR,
    previousHighscore: Int = 0,
    onBack: () -> Unit,
    onFinishGame: (score: Int, coins: Int) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val audio = remember { GameAudioManager.getInstance(context) }
    val engine = remember { FootstepsGameEngine() }

    val displayPet = remember(pet) { pet?.copy(isSleeping = false) }

    var frameEpoch by remember { mutableIntStateOf(0) }
    var animating by remember { mutableStateOf(false) }
    var hudScore by remember { mutableIntStateOf(0) }
    var hudCombo by remember { mutableIntStateOf(1) }
    var hudLives by remember { mutableIntStateOf(3) }
    var hudPhase by remember { mutableStateOf(FootstepsPhase.READY) }
    var statusText by remember { mutableStateOf("Toque em Comecar!") }
    var isGameOverUi by remember { mutableStateOf(false) }
    var finishReported by remember { mutableStateOf(false) }
    var sessionBest by remember { mutableIntStateOf(previousHighscore) }
    var lastCoins by remember { mutableIntStateOf(0) }
    var celebrate by remember { mutableStateOf(false) }

    fun syncHud() {
        hudScore = engine.score
        hudCombo = engine.combo
        hudLives = engine.lives
        hudPhase = engine.phase
        statusText = when (engine.phase) {
            FootstepsPhase.READY -> if (engine.round == 0) "Toque em Comecar!" else "Muito bem! Proxima rodada..."
            FootstepsPhase.DEMO_WALK, FootstepsPhase.DEMO_HOLD -> "Observe o caminho!"
            FootstepsPhase.PLAYER_INPUT -> "Agora repita o caminho!"
            FootstepsPhase.PLAYER_WALK -> "Agora repita o caminho!"
            FootstepsPhase.CELEBRATE -> "Muito bem!"
            FootstepsPhase.GAME_OVER -> "Fim de jogo!"
        }
        animating = engine.needsAnimationLoop
        celebrate = engine.phase == FootstepsPhase.CELEBRATE
    }

    fun handleEvents(events: List<FootstepsEvent>) {
        for (e in events) {
            when (e) {
                FootstepsEvent.WALK_START, FootstepsEvent.DEMO_STEP, FootstepsEvent.PLAYER_STEP_OK ->
                    audio.playSfx(SoundEffect.TAP)
                FootstepsEvent.DEMO_DONE -> Unit
                FootstepsEvent.PLAYER_STEP_WRONG -> audio.playSfx(SoundEffect.PET_SAD)
                FootstepsEvent.ROUND_COMPLETE -> audio.playSfx(SoundEffect.COIN)
                FootstepsEvent.GAME_OVER -> audio.playSfx(SoundEffect.PET_SICK)
                FootstepsEvent.WALK_ARRIVED -> Unit
            }
        }
    }

    fun startOrContinue() {
        if (engine.isGameOver) return
        when (engine.phase) {
            FootstepsPhase.READY -> {
                val ev = engine.startRound()
                handleEvents(ev)
                syncHud()
                frameEpoch++
            }
            FootstepsPhase.CELEBRATE -> Unit
            else -> Unit
        }
    }

    fun restartGame() {
        engine.reset()
        isGameOverUi = false
        finishReported = false
        lastCoins = 0
        celebrate = false
        syncHud()
        frameEpoch++
    }

    // Auto-inicia primeira rodada e próximas após celebração
    LaunchedEffect(hudPhase) {
        if (hudPhase == FootstepsPhase.READY && !engine.isGameOver) {
            kotlinx.coroutines.delay(400)
            if (engine.phase == FootstepsPhase.READY && !engine.isGameOver) {
                startOrContinue()
            }
        }
    }

    // Loop só enquanto anima
    LaunchedEffect(animating) {
        if (!animating) return@LaunchedEffect
        var last = 0L
        while (isActive && engine.needsAnimationLoop) {
            withFrameNanos { now ->
                if (last == 0L) {
                    last = now
                    return@withFrameNanos
                }
                val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, FootstepsGameEngine.MAX_DT_SECONDS)
                last = now
                val events = engine.update(dt)
                handleEvents(events)
                frameEpoch++
                syncHud()
                if (engine.isGameOver && !finishReported) {
                    val rewards = engine.consumeEndRewards()
                    if (rewards != null) {
                        finishReported = true
                        isGameOverUi = true
                        lastCoins = rewards.second
                        sessionBest = max(sessionBest, rewards.first)
                        onFinishGame(rewards.first, rewards.second)
                    }
                }
            }
        }
        syncHud()
    }

    val petSize = 100.dp
    val petSizePx = with(density) { petSize.toPx() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Siga as Pegadas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { restartGame() }, modifier = Modifier.testTag("restart_footsteps_button")) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Reiniciar")
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("footsteps_root")
        ) {
            val theme = OutdoorAmbience.theme(dayPeriod, weather)

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(hudPhase, isGameOverUi) {
                        detectTapGestures { offset ->
                            if (isGameOverUi || engine.phase != FootstepsPhase.PLAYER_INPUT) return@detectTapGestures
                            val w = size.width
                            val h = size.height
                            val pad = hitPad(offset.x / w, offset.y / h) ?: return@detectTapGestures
                            val events = engine.onPadTapped(pad)
                            handleEvents(events)
                            syncHud()
                            frameEpoch++
                            if (engine.isGameOver && !finishReported) {
                                val rewards = engine.consumeEndRewards()
                                if (rewards != null) {
                                    finishReported = true
                                    isGameOverUi = true
                                    lastCoins = rewards.second
                                    sessionBest = max(sessionBest, rewards.first)
                                    onFinishGame(rewards.first, rewards.second)
                                }
                            }
                        }
                    }
                    .testTag("footsteps_canvas")
            ) {
                @Suppress("UNUSED_EXPRESSION")
                frameEpoch

                val w = size.width
                val h = size.height

                // Céu / parque
                drawRect(
                    brush = Brush.verticalGradient(theme.skyColors, endY = h * 0.35f),
                    size = Size(w, h * 0.35f)
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF86EFAC), Color(0xFF4ADE80), Color(0xFF22C55E)),
                        startY = h * 0.32f,
                        endY = h
                    ),
                    topLeft = Offset(0f, h * 0.32f),
                    size = Size(w, h * 0.68f)
                )
                // Árvores
                drawCircle(Color(0xFF166534), 36f, Offset(w * 0.08f, h * 0.38f))
                drawRect(Color(0xFF78350F), Offset(w * 0.08f - 5f, h * 0.38f), Size(10f, 40f))
                drawCircle(Color(0xFF15803D), 32f, Offset(w * 0.92f, h * 0.40f))
                drawRect(Color(0xFF78350F), Offset(w * 0.92f - 5f, h * 0.40f), Size(10f, 36f))
                // Flores
                for (i in 0..6) {
                    val fx = w * (0.12f + i * 0.12f)
                    drawCircle(Color(0xFFF472B6), 4f, Offset(fx, h * 0.88f))
                    drawLine(Color(0xFF166534), Offset(fx, h * 0.88f), Offset(fx, h * 0.92f), 2f)
                }

                // Pads / pedras
                for (i in 0 until FootstepsGameEngine.PAD_COUNT) {
                    val px = FootstepsGameEngine.PAD_X[i] * w
                    val py = FootstepsGameEngine.PAD_Y[i] * h
                    val base = Color(0xFFD6D3D1).copy(alpha = 0.55f)
                    val highlighted = engine.highlightPad == i
                    val foot = i in engine.visibleFootprints
                    val err = engine.errorPad == i && engine.errorTimer > 0f
                    val fill = when {
                        err -> Color(0xFFFECACA)
                        highlighted -> Color(0xFFFEF08A)
                        foot -> Color(0xFFE7E5E4)
                        else -> base
                    }
                    drawRoundRect(
                        color = fill,
                        topLeft = Offset(px - 34f, py - 22f),
                        size = Size(68f, 44f),
                        cornerRadius = CornerRadius(16f, 16f)
                    )
                    drawRoundRect(
                        color = Color(0xFF78716C).copy(alpha = 0.45f),
                        topLeft = Offset(px - 34f, py - 22f),
                        size = Size(68f, 44f),
                        cornerRadius = CornerRadius(16f, 16f),
                        style = Stroke(width = 2f)
                    )
                    if (foot || highlighted) {
                        // pegada simples
                        drawCircle(Color(0xFF57534E).copy(alpha = 0.7f), 6f, Offset(px - 4f, py - 2f))
                        drawCircle(Color(0xFF57534E).copy(alpha = 0.7f), 6f, Offset(px + 6f, py + 2f))
                    }
                    if (err) {
                        drawCircle(Color(0xFFEF4444).copy(alpha = 0.55f), 16f, Offset(px, py), style = Stroke(3f))
                    }
                }
            }

            // Pet real
            if (displayPet != null) {
                val behavior = when {
                    celebrate -> PetBehaviorState.FELIZ
                    engine.phase == FootstepsPhase.DEMO_WALK ||
                        engine.phase == FootstepsPhase.PLAYER_WALK -> PetBehaviorState.CAMINHANDO
                    else -> PetBehaviorState.OCIOSO
                }
                PetCanvasRenderer(
                    pet = displayPet,
                    size = petSize,
                    behaviorState = behavior,
                    walkDirection = if (engine.animToX >= engine.animFromX) 1f else -1f,
                    modifier = Modifier
                        .offset(
                            x = with(density) { (engine.petX * constraints.maxWidth - petSizePx / 2f).toDp() },
                            y = with(density) { (engine.petY * constraints.maxHeight - petSizePx * 0.72f).toDp() }
                        )
                        .testTag("footsteps_pet")
                )
            }

            // HUD
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(10.dp)
                    .testTag("footsteps_hud"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Chip("⭐ $hudScore", Color(0xFF0EA5E9), Modifier.weight(1f))
                Chip("🔥 x$hudCombo", Color(0xFFEA580C), Modifier.weight(0.8f))
                Chip(
                    text = buildString {
                        repeat(3) { i -> append(if (i < hudLives) "❤️" else "🖤") }
                    },
                    Color(0xFFBE123C),
                    Modifier.weight(1.1f)
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 52.dp)
                    .testTag("footsteps_status")
            ) {
                Text(
                    text = statusText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            if (isGameOverUi) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(10.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.88f)
                        .padding(16.dp)
                        .testTag("footsteps_game_over_card")
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Fim de jogo!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Box(modifier = Modifier.height(10.dp))
                        Text("Pontuacao: $hudScore pts")
                        Text("Melhor: $sessionBest pts")
                        Text(
                            text = "Recompensa: +$lastCoins moedas",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                        Box(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { restartGame() },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("footsteps_play_again")
                        ) {
                            Text("Jogar novamente", fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Voltar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.92f),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Text(
            text = text,
            color = accent,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            maxLines = 1
        )
    }
}

private fun hitPad(nx: Float, ny: Float): Int? {
    var best = -1
    var bestDist = 0.09f
    for (i in 0 until FootstepsGameEngine.PAD_COUNT) {
        val d = hypot(nx - FootstepsGameEngine.PAD_X[i], ny - FootstepsGameEngine.PAD_Y[i])
        if (d < bestDist) {
            bestDist = d
            best = i
        }
    }
    return if (best >= 0) best else null
}
