package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GameAudioManager
import com.example.audio.SoundEffect
import kotlinx.coroutines.delay
import kotlin.random.Random

data class RunnerObstacle(
    var x: Float,
    val width: Float = 36f,
    val height: Float = 48f
)

data class RunnerCoin(
    var x: Float,
    val y: Float,
    val radius: Float = 16f,
    var isCollected: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunnerMinigameScreen(
    onBack: () -> Unit,
    onFinishGame: (score: Int, coins: Int) -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { GameAudioManager.getInstance(context) }
    var isRunning by remember { mutableStateOf(true) }
    var isGameOver by remember { mutableStateOf(false) }

    // Game Physics & State
    var petY by remember { mutableFloatStateOf(0f) }
    var petVelocityY by remember { mutableFloatStateOf(0f) }
    var isGrounded by remember { mutableStateOf(true) }

    var distanceScore by remember { mutableIntStateOf(0) }
    var coinsCollected by remember { mutableIntStateOf(0) }

    val obstacles = remember { mutableStateListOf<RunnerObstacle>() }
    val collectibleCoins = remember { mutableStateListOf<RunnerCoin>() }

    var groundY by remember { mutableFloatStateOf(0f) }
    var speed by remember { mutableFloatStateOf(7.5f) }

    fun jump() {
        if (isGrounded && isRunning && !isGameOver) {
            audioManager.playSfx(SoundEffect.TAP)
            petVelocityY = -18f
            isGrounded = false
        }
    }

    fun restartGame() {
        petY = 0f
        petVelocityY = 0f
        isGrounded = true
        distanceScore = 0
        coinsCollected = 0
        speed = 7.5f
        obstacles.clear()
        collectibleCoins.clear()
        isGameOver = false
        isRunning = true
    }

    // Main Game Loop
    LaunchedEffect(isRunning, isGameOver) {
        var spawnTimer = 0
        while (isRunning && !isGameOver) {
            delay(20) // ~50 fps

            distanceScore += 1
            if (distanceScore % 150 == 0) {
                speed += 0.4f
            }

            // Gravity & Jump Physics
            if (!isGrounded) {
                petY += petVelocityY
                petVelocityY += 1.1f // Gravity
                if (petY >= 0f) {
                    petY = 0f
                    petVelocityY = 0f
                    isGrounded = true
                }
            }

            // Spawn Obstacles & Coins
            spawnTimer++
            if (spawnTimer >= 80) {
                spawnTimer = 0
                obstacles.add(RunnerObstacle(x = 1000f, width = 36f, height = Random.nextInt(35, 55).toFloat()))

                if (Random.nextBoolean()) {
                    collectibleCoins.add(RunnerCoin(x = 1000f + Random.nextInt(60, 160), y = -Random.nextInt(50, 130).toFloat()))
                }
            }

            // Move Obstacles
            val iterator = obstacles.iterator()
            while (iterator.hasNext()) {
                val obs = iterator.next()
                obs.x -= speed
                if (obs.x < -100f) {
                    iterator.remove()
                } else {
                    // Collision check with Pet (Pet position: X around 140, Y from petY)
                    val petLeft = 110f
                    val petRight = 170f
                    val petBottom = 0f + petY
                    val petTop = -60f + petY

                    val obsLeft = obs.x
                    val obsRight = obs.x + obs.width
                    val obsTop = -obs.height

                    if (petRight > obsLeft && petLeft < obsRight && petBottom > obsTop) {
                        audioManager.playSfx(SoundEffect.PET_SAD)
                        isGameOver = true
                        isRunning = false
                        val totalCoinsEarned = coinsCollected + (distanceScore / 40)
                        onFinishGame(distanceScore, totalCoinsEarned)
                    }
                }
            }

            // Move Coins
            val coinIter = collectibleCoins.iterator()
            while (coinIter.hasNext()) {
                val coin = coinIter.next()
                coin.x -= speed
                if (coin.x < -100f) {
                    coinIter.remove()
                } else if (!coin.isCollected) {
                    // Check Coin Pickup
                    val petCenterX = 140f
                    val petCenterY = -30f + petY
                    val dx = petCenterX - coin.x
                    val dy = petCenterY - coin.y
                    val distSq = dx * dx + dy * dy
                    if (distSq < 42f * 42f) {
                        audioManager.playSfx(SoundEffect.COIN)
                        coin.isCollected = true
                        coinsCollected++
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
                    IconButton(onClick = { restartGame() }, modifier = Modifier.testTag("restart_runner_button")) {
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
                .background(Brush.verticalGradient(listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD), Color(0xFF7DD3FC))))
        ) {
            // Stats HUD
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
                        text = "Distância: ${distanceScore}m",
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
                            text = "$coinsCollected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F)
                        )
                    }
                }
            }

            // Game Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { jump() })
                    }
                    .testTag("runner_canvas_container")
            ) {
                Canvas(modifier = Modifier.fillMaxSize().testTag("runner_canvas")) {
                    val w = size.width
                    val h = size.height
                    val groundLevel = h * 0.72f
                    groundY = groundLevel

                    // Draw Clouds
                    drawCircle(Color.White.copy(alpha = 0.8f), radius = 30.dp.toPx(), center = Offset(w * 0.2f, h * 0.18f))
                    drawCircle(Color.White.copy(alpha = 0.8f), radius = 42.dp.toPx(), center = Offset(w * 0.28f, h * 0.16f))
                    drawCircle(Color.White.copy(alpha = 0.8f), radius = 28.dp.toPx(), center = Offset(w * 0.36f, h * 0.19f))

                    drawCircle(Color.White.copy(alpha = 0.7f), radius = 36.dp.toPx(), center = Offset(w * 0.75f, h * 0.24f))
                    drawCircle(Color.White.copy(alpha = 0.7f), radius = 24.dp.toPx(), center = Offset(w * 0.82f, h * 0.25f))

                    // Draw Ground Layer
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF22C55E), Color(0xFF16A34A), Color(0xFF78350F)),
                            startY = groundLevel,
                            endY = h
                        ),
                        topLeft = Offset(0f, groundLevel),
                        size = Size(w, h - groundLevel)
                    )
                    drawLine(
                        color = Color(0xFF15803D),
                        start = Offset(0f, groundLevel),
                        end = Offset(w, groundLevel),
                        strokeWidth = 6.dp.toPx()
                    )

                    // Draw Pet Runner Avatar
                    val petBaseX = 140f
                    val currentPetY = groundLevel + petY

                    // Pet Body
                    drawCircle(
                        color = Color(0xFFF59E0B),
                        radius = 28.dp.toPx(),
                        center = Offset(petBaseX, currentPetY - 30.dp.toPx())
                    )
                    // Pet Eyes & Smile
                    drawCircle(Color(0xFF1E293B), radius = 4.dp.toPx(), center = Offset(petBaseX + 8.dp.toPx(), currentPetY - 34.dp.toPx()))
                    drawCircle(Color.White, radius = 1.5.dp.toPx(), center = Offset(petBaseX + 7.dp.toPx(), currentPetY - 35.dp.toPx()))
                    drawCircle(Color(0xFFF472B6), radius = 4.dp.toPx(), center = Offset(petBaseX + 16.dp.toPx(), currentPetY - 26.dp.toPx()))

                    // Draw Obstacles (Hurdles)
                    obstacles.forEach { obs ->
                        val obsX = (obs.x / 1000f) * w
                        val obsH = obs.height.dp.toPx()
                        val obsW = obs.width.dp.toPx()

                        drawRoundRect(
                            color = Color(0xFFDC2626),
                            topLeft = Offset(obsX, groundLevel - obsH),
                            size = Size(obsW, obsH),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                        drawRoundRect(
                            color = Color(0xFFFEF2F2),
                            topLeft = Offset(obsX + 4.dp.toPx(), groundLevel - obsH + 4.dp.toPx()),
                            size = Size(obsW - 8.dp.toPx(), 8.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }

                    // Draw Collectible Coins
                    collectibleCoins.filter { !it.isCollected }.forEach { coin ->
                        val coinX = (coin.x / 1000f) * w
                        val coinY = groundLevel + coin.y.dp.toPx()

                        drawCircle(
                            color = Color(0xFFF59E0B),
                            radius = 12.dp.toPx(),
                            center = Offset(coinX, coinY)
                        )
                        drawCircle(
                            color = Color(0xFFFDE047),
                            radius = 9.dp.toPx(),
                            center = Offset(coinX, coinY)
                        )
                    }
                }

                // Tap Guide
                if (isRunning && !isGameOver) {
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
                            .testTag("runner_game_over_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "💥 Fim de Corrida!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Distância Percorrida: ${distanceScore}m", style = MaterialTheme.typography.bodyLarge)
                            Text(text = "Moedas Coletadas: +$coinsCollected 🪙", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { restartGame() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
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
