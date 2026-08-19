package com.example.ui.screens

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GameAudioManager
import com.example.audio.SoundEffect
import com.example.game.catchgame.CatchGameEngine
import com.example.game.catchgame.CatchGameEvent
import kotlinx.coroutines.isActive

private const val HUD_UPDATE_INTERVAL_SEC = 0.1f // ~10 Hz

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchMinigameScreen(
    onBack: () -> Unit,
    onFinishGame: (score: Int, coins: Int) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val audioManager = remember { GameAudioManager.getInstance(context) }
    val engine = remember { CatchGameEngine() }

    val skyBrush = remember {
        Brush.verticalGradient(
            listOf(Color(0xFFFEF08A), Color(0xFFFDE047), Color(0xFFF59E0B))
        )
    }
    val groundColor = remember { Color(0xFF78350F).copy(alpha = 0.3f) }
    val petBodyColor = remember { Color(0xFF3B82F6) }
    val petStrokeColor = remember { Color(0xFF1E3A8A) }
    val whiteColor = remember { Color.White }
    val blackColor = remember { Color.Black }

    val emojiPaint = remember {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            textAlign = AndroidPaint.Align.CENTER
            typeface = Typeface.DEFAULT
        }
    }
    val emojiTextSizePx = with(density) { 28.sp.toPx() }
    val petStroke = remember(density) {
        Stroke(width = with(density) { 3.dp.toPx() })
    }
    val bodyRadiusPx = with(density) { 26.dp.toPx() }
    val eyeOffsetX = with(density) { 8.dp.toPx() }
    val eyeOffsetY = with(density) { 6.dp.toPx() }
    val eyeRadiusPx = with(density) { 5.dp.toPx() }
    val pupilRadiusPx = with(density) { 2.5.dp.toPx() }

    var frameEpoch by remember { mutableIntStateOf(0) }

    var hudScore by remember { mutableIntStateOf(0) }
    var hudCoins by remember { mutableIntStateOf(0) }
    var hudLives by remember { mutableIntStateOf(CatchGameEngine.INITIAL_LIVES) }
    var hudCombo by remember { mutableIntStateOf(0) }

    var isGameOverUi by remember { mutableStateOf(false) }
    var finishReported by remember { mutableStateOf(false) }

    fun syncHudFromEngine() {
        hudScore = engine.score
        hudCoins = engine.coinsEarned
        hudLives = engine.lives
        hudCombo = engine.combo
    }

    fun restartGame() {
        engine.reset()
        syncHudFromEngine()
        isGameOverUi = false
        finishReported = false
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
                val dt = rawDt.coerceIn(0f, CatchGameEngine.MAX_DT_SECONDS)

                if (!engine.isGameOver) {
                    val events = engine.update(dt)
                    for (event in events) {
                        when (event) {
                            CatchGameEvent.COIN_CATCH ->
                                audioManager.playSfx(SoundEffect.COIN)
                            CatchGameEvent.GOOD_CATCH ->
                                audioManager.playSfx(SoundEffect.TAP)
                            CatchGameEvent.BOMB_HIT ->
                                audioManager.playSfx(SoundEffect.PET_SAD)
                            CatchGameEvent.GAME_OVER ->
                                audioManager.playSfx(SoundEffect.PET_SICK)
                        }
                    }
                    frameEpoch++

                    hudAccum += dt
                    if (hudAccum >= HUD_UPDATE_INTERVAL_SEC) {
                        hudAccum = 0f
                        syncHudFromEngine()
                    }

                    if (engine.isGameOver && !finishReported) {
                        finishReported = true
                        isGameOverUi = true
                        syncHudFromEngine()
                        onFinishGame(engine.score, engine.coinsEarned)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Captura de Objetos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { restartGame() },
                        modifier = Modifier.testTag("restart_catch_button")
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { i ->
                            Text(
                                text = if (i < hudLives) "❤️" else "🖤",
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Row {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = "Pontos: $hudScore",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🪙", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$hudCoins",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaNormalized = dragAmount.x / size.width
                            engine.setPetNormalizedX(engine.petNormalizedX + deltaNormalized)
                            // Redraw imediato do pet sem recompor HUD
                            frameEpoch++
                        }
                    }
                    .testTag("catch_canvas_box")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    @Suppress("UNUSED_EXPRESSION")
                    frameEpoch

                    val w = size.width
                    val h = size.height

                    drawRect(
                        color = groundColor,
                        topLeft = Offset(0f, h * 0.9f),
                        size = Size(w, h * 0.1f)
                    )

                    val items = engine.fallingItems
                    emojiPaint.textSize = emojiTextSizePx
                    drawIntoCanvas { canvas ->
                        val native = canvas.nativeCanvas
                        for (index in items.indices) {
                            val item = items[index]
                            native.drawText(
                                item.type.emoji,
                                item.x * w,
                                item.y * h,
                                emojiPaint
                            )
                        }
                    }

                    val basketX = engine.petNormalizedX * w
                    val basketY = h * 0.84f

                    drawCircle(petBodyColor, radius = bodyRadiusPx, center = Offset(basketX, basketY))
                    drawCircle(
                        petStrokeColor,
                        radius = bodyRadiusPx,
                        center = Offset(basketX, basketY),
                        style = petStroke
                    )
                    drawCircle(
                        whiteColor,
                        radius = eyeRadiusPx,
                        center = Offset(basketX - eyeOffsetX, basketY - eyeOffsetY)
                    )
                    drawCircle(
                        whiteColor,
                        radius = eyeRadiusPx,
                        center = Offset(basketX + eyeOffsetX, basketY - eyeOffsetY)
                    )
                    drawCircle(
                        blackColor,
                        radius = pupilRadiusPx,
                        center = Offset(basketX - eyeOffsetX, basketY - eyeOffsetY)
                    )
                    drawCircle(
                        blackColor,
                        radius = pupilRadiusPx,
                        center = Offset(basketX + eyeOffsetX, basketY - eyeOffsetY)
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            engine.nudgePet(-CatchGameEngine.PET_NUDGE)
                            frameEpoch++
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("move_left_button")
                    ) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Esquerda")
                    }

                    FilledTonalIconButton(
                        onClick = {
                            engine.nudgePet(CatchGameEngine.PET_NUDGE)
                            frameEpoch++
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("move_right_button")
                    ) {
                        Icon(Icons.Rounded.ArrowForwardIos, contentDescription = "Direita")
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
                            .testTag("catch_game_over_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🎮 Fim de Jogo!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Pontuação Total: $hudScore pts",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Moedas Ganhas: +$hudCoins 🪙",
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
                                Text("Jogar Novamente", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
