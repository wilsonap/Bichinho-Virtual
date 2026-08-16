package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.PetStage
import com.example.data.model.Rarity
import com.example.data.model.Species

@Entity(tableName = "pet")
data class PetEntity(
    @PrimaryKey val id: Int = 1, // Single pet constraint: only 1 pet allowed!
    val name: String = "",
    val speciesId: String = "",
    val rarity: String = Rarity.COMUM.name,
    val stage: String = PetStage.OVO.name,
    val hunger: Int = 80, // 0 - 100
    val energy: Int = 90, // 0 - 100
    val happiness: Int = 80, // 0 - 100
    val hygiene: Int = 85, // 0 - 100
    val health: Int = 100, // 0 - 100
    val exp: Int = 0, // 0 - 100 in current stage
    val totalExp: Int = 0,
    val level: Int = 1,
    val birthTimestamp: Long = System.currentTimeMillis(),
    val eggWarmProgress: Int = 0, // 0 - 100 taps/care to warm egg
    val isHatched: Boolean = false,
    val isSleeping: Boolean = false,
    val sleepStartTimestamp: Long = 0L,
    val equippedHat: String = "",
    val equippedAccessory: String = "",
    val roomTheme: String = "decor_bedroom",
    val lastUpdateTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "player")
data class PlayerEntity(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 50,
    val currentStreak: Int = 1,
    val bestStreak: Int = 1,
    val lastLoginDate: String = "",
    val totalMinigamesWon: Int = 0
)

@Entity(tableName = "inventory")
data class InventoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemId: String,
    val category: String,
    val name: String,
    val quantity: Int = 1,
    val isEquipped: Boolean = false
)

@Entity(tableName = "daily_missions")
data class DailyMissionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val targetCount: Int,
    val currentCount: Int = 0,
    val rewardCoins: Int,
    val rewardExp: Int,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val missionDate: String = ""
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val rewardCoins: Int,
    val currentProgress: Int = 0,
    val maxProgress: Int,
    val isUnlocked: Boolean = false,
    val isClaimed: Boolean = false
)

@Entity(tableName = "game_stats")
data class GameStatsEntity(
    @PrimaryKey val id: Int = 1,
    val timesFed: Int = 0,
    val timesBathed: Int = 0,
    val timesSlept: Int = 0,
    val timesPlayed: Int = 0,
    val timesDoctor: Int = 0,
    val minigamesPlayed: Int = 0,
    val memoryHighscore: Int = 0,
    val runnerHighscore: Int = 0,
    val catchHighscore: Int = 0,
    val totalCoinsEarned: Int = 0,
    val totalItemsBought: Int = 0,
    val evolutionsCount: Int = 0
)
