package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PetEntity
import com.example.data.model.Species
import com.example.ui.components.PetCanvasRenderer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class EggVisualStage(val title: String, val description: String, val icon: String) {
    INTACT("Ovo Intacto", "Ovo calmo e dormindo quentinho", "🥚"),
    CRACKING("Pequenas Rachaduras", "O bichinho está começando a se mexer!", "🥚⚡"),
    ALMOST_HATCHING("Quase se Abrindo!", "Muita luz e energia emanando da casca!", "🌟")
}

data class FloatingParticle(
    val id: Long,
    val x: Float,
    val y: Float,
    val type: ParticleType,
    val scale: Float = 1f,
    val alpha: Float = 1f
)

enum class ParticleType {
    HEART, STAR, SPARKLE
}

@Composable
fun EggHatchingScreen(
    pet: PetEntity,
    onWarmEgg: (Int) -> Unit,
    onHatchEgg: (String, Species) -> Unit,
    onSetName: (String) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Name text field state
    var petNameInput by remember { mutableStateOf(pet.name) }
    var isNameConfirmed by remember { mutableStateOf(pet.name.isNotBlank()) }
    var nameError by remember { mutableStateOf<String?>(null) }

    // Touch and interaction states
    var isTouching by remember { mutableStateOf(false) }
    var lastRubTime by remember { mutableLongStateOf(0L) }
    var touchShakeAngle by remember { mutableFloatStateOf(0f) }
    var tapParticles by remember { mutableStateOf<List<FloatingParticle>>(emptyList()) }

    // Cinematic Hatching Sequence States
    var isHatchingCutsceneActive by remember { mutableStateOf(false) }
    var previewSpecies by remember { mutableStateOf<Species?>(null) }

    val eggProgress = pet.eggWarmProgress.coerceIn(0, 100)

    // Visual Stage calculation
    val currentStage = when {
        eggProgress < 36 -> EggVisualStage.INTACT
        eggProgress < 76 -> EggVisualStage.CRACKING
        else -> EggVisualStage.ALMOST_HATCHING
    }

    // Infinite ambient animations
    val infiniteTransition = rememberInfiniteTransition(label = "egg_ambient")

    // Breathing pulse (scale 0.98 to 1.03)
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (eggProgress >= 76) 800 else 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "egg_breathing"
    )

    // Rocking / Shivering angle
    val ambientRockAngle by infiniteTransition.animateFloat(
        initialValue = if (eggProgress >= 76) -6f else if (eggProgress >= 36) -3f else -1.5f,
        targetValue = if (eggProgress >= 76) 6f else if (eggProgress >= 36) 3f else 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (eggProgress >= 76) 400 else if (eggProgress >= 36) 700 else 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "egg_rocking"
    )

    // Glowing aura intensity
    val auraGlow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "egg_aura_glow"
    )

    // Ambient floating particles phase
    val ambientParticlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_particles"
    )

    // Animate progress smoothly
    val animatedProgress by animateFloatAsState(
        targetValue = eggProgress / 100f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "animated_egg_progress"
    )

    // Interaction response helper
    fun registerInteraction(gain: Int, x: Float? = null, y: Float? = null, type: ParticleType = ParticleType.HEART) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onWarmEgg(gain)
        touchShakeAngle = (Random.nextFloat() * 14f) - 7f

        // Spawn particle at touch position or around egg
        val px = x ?: (200f + Random.nextFloat() * 200f)
        val py = y ?: (300f + Random.nextFloat() * 200f)
        val newParticle = FloatingParticle(
            id = System.currentTimeMillis() + Random.nextLong(1000),
            x = px,
            y = py,
            type = type,
            scale = 0.8f + Random.nextFloat() * 0.5f
        )
        tapParticles = (tapParticles + newParticle).takeLast(12)

        coroutineScope.launch {
            delay(180)
            touchShakeAngle = 0f
            delay(700)
            tapParticles = tapParticles.filter { it.id != newParticle.id }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFBEB), // Warm cream
                        Color(0xFFFEF3C7), // Light amber
                        Color(0xFFFDE68A)  // Golden peach
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==========================================
            // 1. CABEÇALHO (Clean & Minimalist)
            // ==========================================
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag("incubator_header")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🥚",
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Incubadora de ovos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF78350F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // 2. MENSAGEM PRINCIPAL (Engaging & Warm)
            // ==========================================
            Text(
                text = "Um novo amigo está prestes a nascer.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = Color(0xFF451A03),
                modifier = Modifier.testTag("main_incubator_heading")
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Cuide do ovo e descubra qual criatura está escondida dentro dele.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF92400E),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 3. ÁREA CENTRAL (OVO PROTAGONISTA ~70%)
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .testTag("egg_hero_area"),
                contentAlignment = Alignment.Center
            ) {
                // Interactive Touch Canvas on Egg
                Box(
                    modifier = Modifier
                        .size(340.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { offset ->
                                    isTouching = true
                                    registerInteraction(gain = 3, x = offset.x, y = offset.y, type = ParticleType.HEART)
                                    tryAwaitRelease()
                                    isTouching = false
                                },
                                onDoubleTap = { offset ->
                                    registerInteraction(gain = 5, x = offset.x, y = offset.y, type = ParticleType.STAR)
                                },
                                onLongPress = { offset ->
                                    registerInteraction(gain = 8, x = offset.x, y = offset.y, type = ParticleType.SPARKLE)
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val now = System.currentTimeMillis()
                                if (now - lastRubTime > 220) {
                                    lastRubTime = now
                                    registerInteraction(
                                        gain = 2,
                                        x = change.position.x,
                                        y = change.position.y,
                                        type = ParticleType.HEART
                                    )
                                }
                            }
                        }
                        .testTag("egg_interactive_target"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val centerX = canvasWidth / 2f
                        val centerY = canvasHeight / 2f

                        // A. Circular Progress Ring around Egg (Substitui barra linear)
                        drawCircularProgressRing(
                            centerX = centerX,
                            centerY = centerY,
                            radius = 145.dp.toPx(),
                            progress = animatedProgress,
                            glowAlpha = if (eggProgress >= 100) auraGlow else 0.4f
                        )

                        // B. Floating Background Particles (Corações, estrelas e brilhos)
                        drawFloatingAmbientParticles(
                            centerX = centerX,
                            centerY = centerY,
                            phase = ambientParticlePhase,
                            progress = eggProgress
                        )

                        // C. Dynamic Shadow on Ground
                        val shadowWidth = 140.dp.toPx() * (if (isTouching) 1.15f else breathingScale)
                        val shadowHeight = 22.dp.toPx()
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x3D451A03), Color(0x00451A03)),
                                center = Offset(centerX, centerY + 118.dp.toPx()),
                                radius = shadowWidth / 2f
                            ),
                            topLeft = Offset(centerX - shadowWidth / 2f, centerY + 110.dp.toPx()),
                            size = Size(shadowWidth, shadowHeight)
                        )

                        // D. Pulsing Warmth & Radiant Aura
                        val auraRadius = (105.dp.toPx() + (eggProgress * 0.45f).dp.toPx()) * (if (isTouching) 1.12f else breathingScale)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x66FBBF24).copy(alpha = (0.25f + (eggProgress / 100f) * 0.45f) * auraGlow),
                                    Color(0x33F59E0B).copy(alpha = 0.15f),
                                    Color(0x00FDE047)
                                ),
                                center = Offset(centerX, centerY),
                                radius = auraRadius
                            ),
                            center = Offset(centerX, centerY),
                            radius = auraRadius
                        )

                        // E. Draw Egg with 3 Visual Stages & Shaking/Breathing
                        val totalAngle = ambientRockAngle + touchShakeAngle
                        val currentScale = (if (isTouching) 1.06f else breathingScale)
                        rotate(totalAngle, pivot = Offset(centerX, centerY + 60.dp.toPx())) {
                            drawEggProtagonist(
                                centerX = centerX,
                                centerY = centerY,
                                scale = currentScale,
                                progress = eggProgress,
                                stage = currentStage,
                                isTouching = isTouching
                            )
                        }

                        // F. Draw User Tap/Rub Floating Particles
                        tapParticles.forEach { particle ->
                            val pOffset = Offset(particle.x, particle.y)
                            when (particle.type) {
                                ParticleType.HEART -> drawSmallHeart(pOffset, Color(0xFFF43F5E), 16.dp.toPx() * particle.scale)
                                ParticleType.STAR -> drawSmallStar(pOffset, Color(0xFFF59E0B), 14.dp.toPx() * particle.scale)
                                ParticleType.SPARKLE -> drawSparkle(pOffset, Color(0xFFEC4899), 12.dp.toPx() * particle.scale)
                            }
                        }
                    }

                    // Progress percentage floating badge at bottom of ring
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.92f),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                            .testTag("egg_progress_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (eggProgress >= 100) Icons.Default.AutoAwesome else Icons.Default.Favorite,
                                contentDescription = null,
                                tint = if (eggProgress >= 100) Color(0xFFEA580C) else Color(0xFFE11D48),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (eggProgress >= 100) "100% • Pronto para Chocar!" else "$eggProgress% Incubado",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = if (eggProgress >= 100) Color(0xFFC2410C) else Color(0xFF78350F)
                            )
                        }
                    }
                }
            }

            // Interactive Gestures Hint
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.75f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👆 Toque", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
                        Text(" • ", fontSize = 12.sp, color = Color(0xFFD97706))
                        Text("🖐️ Esfregue", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
                        Text(" • ", fontSize = 12.sp, color = Color(0xFFD97706))
                        Text("⏱️ Pressione", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // 4. ESCOLHA DO NOME OBRIGATÓRIA (Cartão logo abaixo)
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pet_naming_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🏷️", fontSize = 16.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Como você gostaria de chamar seu novo companheiro?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF451A03)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = petNameInput,
                        onValueChange = {
                            petNameInput = it
                            nameError = null
                            if (it.isNotBlank()) {
                                isNameConfirmed = false
                            }
                        },
                        placeholder = { Text("Ex: Pipoca, Luna, Sol, Toby...") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pet_name_input_field"),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            if (petNameInput.trim().isNotBlank()) {
                                isNameConfirmed = true
                                onSetName(petNameInput.trim())
                            } else {
                                nameError = "Por favor, escolha um nome com carinho!"
                            }
                        }),
                        trailingIcon = {
                            if (petNameInput.isNotBlank() && isNameConfirmed) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Nome Confirmado", tint = Color(0xFF16A34A))
                            }
                        },
                        isError = nameError != null
                    )

                    if (nameError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = nameError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (petNameInput.trim().isNotBlank()) {
                                isNameConfirmed = true
                                nameError = null
                                onSetName(petNameInput.trim())
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else {
                                nameError = "O nome é obrigatório para chocar seu amigo!"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("confirm_pet_name_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNameConfirmed) Color(0xFF16A34A) else Color(0xFFD97706)
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isNameConfirmed) "✔ Nome Confirmado: ${petNameInput.trim()}" else "✔ Confirmar nome",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // 5. INFORMAÇÕES MISTERIOSAS (Raridades Possíveis)
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mystery_rarities_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✨", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Possíveis resultados:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF78350F)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RarityPill("Comum", Color(0xFF64748B), "50%")
                        RarityPill("Raro", Color(0xFF0284C7), "30%")
                        RarityPill("Épico", Color(0xFF9333EA), "15%")
                        RarityPill("Lendário", Color(0xFFEAB308), "5%")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 6. BOTÃO DE ECLOSÃO MÁGICA (Ao atingir 100%)
            // ==========================================
            if (eggProgress >= 100) {
                Button(
                    onClick = {
                        if (petNameInput.trim().isBlank()) {
                            nameError = "Por favor, defina o nome do seu bichinho antes de chocar!"
                            return@Button
                        }
                        // Start Cinematic Hatching Sequence
                        previewSpecies = Species.getRandomSpecies()
                        isHatchingCutsceneActive = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .shadow(8.dp, RoundedCornerShape(18.dp))
                        .testTag("cinematic_hatch_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEA580C)
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "✨ CHOCAR OVO AGORA! ✨",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            } else {
                // Secondary Quick Warm Button for Accessibility
                Button(
                    onClick = {
                        registerInteraction(gain = 10, type = ParticleType.HEART)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("quick_warm_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD97706)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Acariciar e Aquecer com Amor (+10%)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // =========================================================================
        // 7. ANIMAÇÃO DA ECLOSÃO CINEMATOGRÁFICA (Overlay Cinematográfico Completo)
        // =========================================================================
        if (isHatchingCutsceneActive && previewSpecies != null) {
            val speciesToHatch = previewSpecies!!
            CinematicHatchingCutscene(
                chosenName = petNameInput.trim().ifBlank { "Amiguinho" },
                species = speciesToHatch,
                onComplete = {
                    isHatchingCutsceneActive = false
                    onHatchEgg(petNameInput.trim().ifBlank { "Amiguinho" }, speciesToHatch)
                }
            )
        }
    }
}

@Composable
private fun RarityPill(name: String, color: Color, percentage: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = color
            )
            Text(
                text = percentage,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = Color(0xFF475569)
            )
        }
    }
}

/**
 * Renders the circular progress ring around the egg with glowing gradient.
 */
private fun DrawScope.drawCircularProgressRing(
    centerX: Float,
    centerY: Float,
    radius: Float,
    progress: Float,
    glowAlpha: Float
) {
    val ringStroke = 9.dp.toPx()

    // Background track ring
    drawCircle(
        color = Color(0x33F59E0B),
        radius = radius,
        center = Offset(centerX, centerY),
        style = Stroke(width = ringStroke, cap = StrokeCap.Round)
    )

    // Progress Arc
    if (progress > 0.01f) {
        val sweepAngle = 360f * progress
        val gradientBrush = Brush.sweepGradient(
            colors = listOf(
                Color(0xFFFBBF24),
                Color(0xFFF59E0B),
                Color(0xFFEA580C),
                Color(0xFFFBBF24)
            ),
            center = Offset(centerX, centerY)
        )

        // Draw Arc Glow
        if (progress >= 0.99f) {
            drawArc(
                brush = Brush.radialGradient(
                    listOf(Color(0x88F59E0B).copy(alpha = glowAlpha), Color(0x00F59E0B)),
                    center = Offset(centerX, centerY),
                    radius = radius + 20.dp.toPx()
                ),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(centerX - radius - 10.dp.toPx(), centerY - radius - 10.dp.toPx()),
                size = Size((radius + 10.dp.toPx()) * 2, (radius + 10.dp.toPx()) * 2),
                style = Stroke(width = ringStroke * 2.2f)
            )
        }

        drawArc(
            brush = gradientBrush,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = ringStroke, cap = StrokeCap.Round)
        )
    }
}

/**
 * Draws the Egg protagonist across the 3 visual stages (Intact, Cracking, Almost Hatching).
 */
private fun DrawScope.drawEggProtagonist(
    centerX: Float,
    centerY: Float,
    scale: Float,
    progress: Int,
    stage: EggVisualStage,
    isTouching: Boolean
) {
    val eggW = 145.dp.toPx() * scale
    val eggH = 190.dp.toPx() * scale

    // Base Egg Path (Organic egg silhouette)
    val eggPath = Path().apply {
        moveTo(centerX, centerY - eggH * 0.5f)
        cubicTo(
            centerX + eggW * 0.58f, centerY - eggH * 0.44f,
            centerX + eggW * 0.65f, centerY + eggH * 0.38f,
            centerX, centerY + eggH * 0.5f
        )
        cubicTo(
            centerX - eggW * 0.65f, centerY + eggH * 0.38f,
            centerX - eggW * 0.58f, centerY - eggH * 0.44f,
            centerX, centerY - eggH * 0.5f
        )
        close()
    }

    // Casca do Ovo (Gradiente aconchegante)
    val shellBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFBEB),
            Color(0xFFFEF3C7),
            Color(0xFFFDE68A),
            Color(0xFFF59E0B)
        ),
        startY = centerY - eggH * 0.5f,
        endY = centerY + eggH * 0.5f
    )
    drawPath(path = eggPath, brush = shellBrush)

    // Manchinhas fofas estilo Tamagotchi / Pokémon
    val spotBrush = Brush.linearGradient(listOf(Color(0xFFF472B6).copy(alpha = 0.85f), Color(0xFFEC4899).copy(alpha = 0.85f)))
    drawCircle(spotBrush, radius = 16.dp.toPx() * scale, center = Offset(centerX - 35.dp.toPx() * scale, centerY - 15.dp.toPx() * scale))
    drawCircle(spotBrush, radius = 12.dp.toPx() * scale, center = Offset(centerX + 38.dp.toPx() * scale, centerY - 38.dp.toPx() * scale))
    drawCircle(spotBrush, radius = 20.dp.toPx() * scale, center = Offset(centerX + 28.dp.toPx() * scale, centerY + 36.dp.toPx() * scale))
    drawCircle(spotBrush, radius = 14.dp.toPx() * scale, center = Offset(centerX - 30.dp.toPx() * scale, centerY + 45.dp.toPx() * scale))

    // ============================================================
    // ESTÁGIO 2: PEQUENAS RACHADURAS LUMINOSAS (36% a 75%)
    // ============================================================
    if (stage == EggVisualStage.CRACKING || stage == EggVisualStage.ALMOST_HATCHING) {
        val crack1 = Path().apply {
            moveTo(centerX - 15.dp.toPx() * scale, centerY - 25.dp.toPx() * scale)
            lineTo(centerX + 8.dp.toPx() * scale, centerY - 8.dp.toPx() * scale)
            lineTo(centerX - 10.dp.toPx() * scale, centerY + 12.dp.toPx() * scale)
            lineTo(centerX + 18.dp.toPx() * scale, centerY + 30.dp.toPx() * scale)
        }
        // Glow behind crack
        drawPath(
            crack1,
            color = Color(0xFFFDE047),
            style = Stroke(width = 6.dp.toPx() * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            crack1,
            color = Color(0xFF451A03),
            style = Stroke(width = 3.dp.toPx() * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    // ============================================================
    // ESTÁGIO 3: RACHADURAS PROFUNDAS E FEIXES DE LUZ (76% a 100%)
    // ============================================================
    if (stage == EggVisualStage.ALMOST_HATCHING) {
        val crack2 = Path().apply {
            moveTo(centerX + 35.dp.toPx() * scale, centerY - 20.dp.toPx() * scale)
            lineTo(centerX + 14.dp.toPx() * scale, centerY - 2.dp.toPx() * scale)
            lineTo(centerX + 32.dp.toPx() * scale, centerY + 18.dp.toPx() * scale)
            lineTo(centerX + 8.dp.toPx() * scale, centerY + 35.dp.toPx() * scale)
        }
        val crack3 = Path().apply {
            moveTo(centerX - 35.dp.toPx() * scale, centerY + 10.dp.toPx() * scale)
            lineTo(centerX - 15.dp.toPx() * scale, centerY + 28.dp.toPx() * scale)
            lineTo(centerX - 4.dp.toPx() * scale, centerY + 55.dp.toPx() * scale)
        }

        // Intense Light Beams bursting from cracks
        drawLine(
            brush = Brush.radialGradient(listOf(Color(0xFFFFFBEB), Color(0x00FFFBEB)), center = Offset(centerX, centerY), radius = 60.dp.toPx()),
            start = Offset(centerX - 10.dp.toPx(), centerY),
            end = Offset(centerX - 60.dp.toPx(), centerY - 40.dp.toPx()),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            brush = Brush.radialGradient(listOf(Color(0xFFFFFBEB), Color(0x00FFFBEB)), center = Offset(centerX, centerY), radius = 60.dp.toPx()),
            start = Offset(centerX + 15.dp.toPx(), centerY + 10.dp.toPx()),
            end = Offset(centerX + 65.dp.toPx(), centerY + 30.dp.toPx()),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawPath(crack2, color = Color(0xFFFEF08A), style = Stroke(width = 6.dp.toPx() * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(crack2, color = Color(0xFF451A03), style = Stroke(width = 3.2.dp.toPx() * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))

        drawPath(crack3, color = Color(0xFFFEF08A), style = Stroke(width = 5.dp.toPx() * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(crack3, color = Color(0xFF451A03), style = Stroke(width = 3.dp.toPx() * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    // Contorno macio do ovo
    drawPath(
        path = eggPath,
        color = Color(0xFF78350F),
        style = Stroke(width = 4.dp.toPx())
    )

    // Brilho especular (Highlight fofo no topo esquerdo)
    drawOval(
        color = Color(0x99FFFFFF),
        topLeft = Offset(centerX - eggW * 0.38f, centerY - eggH * 0.38f),
        size = Size(26.dp.toPx() * scale, 48.dp.toPx() * scale)
    )
}

/**
 * Ambient floating particles (hearts, stars, sparkles).
 */
private fun DrawScope.drawFloatingAmbientParticles(
    centerX: Float,
    centerY: Float,
    phase: Float,
    progress: Int
) {
    val offsets = listOf(
        Offset(-90.dp.toPx(), -70.dp.toPx()),
        Offset(95.dp.toPx(), -80.dp.toPx()),
        Offset(-105.dp.toPx(), 45.dp.toPx()),
        Offset(100.dp.toPx(), 60.dp.toPx()),
        Offset(-40.dp.toPx(), -120.dp.toPx()),
        Offset(50.dp.toPx(), -110.dp.toPx())
    )

    offsets.forEachIndexed { i, baseOffset ->
        val yShift = ((phase + (i * 0.18f)) % 1f) * -50.dp.toPx()
        val pPos = Offset(centerX + baseOffset.x, centerY + baseOffset.y + yShift)
        val alpha = (sin((phase + (i * 0.2f)) * 3.14159f).coerceIn(0f, 1f)) * (0.4f + (progress / 100f) * 0.5f)

        if (i % 2 == 0) {
            drawSmallHeart(pPos, Color(0xFFF43F5E).copy(alpha = alpha), 12.dp.toPx())
        } else {
            drawSmallStar(pPos, Color(0xFFF59E0B).copy(alpha = alpha), 11.dp.toPx())
        }
    }
}

private fun DrawScope.drawSmallHeart(center: Offset, color: Color, sizePx: Float) {
    val path = Path().apply {
        val w = sizePx
        val h = sizePx
        moveTo(center.x, center.y + h * 0.3f)
        cubicTo(center.x - w * 0.5f, center.y - h * 0.3f, center.x - w * 0.5f, center.y - h * 0.6f, center.x, center.y - h * 0.3f)
        cubicTo(center.x + w * 0.5f, center.y - h * 0.6f, center.x + w * 0.5f, center.y - h * 0.3f, center.x, center.y + h * 0.3f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawSmallStar(center: Offset, color: Color, radius: Float) {
    val path = Path()
    val numPoints = 4
    val innerRadius = radius * 0.45f
    for (i in 0 until numPoints * 2) {
        val r = if (i % 2 == 0) radius else innerRadius
        val angle = i * Math.PI / numPoints - Math.PI / 2
        val x = (center.x + r * cos(angle)).toFloat()
        val y = (center.y + r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawSparkle(center: Offset, color: Color, radius: Float) {
    drawCircle(color, radius = radius * 0.6f, center = center)
    drawLine(color, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
}

/**
 * Dramatic Cinematic Hatching Cutscene with screen darken, intense egg shaking, light burst, and creature reveal!
 */
@Composable
private fun CinematicHatchingCutscene(
    chosenName: String,
    species: Species,
    onComplete: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) } // 0: Darken, 1: Shaking & Cracking, 2: Flash, 3: Revealed

    val shakeAnim = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }
    val creatureScale = remember { Animatable(0.2f) }

    LaunchedEffect(Unit) {
        // Step 0: Darken screen
        step = 0
        delay(600)

        // Step 1: Intense Shaking & Cracks
        step = 1
        repeat(14) { i ->
            val mag = (i + 1) * 2.2f
            shakeAnim.animateTo(mag, animationSpec = tween(60, easing = LinearEasing))
            shakeAnim.animateTo(-mag, animationSpec = tween(60, easing = LinearEasing))
        }
        shakeAnim.animateTo(0f)

        // Step 2: Screen Flash
        step = 2
        flashAlpha.animateTo(1f, animationSpec = tween(250))
        delay(150)

        // Step 3: Reveal Creature
        step = 3
        flashAlpha.animateTo(0f, animationSpec = tween(600))
        creatureScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (step >= 3) 0.88f else 0.94f))
            .testTag("cinematic_hatching_overlay"),
        contentAlignment = Alignment.Center
    ) {
        if (step < 3) {
            // Shaking Egg Stage
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = if (step == 0) "O ovo está esquentando..." else "O ovo está rachando!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFDE68A),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(30.dp))

                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .scale(1f + (step * 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f + shakeAnim.value
                        val cy = size.height / 2f

                        // Egg Shell with deep cracking
                        drawEggProtagonist(
                            centerX = cx,
                            centerY = cy,
                            scale = 1.15f,
                            progress = 100,
                            stage = EggVisualStage.ALMOST_HATCHING,
                            isTouching = true
                        )
                    }
                }
            }
        } else {
            // STEP 3: REVEALED CREATURE (Pokémon / Animal Crossing style celebration)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .scale(creatureScale.value)
                    .testTag("cinematic_revealed_container"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🎉 NASCEU!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFBBF24)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Creature Canvas Representation
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Dummy pet for renderer representation
                        val dummyPet = remember {
                            PetEntity(
                                id = 1,
                                name = chosenName,
                                speciesId = species.id,
                                rarity = species.rarity.name,
                                stage = "FILHOTE",
                                hunger = 100,
                                energy = 100,
                                happiness = 100,
                                hygiene = 100,
                                health = 100,
                                isHatched = true
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(CircleShape)
                                .background(Color(species.secondaryColorHex).copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            PetCanvasRenderer(
                                pet = dummyPet,
                                size = 180.dp,
                                isInteracting = true
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = chosenName,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Rarity & Species Label
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(species.rarity.colorHex).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(species.rarity.colorHex))
                            ) {
                                Text(
                                    text = "✨ ${species.rarity.displayName}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color(species.rarity.colorHex),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Espécie: ${species.displayName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF475569)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "\"${species.soundLabel}\"",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = species.description,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onComplete,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("welcome_pet_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "💖 Entrar no Mundo com $chosenName!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        // Screen Flash Effect
        if (flashAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha.value))
            )
        }
    }
}
