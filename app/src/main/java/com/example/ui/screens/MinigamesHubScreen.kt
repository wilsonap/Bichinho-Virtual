package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
    // Insets zerados: o Scaffold do MainApp ja reserva a Bottom Navigation.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Minijogos", fontWeight = FontWeight.Bold) },
                expandedHeight = 48.dp,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("minigames_list"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                // Folga extra para o ultimo card nao colar na Bottom Nav
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("minigames_info_card")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "\uD83C\uDFAE", fontSize = 26.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Ganhe Moedas e Felicidade!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Jogue minijogos para acumular moedas, subir o nível e deixar seu bichinho feliz.",
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
                    description = "Corra e salte por cima de obstáculos coletando moedas de ouro!",
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
                    description = "Desvie das bombas e capture frutas, estrelas e guloseimas!",
                    icon = Icons.Rounded.PanTool,
                    iconBgColor = Color(0xFFF59E0B),
                    highscore = stats?.catchHighscore ?: 0,
                    highscoreSuffix = "pts",
                    onClick = { onSelectGame("catch") },
                    testTag = "play_catch_game"
                )
            }

            item {
                MinigameCard(
                    title = "Pescaria",
                    description = "Lance o anzol, puxe na hora certa e pesque peixes raros!",
                    icon = Icons.Rounded.Pool,
                    iconBgColor = Color(0xFF0284C7),
                    highscore = stats?.fishingHighscore ?: 0,
                    highscoreSuffix = "pts",
                    onClick = { onSelectGame("fishing") },
                    testTag = "play_fishing_game"
                )
            }

            item {
                MinigameCard(
                    title = "Siga as Pegadas",
                    description = "Observe o caminho do bichinho e repita a sequência no parque!",
                    icon = Icons.Rounded.Pets,
                    iconBgColor = Color(0xFF16A34A),
                    highscore = stats?.footstepsHighscore ?: 0,
                    highscoreSuffix = "pts",
                    onClick = { onSelectGame("footsteps") },
                    testTag = "play_footsteps_game"
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconBgColor,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Recorde: $highscore $highscoreSuffix",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = "Jogar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
