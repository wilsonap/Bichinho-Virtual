package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    // Pet Operations (Single Pet Rule)
    @Query("SELECT * FROM pet WHERE id = 1 LIMIT 1")
    fun getPetFlow(): Flow<PetEntity?>

    @Query("SELECT * FROM pet WHERE id = 1 LIMIT 1")
    suspend fun getPet(): PetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePet(pet: PetEntity)

    @Query("DELETE FROM pet")
    suspend fun clearPet()

    // Player Operations
    @Query("SELECT * FROM player WHERE id = 1 LIMIT 1")
    fun getPlayerFlow(): Flow<PlayerEntity?>

    @Query("SELECT * FROM player WHERE id = 1 LIMIT 1")
    suspend fun getPlayer(): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePlayer(player: PlayerEntity)

    // Inventory Operations
    @Query("SELECT * FROM inventory")
    fun getInventoryFlow(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory WHERE itemId = :itemId LIMIT 1")
    suspend fun getInventoryItem(itemId: String): InventoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryEntity)

    @Update
    suspend fun updateInventoryItem(item: InventoryEntity)

    @Query("DELETE FROM inventory WHERE id = :id")
    suspend fun deleteInventoryItem(id: Int)

    @Query("UPDATE inventory SET isEquipped = 0 WHERE category = :category")
    suspend fun unequipCategory(category: String)

    @Query("UPDATE inventory SET isEquipped = :isEquipped WHERE itemId = :itemId")
    suspend fun setItemEquipped(itemId: String, isEquipped: Boolean)

    // Daily Missions Operations
    @Query("SELECT * FROM daily_missions")
    fun getDailyMissionsFlow(): Flow<List<DailyMissionEntity>>

    @Query("SELECT * FROM daily_missions WHERE id = :id LIMIT 1")
    suspend fun getDailyMission(id: String): DailyMissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyMissions(missions: List<DailyMissionEntity>)

    @Update
    suspend fun updateDailyMission(mission: DailyMissionEntity)

    @Query("DELETE FROM daily_missions")
    suspend fun clearDailyMissions()

    // Achievements Operations
    @Query("SELECT * FROM achievements")
    fun getAchievementsFlow(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE id = :id LIMIT 1")
    suspend fun getAchievement(id: String): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    // Stats Operations
    @Query("SELECT * FROM game_stats WHERE id = 1 LIMIT 1")
    fun getGameStatsFlow(): Flow<GameStatsEntity?>

    @Query("SELECT * FROM game_stats WHERE id = 1 LIMIT 1")
    suspend fun getGameStats(): GameStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGameStats(stats: GameStatsEntity)
}
