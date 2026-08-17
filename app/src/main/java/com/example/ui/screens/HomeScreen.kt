package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.InventoryEntity
import com.example.data.local.PetEntity
import com.example.data.local.PlayerEntity
import com.example.data.model.*
import com.example.ui.components.*

@Composable
fun HomeScreen(
    pet: PetEntity,
    player: PlayerEntity?,
    inventory: List<InventoryEntity>,
    isBathing: Boolean,
    autonomousState: PetAutonomousState = PetAutonomousState(),
    notificationHighlight: String? = null,
    onInteractWithPet: () -> Unit = {},
    onWalkToPosition: (Float) -> Unit = {},
    onFeed: (ShopItem?) -> Unit,
    onBathe: () -> Unit,
    onToggleSleep: () -> Unit,
    onPlay: (ShopItem?) -> Unit,
    onDoctor: () -> Unit,
    onOpenMinigames: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenInventory: () -> Unit
) {
    var showFeedDialog by remember { mutableStateOf(false) }
    var showToyDialog by remember { mutableStateOf(false) }
    var showDoctorDialog by remember { mutableStateOf(false) }
    var activeNotificationAlert by remember(notificationHighlight) { mutableStateOf(notificationHighlight) }

    val species = Species.fromId(pet.speciesId)
    val stage = try {
        PetStage.valueOf(pet.stage)
    } catch (_: Exception) {
        PetStage.FILHOTE
    }
    val rarity = try {
        Rarity.valueOf(pet.rarity)
    } catch (_: Exception) {
        Rarity.COMUM
    }

    val configuration = LocalConfiguration.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(if (pet.isSleeping) Color(0xFF0F172A) else MaterialTheme.colorScheme.background)
            .testTag("home_screen_container")
    ) {
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || (maxWidth > maxHeight && maxWidth > 480.dp)

        if (isLandscape) {
            // Landscape Layout: Left sidebar (HUD & Stats) + Center/Right living pet stage & bottom care dock
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Stats & Info Panel
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NotificationBanner(
                        alert = activeNotificationAlert,
                        onClose = { activeNotificationAlert = null }
                    )

                    PetHeaderCard(
                        pet = pet,
                        species = species,
                        stage = stage,
                        rarity = rarity,
                        player = player,
                        modifier = Modifier.fillMaxWidth()
                    )

                    PetStatsPanel(
                        pet = pet,
                        isSleeping = pet.isSleeping,
                        isCompact = true,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }

                // Right Column: Main Stage (Pet Avatar Center + Actions Dock Bottom)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PetLivingStage(
                        pet = pet,
                        autonomousState = autonomousState,
                        isBathing = isBathing,
                        onInteractWithPet = onInteractWithPet,
                        onWalkToPosition = onWalkToPosition,
                        onOpenMinigames = onOpenMinigames,
                        petSize = 190.dp,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )

                    CareActionsDock(
                        pet = pet,
                        isCompact = true,
                        onFeedClick = { showFeedDialog = true },
                        onBathe = onBathe,
                        onToggleSleep = onToggleSleep,
                        onPlay = { showToyDialog = true },
                        onDoctorClick = { showDoctorDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            // Portrait Layout: Vertical stack with balanced padding & space
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                NotificationBanner(
                    alert = activeNotificationAlert,
                    onClose = { activeNotificationAlert = null }
                )

                PetHeaderCard(
                    pet = pet,
                    species = species,
                    stage = stage,
                    rarity = rarity,
                    player = player,
                    modifier = Modifier.fillMaxWidth()
                )

                PetLivingStage(
                    pet = pet,
                    autonomousState = autonomousState,
                    isBathing = isBathing,
                    onInteractWithPet = onInteractWithPet,
                    onWalkToPosition = onWalkToPosition,
                    onOpenMinigames = onOpenMinigames,
                    petSize = 230.dp,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )

                PetStatsPanel(
                    pet = pet,
                    isSleeping = pet.isSleeping,
                    isCompact = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                CareActionsDock(
                    pet = pet,
                    isCompact = false,
                    onFeedClick = { showFeedDialog = true },
                    onBathe = onBathe,
                    onToggleSleep = onToggleSleep,
                    onPlay = { showToyDialog = true },
                    onDoctorClick = { showDoctorDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Dialogs
        if (showFeedDialog) {
            FeedDialog(
                inventory = inventory,
                onDismiss = { showFeedDialog = false },
                onFeedItem = { item ->
                    onFeed(item)
                }
            )
        }

        if (showToyDialog) {
            ToySelectionDialog(
                inventory = inventory,
                onDismiss = { showToyDialog = false },
                onPlayToy = { toy ->
                    onPlay(toy)
                },
                onOpenMinigames = onOpenMinigames
            )
        }

        if (showDoctorDialog) {
            DoctorCheckupDialog(
                currentHealth = pet.health,
                onDismiss = { showDoctorDialog = false },
                onPerformTreatment = onDoctor
            )
        }
    }
}

@Composable
private fun NotificationBanner(
    alert: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = alert != null,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (alert) {
                    "HEALTH" -> Color(0xFFFEE2E2)
                    "HUNGER" -> Color(0xFFFFEDD5)
                    "HYGIENE" -> Color(0xFFE0F2FE)
                    "ENERGY" -> Color(0xFFEDE9FE)
                    else -> Color(0xFFFCE7F3)
                }
            ),
            modifier = modifier.fillMaxWidth().padding(bottom = 6.dp).testTag("notification_alert_banner")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when (alert) {
                            "HUNGER" -> "🍎"
                            "HYGIENE" -> "🛁"
                            "ENERGY" -> "😴"
                            "HEALTH" -> "🏥"
                            else -> "❤️"
                        },
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (alert) {
                            "HUNGER" -> "Hora do lanche! Bichinho com fome."
                            "HYGIENE" -> "Hora do banho! Deixe seu bichinho limpo."
                            "ENERGY" -> "Seu bichinho precisa descansar."
                            "HEALTH" -> "Atenção: Cuide da saúde no médico!"
                            else -> "Seu bichinho estava com saudades!"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun PetHeaderCard(
    pet: PetEntity,
    species: Species,
    stage: PetStage,
    rarity: Rarity,
    player: PlayerEntity?,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (pet.isSleeping) Color(0xFF1E293B) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(3.dp),
        modifier = modifier.testTag("pet_header_card")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = pet.name.ifBlank { species.displayName },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (pet.isSleeping) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Nv. ${pet.level}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${species.displayName} • ${stage.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (pet.isSleeping) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RarityBadge(rarity)
                    Spacer(modifier = Modifier.width(6.dp))
                    CoinBadge(player?.coins ?: 0)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // XP Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "XP",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                LinearProgressIndicator(
                    progress = { (pet.exp % 100) / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${pet.exp % 100}/100",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (pet.isSleeping) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PetLivingStage(
    pet: PetEntity,
    autonomousState: PetAutonomousState,
    isBathing: Boolean,
    onInteractWithPet: () -> Unit,
    onWalkToPosition: (Float) -> Unit,
    onOpenMinigames: () -> Unit,
    petSize: Dp,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val normalizedX = ((offset.x / size.width) - 0.5f) * 200f
                    onWalkToPosition(normalizedX)
                }
            }
            .testTag("interactive_room_area"),
        contentAlignment = Alignment.Center
    ) {
        // Thematic Room Scenery Layer (Rendered inside the living stage viewport)
        androidx.compose.animation.Crossfade(
            targetState = pet.roomTheme,
            animationSpec = tween(600, easing = FastOutSlowInEasing),
            label = "room_scenery_crossfade"
        ) { theme ->
            RoomSceneryRenderer(
                themeId = theme,
                isSleeping = pet.isSleeping,
                modifier = Modifier.fillMaxSize()
            )
        }

        val roomMaxOffsetX = (maxWidth.value / 2f) - 90f
        val clampedWalkX = autonomousState.walkOffsetX.coerceIn(-roomMaxOffsetX, roomMaxOffsetX).dp

        val isSleeping = pet.isSleeping || autonomousState.behaviorState == PetBehaviorState.DORMINDO
        val isBedroom = pet.roomTheme.isEmpty() || pet.roomTheme == "decor_bedroom"

        // -----------------------------------------------------------------------------------------
        // EXACT ROOM GEOMETRY & BED COORDINATE REFERENCES
        // -----------------------------------------------------------------------------------------
        val roomFloorY = maxHeight * 0.70f
        val rugSurfaceY = roomFloorY + 2.dp

        // Bed coordinates (strictly matching RoomSceneryRenderer geometry)
        val bedW = (maxWidth * 0.38f).coerceIn(135.dp, 230.dp)
        val bedH = (maxHeight * 0.30f).coerceIn(85.dp, 140.dp)
        val bedX = maxWidth * 0.02f
        val bedY = roomFloorY - bedH * 0.72f
        val headboardW = bedW * 0.20f
        val mattressX = bedX + headboardW * 0.5f
        val mattressW = bedW - (headboardW * 0.5f)
        val mattressY = bedY + bedH * 0.18f
        val mattressH = bedH * 0.48f

        // Mattress & Pillow Snuggle Center for Sleeping Pet
        val bedCenterX = mattressX + mattressW * 0.44f
        val bedCenterY = mattressY + mattressH * 0.36f

        // Awake Position (Centered on living room rug with walking displacement)
        val awakeX = (maxWidth / 2f) - (petSize / 2f) + clampedWalkX
        val awakeY = rugSurfaceY - (petSize * 0.70f)

        // Sleeping Position (Directly nestled on the bed mattress and pillow)
        val sleepX = if (isBedroom) {
            bedCenterX - (petSize / 2f)
        } else {
            (maxWidth / 2f) - (petSize / 2f)
        }
        val sleepY = if (isBedroom) {
            bedCenterY - (petSize / 2f)
        } else {
            rugSurfaceY - (petSize * 0.70f)
        }

        // Smooth transition to the bed during sleep, and back to center rug upon waking
        val targetPetX = if (isSleeping) sleepX else awakeX
        val targetPetY = if (isSleeping) sleepY else awakeY

        val animatedPetX by animateDpAsState(
            targetValue = targetPetX,
            animationSpec = tween(
                durationMillis = if (autonomousState.behaviorState == PetBehaviorState.ACORDANDO) 1400 else 1100,
                easing = FastOutSlowInEasing
            ),
            label = "pet_sleep_walk_x"
        )
        val animatedPetY by animateDpAsState(
            targetValue = targetPetY,
            animationSpec = tween(
                durationMillis = if (autonomousState.behaviorState == PetBehaviorState.ACORDANDO) 1200 else 900,
                easing = FastOutSlowInEasing
            ),
            label = "pet_sleep_walk_y"
        )

        // Top State / Mood Pill
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (pet.isSleeping) Color(0xFF334155).copy(alpha = 0.9f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
                .testTag("pet_state_badge")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = autonomousState.behaviorState.defaultIcon,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = autonomousState.behaviorState.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (pet.isSleeping) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Dynamic Speech & Thought Bubble (dynamically centered over the pet)
        val bubbleX = (animatedPetX + (petSize / 2f) - 105.dp).coerceIn(8.dp, (maxWidth - 218.dp).coerceAtLeast(8.dp))
        val bubbleY = (animatedPetY - 50.dp).coerceAtLeast(6.dp)

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = bubbleX, y = bubbleY)
        ) {
            AnimatedVisibility(
                visible = autonomousState.speechBubbleVisible && autonomousState.currentSpeechText.isNotBlank(),
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (pet.isSleeping) Color(0xFF334155) else Color.White,
                        shadowElevation = 6.dp,
                        modifier = Modifier.widthIn(max = 210.dp).testTag("thought_bubble")
                    ) {
                        Text(
                            text = autonomousState.currentSpeechText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (pet.isSleeping) Color.White else Color(0xFF1E293B),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (pet.isSleeping) Color(0xFF334155) else Color.White,
                        modifier = Modifier
                            .size(7.dp)
                            .offset(y = (-2).dp)
                    ) {}
                }
            }
        }

        // Living Pet Avatar
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = animatedPetX, y = animatedPetY)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onInteractWithPet()
                }
                .testTag("pet_main_avatar")
        ) {
            PetCanvasRenderer(
                pet = pet,
                size = petSize,
                behaviorState = autonomousState.behaviorState,
                walkOffsetX = autonomousState.walkOffsetX,
                walkDirection = autonomousState.walkDirection,
                blinkProgress = autonomousState.blinkProgress,
                lookGazeX = autonomousState.lookGazeX,
                lookGazeY = autonomousState.lookGazeY,
                jumpProgress = autonomousState.jumpProgress,
                isSquishing = autonomousState.isSquishing,
                isInteracting = autonomousState.isSquishing,
                showBubbles = isBathing
            )
        }

        // Shortcut Banner for minigames when pet is bored / awake
        if (!pet.isSleeping && pet.happiness < 50) {
            Button(
                onClick = onOpenMinigames,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 4.dp)
                    .testTag("play_minigame_shortcut")
            ) {
                Text(text = "🎮 Jogar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PetStatsPanel(
    pet: PetEntity,
    isSleeping: Boolean,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSleeping) Color(0xFF1E293B) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier.testTag("stats_panel")
    ) {
        Column(
            modifier = Modifier
                .padding(if (isCompact) 8.dp else 12.dp)
                .fillMaxWidth(),
            verticalArrangement = if (isCompact) Arrangement.SpaceEvenly else Arrangement.spacedBy(4.dp)
        ) {
            if (isCompact) {
                // Compact list layout for landscape sidebar
                CompactStatRow("Fome", pet.hunger, Icons.Rounded.Restaurant, Color(0xFFF97316), isSleeping)
                CompactStatRow("Energia", pet.energy, Icons.Rounded.Bolt, Color(0xFFEAB308), isSleeping)
                CompactStatRow("Felicidade", pet.happiness, Icons.Rounded.SentimentSatisfiedAlt, Color(0xFFEC4899), isSleeping)
                CompactStatRow("Higiene", pet.hygiene, Icons.Rounded.Bathtub, Color(0xFF06B6D4), isSleeping)
                CompactStatRow("Saúde", pet.health, Icons.Rounded.Favorite, Color(0xFFEF4444), isSleeping)
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatBar(
                        label = "Fome",
                        value = pet.hunger,
                        icon = Icons.Rounded.Restaurant,
                        modifier = Modifier.weight(1f)
                    )
                    StatBar(
                        label = "Energia",
                        value = pet.energy,
                        icon = Icons.Rounded.Bolt,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatBar(
                        label = "Felicidade",
                        value = pet.happiness,
                        icon = Icons.Rounded.SentimentSatisfiedAlt,
                        modifier = Modifier.weight(1f)
                    )
                    StatBar(
                        label = "Higiene",
                        value = pet.hygiene,
                        icon = Icons.Rounded.Bathtub,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatBar(
                        label = "Saúde",
                        value = pet.health,
                        icon = Icons.Rounded.Favorite,
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CompactStatRow(
    label: String,
    value: Int,
    icon: ImageVector,
    color: Color,
    isSleeping: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSleeping) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(62.dp)
        )
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$value%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSleeping) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun CareActionsDock(
    pet: PetEntity,
    isCompact: Boolean,
    onFeedClick: () -> Unit,
    onBathe: () -> Unit,
    onToggleSleep: () -> Unit,
    onPlay: () -> Unit,
    onDoctorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (pet.isSleeping) Color(0xFF1E293B) else MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 4.dp,
        modifier = modifier.testTag("care_actions_dock")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = if (isCompact) 6.dp else 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CareActionButton(
                icon = Icons.Rounded.Restaurant,
                label = "Alimentar",
                color = Color(0xFFF97316),
                enabled = !pet.isSleeping,
                isCompact = isCompact,
                onClick = onFeedClick,
                testTag = "action_feed"
            )

            CareActionButton(
                icon = Icons.Rounded.Bathtub,
                label = "Banho",
                color = Color(0xFF06B6D4),
                enabled = !pet.isSleeping,
                isCompact = isCompact,
                onClick = onBathe,
                testTag = "action_bath"
            )

            val isNight = com.example.notification.PetStatsCalculator.isNightTime()
            CareActionButton(
                icon = if (pet.isSleeping) (if (isNight) Icons.Rounded.NightsStay else Icons.Rounded.Lightbulb) else Icons.Rounded.NightsStay,
                label = if (isNight && pet.isSleeping) "Sono (22h-8h)" else if (pet.isSleeping) "Acordar" else "Dormir",
                color = Color(0xFF8B5CF6),
                enabled = !isNight,
                isCompact = isCompact,
                onClick = onToggleSleep,
                testTag = "action_sleep"
            )

            CareActionButton(
                icon = Icons.Rounded.SportsEsports,
                label = "Brincar",
                color = Color(0xFFEC4899),
                enabled = !pet.isSleeping,
                isCompact = isCompact,
                onClick = onPlay,
                testTag = "action_play"
            )

            CareActionButton(
                icon = Icons.Rounded.LocalHospital,
                label = "Médico",
                color = Color(0xFFEF4444),
                enabled = !pet.isSleeping,
                isCompact = isCompact,
                onClick = onDoctorClick,
                testTag = "action_doctor"
            )
        }
    }
}

@Composable
private fun CareActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    enabled: Boolean,
    isCompact: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag(testTag)
    ) {
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = color,
                disabledContainerColor = Color(0xFF64748B)
            ),
            modifier = Modifier.size(if (isCompact) 42.dp else 52.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(if (isCompact) 22.dp else 26.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = if (isCompact) 10.sp else 11.sp,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray
        )
    }
}
