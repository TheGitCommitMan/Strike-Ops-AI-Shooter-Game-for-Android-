package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Entity: Player Profile & Game Progress
@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Viper_One",
    val level: Int = 1,
    val xp: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val totalKills: Int = 0,
    val campaignChapter: Int = 1, // Progress tracked from chap 1 to 4
    val hapticStrength: Float = 0.8f,
    val hapticFrequency: Float = 60f,
    val hapticShootEnabled: Boolean = true,
    val hapticReloadEnabled: Boolean = true,
    val hapticHitEnabled: Boolean = true,
    val hapticStyleIndex: Int = 0, // 0: Realistic Click, 1: Tactical Heavy, 2: Cyber Pulse
    val aimingSensitivity: Float = 1.0f,
    val deadzone: Float = 0.15f,
    val currentPreset: Int = 0, // 0: Modern Tactical (Default), 1: Claws-Ergonomic, 2: Lefthanded
    // Joystick/Button offsets for Ergonomics layout
    val moveJoyX: Float = 120f,
    val moveJoyY: Float = 150f,
    val aimJoyX: Float = 120f,
    val aimJoyY: Float = 150f,
    val fireBtnX: Float = 120f,
    val fireBtnY: Float = 300f
)

// 2. Entity: Weapon Slot Loader Specs
@Entity(tableName = "weapon_configs")
data class WeaponConfig(
    @PrimaryKey val id: Int = 1, // 1: Prim. loadout, 2: Sec. loadout
    val baseType: String = "ASSAULT_RIFLE", // ASSAULT_RIFLE, SMG, SNIPER, SHOTGUN
    val weaponName: String = "AR-25 Vanguard",
    val opticalAttach: String = "Holographic", // Holographic, ACOG (+Zoom), None
    val muzzleAttach: String = "Heavy Brake", // Compensator (+Recoil red.), Silencer (-Noise & Muzzle-glow), None
    val magazineAttach: String = "Extended Mag", // Extended Mag, Fast Mag, None
    val stockAttach: String = "Tactical Stock", // Precision Stock, Ergonomic Grip, None
    val isUnlocked: Boolean = true,
    val fireRateMs: Long = 130L,
    val damagePerBullet: Float = 24f,
    val stabilityRatio: Float = 0.85f, // Recoil coefficient (lower means more recoil)
    val weaponWeight: Float = 0.7f // Affects player run speed
)

@Dao
interface GameDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun getPlayerProfile(): Flow<PlayerProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlayerProfile(profile: PlayerProfile)

    @Query("SELECT * FROM weapon_configs ORDER BY id ASC")
    fun getWeaponConfigs(): Flow<List<WeaponConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWeaponConfig(config: WeaponConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWeaponConfigs(configs: List<WeaponConfig>)
}

@Database(entities = [PlayerProfile::class, WeaponConfig::class], version = 1, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "strike_ops_db"
                )
                // Fallback rather than migrations for initial prototype
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class GameRepository(private val gameDao: GameDao) {
    val playerProfile: Flow<PlayerProfile?> = gameDao.getPlayerProfile()
    val weaponConfigs: Flow<List<WeaponConfig>> = gameDao.getWeaponConfigs()

    suspend fun updateProfile(profile: PlayerProfile) {
        gameDao.savePlayerProfile(profile)
    }

    suspend fun updateWeaponConfig(config: WeaponConfig) {
        gameDao.saveWeaponConfig(config)
    }

    suspend fun populateDefaultsIfNeeded(hasConfigsAlready: Boolean) {
        if (!hasConfigsAlready) {
            val defaults = listOf(
                WeaponConfig(
                    id = 1,
                    baseType = "ASSAULT_RIFLE",
                    weaponName = "AR-25 Vanguard",
                    opticalAttach = "Holographic",
                    muzzleAttach = "Heavy Brake",
                    magazineAttach = "Extended Mag",
                    stockAttach = "Tactical Stock",
                    isUnlocked = true,
                    fireRateMs = 120L,
                    damagePerBullet = 22f,
                    stabilityRatio = 0.85f,
                    weaponWeight = 0.72f
                ),
                WeaponConfig(
                    id = 2,
                    baseType = "SMG",
                    weaponName = "MP-9 Blackout",
                    opticalAttach = "Red Dot",
                    muzzleAttach = "Silencer",
                    magazineAttach = "Speed Loader",
                    stockAttach = "Folding Stock",
                    isUnlocked = true,
                    fireRateMs = 80L,
                    damagePerBullet = 15f,
                    stabilityRatio = 0.9f,
                    weaponWeight = 0.5f
                ),
                WeaponConfig(
                    id = 3,
                    baseType = "SNIPER",
                    weaponName = "SR-88 Phantom",
                    opticalAttach = "ACOG 4x",
                    muzzleAttach = "Heavy Brake",
                    magazineAttach = "Standard Mag",
                    stockAttach = "Cheek Riser",
                    isUnlocked = true,
                    fireRateMs = 1000L,
                    damagePerBullet = 95f,
                    stabilityRatio = 0.35f,
                    weaponWeight = 1.2f
                ),
                WeaponConfig(
                    id = 4,
                    baseType = "SHOTGUN",
                    weaponName = "SG-12 Breacher",
                    opticalAttach = "None",
                    muzzleAttach = "Choke",
                    magazineAttach = "Drum Mag",
                    stockAttach = "Pistol Grip",
                    isUnlocked = true,
                    fireRateMs = 700L,
                    damagePerBullet = 15f, // Per pellet, dynamic calculation
                    stabilityRatio = 0.5f,
                    weaponWeight = 0.85f
                )
            )
            gameDao.saveWeaponConfigs(defaults)
            gameDao.savePlayerProfile(PlayerProfile(id = 1))
        }
    }
}
