package com.example

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.PlayerProfile
import com.example.data.WeaponConfig
import com.example.game.GameEngine
import com.example.ui.StrikeOpsMainScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    private lateinit var repository: GameRepository
    private var isGamepadConnectedState = mutableStateOf(false)
    private var lastGamepadEventTextState = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Room Local Persistence Database
        val database = GameDatabase.getDatabase(this)
        repository = GameRepository(database.gameDao())

        // 2. Populate standard weapons configurations on first-turn boot
        lifecycleScope.launch {
            val ex = repository.weaponConfigs.firstOrNull()
            repository.populateDefaultsIfNeeded(ex != null && ex.isNotEmpty())
            
            // Check connected gamepad devices at start
            isGamepadConnectedState.value = checkControllerConnected()
        }

        setContent {
            MyApplicationTheme {
                val profileState = repository.playerProfile.collectAsState(initial = null)
                val weaponsState = repository.weaponConfigs.collectAsState(initial = emptyList())

                val activeProfile = profileState.value ?: PlayerProfile()

                StrikeOpsMainScreen(
                    profile = activeProfile,
                    weapons = weaponsState.value,
                    onSaveProfile = { updatedProfile ->
                        lifecycleScope.launch {
                            repository.updateProfile(updatedProfile)
                        }
                    },
                    onSaveWeapon = { updatedWeapon ->
                        lifecycleScope.launch {
                            repository.updateWeaponConfig(updatedWeapon)
                        }
                    },
                    isGamepadConnected = isGamepadConnectedState.value,
                    lastGamepadEventText = lastGamepadEventTextState.value
                )
            }
        }
    }

    // A. Intercept joystick motion triggers from physical gamepads
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val source = event.source
        if ((source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
            event.action == MotionEvent.ACTION_MOVE
        ) {
            isGamepadConnectedState.value = true

            // Gather movements
            val moveX = event.getAxisValue(MotionEvent.AXIS_X)
            val moveY = -event.getAxisValue(MotionEvent.AXIS_Y) // invert standard analog coordinates

            // Gather rotations
            val turnX = event.getAxisValue(MotionEvent.AXIS_Z) // Standard right-stick Yaw axis
            val turnY = event.getAxisValue(MotionEvent.AXIS_RZ) // Pitch axis

            val gameEngine = GameEngine.get()
            if (gameEngine.matchStatus.value == "PLAYING" && !gameEngine.playerDead) {
                // Instantly apply movements to map boundaries with collision checks
                val weightMultiplier = 1.0f
                val spd = 0.08f * weightMultiplier
                val dx = (cos(gameEngine.playerAngle) * moveY - sin(gameEngine.playerAngle) * moveX) * spd
                val dy = (sin(gameEngine.playerAngle) * moveY + cos(gameEngine.playerAngle) * moveX) * spd

                // Basic bounds collision checkers
                val nextX = gameEngine.playerX + dx
                val nextY = gameEngine.playerY + dy

                val mapHeight = gameEngine.activeMapGrid.size
                val mapWidth = gameEngine.activeMapGrid[0].size

                fun canWalkAt(tx: Float, ty: Float): Boolean {
                    val col = tx.toInt()
                    val row = ty.toInt()
                    if (col < 0 || col >= mapWidth || row < 0 || row >= mapHeight) return false
                    val cellVal = gameEngine.activeMapGrid[row][col]
                    return cellVal == 0 || cellVal == 8
                }

                if (canWalkAt(nextX, gameEngine.playerY)) {
                    gameEngine.playerX = nextX
                }
                if (canWalkAt(gameEngine.playerX, nextY)) {
                    gameEngine.playerY = nextY
                }

                // Camera turning yaw modifier
                gameEngine.playerAngle += turnX * 0.045f

                lastGamepadEventTextState.value = "L-Stick (${"%.2f".format(moveX)}, ${"%.2f".format(moveY)}) | R-Stick (${"%.2f".format(turnX)}, ${"%.2f".format(turnY)})"
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }

    // B. Intercept push buttons (triggers, bumpers, reloads)
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        isGamepadConnectedState.value = true
        val gameEngine = GameEngine.get()
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_DPAD_CENTER -> {
                gameEngine.triggerPlayerShoot()
                lastGamepadEventTextState.value = "Key Signal: Shoot [R2/Space] Triggered"
                return true
            }
            KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_R -> {
                gameEngine.triggerReload()
                lastGamepadEventTextState.value = "Key Signal: Reload [Y/R-Key] Triggered"
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun checkControllerConnected(): Boolean {
        val devices = InputDevice.getDeviceIds()
        for (id in devices) {
            val device = InputDevice.getDevice(id)
            val sources = device?.sources ?: 0
            if ((sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            ) {
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        isGamepadConnectedState.value = checkControllerConnected()
    }
}
