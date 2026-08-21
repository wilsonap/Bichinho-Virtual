package com.example.ui.screens

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.game.fishing.FishingGameEngine
import com.example.game.fishing.FishingGameEvent
import com.example.game.fishing.HookPhase
import com.example.ui.components.PetCanvasRenderer
import com.example.ui.screens.fishing.FishingCatchRenderer
import com.example.ui.screens.fishing.FishingLakeRenderer
import kotlinx.coroutines.isActive
import kotlin.math.max

private const val HUD_UPDATE_INTERVAL_SEC = 0.1f

/** Estado visual efêmero (fora do Compose state quente). */
private class FishingVisualFx {
    var phase: Float = 0f
    var splashTimer: Float = 0f
    var splashY: Float = FishingGameEngine.SURFACE_Y
    var rareSplash: Boolean = false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishingMinigameScreen(
    pet: PetEntity?,
    dayPeriod: DayPeriod = DayPeriod.AFTERNOON,
    weather: WeatherState = WeatherState.CLEAR,
    previousHighscore: Int = 0,
    onBack: () -> Unit,
    onFinishGame: (score: Int, coins: Int) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val audioManager = remember { GameAudioManager.getInstance(context) }
    val engine = remember { FishingGameEngine() }
    val fx = remember { FishingVisualFx() }

    val displayPet = remember(pet) {
        pet?.copy(isSleeping = false)
    }

    val rodColor = remember { Color(0xFF78350F) }
    val lineColor = remember { Color(0xFFE2E8F0) }
    val hookMetal = remember { Color(0xFFCBD5E1) }

    val emojiPaint = remember {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            textAlign = AndroidPaint.Align.CENTER
            typeface = Typeface.DEFAULT
        }
    }
    val emojiSizePx = with(density) { 24.sp.toPx() }

    var frameEpoch by remember { mutableIntStateOf(0) }
    var hudScore by remember { mutableIntStateOf(0) }
    var hudCombo by remember { mutableIntStateOf(0) }
    var hudTime by remember { mutableIntStateOf(60) }
    var hudPhase by remember { mutableStateOf(HookPhase.IDLE) }
    var hudReaction by remember { mutableStateOf<String?>(null) }
    var hudFisgou by remember { mutableStateOf(false) }
    var hudComboFlash by remember { mutableFloatStateOf(0f) }
    var isGameOverUi by remember { mutableStateOf(false) }
    var finishReported by remember { mutableStateOf(false) }
    var sessionBest by remember { mutableIntStateOf(previousHighscore) }
    var lastCoins by remember { mutableIntStateOf(0) }

    fun syncHud() {
        hudScore = engine.score
        hudCombo = engine.combo
        hudTime = engine.timeLeftSec.toInt().coerceAtLeast(0)
        hudPhase = engine.hookPhase
        hudReaction = engine.reactionText
    }

    fun restartGame() {
        engine.reset()
        fx.splashTimer = 0f
        fx.phase = 0f
        syncHud()
        hudFisgou = false
        hudComboFlash = 0f
        isGameOverUi = false
        finishReported = false
        lastCoins = 0
        frameEpoch++
    }

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
                val dt = rawDt.coerceIn(0f, FishingGameEngine.MAX_DT_SECONDS)

                fx.phase += dt
                if (fx.splashTimer > 0f) fx.splashTimer = (fx.splashTimer - dt).coerceAtLeast(0f)
                if (hudComboFlash > 0f) hudComboFlash = (hudComboFlash - dt).coerceAtLeast(0f)

                if (!engine.isGameOver) {
                    val prevCombo = engine.combo
                    val events = engine.update(dt)
                    for (event in events) {
                        when (event) {
                            FishingGameEvent.CAST -> audioManager.playSfx(SoundEffect.TAP)
                            FishingGameEvent.CATCH_FISH -> {
                                audioManager.playSfx(SoundEffect.TAP)
                                fx.splashTimer = 0.45f
                                fx.splashY = engine.hookY
                                fx.rareSplash = false
                                hudFisgou = true
                            }
                            FishingGameEvent.CATCH_RARE -> {
                                audioManager.playSfx(SoundEffect.COIN)
                                fx.splashTimer = 0.55f
                                fx.splashY = engine.hookY
                                fx.rareSplash = true
                                hudFisgou = true
                            }
                            FishingGameEvent.CATCH_JUNK -> {
                                audioManager.playSfx(SoundEffect.PET_SAD)
                                fx.splashTimer = 0.35f
                                fx.splashY = engine.hookY
                                fx.rareSplash = false
                                hudFisgou = false
                            }
                            FishingGameEvent.GAME_OVER -> audioManager.playSfx(SoundEffect.LEVEL_UP)
                        }
                    }
                    if (engine.combo > prevCombo && engine.combo >= 2) {
                        hudComboFlash = 0.7f
                    }
                    frameEpoch++
                    hudAccum += dt
                    if (hudAccum >= HUD_UPDATE_INTERVAL_SEC) {
                        hudAccum = 0f
                        syncHud()
                        if (fx.splashTimer <= 0f && engine.attached == null) {
                            hudFisgou = false
                        }
                    }
                    if (engine.isGameOver && !finishReported) {
                        val rewards = engine.consumeEndRewards()
                        if (rewards != null) {
                            finishReported = true
                            isGameOverUi = true
                            syncHud()
                            hudFisgou = false
                            lastCoins = rewards.second
                            sessionBest = max(sessionBest, rewards.first)
                            onFinishGame(rewards.first, rewards.second)
                        }
                    }
                } else {
                    frameEpoch++
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Pescaria", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { restartGame() },
                        modifier = Modifier.testTag("restart_fishing_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Reiniciar")
                    }
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("fishing_root")
        ) {
            // Canvas: lago + peixes + vara/linha (sem pet genérico)
            Canvas(modifier = Modifier.fillMaxSize().testTag("fishing_canvas_box")) {
                @Suppress("UNUSED_EXPRESSION")
                frameEpoch

                val w = size.width
                val h = size.height
                val surfaceY = FishingGameEngine.SURFACE_Y * h

                with(FishingLakeRenderer) {
                    drawLakeScene(w, h, fx.phase, dayPeriod, weather)
                }

                // Empunhadura da vara (próximo ao pet Compose na margem esquerda)
                val grip = Offset(w * 0.30f, surfaceY - h * 0.05f)
                val rodTip = Offset(w * FishingGameEngine.HOOK_X, surfaceY - h * 0.012f)
                drawLine(rodColor, grip, rodTip, strokeWidth = 7f, cap = StrokeCap.Round)
                drawLine(
                    Color(0xFFA16207),
                    grip,
                    Offset(grip.x - w * 0.04f, grip.y + h * 0.02f),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )

                val hookPos = Offset(w * FishingGameEngine.HOOK_X, engine.hookY * h)
                drawLine(lineColor, rodTip, hookPos, strokeWidth = 3.2f, cap = StrokeCap.Round)
                // Anzol
                drawCircle(hookMetal, 6f, hookPos)
                drawCircle(Color(0xFF64748B), 5f, Offset(hookPos.x + 1f, hookPos.y + 8f))
                drawLine(
                    Color(0xFF475569),
                    Offset(hookPos.x + 1f, hookPos.y + 6f),
                    Offset(hookPos.x + 6f, hookPos.y + 14f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )

                with(FishingCatchRenderer) {
                    drawCatchables(
                        engine.entities,
                        engine.attached?.id,
                        w, h,
                        emojiPaint,
                        emojiSizePx,
                        fx.phase
                    )
                    drawAttached(engine.attached, w, h, emojiPaint, emojiSizePx, fx.phase)
                }

                with(FishingLakeRenderer) {
                    drawSplash(w, h, fx.splashY, fx.splashTimer)
                }
                if (fx.rareSplash && fx.splashTimer > 0f) {
                    drawCircle(
                        Color(0xFFFDE047).copy(alpha = fx.splashTimer / 0.55f * 0.4f),
                        30f,
                        Offset(w * FishingGameEngine.HOOK_X, fx.splashY * h)
                    )
                }
            }

            // Pet real na margem (Compose — reutiliza PetCanvasRenderer)
            if (displayPet != null) {
                val petSize = 118.dp
                val surfaceYDp = maxHeight * FishingGameEngine.SURFACE_Y
                PetCanvasRenderer(
                    pet = displayPet,
                    size = petSize,
                    behaviorState = PetBehaviorState.SENTADO,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = maxWidth * 0.02f,
                            y = surfaceYDp - petSize * 0.70f
                        )
                        .testTag("fishing_pet")
                )
            }

            // HUD compacto
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("fishing_hud"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HudChip(text = "Pontos $hudScore", accent = Color(0xFF0EA5E9), modifier = Modifier.weight(1f))
                HudChip(
                    text = "Combo x${hudCombo.coerceAtLeast(1)}",
                    accent = if (hudComboFlash > 0f) Color(0xFFF59E0B) else Color(0xFFEA580C),
                    modifier = Modifier.weight(1f)
                )
                HudChip(text = "${hudTime}s", accent = Color(0xFF6366F1), modifier = Modifier.weight(0.7f))
            }

            if (hudFisgou && !isGameOverUi) {
                Text(
                    text = if (fx.rareSplash) "Fisgou! Raro!" else "Fisgou!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFBBF24),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-40).dp)
                        .testTag("fishing_fisgou")
                )
            }

            hudReaction?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEE2E2),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp)
                        .testTag("fishing_reaction")
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF9F1239)
                    )
                }
            }

            if (!isGameOverUi) {
                val (label, colors) = when (hudPhase) {
                    HookPhase.IDLE -> "Lancar" to ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7)
                    )
                    HookPhase.DESCENDING -> "Recolher" to ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEA580C)
                    )
                    HookPhase.ASCENDING -> "..." to ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF64748B)
                    )
                }
                Button(
                    onClick = {
                        engine.onCastTap()
                        frameEpoch++
                        syncHud()
                    },
                    enabled = hudPhase != HookPhase.ASCENDING,
                    colors = colors,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 14.dp)
                        .widthIn(max = 148.dp)
                        .height(42.dp)
                        .testTag("fishing_cast_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
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
                        .testTag("fishing_game_over_card")
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Fim da Pescaria!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Box(modifier = Modifier.height(10.dp))
                        Text(text = "Pontuacao: $hudScore pts", style = MaterialTheme.typography.bodyLarge)
                        Text(text = "Melhor pontuacao: $sessionBest pts", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Recompensa: +$lastCoins moedas",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                        Box(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { restartGame() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("fishing_play_again"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "Jogar novamente", fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("fishing_back_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "Voltar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HudChip(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.92f),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            maxLines = 1
        )
    }
}
