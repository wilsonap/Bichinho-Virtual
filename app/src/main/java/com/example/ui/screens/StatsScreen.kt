package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.GameStatsEntity
import com.example.data.local.PetEntity
import com.example.data.local.PlayerEntity
import com.example.data.model.EvolutionProgress
import com.example.data.model.PetEvolutionCalculator
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

    val evolutionProgress: EvolutionProgress? = pet?.let {
        PetEvolutionCalculator.getEvolutionProgress(it)
    }

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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StageBadge(stage)
                            Text(
                                text = "Nível de Vínculo: ${pet?.level ?: 1}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stage.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Hybrid Evolution & Bond Milestone Card
            if (evolutionProgress != null && pet.isHatched) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().testTag("stats_evolution_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ciclo de Vida & Evolução",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            StatRow(label = "Fase Atual", value = stage.displayName)

                            if (evolutionProgress.nextStage != null) {
                                val next = evolutionProgress.nextStage
                                StatRow(label = "Próxima Fase", value = next.displayName)

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Requisitos para Evolução:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                // Requirement 1: Days of Life
                                EvolutionRequirementRow(
                                    title = "Tempo de Convivência Real",
                                    currentValue = "${evolutionProgress.daysAlive} dias",
                                    requiredValue = "${next.minDaysAlive} dias",
                                    isMet = evolutionProgress.isDaysRequirementMet,
                                    progress = if (next.minDaysAlive > 0) (evolutionProgress.daysAlive.toFloat() / next.minDaysAlive).coerceIn(0f, 1f) else 1f
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Requirement 2: Bond Level
                                EvolutionRequirementRow(
                                    title = "Nível Mínimo de Vínculo",
                                    currentValue = "Nv. ${evolutionProgress.currentLevel}",
                                    requiredValue = "Nv. ${next.minLevel}",
                                    isMet = evolutionProgress.isLevelRequirementMet,
                                    progress = if (next.minLevel > 0) (evolutionProgress.currentLevel.toFloat() / next.minLevel).coerceIn(0f, 1f) else 1f
                                )
                            } else {
                                StatRow(label = "Próxima Fase", value = "Fase Máxima (Ancião Sábio)")
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFEF3C7),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "👑", fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Companheiro Lendário & Sábio",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF92400E)
                                            )
                                            Text(
                                                text = "Seu pet atingiu a sabedoria máxima da fase Idoso e continuará vivendo feliz com você indefinidamente!",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFB45309)
                                            )
                                        }
                                    }
                                }
                            }

                            // Historical Evolution Dates
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            val hasHistory = pet.hatchedTimestamp > 0L || pet.youthTimestamp > 0L || pet.adultTimestamp > 0L || pet.seniorTimestamp > 0L
                            if (hasHistory) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Marcos Históricos:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (pet.hatchedTimestamp > 0L) {
                                    StatRow(label = "🐣 Nascimento / Choco", value = dateFormat.format(Date(pet.hatchedTimestamp)))
                                }
                                if (pet.youthTimestamp > 0L) {
                                    StatRow(label = "🌱 Fase Jovem", value = dateFormat.format(Date(pet.youthTimestamp)))
                                }
                                if (pet.adultTimestamp > 0L) {
                                    StatRow(label = "⭐ Fase Adulta", value = dateFormat.format(Date(pet.adultTimestamp)))
                                }
                                if (pet.seniorTimestamp > 0L) {
                                    StatRow(label = "👑 Fase Idoso (Ancião)", value = dateFormat.format(Date(pet.seniorTimestamp)))
                                }
                            }
                        }
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

                        val daysCount = evolutionProgress?.daysAlive ?: 0
                        StatRow(label = "Dias Reais de Vida", value = "$daysCount dia(s)")
                        StatRow(label = "Data de Início", value = birthDateFormatted)
                        StatRow(label = "Refeições Servidas", value = "${stats?.timesFed ?: 0} vezes")
                        StatRow(label = "Banhos Tomados", value = "${stats?.timesBathed ?: 0} vezes")
                        StatRow(label = "Sonecas Realizadas", value = "${stats?.timesSlept ?: 0} vezes")
                        StatRow(label = "Brincadeiras e Jogos", value = "${stats?.timesPlayed ?: 0} vezes")
                        StatRow(label = "Visitas ao Médico", value = "${stats?.timesDoctor ?: 0} vezes")
                        StatRow(label = "Evoluções Concluídas", value = "${stats?.evolutionsCount ?: 0}")
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
private fun EvolutionRequirementRow(
    title: String,
    currentValue: String,
    requiredValue: String,
    isMet: Boolean,
    progress: Float
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$currentValue / $requiredValue",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isMet) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                if (isMet) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Concluído",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "Em andamento",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (isMet) Color(0xFF10B981) else Color(0xFFF59E0B),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
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

