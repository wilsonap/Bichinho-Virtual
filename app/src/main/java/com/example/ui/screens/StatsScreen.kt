package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.GameStatsEntity
import com.example.data.local.PetEntity
import com.example.data.local.PlayerEntity
import com.example.data.model.PetStage
import com.example.data.model.Rarity
import com.example.data.model.Species
import com.example.ui.components.RarityBadge
import com.example.ui.components.StageBadge
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    pet: PetEntity?,
    player: PlayerEntity?,
    stats: GameStatsEntity?
) {
    val species = Species.fromId(pet?.speciesId ?: "")
    val stage = try {
        PetStage.valueOf(pet?.stage ?: "FILHOTE")
    } catch (_: Exception) {
        PetStage.FILHOTE
    }
    val rarity = try {
        Rarity.valueOf(pet?.rarity ?: "COMUM")
    } catch (_: Exception) {
        Rarity.COMUM
    }

    val birthDateFormatted = pet?.birthTimestamp?.let {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
    } ?: "Hoje"

    val daysAlive = pet?.birthTimestamp?.let {
        val diffMs = System.currentTimeMillis() - it
        (diffMs / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
    } ?: 1

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Estatísticas & Perfil", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pet Profile Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().testTag("stats_pet_profile_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = pet?.name?.ifBlank { species.displayName } ?: "Bichinho",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Espécie: ${species.displayName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            RarityBadge(rarity)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StageBadge(stage)
                            Text(
                                text = "Nível ${pet?.level ?: 1} (${pet?.totalExp ?: 0} XP total)",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = species.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Care History Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().testTag("stats_care_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Histórico de Cuidados",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        StatRow(label = "Dias de Convivência", value = "$daysAlive dia(s)")
                        StatRow(label = "Data de Nascimento", value = birthDateFormatted)
                        StatRow(label = "Refeições Servidas", value = "${stats?.timesFed ?: 0} vezes")
                        StatRow(label = "Banhos Tomados", value = "${stats?.timesBathed ?: 0} vezes")
                        StatRow(label = "Sonecas Realizadas", value = "${stats?.timesSlept ?: 0} vezes")
                        StatRow(label = "Brincadeiras e Jogos", value = "${stats?.timesPlayed ?: 0} vezes")
                        StatRow(label = "Visitas ao Médico", value = "${stats?.timesDoctor ?: 0} vezes")
                        StatRow(label = "Evoluções Alcançadas", value = "${stats?.evolutionsCount ?: 0}")
                    }
                }
            }

            // Minigames & Economy Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().testTag("stats_games_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Minijogos & Conquistas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        StatRow(label = "Sequência Diária Atual", value = "${player?.currentStreak ?: 1} dias")
                        StatRow(label = "Melhor Sequência Diária", value = "${player?.bestStreak ?: 1} dias")
                        StatRow(label = "Total de Partidas Jogadas", value = "${stats?.minigamesPlayed ?: 0}")
                        StatRow(label = "Recorde Jogo da Memória", value = "${stats?.memoryHighscore ?: 0} pts")
                        StatRow(label = "Recorde Corrida", value = "${stats?.runnerHighscore ?: 0} m")
                        StatRow(label = "Recorde Captura de Objetos", value = "${stats?.catchHighscore ?: 0} pts")
                        StatRow(label = "Total de Moedas Acumuladas", value = "🪙 ${stats?.totalCoinsEarned ?: 0}")
                        StatRow(label = "Itens Comprados na Loja", value = "${stats?.totalItemsBought ?: 0}")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
