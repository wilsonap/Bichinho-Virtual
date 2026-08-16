package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GameAudioManager
import com.example.audio.SoundEffect
import kotlinx.coroutines.delay
import kotlin.math.max

data class MemoryCard(
    val id: Int,
    val content: String,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryMinigameScreen(
    onBack: () -> Unit,
    onFinishGame: (score: Int, coins: Int) -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { GameAudioManager.getInstance(context) }
    val symbols = listOf("🐱", "🐶", "🐰", "🐹", "🦊", "🐼")
    var cards by remember {
        mutableStateOf(
            (symbols + symbols).shuffled().mapIndexed { index, symbol ->
                MemoryCard(id = index, content = symbol)
            }
        )
    }

    var selectedIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var moves by remember { mutableIntStateOf(0) }
    var matchesFound by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }

    fun restartGame() {
        cards = (symbols + symbols).shuffled().mapIndexed { index, symbol ->
            MemoryCard(id = index, content = symbol)
        }
        selectedIndices = emptyList()
        moves = 0
        matchesFound = 0
        isGameOver = false
        isChecking = false
    }

    LaunchedEffect(selectedIndices) {
        if (selectedIndices.size == 2) {
            isChecking = true
            moves++
            val first = cards[selectedIndices[0]]
            val second = cards[selectedIndices[1]]

            if (first.content == second.content) {
                delay(300)
                audioManager.playSfx(SoundEffect.COIN)
                cards = cards.mapIndexed { index, card ->
                    if (index == selectedIndices[0] || index == selectedIndices[1]) {
                        card.copy(isMatched = true, isFaceUp = true)
                    } else card
                }
                matchesFound++
                selectedIndices = emptyList()
                isChecking = false

                if (matchesFound == symbols.size) {
                    audioManager.playSfx(SoundEffect.LEVEL_UP)
                    isGameOver = true
                    val score = max(100, 1000 - (moves * 35))
                    val coins = max(15, 50 - (moves * 2))
                    onFinishGame(score, coins)
                }
            } else {
                delay(900)
                audioManager.playSfx(SoundEffect.PET_SAD)
                cards = cards.mapIndexed { index, card ->
                    if (index == selectedIndices[0] || index == selectedIndices[1]) {
                        card.copy(isFaceUp = false)
                    } else card
                }
                selectedIndices = emptyList()
                isChecking = false
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Jogo da Memória", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { restartGame() }, modifier = Modifier.testTag("restart_memory_button")) {
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Movimentos: $moves",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Pares: $matchesFound / ${symbols.size}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dynamic Card Grid that strictly fits in available viewport (4 rows x 3 columns = 12 cards)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("memory_grid"),
                contentAlignment = Alignment.Center
            ) {
                val availableW = maxWidth
                val availableH = maxHeight
                val spacing = if (availableH < 400.dp || availableW < 320.dp) 6.dp else 8.dp

                // 4 Rows, 3 Columns fitted perfectly to available container
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            for (col in 0 until 3) {
                                val index = (row * 3) + col
                                val card = cards.getOrNull(index)

                                if (card != null) {
                                    val isFaceUp = card.isFaceUp || card.isMatched
                                    val rotation by animateFloatAsState(
                                        targetValue = if (isFaceUp) 180f else 0f,
                                        animationSpec = tween(350),
                                        label = "card_flip"
                                    )

                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (card.isMatched) {
                                                Color(0xFFD1FAE5) // Light green for matched
                                            } else if (isFaceUp) {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        ),
                                        elevation = CardDefaults.cardElevation(if (card.isMatched) 1.dp else 3.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .graphicsLayer {
                                                rotationY = rotation
                                                cameraDistance = 12f * density
                                            }
                                            .clickable(enabled = !isFaceUp && !isChecking && !card.isMatched) {
                                                cards = cards.mapIndexed { i, c ->
                                                    if (i == index) c.copy(isFaceUp = true) else c
                                                }
                                                selectedIndices = selectedIndices + index
                                            }
                                            .testTag("memory_card_$index")
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (rotation > 90f) {
                                                Text(
                                                    text = card.content,
                                                    fontSize = if (availableH < 420.dp) 24.sp else 30.sp,
                                                    modifier = Modifier.graphicsLayer { rotationY = 180f }
                                                )
                                            } else {
                                                Text(
                                                    text = "❓",
                                                    fontSize = if (availableH < 420.dp) 18.sp else 22.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Win Modal Overlay over the grid (so grid is never cut off or pushed down)
                if (isGameOver) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF59E0B)),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .testTag("memory_game_over_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🎉 Parabéns! Você Venceu!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF78350F)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Você encontrou todos os 6 pares em $moves jogadas!",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { restartGame() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(40.dp)
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
