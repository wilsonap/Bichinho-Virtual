package com.example.data.repository

import android.util.Log
import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class PetRepository(private val dao: PetDao) {

    val petFlow: Flow<PetEntity?> = dao.getPetFlow()
    val playerFlow: Flow<PlayerEntity?> = dao.getPlayerFlow()
    val inventoryFlow: Flow<List<InventoryEntity>> = dao.getInventoryFlow()
    val dailyMissionsFlow: Flow<List<DailyMissionEntity>> = dao.getDailyMissionsFlow()
    val achievementsFlow: Flow<List<AchievementEntity>> = dao.getAchievementsFlow()
    val gameStatsFlow: Flow<GameStatsEntity?> = dao.getGameStatsFlow()

    private var _lastSanitizationResult: InventorySanitizationResult? = null
    val lastSanitizationResult: InventorySanitizationResult?
        get() = _lastSanitizationResult

    suspend fun getPet(): PetEntity? = dao.getPet()

    private fun getCurrentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    suspend fun sanitizeInventory(): InventorySanitizationResult {
        val allItems = dao.getAllInventoryList()
        if (allItems.isEmpty()) {
            val emptyResult = InventorySanitizationResult(0, emptyList(), emptyMap())
            _lastSanitizationResult = emptyResult
            return emptyResult
        }

        val grouped = allItems.groupBy { it.itemId }
        var duplicatesRemoved = 0
        val consolidated = mutableListOf<String>()

        val pet = dao.getPet()
        val activeTheme = pet?.roomTheme?.ifBlank { "decor_bedroom" } ?: "decor_bedroom"
        val activeHat = pet?.equippedHat.orEmpty()
        val activeAccessory = pet?.equippedAccessory.orEmpty()

        for ((itemId, items) in grouped) {
            val primary = items.first()
            val categoryName = primary.category
            val isReusable = categoryName in listOf(
                ItemCategory.BRINQUEDO.name,
                ItemCategory.ROUPA.name,
                ItemCategory.ACESSORIO.name,
                ItemCategory.DECORACAO.name
            )

            val totalQuantity = if (isReusable) {
                1
            } else {
                items.sumOf { it.quantity }
            }

            val isEquipped = when (categoryName) {
                ItemCategory.DECORACAO.name -> (itemId == activeTheme)
                ItemCategory.ROUPA.name -> (itemId == activeHat && activeHat.isNotBlank())
                ItemCategory.ACESSORIO.name -> (itemId == activeAccessory && activeAccessory.isNotBlank())
                else -> false
            }

            if (items.size > 1) {
                // Update primary item with consolidated quantity and proper equipped status
                dao.updateInventoryItem(
                    primary.copy(
                        quantity = totalQuantity,
                        isEquipped = isEquipped
                    )
                )
                // Delete duplicate excess rows
                for (dup in items.drop(1)) {
                    dao.deleteInventoryItem(dup.id)
                    duplicatesRemoved++
                }
                consolidated.add(itemId)
            } else {
                var needsUpdate = false
                var updatedItem = primary
                if (isReusable && primary.quantity != 1) {
                    updatedItem = updatedItem.copy(quantity = 1)
                    needsUpdate = true
                }
                if (primary.isEquipped != isEquipped) {
                    updatedItem = updatedItem.copy(isEquipped = isEquipped)
                    needsUpdate = true
                }
                if (needsUpdate) {
                    dao.updateInventoryItem(updatedItem)
                }
            }
        }

        // Guarantee only ONE decoration is marked isEquipped in database
        dao.unequipCategory(ItemCategory.DECORACAO.name)
        if (activeTheme.isNotBlank()) {
            dao.setItemEquipped(activeTheme, true)
        }

        val freshList = dao.getAllInventoryList()
        val multiQtyMap = freshList.filter { it.quantity > 1 }.associate { it.name.ifBlank { it.itemId } to it.quantity }

        Log.d("ITEM_AUDIT", "sanitizeInventory finished: removed=$duplicatesRemoved, consolidated=$consolidated, multiQty=$multiQtyMap")

        val result = InventorySanitizationResult(
            totalDuplicatesRemoved = duplicatesRemoved,
            consolidatedItemIds = consolidated,
            itemsWithQuantityGreaterThanOne = multiQtyMap
        )
        _lastSanitizationResult = result
        return result
    }

    suspend fun initializeGameIfNeeded() {
        // 1. Sanitize Inventory first to clean any duplicates safely without data loss
        sanitizeInventory()

        // 2. Check Player
        var player = dao.getPlayer()
        val today = getCurrentDateString()
        if (player == null) {
            player = PlayerEntity(
                id = 1,
                coins = 80,
                currentStreak = 1,
                bestStreak = 1,
                lastLoginDate = today
            )
            dao.insertOrUpdatePlayer(player)
        } else {
            // Update daily streak
            if (player.lastLoginDate != today) {
                val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() - 86400000L))
                val newStreak = if (player.lastLoginDate == yesterday) player.currentStreak + 1 else 1
                val best = max(player.bestStreak, newStreak)
                dao.insertOrUpdatePlayer(player.copy(currentStreak = newStreak, bestStreak = best, lastLoginDate = today))

                // Progress streak achievements
                updateAchievementProgress("ach_streak_7", newStreak)
                updateAchievementProgress("ach_streak_30", newStreak)
            }
        }

        // 3. Check Pet (Single Pet Rule)
        var pet = dao.getPet()
        if (pet == null) {
            pet = PetEntity(
                id = 1,
                name = "",
                speciesId = "",
                rarity = Rarity.COMUM.name,
                stage = PetStage.OVO.name,
                hunger = 100,
                energy = 100,
                happiness = 100,
                hygiene = 100,
                health = 100,
                exp = 0,
                totalExp = 0,
                level = 1,
                birthTimestamp = System.currentTimeMillis(),
                eggWarmProgress = 0,
                isHatched = false,
                isSleeping = false,
                roomTheme = "decor_bedroom",
                lastUpdateTimestamp = System.currentTimeMillis()
            )
            dao.insertOrUpdatePet(pet)
        } else {
            // Simulate offline stat changes (offline time does NOT grant XP)
            updateOfflineStats(pet)
            checkEvolution()
        }

        // 4. Seed Initial Inventory ONLY if inventory table is completely empty (never use food_apple as indicator)
        val totalInventoryCount = dao.getInventoryCount()
        if (totalInventoryCount == 0) {
            dao.insertInventoryItem(InventoryEntity(itemId = "food_apple", category = ItemCategory.ALIMENTO.name, name = "Maçã Fresca", quantity = 3))
            dao.insertInventoryItem(InventoryEntity(itemId = "food_cookie", category = ItemCategory.ALIMENTO.name, name = "Biscoito Doce", quantity = 2))
            dao.insertInventoryItem(InventoryEntity(itemId = "toy_ball", category = ItemCategory.BRINQUEDO.name, name = "Bola Saltitante", quantity = 1))
            dao.insertInventoryItem(InventoryEntity(itemId = "decor_bedroom", category = ItemCategory.DECORACAO.name, name = "Quarto Aconchegante", quantity = 1, isEquipped = true))
        }

        // 5. Seed Achievements
        seedAchievementsIfNeeded()

        // 6. Seed Daily Missions for Today
        seedDailyMissionsIfNeeded(today)

        // 7. Check Game Stats
        var stats = dao.getGameStats()
        if (stats == null) {
            stats = GameStatsEntity(id = 1)
            dao.insertOrUpdateGameStats(stats)
        }
    }

    suspend fun updateOfflineStats(pet: PetEntity): Boolean {
        if (!pet.isHatched) return false

        val now = System.currentTimeMillis()
        val simulated = com.example.notification.PetStatsCalculator.calculateSimulatedStats(pet, now)
        if (simulated.elapsedMinutes <= 0) return false

        val updatedPet = pet.copy(
            hunger = simulated.hunger,
            energy = simulated.energy,
            happiness = simulated.happiness,
            hygiene = simulated.hygiene,
            health = simulated.health,
            disease = simulated.disease,
            lowHygieneExposure = simulated.lowHygieneExposure,
            exhaustionCount = simulated.exhaustionCount,
            indigestionStreak = simulated.indigestionStreak,
            isSleeping = simulated.isSleeping,
            lastUpdateTimestamp = now
        )
        dao.insertOrUpdatePet(updatedPet)
        return simulated.wasLonging
    }

    /**
     * @param now Timestamp do tick (injetável para testes). Default: relógio do sistema.
     */
    suspend fun tickLiveStats(now: Long = System.currentTimeMillis()): PetEntity? {
        val pet = dao.getPet() ?: return null
        if (!pet.isHatched) return pet

        var hunger = pet.hunger
        var energy = pet.energy
        var happiness = pet.happiness
        var hygiene = pet.hygiene
        var health = pet.health
        var disease = pet.disease
        var lowHygieneExposure = pet.lowHygieneExposure
        var exhaustionCount = pet.exhaustionCount
        var indigestionStreak = pet.indigestionStreak
        var isSleeping = pet.isSleeping
        val isNight = com.example.notification.PetStatsCalculator.isNightTime(now)

        if (isNight) {
            // --- NIGHTTIME (22:00 to 08:00): Protected Rest Period ---
            isSleeping = true // Pet is in night sleep
            energy = min(100, energy + 2) // Reaches up to 100 and stays sleeping!

            // Slow hunger decay, zero health damage during night
            if (hunger > 0 && Math.random() < 0.2) {
                hunger = max(0, hunger - 1)
            }
            // Hygiene and happiness protected from decay
            // Health protected: zero damage from hunger or hygiene during night
            // Protected: no new disease during night
        } else {
            // --- DAYTIME (08:00 to 22:00): Active / Nap Period ---
            val wasNightAtLastUpdate =
                com.example.notification.PetStatsCalculator.isNightTime(pet.lastUpdateTimestamp)
            if (isSleeping && wasNightAtLastUpdate) {
                isSleeping = false
                Log.d(
                    "SLEEP_AUDIT",
                    "Auto-awakening pet at daytime start (08:00): night sleep ended (energy=$energy)"
                )
            }

            if (isSleeping) {
                // Daytime Nap / Exhaustion sleep
                energy = min(100, energy + 2)
                if (energy >= 100) {
                    isSleeping = false
                    Log.d("SLEEP_AUDIT", "Auto-awakening pet in daytime live tick: energy reached 100%")
                }
            } else {
                // Active daytime decay
                if (hunger > 0) hunger = max(0, hunger - 1)
                if (energy > 0) energy = max(0, energy - 1)
                if (hygiene > 0) hygiene = max(0, hygiene - 1)
                if (happiness > 0) happiness = max(0, happiness - 1)

                // Daytime exhaustion sleep: auto-sleep if energy <= 5
                if (energy <= 5) {
                    isSleeping = true
                    exhaustionCount++
                    if (exhaustionCount >= PetHealthRules.EXHAUSTION_COUNT_LIMIT && disease == PetDisease.NONE.name) {
                        disease = PetDisease.FADIGA.name
                    }
                    Log.d("SLEEP_AUDIT", "Auto-sleeping pet: daytime energy dropped to <= 5%")
                }

                // Hygiene exposure tracking
                if (hygiene <= PetHealthRules.HYGIENE_DAMAGE_THRESHOLD) {
                    lowHygieneExposure++
                    if (lowHygieneExposure >= PetHealthRules.LOW_HYGIENE_EXPOSURE_LIMIT && disease == PetDisease.NONE.name) {
                        disease = PetDisease.RESFRIADO.name
                    }
                } else if (hygiene >= 60) {
                    lowHygieneExposure = 0
                }
            }

            // Daytime health damage if starving (hunger <= 20) or dirty (hygiene <= 20)
            if (hunger <= PetHealthRules.HUNGER_DAMAGE_THRESHOLD || hygiene <= PetHealthRules.HYGIENE_DAMAGE_THRESHOLD) {
                health = max(PetHealthRules.MIN_HEALTH, health - 1)
            }
        }

        val updated = pet.copy(
            hunger = hunger,
            energy = energy,
            happiness = happiness,
            hygiene = hygiene,
            health = health,
            disease = disease,
            lowHygieneExposure = lowHygieneExposure,
            exhaustionCount = exhaustionCount,
            indigestionStreak = indigestionStreak,
            isSleeping = isSleeping,
            lastUpdateTimestamp = now
        )
        dao.insertOrUpdatePet(updated)
        return updated
    }

    private suspend fun seedAchievementsIfNeeded() {
        val existing = dao.getAchievement("ach_bath_1")
        if (existing == null) {
            val achievements = listOf(
                AchievementEntity("ach_bath_1", "Primeiro Banho", "Dê o primeiro banho com sabão e bolhas", 30, 0, 1),
                AchievementEntity("ach_toy_1", "Primeiro Brinquedo", "Compre ou brinque com seu primeiro brinquedo", 40, 0, 1),
                AchievementEntity("ach_evolve_1", "Primeira Evolução", "Evolua o bichinho de filhote para jovem", 100, 0, 1),
                AchievementEntity("ach_adult", "Fase Adulta", "Crie o bichinho até atingir a fase adulta majestosa", 150, 0, 1),
                AchievementEntity("ach_senior", "Ancião Sábio", "Alcance a fase Idoso com seu companheiro.", 300, 0, 1),
                AchievementEntity("ach_streak_7", "Sete Dias Consecutivos", "Cuide do seu bichinho por 7 dias seguidos", 200, 0, 7),
                AchievementEntity("ach_streak_30", "Trinta Dias Consecutivos", "Cuide do seu bichinho por 30 dias seguidos", 500, 0, 30),
                AchievementEntity("ach_minigames_10", "Mestre dos Jogos", "Jogue 10 partidas de minijogos", 80, 0, 10),
                AchievementEntity("ach_shop_5", "Colecionador Fiel", "Compre 5 itens diferentes na loja", 120, 0, 5),
                AchievementEntity("ach_legendary", "Espécie Lendária", "Chocou uma espécie lendária (Dragão ou Fênix)", 250, 0, 1)
            )
            dao.insertAchievements(achievements)
        }
    }

    private suspend fun seedDailyMissionsIfNeeded(today: String) {
        val existing = dao.getDailyMission("miss_feed_$today")
        if (existing == null) {
            dao.clearDailyMissions()
            val missions = listOf(
                DailyMissionEntity("miss_feed_$today", "Alimentar o Bichinho", "Alimente seu companheiro 3 vezes com comidinhas gostosas", 3, 0, 30, 20, missionDate = today),
                DailyMissionEntity("miss_bath_$today", "Hora da Higiene", "Dê pelo menos 1 banho caprichado", 1, 0, 25, 15, missionDate = today),
                DailyMissionEntity("miss_play_$today", "Diversão Garantida", "Brinque em 2 partidas de minijogos", 2, 0, 40, 25, missionDate = today),
                DailyMissionEntity("miss_coins_$today", "Cofre Recheado", "Ganhe 40 moedas em minijogos", 40, 0, 35, 20, missionDate = today)
            )
            dao.insertDailyMissions(missions)
        }
    }

    suspend fun warmEgg(amount: Int = 10): PetEntity? {
        val pet = dao.getPet() ?: return null
        if (pet.isHatched) return pet
        val newProgress = min(100, pet.eggWarmProgress + amount)
        val updated = pet.copy(eggWarmProgress = newProgress)
        dao.insertOrUpdatePet(updated)
        return updated
    }

    suspend fun hatchEgg(petName: String, specificSpecies: Species? = null): Pair<PetEntity, Species> {
        val pet = dao.getPet() ?: throw IllegalStateException("Pet not found")
        val chosenSpecies = specificSpecies ?: Species.getRandomSpecies()
        val nameToUse = petName.ifBlank { pet.name.ifBlank { "Pipoca" } }
        val now = System.currentTimeMillis()

        val hatchedPet = pet.copy(
            name = nameToUse,
            speciesId = chosenSpecies.id,
            rarity = chosenSpecies.rarity.name,
            stage = PetStage.FILHOTE.name,
            hunger = 90,
            energy = 100,
            happiness = 100,
            hygiene = 90,
            health = 100,
            exp = 0,
            totalExp = 0,
            level = 1,
            birthTimestamp = if (pet.birthTimestamp > 0L) pet.birthTimestamp else now,
            hatchedTimestamp = now,
            youthTimestamp = 0L,
            adultTimestamp = 0L,
            seniorTimestamp = 0L,
            eggWarmProgress = 100,
            isHatched = true,
            isSleeping = false,
            lastUpdateTimestamp = now
        )
        dao.insertOrUpdatePet(hatchedPet)

        if (chosenSpecies.rarity == Rarity.LENDARIA) {
            updateAchievementProgress("ach_legendary", 1)
        }

        return Pair(hatchedPet, chosenSpecies)
    }

    suspend fun setPetSpeciesAndStage(species: Species, stage: PetStage) {
        val pet = dao.getPet() ?: return
        val updated = pet.copy(
            speciesId = species.id,
            rarity = species.rarity.name,
            stage = stage.name,
            isHatched = stage != PetStage.OVO,
            lastUpdateTimestamp = System.currentTimeMillis()
        )
        dao.insertOrUpdatePet(updated)
    }

    suspend fun setPetName(name: String) {
        val pet = dao.getPet() ?: return
        dao.insertOrUpdatePet(pet.copy(name = name.trim()))
    }

    suspend fun feedPet(shopItem: ShopItem?): FeedOutcome {
        val pet = dao.getPet() ?: return FeedOutcome.failed()
        if (!pet.isHatched) return FeedOutcome.failed()

        // Guard: Reject non-food items if a specific shop item was passed
        if (shopItem != null && shopItem.category != ItemCategory.ALIMENTO) {
            Log.w("ITEM_AUDIT", "itemId=${shopItem.id}, itemName=${shopItem.name}, itemType=${shopItem.category.name}, action=FEED, result=REJECTED_WRONG_CATEGORY")
            return FeedOutcome.failed()
        }

        val species = Species.fromId(pet.speciesId)
        val bonuses = FoodBonusResolver.resolve(species, shopItem)
        val hungerGain = bonuses.hungerGain
        val healthGain = bonuses.healthGain
        val energyGain = bonuses.energyGain
        val happinessGain = bonuses.happinessGain
        val expGain = bonuses.expGain

        // Consume item from inventory if specific item passed
        if (shopItem != null) {
            val inv = dao.getInventoryItem(shopItem.id)
            if (inv != null && inv.quantity > 0) {
                if (inv.quantity == 1) {
                    dao.deleteInventoryItem(inv.id)
                } else {
                    dao.updateInventoryItem(inv.copy(quantity = inv.quantity - 1))
                }
            } else {
                Log.d("ITEM_AUDIT", "itemId=${shopItem.id}, itemName=${shopItem.name}, itemType=FOOD, action=FEED, result=NOT_IN_INVENTORY")
                return FeedOutcome.failed()
            }
        }

        // Indigestion tracking: sweets or overfeeding when hunger >= 90
        var newIndigestionStreak = pet.indigestionStreak
        var newDisease = pet.disease
        val isSweetOrJunk = shopItem?.id in listOf("food_cookie", "food_pizza")
        val isOverfed = pet.hunger >= 90

        if (isSweetOrJunk || isOverfed) {
            newIndigestionStreak++
            if (newIndigestionStreak >= PetHealthRules.INDIGESTION_STREAK_LIMIT && newDisease == PetDisease.NONE.name) {
                newDisease = PetDisease.INDIGESTAO.name
            }
        } else if (shopItem?.id in listOf("food_apple", "food_fish")) {
            newIndigestionStreak = max(0, newIndigestionStreak - 1)
        }

        applyPetCare(
            hungerDelta = hungerGain,
            energyDelta = energyGain,
            happinessDelta = happinessGain,
            hygieneDelta = -2,
            healthDelta = healthGain,
            expDelta = expGain,
            customDisease = newDisease,
            customIndigestionStreak = newIndigestionStreak
        )

        // Increment stats & daily missions
        val stats = dao.getGameStats() ?: GameStatsEntity(id = 1)
        dao.insertOrUpdateGameStats(stats.copy(timesFed = stats.timesFed + 1))
        incrementDailyMission("miss_feed_${getCurrentDateString()}", 1)

        Log.d(
            "ITEM_AUDIT",
            "itemId=${shopItem?.id ?: "snack"}, itemName=${shopItem?.name ?: "Ração Básica"}, itemType=FOOD, action=FEED, result=SUCCESS(hunger+$hungerGain, exp+$expGain, favorite=${bonuses.wasFavorite}, consumed=${shopItem != null})"
        )

        return FeedOutcome(success = true, wasFavorite = bonuses.wasFavorite)
    }

    suspend fun bathePet() {
        val pet = dao.getPet() ?: return
        if (!pet.isHatched) return

        applyPetCare(
            hungerDelta = -2,
            energyDelta = -5,
            happinessDelta = 20,
            hygieneDelta = 60,
            healthDelta = 10,
            expDelta = 15,
            customLowHygieneExposure = 0
        )

        val stats = dao.getGameStats() ?: GameStatsEntity(id = 1)
        dao.insertOrUpdateGameStats(stats.copy(timesBathed = stats.timesBathed + 1))
        incrementDailyMission("miss_bath_${getCurrentDateString()}", 1)
        updateAchievementProgress("ach_bath_1", 1)
    }

    suspend fun wakeUpPet(force: Boolean = false): Boolean {
        val pet = dao.getPet() ?: return false
        if (!pet.isHatched || !pet.isSleeping) return false

        val now = System.currentTimeMillis()
        if (com.example.notification.PetStatsCalculator.isNightTime(now) && !force) {
            Log.d("SLEEP_AUDIT", "wakeUpPet: Rejected because pet is in protected night sleep (22:00-08:00)")
            return false
        }

        val sleepDurationMinutes = ((now - pet.sleepStartTimestamp) / (1000 * 60)).toInt()
        val energyRestored = min(60, max(20, sleepDurationMinutes * 2))
        val updated = pet.copy(
            isSleeping = false,
            energy = min(100, pet.energy + energyRestored),
            exhaustionCount = if (pet.energy + energyRestored >= 70) 0 else pet.exhaustionCount,
            lastUpdateTimestamp = now
        )
        dao.insertOrUpdatePet(updated)
        Log.d("SLEEP_AUDIT", "wakeUpPet: pet woke up with energy=${updated.energy}")
        return true
    }

    suspend fun putPetToSleep(): Boolean {
        val pet = dao.getPet() ?: return false
        if (!pet.isHatched || pet.isSleeping) return false

        val now = System.currentTimeMillis()
        val stats = dao.getGameStats() ?: GameStatsEntity(id = 1)
        dao.insertOrUpdateGameStats(stats.copy(timesSlept = stats.timesSlept + 1))
        val updated = pet.copy(
            isSleeping = true,
            sleepStartTimestamp = now,
            lastUpdateTimestamp = now
        )
        dao.insertOrUpdatePet(updated)
        Log.d("SLEEP_AUDIT", "putPetToSleep: pet went to sleep with energy=${pet.energy}")
        return true
    }

    suspend fun toggleSleep(): Boolean {
        val pet = dao.getPet() ?: return false
        if (!pet.isHatched) return false
        val now = System.currentTimeMillis()
        if (com.example.notification.PetStatsCalculator.isNightTime(now)) {
            // During protected night hours, pet stays sleeping
            return true
        }

        return if (pet.isSleeping) {
            wakeUpPet()
            false
        } else {
            putPetToSleep()
            true
        }
    }

    suspend fun playWithPet(toyItem: ShopItem? = null): Boolean {
        val pet = dao.getPet() ?: return false
        if (!pet.isHatched) return false

        // Guard: If a specific item is passed, verify it is BRINQUEDO
        if (toyItem != null && toyItem.category != ItemCategory.BRINQUEDO) {
            Log.w("ITEM_AUDIT", "itemId=${toyItem.id}, itemName=${toyItem.name}, itemType=${toyItem.category.name}, action=PLAY, result=REJECTED_WRONG_CATEGORY")
            return false
        }

        val happyGain = toyItem?.happinessBoost ?: 25
        val expGain = toyItem?.expBoost ?: 15
        val hygieneGain = toyItem?.hygieneBoost ?: 0
        val energyGain = toyItem?.energyBoost ?: 0

        // Important: Toys are REUSABLE. We DO NOT decrement quantity or delete item from inventory.
        // Important: hungerDelta = 0. Toys NEVER alter hunger!
        applyPetCare(
            hungerDelta = 0,
            energyDelta = if (energyGain > 0) energyGain else -5,
            happinessDelta = happyGain,
            hygieneDelta = hygieneGain,
            healthDelta = 0,
            expDelta = expGain
        )

        val stats = dao.getGameStats() ?: GameStatsEntity(id = 1)
        dao.insertOrUpdateGameStats(stats.copy(timesPlayed = stats.timesPlayed + 1))
        updateAchievementProgress("ach_toy_1", 1)

        Log.d(
            "ITEM_AUDIT",
            "itemId=${toyItem?.id ?: "default_toy"}, itemName=${toyItem?.name ?: "Brincadeira Simples"}, itemType=TOY, action=PLAY, result=SUCCESS(happiness+$happyGain, exp+$expGain, hungerDelta=0, consumed=false)"
        )
        return true
    }

    suspend fun useMedicine(medicineItem: ShopItem): Boolean {
        val pet = dao.getPet() ?: return false
        if (!pet.isHatched) return false

        // Guard: Verify item is MEDICAMENTO
        if (medicineItem.category != ItemCategory.MEDICAMENTO) {
            Log.w("ITEM_AUDIT", "itemId=${medicineItem.id}, itemName=${medicineItem.name}, itemType=${medicineItem.category.name}, action=MEDICINE, result=REJECTED_WRONG_CATEGORY")
            return false
        }

        val inv = dao.getInventoryItem(medicineItem.id) ?: dao.getInventoryItem("food_potion")
        if (inv == null || inv.quantity <= 0) {
            Log.d("ITEM_AUDIT", "itemId=${medicineItem.id}, itemName=${medicineItem.name}, itemType=MEDICINE, action=USE_MEDICINE, result=NOT_IN_INVENTORY")
            return false
        }

        // Consume 1 unit of medicine
        if (inv.quantity == 1) {
            dao.deleteInventoryItem(inv.id)
        } else {
            dao.updateInventoryItem(inv.copy(quantity = inv.quantity - 1))
        }

        val healthGain = if (medicineItem.healthBoost > 0) medicineItem.healthBoost else 30
        val energyGain = if (medicineItem.energyBoost > 0) medicineItem.energyBoost else 15
        val expGain = if (medicineItem.expBoost > 0) medicineItem.expBoost else 15

        val currentDisease = PetDisease.fromString(pet.disease)
        var newDisease = pet.disease
        var newIndigestion = pet.indigestionStreak
        var newHygieneExp = pet.lowHygieneExposure
        var newExhaustion = pet.exhaustionCount

        val isCurative = when (medicineItem.id) {
            "med_potion", "food_potion" -> true
            "med_vitamin" -> currentDisease == PetDisease.FADIGA
            "med_digestive" -> currentDisease == PetDisease.INDIGESTAO
            "med_cold" -> currentDisease == PetDisease.RESFRIADO
            else -> false
        }

        if (isCurative) {
            newDisease = PetDisease.NONE.name
            when (currentDisease) {
                PetDisease.INDIGESTAO -> newIndigestion = 0
                PetDisease.RESFRIADO -> newHygieneExp = 0
                PetDisease.FADIGA -> newExhaustion = 0
                PetDisease.NONE -> {}
            }
        }

        // Medicines restore health and energy without modifying hunger
        applyPetCare(
            hungerDelta = 0,
            energyDelta = energyGain,
            happinessDelta = 15,
            hygieneDelta = 0,
            healthDelta = healthGain,
            expDelta = expGain,
            customDisease = newDisease,
            customIndigestionStreak = newIndigestion,
            customLowHygieneExposure = newHygieneExp,
            customExhaustionCount = newExhaustion
        )

        val stats = dao.getGameStats() ?: GameStatsEntity(id = 1)
        dao.insertOrUpdateGameStats(stats.copy(timesDoctor = stats.timesDoctor + 1))

        Log.d(
            "ITEM_AUDIT",
            "itemId=${medicineItem.id}, itemName=${medicineItem.name}, cured=$isCurative, newDisease=$newDisease, action=USE_MEDICINE, result=SUCCESS(health+$healthGain, energy+$energyGain, hungerDelta=0, consumed=true)"
        )
        return true
    }

    suspend fun doctorCheckup(payWithCoins: Boolean = false): DoctorCheckupResult {
        val pet = dao.getPet() ?: return DoctorCheckupResult.Success(true, "Pet não encontrado")
        if (!pet.isHatched) return DoctorCheckupResult.Success(true, "Ovo ainda não chocado")

        val now = System.currentTimeMillis()
        val elapsedSinceLast = now - pet.lastDoctorCheckupTimestamp
        val isFree = elapsedSinceLast >= PetHealthRules.DOCTOR_COOLDOWN_MS || pet.lastDoctorCheckupTimestamp == 0L
        val isCritical = pet.health <= PetHealthRules.HEALTH_CRITICO_MAX

        var usedCoins = false

        if (!isFree) {
            val player = dao.getPlayer() ?: PlayerEntity()
            if (payWithCoins) {
                if (player.coins >= PetHealthRules.DOCTOR_PAID_COST) {
                    dao.insertOrUpdatePlayer(player.copy(coins = player.coins - PetHealthRules.DOCTOR_PAID_COST))
                    usedCoins = true
                } else if (isCritical) {
                    // Emergency care
                    val coinsToDeduct = min(player.coins, PetHealthRules.DOCTOR_PAID_COST)
                    dao.insertOrUpdatePlayer(player.copy(coins = player.coins - coinsToDeduct))
                    usedCoins = true
                } else {
                    return DoctorCheckupResult.InsufficientCoins(PetHealthRules.DOCTOR_PAID_COST, player.coins)
                }
            } else if (isCritical) {
                val playerCoins = player.coins
                val coinsToDeduct = min(playerCoins, PetHealthRules.DOCTOR_PAID_COST)
                if (coinsToDeduct > 0) {
                    dao.insertOrUpdatePlayer(player.copy(coins = playerCoins - coinsToDeduct))
                    usedCoins = true
                }
            } else {
                val remainingMs = PetHealthRules.DOCTOR_COOLDOWN_MS - elapsedSinceLast
                return DoctorCheckupResult.Cooldown(remainingMs, PetHealthRules.DOCTOR_PAID_COST)
            }
        }

        val updated = pet.copy(
            health = 100,
            happiness = min(100, pet.happiness + 20),
            energy = min(100, pet.energy + 15),
            disease = PetDisease.NONE.name,
            lowHygieneExposure = 0,
            exhaustionCount = 0,
            indigestionStreak = 0,
            lastDoctorCheckupTimestamp = now,
            lastUpdateTimestamp = now
        )
        dao.insertOrUpdatePet(updated)

        val stats = dao.getGameStats() ?: GameStatsEntity(id = 1)
        dao.insertOrUpdateGameStats(stats.copy(timesDoctor = stats.timesDoctor + 1))

        return DoctorCheckupResult.Success(
            isFree = !usedCoins,
            message = if (usedCoins) "Consulta realizada por ${PetHealthRules.DOCTOR_PAID_COST} moedas!" else "Consulta médica gratuita realizada com sucesso!"
        )
    }

    private suspend fun applyPetCare(
        hungerDelta: Int,
        energyDelta: Int,
        happinessDelta: Int,
        hygieneDelta: Int,
        healthDelta: Int,
        expDelta: Int,
        customDisease: String? = null,
        customIndigestionStreak: Int? = null,
        customLowHygieneExposure: Int? = null,
        customExhaustionCount: Int? = null
    ) {
        val pet = dao.getPet() ?: return

        val newHunger = min(100, max(0, pet.hunger + hungerDelta))
        val newEnergy = min(100, max(0, pet.energy + energyDelta))
        val newHappiness = min(100, max(0, pet.happiness + happinessDelta))
        val newHygiene = min(100, max(0, pet.hygiene + hygieneDelta))
        val newHealth = min(100, max(PetHealthRules.MIN_HEALTH, pet.health + healthDelta))

        val newDisease = customDisease ?: pet.disease
        val newIndigestionStreak = customIndigestionStreak ?: pet.indigestionStreak
        val newLowHygieneExposure = customLowHygieneExposure ?: pet.lowHygieneExposure
        val newExhaustionCount = customExhaustionCount ?: pet.exhaustionCount

        val newTotalExp = pet.totalExp + expDelta
        val newLevel = 1 + (newTotalExp / 60)
        val expInStage = (newTotalExp % 100)

        // Stage evolution progression calculated via central PetEvolutionCalculator (Non-Regression Hybrid Engine)
        val currentStage = try {
            PetStage.valueOf(pet.stage)
        } catch (_: Exception) {
            PetStage.FILHOTE
        }

        val now = System.currentTimeMillis()
        val daysAlive = PetEvolutionCalculator.calculateDaysAlive(pet.birthTimestamp, now)
        val eligibleStage = PetEvolutionCalculator.evaluateEligibleStage(
            currentStage = currentStage,
            daysAlive = daysAlive,
            level = newLevel,
            isHatched = pet.isHatched
        )

        var hatchedTs = pet.hatchedTimestamp
        var youthTs = pet.youthTimestamp
        var adultTs = pet.adultTimestamp
        var seniorTs = pet.seniorTimestamp
        var evolutionsCount = 0

        if (eligibleStage.ordinal > currentStage.ordinal) {
            if (hatchedTs == 0L) hatchedTs = pet.birthTimestamp
            if (eligibleStage.ordinal >= PetStage.JOVEM.ordinal && youthTs == 0L) {
                youthTs = now
                updateAchievementProgress("ach_evolve_1", 1)
            }
            if (eligibleStage.ordinal >= PetStage.ADULTO.ordinal && adultTs == 0L) {
                adultTs = now
                updateAchievementProgress("ach_adult", 1)
            }
            if (eligibleStage.ordinal >= PetStage.IDOSO.ordinal && seniorTs == 0L) {
                seniorTs = now
                updateAchievementProgress("ach_senior", 1)
            }
            evolutionsCount = eligibleStage.ordinal - currentStage.ordinal
        }

        if (evolutionsCount > 0) {
            val stats = dao.getGameStats() ?: GameStatsEntity(id = 1)
            dao.insertOrUpdateGameStats(stats.copy(evolutionsCount = stats.evolutionsCount + evolutionsCount))
        }

        val updated = pet.copy(
            hunger = newHunger,
            energy = newEnergy,
            happiness = newHappiness,
            hygiene = newHygiene,
            health = newHealth,
            disease = newDisease,
            indigestionStreak = newIndigestionStreak,
            lowHygieneExposure = newLowHygieneExposure,
            exhaustionCount = newExhaustionCount,
            exp = expInStage,
            totalExp = newTotalExp,
            level = newLevel,
            stage = eligibleStage.name,
            hatchedTimestamp = hatchedTs,
            youthTimestamp = youthTs,
            adultTimestamp = adultTs,
            seniorTimestamp = seniorTs,
            lastUpdateTimestamp = now
        )
        dao.insertOrUpdatePet(updated)
    }

    /**
     * Verifica e aplica evolução do pet caso ele tenha cumprido os requisitos híbridos (dias + nível).
     * @param now Timestamp da avaliação (injetável para testes).
     * @return PetEntity atualizado se houve evolução ou o pet atual.
     */
    suspend fun checkEvolution(now: Long = System.currentTimeMillis()): PetEntity? {
        val pet = dao.getPet() ?: return null
        if (!pet.isHatched) return pet

        val currentStage = try {
            PetStage.valueOf(pet.stage)
        } catch (_: Exception) {
            PetStage.FILHOTE
        }

        val daysAlive = PetEvolutionCalculator.calculateDaysAlive(pet.birthTimestamp, now)
        val eligibleStage = PetEvolutionCalculator.evaluateEligibleStage(
            currentStage = currentStage,
            daysAlive = daysAlive,
            level = pet.level,
            isHatched = pet.isHatched
        )

        if (eligibleStage.ordinal > currentStage.ordinal) {
            var hatchedTs = pet.hatchedTimestamp
            var youthTs = pet.youthTimestamp
            var adultTs = pet.adultTimestamp
            var seniorTs = pet.seniorTimestamp

            if (hatchedTs == 0L) hatchedTs = pet.birthTimestamp
            if (eligibleStage.ordinal >= PetStage.JOVEM.ordinal && youthTs == 0L) {
                youthTs = now
                updateAchievementProgress("ach_evolve_1", 1)
            }
            if (eligibleStage.ordinal >= PetStage.ADULTO.ordinal && adultTs == 0L) {
                adultTs = now
                updateAchievementProgress("ach_adult", 1)
            }
            if (eligibleStage.ordinal >= PetStage.IDOSO.ordinal && seniorTs == 0L) {
                seniorTs = now
                updateAchievementProgress("ach_senior", 1)
            }

            val evolutionsGained = eligibleStage.ordinal - currentStage.ordinal
            val stats = dao.getGameStats() ?: GameStatsEntity(id = 1)
            dao.insertOrUpdateGameStats(stats.copy(evolutionsCount = stats.evolutionsCount + evolutionsGained))

            val updatedPet = pet.copy(
                stage = eligibleStage.name,
                hatchedTimestamp = hatchedTs,
                youthTimestamp = youthTs,
                adultTimestamp = adultTs,
                seniorTimestamp = seniorTs,
                lastUpdateTimestamp = now
            )
            dao.insertOrUpdatePet(updatedPet)
            return updatedPet
        }

        return pet
    }

    suspend fun recordMinigameResult(gameType: String, score: Int, coinsEarned: Int) {
        addCoins(coinsEarned)

        val stats = dao.getGameStats() ?: GameStatsEntity(id = 1)
        val newMem = if (gameType == "memory") max(stats.memoryHighscore, score) else stats.memoryHighscore
        val newRun = if (gameType == "runner") max(stats.runnerHighscore, score) else stats.runnerHighscore
        val newCat = if (gameType == "catch") max(stats.catchHighscore, score) else stats.catchHighscore

        dao.insertOrUpdateGameStats(
            stats.copy(
                minigamesPlayed = stats.minigamesPlayed + 1,
                memoryHighscore = newMem,
                runnerHighscore = newRun,
                catchHighscore = newCat
            )
        )

        val today = getCurrentDateString()
        incrementDailyMission("miss_play_$today", 1)
        incrementDailyMission("miss_coins_$today", coinsEarned)
        updateAchievementProgress("ach_minigames_10", stats.minigamesPlayed + 1)

        // Give pet happiness & exp for playing minigames
        applyPetCare(
            hungerDelta = -4,
            energyDelta = -8,
            happinessDelta = 25,
            hygieneDelta = -3,
            healthDelta = 5,
            expDelta = 20
        )
    }

    suspend fun addCoins(amount: Int) {
        if (amount <= 0) return
        val player = dao.getPlayer() ?: return
        dao.insertOrUpdatePlayer(player.copy(coins = player.coins + amount))

        val stats = dao.getGameStats() ?: GameStatsEntity(id = 1)
        dao.insertOrUpdateGameStats(stats.copy(totalCoinsEarned = stats.totalCoinsEarned + amount))
    }

    suspend fun buyItem(item: ShopItem, equipImmediately: Boolean = false): Boolean {
        val player = dao.getPlayer() ?: return false
        if (player.coins < item.price) return false

        val isReusable = item.category in listOf(
            ItemCategory.BRINQUEDO,
            ItemCategory.ROUPA,
            ItemCategory.ACESSORIO,
            ItemCategory.DECORACAO
        )

        val existing = dao.getInventoryItem(item.id)

        // Block repurchase of reusable items already owned
        if (isReusable && existing != null && existing.quantity > 0) {
            Log.w("ITEM_AUDIT", "itemId=${item.id}, action=BUY, result=REJECTED_ALREADY_OWNED")
            if (equipImmediately && (item.category == ItemCategory.ROUPA || item.category == ItemCategory.ACESSORIO || item.category == ItemCategory.DECORACAO)) {
                equipItem(existing)
            }
            return false
        }

        // Deduct coins
        dao.insertOrUpdatePlayer(player.copy(coins = player.coins - item.price))

        // Add to inventory
        val targetEntity = if (existing != null) {
            val updated = existing.copy(quantity = existing.quantity + 1)
            dao.updateInventoryItem(updated)
            updated
        } else {
            val newEntity = InventoryEntity(
                itemId = item.id,
                category = item.category.name,
                name = item.name,
                quantity = 1,
                isEquipped = false
            )
            dao.insertInventoryItem(newEntity)
            newEntity
        }

        val stats = dao.getGameStats() ?: GameStatsEntity(id = 1)
        val newItemsBought = stats.totalItemsBought + 1
        dao.insertOrUpdateGameStats(stats.copy(totalItemsBought = newItemsBought))
        updateAchievementProgress("ach_shop_5", newItemsBought)

        Log.d(
            "ITEM_AUDIT",
            "itemId=${item.id}, itemName=${item.name}, itemType=${item.category.name}, action=BUY, result=SUCCESS(price=${item.price}, remainingCoins=${player.coins - item.price})"
        )

        if (equipImmediately && (item.category == ItemCategory.ROUPA || item.category == ItemCategory.ACESSORIO || item.category == ItemCategory.DECORACAO)) {
            val freshItem = dao.getInventoryItem(item.id) ?: targetEntity
            equipItem(freshItem)
        }

        return true
    }

    suspend fun equipItem(inventoryItem: InventoryEntity) {
        val pet = dao.getPet() ?: return
        when (inventoryItem.category) {
            ItemCategory.ROUPA.name -> {
                val isCurrentlyEquipped = pet.equippedHat == inventoryItem.itemId
                val newHat = if (isCurrentlyEquipped) "" else inventoryItem.itemId
                dao.insertOrUpdatePet(pet.copy(equippedHat = newHat))
                dao.unequipCategory(ItemCategory.ROUPA.name)
                if (!isCurrentlyEquipped) {
                    dao.setItemEquipped(inventoryItem.itemId, true)
                }
            }
            ItemCategory.ACESSORIO.name -> {
                val isCurrentlyEquipped = pet.equippedAccessory == inventoryItem.itemId
                val newAcc = if (isCurrentlyEquipped) "" else inventoryItem.itemId
                dao.insertOrUpdatePet(pet.copy(equippedAccessory = newAcc))
                dao.unequipCategory(ItemCategory.ACESSORIO.name)
                if (!isCurrentlyEquipped) {
                    dao.setItemEquipped(inventoryItem.itemId, true)
                }
            }
            ItemCategory.DECORACAO.name -> {
                val isCurrentlyEquipped = pet.roomTheme == inventoryItem.itemId
                val newTheme = if (isCurrentlyEquipped) "decor_bedroom" else inventoryItem.itemId
                dao.insertOrUpdatePet(pet.copy(roomTheme = newTheme))
                dao.unequipCategory(ItemCategory.DECORACAO.name)
                if (!isCurrentlyEquipped) {
                    dao.setItemEquipped(inventoryItem.itemId, true)
                }
            }
        }
        Log.d(
            "ITEM_AUDIT",
            "itemId=${inventoryItem.itemId}, itemName=${inventoryItem.name}, itemType=${inventoryItem.category}, action=EQUIP, result=SUCCESS(isEquipped=${!inventoryItem.isEquipped})"
        )
    }

    suspend fun equipItemById(itemId: String, categoryName: String) {
        val inv = dao.getInventoryItem(itemId)
        if (inv != null) {
            equipItem(inv)
        } else {
            equipItem(InventoryEntity(itemId = itemId, category = categoryName, name = "", quantity = 1, isEquipped = false))
        }
    }

    suspend fun claimDailyMission(missionId: String): Boolean {
        val mission = dao.getDailyMission(missionId) ?: return false
        if (!mission.isCompleted || mission.isClaimed) return false

        dao.updateDailyMission(mission.copy(isClaimed = true))
        addCoins(mission.rewardCoins)
        applyPetCare(0, 0, 10, 0, 0, mission.rewardExp)
        return true
    }

    suspend fun claimAchievement(achievementId: String): Boolean {
        val achievement = dao.getAchievement(achievementId) ?: return false
        if (!achievement.isUnlocked || achievement.isClaimed) return false

        dao.updateAchievement(achievement.copy(isClaimed = true))
        addCoins(achievement.rewardCoins)
        applyPetCare(0, 0, 15, 0, 0, 30)
        return true
    }

    private suspend fun incrementDailyMission(missionId: String, amount: Int) {
        val mission = dao.getDailyMission(missionId) ?: return
        val newCurrent = min(mission.targetCount, mission.currentCount + amount)
        val isCompleted = newCurrent >= mission.targetCount
        dao.updateDailyMission(mission.copy(currentCount = newCurrent, isCompleted = isCompleted))
    }

    private suspend fun updateAchievementProgress(achievementId: String, progress: Int) {
        val achievement = dao.getAchievement(achievementId) ?: return
        val newProg = min(achievement.maxProgress, max(achievement.currentProgress, progress))
        val isUnlocked = newProg >= achievement.maxProgress
        dao.updateAchievement(achievement.copy(currentProgress = newProg, isUnlocked = isUnlocked))
    }

    suspend fun resetPet() {
        dao.clearPet()
        dao.clearDailyMissions()
        // Reset player stats
        val player = dao.getPlayer()
        if (player != null) {
            dao.insertOrUpdatePlayer(player.copy(coins = 80))
        }
        initializeGameIfNeeded()
    }
}

data class InventorySanitizationResult(
    val totalDuplicatesRemoved: Int,
    val consolidatedItemIds: List<String>,
    val itemsWithQuantityGreaterThanOne: Map<String, Int>
)
