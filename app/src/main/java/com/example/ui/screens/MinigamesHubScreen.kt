package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.GameStatsEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinigamesHubScreen(
    stats: GameStatsEntity?,
    onSelectGame: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minijogos", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().testTag("minigames_info_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🎮", fontSize = 32.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ganhe Moedas e Felicidade!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Jogue minijogos para acumular moedas, subir o nível de experiência e deixar seu bichinho nas alturas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            item {
                MinigameCard(
                    title = "Jogo da Memória",
                    description = "Encontre todos os pares de bichinhos no menor número de jogadas!",
                    icon = Icons.Rounded.Psychology,
                    iconBgColor = Color(0xFF8B5CF6),
                    highscore = stats?.memoryHighscore ?: 0,
                    highscoreSuffix = "pts",
                    onClick = { onSelectGame("memory") },
                    testTag = "play_memory_game"
                )
            }

            item {
                MinigameCard(
                    title = "Corrida do Bichinho",
                    description = "Corra e salte por cima de obstáculos perigosos coletando moedas de ouro!",
                    icon = Icons.Rounded.DirectionsRun,
                    iconBgColor = Color(0xFF0EA5E9),
                    highscore = stats?.runnerHighscore ?: 0,
                    highscoreSuffix = "m",
                    onClick = { onSelectGame("runner") },
                    testTag = "play_runner_game"
                )
            }

            item {
                MinigameCard(
                    title = "Captura de Objetos",
                    description = "Desvie das bombas e capture frutas, estrelas e guloseimas deliciosas!",
                    icon = Icons.Rounded.PanTool,
                    iconBgColor = Color(0xFFF59E0B),
                    highscore = stats?.catchHighscore ?: 0,
                    highscoreSuffix = "pts",
                    onClick = { onSelectGame("catch") },
                    testTag = "play_catch_game"
                )
            }
        }
    }
}

@Composable
private fun MinigameCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconBgColor: Color,
    highscore: Int,
    highscoreSuffix: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = iconBgColor,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Recorde: $highscore $highscoreSuffix",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = "Jogar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
