package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.EvolutionDialog
import com.example.ui.screens.*
import com.example.ui.viewmodel.PetViewModel

sealed class Screen(val route: String, val title: String, val icon: @Composable (Boolean) -> Unit) {
    data object Home : Screen("home", "Bichinho", { isSelected ->
        Icon(if (isSelected) Icons.Filled.Pets else Icons.Outlined.Pets, contentDescription = "Bichinho")
    })
    data object Minigames : Screen("minigames", "Jogos", { isSelected ->
        Icon(if (isSelected) Icons.Filled.SportsEsports else Icons.Outlined.SportsEsports, contentDescription = "Jogos")
    })
    data object Shop : Screen("shop", "Loja", { isSelected ->
        Icon(if (isSelected) Icons.Filled.ShoppingBag else Icons.Outlined.ShoppingBag, contentDescription = "Loja")
    })
    data object Inventory : Screen("inventory", "Mochila", { isSelected ->
        Icon(if (isSelected) Icons.Filled.Backpack else Icons.Outlined.Backpack, contentDescription = "Mochila")
    })
    data object Missions : Screen("missions", "Missões", { isSelected ->
        Icon(if (isSelected) Icons.Filled.AssignmentTurnedIn else Icons.Outlined.AssignmentTurnedIn, contentDescription = "Missões")
    })
    data object Achievements : Screen("achievements", "Troféus", { isSelected ->
        Icon(if (isSelected) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents, contentDescription = "Troféus")
    })
    data object Stats : Screen("stats", "Perfil", { isSelected ->
        Icon(if (isSelected) Icons.Filled.BarChart else Icons.Outlined.BarChart, contentDescription = "Perfil")
    })
    data object Settings : Screen("settings", "Ajustes", { isSelected ->
        Icon(if (isSelected) Icons.Filled.Settings else Icons.Outlined.Settings, contentDescription = "Ajustes")
    })

    // Minigame dedicated subroutes
    data object MemoryGame : Screen("game_memory", "Memória", {})
    data object RunnerGame : Screen("game_runner", "Corrida", {})
    data object CatchGame : Screen("game_catch", "Captura", {})
}

@Composable
fun MainApp(
    notificationType: String? = null,
    viewModel: PetViewModel = viewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    val pet by viewModel.petState.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()
    val inventory by viewModel.inventoryState.collectAsStateWithLifecycle()
    val missions by viewModel.dailyMissionsState.collectAsStateWithLifecycle()
    val achievements by viewModel.achievementsState.collectAsStateWithLifecycle()
    val stats by viewModel.gameStatsState.collectAsStateWithLifecycle()
    val isBathing by viewModel.isBathingAnimation.collectAsStateWithLifecycle()
    val autonomousState by viewModel.autonomousState.collectAsStateWithLifecycle()

    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val hungerNotifications by viewModel.hungerNotifications.collectAsStateWithLifecycle()
    val hygieneNotifications by viewModel.hygieneNotifications.collectAsStateWithLifecycle()
    val energyNotifications by viewModel.energyNotifications.collectAsStateWithLifecycle()
    val healthNotifications by viewModel.healthNotifications.collectAsStateWithLifecycle()
    val longingNotifications by viewModel.longingNotifications.collectAsStateWithLifecycle()

    val isMusicEnabled by viewModel.isMusicEnabled.collectAsStateWithLifecycle()
    val isSfxEnabled by viewModel.isSfxEnabled.collectAsStateWithLifecycle()
    val musicVolume by viewModel.musicVolume.collectAsStateWithLifecycle()
    val sfxVolume by viewModel.sfxVolume.collectAsStateWithLifecycle()

    val toastMsg by viewModel.toastMessage.collectAsStateWithLifecycle()
    val evolutionStage by viewModel.evolutionCelebration.collectAsStateWithLifecycle()

    val audioManager = remember { com.example.audio.GameAudioManager.getInstance(context) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Screen-based BGM management
    LaunchedEffect(currentRoute, pet?.isHatched) {
        when {
            currentRoute in listOf(Screen.MemoryGame.route, Screen.RunnerGame.route, Screen.CatchGame.route) -> {
                audioManager.playBgm(com.example.audio.BgmTrack.MINIGAME)
            }
            currentRoute == Screen.Shop.route -> {
                audioManager.playBgm(com.example.audio.BgmTrack.SHOP)
            }
            pet != null && !pet!!.isHatched && currentRoute == Screen.Home.route -> {
                audioManager.playBgm(com.example.audio.BgmTrack.INCUBATOR)
            }
            else -> {
                audioManager.playBgm(com.example.audio.BgmTrack.HOME)
            }
        }
    }

    // Trigger audio fanfare on evolution
    LaunchedEffect(evolutionStage) {
        if (evolutionStage != null) {
            audioManager.playEvolutionSequence()
        }
    }

    // Show Android toast when message is emitted
    LaunchedEffect(toastMsg) {
        toastMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Minigames,
        Screen.Shop,
        Screen.Inventory,
        Screen.Missions,
        Screen.Achievements,
        Screen.Stats,
        Screen.Settings
    )

    // Hide bottom bar when inside a full-screen minigame
    val isPlayingGame = currentRoute in listOf(
        Screen.MemoryGame.route,
        Screen.RunnerGame.route,
        Screen.CatchGame.route
    )

    Scaffold(
        bottomBar = {
            if (!isPlayingGame) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar"),
                    tonalElevation = 4.dp
                ) {
                    val visibleItems = listOf(
                        Screen.Home,
                        Screen.Minigames,
                        Screen.Shop,
                        Screen.Inventory,
                        Screen.Missions,
                        Screen.Stats,
                        Screen.Settings
                    )
                    visibleItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = { screen.icon(isSelected) },
                            label = {
                                Text(
                                    text = screen.title,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            selected = isSelected,
                            alwaysShowLabel = false,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    viewModel.playUiClick()
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val currentPet = pet
                if (currentPet != null && !currentPet.isHatched) {
                    EggHatchingScreen(
                        pet = currentPet,
                        onWarmEgg = { viewModel.warmEgg() },
                        onHatchEgg = { name, species -> viewModel.hatchEgg(name, species) }
                    )
                } else if (currentPet != null) {
                    HomeScreen(
                        pet = currentPet,
                        player = player,
                        inventory = inventory,
                        isBathing = isBathing,
                        autonomousState = autonomousState,
                        notificationHighlight = notificationType,
                        onInteractWithPet = { viewModel.interactWithPet() },
                        onWalkToPosition = { targetX -> viewModel.walkToPosition(targetX) },
                        onFeed = { item -> viewModel.feed(item) },
                        onBathe = { viewModel.bathe() },
                        onToggleSleep = { viewModel.toggleSleep() },
                        onPlay = { item -> viewModel.playWithToy(item) },
                        onDoctor = { payWithCoins -> viewModel.doctorCheckup(payWithCoins) },
                        onOpenMinigames = { navController.navigate(Screen.Minigames.route) },
                        onOpenShop = { navController.navigate(Screen.Shop.route) },
                        onOpenInventory = { navController.navigate(Screen.Inventory.route) }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            composable(Screen.Minigames.route) {
                MinigamesHubScreen(
                    stats = stats,
                    onSelectGame = { gameType ->
                        when (gameType) {
                            "memory" -> navController.navigate(Screen.MemoryGame.route)
                            "runner" -> navController.navigate(Screen.RunnerGame.route)
                            "catch" -> navController.navigate(Screen.CatchGame.route)
                        }
                    }
                )
            }

            composable(Screen.Shop.route) {
                ShopScreen(
                    userCoins = player?.coins ?: 0,
                    pet = pet,
                    inventory = inventory,
                    onBuyItem = { item, equipImmediately ->
                        viewModel.buyShopItem(item, equipImmediately = equipImmediately)
                    },
                    onEquipItem = { item ->
                        viewModel.equipShopItem(item)
                    }
                )
            }

            composable(Screen.Inventory.route) {
                InventoryScreen(
                    inventory = inventory,
                    pet = pet,
                    onUseItem = { item -> viewModel.useItem(item) },
                    onEquipItem = { item -> viewModel.equipInventoryItem(item) }
                )
            }

            composable(Screen.Missions.route) {
                MissionsScreen(
                    missions = missions,
                    onClaimReward = { id -> viewModel.claimMission(id) }
                )
            }

            composable(Screen.Achievements.route) {
                AchievementsScreen(
                    achievements = achievements,
                    onClaimAchievement = { id -> viewModel.claimAchievement(id) }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen(
                    pet = pet,
                    player = player,
                    stats = stats
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    pet = pet,
                    notificationsEnabled = notificationsEnabled,
                    hungerNotifications = hungerNotifications,
                    hygieneNotifications = hygieneNotifications,
                    energyNotifications = energyNotifications,
                    healthNotifications = healthNotifications,
                    longingNotifications = longingNotifications,
                    isMusicEnabled = isMusicEnabled,
                    isSfxEnabled = isSfxEnabled,
                    musicVolume = musicVolume,
                    sfxVolume = sfxVolume,
                    onToggleNotifications = { enabled -> viewModel.setNotificationsEnabled(enabled) },
                    onToggleHunger = { enabled -> viewModel.setHungerNotifications(enabled) },
                    onToggleHygiene = { enabled -> viewModel.setHygieneNotifications(enabled) },
                    onToggleEnergy = { enabled -> viewModel.setEnergyNotifications(enabled) },
                    onToggleHealth = { enabled -> viewModel.setHealthNotifications(enabled) },
                    onToggleLonging = { enabled -> viewModel.setLongingNotifications(enabled) },
                    onToggleMusic = { enabled -> viewModel.setMusicEnabled(enabled) },
                    onToggleSfx = { enabled -> viewModel.setSfxEnabled(enabled) },
                    onChangeMusicVolume = { vol -> viewModel.setMusicVolume(vol) },
                    onChangeSfxVolume = { vol -> viewModel.setSfxVolume(vol) },
                    onTestSfx = { viewModel.playSfx(com.example.audio.SoundEffect.PET_HAPPY) },
                    onSendTestNotification = { viewModel.sendTestNotification() },
                    onTriggerImmediateCheck = { viewModel.triggerImmediateNeedsCheck() },
                    onRenamePet = { newName -> viewModel.renamePet(newName) },
                    onResetPet = {
                        viewModel.resetPet()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            // Minigame Fullscreens
            composable(Screen.MemoryGame.route) {
                MemoryMinigameScreen(
                    onBack = { navController.popBackStack() },
                    onFinishGame = { score, coins ->
                        viewModel.recordMinigameScore("memory", score, coins)
                    }
                )
            }

            composable(Screen.RunnerGame.route) {
                RunnerMinigameScreen(
                    onBack = { navController.popBackStack() },
                    onFinishGame = { score, coins ->
                        viewModel.recordMinigameScore("runner", score, coins)
                    }
                )
            }

            composable(Screen.CatchGame.route) {
                CatchMinigameScreen(
                    onBack = { navController.popBackStack() },
                    onFinishGame = { score, coins ->
                        viewModel.recordMinigameScore("catch", score, coins)
                    }
                )
            }
        }
    }

    // Evolution Celebration Dialog
    evolutionStage?.let { stage ->
        EvolutionDialog(
            newStage = stage,
            petName = pet?.name ?: "Bichinho",
            onDismiss = { viewModel.dismissEvolution() }
        )
    }
}
