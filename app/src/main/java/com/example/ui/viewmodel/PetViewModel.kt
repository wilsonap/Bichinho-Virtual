package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GameAudioManager
import com.example.audio.SoundEffect
import com.example.data.local.*
import com.example.data.model.*
import com.example.notification.NotificationHelper
import com.example.notification.NotificationPreferences
import com.example.notification.PetCareScheduler
import com.example.notification.PetNotificationType
import com.example.notification.PetStatsCalculator
import com.example.data.repository.PetRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

class PetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PetRepository
    private val notificationPrefs: NotificationPreferences = NotificationPreferences(application)
    private val audioManager: GameAudioManager = GameAudioManager.getInstance(application)

    val petState: StateFlow<PetEntity?>
    val playerState: StateFlow<PlayerEntity?>
    val inventoryState: StateFlow<List<InventoryEntity>>
    val dailyMissionsState: StateFlow<List<DailyMissionEntity>>
    val achievementsState: StateFlow<List<AchievementEntity>>
    val gameStatsState: StateFlow<GameStatsEntity?>

    // Audio settings states
    val isMusicEnabled: StateFlow<Boolean> = audioManager.isMusicEnabled
    val isSfxEnabled: StateFlow<Boolean> = audioManager.isSfxEnabled
    val musicVolume: StateFlow<Float> = audioManager.musicVolume
    val sfxVolume: StateFlow<Float> = audioManager.sfxVolume

    // Notification settings states
    private val _notificationsEnabled = MutableStateFlow(notificationPrefs.isNotificationsEnabled)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _hungerNotifications = MutableStateFlow(notificationPrefs.isHungerEnabled)
    val hungerNotifications: StateFlow<Boolean> = _hungerNotifications.asStateFlow()

    private val _hygieneNotifications = MutableStateFlow(notificationPrefs.isHygieneEnabled)
    val hygieneNotifications: StateFlow<Boolean> = _hygieneNotifications.asStateFlow()

    private val _energyNotifications = MutableStateFlow(notificationPrefs.isEnergyEnabled)
    val energyNotifications: StateFlow<Boolean> = _energyNotifications.asStateFlow()

    private val _healthNotifications = MutableStateFlow(notificationPrefs.isHealthEnabled)
    val healthNotifications: StateFlow<Boolean> = _healthNotifications.asStateFlow()

    private val _longingNotifications = MutableStateFlow(notificationPrefs.isLongingEnabled)
    val longingNotifications: StateFlow<Boolean> = _longingNotifications.asStateFlow()

    private val _isBathingAnimation = MutableStateFlow(false)
    val isBathingAnimation: StateFlow<Boolean> = _isBathingAnimation.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _evolutionCelebration = MutableStateFlow<PetStage?>(null)
    val evolutionCelebration: StateFlow<PetStage?> = _evolutionCelebration.asStateFlow()

    // Autonomous behavior state for lifelike pet animation & state machine
    private val _autonomousState = MutableStateFlow(PetAutonomousState())
    val autonomousState: StateFlow<PetAutonomousState> = _autonomousState.asStateFlow()

    private var previousStage: PetStage? = null
    private var stateDurationSeconds = 0

    init {
        NotificationHelper.createNotificationChannels(application)
        notificationPrefs.onAppOpened()

        val db = AppDatabase.getDatabase(application)
        repository = PetRepository(db.petDao())

        petState = repository.petFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )
        playerState = repository.playerFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )
        inventoryState = repository.inventoryFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        dailyMissionsState = repository.dailyMissionsFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        achievementsState = repository.achievementsFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        gameStatsState = repository.gameStatsFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

        viewModelScope.launch {
            repository.initializeGameIfNeeded()
            val pet = repository.getPet()
            if (pet != null && pet.isHatched) {
                val wasLonging = repository.updateOfflineStats(pet)
                if (wasLonging) {
                    triggerLongingGreeting(pet)
                }
                PetCareScheduler.scheduleNextCheck(application)
            }
        }

        // Monitor evolution changes to trigger celebration modal
        viewModelScope.launch {
            petState.collect { pet ->
                if (pet != null && pet.isHatched) {
                    val currentStage = try {
                        PetStage.valueOf(pet.stage)
                    } catch (_: Exception) {
                        null
                    }
                    if (previousStage != null && currentStage != null && currentStage != previousStage && currentStage != PetStage.OVO) {
                        _evolutionCelebration.value = currentStage
                    }
                    previousStage = currentStage
                }
            }
        }

        // Monitor sleep transitions to handle automatic and manual awakening smoothly
        var previousIsSleeping: Boolean? = null
        viewModelScope.launch {
            petState.collect { pet ->
                if (pet != null && pet.isHatched) {
                    if (previousIsSleeping == true && !pet.isSleeping) {
                        onPetAwakened(isAutomatic = true)
                    }
                    previousIsSleeping = pet.isSleeping
                }
            }
        }

        // Start continuous state machine & animation loop
        startAutonomousLoop()
        startPeriodicLiveStatTick()
    }

    private fun triggerLongingGreeting(pet: PetEntity) {
        val species = Species.fromId(pet.speciesId)
        _autonomousState.update {
            it.copy(
                behaviorState = PetBehaviorState.COM_SAUDADE,
                currentSpeechText = "Que saudade! Você voltou! ${species.soundLabel} ❤️",
                speechBubbleVisible = true,
                isLongingGreeting = true
            )
        }
    }

    private fun startAutonomousLoop() {
        // High frequency loop (100ms) for smooth locomotion, blinking & jumps
        viewModelScope.launch {
            var blinkTimer = 0
            var isBlinkingNow = false
            var jumpTimer = 0
            var isJumpingNow = false

            while (true) {
                delay(100)
                val currentPet = petState.value

                if (currentPet == null || !currentPet.isHatched) {
                    continue
                }

                _autonomousState.update { current ->
                    // 1. Locomotion / Walking physics
                    var newOffsetX = current.walkOffsetX
                    var newDirection = current.walkDirection

                    if (currentPet.isSleeping || current.behaviorState == PetBehaviorState.DORMINDO) {
                        newOffsetX = 0f
                        newDirection = 1f
                    } else if (current.behaviorState == PetBehaviorState.CAMINHANDO) {
                        val speed = 3.5f * (if (currentPet.energy > 70) 1.2f else if (currentPet.energy < 30) 0.6f else 1.0f)
                        val diff = current.targetOffsetX - current.walkOffsetX
                        if (abs(diff) > speed) {
                            newDirection = if (diff > 0) 1f else -1f
                            newOffsetX += newDirection * speed
                        } else {
                            newOffsetX = current.targetOffsetX
                        }
                    }

                    // 2. Natural Eye Blinking
                    blinkTimer++
                    var blinkProgress = current.blinkProgress
                    if (!currentPet.isSleeping && current.behaviorState != PetBehaviorState.DORMINDO) {
                        if (!isBlinkingNow && blinkTimer > Random.nextInt(25, 55)) {
                            isBlinkingNow = true
                            blinkTimer = 0
                        }
                        if (isBlinkingNow) {
                            blinkProgress += 0.35f
                            if (blinkProgress >= 1f) {
                                blinkProgress = 0f
                                isBlinkingNow = false
                            }
                        }
                    } else {
                        blinkProgress = 1f
                    }

                    // 3. Jump Physics (Only during explicit jumping / playful leaping states)
                    var jumpProgress = current.jumpProgress
                    if (current.behaviorState == PetBehaviorState.PULANDO || current.behaviorState == PetBehaviorState.BRINCANDO) {
                        jumpTimer++
                        val t = (jumpTimer % 14) / 14f
                        // Parabolic jump arc: 4 * t * (1 - t)
                        jumpProgress = (4f * t * (1f - t)).coerceIn(0f, 1f)
                    } else {
                        jumpProgress = 0f
                        jumpTimer = 0
                    }

                    current.copy(
                        walkOffsetX = newOffsetX,
                        walkDirection = newDirection,
                        blinkProgress = blinkProgress,
                        isBlinking = isBlinkingNow,
                        jumpProgress = jumpProgress
                    )
                }
            }
        }

        // Behavior decision engine (every 2.5 to 5 seconds)
        viewModelScope.launch {
            while (true) {
                delay(3000)
                val currentPet = petState.value ?: continue
                if (!currentPet.isHatched) continue

                // Don't interrupt if wake-up or bathing animation active
                if (_autonomousState.value.behaviorState == PetBehaviorState.ACORDANDO) {
                    continue
                }

                if (_isBathingAnimation.value) {
                    _autonomousState.update {
                        it.copy(
                            behaviorState = PetBehaviorState.TOMANDO_BANHO,
                            currentSpeechText = "Adoro bolhas de sabão! 🧼🫧",
                            speechBubbleVisible = true
                        )
                    }
                    continue
                }

                // If pet is sleeping
                if (currentPet.isSleeping) {
                    val isNight = PetStatsCalculator.isNightTime()
                    val sleepSpeech = if (isNight) {
                        "Shhh... ${currentPet.name.ifBlank { "O bichinho" }} está dormindo. 💤"
                    } else {
                        "Zzz... Sono tranquilo 💤"
                    }
                    _autonomousState.update {
                        it.copy(
                            behaviorState = PetBehaviorState.DORMINDO,
                            currentSpeechText = sleepSpeech,
                            lookGazeX = 0f,
                            lookGazeY = 0f
                        )
                    }
                    continue
                }

                // Check for night transition or daytime exhaustion sleep
                if (PetStatsCalculator.isNightTime()) {
                    repository.putPetToSleep()
                    continue
                }

                // Auto-sleep if energy is critically low during daytime
                if (currentPet.energy <= 5) {
                    toggleSleep()
                    continue
                }

                // Choose next state intelligently based on attributes
                val hunger = currentPet.hunger
                val energy = currentPet.energy
                val happiness = currentPet.happiness
                val hygiene = currentPet.hygiene
                val health = currentPet.health
                val species = Species.fromId(currentPet.speciesId)

                val nextState: PetBehaviorState
                val nextThought: String

                when {
                    health < 35 -> {
                        nextState = PetBehaviorState.DOENTE
                        nextThought = "Não estou me sentindo bem... vamos ao médico? 🩺"
                    }
                    hunger < 15 -> {
                        nextState = PetBehaviorState.PROCURANDO_COMIDA
                        nextThought = "Minha barriguinha está roncando! Preciso de comida! 🍎"
                    }
                    hunger < 35 -> {
                        nextState = listOf(PetBehaviorState.PROCURANDO_COMIDA, PetBehaviorState.OLHANDO_LADOS).random()
                        nextThought = "Hmm... onde estão os petiscos? 🍽️"
                    }
                    energy < 25 -> {
                        nextState = listOf(PetBehaviorState.BOCEJANDO, PetBehaviorState.SENTADO).random()
                        nextThought = "Uaaah... que soninho! Vamos dormir? 🥱"
                    }
                    hygiene < 30 -> {
                        nextState = PetBehaviorState.TRISTE
                        nextThought = "Estou sujinho... um banho quentinho seria perfeito! 🧼"
                    }
                    happiness < 30 -> {
                        nextState = PetBehaviorState.TRISTE
                        nextThought = "Estou me sentindo sozinho... vamos brincar? 🥺"
                    }
                    happiness > 80 && energy > 60 -> {
                        // Very happy and energetic
                        nextState = listOf(
                            PetBehaviorState.FELIZ,
                            PetBehaviorState.PULANDO,
                            PetBehaviorState.BRINCANDO,
                            PetBehaviorState.CAMINHANDO
                        ).random()
                        nextThought = listOf(
                            "Você é o melhor amigo do mundo! 💖",
                            "${species.soundLabel} Estou muito feliz!",
                            "Hoje é um dia maravilhoso! ✨",
                            "Que alegria estar com você! 🎈"
                        ).random()
                    }
                    else -> {
                        // Normal active day cycle
                        nextState = listOf(
                            PetBehaviorState.OCIOSO,
                            PetBehaviorState.CAMINHANDO,
                            PetBehaviorState.OLHANDO_LADOS,
                            PetBehaviorState.SENTADO,
                            PetBehaviorState.PULANDO
                        ).random()
                        nextThought = listOf(
                            "${species.soundLabel} Adoro você!",
                            "O que vamos fazer agora?",
                            "Olhando ao redor... que quarto aconchegante! 🏡",
                            "Cuidar de você me faz muito bem! ✨"
                        ).random()
                    }
                }

                // Random target position for walking
                val newTargetX = if (nextState == PetBehaviorState.CAMINHANDO) {
                    Random.nextInt(-95, 95).toFloat()
                } else {
                    _autonomousState.value.walkOffsetX
                }

                // Random gaze
                val gazeX = if (nextState == PetBehaviorState.OLHANDO_LADOS) {
                    listOf(-1f, 1f, 0f).random()
                } else if (nextState == PetBehaviorState.CAMINHANDO) {
                    _autonomousState.value.walkDirection
                } else 0f

                val gazeY = if (nextState == PetBehaviorState.PROCURANDO_COMIDA) 1f else 0f

                _autonomousState.update {
                    it.copy(
                        behaviorState = nextState,
                        targetOffsetX = newTargetX,
                        lookGazeX = gazeX,
                        lookGazeY = gazeY,
                        currentSpeechText = nextThought,
                        speechBubbleVisible = true
                    )
                }
            }
        }
    }

    private fun startPeriodicLiveStatTick() {
        viewModelScope.launch {
            while (true) {
                val pet = repository.getPet()
                val tickDelay = if (pet?.isSleeping == true) 10000L else 20000L
                delay(tickDelay)
                if (pet != null && pet.isHatched) {
                    repository.tickLiveStats()
                }
            }
        }
    }

    fun interactWithPet() {
        val pet = petState.value ?: return
        val species = Species.fromId(pet.speciesId)

        viewModelScope.launch {
            if (pet.isSleeping) {
                audioManager.playSfx(SoundEffect.SLEEP)
                val isNight = PetStatsCalculator.isNightTime()
                val sleepText = if (isNight) {
                    "Shhh... ${pet.name.ifBlank { "O bichinho" }} está dormindo. 💤"
                } else {
                    "Zzz... Recuperando energia... 💤"
                }
                _autonomousState.update {
                    it.copy(
                        currentSpeechText = sleepText,
                        speechBubbleVisible = true
                    )
                }
            } else {
                audioManager.playSfx(SoundEffect.TAP)
                if (pet.health < 40) {
                    audioManager.playSfx(SoundEffect.PET_SICK)
                } else if (pet.happiness < 40) {
                    audioManager.playSfx(SoundEffect.PET_SAD)
                } else {
                    audioManager.playSfx(SoundEffect.PET_HAPPY)
                }

                // Happy squish & love burst
                _autonomousState.update {
                    it.copy(
                        behaviorState = PetBehaviorState.FELIZ,
                        isSquishing = true,
                        currentSpeechText = listOf(
                            "Hihihi, que cócegas boas! 🥰",
                            "${species.soundLabel} Muito carinho!",
                            "Eu amo você! ❤️✨",
                            "Você é tão carinhoso comigo! 💖"
                        ).random(),
                        speechBubbleVisible = true
                    )
                }
                delay(300)
                _autonomousState.update { it.copy(isSquishing = false) }
            }
        }
    }

    fun walkToPosition(targetX: Float) {
        val pet = petState.value ?: return
        if (pet.isSleeping) return

        val clampedTarget = targetX.coerceIn(-100f, 100f)
        _autonomousState.update {
            it.copy(
                behaviorState = PetBehaviorState.CAMINHANDO,
                targetOffsetX = clampedTarget,
                walkDirection = if (clampedTarget >= it.walkOffsetX) 1f else -1f,
                speechBubbleVisible = false
            )
        }
    }

    fun warmEgg(amount: Int = 10) {
        audioManager.playSfx(SoundEffect.TAP)
        viewModelScope.launch {
            repository.warmEgg(amount)
        }
    }

    fun setPetName(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.setPetName(name)
            }
        }
    }

    fun hatchEgg(name: String, specificSpecies: Species? = null) {
        audioManager.playBirthSequence {
            viewModelScope.launch {
                try {
                    val (_, species) = repository.hatchEgg(name, specificSpecies)
                    PetCareScheduler.scheduleNextCheck(getApplication())
                    _toastMessage.value = "🎉 Nasceu um(a) ${species.displayName} (${species.rarity.displayName})!"
                } catch (e: Exception) {
                    _toastMessage.value = "Erro ao chocar ovo: ${e.message}"
                }
            }
        }
    }

    fun setPetSpeciesAndStage(species: Species, stage: PetStage) {
        viewModelScope.launch {
            repository.setPetSpeciesAndStage(species, stage)
            _toastMessage.value = "Espécie alterada para ${species.displayName} (${stage.displayName})!"
        }
    }

    fun renamePet(newName: String) {
        viewModelScope.launch {
            if (newName.isNotBlank()) {
                repository.setPetName(newName)
                _toastMessage.value = "Nome alterado para $newName com sucesso!"
            }
        }
    }

    fun feed(foodItem: ShopItem?) {
        audioManager.playSfx(SoundEffect.FEED)
        viewModelScope.launch {
            _autonomousState.update {
                it.copy(
                    behaviorState = PetBehaviorState.COMENDO,
                    currentSpeechText = if (foodItem != null) "Nham nham! Que delícia de ${foodItem.name}! 😋" else "Nham nham! Ração quentinha! 😋",
                    speechBubbleVisible = true
                )
            }
            val success = repository.feedPet(foodItem)
            if (success) {
                notificationPrefs.onPetFed()
                PetCareScheduler.scheduleNextCheck(getApplication())
                _toastMessage.value = if (foodItem != null) "Alimentou com ${foodItem.name}! 😋" else "Alimentou com ração básica! 😋"
            } else {
                _toastMessage.value = "Item não disponível no inventário."
            }
            delay(2800)
            _autonomousState.update {
                it.copy(behaviorState = PetBehaviorState.FELIZ, currentSpeechText = "Barriguinha cheia e feliz! ❤️")
            }
        }
    }

    fun bathe() {
        audioManager.playSfx(SoundEffect.BATH)
        viewModelScope.launch {
            _isBathingAnimation.value = true
            _autonomousState.update {
                it.copy(
                    behaviorState = PetBehaviorState.TOMANDO_BANHO,
                    currentSpeechText = "Banho de espuma quentinho! 🧼🫧",
                    speechBubbleVisible = true
                )
            }
            repository.bathePet()
            notificationPrefs.onPetBathed()
            PetCareScheduler.scheduleNextCheck(getApplication())
            _toastMessage.value = "Bichinho limpo e cheiroso! 🧼🫧"
            delay(2500)
            _isBathingAnimation.value = false
            _autonomousState.update {
                it.copy(behaviorState = PetBehaviorState.FELIZ, currentSpeechText = "Estou tão cheiroso e brilhando! ✨")
            }
        }
    }

    private fun onPetAwakened(isAutomatic: Boolean = false) {
        audioManager.playSfx(SoundEffect.WAKEUP)
        notificationPrefs.onPetSlept()
        PetCareScheduler.scheduleNextCheck(getApplication())

        val isNight = PetStatsCalculator.isNightTime()
        val wakeSpeech = if (isNight) {
            "Bom dia! ☀️ Dormi muito bem!"
        } else if (isAutomatic) {
            "Bom dia! Acordei 100% descansado e cheio de energia! ☀️✨"
        } else {
            "Bom dia! Acordei descansado e feliz! ☀️"
        }

        _autonomousState.update {
            it.copy(
                behaviorState = PetBehaviorState.ACORDANDO,
                currentSpeechText = wakeSpeech,
                speechBubbleVisible = true,
                walkOffsetX = 0f,
                targetOffsetX = 0f
            )
        }
        _toastMessage.value = if (isAutomatic) {
            "Energia 100%! Bichinho acordou automaticamente! ☀️"
        } else {
            "Bom dia! Bichinho acordou cheio de energia! ☀️"
        }
        viewModelScope.launch {
            delay(2500)
            _autonomousState.update {
                it.copy(
                    behaviorState = PetBehaviorState.FELIZ,
                    currentSpeechText = "Pronto para brincar e me divertir com você! 🥰",
                    speechBubbleVisible = true
                )
            }
        }
    }

    fun wakeUpPet() {
        viewModelScope.launch {
            val pet = petState.value ?: return@launch
            if (PetStatsCalculator.isNightTime()) {
                _toastMessage.value = "Shhh... Horário de descanso noturno (22h às 08h) 💤"
                return@launch
            }
            if (pet.isSleeping) {
                repository.wakeUpPet()
            }
        }
    }

    fun toggleSleep() {
        viewModelScope.launch {
            val currentPet = petState.value ?: return@launch
            if (PetStatsCalculator.isNightTime()) {
                _toastMessage.value = "Shhh... Horário de descanso noturno (22h às 08h) 💤"
                _autonomousState.update {
                    it.copy(
                        behaviorState = PetBehaviorState.DORMINDO,
                        currentSpeechText = "Shhh... ${currentPet.name.ifBlank { "O bichinho" }} está dormindo. 💤",
                        speechBubbleVisible = true,
                        walkOffsetX = 0f,
                        targetOffsetX = 0f
                    )
                }
                return@launch
            }

            if (currentPet.isSleeping) {
                repository.wakeUpPet()
            } else {
                repository.putPetToSleep()
                audioManager.playSfx(SoundEffect.SLEEP)
                notificationPrefs.onPetSlept()
                PetCareScheduler.scheduleNextCheck(getApplication())
                _autonomousState.update {
                    it.copy(
                        behaviorState = PetBehaviorState.DORMINDO,
                        currentSpeechText = "Luzes apagadas... Bons sonhos! 💤",
                        speechBubbleVisible = true,
                        walkOffsetX = 0f,
                        targetOffsetX = 0f
                    )
                }
                _toastMessage.value = "Luzes apagadas! Bichinho foi dormir... 💤"
            }
        }
    }

    fun playWithToy(toy: ShopItem? = null) {
        audioManager.playSfx(SoundEffect.PLAY)
        viewModelScope.launch {
            val speechText = when (toy?.id) {
                "toy_ball" -> "Olha a bola quicando! Pega, corre! ⚽✨"
                "toy_duck" -> "Quack quack! O patinho de borracha faz barulhinho! 🦆🎵"
                "toy_laser" -> "Vou pegar o pontinho vermelho! Rápido! 🔦⚡"
                "toy_plush" -> "Abraço bem fofinho e quentinho no ursinho! 🧸💖"
                else -> "Ebaaa! Brincadeira super divertida com ${toy?.name ?: "brinquedo"}! 🎾🎈"
            }

            _autonomousState.update {
                it.copy(
                    behaviorState = PetBehaviorState.BRINCANDO,
                    currentSpeechText = speechText,
                    speechBubbleVisible = true
                )
            }
            val success = repository.playWithPet(toy)
            if (success) {
                PetCareScheduler.scheduleNextCheck(getApplication())
                val happy = toy?.happinessBoost ?: 25
                val exp = toy?.expBoost ?: 15
                _toastMessage.value = "Brincou com ${toy?.name ?: "brinquedo"}! +$happy Felicidade, +$exp XP! 🎾"
            } else {
                _toastMessage.value = "Não foi possível brincar com este item."
            }
            delay(3000)
            _autonomousState.update {
                it.copy(behaviorState = PetBehaviorState.FELIZ, currentSpeechText = "Adorei brincar com você! 🥰")
            }
        }
    }

    fun useMedicine(medicineItem: ShopItem) {
        audioManager.playSfx(SoundEffect.LEVEL_UP)
        viewModelScope.launch {
            _autonomousState.update {
                it.copy(
                    behaviorState = PetBehaviorState.FELIZ,
                    currentSpeechText = "Tomei ${medicineItem.name} e estou me sentindo bem melhor! 💊✨",
                    speechBubbleVisible = true
                )
            }
            val success = repository.useMedicine(medicineItem)
            if (success) {
                notificationPrefs.onPetDoctorTreated()
                PetCareScheduler.scheduleNextCheck(getApplication())
                _toastMessage.value = "Usou ${medicineItem.name}! Saúde restaurada! 🩺❤️"
            } else {
                _toastMessage.value = "Item não disponível no inventário."
            }
            delay(2500)
            _autonomousState.update {
                it.copy(behaviorState = PetBehaviorState.FELIZ, currentSpeechText = "Estou 100% forte e saudável! 💪✨")
            }
        }
    }

    fun useItem(item: ShopItem) {
        when (item.category) {
            ItemCategory.ALIMENTO -> feed(item)
            ItemCategory.BRINQUEDO -> playWithToy(item)
            ItemCategory.MEDICAMENTO -> useMedicine(item)
            ItemCategory.ROUPA, ItemCategory.ACESSORIO, ItemCategory.DECORACAO -> equipShopItem(item)
        }
    }

    fun doctorCheckup() {
        audioManager.playSfx(SoundEffect.PET_HAPPY)
        viewModelScope.launch {
            repository.doctorCheckup()
            notificationPrefs.onPetDoctorTreated()
            PetCareScheduler.scheduleNextCheck(getApplication())
            _autonomousState.update {
                it.copy(
                    behaviorState = PetBehaviorState.FELIZ,
                    currentSpeechText = "Agora estou com 100% de saúde e cheio de energia! 🩺❤️",
                    speechBubbleVisible = true
                )
            }
            _toastMessage.value = "Checkup concluído! Saúde 100% restaurada! 🩺❤️"
        }
    }

    fun buyShopItem(item: ShopItem, equipImmediately: Boolean = false, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val playerCoins = playerState.value?.coins ?: 0
            if (playerCoins < item.price) {
                _toastMessage.value = "Moedas insuficientes para comprar ${item.name}."
                return@launch
            }
            val success = repository.buyItem(item, equipImmediately = equipImmediately)
            if (success) {
                audioManager.playSfx(SoundEffect.BUY)
                if (equipImmediately) {
                    _toastMessage.value = "Comprou e equipou ${item.name}! ✨"
                } else {
                    _toastMessage.value = "Comprou ${item.name} com sucesso! 🛍️"
                }
                onSuccess?.invoke()
            } else {
                _toastMessage.value = "Você já possui ${item.name} no seu inventário!"
            }
        }
    }

    fun equipShopItem(item: ShopItem) {
        audioManager.playSfx(SoundEffect.BUTTON)
        viewModelScope.launch {
            repository.equipItemById(item.id, item.category.name)
            if (item.category == ItemCategory.DECORACAO) {
                val decorSpeech = when (item.id) {
                    "decor_forest" -> "Adorei essa floresta! 🌲"
                    "decor_beach" -> "Que praia linda! 🏖️"
                    "decor_space" -> "Estou me sentindo no espaço! 🚀"
                    else -> "Que quarto aconchegante! 🏠"
                }
                _autonomousState.update {
                    it.copy(
                        behaviorState = PetBehaviorState.FELIZ,
                        currentSpeechText = decorSpeech,
                        speechBubbleVisible = true
                    )
                }
                _toastMessage.value = "Decoração alterada: ${item.name}! ✨"
            } else {
                _toastMessage.value = "Acessório atualizado no seu bichinho! ✨"
            }
        }
    }

    fun equipInventoryItem(item: InventoryEntity) {
        audioManager.playSfx(SoundEffect.BUTTON)
        viewModelScope.launch {
            repository.equipItem(item)
            if (item.category == ItemCategory.DECORACAO.name) {
                val decorSpeech = when (item.itemId) {
                    "decor_forest" -> "Adorei essa floresta! 🌲"
                    "decor_beach" -> "Que praia linda! 🏖️"
                    "decor_space" -> "Estou me sentindo no espaço! 🚀"
                    else -> "Que quarto aconchegante! 🏠"
                }
                _autonomousState.update {
                    it.copy(
                        behaviorState = PetBehaviorState.FELIZ,
                        currentSpeechText = decorSpeech,
                        speechBubbleVisible = true
                    )
                }
                _toastMessage.value = if (item.isEquipped) "Cenário padrão restaurado!" else "Cenário aplicado: ${item.name}! ✨"
            } else {
                _toastMessage.value = if (item.isEquipped) "Desequipou ${item.name}." else "Equipou ${item.name}!"
            }
        }
    }

    fun claimMission(missionId: String) {
        viewModelScope.launch {
            val success = repository.claimDailyMission(missionId)
            if (success) {
                audioManager.playSfx(SoundEffect.MISSION)
                delay(300)
                audioManager.playSfx(SoundEffect.COIN)
                _toastMessage.value = "Recompensa de missão diária resgatada! 🪙"
            }
        }
    }

    fun claimAchievement(achievementId: String) {
        viewModelScope.launch {
            val success = repository.claimAchievement(achievementId)
            if (success) {
                audioManager.playSfx(SoundEffect.ACHIEVEMENT)
                delay(400)
                audioManager.playSfx(SoundEffect.COIN)
                _toastMessage.value = "Conquista resgatada com sucesso! 🏆🪙"
            }
        }
    }

    fun recordMinigameScore(gameType: String, score: Int, coinsEarned: Int) {
        if (coinsEarned > 0) {
            audioManager.playSfx(SoundEffect.COIN)
        }
        viewModelScope.launch {
            repository.recordMinigameResult(gameType, score, coinsEarned)
            _autonomousState.update {
                it.copy(
                    behaviorState = PetBehaviorState.FELIZ,
                    currentSpeechText = "Você foi incrível no minijogo! Parabéns! 🎮🏆",
                    speechBubbleVisible = true
                )
            }
            _toastMessage.value = "Fim de jogo! Você ganhou $coinsEarned moedas! 🪙"
        }
    }

    fun dismissEvolution() {
        _evolutionCelebration.value = null
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun resetPet() {
        viewModelScope.launch {
            repository.resetPet()
            notificationPrefs.resetAllAlerts()
            PetCareScheduler.cancelAllChecks(getApplication())
            previousStage = null
            _autonomousState.value = PetAutonomousState()
            _toastMessage.value = "Um novo ovo foi concedido para você cuidar!"
        }
    }

    // Audio Settings Controls
    fun setMusicEnabled(enabled: Boolean) {
        audioManager.setMusicEnabled(enabled)
    }

    fun setSfxEnabled(enabled: Boolean) {
        audioManager.setSfxEnabled(enabled)
    }

    fun setMusicVolume(volume: Float) {
        audioManager.setMusicVolume(volume)
    }

    fun setSfxVolume(volume: Float) {
        audioManager.setSfxVolume(volume)
    }

    fun playSfx(sfx: SoundEffect) {
        audioManager.playSfx(sfx)
    }

    fun playUiClick() {
        audioManager.playSfx(SoundEffect.BUTTON)
    }

    // Notification Settings Controls
    fun setNotificationsEnabled(enabled: Boolean) {
        notificationPrefs.isNotificationsEnabled = enabled
        _notificationsEnabled.value = enabled
        if (enabled) {
            PetCareScheduler.scheduleNextCheck(getApplication())
        } else {
            PetCareScheduler.cancelAllChecks(getApplication())
        }
    }

    fun setHungerNotifications(enabled: Boolean) {
        notificationPrefs.isHungerEnabled = enabled
        _hungerNotifications.value = enabled
    }

    fun setHygieneNotifications(enabled: Boolean) {
        notificationPrefs.isHygieneEnabled = enabled
        _hygieneNotifications.value = enabled
    }

    fun setEnergyNotifications(enabled: Boolean) {
        notificationPrefs.isEnergyEnabled = enabled
        _energyNotifications.value = enabled
    }

    fun setHealthNotifications(enabled: Boolean) {
        notificationPrefs.isHealthEnabled = enabled
        _healthNotifications.value = enabled
    }

    fun setLongingNotifications(enabled: Boolean) {
        notificationPrefs.isLongingEnabled = enabled
        _longingNotifications.value = enabled
    }

    fun sendTestNotification() {
        val pet = petState.value
        NotificationHelper.sendPetNotification(
            getApplication(),
            PetNotificationType.TEST,
            pet?.name ?: "Bichinho"
        )
        _toastMessage.value = "Notificação de teste enviada! 🔔"
    }

    fun triggerImmediateNeedsCheck() {
        PetCareScheduler.triggerImmediateCheck(getApplication())
        _toastMessage.value = "Verificação de necessidades executada!"
    }
}
