package com.example.game

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.PlayerProfile
import com.example.data.WeaponConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

// Directional vectors for simple bots
data class GameVec2(val x: Float, val y: Float) {
    fun length() = sqrt(x * x + y * y)
    fun normalized(): GameVec2 {
        val len = length()
        return if (len > 0f) GameVec2(x / len, y / len) else GameVec2(0f, 0f)
    }
}

// Map Layout configurations
object MapGridData {
    // 0 = Empty floor
    // 1 = Steel reinforced wall
    // 2 = Cyber-bunker concrete
    // 3 = Glowing Command screen wall
    // 4 = Armory crates / barricade
    // 8 = Extraction Point / Objective spot
    // 9 = Destructible Server/Target console

    val campaignMaps = listOf(
        // Chapter 1 Map (Steel Breach)
        arrayOf(
            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
            intArrayOf(1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 8, 1),
            intArrayOf(1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 1),
            intArrayOf(1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1),
            intArrayOf(1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1),
            intArrayOf(1, 1, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1),
            intArrayOf(1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1),
            intArrayOf(1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1),
            intArrayOf(1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
        ),
        // Chapter 2 Map (Rogue Command Console Wipe)
        arrayOf(
            intArrayOf(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2),
            intArrayOf(2, 0, 0, 0, 0, 0, 2, 0, 0, 0, 9, 2),
            intArrayOf(2, 0, 2, 2, 0, 0, 2, 0, 2, 2, 2, 2),
            intArrayOf(2, 0, 2, 9, 0, 0, 0, 0, 0, 0, 0, 2),
            intArrayOf(2, 0, 2, 2, 2, 0, 2, 2, 2, 2, 0, 2),
            intArrayOf(2, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 2),
            intArrayOf(2, 2, 2, 0, 2, 2, 2, 0, 2, 2, 0, 2),
            intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 2),
            intArrayOf(2, 0, 2, 2, 2, 2, 2, 0, 2, 9, 0, 2),
            intArrayOf(2, 0, 0, 0, 0, 0, 2, 0, 2, 2, 0, 2),
            intArrayOf(2, 2, 2, 0, 0, 8, 2, 0, 0, 0, 0, 2),
            intArrayOf(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2)
        ),
        // Chapter 3 Map (Weapon Depot Boss Raid)
        arrayOf(
            intArrayOf(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3),
            intArrayOf(3, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 3),
            intArrayOf(3, 0, 3, 3, 3, 0, 3, 0, 3, 3, 0, 3),
            intArrayOf(3, 0, 3, 0, 0, 0, 0, 0, 0, 3, 0, 3),
            intArrayOf(3, 0, 3, 0, 3, 3, 3, 3, 0, 3, 0, 3),
            intArrayOf(3, 0, 0, 0, 3, 0, 0, 3, 0, 0, 0, 3),
            intArrayOf(3, 3, 3, 0, 3, 0, 0, 3, 3, 3, 0, 3),
            intArrayOf(3, 0, 0, 0, 3, 0, 0, 3, 0, 0, 0, 3),
            intArrayOf(3, 0, 3, 3, 3, 3, 3, 3, 3, 3, 0, 3),
            intArrayOf(3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3),
            intArrayOf(3, 3, 3, 3, 3, 3, 8, 3, 3, 3, 3, 3),
            intArrayOf(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3)
        ),
        // Chapter 4 Map (Apex HQ Final Ascent)
        arrayOf(
            intArrayOf(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4),
            intArrayOf(4, 0, 0, 0, 0, 0, 4, 0, 0, 0, 8, 4),
            intArrayOf(4, 0, 4, 4, 0, 0, 4, 0, 4, 4, 4, 4),
            intArrayOf(4, 0, 4, 0, 0, 0, 0, 0, 0, 4, 0, 4),
            intArrayOf(4, 0, 4, 0, 4, 4, 4, 4, 0, 4, 0, 4),
            intArrayOf(4, 0, 0, 0, 4, 0, 0, 4, 0, 0, 0, 4),
            intArrayOf(4, 4, 4, 0, 4, 0, 0, 4, 4, 4, 0, 4),
            intArrayOf(4, 0, 0, 0, 4, 0, 0, 4, 0, 0, 0, 4),
            intArrayOf(4, 0, 4, 4, 4, 4, 4, 4, 4, 4, 0, 4),
            intArrayOf(4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4),
            intArrayOf(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4),
            intArrayOf(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
        )
    )

    // Dedicated Arena Map for Skirmish multiplayer simulations
    val skirmishArena = arrayOf(
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1),
        intArrayOf(1, 0, 1, 0, 1, 0, 4, 4, 4, 4, 0, 1, 0, 1, 0, 1),
        intArrayOf(1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1),
        intArrayOf(1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1),
        intArrayOf(1, 0, 0, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1),
        intArrayOf(1, 1, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 1, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 1, 0, 1, 1, 1, 1, 0, 1, 0, 0, 0, 1),
        intArrayOf(1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1),
        intArrayOf(1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1),
        intArrayOf(1, 0, 1, 0, 1, 0, 4, 4, 4, 4, 0, 1, 0, 1, 0, 1),
        intArrayOf(1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
    )
}

enum class BotState { PATROL, CHASE, SHOOT, DEAD }

// Enemy & Friendly Bot Agent
data class GameBot(
    val id: Int,
    val name: String,
    var x: Float,
    var y: Float,
    var angle: Float = 0f,
    var health: Float = 100f,
    val isEnemy: Boolean = true, // Friendly bots have isEnemy = false
    var state: BotState = BotState.PATROL,
    var alarmLevel: Float = 0f, // 0f to 1f indicator
    var lastShotTime: Long = 0L,
    var pointsScored: Int = 0,
    var targetWayX: Float = 3.5f,
    var targetWayY: Float = 3.5f,
    var patrolTimeLeft: Int = 0,
    var deathResetDelay: Int = 0
)

// Bullet Sparks, Recoil and Blood particle structures
data class SparkParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Int, // Hex integer: 0xFFFF5500 red, etc.
    var ageMax: Int = 15,
    var age: Int = 0
)

data class BloodSplatter(
    val x: Float,
    val y: Float,
    val radius: Float,
    var alpha: Float = 1.0f,
    var age: Int = 0
)

class GameEngine {

    companion object {
         private var sharedEngine: GameEngine? = null
         fun get() = sharedEngine ?: GameEngine().also { sharedEngine = it }
    }

    // Interactive configuration states
    private val _isCampaign = MutableStateFlow(true)
    val isCampaign: StateFlow<Boolean> = _isCampaign.asStateFlow()

    private val _currentChapter = MutableStateFlow(1) // 1 to 4
    val currentChapter: StateFlow<Int> = _currentChapter.asStateFlow()

    private val _skirmishBlueScore = MutableStateFlow(0)
    val skirmishBlueScore: StateFlow<Int> = _skirmishBlueScore.asStateFlow()

    private val _skirmishRedScore = MutableStateFlow(0)
    val skirmishRedScore: StateFlow<Int> = _skirmishRedScore.asStateFlow()

    private val _matchStatus = MutableStateFlow("TUTORIAL") // PLAYING, VICTORY, DEFEAT
    val matchStatus: StateFlow<String> = _matchStatus.asStateFlow()

    // Game Variables
    var playerX = 2.5f
    var playerY = 2.5f
    var playerAngle = 0.5f // Radians
    var playerHealth = 100f
    var playerShield = 100f
    var playerAmmoInClip = 30
    var playerReserveAmmo = 125
    var playerDead = false

    var isFiring = false
    var lastShootTriggerTime = 0L
    var recoilBobbing = 0f
    var muzzleFlashIntensity = 0f
    var isReloading = false
    var reloadTimerFrames = 0

    // Sound / Haptic queues mapped as state to trigger Compose side effects easily
    private val _hapticTriggerFlow = MutableStateFlow<String?>(null)
    val hapticTriggerFlow: StateFlow<String?> = _hapticTriggerFlow.asStateFlow()

    // Particles lists for physical visual haptics rendering on screen canvas
    val sparkList = mutableListOf<SparkParticle>()
    val bloodList = mutableListOf<BloodSplatter>()

    // Current levels walls representation
    var activeMapGrid = MapGridData.campaignMaps[0]

    // Active Mission targets status in Campaign
    var isIntelHarvested = false
    var targetServersRemaining = listOf<Pair<Int, Int>>() // list of server coordinates (X, Y) which must be destroyed
    var bossSpawned = false
    var bossHealth = 400f // ELITE Overlord Boss spawned in Chapter 3
    var bossDefeated = false

    // Active bot soldiers
    val botList = mutableListOf<GameBot>()

    // Local profile setting overrides cached here for instant calculations inside loop
    private var cachedProfile = PlayerProfile()
    private var activeWeapon = WeaponConfig()

    fun resetHapticEffectTrigger() {
        _hapticTriggerFlow.value = null
    }

    fun applyTriggerVibration(context: Context, styleStr: String) {
        // STYLE MAP: "shoot", "hit", "reload"
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            val hapticIntensity = cachedProfile.hapticStrength
            val hapticFrequencyHz = cachedProfile.hapticFrequency

            if (styleStr == "shoot" && cachedProfile.hapticShootEnabled) {
                // Real-world dynamic weapons haptic customisable profiles
                val milliseconds = clamp((styleStr.length * 10f * hapticIntensity).toLong(), 10L, 80L)
                val shootEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val strength = clamp((255 * hapticIntensity).toInt(), 10, 255)
                    VibrationEffect.createOneShot(milliseconds, strength)
                } else {
                    @Suppress("DEPRECATION")
                    VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator.vibrate(shootEffect)
            } else if (styleStr == "reload" && cachedProfile.hapticReloadEnabled) {
                val patterns = longArrayOf(0, 100, 80, 150) // Double click sound haptic
                val reloadEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val strengthArray = intArrayOf(0, clamp((180 * hapticIntensity).toInt(), 10, 255), 0, clamp((240 * hapticIntensity).toInt(), 10, 255))
                    VibrationEffect.createWaveform(patterns, strengthArray, -1)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(patterns, -1)
                    null
                }
                if (reloadEffect != null) vibrator.vibrate(reloadEffect)
            } else if (styleStr == "hit" && cachedProfile.hapticHitEnabled) {
                // Heavy tactical rumble on damage
                val patterns = longArrayOf(0, 200)
                val hitEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val strengthArray = intArrayOf(0, clamp((255 * hapticIntensity).toInt(), 50, 255))
                    VibrationEffect.createWaveform(patterns, strengthArray, -1)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(patterns, -1)
                    null
                }
                if (hitEffect != null) vibrator.vibrate(hitEffect)
            }
        }
    }

    // Initialize Game Parameters based on Mode Selection
    fun initializeSession(isCampaignSelected: Boolean, chapterIndex: Int, profile: PlayerProfile, weapon: WeaponConfig) {
        cachedProfile = profile
        activeWeapon = weapon
        _isCampaign.value = isCampaignSelected
        _currentChapter.value = chapterIndex
        _matchStatus.value = "PLAYING"

        playerHealth = 100f
        playerShield = 100f
        playerAmmoInClip = if (weapon.magazineAttach == "Extended Mag") 45 else 30
        playerReserveAmmo = 120
        playerDead = false
        playerX = 2.5f
        playerY = 2.5f
        playerAngle = 0.5f

        sparkList.clear()
        bloodList.clear()
        botList.clear()

        isIntelHarvested = false
        bossSpawned = false
        bossDefeated = false
        isReloading = false

        if (isCampaignSelected) {
            val mapIdx = clamp(chapterIndex - 1, 0, 3)
            activeMapGrid = MapGridData.campaignMaps[mapIdx]

            // Objective generation based on levels
            if (chapterIndex == 2) {
                // Discover target Server columns (row, col) with ID = 9
                val list = mutableListOf<Pair<Int, Int>>()
                for (r in activeMapGrid.indices) {
                    for (c in activeMapGrid[r].indices) {
                        if (activeMapGrid[r][c] == 9) {
                            list.add(Pair(r, c))
                        }
                    }
                }
                targetServersRemaining = list
            } else if (chapterIndex == 3) {
                bossSpawned = true
                bossHealth = 400f
                // Spawn elite enemy Overlord in the center room
                botList.add(
                    GameBot(
                        id = 99,
                        name = "Apex_Warlord_X",
                        x = 5.5f,
                        y = 5.5f,
                        health = 400f,
                        isEnemy = true,
                        state = BotState.PATROL
                    )
                )
            }

            // Spawn moderate wave of guards in campaign
            spawnGuardsForCampaign(chapterIndex)
        } else {
            // Tactical Skirmish Match Mode Setup
            activeMapGrid = MapGridData.skirmishArena
            _skirmishBlueScore.value = 0
            _skirmishRedScore.value = 0

            // Spawn tactical multiplayer friendly and hostile bots!
            // Friendly team is blue (Team Blue = Player + 2 Friendly Bots)
            botList.add(GameBot(id = 1, name = "Aegis_01", x = 1.5f, y = 14.5f, isEnemy = false))
            botList.add(GameBot(id = 2, name = "Phantom_04", x = 14.5f, y = 14.5f, isEnemy = false))

            // Enemies are red (Team Red = 3 Hostile Bots)
            botList.add(GameBot(id = 10, name = "Specter_Red_A", x = 14.5f, y = 1.5f, isEnemy = true))
            botList.add(GameBot(id = 11, name = "Recon_Red_B", x = 8.5f, y = 8.5f, isEnemy = true))
            botList.add(GameBot(id = 12, name = "Heavy_Red_C", x = 1.5f, y = 1.5f, isEnemy = true))
        }
    }

    private fun spawnGuardsForCampaign(chap: Int) {
        val qty = 2 + chap // more guards higher levels
        var guardsSpawned = 0
        val sizeY = activeMapGrid.size
        val sizeX = activeMapGrid[0].size

        // Find random empty spots on map grid to drop enemy guards
        for (r in 3 until sizeY - 2) {
            for (c in 3 until sizeX - 2) {
                if (activeMapGrid[r][c] == 0 && guardsSpawned < qty) {
                    guardsSpawned++
                    botList.add(
                        GameBot(
                            id = 10 + guardsSpawned,
                            name = "Guard_Alpha_$guardsSpawned",
                            x = c + 0.5f,
                            y = r + 0.5f,
                            health = 80f + 10f * chap,
                            isEnemy = true
                        )
                    )
                }
            }
        }
    }

    // Primary frame ticking update rate. Handled inside Main Thread / Canvas state ticks
    fun updateFrame(moveX: Float, moveY: Float, turnSpeed: Float) {
        if (_matchStatus.value != "PLAYING" || playerDead) return

        // 1. Process player movements (With clean wall collision checks)
        playerAngle += turnSpeed * cachedProfile.aimingSensitivity * 0.05f

        val weightMultiplier = 1.0f / (activeWeapon.weaponWeight.coerceIn(0.4f, 2.0f))
        val spd = 0.08f * weightMultiplier
        val dx = (cos(playerAngle) * moveY - sin(playerAngle) * moveX) * spd
        val dy = (sin(playerAngle) * moveY + cos(playerAngle) * moveX) * spd

        val nextX = playerX + dx
        val nextY = playerY + dy

        // Edge detection checking map bounding
        if (canWalkAt(nextX, playerY)) {
            playerX = nextX
        }
        if (canWalkAt(playerX, nextY)) {
            playerY = nextY
        }

        // Running head-recoil bobbing
        if (abs(dx) > 0.01f || abs(dy) > 0.01f) {
            recoilBobbing += 0.25f
        } else {
            recoilBobbing *= 0.8f // decay naturally
        }

        // 2. Gun flash and reload decays
        if (muzzleFlashIntensity > 0f) {
            muzzleFlashIntensity -= 0.2f
        }

        if (isReloading) {
            reloadTimerFrames--
            if (reloadTimerFrames <= 0) {
                isReloading = false
                val clipCap = if (activeWeapon.magazineAttach == "Extended Mag") 45 else 30
                val need = clipCap - playerAmmoInClip
                val fill = min(need, playerReserveAmmo)
                playerAmmoInClip += fill
                playerReserveAmmo -= fill
            }
        }

        // 3. Update active Bots logic (State transition AI)
        updateBotsAI()

        // 4. Update floating visual particles
        updateParticles()

        // 5. Check Campaign Objective intersections
        checkCampaignObjectives()
    }

    // Trigger Gun shooting action
    fun triggerPlayerShoot(): Boolean {
        if (playerDead || isReloading || _matchStatus.value != "PLAYING") return false

        if (playerAmmoInClip <= 0) {
            triggerReload()
            return false
        }

        val now = System.currentTimeMillis()
        if (now - lastShootTriggerTime < activeWeapon.fireRateMs) return false

        lastShootTriggerTime = now
        playerAmmoInClip--

        // Muzzle burst flash
        muzzleFlashIntensity = 1.0f
        _hapticTriggerFlow.value = "shoot"

        // Weapon feedback pushback recoil
        recoilBobbing += 5.0f

        // Instant Cast Bullet Ray to detect target hits!
        castBulletRay()
        return true
    }

    fun triggerReload() {
        if (isReloading || playerAmmoInClip >= (if (activeWeapon.magazineAttach == "Extended Mag") 45 else 30) || playerReserveAmmo <= 0) return
        isReloading = true
        reloadTimerFrames = 35 // reload frames delay
        _hapticTriggerFlow.value = "reload"
    }

    // Raycast standard projectile trajectory
    private fun castBulletRay() {
        var rx = playerX
        var ry = playerY
        val cosA = cos(playerAngle)
        val sinA = sin(playerAngle)
        val step = 0.1f

        var hitDestructibleIndex = -1
        var hitBotId = -1

        // Ray traveling maximum index range unit
        for (i in 0 until 120) {
            rx += cosA * step
            ry += sinA * step

            val colIdx = rx.toInt()
            val rowIdx = ry.toInt()

            if (colIdx < 0 || colIdx >= activeMapGrid[0].size || rowIdx < 0 || rowIdx >= activeMapGrid.size) {
                break
            }

            // A. Check wall intersections
            val wallType = activeMapGrid[rowIdx][colIdx]
            if (wallType > 0 && wallType != 8) {
                if (wallType == 9) { // Destructible Console Core Server
                    hitDestructibleIndex = rowIdx * 100 + colIdx
                }
                // Generate metal wall sparks
                for (s in 0..4) {
                    sparkList.add(
                        SparkParticle(
                            x = rx,
                            y = ry,
                            vx = (Math.random() - 0.5f).toFloat() * 0.15f,
                            vy = (Math.random() - 0.5f).toFloat() * 0.15f,
                            color = 0xFFFF9900.toInt() // Sparks
                        )
                    )
                }
                break
            }

            // B. Check enemy collision intersections
            var hitOccurred = false
            for (bot in botList) {
                if (bot.health > 0) {
                    val distToBot = sqrt((rx - bot.x).pow(2) + (ry - bot.y).pow(2))
                    if (distToBot < 0.35f) { // Collision hit circumference
                        hitBotId = bot.id
                        hitOccurred = true
                        break
                    }
                }
            }

            if (hitOccurred) {
                // Blood particles simulation
                for (b in 0..5) {
                    sparkList.add(
                        SparkParticle(
                            x = rx,
                            y = ry,
                            vx = (Math.random() - 0.5f).toFloat() * 0.1f,
                            vy = (Math.random() - 0.5f).toFloat() * 0.1f,
                            color = 0xFFFF0000.toInt(), // Blood Red
                            ageMax = 20
                        )
                    )
                }
                bloodList.add(BloodSplatter(rx, ry, 0.12f))
                break
            }
        }

        // Apply hit damage outputs
        if (hitBotId != -1) {
            val dmg = activeWeapon.damagePerBullet
            val targetedBot = botList.find { it.id == hitBotId }
            if (targetedBot != null && targetedBot.health > 0) {
                targetedBot.health -= dmg
                if (isCampaign.value) {
                    targetedBot.state = BotState.CHASE // Turn back and hunt attacker
                }
                if (targetedBot.health <= 0f) {
                    targetedBot.health = 0f
                    targetedBot.state = BotState.DEAD
                    targetedBot.deathResetDelay = 80 // Spawn delay inside skirmishes
                    if (!isCampaign.value) {
                        // multiplayer scoring
                        if (targetedBot.isEnemy) {
                            _skirmishBlueScore.value += 1
                        } else {
                            _skirmishRedScore.value += 1
                        }
                    }
                }
            }
        }

        if (hitDestructibleIndex != -1) {
            val r = hitDestructibleIndex / 100
            val c = hitDestructibleIndex % 100
            if (activeMapGrid[r][c] == 9) {
                activeMapGrid[r][c] = 0 // Console vaporized to empty floor!
                // Trigger profile objective change
                targetServersRemaining = targetServersRemaining.filter { it.first != r || it.second != c }
            }
        }
    }

    private fun checkCampaignObjectives() {
        if (!isCampaign.value) {
            // Skirmish scoring terminal state check (Fast Match limit target: 15)
            if (_skirmishBlueScore.value >= 15) {
                _matchStatus.value = "VICTORY"
            } else if (_skirmishRedScore.value >= 15) {
                _matchStatus.value = "DEFEAT"
            }
            return
        }

        val px = playerX.toInt()
        val py = playerY.toInt()

        // Chapter 1 Intel laptop spot
        if (currentChapter.value == 1) {
            if (px == 10 && py == 1 && !isIntelHarvested) {
                isIntelHarvested = true
                // Open visual indicators
            }
        }

        // Chapter 3 Boss eradication
        if (currentChapter.value == 3) {
            val boss = botList.find { it.id == 99 }
            if (boss == null || boss.health <= 0f) {
                bossDefeated = true
            }
        }

        // Check level objective location intersection (Cell code 8: Extraction pod gate)
        if (px >= 0 && px < activeMapGrid[0].size && py >= 0 && py < activeMapGrid.size) {
            if (activeMapGrid[py][px] == 8) {
                var canExtract = false
                when (currentChapter.value) {
                    1 -> if (isIntelHarvested) canExtract = true
                    2 -> if (targetServersRemaining.isEmpty()) canExtract = true
                    3 -> if (bossDefeated) canExtract = true
                    4 -> if (botList.none { it.health > 0 }) canExtract = true
                }
                if (canExtract) {
                    _matchStatus.value = "VICTORY"
                }
            }
        }
    }

    private fun updateBotsAI() {
        val now = System.currentTimeMillis()
        for (bot in botList) {
            if (bot.health <= 0f) {
                bot.state = BotState.DEAD
                if (!isCampaign.value) {
                    // Respawn logic inside tactical Bot Matchmaking Skirmish Mode!
                    bot.deathResetDelay--
                    if (bot.deathResetDelay <= 0) {
                        bot.health = 100f
                        bot.state = BotState.PATROL
                        // Random drop spot in Skirmish map
                        bot.x = if (bot.isEnemy) 1.5f else 14.5f
                        bot.y = if (bot.isEnemy) 1.5f else 14.5f
                    }
                }
                continue
            }

            // Distances mapping
            val distToPlayer = sqrt((playerX - bot.x).pow(2) + (playerY - bot.y).pow(2))

            if (isCampaign.value) {
                // CAMPAIGN AI: Single Player enemies HUNT Player
                bot.angle = atan2(playerY - bot.y, playerX - bot.x)

                if (distToPlayer < 7.0f) {
                    if (distToPlayer < 3.0f) {
                        bot.state = BotState.SHOOT
                    } else {
                        bot.state = BotState.CHASE
                    }
                } else {
                    bot.state = BotState.PATROL
                }

                // Execute states
                if (bot.state == BotState.CHASE) {
                    val step = 0.035f
                    val nx = bot.x + cos(bot.angle) * step
                    val ny = bot.y + sin(bot.angle) * step
                    if (canWalkAt(nx, bot.y)) bot.x = nx
                    if (canWalkAt(bot.x, ny)) bot.y = ny
                } else if (bot.state == BotState.SHOOT) {
                    if (now - bot.lastShotTime > 700L) {
                        bot.lastShotTime = now
                        shootAtPlayer()
                    }
                }
            } else {
                // SKIRMISH TACTICAL AI: Multiplayer simulation bots hunt the opposite team!
                // Enemy bots search for Player or Free-standing Blue Friendly bots.
                // Friendly bots search for Team Red target bots.
                var targetAgentX = playerX
                var targetAgentY = playerY
                var minTargetDist = distToPlayer
                var opponentFound = true

                if (bot.isEnemy) {
                    // Enemy target is either Player or Blue friendly bot
                    for (friend in botList.filter { !it.isEnemy }) {
                        if (friend.health > 0) {
                            val dist = sqrt((friend.x - bot.x).pow(2) + (friend.y - bot.y).pow(2))
                            if (dist < minTargetDist) {
                                minTargetDist = dist
                                targetAgentX = friend.x
                                targetAgentY = friend.y
                            }
                        }
                    }
                } else {
                    // Friendly bot target is any active Red Enemy bot
                    opponentFound = false
                    minTargetDist = 999f
                    for (enemy in botList.filter { it.isEnemy }) {
                        if (enemy.health > 0) {
                            val dist = sqrt((enemy.x - bot.x).pow(2) + (enemy.y - bot.y).pow(2))
                            if (dist < minTargetDist) {
                                minTargetDist = dist
                                targetAgentX = enemy.x
                                targetAgentY = enemy.y
                                opponentFound = true
                            }
                        }
                    }
                }

                if (opponentFound || bot.isEnemy) {
                    bot.angle = atan2(targetAgentY - bot.y, targetAgentX - bot.x)
                    if (minTargetDist < 3.2f) {
                        bot.state = BotState.SHOOT
                        if (now - bot.lastShotTime > 800L) {
                            bot.lastShotTime = now
                            if (bot.isEnemy && targetAgentX == playerX && targetAgentY == playerY) {
                                shootAtPlayer()
                            } else {
                                // Simulate bot shooting another bot
                                if (bot.isEnemy) {
                                    val victim = botList.find { !it.isEnemy && it.health > 0 && sqrt((it.x - bot.x).pow(2) + (it.y - bot.y).pow(2)) < 4f }
                                    if (victim != null) {
                                        victim.health -= 15f
                                        if (victim.health <= 0f) {
                                            victim.health = 0f
                                            victim.state = BotState.DEAD
                                            victim.deathResetDelay = 80
                                            _skirmishRedScore.value += 1
                                        }
                                    }
                                } else {
                                    val victim = botList.find { it.isEnemy && it.health > 0 && sqrt((it.x - bot.x).pow(2) + (it.y - bot.y).pow(2)) < 4f }
                                    if (victim != null) {
                                        victim.health -= 15f
                                        if (victim.health <= 0f) {
                                            victim.health = 0f
                                            victim.state = BotState.DEAD
                                            victim.deathResetDelay = 80
                                            _skirmishBlueScore.value += 1
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        bot.state = BotState.CHASE
                        val step = 0.04f
                        val nx = bot.x + cos(bot.angle) * step
                        val ny = bot.y + sin(bot.angle) * step
                        if (canWalkAt(nx, bot.y)) bot.x = nx
                        if (canWalkAt(bot.x, ny)) bot.y = ny
                    }
                } else {
                    bot.state = BotState.PATROL
                    // Move around to predefined waypoints
                    if (bot.patrolTimeLeft <= 0) {
                        bot.targetWayX = (2 until activeMapGrid[0].size - 2).random() + 0.5f
                        bot.targetWayY = (2 until activeMapGrid.size - 2).random() + 0.5f
                        bot.patrolTimeLeft = 100
                    } else {
                        bot.patrolTimeLeft--
                        val rot = atan2(bot.targetWayY - bot.y, bot.targetWayX - bot.x)
                        bot.angle = rot
                        val step = 0.03f
                        val nx = bot.x + cos(rot) * step
                        val ny = bot.y + sin(rot) * step
                        if (canWalkAt(nx, bot.y)) bot.x = nx
                        if (canWalkAt(bot.x, ny)) bot.y = ny
                    }
                }
            }
        }
    }

    private fun shootAtPlayer() {
        if (playerDead) return

        // Bullet hit calculation based on player velocity and distance (90% accuracy index)
        val rnd = Math.random()
        if (rnd < 0.38f) { // Bullet hits player shield/health!
            _hapticTriggerFlow.value = "hit"
            
            val dmg = 12f * currentChapter.value.coerceAtMost(3)
            if (playerShield > 0f) {
                playerShield -= dmg
                if (playerShield < 0f) {
                    playerHealth += playerShield // subtract residue
                    playerShield = 0f
                }
            } else {
                playerHealth -= dmg
            }

            if (playerHealth <= 0f) {
                playerHealth = 0f
                playerDead = true
                _matchStatus.value = "DEFEAT"
            }
        }
    }

    private fun updateParticles() {
        // Redraw/calculate spark positions
        val sparkIter = sparkList.iterator()
        while (sparkIter.hasNext()) {
            val spark = sparkIter.next()
            spark.age++
            if (spark.age >= spark.ageMax) {
                sparkIter.remove()
            } else {
                // Apply velocity vector
                // Gravity bias if needed
            }
        }

        // Dissolve wall-stuck blood splatters over time
        val bloodIter = bloodList.iterator()
        while (bloodIter.hasNext()) {
            val blood = bloodIter.next()
            blood.age++
            blood.alpha = (1.0f - blood.age / 120.0f).coerceIn(0.0f, 1.0f)
            if (blood.age >= 120) {
                bloodIter.remove()
            }
        }
    }

    private fun canWalkAt(tx: Float, ty: Float): Boolean {
        val col = tx.toInt()
        val row = ty.toInt()
        if (col < 0 || col >= activeMapGrid[0].size || row < 0 || row >= activeMapGrid.size) return false
        val cellVal = activeMapGrid[row][col]

        // 0: floor, 8: extraction portal. Player can step on objective zones
        return cellVal == 0 || cellVal == 8
    }

    private fun clamp(value: Long, min: Long, max: Long): Long {
        return max(min, min(max, value))
    }

    private fun clamp(value: Int, min: Int, max: Int): Int {
        return max(min, min(max, value))
    }
}
