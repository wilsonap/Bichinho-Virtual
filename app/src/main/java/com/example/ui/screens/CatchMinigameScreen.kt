package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GameAudioManager
import com.example.audio.SoundEffect
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

data class FallingItem(
    val id: Long,
    var x: Float,
    var y: Float,
    val type: CatchItemType,
    val speed: Float = 6f
)

enum class CatchItemType(val emoji: String, val points: Int, val isBomb: Boolean) {
    APPLE("🍎", 10, false),
    COOKIE("🍪", 15, false),
    STAR("⭐", 25, false),
    COIN("🪙", 20, false),
    BOMB("💣", -1, true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchMinigameScreen(
    onBack: () -> Unit,
    onFinishGame: (score: Int, coins: Int) -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { GameAudioManager.getInstance(context) }
    var isRunning by remember { mutableStateOf(true) }
    var isGameOver by remember { mutableStateOf(false) }

    var petNormalizedX by remember { mutableFloatStateOf(0.5f) } // 0.0 to 1.0
    var score by remember { mutableIntStateOf(0) }
    var coinsEarned by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var combo by remember { mutableIntStateOf(0) }

    val fallingItems = remember { mutableStateListOf<FallingItem>() }
    var nextItemId by remember { mutableLongStateOf(0L) }

    fun restartGame() {
        petNormalizedX = 0.5f
        score = 0
        coinsEarned = 0
        lives = 3
        combo = 0
        fallingItems.clear()
        isGameOver = false
        isRunning = true
    }

    // Game loop
    LaunchedEffect(isRunning, isGameOver) {
        var tick = 0
        while (isRunning && !isGameOver) {
            delay(20) // ~50 fps
            tick++

            // Spawn falling item every 35-45 ticks
            if (tick % 35 == 0) {
                val isBomb = Random.nextInt(100) < 25
                val type = if (isBomb) CatchItemType.BOMB else listOf(CatchItemType.APPLE, CatchItemType.COOKIE, CatchItemType.STAR, CatchItemType.COIN).random()
                fallingItems.add(
                    FallingItem(
                        id = nextItemId++,
                        x = Random.nextFloat().coerceIn(0.1f, 0.9f),
                        y = -0.05f,
                        type = type,
                        speed = Random.nextFloat() * 0.004f + 0.008f
                    )
                )
            }

            // Update items position
            val iterator = fallingItems.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                item.y += item.speed

                // Check Catch at bottom (Y between 0.78 and 0.88)
                if (item.y in 0.78f..0.88f) {
                    val dist = abs(item.x - petNormalizedX)
                    if (dist < 0.12f) {
                        // Caught!
                        if (item.type.isBomb) {
                            audioManager.playSfx(SoundEffect.PET_SAD)
                            lives -= 1
                            combo = 0
                            if (lives <= 0) {
                                audioManager.playSfx(SoundEffect.PET_SICK)
                                isGameOver = true
                                isRunning = false
                                onFinishGame(score, coinsEarned)
                            }
                        } else {
                            if (item.type == CatchItemType.COIN) {
                                audioManager.playSfx(SoundEffect.COIN)
                                coinsEarned += 2
                            } else {
                                audioManager.playSfx(SoundEffect.TAP)
                                coinsEarned += 1
                            }
                            score += item.type.points
                            combo += 1
                        }
                        iterator.remove()
                        continue
                    }
                }

                // Missed bottom
                if (item.y > 1.05f) {
                    if (!item.type.isBomb) {
                        combo = 0
                    }
                    iterator.remove()
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
                    IconButton(onClick = { restartGame() }, modifier = Modifier.testTag("restart_catch_button")) {
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
                .background(Brush.verticalGradient(listOf(Color(0xFFFEF08A), Color(0xFFFDE047), Color(0xFFF59E0B))))
        ) {
            // Top HUD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lives
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { i ->
                            Text(text = if (i < lives) "❤️" else "🖤", fontSize = 16.sp)
                        }
                    }
                }

                // Score & Coins
                Row {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = "Pontos: $score",
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
                                text = "$coinsEarned",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F)
                            )
                        }
                    }
                }
            }

            // Canvas & Falling Objects
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaNormalized = dragAmount.x / size.width
                            petNormalizedX = (petNormalizedX + deltaNormalized).coerceIn(0.1f, 0.9f)
                        }
                    }
                    .testTag("catch_canvas_box")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Ground
                    drawRect(
                        color = Color(0xFF78350F).copy(alpha = 0.3f),
                        topLeft = Offset(0f, h * 0.9f),
                        size = androidx.compose.ui.geometry.Size(w, h * 0.1f)
                    )

                    // Draw Pet Basket / Catcher at petNormalizedX
                    val basketX = petNormalizedX * w
                    val basketY = h * 0.84f

                    // Pet Catcher Body
                    drawCircle(
                        color = Color(0xFF3B82F6),
                        radius = 26.dp.toPx(),
                        center = Offset(basketX, basketY)
                    )
                    drawCircle(
                        color = Color(0xFF1E3A8A),
                        radius = 26.dp.toPx(),
                        center = Offset(basketX, basketY),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx())
                    )
                    // Cute Eyes
                    drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(basketX - 8.dp.toPx(), basketY - 6.dp.toPx()))
                    drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(basketX + 8.dp.toPx(), basketY - 6.dp.toPx()))
                    drawCircle(Color.Black, radius = 2.5.dp.toPx(), center = Offset(basketX - 8.dp.toPx(), basketY - 6.dp.toPx()))
                    drawCircle(Color.Black, radius = 2.5.dp.toPx(), center = Offset(basketX + 8.dp.toPx(), basketY - 6.dp.toPx()))
                }

                // Render Emojis for falling items
                fallingItems.forEach { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(
                                    x = (item.x * 320).dp,
                                    y = (item.y * 500).dp
                                )
                        ) {
                            Text(text = item.type.emoji, fontSize = 28.sp)
                        }
                    }
                }

                // Control Buttons for easy touch
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilledTonalIconButton(
                        onClick = { petNormalizedX = (petNormalizedX - 0.12f).coerceIn(0.1f, 0.9f) },
                        modifier = Modifier.size(56.dp).testTag("move_left_button")
                    ) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Esquerda")
                    }

                    FilledTonalIconButton(
                        onClick = { petNormalizedX = (petNormalizedX + 0.12f).coerceIn(0.1f, 0.9f) },
                        modifier = Modifier.size(56.dp).testTag("move_right_button")
                    ) {
                        Icon(Icons.Rounded.ArrowForwardIos, contentDescription = "Direita")
                    }
                }

                // Game Over Overlay
                if (isGameOver) {
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
                            Text(text = "🎮 Fim de Jogo!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "Pontuação Total: $score pts", style = MaterialTheme.typography.bodyLarge)
                            Text(text = "Moedas Ganhas: +$coinsEarned 🪙", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { restartGame() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
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
