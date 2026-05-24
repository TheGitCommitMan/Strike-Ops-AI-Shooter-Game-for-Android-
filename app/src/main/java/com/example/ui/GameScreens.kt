package com.example.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlayerProfile
import com.example.data.WeaponConfig
import com.example.game.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

// UI Color tokens
val TechGreen = Color(0xFF00FFCC)
val DarkGreen = Color(0xFF0D2C22)
val NeonOrange = Color(0xFFFF6600)
val CyberDark = Color(0xFF020907)
val CardBackground = Color(0xFF081511)
val AccentTeal = Color(0xFF00A383)

@Composable
fun StrikeOpsMainScreen(
    profile: PlayerProfile,
    weapons: List<WeaponConfig>,
    onSaveProfile: (PlayerProfile) -> Unit,
    onSaveWeapon: (WeaponConfig) -> Unit,
    isGamepadConnected: Boolean,
    lastGamepadEventText: String
) {
    var activeTab by remember { mutableStateOf("MENU") } // MENU, CAMPAIGN, SKIRMISH, GUNSMITH, CONTROLS, PROFILE, GAME
    var selectedChapterForLaunch by remember { mutableStateOf(1) }
    var selectedWeaponConfig by remember { mutableStateOf<WeaponConfig?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val gameEngine = remember { GameEngine.get() }

    // Synchronize selected weapon if null or loaded at start
    if (selectedWeaponConfig == null && weapons.isNotEmpty()) {
        selectedWeaponConfig = weapons.first()
    }

    LaunchedEffect(key1 = weapons) {
        if (selectedWeaponConfig == null && weapons.isNotEmpty()) {
            selectedWeaponConfig = weapons.first()
        }
    }

    // Capture haptics requested from GameEngine
    val hapticCode by gameEngine.hapticTriggerFlow.collectAsState()
    LaunchedEffect(hapticCode) {
        hapticCode?.let {
            gameEngine.applyTriggerVibration(context, it)
            gameEngine.resetHapticEffectTrigger()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberDark)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Subtle cyber-grid lines in background
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.08f)) {
            val stepX = 40.dp.toPx()
            val stepY = 40.dp.toPx()
            for (x in 0 until (size.width / stepX).toInt()) {
                drawLine(
                    Color(0xFF00FFCC),
                    Offset(x * stepX, 0f),
                    Offset(x * stepX, size.height),
                    strokeWidth = 1f
                )
            }
            for (y in 0 until (size.height / stepY).toInt()) {
                drawLine(
                    Color(0xFF00FFCC),
                    Offset(0f, y * stepY),
                    Offset(size.width, y * stepY),
                    strokeWidth = 1f
                )
            }
        }

        // Animated Screen Routing System
        AnimatedContent(
            targetState = activeTab,
            transitionSpec = {
                fadeIn(spring()) togetherWith fadeOut(spring())
            },
            label = "ScreenTransition"
        ) { tabState ->
            when (tabState) {
                "MENU" -> MainMenuScreen(
                    profile = profile,
                    isGamepadConnected = isGamepadConnected,
                    onNavigateCampaign = { activeTab = "CAMPAIGN" },
                    onNavigateSkirmish = { activeTab = "SKIRMISH" },
                    onNavigateGunsmith = { activeTab = "GUNSMITH" },
                    onNavigateControls = { activeTab = "CONTROLS" },
                    onNavigateProfile = { activeTab = "PROFILE" }
                )
                "CAMPAIGN" -> CampaignBriefingScreen(
                    profile = profile,
                    onBack = { activeTab = "MENU" },
                    onChapterSelect = { chapVal ->
                        selectedChapterForLaunch = chapVal
                        val activeWep = selectedWeaponConfig ?: weapons.firstOrNull() ?: WeaponConfig()
                        gameEngine.initializeSession(true, chapVal, profile, activeWep)
                        activeTab = "GAME"
                    }
                )
                "SKIRMISH" -> SkirmishMatchSetupScreen(
                    profile = profile,
                    weapons = weapons,
                    selectedWep = selectedWeaponConfig ?: weapons.firstOrNull() ?: WeaponConfig(),
                    onSelectWep = { selectedWeaponConfig = it },
                    onBack = { activeTab = "MENU" },
                    onLaunchMatch = {
                        val activeWep = selectedWeaponConfig ?: weapons.firstOrNull() ?: WeaponConfig()
                        gameEngine.initializeSession(false, 1, profile, activeWep)
                        activeTab = "GAME"
                    }
                )
                "GUNSMITH" -> GunsmithScreen(
                    weapons = weapons,
                    selectedWep = selectedWeaponConfig ?: weapons.firstOrNull() ?: WeaponConfig(),
                    onSelectWep = { selectedWeaponConfig = it },
                    onUpdateConfig = { config ->
                        onSaveWeapon(config)
                        selectedWeaponConfig = config
                    },
                    onBack = { activeTab = "MENU" }
                )
                "CONTROLS" -> TouchControlsErgonomicsScreen(
                    profile = profile,
                    onSaveProfile = { onSaveProfile(it); gameEngine.applyTriggerVibration(context, "reload") },
                    isGamepadConnected = isGamepadConnected,
                    lastGamepadEventText = lastGamepadEventText,
                    onBack = { activeTab = "MENU" }
                )
                "PROFILE" -> CareersScreen(
                    profile = profile,
                    onBack = { activeTab = "MENU" },
                    onResetStats = {
                        onSaveProfile(PlayerProfile(id = 1, name = profile.name))
                    }
                )
                "GAME" -> ActiveGamePlaygroundScreen(
                    profile = profile,
                    activeWep = selectedWeaponConfig ?: weapons.firstOrNull() ?: WeaponConfig(),
                    gameEngine = gameEngine,
                    isGamepadConnected = isGamepadConnected,
                    onBackToMenu = {
                        activeTab = "MENU"
                    },
                    onMatchEnded = { outcome ->
                        if (outcome == "VICTORY" && gameEngine.isCampaign.value) {
                            // Upgrade profile level/XP and next unlocked sector
                            val xpGained = gameEngine.currentChapter.value * 250
                            val nextChapter = if (profile.campaignChapter <= gameEngine.currentChapter.value) {
                                (gameEngine.currentChapter.value + 1).coerceAtMost(4)
                            } else {
                                profile.campaignChapter
                            }
                            val updatedProfile = profile.copy(
                                xp = profile.xp + xpGained,
                                level = if (profile.xp + xpGained > profile.level * 1000) profile.level + 1 else profile.level,
                                wins = profile.wins + 1,
                                campaignChapter = nextChapter,
                                totalKills = profile.totalKills + (gameEngine.botList.filter { it.state == BotState.DEAD }.size)
                            )
                            onSaveProfile(updatedProfile)
                        } else if (outcome == "DEFEAT") {
                            onSaveProfile(profile.copy(losses = profile.losses + 1))
                        }
                        activeTab = "MENU"
                    }
                )
            }
        }
    }
}

// 1. Core Title Menu Section
@Composable
fun MainMenuScreen(
    profile: PlayerProfile,
    isGamepadConnected: Boolean,
    onNavigateCampaign: () -> Unit,
    onNavigateSkirmish: () -> Unit,
    onNavigateGunsmith: () -> Unit,
    onNavigateControls: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App top identity info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "STRIKE OPS",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = TechGreen,
                        letterSpacing = 4.sp
                    )
                )
                Text(
                    text = "AAA TACTICAL OFFLINE MATCHES",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Connection card indicator
            Surface(
                color = if (isGamepadConnected) DarkGreen else Color.DarkGray,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.border(1.dp, if (isGamepadConnected) TechGreen else Color.Transparent, RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isGamepadConnected) Icons.Default.VideogameAsset else Icons.Default.StayCurrentPortrait,
                        contentDescription = "Device info icon",
                        tint = if (isGamepadConnected) TechGreen else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isGamepadConnected) "CONTROLLER ACTIVE" else "TOUCH GAMEPAD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGamepadConnected) TechGreen else Color.LightGray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Substantial Center Banner or Weapon visual representation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CardBackground)
                .border(1.dp, TechGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Visual decorative game radar scanner grid in the background
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = TechGreen,
                    radius = size.width / 4,
                    center = Offset(size.width / 2, size.height / 2),
                    style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                )
                drawCircle(
                    color = TechGreen,
                    radius = size.width / 2,
                    center = Offset(size.width / 2, size.height / 2),
                    style = Stroke(width = 1.5f)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CrisisAlert,
                    contentDescription = "Radar Crosshair decoration",
                    tint = TechGreen,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "OPERATOR: ${profile.name.uppercase()}",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "LEVEL ${profile.level} TACTICIAN",
                    color = TechGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Action Menu Navigation Rows
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onNavigateCampaign,
                    modifier = Modifier.weight(1f).height(54.dp).testTag("campaign_mode_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = TechGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = CyberDark)
                        Text("ROGUE CAMPAIGN", fontWeight = FontWeight.Black, color = CyberDark, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    }
                }

                Button(
                    onClick = onNavigateSkirmish,
                    modifier = Modifier.weight(1f).height(54.dp).testTag("skirmish_mode_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.SportsEspoints, contentDescription = null, tint = Color.White)
                        Text("BOT MULTIPLAYER", fontWeight = FontWeight.Black, color = Color.White, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onNavigateGunsmith,
                    modifier = Modifier.weight(0.9f).height(48.dp).testTag("gunsmith_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, TechGreen.copy(alpha = 0.4f))
                ) {
                    Icon(imageVector = Icons.Default.SettingsSuggest, contentDescription = null, tint = TechGreen)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("GUNSMITH LOADOUTS", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = onNavigateControls,
                    modifier = Modifier.weight(1.1f).height(48.dp).testTag("controls_and_haptics_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, TechGreen.copy(alpha = 0.4f))
                ) {
                    Icon(imageVector = Icons.Default.SettingsInputGamepad, contentDescription = null, tint = TechGreen)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CONTROL & HAPTIC SYSTEM", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                IconButton(
                    onClick = onNavigateProfile,
                    modifier = Modifier.size(48.dp).background(CardBackground, RoundedCornerShape(8.dp)).border(1.dp, TechGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    Icon(imageVector = Icons.Default.ContactPage, contentDescription = "Profile icon log", tint = TechGreen)
                }
            }
        }
    }
}

// 2. Campaign Level Briefings Sub-Screen
@Composable
fun CampaignBriefingScreen(
    profile: PlayerProfile,
    onBack: () -> Unit,
    onChapterSelect: (Int) -> Unit
) {
    val chapters = listOf(
        Triple(1, "SECTOR 7 BREACH", "Intel Extraction - Locate the decrypted terminal inside Intel Command and extract. Expect heavy tactical counter-ops surveillance!"),
        Triple(2, "ROGUE LABS", "Data Clear Wipe - Destroy the 3 flashing command server racks by shooting them directly on sight, and escape through the southern gate."),
        Triple(3, "ARMORY DEPOT BOOTY", "Assent Overlord Raid - Track down the elite heavy warlord commanding deep inside Sector 3 bunkers. Eradicate him to secure peace."),
        Triple(4, "APEX FORTIFIED HEADQUARTERS", "Final Elimination Conquest - Hostiles have barricaded the extraction mainframe. Eliminate every remaining mercenary guard on-site.")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, TechGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TechGreen)
                Spacer(modifier = Modifier.width(4.dp))
                Text("MAIN MENU", color = Color.White, fontFamily = FontFamily.Monospace)
            }

            Text(
                text = "OPERATIONS SELECT",
                color = TechGreen,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chapters) { item ->
                val (num, title, desc) = item
                val isUnlocked = profile.campaignChapter >= num

                Surface(
                    color = if (isUnlocked) CardBackground else Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isUnlocked) TechGreen.copy(alpha = 0.5f) else Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (isUnlocked) TechGreen else Color.DarkGray,
                                    shape = CircleShape,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$num",
                                            color = if (isUnlocked) CyberDark else Color.Gray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUnlocked) Color.White else Color.Gray,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = desc,
                                fontSize = 11.sp,
                                color = if (isUnlocked) Color.LightGray else Color.DarkGray,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        if (isUnlocked) {
                            Button(
                                onClick = { onChapterSelect(num) },
                                colors = ButtonDefaults.buttonColors(containerColor = TechGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("LAUNCH", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LOCKED", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "TACTICAL BRIEFING: All campaign missions support physical controller layout mapping and dynamic haptics configuration feedback overlays.",
            fontSize = 10.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// 3. Tactical Skirmish Setup tab
@Composable
fun SkirmishMatchSetupScreen(
    profile: PlayerProfile,
    weapons: List<WeaponConfig>,
    selectedWep: WeaponConfig,
    onSelectWep: (WeaponConfig) -> Unit,
    onBack: () -> Unit,
    onLaunchMatch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, TechGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TechGreen)
                Spacer(modifier = Modifier.width(4.dp))
                Text("MENU", color = Color.White, fontFamily = FontFamily.Monospace)
            }

            Text(
                text = "MULTPLAYER SIM (OFFLINE BOTS)",
                color = TechGreen,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }

        // Configuration Panel
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = CardBackground,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TechGreen.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MATCH FORMAT",
                        color = TechGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "3v3 Team Deathmatch (Symmetric Arena Map)\nTarget Kills: 15\nTeam Blue: You + 2 tactical AI Recruits\nTeam Red: 3 Veteran Opponent AI bots that take cover, flank, and shoot.",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Weapon Loadout selection inside setup
            Text(
                text = "CHOOSE ACTIVE WEAPON LOADOUT IN SKIRMISH:",
                color = TechGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(weapons) { wep ->
                    val isSelected = selectedWep.id == wep.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectWep(wep) },
                        color = if (isSelected) TechGreen.copy(alpha = 0.08f) else CardBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) TechGreen else TechGreen.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = wep.weaponName,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Class: ${wep.baseType}  |  Optic: ${wep.opticalAttach}  |  Muzzle: ${wep.muzzleAttach}",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected loadout",
                                    tint = TechGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Play
        Button(
            onClick = onLaunchMatch,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TechGreen),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Launch play match", tint = CyberDark)
            Spacer(modifier = Modifier.width(8.dp))
            Text("DEPLOY TO COMBAT ZONE", fontWeight = FontWeight.Black, color = CyberDark, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        }
    }
}

// 4. Weapon Customization: Gunsmith System
@Composable
fun GunsmithScreen(
    weapons: List<WeaponConfig>,
    selectedWep: WeaponConfig,
    onSelectWep: (WeaponConfig) -> Unit,
    onUpdateConfig: (WeaponConfig) -> Unit,
    onBack: () -> Unit
) {
    val optics = listOf("Holographic", "Red Dot", "ACOG 4x", "None")
    val muzzles = listOf("Heavy Brake", "Silencer", "Stabilizing Compensator", "None")
    val magazines = listOf("Extended Mag", "Fast Mag", "None")
    val stocks = listOf("Tactical Stock", "Folding Stock", "Precision Stocks", "None")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, TechGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back icon", tint = TechGreen)
                Spacer(modifier = Modifier.width(4.dp))
                Text("MENU", color = Color.White, fontFamily = FontFamily.Monospace)
            }

            Text(
                text = "TAC-GUNSMITH INTERFACE",
                color = TechGreen,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }

        // Row of current weapons choices
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            weapons.forEach { wep ->
                val isSel = wep.id == selectedWep.id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isSel) TechGreen.copy(alpha = 0.15f) else CardBackground, RoundedCornerShape(6.dp))
                        .border(1.dp, if (isSel) TechGreen else Color.DarkGray, RoundedCornerShape(6.dp))
                        .clickable { onSelectWep(wep) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = wep.weaponName.split(" ").lastOrNull() ?: wep.weaponName,
                        color = if (isSel) TechGreen else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // CAD Grid Design Blueprint Canvas Representation of gun!
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color(0xFF020C09), RoundedCornerShape(10.dp))
                .border(2.dp, Color(0xFF00FFCC).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background technical grid lines lines
                for (i in 0 until (size.height / 10f).toInt()) {
                    drawLine(Color(0xFF00FFCC).copy(alpha = 0.04f), Offset(0f, i * 10f), Offset(size.width, i * 10f))
                }
                for (i in 0 until (size.width / 10f).toInt()) {
                    drawLine(Color(0xFF00FFCC).copy(alpha = 0.04f), Offset(i * 10f, 0f), Offset(i * 10f, size.height))
                }

                // Call custom technical gun Blueprint silhouette lines!
                val p = Path()
                p.moveTo(size.width * 0.15f, size.height * 0.45f) // Stock
                p.lineTo(size.width * 0.3f, size.height * 0.45f) // Stock rise
                p.lineTo(size.width * 0.4f, size.height * 0.5f)  // Receiver back
                p.lineTo(size.width * 0.75f, size.height * 0.5f) // Barrel top
                p.lineTo(size.width * 0.82f, size.height * 0.48f) // Muzzle Brake
                p.lineTo(size.width * 0.82f, size.height * 0.55f)
                p.lineTo(size.width * 0.75f, size.height * 0.53f) // Barrel bottom
                p.lineTo(size.width * 0.55f, size.height * 0.54f) // Handguard
                p.lineTo(size.width * 0.45f, size.height * 0.65f) // Pistol grip
                p.lineTo(size.width * 0.4f, size.height * 0.65f)
                p.lineTo(size.width * 0.38f, size.height * 0.55f)
                p.lineTo(size.width * 0.15f, size.height * 0.55f) // Stock bottom
                p.close()

                drawPath(p, Color(0xFF00FFCC), alpha = 0.8f, style = Stroke(width = 3f))

                // Optional Optic indicator bubble on blueprint
                if (selectedWep.opticalAttach != "None") {
                    drawRect(
                        color = NeonOrange,
                        topLeft = Offset(size.width * 0.42f, size.height * 0.42f),
                        size = Size(25.dp.toPx(), 12.dp.toPx()),
                        style = Stroke(width = 2f)
                    )
                    drawCircle(NeonOrange, radius = 5f, center = Offset(size.width * 0.42f + 12.dp.toPx(), size.height * 0.42f + 6.dp.toPx()))
                }
                // Silencer indicator on muzzle path
                if (selectedWep.muzzleAttach == "Silencer") {
                    drawOval(
                        color = Color.LightGray,
                        topLeft = Offset(size.width * 0.82f, size.height * 0.45f),
                        size = Size(35f, 20f),
                        style = Stroke(width = 2f)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "SPECIFICATIONS COMPLY:",
                    color = TechGreen.copy(alpha = 0.6f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${selectedWep.weaponName}  [ ${selectedWep.baseType} ]",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Customizable Dropdown controls and Stats Bars
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("TACTICAL BLUEPRINT ATTACHMENTS:", color = TechGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            // A. Scope attachment selection row
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("OPTICAL:", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(90.dp), fontFamily = FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        optics.forEach { item ->
                            val sel = selectedWep.opticalAttach == item
                            Box(
                                modifier = Modifier
                                    .background(if (sel) TechGreen else CardBackground, RoundedCornerShape(4.dp))
                                    .clickable { onUpdateConfig(selectedWep.copy(opticalAttach = item)) }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Text(item, fontSize = 10.sp, color = if (sel) CyberDark else Color.LightGray, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            // B. Muzzle attachment selection row
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("MUZZLE:", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(90.dp), fontFamily = FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        muzzles.take(3).forEach { item -> // Limit space
                            val sel = selectedWep.muzzleAttach == item
                            Box(
                                modifier = Modifier
                                    .background(if (sel) TechGreen else CardBackground, RoundedCornerShape(4.dp))
                                    .clickable { onUpdateConfig(selectedWep.copy(muzzleAttach = item)) }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Text(item.split(" ").firstOrNull() ?: item, fontSize = 10.sp, color = if (sel) CyberDark else Color.LightGray, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            // C. Stock selection row
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("STOCK TYPE:", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(90.dp), fontFamily = FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        stocks.take(3).forEach { item ->
                            val sel = selectedWep.stockAttach == item
                            Box(
                                modifier = Modifier
                                    .background(if (sel) TechGreen else CardBackground, RoundedCornerShape(4.dp))
                                    .clickable { onUpdateConfig(selectedWep.copy(stockAttach = item)) }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Text(item.split(" ").firstOrNull() ?: item, fontSize = 10.sp, color = if (sel) CyberDark else Color.LightGray, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            // D. Magazine selection row
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("MAGAZINE:", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(90.dp), fontFamily = FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        magazines.forEach { item ->
                            val sel = selectedWep.magazineAttach == item
                            Box(
                                modifier = Modifier
                                    .background(if (sel) TechGreen else CardBackground, RoundedCornerShape(4.dp))
                                    .clickable { onUpdateConfig(selectedWep.copy(magazineAttach = item)) }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Text(item, fontSize = 10.sp, color = if (sel) CyberDark else Color.LightGray, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            // Dynamic specifications indicator graphs
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(CardBackground, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("DYNAMIC ATTRIBUTES METRIC:", color = TechGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                    // Stability Recoil spec
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("STABILITY:", color = Color.White, fontSize = 10.sp, modifier = Modifier.width(80.dp), fontFamily = FontFamily.Monospace)
                        LinearProgressIndicator(
                            progress = { selectedWep.stabilityRatio },
                            modifier = Modifier.weight(1f).height(6.dp),
                            color = TechGreen,
                            trackColor = Color.DarkGray,
                        )
                    }

                    // Weight Speed spec
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("WEIGHT SLOW:", color = Color.White, fontSize = 10.sp, modifier = Modifier.width(80.dp), fontFamily = FontFamily.Monospace)
                        LinearProgressIndicator(
                            progress = { selectedWep.weaponWeight / 1.5f },
                            modifier = Modifier.weight(1f).height(6.dp),
                            color = NeonOrange,
                            trackColor = Color.DarkGray,
                        )
                    }

                    // Fire rate spec
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("DAMAGE RATIO:", color = Color.White, fontSize = 10.sp, modifier = Modifier.width(80.dp), fontFamily = FontFamily.Monospace)
                        LinearProgressIndicator(
                            progress = { (selectedWep.damagePerBullet / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.weight(1f).height(6.dp),
                            color = TechGreen,
                            trackColor = Color.DarkGray,
                        )
                    }
                }
            }
        }
    }
}

// 5. Controls & Ergonomics Customization Tab
// Allows you to change haptic vibration configurations & literally drag touchscreen controller buttons around!
@Composable
fun TouchControlsErgonomicsScreen(
    profile: PlayerProfile,
    onSaveProfile: (PlayerProfile) -> Unit,
    isGamepadConnected: Boolean,
    lastGamepadEventText: String,
    onBack: () -> Unit
) {
    var editJoystickMode by remember { mutableStateOf(false) }

    // Visual placement state
    var moveJoyOffset by remember { mutableStateOf(Offset(profile.moveJoyX, profile.moveJoyY)) }
    var aimJoyOffset by remember { mutableStateOf(Offset(profile.aimJoyX, profile.aimJoyY)) }
    var fireBtnOffset by remember { mutableStateOf(Offset(profile.fireBtnX, profile.fireBtnY)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, TechGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back back", tint = TechGreen)
                Spacer(modifier = Modifier.width(4.dp))
                Text("MENU", color = Color.White, fontFamily = FontFamily.Monospace)
            }

            Text(
                text = "SYSTEM ENGINE CONSOLE",
                color = TechGreen,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (editJoystickMode) {
            // HIGH-FIDELITY INTERACTIVE LAYOUT DESIGN CANVAS WORKSPACE
            // Users can directly drag elements to configure their exact preferred claw layout!
            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Surface(
                    color = CardBackground,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, TechGreen)
                ) {
                    Text(
                        text = "TOUCH DRAG BUTTONS TO EDIT ERGONOMICS LAYOUT:",
                        color = TechGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp)
                        .background(Color(0xFF040B0A), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                ) {
                    // 1. Move Joystick simulator anchor
                    Box(
                        modifier = Modifier
                            .offset(x = moveJoyOffset.x.dp, y = moveJoyOffset.y.dp)
                            .size(70.dp)
                            .background(Color.DarkGray.copy(alpha = 0.5f), CircleShape)
                            .border(2.dp, TechGreen, CircleShape)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    moveJoyOffset = Offset(
                                        x = (moveJoyOffset.x + dragAmount.x / 2.5f).coerceIn(10f, 150f),
                                        y = (moveJoyOffset.y + dragAmount.y / 2.5f).coerceIn(10f, 320f)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("MOVE", color = TechGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    // 2. Aim Joystick simulator anchor
                    Box(
                        modifier = Modifier
                            .offset(x = (aimJoyOffset.x + 140f).dp, y = aimJoyOffset.y.dp)
                            .size(70.dp)
                            .background(Color.DarkGray.copy(alpha = 0.5f), CircleShape)
                            .border(2.dp, AccentTeal, CircleShape)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    aimJoyOffset = Offset(
                                        x = (aimJoyOffset.x + dragAmount.x / 2.5f).coerceIn(10f, 160f),
                                        y = (aimJoyOffset.y + dragAmount.y / 2.5f).coerceIn(10f, 320f)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("AIM", color = AccentTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    // 3. Fire Button simulator anchor
                    Box(
                        modifier = Modifier
                            .offset(x = (fireBtnOffset.x + 130f).dp, y = fireBtnOffset.y.dp)
                            .size(60.dp)
                            .background(Color.Red.copy(alpha = 0.4f), CircleShape)
                            .border(2.dp, Color.Red, CircleShape)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    fireBtnOffset = Offset(
                                        x = (fireBtnOffset.x + dragAmount.x / 2.5f).coerceIn(10f, 160f),
                                        y = (fireBtnOffset.y + dragAmount.y / 2.5f).coerceIn(10f, 320f)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("FIRE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Button(
                    onClick = {
                        editJoystickMode = false
                        onSaveProfile(
                            profile.copy(
                                moveJoyX = moveJoyOffset.x,
                                moveJoyY = moveJoyOffset.y,
                                aimJoyX = aimJoyOffset.x,
                                aimJoyY = aimJoyOffset.y,
                                fireBtnX = fireBtnOffset.x,
                                fireBtnY = fireBtnOffset.y
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TechGreen)
                ) {
                    Text("SAVE ADVANCED ERGONOMICS", color = CyberDark, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        } else {
            // SLIDERS CONTROLLER CONFIG
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // A. Haptic strength slider setup
                item {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TACTILE HAPTIC STRENGTH:", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("${(profile.hapticStrength * 100).toInt()}%", color = TechGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = profile.hapticStrength,
                            onValueChange = { onSaveProfile(profile.copy(hapticStrength = it)) },
                            colors = SliderDefaults.colors(thumbColor = TechGreen, activeTrackColor = TechGreen)
                        )
                    }
                }

                // B. Aiming sensitivity selector slider
                item {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("AIMING SENSITIVITY MULTIPLY:", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("${"%.2f".format(profile.aimingSensitivity)}x", color = TechGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = profile.aimingSensitivity,
                            onValueChange = { onSaveProfile(profile.copy(aimingSensitivity = it)) },
                            valueRange = 0.2f..2.5f,
                            colors = SliderDefaults.colors(thumbColor = TechGreen, activeTrackColor = TechGreen)
                        )
                    }
                }

                // C. Physical Gamepad mapping diagnosis card
                item {
                    val statusTextColor = if (isGamepadConnected) TechGreen else Color.LightGray
                    Surface(
                        color = CardBackground,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isGamepadConnected) TechGreen else Color.Transparent)
                    ) {
                        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                            Text("PHYSICAL CONTROLLER CONSOLE INTEGRATION:", color = TechGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isGamepadConnected) {
                                    "Connected! Standard direct key code mappings detected:\n- Left Stick: Run & Strafe\n- Right Stick: Camera look orientation\n- R1/R2 Bumper Shoot: SHOOT TARGET\n- Button West / Y: RELOAD AMMO"
                                } else {
                                    "No physical bluetooth controller connected via USB or OTA right now.\n(Connect any standard Xbox, PlayStation, or OTG mobile controller for a native triple-A console feel)."
                                },
                                color = statusTextColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (isGamepadConnected && lastGamepadEventText.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Last Controller Signal: $lastGamepadEventText", color = NeonOrange, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                // D. Physical layout presets
                item {
                    Column {
                        Text("BUTTON PLACEMENT PRESETS:", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Symmetric tactical layout", "Claw tactical layout", "Custom touch positions").forEachIndexed { index, option ->
                                val sel = profile.currentPreset == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (sel) TechGreen else CardBackground, RoundedCornerShape(6.dp))
                                        .border(1.dp, TechGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .clickable {
                                            if (index == 2) {
                                                editJoystickMode = true
                                            } else {
                                                onSaveProfile(profile.copy(currentPreset = index))
                                            }
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option,
                                        fontSize = 9.sp,
                                        color = if (sel) CyberDark else Color.LightGray,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 6. Careers Profiles High Scores Section
@Composable
fun CareersScreen(
    profile: PlayerProfile,
    onBack: () -> Unit,
    onResetStats: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, TechGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TechGreen)
                Spacer(modifier = Modifier.width(4.dp))
                Text("MENU", color = Color.White, fontFamily = FontFamily.Monospace)
            }

            Text(
                text = "OPERATOR DOSSIER INDEX",
                color = TechGreen,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }

        // Stats report Cards
        Column(
            modifier = Modifier.weight(1f).padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = CardBackground,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TechGreen.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, tint = TechGreen, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("AGENT SPEC-OPS REGISTER:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text(profile.name.uppercase(), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
                        Text("LEVEL ${profile.level} OPERATOR [ XP Progress: ${profile.xp} PTS ]", color = TechGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = CardBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CAMPAIGN WINS", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("${profile.wins}", color = TechGreen, fontWeight = FontWeight.Bold, fontSize = 24.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Surface(
                    color = CardBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("COMBAT LOSSES", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("${profile.losses}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 24.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Surface(
                color = CardBackground,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("TOTAL KILLS RECORDED:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("${profile.totalKills} ELIMINATED", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
                    Text("CHAPTER UNLOCKED LIMIT: AREA ${profile.campaignChapter}", color = TechGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Button(
            onClick = onResetStats,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
            modifier = Modifier.border(1.dp, Color.Red).fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("ERASE Dossier Records", color = Color.Red, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

// 7. Active Gameplay Arena screen. Draws 3D pseudo/2.5D Raycasting Tactical Canvas
@Composable
fun ActiveGamePlaygroundScreen(
    profile: PlayerProfile,
    activeWep: WeaponConfig,
    gameEngine: GameEngine,
    isGamepadConnected: Boolean,
    onBackToMenu: () -> Unit,
    onMatchEnded: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var tickCounter by remember { mutableStateOf(0) }

    // Touch gesture speeds
    var joyMoveX by remember { mutableStateOf(0f) }
    var joyMoveY by remember { mutableStateOf(0f) }
    var padTurnSpeed by remember { mutableStateOf(0f) }

    val blueScore by gameEngine.skirmishBlueScore.collectAsState()
    val redScore by gameEngine.skirmishRedScore.collectAsState()
    val matchStatus by gameEngine.matchStatus.collectAsState()

    // Trigger regular loop ticks via side-effect coroutine
    LaunchedEffect(key1 = true) {
        while (true) {
            delay(16L) // ~60fps rendering ticks
            gameEngine.updateFrame(
                moveX = joyMoveX,
                moveY = joyMoveY,
                turnSpeed = padTurnSpeed
            )
            tickCounter++
        }
    }

    // Monitor match results terminal status updates
    LaunchedEffect(matchStatus) {
        if (matchStatus == "VICTORY" || matchStatus == "DEFEAT") {
            onMatchEnded(matchStatus)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Master High-Fidelity raycasted FPS view Canvas!
        Canvas(modifier = Modifier.fillMaxSize()) {
            val numRays = 140
            val fov = 1.05f // Wide angled view field (~60 degrees)
            val canvasW = size.width
            val canvasH = size.height

            // A. DRAW CEILING/SKY GRADIENT (Dark Space Blue/Grey to pitch black)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF030D0A), CyberDark),
                    startY = 0f,
                    endY = canvasH / 2
                ),
                topLeft = Offset.Zero,
                size = Size(canvasW, canvasH / 2)
            )

            // B. DRAW GROUND GRADIENT (Deep dark metallic plates slate)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(CyberDark, Color(0xFF081C17)),
                    startY = canvasH / 2,
                    endY = canvasH
                ),
                topLeft = Offset(0f, canvasH / 2),
                size = Size(canvasW, canvasH / 2)
            )

            // C. RAYCASTING CALCULATION LOOP
            val rayWidth = canvasW / numRays
            val mapHeight = gameEngine.activeMapGrid.size
            val mapWidth = gameEngine.activeMapGrid[0].size

            for (rayIndex in 0 until numRays) {
                val rAngle = gameEngine.playerAngle - fov / 2f + (rayIndex.toFloat() / numRays.toFloat()) * fov
                var distance = 0f
                var hitWallType = 0
                val px = gameEngine.playerX
                val py = gameEngine.playerY

                val cosRay = cos(rAngle)
                val sinRay = sin(rAngle)

                // Raymarch incremental step scanning map array grids
                for (step in 1..140) {
                    val marchD = step * 0.08f
                    val rx = px + cosRay * marchD
                    val ry = py + sinRay * marchD

                    val cellX = rx.toInt()
                    val cellY = ry.toInt()

                    if (cellX < 0 || cellX >= mapWidth || cellY < 0 || cellY >= mapHeight) {
                        break
                    }

                    val cellVal = gameEngine.activeMapGrid[cellY][cellX]
                    if (cellVal > 0 && cellVal != 8) { // solid hit
                        distance = marchD
                        hitWallType = cellVal
                        break
                    }
                }

                if (distance > 0f) {
                    // Correct fish-eye distortion perspective
                    val angleDiff = rAngle - gameEngine.playerAngle
                    val correctedD = distance * cos(angleDiff)

                    val wallHeight = (canvasH * 0.42f / correctedD).coerceAtMost(canvasH * 0.95f)
                    val wallTop = (canvasH / 2) - (wallHeight / 2)

                    // Pick wall color based on ID grid configurations
                    val baseCol = when (hitWallType) {
                        1 -> Color(0xFF4A5552) // Steel Grey
                        2 -> Color(0xFF003D33) // Cyber Concrete (Dark teal)
                        3 -> Color(0xFF004D40) // Glowing screen Command wall (M3 Teal variant)
                        4 -> Color(0xFF886600) // Hazard armory container crates (Dark gold)
                        9 -> NeonOrange // Destructible laptop server cons
                        else -> TechGreen
                    }

                    // Shaded gradient projection: Darker further away to simulate fog-atmosphere depth!
                    val fadeFrac = (1.0f - (distance / 12.0f)).coerceIn(0.08f, 1.0f)
                    val finalCol = Color(
                        red = baseCol.red * fadeFrac,
                        green = baseCol.green * fadeFrac,
                        blue = baseCol.blue * fadeFrac,
                        alpha = 1.0f
                    )

                    drawRect(
                        color = finalCol,
                        topLeft = Offset(rayIndex * rayWidth, wallTop),
                        size = Size(rayWidth + 1f, wallHeight)
                    )

                    // Draw optional neon command lines if center wall type is 3
                    if (hitWallType == 3 && distance < 6.5f) {
                        drawLine(
                            TechGreen.copy(alpha = fadeFrac),
                            Offset(rayIndex * rayWidth, wallTop + wallHeight * 0.2f),
                            Offset((rayIndex + 1) * rayWidth, wallTop + wallHeight * 0.2f),
                            strokeWidth = 2f
                        )
                    }
                }
            }

            // D. DRAW ACTIVE SOLDIER BOTS AS 3D SPRITE BILLBOARDS IN FOV
            gameEngine.botList.filter { it.health > 0 }.sortedByDescending {
                sqrt((it.x - gameEngine.playerX).pow(2) + (it.y - gameEngine.playerY).pow(2))
            }.forEach { bot ->
                val botDx = bot.x - gameEngine.playerX
                val botDy = bot.y - gameEngine.playerY

                // Angle difference relative to player yaw
                var botAng = atan2(botDy, botDx) - gameEngine.playerAngle
                while (botAng < -PI) botAng += 2 * (PI).toFloat()
                while (botAng > PI) botAng -= 2 * (PI).toFloat()

                // If in camera FOV bounds
                if (abs(botAng) < fov / 1.7f) {
                    val botDist = sqrt(botDx * botDx + botDy * botDy)
                    if (botDist > 0.45f && botDist < 12.0f) {
                        val correctedBotD = botDist * cos(botAng)

                        val botHeight = (canvasH * 0.42f / correctedBotD).coerceAtMost(canvasH * 0.8f)
                        val botWidth = botHeight * 0.5f

                        // screen middle horizontal index
                        val screenCenterX = canvasW / 2
                        val screenXOffset = (botAng / (fov / 2f)) * (canvasW / 2)
                        val botScreenX = screenCenterX + screenXOffset - (botWidth / 2)
                        val botScreenY = (canvasH / 2) - (botHeight / 2)

                        // Render silhouette of soldier bot
                        val colorTeam = if (bot.isEnemy) NeonOrange else TechGreen
                        val borderAlpha = (1.0f - botDist / 12.0f).coerceIn(0.1f, 1.0f)

                        // Draw bot outline avatar
                        drawRect(
                            color = colorTeam.copy(alpha = 0.35f * borderAlpha),
                            topLeft = Offset(botScreenX, botScreenY),
                            size = Size(botWidth, botHeight)
                        )

                        // Draw outline indicator lines
                        drawRect(
                            color = colorTeam.copy(alpha = borderAlpha),
                            topLeft = Offset(botScreenX, botScreenY),
                            size = Size(botWidth, botHeight),
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Technical HUD info text over bot card
                        val botTitle = "${bot.name} [${bot.health.toInt()}]"
                        // Draw health bar overlay
                        drawRect(
                            color = Color.DarkGray,
                            topLeft = Offset(botScreenX + botWidth * 0.1f, botScreenY - 14.dp.toPx()),
                            size = Size(botWidth * 0.8f, 4.dp.toPx())
                        )
                        drawRect(
                            color = if (bot.isEnemy) Color.Red else TechGreen,
                            topLeft = Offset(botScreenX + botWidth * 0.1f, botScreenY - 14.dp.toPx()),
                            size = Size(botWidth * 0.8f * (bot.health / 100f), 4.dp.toPx())
                        )
                    }
                }
            }

            // E. RED DESTRUCTIBLE BLOOD FLASHES / VIGNETTE IF HIT
            if (gameEngine.playerHealth < 40f) {
                val cyclePulse = abs(sin(tickCounter * 0.08f))
                drawRect(
                    color = Color.Red.copy(alpha = 0.22f * cyclePulse),
                    topLeft = Offset.Zero,
                    size = size,
                    style = Stroke(width = 32.dp.toPx())
                )
            }
        }

        // F. FOREGROUND RETICLE / GUN MODEL WEAPON BOBBING ANIMATION OVERLAY
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.9f)
                .height(200.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Simulated gun weapon frame shifting based on recoil
            val recoilShiftY = (gameEngine.recoilBobbing * 3.5f).coerceAtMost(50f)
            val recoilShiftX = (gameEngine.recoilBobbing * 1.5f * sin(tickCounter * 0.2f)).coerceIn(-20f, 20f)

            Box(
                modifier = Modifier
                    .offset(x = recoilShiftX.dp, y = (50f + recoilShiftY).dp)
                    .width(180.dp)
                    .height(180.dp)
            ) {
                // High-fidelity vector layout blueprint representing FPS Weapon overlay in your hands!
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val p = Path()
                    p.moveTo(size.width * 0.35f, size.height) // bottom grip
                    p.lineTo(size.width * 0.5f, size.height * 0.4f) // frame scope rise
                    p.lineTo(size.width * 0.55f, size.height * 0.1f) // tactical silencer muzzle
                    p.lineTo(size.width * 0.62f, size.height * 0.1f)
                    p.lineTo(size.width * 0.65f, size.height * 0.45f)
                    p.lineTo(size.width * 0.75f, size.height) // right hand anchor
                    p.close()

                    drawPath(p, Color.DarkGray)
                    drawPath(p, TechGreen, style = Stroke(width = 2.dp.toPx()))

                    // Draw Muzzle Flash Burst Fire if triggered!
                    if (gameEngine.muzzleFlashIntensity > 0f) {
                        drawCircle(
                            color = NeonOrange,
                            radius = 24.dp.toPx() * gameEngine.muzzleFlashIntensity,
                            center = Offset(size.width * 0.58f, size.height * 0.1f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 12.dp.toPx() * gameEngine.muzzleFlashIntensity,
                            center = Offset(size.width * 0.58f, size.height * 0.1f)
                        )
                    }
                }
            }
        }

        // G. REAL-TIME TARGETING CROSSHAIR RETICLE AT SCREEN CENTER
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val baseGap = 8.dp.toPx() + (gameEngine.recoilBobbing * 1.5f).dp.toPx()
                val lineL = 10.dp.toPx()
                // Left tick
                drawLine(TechGreen, Offset(size.width / 2 - baseGap - lineL, size.height / 2), Offset(size.width / 2 - baseGap, size.height / 2), strokeWidth = 2.5f)
                // Right tick
                drawLine(TechGreen, Offset(size.width / 2 + baseGap, size.height / 2), Offset(size.width / 2 + baseGap + lineL, size.height / 2), strokeWidth = 2.5f)
                // Top tick
                drawLine(TechGreen, Offset(size.width / 2, size.height / 2 - baseGap - lineL), Offset(size.width / 2, size.height / 2 - baseGap), strokeWidth = 2.5f)
                // Bottom tick
                drawLine(TechGreen, Offset(size.width / 2, size.height / 2 + baseGap), Offset(size.width / 2, size.height / 2 + baseGap + lineL), strokeWidth = 2.5f)

                // Laser dot center highlight
                drawCircle(NeonOrange, radius = 3f, center = Offset(size.width / 2, size.height / 2))
            }
        }

        // H. ACTIVE HEAVY TACTICAL HUD COCKPIT OVERLAY
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player stats layout box
            Column(
                modifier = Modifier
                    .background(CyberDark.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .border(1.dp, TechGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "SPEC-OPS AGENT HUD",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "VITAL HP: ${gameEngine.playerHealth.toInt()}",
                        color = if (gameEngine.playerHealth > 40f) TechGreen else Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "AP SHIELD: ${gameEngine.playerShield.toInt()}",
                        color = Color.Cyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                // Objective layout
                if (gameEngine.isCampaign.value) {
                    val statusText = when (gameEngine.currentChapter.value) {
                        1 -> if (gameEngine.isIntelHarvested) "INTEL RETRIEVED - GO TO EXTRACTION GATE (CELL 8)" else "GO TO LAPTOP AREA (REAR NORTH) TO HARVEST DATALINKS"
                        2 -> if (gameEngine.targetServersRemaining.isEmpty()) "ALL DATABANKS TERMINATED - PROCEED TO SOUTHERN EXTRACT" else "TERMINATE SERVER consoles (${gameEngine.targetServersRemaining.size} REMAINING)"
                        3 -> if (gameEngine.bossDefeated) "APEX WARLORD NEUTRALIZED - HEAD FOR ESCAPE PORTAL" else "CRITICAL ASSAULT: TARGET ELITE BOSS APEX [HP: ${gameEngine.bossHealth.toInt()}]"
                        else -> "ELIMINATE GUARDRADAR WAVE ON AREA GRID"
                    }
                    Text(
                        text = "OBJECTIVE: $statusText",
                        color = NeonOrange,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(220.dp)
                    )
                } else {
                    // Skirmish Score
                    Text(
                        text = "TEAM BLUE (US): $blueScore  ||  TEAM RED: $redScore",
                        color = TechGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Gun Ammo card layout
            Column(
                modifier = Modifier
                    .background(CyberDark.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .border(1.dp, TechGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = activeWep.weaponName.uppercase(),
                    color = TechGreen,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (gameEngine.isReloading) "RELOADING" else "${gameEngine.playerAmmoInClip} / ${gameEngine.playerReserveAmmo}",
                    color = if (gameEngine.playerAmmoInClip <= 5) Color.Red else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // I. INTEGRATED TACTILE/ERGONOMICS TOUCH BUTTONS COCKPIT (IF NOT USING CONTROLLER)
        // Set positions dynamically mapped based on profile setup
        if (!isGamepadConnected) {
            // A. Movement Axis joystick controller trackpad on bottom-left
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 24.dp)
                    .size(110.dp)
                    .background(Color.DarkGray.copy(alpha = 0.3f), CircleShape)
                    .testTag("left_movement_joystick")
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                joyMoveX = 0f
                                joyMoveY = 0f
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            // Clamp motion delta to normalized joystick coordinates
                            joyMoveY = (-dragAmount.y / 25f).coerceIn(-1.0f, 1.0f)
                            joyMoveX = (dragAmount.x / 25f).coerceIn(-1.0f, 1.0f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Interactive thumb center anchor
                Box(
                    modifier = Modifier
                        .offset(x = (joyMoveX * 25).dp, y = (-joyMoveY * 25).dp)
                        .size(44.dp)
                        .background(TechGreen.copy(alpha = 0.8f), CircleShape)
                )
            }

            // B. Camera looking turning trackpad on bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 24.dp)
                    .size(110.dp)
                    .background(Color.DarkGray.copy(alpha = 0.3f), CircleShape)
                    .testTag("right_aiming_joystick")
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                padTurnSpeed = 0f
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            padTurnSpeed = (dragAmount.x / 20f).coerceIn(-1.0f, 1.0f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = (padTurnSpeed * 25).dp)
                        .size(44.dp)
                        .background(AccentTeal.copy(alpha = 0.8f), CircleShape)
                )
            }

            // C. Fire projectile button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .size(64.dp)
                    .background(Color.Red.copy(alpha = 0.5f), CircleShape)
                    .border(2.dp, Color.Red, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 32.dp),
                        onClick = {
                            gameEngine.triggerPlayerShoot()
                        }
                    )
                    .testTag("fire_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CrisisAlert,
                    contentDescription = "Trigger fire target bullet",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // D. Bullet reload button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 95.dp)
                    .size(46.dp)
                    .background(CardBackground.copy(alpha = 0.82f), CircleShape)
                    .border(1.dp, TechGreen, CircleShape)
                    .clickable { gameEngine.triggerReload() }
                    .testTag("reload_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cached,
                    contentDescription = "Reload ammo",
                    tint = TechGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            // Visual gamepad assistant active labels if physical controller connected
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
                    .background(CyberDark.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                    .border(1.dp, TechGreen, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "CONTROLLER MAPPED: [L-Stick Move] [R-Stick Aim] [R2 Bumper Fire] [Y Button Reload]",
                    color = TechGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // K. BACK TO PORTAL ESCAPE BUTTON
        Button(
            onClick = onBackToMenu,
            colors = ButtonDefaults.buttonColors(containerColor = CardBackground.copy(alpha = 0.85f)),
            border = BorderStroke(1.dp, Color.Red),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp)
        ) {
            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Exit to deployment central", tint = Color.Red)
            Spacer(modifier = Modifier.width(4.dp))
            Text("ABANDON ENCOUNTER", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
