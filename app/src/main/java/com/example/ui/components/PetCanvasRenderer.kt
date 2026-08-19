package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.PetEntity
import com.example.data.model.PetBehaviorState
import com.example.data.model.PetStage
import com.example.data.model.Rarity
import com.example.data.model.Species
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PetCanvasRenderer(
    pet: PetEntity,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    behaviorState: PetBehaviorState = PetBehaviorState.OCIOSO,
    walkOffsetX: Float = 0f,
    walkDirection: Float = 1f,
    blinkProgress: Float = 0f,
    lookGazeX: Float = 0f,
    lookGazeY: Float = 0f,
    jumpProgress: Float = 0f,
    isSquishing: Boolean = false,
    isInteracting: Boolean = false,
    showBubbles: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pet_anim")

    // Ambient breathing - grounded when idle, gentle rhythm when sleeping
    val breathingOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (pet.isSleeping || behaviorState == PetBehaviorState.DORMINDO) 2.5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                if (pet.isSleeping || behaviorState == PetBehaviorState.DORMINDO) 1800 else 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Subtle rhythmic chest expansion without moving feet off ground
    val idleChestScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (pet.isSleeping || behaviorState == PetBehaviorState.DORMINDO) 1.04f else 1.012f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                if (pet.isSleeping || behaviorState == PetBehaviorState.DORMINDO) 1800 else 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_chest_scale"
    )

    // Tail wag / wing flutter / walking waddle
    val flutterAngle by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                if (behaviorState == PetBehaviorState.FELIZ || behaviorState == PetBehaviorState.BRINCANDO) 250 else if (behaviorState == PetBehaviorState.CAMINHANDO) 300 else 600,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flutter"
    )

    // Walking leg step alternation
    val stepPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "step_phase"
    )

    // Chewing animation for eating
    val chewPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(320, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "chew_phase"
    )

    // Floating particles (Zzz, hearts, sparkles, sweatdrop, teardrop)
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particles"
    )

    val eggRockAngle by infiniteTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "egg_rock"
    )

    val species = Species.fromId(pet.speciesId)
    val stage = try {
        PetStage.valueOf(pet.stage)
    } catch (_: Exception) {
        PetStage.OVO
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("pet_canvas_box"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().testTag("pet_canvas")) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            val centerX = canvasWidth / 2f

            val isSleeping = pet.isSleeping || behaviorState == PetBehaviorState.DORMINDO

            // Common Ground Reference Plane inside the canvas
            val groundY = canvasHeight * 0.70f

            // Vertical offset from pet center (centerY) to the bottom of paws/talons on the floor
            val feetBottomOffsetY = if (!pet.isHatched || stage == PetStage.OVO) {
                65.dp.toPx()
            } else {
                38.dp.toPx() * stage.scaleMultiplier
            }

            // Ground-anchored center position when awake; canvas-centered when sleeping in bed
            val baseCenterY = if (isSleeping) {
                canvasHeight * 0.50f
            } else {
                groundY - feetBottomOffsetY
            }

            // Calculate vertical offsets for animations
            val isSitting = behaviorState == PetBehaviorState.SENTADO
            val isJumping = (behaviorState == PetBehaviorState.PULANDO || behaviorState == PetBehaviorState.BRINCANDO || jumpProgress > 0.05f) && !isSleeping
            val jumpOffsetY = if (isJumping) -jumpProgress * 38.dp.toPx() else 0f
            val sitOffsetY = if (isSitting && !isSleeping) 4.dp.toPx() else 0f

            // Subtle walking step oscillation (anchored firmly to floor, no high flying jumps)
            val walkStepBounce = if (behaviorState == PetBehaviorState.CAMINHANDO && !isSleeping) {
                -kotlin.math.abs(sin(stepPhase * 6.28318f)) * 2.2.dp.toPx()
            } else 0f

            val centerY = baseCenterY + jumpOffsetY + sitOffsetY + walkStepBounce

            // Ground Shadow: directly underneath the feet, scaled dynamically during jumps (only when awake on the floor)
            if (!isSleeping) {
                val jumpShadowScale = (1f - (jumpProgress * 0.35f)).coerceIn(0.58f, 1.0f)
                val shadowWidth = 56.dp.toPx() * stage.scaleMultiplier * jumpShadowScale * (if (isSquishing) 1.15f else 1f)
                val shadowHeight = 9.dp.toPx() * jumpShadowScale
                drawOval(
                    color = Color(0x30000000),
                    topLeft = Offset(centerX - shadowWidth / 2f, groundY - shadowHeight * 0.40f),
                    size = Size(shadowWidth, shadowHeight)
                )
            }

            if (!pet.isHatched || stage == PetStage.OVO) {
                // Render Egg
                drawEgg(
                    centerX = centerX,
                    centerY = centerY,
                    rockAngle = eggRockAngle,
                    warmProgress = pet.eggWarmProgress,
                    isWarming = isInteracting
                )
            } else {
                // Render Hatched Pet
                val healthState = com.example.data.model.PetHealthRules.getHealthState(pet.health)
                val hasNamedDisease = pet.disease.isNotEmpty() && pet.disease != "NONE"
                val showIllnessVisual =
                    healthState == com.example.data.model.PetHealthState.DOENTE ||
                        healthState == com.example.data.model.PetHealthState.CRITICO ||
                        hasNamedDisease ||
                        behaviorState == PetBehaviorState.DOENTE
                val showTiredEyes =
                    showIllnessVisual ||
                        healthState == com.example.data.model.PetHealthState.INDISPOSTO
                val isCritical = healthState == com.example.data.model.PetHealthState.CRITICO
                val isDirty = pet.hygiene < 35
                val isHappy = pet.happiness > 75 || behaviorState == PetBehaviorState.FELIZ || behaviorState == PetBehaviorState.BRINCANDO
                val isHungry = pet.hunger < 30 || behaviorState == PetBehaviorState.PROCURANDO_COMIDA

                // Horizontal flip based on walk direction (when sleeping, face right towards foot of bed)
                val flipScaleX = if (isSleeping) 1f else (if (walkDirection < 0f) -1f else 1f)

                // Sleep scale reduction: ~18% smaller for cozy Tamagotchi bed feel
                val sleepScale = if (isSleeping) 0.82f else 1.0f

                val sleepingBreathingScaleX = if (isSleeping) 1.05f * idleChestScale else 1f
                val sleepingBreathingScaleY = if (isSleeping) 0.95f * (2f - idleChestScale) else idleChestScale
                val squashScaleX = (if (isSquishing) 1.16f else if (isJumping) 0.92f else if (isSitting) 1.08f else 1f) * sleepingBreathingScaleX * sleepScale
                val squashScaleY = (if (isSquishing) 0.84f else if (isJumping) 1.15f else if (isSitting) 0.92f else 1f) * sleepingBreathingScaleY * sleepScale

                // Body waddle angle when walking or gentle head/body rest angle when sleeping
                val sleepBedTiltAngle = if (isSleeping) -15f else 0f
                val sleepHeadSway = if (isSleeping) sin(floatAnim * 3.14159f) * 2.5f else 0f
                val waddleAngle = if (behaviorState == PetBehaviorState.CAMINHANDO && !isSleeping) {
                    sin(stepPhase * 6.28318f) * 6f
                } else 0f

                val finalRotation = sleepBedTiltAngle + sleepHeadSway + waddleAngle

                scale(scaleX = flipScaleX * squashScaleX, scaleY = squashScaleY, pivot = Offset(centerX, centerY)) {
                    rotate(finalRotation, pivot = Offset(centerX, centerY + (if (isSleeping) 0f else 30.dp.toPx()))) {
                        drawHatchedPet(
                            pet = pet,
                            species = species,
                            stage = stage,
                            behaviorState = behaviorState,
                            centerX = centerX,
                            centerY = centerY,
                            flutterAngle = flutterAngle,
                            blinkProgress = blinkProgress,
                            lookGazeX = lookGazeX,
                            lookGazeY = lookGazeY,
                            stepPhase = stepPhase,
                            chewPhase = chewPhase,
                            isSleeping = isSleeping,
                            isDirty = isDirty,
                            showIllnessVisual = showIllnessVisual,
                            showTiredEyes = showTiredEyes,
                            isCritical = isCritical,
                            isHappy = isHappy,
                            isHungry = isHungry,
                            isSitting = isSitting
                        )
                    }
                }
            }

            // Draw Floating Effects
            if (showBubbles || behaviorState == PetBehaviorState.TOMANDO_BANHO) {
                drawSoapBubbles(centerX, centerY, floatAnim)
            }
            if (isSleeping) {
                drawZzz(centerX + 26.dp.toPx(), centerY - 32.dp.toPx(), floatAnim)
            } else if (behaviorState == PetBehaviorState.BOCEJANDO) {
                drawZzz(centerX + 40.dp.toPx(), centerY - 40.dp.toPx(), floatAnim)
            } else if (behaviorState == PetBehaviorState.FELIZ || (pet.happiness > 80 && !isInteracting)) {
                drawFloatingHearts(centerX, centerY - 55.dp.toPx(), floatAnim)
            } else if (behaviorState == PetBehaviorState.COM_SAUDADE) {
                drawFloatingHearts(centerX, centerY - 60.dp.toPx(), floatAnim)
            } else if (behaviorState == PetBehaviorState.BRINCANDO) {
                drawMusicNotes(centerX + 45.dp.toPx(), centerY - 50.dp.toPx(), floatAnim)
            } else if (behaviorState == PetBehaviorState.COMENDO) {
                drawFoodCrumbs(centerX, centerY + 12.dp.toPx(), chewPhase)
            } else if (behaviorState == PetBehaviorState.PROCURANDO_COMIDA) {
                drawSweatDrop(centerX + 32.dp.toPx(), centerY - 28.dp.toPx(), floatAnim)
            } else if (behaviorState == PetBehaviorState.TRISTE) {
                drawTearDrop(centerX + 18.dp.toPx(), centerY + 5.dp.toPx(), floatAnim)
            }
        }
    }
}

private fun DrawScope.drawEgg(
    centerX: Float,
    centerY: Float,
    rockAngle: Float,
    warmProgress: Int,
    isWarming: Boolean
) {
    rotate(if (isWarming) rockAngle * 1.5f else rockAngle, pivot = Offset(centerX, centerY + 40f)) {
        val eggW = 105.dp.toPx()
        val eggH = 135.dp.toPx()

        // Egg aura if getting warm
        if (warmProgress > 30) {
            val auraRadius = (warmProgress / 100f) * 80.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x66FDE047), Color(0x00FDE047)),
                    center = Offset(centerX, centerY),
                    radius = auraRadius
                ),
                center = Offset(centerX, centerY),
                radius = auraRadius
            )
        }

        // Egg Main Shell
        val eggPath = Path().apply {
            moveTo(centerX, centerY - eggH * 0.5f)
            cubicTo(
                centerX + eggW * 0.6f, centerY - eggH * 0.45f,
                centerX + eggW * 0.65f, centerY + eggH * 0.4f,
                centerX, centerY + eggH * 0.5f
            )
            cubicTo(
                centerX - eggW * 0.65f, centerY + eggH * 0.4f,
                centerX - eggW * 0.6f, centerY - eggH * 0.45f,
                centerX, centerY - eggH * 0.5f
            )
            close()
        }

        drawPath(
            path = eggPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFF7ED), Color(0xFFFDE68A), Color(0xFFF59E0B)),
                startY = centerY - eggH * 0.5f,
                endY = centerY + eggH * 0.5f
            )
        )

        // Egg Outline
        drawPath(
            path = eggPath,
            color = Color(0xFF78350F),
            style = Stroke(width = 3.dp.toPx())
        )

        // Cute Egg Spots / Pattern
        val spotBrush = Brush.linearGradient(listOf(Color(0xFFF472B6), Color(0xFFEC4899)))
        drawCircle(spotBrush, radius = 12.dp.toPx(), center = Offset(centerX - 24.dp.toPx(), centerY - 10.dp.toPx()))
        drawCircle(spotBrush, radius = 8.dp.toPx(), center = Offset(centerX + 26.dp.toPx(), centerY - 25.dp.toPx()))
        drawCircle(spotBrush, radius = 15.dp.toPx(), center = Offset(centerX + 18.dp.toPx(), centerY + 24.dp.toPx()))
        drawCircle(spotBrush, radius = 9.dp.toPx(), center = Offset(centerX - 20.dp.toPx(), centerY + 30.dp.toPx()))

        // Egg Shell Cracks based on warm progress
        if (warmProgress >= 40) {
            val crackPath = Path().apply {
                moveTo(centerX - 10.dp.toPx(), centerY - 15.dp.toPx())
                lineTo(centerX + 5.dp.toPx(), centerY - 5.dp.toPx())
                lineTo(centerX - 8.dp.toPx(), centerY + 8.dp.toPx())
                lineTo(centerX + 12.dp.toPx(), centerY + 20.dp.toPx())
            }
            drawPath(crackPath, color = Color(0xFF451A03), style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        if (warmProgress >= 75) {
            val crackPath2 = Path().apply {
                moveTo(centerX + 22.dp.toPx(), centerY - 10.dp.toPx())
                lineTo(centerX + 10.dp.toPx(), centerY + 2.dp.toPx())
                lineTo(centerX + 25.dp.toPx(), centerY + 14.dp.toPx())
            }
            drawPath(crackPath2, color = Color(0xFF451A03), style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        }

        // Egg Highlight
        drawOval(
            color = Color(0x88FFFFFF),
            topLeft = Offset(centerX - eggW * 0.35f, centerY - eggH * 0.35f),
            size = Size(18.dp.toPx(), 32.dp.toPx())
        )
    }
}

private fun DrawScope.drawHatchedPet(
    pet: PetEntity,
    species: Species,
    stage: PetStage,
    behaviorState: PetBehaviorState,
    centerX: Float,
    centerY: Float,
    flutterAngle: Float,
    blinkProgress: Float,
    lookGazeX: Float,
    lookGazeY: Float,
    stepPhase: Float,
    chewPhase: Float,
    isSleeping: Boolean,
    isDirty: Boolean,
    showIllnessVisual: Boolean,
    showTiredEyes: Boolean,
    isCritical: Boolean,
    isHappy: Boolean,
    isHungry: Boolean,
    isSitting: Boolean
) {
    val scale = stage.scaleMultiplier
    scale(scale, pivot = Offset(centerX, centerY)) {
        val primaryColor = Color(species.primaryColorHex)
        val secondaryColor = Color(species.secondaryColorHex)
        val outlineColor = Color(0xFF1E293B)

        // 1. Rarity Aura (Epics & Legendaries) or Elder Wisdom Aura
        if (stage == PetStage.IDOSO) {
            // Golden Aura of Wisdom & Ancient Bond for Senior Pets
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x44F59E0B), Color(0x15FBBF24), Color.Transparent),
                    center = Offset(centerX, centerY),
                    radius = 100.dp.toPx()
                ),
                center = Offset(centerX, centerY),
                radius = 100.dp.toPx()
            )
        } else if (species.rarity == Rarity.LENDARIA || species.rarity == Rarity.EPICA) {
            val auraColor = if (species.rarity == Rarity.LENDARIA) {
                if (species == Species.FENIX) Color(0x66EF4444) else Color(0x66F59E0B)
            } else Color(0x559333EA)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(auraColor, Color.Transparent),
                    center = Offset(centerX, centerY),
                    radius = 95.dp.toPx()
                ),
                center = Offset(centerX, centerY),
                radius = 95.dp.toPx()
            )
        }

        // 2. Wings / Tail / Carapace (Behind Body)
        drawWingsAndTail(species, stage, centerX, centerY, flutterAngle, primaryColor, secondaryColor, outlineColor)

        // 3. Main Body
        val bodyW = if (species == Species.HAMSTER) 80.dp.toPx() else 74.dp.toPx()
        val bodyH = if (species == Species.HAMSTER) 68.dp.toPx() else 70.dp.toPx()

        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.92f)),
                startY = centerY - bodyH * 0.5f,
                endY = centerY + bodyH * 0.5f
            ),
            topLeft = Offset(centerX - bodyW / 2, centerY - bodyH / 2),
            size = Size(bodyW, bodyH)
        )
        drawOval(
            color = outlineColor,
            topLeft = Offset(centerX - bodyW / 2, centerY - bodyH / 2),
            size = Size(bodyW, bodyH),
            style = Stroke(width = 3.dp.toPx())
        )

        // Belly Patch
        if (species != Species.POLVO) {
            val bellyW = if (species == Species.PANDA) bodyW * 0.75f else bodyW * 0.68f
            val bellyH = bodyH * 0.52f
            drawOval(
                color = secondaryColor,
                topLeft = Offset(centerX - (bellyW / 2), centerY - (bodyH * 0.08f)),
                size = Size(bellyW, bellyH)
            )
        }

        // 4. Ears / Horns / Mane / Head Details
        drawEarsAndHorns(species, stage, centerX, centerY, primaryColor, secondaryColor, outlineColor)

        // 5. Species Special Markings (Whiskers, Panda eye patches, Raccoon mask, Tiger stripes, Owl facial disk, Turtle scutes)
        drawSpeciesMarkings(species, stage, centerX, centerY, primaryColor, secondaryColor, outlineColor)

        // 6. Facial Expressions (Eyes, Beak/Nose, Teeth, Mouth, Cheeks)
        drawFace(
            species = species,
            centerX = centerX,
            centerY = centerY,
            behaviorState = behaviorState,
            blinkProgress = blinkProgress,
            lookGazeX = lookGazeX,
            lookGazeY = lookGazeY,
            chewPhase = chewPhase,
            isSleeping = isSleeping,
            showTiredEyes = showTiredEyes,
            showIllnessVisual = showIllnessVisual,
            isCritical = isCritical,
            isHappy = isHappy,
            isHungry = isHungry,
            outlineColor = outlineColor
        )

        // 7. Paws / Feet / Talons (Dynamic stepping or sitting)
        if (species != Species.POLVO) {
            drawPaws(
                species = species,
                centerX = centerX,
                centerY = centerY,
                behaviorState = behaviorState,
                stepPhase = stepPhase,
                pawColor = when (species) {
                    Species.PANDA -> Color(0xFF1E293B)
                    Species.RAPOSA -> Color(0xFF374151)
                    Species.CORUJA, Species.FENIX -> Color(0xFFF59E0B)
                    else -> secondaryColor
                },
                outlineColor = outlineColor,
                isSitting = isSitting
            )
        }

        // 8. Dirt patches if dirty
        if (isDirty) {
            drawCircle(Color(0xBB78350F), radius = 6.dp.toPx(), center = Offset(centerX - 18.dp.toPx(), centerY - 8.dp.toPx()))
            drawCircle(Color(0xBB78350F), radius = 4.dp.toPx(), center = Offset(centerX + 20.dp.toPx(), centerY + 12.dp.toPx()))
            drawCircle(Color(0xBB78350F), radius = 5.dp.toPx(), center = Offset(centerX + 5.dp.toPx(), centerY - 25.dp.toPx()))
        }

        // 9. Sick Band-Aid + indicador de doença (não usa olhos em X)
        if (showIllnessVisual) {
            // Curativo principal
            drawRoundRect(
                color = Color(0xFFFFCC80),
                topLeft = Offset(centerX + 12.dp.toPx(), centerY - 32.dp.toPx()),
                size = Size(20.dp.toPx(), 8.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            drawLine(
                color = Color(0xFFEF4444),
                start = Offset(centerX + 18.dp.toPx(), centerY - 28.dp.toPx()),
                end = Offset(centerX + 26.dp.toPx(), centerY - 28.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFEF4444),
                start = Offset(centerX + 22.dp.toPx(), centerY - 32.dp.toPx()),
                end = Offset(centerX + 22.dp.toPx(), centerY - 24.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Gotas de suor / alerta crítico
            if (isCritical) {
                drawOval(
                    color = Color(0xAA60A5FA),
                    topLeft = Offset(centerX - 34.dp.toPx(), centerY - 18.dp.toPx()),
                    size = Size(5.dp.toPx(), 8.dp.toPx())
                )
                drawOval(
                    color = Color(0xAA60A5FA),
                    topLeft = Offset(centerX - 28.dp.toPx(), centerY - 8.dp.toPx()),
                    size = Size(4.dp.toPx(), 6.dp.toPx())
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x33EF4444), Color.Transparent),
                        center = Offset(centerX, centerY),
                        radius = 70.dp.toPx()
                    ),
                    center = Offset(centerX, centerY),
                    radius = 70.dp.toPx()
                )
            }
        }

        // 10. Equipped Wearables (Hats & Accessories)
        drawEquippedItems(pet.equippedHat, pet.equippedAccessory, centerX, centerY, species, stage)
    }
}

private fun DrawScope.drawPaws(
    species: Species,
    centerX: Float,
    centerY: Float,
    behaviorState: PetBehaviorState,
    stepPhase: Float,
    pawColor: Color,
    outlineColor: Color,
    isSitting: Boolean
) {
    val pawRadius = when (species) {
        Species.HAMSTER -> 7.dp.toPx()
        Species.TARTARUGA -> 11.dp.toPx()
        Species.CORUJA, Species.FENIX -> 8.dp.toPx()
        else -> 9.dp.toPx()
    }

    if (species == Species.CORUJA || species == Species.FENIX) {
        // Bird Talons
        val talonY = centerY + 31.dp.toPx()
        for (side in listOf(-1, 1)) {
            val tx = centerX + (side * 18.dp.toPx())
            drawLine(pawColor, start = Offset(tx, talonY), end = Offset(tx - 5.dp.toPx(), talonY + 6.dp.toPx()), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            drawLine(pawColor, start = Offset(tx, talonY), end = Offset(tx, talonY + 7.dp.toPx()), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            drawLine(pawColor, start = Offset(tx, talonY), end = Offset(tx + 5.dp.toPx(), talonY + 6.dp.toPx()), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
        return
    }

    if (isSitting) {
        // Tucked paws forward
        drawCircle(pawColor, radius = pawRadius, center = Offset(centerX - 18.dp.toPx(), centerY + 24.dp.toPx()))
        drawCircle(outlineColor, radius = pawRadius, center = Offset(centerX - 18.dp.toPx(), centerY + 24.dp.toPx()), style = Stroke(2.dp.toPx()))
        drawCircle(pawColor, radius = pawRadius, center = Offset(centerX + 18.dp.toPx(), centerY + 24.dp.toPx()))
        drawCircle(outlineColor, radius = pawRadius, center = Offset(centerX + 18.dp.toPx(), centerY + 24.dp.toPx()), style = Stroke(2.dp.toPx()))
    } else if (behaviorState == PetBehaviorState.CAMINHANDO) {
        // Stepping feet alternation
        val leftOffset = sin(stepPhase * 6.28318f) * 6.dp.toPx()
        val rightOffset = -sin(stepPhase * 6.28318f) * 6.dp.toPx()

        drawCircle(pawColor, radius = pawRadius, center = Offset(centerX - 22.dp.toPx(), centerY + 30.dp.toPx() + leftOffset))
        drawCircle(outlineColor, radius = pawRadius, center = Offset(centerX - 22.dp.toPx(), centerY + 30.dp.toPx() + leftOffset), style = Stroke(2.dp.toPx()))
        drawCircle(pawColor, radius = pawRadius, center = Offset(centerX + 22.dp.toPx(), centerY + 30.dp.toPx() + rightOffset))
        drawCircle(outlineColor, radius = pawRadius, center = Offset(centerX + 22.dp.toPx(), centerY + 30.dp.toPx() + rightOffset), style = Stroke(2.dp.toPx()))
    } else if (behaviorState == PetBehaviorState.COM_SAUDADE || behaviorState == PetBehaviorState.PULANDO) {
        // Raised waving paws!
        drawCircle(pawColor, radius = pawRadius, center = Offset(centerX - 26.dp.toPx(), centerY + 8.dp.toPx()))
        drawCircle(outlineColor, radius = pawRadius, center = Offset(centerX - 26.dp.toPx(), centerY + 8.dp.toPx()), style = Stroke(2.dp.toPx()))
        drawCircle(pawColor, radius = pawRadius, center = Offset(centerX + 26.dp.toPx(), centerY + 8.dp.toPx()))
        drawCircle(outlineColor, radius = pawRadius, center = Offset(centerX + 26.dp.toPx(), centerY + 8.dp.toPx()), style = Stroke(2.dp.toPx()))
    } else {
        // Standard paws
        drawCircle(pawColor, radius = pawRadius, center = Offset(centerX - 22.dp.toPx(), centerY + 30.dp.toPx()))
        drawCircle(outlineColor, radius = pawRadius, center = Offset(centerX - 22.dp.toPx(), centerY + 30.dp.toPx()), style = Stroke(2.dp.toPx()))
        drawCircle(pawColor, radius = pawRadius, center = Offset(centerX + 22.dp.toPx(), centerY + 30.dp.toPx()))
        drawCircle(outlineColor, radius = pawRadius, center = Offset(centerX + 22.dp.toPx(), centerY + 30.dp.toPx()), style = Stroke(2.dp.toPx()))
    }
}

private fun DrawScope.drawEarsAndHorns(
    species: Species,
    stage: PetStage,
    centerX: Float,
    centerY: Float,
    primaryColor: Color,
    secondaryColor: Color,
    outlineColor: Color
) {
    val earY = centerY - 32.dp.toPx()

    when (species) {
        Species.GATO -> {
            // Pointy Cat Ears
            val earH = if (stage == PetStage.ADULTO || stage == PetStage.IDOSO) 22.dp.toPx() else 18.dp.toPx()
            val leftEar = Path().apply {
                moveTo(centerX - 28.dp.toPx(), earY + 12.dp.toPx())
                lineTo(centerX - 35.dp.toPx(), earY - earH)
                lineTo(centerX - 12.dp.toPx(), earY + 4.dp.toPx())
                close()
            }
            val rightEar = Path().apply {
                moveTo(centerX + 28.dp.toPx(), earY + 12.dp.toPx())
                lineTo(centerX + 35.dp.toPx(), earY - earH)
                lineTo(centerX + 12.dp.toPx(), earY + 4.dp.toPx())
                close()
            }
            drawPath(leftEar, primaryColor)
            drawPath(leftEar, outlineColor, style = Stroke(2.5.dp.toPx()))
            drawPath(rightEar, primaryColor)
            drawPath(rightEar, outlineColor, style = Stroke(2.5.dp.toPx()))

            // Pink inner ears
            val innerLeft = Path().apply {
                moveTo(centerX - 26.dp.toPx(), earY + 8.dp.toPx())
                lineTo(centerX - 32.dp.toPx(), earY - earH + 6.dp.toPx())
                lineTo(centerX - 16.dp.toPx(), earY + 4.dp.toPx())
                close()
            }
            val innerRight = Path().apply {
                moveTo(centerX + 26.dp.toPx(), earY + 8.dp.toPx())
                lineTo(centerX + 32.dp.toPx(), earY - earH + 6.dp.toPx())
                lineTo(centerX + 16.dp.toPx(), earY + 4.dp.toPx())
                close()
            }
            drawPath(innerLeft, Color(0xFFF472B6))
            drawPath(innerRight, Color(0xFFF472B6))
        }

        Species.CACHORRO -> {
            // Floppy Puppy Ears on sides of head
            val earDrop = if (stage == PetStage.ADULTO || stage == PetStage.IDOSO) 36.dp.toPx() else 28.dp.toPx()
            drawOval(
                color = primaryColor,
                topLeft = Offset(centerX - 44.dp.toPx(), earY - 4.dp.toPx()),
                size = Size(18.dp.toPx(), earDrop)
            )
            drawOval(
                color = outlineColor,
                topLeft = Offset(centerX - 44.dp.toPx(), earY - 4.dp.toPx()),
                size = Size(18.dp.toPx(), earDrop),
                style = Stroke(2.5.dp.toPx())
            )
            drawOval(
                color = primaryColor,
                topLeft = Offset(centerX + 26.dp.toPx(), earY - 4.dp.toPx()),
                size = Size(18.dp.toPx(), earDrop)
            )
            drawOval(
                color = outlineColor,
                topLeft = Offset(centerX + 26.dp.toPx(), earY - 4.dp.toPx()),
                size = Size(18.dp.toPx(), earDrop),
                style = Stroke(2.5.dp.toPx())
            )
        }

        Species.COELHO -> {
            // Tall Distinctive Bunny Ears with pink interior!
            val earHeight = when (stage) {
                PetStage.FILHOTE -> 40.dp.toPx()
                PetStage.JOVEM -> 52.dp.toPx()
                PetStage.ADULTO, PetStage.IDOSO -> 60.dp.toPx()
                else -> 48.dp.toPx()
            }
            val earWidth = 16.dp.toPx()

            // Left Bunny Ear
            drawOval(
                color = primaryColor,
                topLeft = Offset(centerX - 28.dp.toPx(), earY - earHeight),
                size = Size(earWidth, earHeight + 10.dp.toPx())
            )
            drawOval(
                color = outlineColor,
                topLeft = Offset(centerX - 28.dp.toPx(), earY - earHeight),
                size = Size(earWidth, earHeight + 10.dp.toPx()),
                style = Stroke(2.5.dp.toPx())
            )
            // Left Pink Inner Ear
            drawOval(
                color = Color(0xFFF8BBD0),
                topLeft = Offset(centerX - 25.dp.toPx(), earY - earHeight + 6.dp.toPx()),
                size = Size(10.dp.toPx(), earHeight - 6.dp.toPx())
            )

            // Right Bunny Ear
            drawOval(
                color = primaryColor,
                topLeft = Offset(centerX + 12.dp.toPx(), earY - earHeight),
                size = Size(earWidth, earHeight + 10.dp.toPx())
            )
            drawOval(
                color = outlineColor,
                topLeft = Offset(centerX + 12.dp.toPx(), earY - earHeight),
                size = Size(earWidth, earHeight + 10.dp.toPx()),
                style = Stroke(2.5.dp.toPx())
            )
            // Right Pink Inner Ear
            drawOval(
                color = Color(0xFFF8BBD0),
                topLeft = Offset(centerX + 15.dp.toPx(), earY - earHeight + 6.dp.toPx()),
                size = Size(10.dp.toPx(), earHeight - 6.dp.toPx())
            )
        }

        Species.HAMSTER -> {
            // Cute small round hamster ears
            drawCircle(primaryColor, radius = 11.dp.toPx(), center = Offset(centerX - 24.dp.toPx(), earY - 2.dp.toPx()))
            drawCircle(outlineColor, radius = 11.dp.toPx(), center = Offset(centerX - 24.dp.toPx(), earY - 2.dp.toPx()), style = Stroke(2.5.dp.toPx()))
            drawCircle(Color(0xFFF472B6), radius = 6.dp.toPx(), center = Offset(centerX - 24.dp.toPx(), earY - 2.dp.toPx()))

            drawCircle(primaryColor, radius = 11.dp.toPx(), center = Offset(centerX + 24.dp.toPx(), earY - 2.dp.toPx()))
            drawCircle(outlineColor, radius = 11.dp.toPx(), center = Offset(centerX + 24.dp.toPx(), earY - 2.dp.toPx()), style = Stroke(2.5.dp.toPx()))
            drawCircle(Color(0xFFF472B6), radius = 6.dp.toPx(), center = Offset(centerX + 24.dp.toPx(), earY - 2.dp.toPx()))
        }

        Species.PANDA -> {
            // Distinctive Black Round Panda Ears
            drawCircle(Color(0xFF1E293B), radius = 14.dp.toPx(), center = Offset(centerX - 28.dp.toPx(), earY - 6.dp.toPx()))
            drawCircle(outlineColor, radius = 14.dp.toPx(), center = Offset(centerX - 28.dp.toPx(), earY - 6.dp.toPx()), style = Stroke(2.5.dp.toPx()))

            drawCircle(Color(0xFF1E293B), radius = 14.dp.toPx(), center = Offset(centerX + 28.dp.toPx(), earY - 6.dp.toPx()))
            drawCircle(outlineColor, radius = 14.dp.toPx(), center = Offset(centerX + 28.dp.toPx(), earY - 6.dp.toPx()), style = Stroke(2.5.dp.toPx()))
        }

        Species.GUAXINIM -> {
            // Round Raccoon Ears with white outer rim
            drawCircle(Color.White, radius = 14.dp.toPx(), center = Offset(centerX - 26.dp.toPx(), earY - 4.dp.toPx()))
            drawCircle(primaryColor, radius = 11.dp.toPx(), center = Offset(centerX - 26.dp.toPx(), earY - 4.dp.toPx()))
            drawCircle(outlineColor, radius = 14.dp.toPx(), center = Offset(centerX - 26.dp.toPx(), earY - 4.dp.toPx()), style = Stroke(2.5.dp.toPx()))

            drawCircle(Color.White, radius = 14.dp.toPx(), center = Offset(centerX + 26.dp.toPx(), earY - 4.dp.toPx()))
            drawCircle(primaryColor, radius = 11.dp.toPx(), center = Offset(centerX + 26.dp.toPx(), earY - 4.dp.toPx()))
            drawCircle(outlineColor, radius = 14.dp.toPx(), center = Offset(centerX + 26.dp.toPx(), earY - 4.dp.toPx()), style = Stroke(2.5.dp.toPx()))
        }

        Species.RAPOSA -> {
            // Large Pointed Fox Ears with black tips and white fluff
            val leftEar = Path().apply {
                moveTo(centerX - 28.dp.toPx(), earY + 10.dp.toPx())
                lineTo(centerX - 38.dp.toPx(), earY - 24.dp.toPx())
                lineTo(centerX - 12.dp.toPx(), earY + 2.dp.toPx())
                close()
            }
            val rightEar = Path().apply {
                moveTo(centerX + 28.dp.toPx(), earY + 10.dp.toPx())
                lineTo(centerX + 38.dp.toPx(), earY - 24.dp.toPx())
                lineTo(centerX + 12.dp.toPx(), earY + 2.dp.toPx())
                close()
            }
            drawPath(leftEar, primaryColor)
            drawPath(leftEar, outlineColor, style = Stroke(2.5.dp.toPx()))
            drawPath(rightEar, primaryColor)
            drawPath(rightEar, outlineColor, style = Stroke(2.5.dp.toPx()))

            // Black tips
            drawCircle(Color(0xFF1E293B), radius = 5.dp.toPx(), center = Offset(centerX - 37.dp.toPx(), earY - 22.dp.toPx()))
            drawCircle(Color(0xFF1E293B), radius = 5.dp.toPx(), center = Offset(centerX + 37.dp.toPx(), earY - 22.dp.toPx()))

            // White inner ear fluff
            val innerLeft = Path().apply {
                moveTo(centerX - 26.dp.toPx(), earY + 6.dp.toPx())
                lineTo(centerX - 33.dp.toPx(), earY - 14.dp.toPx())
                lineTo(centerX - 16.dp.toPx(), earY + 2.dp.toPx())
                close()
            }
            val innerRight = Path().apply {
                moveTo(centerX + 26.dp.toPx(), earY + 6.dp.toPx())
                lineTo(centerX + 33.dp.toPx(), earY - 14.dp.toPx())
                lineTo(centerX + 16.dp.toPx(), earY + 2.dp.toPx())
                close()
            }
            drawPath(innerLeft, Color.White)
            drawPath(innerRight, Color.White)
        }

        Species.LOBO -> {
            // Noble Wolf Ears with steel grey rim
            val leftEar = Path().apply {
                moveTo(centerX - 28.dp.toPx(), earY + 10.dp.toPx())
                lineTo(centerX - 36.dp.toPx(), earY - 20.dp.toPx())
                lineTo(centerX - 12.dp.toPx(), earY + 4.dp.toPx())
                close()
            }
            val rightEar = Path().apply {
                moveTo(centerX + 28.dp.toPx(), earY + 10.dp.toPx())
                lineTo(centerX + 36.dp.toPx(), earY - 20.dp.toPx())
                lineTo(centerX + 12.dp.toPx(), earY + 4.dp.toPx())
                close()
            }
            drawPath(leftEar, primaryColor)
            drawPath(leftEar, outlineColor, style = Stroke(2.5.dp.toPx()))
            drawPath(rightEar, primaryColor)
            drawPath(rightEar, outlineColor, style = Stroke(2.5.dp.toPx()))
            drawCircle(Color(0xFF374151), radius = 4.dp.toPx(), center = Offset(centerX - 35.dp.toPx(), earY - 18.dp.toPx()))
            drawCircle(Color(0xFF374151), radius = 4.dp.toPx(), center = Offset(centerX + 35.dp.toPx(), earY - 18.dp.toPx()))
        }

        Species.TIGRE -> {
            // Rounded Tiger Ears with black borders & white spot
            drawCircle(primaryColor, radius = 13.dp.toPx(), center = Offset(centerX - 26.dp.toPx(), earY - 6.dp.toPx()))
            drawCircle(outlineColor, radius = 13.dp.toPx(), center = Offset(centerX - 26.dp.toPx(), earY - 6.dp.toPx()), style = Stroke(2.5.dp.toPx()))
            drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(centerX - 26.dp.toPx(), earY - 6.dp.toPx()))

            drawCircle(primaryColor, radius = 13.dp.toPx(), center = Offset(centerX + 26.dp.toPx(), earY - 6.dp.toPx()))
            drawCircle(outlineColor, radius = 13.dp.toPx(), center = Offset(centerX + 26.dp.toPx(), earY - 6.dp.toPx()), style = Stroke(2.5.dp.toPx()))
            drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(centerX + 26.dp.toPx(), earY - 6.dp.toPx()))
        }

        Species.LEAO -> {
            // Majestic Lion Mane Framing the Head
            val maneRadius = when (stage) {
                PetStage.FILHOTE -> 44.dp.toPx()
                PetStage.JOVEM -> 50.dp.toPx()
                PetStage.ADULTO, PetStage.IDOSO -> 56.dp.toPx()
                else -> 48.dp.toPx()
            }
            val maneColor = if (stage == PetStage.IDOSO) Color(0xFFB45309) else Color(0xFFD97706)
            drawCircle(color = maneColor, radius = maneRadius, center = Offset(centerX, centerY - 6.dp.toPx()))
            drawCircle(color = outlineColor, radius = maneRadius, center = Offset(centerX, centerY - 6.dp.toPx()), style = Stroke(3.dp.toPx()))

            // Lion Ears on top of mane
            drawCircle(primaryColor, radius = 11.dp.toPx(), center = Offset(centerX - 28.dp.toPx(), earY - 4.dp.toPx()))
            drawCircle(outlineColor, radius = 11.dp.toPx(), center = Offset(centerX - 28.dp.toPx(), earY - 4.dp.toPx()), style = Stroke(2.dp.toPx()))
            drawCircle(primaryColor, radius = 11.dp.toPx(), center = Offset(centerX + 28.dp.toPx(), earY - 4.dp.toPx()))
            drawCircle(outlineColor, radius = 11.dp.toPx(), center = Offset(centerX + 28.dp.toPx(), earY - 4.dp.toPx()), style = Stroke(2.dp.toPx()))
        }

        Species.CORUJA -> {
            // Owl Feather Ear Horns / Tufts
            val leftTuft = Path().apply {
                moveTo(centerX - 24.dp.toPx(), earY + 12.dp.toPx())
                lineTo(centerX - 36.dp.toPx(), earY - 14.dp.toPx())
                lineTo(centerX - 14.dp.toPx(), earY + 2.dp.toPx())
                close()
            }
            val rightTuft = Path().apply {
                moveTo(centerX + 24.dp.toPx(), earY + 12.dp.toPx())
                lineTo(centerX + 36.dp.toPx(), earY - 14.dp.toPx())
                lineTo(centerX + 14.dp.toPx(), earY + 2.dp.toPx())
                close()
            }
            drawPath(leftTuft, primaryColor)
            drawPath(leftTuft, outlineColor, style = Stroke(2.5.dp.toPx()))
            drawPath(rightTuft, primaryColor)
            drawPath(rightTuft, outlineColor, style = Stroke(2.5.dp.toPx()))
        }

        Species.DRAGAO -> {
            // Sweeping Majestic Golden Dragon Horns
            val hornLength = when (stage) {
                PetStage.FILHOTE -> 20.dp.toPx()
                PetStage.JOVEM -> 28.dp.toPx()
                PetStage.ADULTO, PetStage.IDOSO -> 38.dp.toPx()
                else -> 28.dp.toPx()
            }
            val leftHorn = Path().apply {
                moveTo(centerX - 18.dp.toPx(), earY + 4.dp.toPx())
                lineTo(centerX - 38.dp.toPx(), earY - hornLength)
                lineTo(centerX - 28.dp.toPx(), earY - 4.dp.toPx())
                close()
            }
            val rightHorn = Path().apply {
                moveTo(centerX + 18.dp.toPx(), earY + 4.dp.toPx())
                lineTo(centerX + 38.dp.toPx(), earY - hornLength)
                lineTo(centerX + 28.dp.toPx(), earY - 4.dp.toPx())
                close()
            }
            drawPath(leftHorn, Color(0xFFF59E0B))
            drawPath(leftHorn, outlineColor, style = Stroke(2.5.dp.toPx()))
            drawPath(rightHorn, Color(0xFFF59E0B))
            drawPath(rightHorn, outlineColor, style = Stroke(2.5.dp.toPx()))
        }

        Species.FENIX -> {
            // Blazing Solar Fire Crest
            val crestHeight = when (stage) {
                PetStage.FILHOTE -> 24.dp.toPx()
                PetStage.JOVEM -> 34.dp.toPx()
                PetStage.ADULTO, PetStage.IDOSO -> 46.dp.toPx()
                else -> 34.dp.toPx()
            }
            val crest = Path().apply {
                moveTo(centerX - 14.dp.toPx(), earY + 4.dp.toPx())
                cubicTo(centerX - 10.dp.toPx(), earY - crestHeight * 0.6f, centerX - 18.dp.toPx(), earY - crestHeight * 0.8f, centerX, earY - crestHeight)
                cubicTo(centerX + 18.dp.toPx(), earY - crestHeight * 0.8f, centerX + 10.dp.toPx(), earY - crestHeight * 0.6f, centerX + 14.dp.toPx(), earY + 4.dp.toPx())
                close()
            }
            drawPath(crest, Brush.verticalGradient(listOf(Color(0xFFFDE047), Color(0xFFEF4444))))
            drawPath(crest, outlineColor, style = Stroke(2.dp.toPx()))
        }

        Species.POLVO -> {
            // Smooth Octopus Mantle Crown
            drawOval(primaryColor, topLeft = Offset(centerX - 20.dp.toPx(), earY - 14.dp.toPx()), size = Size(40.dp.toPx(), 22.dp.toPx()))
            drawOval(outlineColor, topLeft = Offset(centerX - 20.dp.toPx(), earY - 14.dp.toPx()), size = Size(40.dp.toPx(), 22.dp.toPx()), style = Stroke(2.5.dp.toPx()))
        }

        Species.TARTARUGA -> {
            // Smooth green reptilian dome head
            drawCircle(primaryColor, radius = 10.dp.toPx(), center = Offset(centerX, earY + 2.dp.toPx()))
        }
    }
}

private fun DrawScope.drawWingsAndTail(
    species: Species,
    stage: PetStage,
    centerX: Float,
    centerY: Float,
    flutterAngle: Float,
    primaryColor: Color,
    secondaryColor: Color,
    outlineColor: Color
) {
    when (species) {
        Species.COELHO -> {
            // Cute Fluffy Round Cotton-Ball Tail (Pompom)
            val pompomRadius = if (stage == PetStage.ADULTO || stage == PetStage.IDOSO) 15.dp.toPx() else 12.dp.toPx()
            drawCircle(Color.White, radius = pompomRadius, center = Offset(centerX + 34.dp.toPx(), centerY + 16.dp.toPx()))
            drawCircle(outlineColor, radius = pompomRadius, center = Offset(centerX + 34.dp.toPx(), centerY + 16.dp.toPx()), style = Stroke(2.5.dp.toPx()))
            drawCircle(Color(0xFFFCE7F3), radius = pompomRadius * 0.5f, center = Offset(centerX + 34.dp.toPx(), centerY + 16.dp.toPx()))
        }

        Species.HAMSTER -> {
            // Tiny Stubby Hamster Tail
            drawCircle(secondaryColor, radius = 7.dp.toPx(), center = Offset(centerX + 34.dp.toPx(), centerY + 20.dp.toPx()))
            drawCircle(outlineColor, radius = 7.dp.toPx(), center = Offset(centerX + 34.dp.toPx(), centerY + 20.dp.toPx()), style = Stroke(2.dp.toPx()))
        }

        Species.PANDA -> {
            // Small Black Round Panda Tail
            drawCircle(Color(0xFF1E293B), radius = 9.dp.toPx(), center = Offset(centerX + 34.dp.toPx(), centerY + 18.dp.toPx()))
            drawCircle(outlineColor, radius = 9.dp.toPx(), center = Offset(centerX + 34.dp.toPx(), centerY + 18.dp.toPx()), style = Stroke(2.dp.toPx()))
        }

        Species.RAPOSA -> {
            // Magnificent Bushy Fox Tail with White Tip
            rotate(flutterAngle * 1.6f, pivot = Offset(centerX + 28.dp.toPx(), centerY + 18.dp.toPx())) {
                val tailPath = Path().apply {
                    moveTo(centerX + 24.dp.toPx(), centerY + 16.dp.toPx())
                    cubicTo(centerX + 64.dp.toPx(), centerY + 12.dp.toPx(), centerX + 70.dp.toPx(), centerY - 32.dp.toPx(), centerX + 52.dp.toPx(), centerY - 28.dp.toPx())
                    cubicTo(centerX + 42.dp.toPx(), centerY - 15.dp.toPx(), centerX + 30.dp.toPx(), centerY + 6.dp.toPx(), centerX + 24.dp.toPx(), centerY + 16.dp.toPx())
                    close()
                }
                drawPath(tailPath, primaryColor)
                drawPath(tailPath, outlineColor, style = Stroke(2.5.dp.toPx()))
                // Iconic White Tip
                drawCircle(Color.White, radius = 10.dp.toPx(), center = Offset(centerX + 56.dp.toPx(), centerY - 26.dp.toPx()))
                drawCircle(outlineColor, radius = 10.dp.toPx(), center = Offset(centerX + 56.dp.toPx(), centerY - 26.dp.toPx()), style = Stroke(2.dp.toPx()))
            }
        }

        Species.GUAXINIM -> {
            // Iconic Striped Ringed Raccoon Tail (4 alternating black/grey bands)
            rotate(flutterAngle * 1.5f, pivot = Offset(centerX + 28.dp.toPx(), centerY + 18.dp.toPx())) {
                val tailPath = Path().apply {
                    moveTo(centerX + 24.dp.toPx(), centerY + 16.dp.toPx())
                    cubicTo(centerX + 60.dp.toPx(), centerY + 12.dp.toPx(), centerX + 64.dp.toPx(), centerY - 26.dp.toPx(), centerX + 48.dp.toPx(), centerY - 22.dp.toPx())
                    cubicTo(centerX + 40.dp.toPx(), centerY - 12.dp.toPx(), centerX + 30.dp.toPx(), centerY + 6.dp.toPx(), centerX + 24.dp.toPx(), centerY + 16.dp.toPx())
                    close()
                }
                drawPath(tailPath, primaryColor)
                drawPath(tailPath, outlineColor, style = Stroke(2.5.dp.toPx()))

                // Stripe rings
                drawLine(Color(0xFF1E293B), start = Offset(centerX + 32.dp.toPx(), centerY + 12.dp.toPx()), end = Offset(centerX + 38.dp.toPx(), centerY + 4.dp.toPx()), strokeWidth = 5.dp.toPx())
                drawLine(Color(0xFF1E293B), start = Offset(centerX + 42.dp.toPx(), centerY + 4.dp.toPx()), end = Offset(centerX + 50.dp.toPx(), centerY - 6.dp.toPx()), strokeWidth = 5.dp.toPx())
                drawLine(Color(0xFF1E293B), start = Offset(centerX + 50.dp.toPx(), centerY - 12.dp.toPx()), end = Offset(centerX + 58.dp.toPx(), centerY - 20.dp.toPx()), strokeWidth = 6.dp.toPx())
            }
        }

        Species.TIGRE -> {
            // Striped Tiger Tail
            rotate(flutterAngle * 1.4f, pivot = Offset(centerX + 28.dp.toPx(), centerY + 18.dp.toPx())) {
                val tailPath = Path().apply {
                    moveTo(centerX + 24.dp.toPx(), centerY + 16.dp.toPx())
                    cubicTo(centerX + 58.dp.toPx(), centerY + 12.dp.toPx(), centerX + 62.dp.toPx(), centerY - 24.dp.toPx(), centerX + 48.dp.toPx(), centerY - 20.dp.toPx())
                    cubicTo(centerX + 40.dp.toPx(), centerY - 10.dp.toPx(), centerX + 30.dp.toPx(), centerY + 6.dp.toPx(), centerX + 24.dp.toPx(), centerY + 16.dp.toPx())
                    close()
                }
                drawPath(tailPath, primaryColor)
                drawPath(tailPath, outlineColor, style = Stroke(2.5.dp.toPx()))

                // Black Rings
                drawLine(Color(0xFF1E293B), start = Offset(centerX + 34.dp.toPx(), centerY + 10.dp.toPx()), end = Offset(centerX + 40.dp.toPx(), centerY + 4.dp.toPx()), strokeWidth = 4.dp.toPx())
                drawLine(Color(0xFF1E293B), start = Offset(centerX + 44.dp.toPx(), centerY + 2.dp.toPx()), end = Offset(centerX + 52.dp.toPx(), centerY - 6.dp.toPx()), strokeWidth = 4.dp.toPx())
                drawCircle(Color(0xFF1E293B), radius = 5.dp.toPx(), center = Offset(centerX + 54.dp.toPx(), centerY - 22.dp.toPx()))
            }
        }

        Species.LEAO -> {
            // Lion Tail with Dark Fluffy Tuft at End
            rotate(flutterAngle * 1.4f, pivot = Offset(centerX + 28.dp.toPx(), centerY + 18.dp.toPx())) {
                val tailPath = Path().apply {
                    moveTo(centerX + 24.dp.toPx(), centerY + 16.dp.toPx())
                    cubicTo(centerX + 56.dp.toPx(), centerY + 14.dp.toPx(), centerX + 60.dp.toPx(), centerY - 20.dp.toPx(), centerX + 48.dp.toPx(), centerY - 18.dp.toPx())
                    cubicTo(centerX + 42.dp.toPx(), centerY - 10.dp.toPx(), centerX + 30.dp.toPx(), centerY + 6.dp.toPx(), centerX + 24.dp.toPx(), centerY + 16.dp.toPx())
                    close()
                }
                drawPath(tailPath, primaryColor)
                drawPath(tailPath, outlineColor, style = Stroke(2.5.dp.toPx()))
                // Bushy dark brown tuft
                drawCircle(Color(0xFF78350F), radius = 8.dp.toPx(), center = Offset(centerX + 52.dp.toPx(), centerY - 20.dp.toPx()))
                drawCircle(outlineColor, radius = 8.dp.toPx(), center = Offset(centerX + 52.dp.toPx(), centerY - 20.dp.toPx()), style = Stroke(2.dp.toPx()))
            }
        }

        Species.GATO, Species.CACHORRO, Species.LOBO -> {
            // Classic Wagging Mammal Tail
            rotate(flutterAngle * 1.5f, pivot = Offset(centerX + 28.dp.toPx(), centerY + 18.dp.toPx())) {
                val tailPath = Path().apply {
                    moveTo(centerX + 25.dp.toPx(), centerY + 15.dp.toPx())
                    cubicTo(centerX + 55.dp.toPx(), centerY + 10.dp.toPx(), centerX + 58.dp.toPx(), centerY - 25.dp.toPx(), centerX + 46.dp.toPx(), centerY - 20.dp.toPx())
                    cubicTo(centerX + 40.dp.toPx(), centerY - 10.dp.toPx(), centerX + 30.dp.toPx(), centerY + 5.dp.toPx(), centerX + 25.dp.toPx(), centerY + 15.dp.toPx())
                    close()
                }
                drawPath(tailPath, primaryColor)
                drawPath(tailPath, outlineColor, style = Stroke(2.5.dp.toPx()))

                if (species == Species.LOBO) {
                    drawCircle(secondaryColor, radius = 7.dp.toPx(), center = Offset(centerX + 48.dp.toPx(), centerY - 20.dp.toPx()))
                }
            }
        }

        Species.TARTARUGA -> {
            // Domed Turtle Shell (Carapace) Behind Body with Hexagon Scutes
            drawOval(
                color = Color(0xFF2E7D32),
                topLeft = Offset(centerX - 44.dp.toPx(), centerY - 38.dp.toPx()),
                size = Size(88.dp.toPx(), 80.dp.toPx())
            )
            drawOval(
                color = outlineColor,
                topLeft = Offset(centerX - 44.dp.toPx(), centerY - 38.dp.toPx()),
                size = Size(88.dp.toPx(), 80.dp.toPx()),
                style = Stroke(3.5.dp.toPx())
            )
            // Turtle Tail
            val turtleTail = Path().apply {
                moveTo(centerX + 36.dp.toPx(), centerY + 10.dp.toPx())
                lineTo(centerX + 48.dp.toPx(), centerY + 14.dp.toPx())
                lineTo(centerX + 36.dp.toPx(), centerY + 18.dp.toPx())
                close()
            }
            drawPath(turtleTail, primaryColor)
            drawPath(turtleTail, outlineColor, style = Stroke(2.dp.toPx()))
        }

        Species.CORUJA -> {
            // Feathered Owl Wings
            val wingSpan = if (stage == PetStage.ADULTO || stage == PetStage.IDOSO) 44.dp.toPx() else 36.dp.toPx()
            rotate(flutterAngle * 1.2f, pivot = Offset(centerX - 35.dp.toPx(), centerY)) {
                val leftWing = Path().apply {
                    moveTo(centerX - 28.dp.toPx(), centerY - 6.dp.toPx())
                    cubicTo(centerX - 28.dp.toPx() - wingSpan, centerY - 28.dp.toPx(), centerX - 30.dp.toPx() - wingSpan, centerY + 18.dp.toPx(), centerX - 28.dp.toPx(), centerY + 14.dp.toPx())
                    close()
                }
                drawPath(leftWing, primaryColor)
                drawPath(leftWing, outlineColor, style = Stroke(2.5.dp.toPx()))
            }
            rotate(-flutterAngle * 1.2f, pivot = Offset(centerX + 35.dp.toPx(), centerY)) {
                val rightWing = Path().apply {
                    moveTo(centerX + 28.dp.toPx(), centerY - 6.dp.toPx())
                    cubicTo(centerX + 28.dp.toPx() + wingSpan, centerY - 28.dp.toPx(), centerX + 30.dp.toPx() + wingSpan, centerY + 18.dp.toPx(), centerX + 28.dp.toPx(), centerY + 14.dp.toPx())
                    close()
                }
                drawPath(rightWing, primaryColor)
                drawPath(rightWing, outlineColor, style = Stroke(2.5.dp.toPx()))
            }
        }

        Species.DRAGAO -> {
            // Majestic Bat-like Dragon Wings
            val wingSpan = if (stage == PetStage.ADULTO || stage == PetStage.IDOSO) 52.dp.toPx() else 40.dp.toPx()
            rotate(flutterAngle * 1.3f, pivot = Offset(centerX - 35.dp.toPx(), centerY)) {
                val leftWing = Path().apply {
                    moveTo(centerX - 28.dp.toPx(), centerY - 10.dp.toPx())
                    lineTo(centerX - 28.dp.toPx() - wingSpan, centerY - 32.dp.toPx())
                    lineTo(centerX - 20.dp.toPx() - wingSpan, centerY)
                    lineTo(centerX - 28.dp.toPx(), centerY + 12.dp.toPx())
                    close()
                }
                drawPath(leftWing, Color(0xFFF59E0B))
                drawPath(leftWing, outlineColor, style = Stroke(2.5.dp.toPx()))
            }
            rotate(-flutterAngle * 1.3f, pivot = Offset(centerX + 35.dp.toPx(), centerY)) {
                val rightWing = Path().apply {
                    moveTo(centerX + 28.dp.toPx(), centerY - 10.dp.toPx())
                    lineTo(centerX + 28.dp.toPx() + wingSpan, centerY - 32.dp.toPx())
                    lineTo(centerX + 20.dp.toPx() + wingSpan, centerY)
                    lineTo(centerX + 28.dp.toPx(), centerY + 12.dp.toPx())
                    close()
                }
                drawPath(rightWing, Color(0xFFF59E0B))
                drawPath(rightWing, outlineColor, style = Stroke(2.5.dp.toPx()))
            }

            // Dragon Tail with Spiked Arrow Tip
            val dragonTail = Path().apply {
                moveTo(centerX + 25.dp.toPx(), centerY + 16.dp.toPx())
                cubicTo(centerX + 55.dp.toPx(), centerY + 12.dp.toPx(), centerX + 62.dp.toPx(), centerY - 20.dp.toPx(), centerX + 50.dp.toPx(), centerY - 18.dp.toPx())
                close()
            }
            drawPath(dragonTail, primaryColor)
            drawPath(dragonTail, outlineColor, style = Stroke(2.5.dp.toPx()))
            // Flame Arrow Tip
            val arrowTip = Path().apply {
                moveTo(centerX + 48.dp.toPx(), centerY - 26.dp.toPx())
                lineTo(centerX + 62.dp.toPx(), centerY - 18.dp.toPx())
                lineTo(centerX + 46.dp.toPx(), centerY - 10.dp.toPx())
                close()
            }
            drawPath(arrowTip, Color(0xFFF59E0B))
            drawPath(arrowTip, outlineColor, style = Stroke(2.dp.toPx()))
        }

        Species.FENIX -> {
            // Blazing Solar Fire Wings
            val wingSpan = if (stage == PetStage.ADULTO || stage == PetStage.IDOSO) 52.dp.toPx() else 42.dp.toPx()
            rotate(flutterAngle * 1.4f, pivot = Offset(centerX - 35.dp.toPx(), centerY)) {
                val leftWing = Path().apply {
                    moveTo(centerX - 28.dp.toPx(), centerY - 10.dp.toPx())
                    cubicTo(centerX - 28.dp.toPx() - wingSpan, centerY - 38.dp.toPx(), centerX - 25.dp.toPx() - wingSpan, centerY + 20.dp.toPx(), centerX - 28.dp.toPx(), centerY + 14.dp.toPx())
                    close()
                }
                drawPath(leftWing, Brush.horizontalGradient(listOf(Color(0xFFFDE047), Color(0xFFEF4444))))
                drawPath(leftWing, outlineColor, style = Stroke(2.dp.toPx()))
            }
            rotate(-flutterAngle * 1.4f, pivot = Offset(centerX + 35.dp.toPx(), centerY)) {
                val rightWing = Path().apply {
                    moveTo(centerX + 28.dp.toPx(), centerY - 10.dp.toPx())
                    cubicTo(centerX + 28.dp.toPx() + wingSpan, centerY - 38.dp.toPx(), centerX + 25.dp.toPx() + wingSpan, centerY + 20.dp.toPx(), centerX + 28.dp.toPx(), centerY + 14.dp.toPx())
                    close()
                }
                drawPath(rightWing, Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFFDE047))))
                drawPath(rightWing, outlineColor, style = Stroke(2.dp.toPx()))
            }

            // Trio of Long Flowing Fire Streamer Ribbons (Tail)
            val streamCount = if (stage == PetStage.ADULTO || stage == PetStage.IDOSO) 3 else 2
            for (i in 0 until streamCount) {
                val yOffset = (i * 8.dp.toPx()) - 8.dp.toPx()
                val streamer = Path().apply {
                    moveTo(centerX + 24.dp.toPx(), centerY + 16.dp.toPx() + yOffset)
                    cubicTo(centerX + 50.dp.toPx(), centerY + 10.dp.toPx() + yOffset, centerX + 60.dp.toPx(), centerY - 30.dp.toPx() + yOffset, centerX + 50.dp.toPx(), centerY - 25.dp.toPx() + yOffset)
                    close()
                }
                drawPath(streamer, Brush.verticalGradient(listOf(Color(0xFFFDE047), Color(0xFFEF4444))))
                drawPath(streamer, outlineColor, style = Stroke(1.5.dp.toPx()))
            }
        }

        Species.POLVO -> {
            // 8 Animated Curving Tentacles with Round Suction Cups
            for (i in -3..4) {
                val tentacleX = centerX + (i * 8.5.dp.toPx()) - 4.dp.toPx()
                val tentacleY = centerY + 26.dp.toPx()
                val waveOffset = (sin(flutterAngle.toDouble() * 0.12 + i) * 7).toFloat()
                drawOval(
                    color = primaryColor,
                    topLeft = Offset(tentacleX - 4.dp.toPx(), tentacleY + waveOffset),
                    size = Size(9.dp.toPx(), 22.dp.toPx())
                )
                drawOval(
                    color = outlineColor,
                    topLeft = Offset(tentacleX - 4.dp.toPx(), tentacleY + waveOffset),
                    size = Size(9.dp.toPx(), 22.dp.toPx()),
                    style = Stroke(2.dp.toPx())
                )
                // Suction cups
                drawCircle(Color(0xFFFCE7F3), radius = 2.5.dp.toPx(), center = Offset(tentacleX, tentacleY + waveOffset + 14.dp.toPx()))
            }
        }
    }
}

private fun DrawScope.drawSpeciesMarkings(
    species: Species,
    stage: PetStage,
    centerX: Float,
    centerY: Float,
    primaryColor: Color,
    secondaryColor: Color,
    outlineColor: Color
) {
    when (species) {
        Species.GATO -> {
            // Whiskers
            val whiskerPath = Path().apply {
                moveTo(centerX - 16.dp.toPx(), centerY + 2.dp.toPx())
                lineTo(centerX - 36.dp.toPx(), centerY - 2.dp.toPx())
                moveTo(centerX - 16.dp.toPx(), centerY + 6.dp.toPx())
                lineTo(centerX - 36.dp.toPx(), centerY + 8.dp.toPx())
                moveTo(centerX + 16.dp.toPx(), centerY + 2.dp.toPx())
                lineTo(centerX + 36.dp.toPx(), centerY - 2.dp.toPx())
                moveTo(centerX + 16.dp.toPx(), centerY + 6.dp.toPx())
                lineTo(centerX + 36.dp.toPx(), centerY + 8.dp.toPx())
            }
            drawPath(whiskerPath, outlineColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }

        Species.COELHO -> {
            // Cute Rabbit Whiskers & White Cheek Puffs
            drawCircle(Color.White, radius = 9.dp.toPx(), center = Offset(centerX - 10.dp.toPx(), centerY + 3.dp.toPx()))
            drawCircle(Color.White, radius = 9.dp.toPx(), center = Offset(centerX + 10.dp.toPx(), centerY + 3.dp.toPx()))
            val whiskerPath = Path().apply {
                moveTo(centerX - 16.dp.toPx(), centerY + 2.dp.toPx())
                lineTo(centerX - 32.dp.toPx(), centerY + 1.dp.toPx())
                moveTo(centerX - 16.dp.toPx(), centerY + 6.dp.toPx())
                lineTo(centerX - 32.dp.toPx(), centerY + 8.dp.toPx())
                moveTo(centerX + 16.dp.toPx(), centerY + 2.dp.toPx())
                lineTo(centerX + 32.dp.toPx(), centerY + 1.dp.toPx())
                moveTo(centerX + 16.dp.toPx(), centerY + 6.dp.toPx())
                lineTo(centerX + 32.dp.toPx(), centerY + 8.dp.toPx())
            }
            drawPath(whiskerPath, outlineColor, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
        }

        Species.HAMSTER -> {
            // Chubby Puffed Cheek Pouches
            drawCircle(Color(0xFFFFCC80), radius = 15.dp.toPx(), center = Offset(centerX - 24.dp.toPx(), centerY + 6.dp.toPx()))
            drawCircle(outlineColor, radius = 15.dp.toPx(), center = Offset(centerX - 24.dp.toPx(), centerY + 6.dp.toPx()), style = Stroke(2.dp.toPx()))
            drawCircle(Color(0xFFFFCC80), radius = 15.dp.toPx(), center = Offset(centerX + 24.dp.toPx(), centerY + 6.dp.toPx()))
            drawCircle(outlineColor, radius = 15.dp.toPx(), center = Offset(centerX + 24.dp.toPx(), centerY + 6.dp.toPx()), style = Stroke(2.dp.toPx()))
        }

        Species.PANDA -> {
            // Iconic Black Eye Patches (Manchas nos Olhos)
            drawOval(
                color = Color(0xFF1E293B),
                topLeft = Offset(centerX - 26.dp.toPx(), centerY - 18.dp.toPx()),
                size = Size(18.dp.toPx(), 20.dp.toPx())
            )
            drawOval(
                color = Color(0xFF1E293B),
                topLeft = Offset(centerX + 8.dp.toPx(), centerY - 18.dp.toPx()),
                size = Size(18.dp.toPx(), 20.dp.toPx())
            )
        }

        Species.GUAXINIM -> {
            // Iconic Bandit Mask (Máscara de Guaxinim)
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(centerX - 30.dp.toPx(), centerY - 16.dp.toPx()),
                size = Size(60.dp.toPx(), 18.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
            // White Eyebrows
            drawArc(Color.White, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(centerX - 22.dp.toPx(), centerY - 20.dp.toPx()), size = Size(12.dp.toPx(), 8.dp.toPx()), style = Stroke(2.dp.toPx()))
            drawArc(Color.White, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(centerX + 10.dp.toPx(), centerY - 20.dp.toPx()), size = Size(12.dp.toPx(), 8.dp.toPx()), style = Stroke(2.dp.toPx()))
        }

        Species.TIGRE -> {
            // Forehead Imperial Tiger Stripes (王 mark) & Flank Stripes
            val stripePath = Path().apply {
                // Forehead stripes
                moveTo(centerX - 8.dp.toPx(), centerY - 24.dp.toPx())
                lineTo(centerX + 8.dp.toPx(), centerY - 24.dp.toPx())
                moveTo(centerX, centerY - 28.dp.toPx())
                lineTo(centerX, centerY - 16.dp.toPx())
                moveTo(centerX - 10.dp.toPx(), centerY - 18.dp.toPx())
                lineTo(centerX + 10.dp.toPx(), centerY - 18.dp.toPx())

                // Side stripes
                moveTo(centerX - 32.dp.toPx(), centerY - 10.dp.toPx())
                lineTo(centerX - 20.dp.toPx(), centerY - 6.dp.toPx())
                moveTo(centerX - 34.dp.toPx(), centerY + 4.dp.toPx())
                lineTo(centerX - 22.dp.toPx(), centerY + 6.dp.toPx())
                moveTo(centerX + 32.dp.toPx(), centerY - 10.dp.toPx())
                lineTo(centerX + 20.dp.toPx(), centerY - 6.dp.toPx())
                moveTo(centerX + 34.dp.toPx(), centerY + 4.dp.toPx())
                lineTo(centerX + 22.dp.toPx(), centerY + 6.dp.toPx())
            }
            drawPath(stripePath, Color(0xFF1E293B), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }

        Species.CORUJA -> {
            // Owl Facial Disk Circles & Chest Feather Flecks
            drawCircle(Color(0xFFFFF9C4), radius = 13.dp.toPx(), center = Offset(centerX - 16.dp.toPx(), centerY - 8.dp.toPx()))
            drawCircle(Color(0xFFFFF9C4), radius = 13.dp.toPx(), center = Offset(centerX + 16.dp.toPx(), centerY - 8.dp.toPx()))

            // V-shaped chest feathers
            val specklePath = Path().apply {
                moveTo(centerX - 8.dp.toPx(), centerY + 10.dp.toPx())
                lineTo(centerX - 5.dp.toPx(), centerY + 14.dp.toPx())
                lineTo(centerX - 2.dp.toPx(), centerY + 10.dp.toPx())

                moveTo(centerX + 2.dp.toPx(), centerY + 10.dp.toPx())
                lineTo(centerX + 5.dp.toPx(), centerY + 14.dp.toPx())
                lineTo(centerX + 8.dp.toPx(), centerY + 10.dp.toPx())

                moveTo(centerX - 3.dp.toPx(), centerY + 18.dp.toPx())
                lineTo(centerX, centerY + 22.dp.toPx())
                lineTo(centerX + 3.dp.toPx(), centerY + 18.dp.toPx())
            }
            drawPath(specklePath, Color(0xFF78350F), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }

        Species.RAPOSA, Species.LOBO -> {
            // White Cheek Fur Flares
            val cheekFlares = Path().apply {
                moveTo(centerX - 32.dp.toPx(), centerY)
                lineTo(centerX - 42.dp.toPx(), centerY + 6.dp.toPx())
                lineTo(centerX - 30.dp.toPx(), centerY + 12.dp.toPx())

                moveTo(centerX + 32.dp.toPx(), centerY)
                lineTo(centerX + 42.dp.toPx(), centerY + 6.dp.toPx())
                lineTo(centerX + 30.dp.toPx(), centerY + 12.dp.toPx())
            }
            drawPath(cheekFlares, Color.White)
            drawPath(cheekFlares, outlineColor, style = Stroke(2.dp.toPx()))
        }

        else -> {}
    }
}

private fun DrawScope.drawFace(
    species: Species,
    centerX: Float,
    centerY: Float,
    behaviorState: PetBehaviorState,
    blinkProgress: Float,
    lookGazeX: Float,
    lookGazeY: Float,
    chewPhase: Float,
    isSleeping: Boolean,
    showTiredEyes: Boolean,
    showIllnessVisual: Boolean,
    isCritical: Boolean,
    isHappy: Boolean,
    isHungry: Boolean,
    outlineColor: Color
) {
    val eyeY = centerY - 8.dp.toPx()
    val eyeSpacing = 16.dp.toPx()

    // Blushing cheeks (mais pálido se doente/crítico)
    if (species != Species.POLVO) {
        val cheek = if (showIllnessVisual) Color(0x44F472B6) else Color(0x88F472B6)
        drawCircle(cheek, radius = 6.dp.toPx(), center = Offset(centerX - 24.dp.toPx(), centerY + 3.dp.toPx()))
        drawCircle(cheek, radius = 6.dp.toPx(), center = Offset(centerX + 24.dp.toPx(), centerY + 3.dp.toPx()))
    }

    // Eye rendering based on behavior & blinking
    if (isSleeping || blinkProgress > 0.75f) {
        // Closed Sleeping / Blinking Eyes
        val leftEye = Path().apply {
            moveTo(centerX - eyeSpacing - 6.dp.toPx(), eyeY)
            quadraticTo(centerX - eyeSpacing, eyeY + 6.dp.toPx(), centerX - eyeSpacing + 6.dp.toPx(), eyeY)
        }
        val rightEye = Path().apply {
            moveTo(centerX + eyeSpacing - 6.dp.toPx(), eyeY)
            quadraticTo(centerX + eyeSpacing, eyeY + 6.dp.toPx(), centerX + eyeSpacing + 6.dp.toPx(), eyeY)
        }
        val eyeColor = if (species == Species.PANDA) Color.White else outlineColor
        drawPath(leftEye, eyeColor, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        drawPath(rightEye, eyeColor, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
    } else if (showTiredEyes) {
        // Olhos semicerrados/cansados (não "X" de morte)
        drawSleepyEye(centerX - eyeSpacing, eyeY, outlineColor)
        drawSleepyEye(centerX + eyeSpacing, eyeY, outlineColor)
        if (isCritical) {
            // Pálpebra mais pesada
            drawLine(
                outlineColor.copy(alpha = 0.55f),
                start = Offset(centerX - eyeSpacing - 5.dp.toPx(), eyeY - 3.dp.toPx()),
                end = Offset(centerX - eyeSpacing + 5.dp.toPx(), eyeY - 3.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                outlineColor.copy(alpha = 0.55f),
                start = Offset(centerX + eyeSpacing - 5.dp.toPx(), eyeY - 3.dp.toPx()),
                end = Offset(centerX + eyeSpacing + 5.dp.toPx(), eyeY - 3.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    } else if (behaviorState == PetBehaviorState.FELIZ || behaviorState == PetBehaviorState.COM_SAUDADE) {
        // Joyful Heart Eyes or Cheerful Arcs
        drawHeartEye(centerX - eyeSpacing, eyeY, Color(0xFFF43F5E))
        drawHeartEye(centerX + eyeSpacing, eyeY, Color(0xFFF43F5E))
    } else if (behaviorState == PetBehaviorState.BOCEJANDO) {
        // Sleepy half-closed droopy eyes
        drawSleepyEye(centerX - eyeSpacing, eyeY, outlineColor)
        drawSleepyEye(centerX + eyeSpacing, eyeY, outlineColor)
    } else {
        // Open eyes with dynamic gaze offset & sparkle
        val gazeOffsetX = lookGazeX * 3.5.dp.toPx()
        val gazeOffsetY = lookGazeY * 2.5.dp.toPx()
        val eyeHeightScale = (1f - blinkProgress * 0.8f).coerceIn(0.2f, 1f)

        drawLivingEye(centerX - eyeSpacing, eyeY, gazeOffsetX, gazeOffsetY, eyeHeightScale, isHappy)
        drawLivingEye(centerX + eyeSpacing, eyeY, gazeOffsetX, gazeOffsetY, eyeHeightScale, isHappy)
    }

    // Nose or Beak
    if (species == Species.CORUJA || species == Species.FENIX) {
        // Sharp Golden Bird Beak (Bico)
        val beak = Path().apply {
            moveTo(centerX - 5.dp.toPx(), centerY - 3.dp.toPx())
            lineTo(centerX + 5.dp.toPx(), centerY - 3.dp.toPx())
            lineTo(centerX, centerY + 6.dp.toPx())
            close()
        }
        drawPath(beak, Color(0xFFF59E0B))
        drawPath(beak, outlineColor, style = Stroke(1.5.dp.toPx()))
    } else if (species == Species.POLVO) {
        // Small circular siphon mouth
        drawCircle(Color(0xFFBE185D), radius = 3.dp.toPx(), center = Offset(centerX, centerY + 6.dp.toPx()))
        drawCircle(outlineColor, radius = 3.dp.toPx(), center = Offset(centerX, centerY + 6.dp.toPx()), style = Stroke(1.5.dp.toPx()))
    } else if (species != Species.TARTARUGA) {
        // Mammal Nose
        val noseColor = when (species) {
            Species.COELHO -> Color(0xFFF472B6)
            Species.HAMSTER -> Color(0xFFF472B6)
            Species.GATO -> Color(0xFFF472B6)
            Species.LEAO -> Color(0xFF78350F)
            else -> Color(0xFF1E293B)
        }
        drawCircle(noseColor, radius = 2.5.dp.toPx(), center = Offset(centerX, centerY - 1.dp.toPx()))
    }

    // Dynamic Mouth based on state & species
    if (species != Species.CORUJA && species != Species.FENIX && species != Species.POLVO) {
        when {
            isSleeping -> {
                drawCircle(Color(0xFFE11D48), radius = 3.dp.toPx(), center = Offset(centerX, centerY + 6.dp.toPx()))
            }
            behaviorState == PetBehaviorState.BOCEJANDO -> {
                // Yawning mouth
                drawOval(Color(0xFF881337), topLeft = Offset(centerX - 8.dp.toPx(), centerY + 3.dp.toPx()), size = Size(16.dp.toPx(), 14.dp.toPx()))
                drawOval(Color(0xFFFDA4AF), topLeft = Offset(centerX - 5.dp.toPx(), centerY + 9.dp.toPx()), size = Size(10.dp.toPx(), 7.dp.toPx()))
                drawOval(outlineColor, topLeft = Offset(centerX - 8.dp.toPx(), centerY + 3.dp.toPx()), size = Size(16.dp.toPx(), 14.dp.toPx()), style = Stroke(2.dp.toPx()))
            }
            behaviorState == PetBehaviorState.COMENDO -> {
                // Chewing mouth
                val chewH = 4.dp.toPx() + (sin(chewPhase * 6.28318f).coerceAtLeast(0f) * 6.dp.toPx())
                drawOval(Color(0xFFBE185D), topLeft = Offset(centerX - 6.dp.toPx(), centerY + 3.dp.toPx()), size = Size(12.dp.toPx(), chewH))
                drawOval(outlineColor, topLeft = Offset(centerX - 6.dp.toPx(), centerY + 3.dp.toPx()), size = Size(12.dp.toPx(), chewH), style = Stroke(2.dp.toPx()))
            }
            behaviorState == PetBehaviorState.PULANDO || behaviorState == PetBehaviorState.BRINCANDO || isHappy -> {
                // Open Laughing / Joyful Smile
                val mouth = Path().apply {
                    moveTo(centerX - 9.dp.toPx(), centerY + 3.dp.toPx())
                    quadraticTo(centerX, centerY + 14.dp.toPx(), centerX + 9.dp.toPx(), centerY + 3.dp.toPx())
                    close()
                }
                drawPath(mouth, Color(0xFFE11D48))
                drawCircle(Color(0xFFFDA4AF), radius = 4.dp.toPx(), center = Offset(centerX, centerY + 9.dp.toPx()))
                drawPath(mouth, outlineColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))

                // Bunny / Hamster Buck Teeth (Dentinhos da Frente)
                if (species == Species.COELHO || species == Species.HAMSTER) {
                    drawRect(Color.White, topLeft = Offset(centerX - 4.dp.toPx(), centerY + 3.dp.toPx()), size = Size(3.5.dp.toPx(), 5.dp.toPx()))
                    drawRect(Color.White, topLeft = Offset(centerX + 0.5.dp.toPx(), centerY + 3.dp.toPx()), size = Size(3.5.dp.toPx(), 5.dp.toPx()))
                    drawRect(outlineColor, topLeft = Offset(centerX - 4.dp.toPx(), centerY + 3.dp.toPx()), size = Size(3.5.dp.toPx(), 5.dp.toPx()), style = Stroke(1.dp.toPx()))
                    drawRect(outlineColor, topLeft = Offset(centerX + 0.5.dp.toPx(), centerY + 3.dp.toPx()), size = Size(3.5.dp.toPx(), 5.dp.toPx()), style = Stroke(1.dp.toPx()))
                }
            }
            showIllnessVisual || behaviorState == PetBehaviorState.TRISTE -> {
                // Sad Trembling Downward Mouth
                val mouth = Path().apply {
                    moveTo(centerX - 8.dp.toPx(), centerY + 8.dp.toPx())
                    quadraticTo(centerX, centerY + 2.dp.toPx(), centerX + 8.dp.toPx(), centerY + 8.dp.toPx())
                }
                drawPath(mouth, outlineColor, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            }
            isHungry -> {
                // Hungry mouth
                val mouth = Path().apply {
                    moveTo(centerX - 7.dp.toPx(), centerY + 7.dp.toPx())
                    quadraticTo(centerX, centerY + 2.dp.toPx(), centerX + 7.dp.toPx(), centerY + 7.dp.toPx())
                }
                drawPath(mouth, outlineColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(Color(0xFF60A5FA), radius = 2.5.dp.toPx(), center = Offset(centerX + 6.dp.toPx(), centerY + 11.dp.toPx()))
            }
            else -> {
                // Cute Anime 'w' Smile
                val mouth = Path().apply {
                    moveTo(centerX - 6.dp.toPx(), centerY + 4.dp.toPx())
                    quadraticTo(centerX - 3.dp.toPx(), centerY + 8.dp.toPx(), centerX, centerY + 4.dp.toPx())
                    quadraticTo(centerX + 3.dp.toPx(), centerY + 8.dp.toPx(), centerX + 6.dp.toPx(), centerY + 4.dp.toPx())
                }
                drawPath(mouth, outlineColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))

                // Bunny / Hamster Buck Teeth
                if (species == Species.COELHO || species == Species.HAMSTER) {
                    drawRect(Color.White, topLeft = Offset(centerX - 3.5.dp.toPx(), centerY + 4.dp.toPx()), size = Size(3.dp.toPx(), 4.dp.toPx()))
                    drawRect(Color.White, topLeft = Offset(centerX + 0.5.dp.toPx(), centerY + 4.dp.toPx()), size = Size(3.dp.toPx(), 4.dp.toPx()))
                }
            }
        }
    }
}

private fun DrawScope.drawLivingEye(
    x: Float,
    y: Float,
    gazeX: Float,
    gazeY: Float,
    heightScale: Float,
    isHappy: Boolean
) {
    val eyeW = 13.dp.toPx()
    val eyeH = 13.dp.toPx() * heightScale

    // Dark eyeball
    drawOval(Color(0xFF0F172A), topLeft = Offset(x - eyeW / 2, y - eyeH / 2), size = Size(eyeW, eyeH))

    // Pupil highlight shifted by gaze direction
    val hlX = x + gazeX - 2.dp.toPx()
    val hlY = y + gazeY - (2.dp.toPx() * heightScale)
    drawCircle(Color.White, radius = (2.6.dp.toPx() * heightScale).coerceAtLeast(1.dp.toPx()), center = Offset(hlX, hlY))
    drawCircle(Color.White, radius = (1.2.dp.toPx() * heightScale).coerceAtLeast(0.5.dp.toPx()), center = Offset(x + gazeX + 2.dp.toPx(), y + gazeY + 2.dp.toPx() * heightScale))
}

private fun DrawScope.drawHeartEye(x: Float, y: Float, color: Color) {
    val size = 12.dp.toPx()
    val path = Path().apply {
        moveTo(x, y + size * 0.35f)
        cubicTo(x - size * 0.55f, y - size * 0.25f, x - size * 0.55f, y - size * 0.6f, x, y - size * 0.25f)
        cubicTo(x + size * 0.55f, y - size * 0.6f, x + size * 0.55f, y - size * 0.25f, x, y + size * 0.35f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawSleepyEye(x: Float, y: Float, outlineColor: Color) {
    val eye = Path().apply {
        moveTo(x - 6.dp.toPx(), y)
        quadraticTo(x, y + 3.dp.toPx(), x + 6.dp.toPx(), y)
    }
    drawPath(eye, outlineColor, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawXEye(x: Float, y: Float, color: Color) {
    val r = 4.dp.toPx()
    drawLine(color, start = Offset(x - r, y - r), end = Offset(x + r, y + r), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color, start = Offset(x + r, y - r), end = Offset(x - r, y + r), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawEquippedItems(
    hatId: String,
    accId: String,
    centerX: Float,
    centerY: Float,
    species: Species,
    stage: PetStage
) {
    // Dynamic vertical anchor based on species head crown
    val headCrownOffsetY = when (species) {
        Species.COELHO -> -28.dp.toPx()
        Species.DRAGAO -> -30.dp.toPx()
        Species.FENIX -> -26.dp.toPx()
        Species.LEAO -> -32.dp.toPx()
        Species.CORUJA -> -30.dp.toPx()
        Species.TARTARUGA -> -26.dp.toPx()
        else -> -32.dp.toPx()
    }
    val headY = centerY + headCrownOffsetY

    when (hatId) {
        "cloth_crown" -> {
            val crownPath = Path().apply {
                moveTo(centerX - 20.dp.toPx(), headY)
                lineTo(centerX - 24.dp.toPx(), headY - 18.dp.toPx())
                lineTo(centerX - 10.dp.toPx(), headY - 8.dp.toPx())
                lineTo(centerX, headY - 22.dp.toPx())
                lineTo(centerX + 10.dp.toPx(), headY - 8.dp.toPx())
                lineTo(centerX + 24.dp.toPx(), headY - 18.dp.toPx())
                lineTo(centerX + 20.dp.toPx(), headY)
                close()
            }
            drawPath(crownPath, Color(0xFFFACC15))
            drawPath(crownPath, Color(0xFF78350F), style = Stroke(2.dp.toPx()))

            // Base crown band
            drawRoundRect(
                Color(0xFFEAB308),
                topLeft = Offset(centerX - 20.dp.toPx(), headY - 4.dp.toPx()),
                size = Size(40.dp.toPx(), 5.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx())
            )

            // Jewels
            drawCircle(Color(0xFFEF4444), radius = 2.5.dp.toPx(), center = Offset(centerX, headY - 16.dp.toPx()))
            drawCircle(Color(0xFF3B82F6), radius = 2.dp.toPx(), center = Offset(centerX - 17.dp.toPx(), headY - 12.dp.toPx()))
            drawCircle(Color(0xFF10B981), radius = 2.dp.toPx(), center = Offset(centerX + 17.dp.toPx(), headY - 12.dp.toPx()))
        }
        "cloth_hat_magic" -> {
            val hatPath = Path().apply {
                moveTo(centerX - 22.dp.toPx(), headY)
                lineTo(centerX + 12.dp.toPx(), headY - 32.dp.toPx())
                lineTo(centerX + 22.dp.toPx(), headY)
                close()
            }
            // Hat Brim
            drawOval(Color(0xFF581C87), topLeft = Offset(centerX - 26.dp.toPx(), headY - 4.dp.toPx()), size = Size(52.dp.toPx(), 9.dp.toPx()))
            drawOval(Color(0xFF1E1B4B), topLeft = Offset(centerX - 26.dp.toPx(), headY - 4.dp.toPx()), size = Size(52.dp.toPx(), 9.dp.toPx()), style = Stroke(1.5.dp.toPx()))
            // Hat Cone
            drawPath(hatPath, Color(0xFF6B21A8))
            drawPath(hatPath, Color(0xFF1E1B4B), style = Stroke(2.dp.toPx()))
            // Gold Band
            drawRoundRect(
                Color(0xFFFACC15),
                topLeft = Offset(centerX - 18.dp.toPx(), headY - 4.dp.toPx()),
                size = Size(36.dp.toPx(), 4.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx())
            )
            // Star on top
            drawCircle(Color(0xFFFDE047), radius = 3.5.dp.toPx(), center = Offset(centerX + 12.dp.toPx(), headY - 32.dp.toPx()))
        }
        "cloth_cap" -> {
            // Sporty Cap (Boné)
            val capColor = Color(0xFF2563EB)
            val capDark = Color(0xFF1D4ED8)
            val capOutline = Color(0xFF1E293B)

            // Cap Dome
            drawArc(
                color = capColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(centerX - 22.dp.toPx(), headY - 14.dp.toPx()),
                size = Size(44.dp.toPx(), 28.dp.toPx())
            )
            drawArc(
                color = capOutline,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(centerX - 22.dp.toPx(), headY - 14.dp.toPx()),
                size = Size(44.dp.toPx(), 28.dp.toPx()),
                style = Stroke(2.dp.toPx())
            )

            // Front Visor / Brim
            val visorPath = Path().apply {
                moveTo(centerX - 8.dp.toPx(), headY)
                lineTo(centerX + 26.dp.toPx(), headY - 4.dp.toPx())
                lineTo(centerX + 30.dp.toPx(), headY + 3.dp.toPx())
                lineTo(centerX - 4.dp.toPx(), headY + 5.dp.toPx())
                close()
            }
            drawPath(visorPath, capDark)
            drawPath(visorPath, capOutline, style = Stroke(1.5.dp.toPx()))

            // Cap Top Button
            drawCircle(Color(0xFFFACC15), radius = 2.5.dp.toPx(), center = Offset(centerX, headY - 14.dp.toPx()))

            // Front Star / Emblem
            drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(centerX - 4.dp.toPx(), headY - 4.dp.toPx()))
            drawCircle(capDark, radius = 2.dp.toPx(), center = Offset(centerX - 4.dp.toPx(), headY - 4.dp.toPx()))
        }
        "cloth_bow" -> {
            val bowY = headY - 2.dp.toPx()
            val bowPath = Path().apply {
                moveTo(centerX, bowY)
                lineTo(centerX - 16.dp.toPx(), bowY - 10.dp.toPx())
                lineTo(centerX - 16.dp.toPx(), bowY + 10.dp.toPx())
                lineTo(centerX, bowY)
                lineTo(centerX + 16.dp.toPx(), bowY - 10.dp.toPx())
                lineTo(centerX + 16.dp.toPx(), bowY + 10.dp.toPx())
                close()
            }
            drawPath(bowPath, Color(0xFFEC4899))
            drawPath(bowPath, Color(0xFF9D174D), style = Stroke(1.5.dp.toPx()))
            drawCircle(Color(0xFFBE185D), radius = 4.dp.toPx(), center = Offset(centerX, bowY))
            drawCircle(Color.White, radius = 1.5.dp.toPx(), center = Offset(centerX - 1.dp.toPx(), bowY - 1.dp.toPx()))
        }
        "cloth_cape" -> {
            val cape = Path().apply {
                moveTo(centerX - 24.dp.toPx(), centerY + 4.dp.toPx())
                lineTo(centerX - 36.dp.toPx(), centerY + 36.dp.toPx())
                lineTo(centerX + 36.dp.toPx(), centerY + 36.dp.toPx())
                lineTo(centerX + 24.dp.toPx(), centerY + 4.dp.toPx())
                close()
            }
            drawPath(cape, Color(0xFFDC2626))
            drawPath(cape, Color(0xFF7F1D1D), style = Stroke(2.dp.toPx()))

            // Cape Golden Collar Clasp
            drawCircle(Color(0xFFFACC15), radius = 4.dp.toPx(), center = Offset(centerX - 16.dp.toPx(), centerY + 6.dp.toPx()))
            drawCircle(Color(0xFFFACC15), radius = 4.dp.toPx(), center = Offset(centerX + 16.dp.toPx(), centerY + 6.dp.toPx()))
            drawLine(Color(0xFFFACC15), start = Offset(centerX - 16.dp.toPx(), centerY + 6.dp.toPx()), end = Offset(centerX + 16.dp.toPx(), centerY + 6.dp.toPx()), strokeWidth = 2.dp.toPx())
        }
    }

    when (accId) {
        "acc_glasses" -> {
            val glassesY = centerY - 14.dp.toPx()
            // Left Lens
            drawRoundRect(Color(0xFF18181B), topLeft = Offset(centerX - 26.dp.toPx(), glassesY), size = Size(22.dp.toPx(), 13.dp.toPx()), cornerRadius = CornerRadius(4.dp.toPx()))
            drawRoundRect(Color(0xFF52525B), topLeft = Offset(centerX - 26.dp.toPx(), glassesY), size = Size(22.dp.toPx(), 13.dp.toPx()), cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(1.5.dp.toPx()))
            // Right Lens
            drawRoundRect(Color(0xFF18181B), topLeft = Offset(centerX + 4.dp.toPx(), glassesY), size = Size(22.dp.toPx(), 13.dp.toPx()), cornerRadius = CornerRadius(4.dp.toPx()))
            drawRoundRect(Color(0xFF52525B), topLeft = Offset(centerX + 4.dp.toPx(), glassesY), size = Size(22.dp.toPx(), 13.dp.toPx()), cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(1.5.dp.toPx()))
            // Bridge
            drawLine(Color(0xFF18181B), start = Offset(centerX - 4.dp.toPx(), glassesY + 5.dp.toPx()), end = Offset(centerX + 4.dp.toPx(), glassesY + 5.dp.toPx()), strokeWidth = 2.5.dp.toPx())
            // Glare reflections
            drawLine(Color(0x88FFFFFF), start = Offset(centerX - 22.dp.toPx(), glassesY + 3.dp.toPx()), end = Offset(centerX - 14.dp.toPx(), glassesY + 10.dp.toPx()), strokeWidth = 1.5.dp.toPx())
            drawLine(Color(0x88FFFFFF), start = Offset(centerX + 8.dp.toPx(), glassesY + 3.dp.toPx()), end = Offset(centerX + 16.dp.toPx(), glassesY + 10.dp.toPx()), strokeWidth = 1.5.dp.toPx())
        }
        "acc_bell" -> {
            val neckY = centerY + 18.dp.toPx()
            // Red Collar
            drawArc(
                color = Color(0xFFEF4444),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(centerX - 20.dp.toPx(), neckY - 8.dp.toPx()),
                size = Size(40.dp.toPx(), 16.dp.toPx()),
                style = Stroke(3.dp.toPx())
            )
            // Golden Bell
            drawCircle(Color(0xFFF59E0B), radius = 6.dp.toPx(), center = Offset(centerX, neckY + 4.dp.toPx()))
            drawCircle(Color(0xFF78350F), radius = 6.dp.toPx(), center = Offset(centerX, neckY + 4.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawCircle(Color(0xFF78350F), radius = 1.5.dp.toPx(), center = Offset(centerX, neckY + 5.dp.toPx()))
        }
        "acc_bowtie" -> {
            val neckY = centerY + 20.dp.toPx()
            val bowPath = Path().apply {
                moveTo(centerX, neckY)
                lineTo(centerX - 13.dp.toPx(), neckY - 6.dp.toPx())
                lineTo(centerX - 13.dp.toPx(), neckY + 6.dp.toPx())
                lineTo(centerX, neckY)
                lineTo(centerX + 13.dp.toPx(), neckY - 6.dp.toPx())
                lineTo(centerX + 13.dp.toPx(), neckY + 6.dp.toPx())
                close()
            }
            drawPath(bowPath, Color(0xFF3B82F6))
            drawPath(bowPath, Color(0xFF1D4ED8), style = Stroke(1.dp.toPx()))
            drawCircle(Color(0xFF1D4ED8), radius = 3.dp.toPx(), center = Offset(centerX, neckY))
        }
        "acc_chain" -> {
            val neckY = centerY + 14.dp.toPx()
            drawArc(
                color = Color(0xFFFACC15),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(centerX - 20.dp.toPx(), neckY),
                size = Size(40.dp.toPx(), 16.dp.toPx()),
                style = Stroke(3.dp.toPx())
            )
            // Star Medallion
            drawCircle(Color(0xFFEAB308), radius = 5.dp.toPx(), center = Offset(centerX, neckY + 16.dp.toPx()))
            drawCircle(Color(0xFFFEF08A), radius = 2.dp.toPx(), center = Offset(centerX, neckY + 16.dp.toPx()))
        }
    }
}

private fun DrawScope.drawSoapBubbles(centerX: Float, centerY: Float, floatAnim: Float) {
    for (i in 0..6) {
        val bubbleX = centerX + ((i - 3f) * 20.dp.toPx())
        val bubbleY = centerY + (20.dp.toPx() - (floatAnim * 60.dp.toPx()) + (i * 7.dp.toPx()))
        val radius = 7.dp.toPx() + (i % 3) * 2.dp.toPx()

        drawCircle(
            color = Color(0x6693C5FD),
            radius = radius,
            center = Offset(bubbleX, bubbleY)
        )
        drawCircle(
            color = Color(0xAAFFFFFF),
            radius = radius,
            center = Offset(bubbleX, bubbleY),
            style = Stroke(1.5.dp.toPx())
        )
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = Offset(bubbleX - radius * 0.4f, bubbleY - radius * 0.4f)
        )
    }
}

private fun DrawScope.drawZzz(x: Float, y: Float, floatAnim: Float) {
    val offsetY = y - (floatAnim * 40.dp.toPx())
    val alpha = (1f - floatAnim).coerceIn(0f, 1f)

    val zPath = Path().apply {
        moveTo(x - 5.dp.toPx(), offsetY - 5.dp.toPx())
        lineTo(x + 5.dp.toPx(), offsetY - 5.dp.toPx())
        lineTo(x - 5.dp.toPx(), offsetY + 5.dp.toPx())
        lineTo(x + 5.dp.toPx(), offsetY + 5.dp.toPx())
    }
    drawPath(zPath, Color(0xFF6366F1).copy(alpha = alpha), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))

    val zPath2 = Path().apply {
        moveTo(x + 10.dp.toPx(), offsetY + 15.dp.toPx() - 5.dp.toPx())
        lineTo(x + 18.dp.toPx(), offsetY + 15.dp.toPx() - 5.dp.toPx())
        lineTo(x + 10.dp.toPx(), offsetY + 15.dp.toPx() + 3.dp.toPx())
        lineTo(x + 18.dp.toPx(), offsetY + 15.dp.toPx() + 3.dp.toPx())
    }
    drawPath(zPath2, Color(0xFF818CF8).copy(alpha = alpha * 0.8f), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawFloatingHearts(centerX: Float, centerY: Float, floatAnim: Float) {
    val heartY = centerY - (floatAnim * 36.dp.toPx())
    val alpha = (1f - floatAnim).coerceIn(0f, 1f)
    val heartColor = Color(0xFFEC4899).copy(alpha = alpha)

    drawCircle(heartColor, radius = 5.dp.toPx(), center = Offset(centerX - 5.dp.toPx(), heartY))
    drawCircle(heartColor, radius = 5.dp.toPx(), center = Offset(centerX + 5.dp.toPx(), heartY))
    val heartBottom = Path().apply {
        moveTo(centerX - 10.dp.toPx(), heartY)
        lineTo(centerX, heartY + 10.dp.toPx())
        lineTo(centerX + 10.dp.toPx(), heartY)
        close()
    }
    drawPath(heartBottom, heartColor)
}

private fun DrawScope.drawMusicNotes(x: Float, y: Float, floatAnim: Float) {
    val offsetY = y - (floatAnim * 30.dp.toPx())
    val alpha = (1f - floatAnim).coerceIn(0f, 1f)
    val noteColor = Color(0xFFF59E0B).copy(alpha = alpha)

    drawCircle(noteColor, radius = 4.dp.toPx(), center = Offset(x, offsetY + 6.dp.toPx()))
    drawLine(noteColor, start = Offset(x + 3.dp.toPx(), offsetY + 6.dp.toPx()), end = Offset(x + 3.dp.toPx(), offsetY - 6.dp.toPx()), strokeWidth = 2.dp.toPx())
    drawLine(noteColor, start = Offset(x + 3.dp.toPx(), offsetY - 6.dp.toPx()), end = Offset(x + 9.dp.toPx(), offsetY - 3.dp.toPx()), strokeWidth = 2.dp.toPx())
}

private fun DrawScope.drawFoodCrumbs(centerX: Float, centerY: Float, phase: Float) {
    val alpha = (sin(phase * 3.14159f)).coerceIn(0.2f, 1f)
    drawCircle(Color(0xFFD97706).copy(alpha = alpha), radius = 2.dp.toPx(), center = Offset(centerX - 10.dp.toPx(), centerY + 4.dp.toPx()))
    drawCircle(Color(0xFFF59E0B).copy(alpha = alpha), radius = 2.5.dp.toPx(), center = Offset(centerX + 12.dp.toPx(), centerY + 6.dp.toPx()))
    drawCircle(Color(0xFFEF4444).copy(alpha = alpha), radius = 1.8.dp.toPx(), center = Offset(centerX - 4.dp.toPx(), centerY + 9.dp.toPx()))
}

private fun DrawScope.drawSweatDrop(x: Float, y: Float, floatAnim: Float) {
    val alpha = (1f - floatAnim * 0.5f).coerceIn(0.3f, 1f)
    val dropColor = Color(0xFF38BDF8).copy(alpha = alpha)

    val path = Path().apply {
        moveTo(x, y - 5.dp.toPx())
        quadraticTo(x + 4.dp.toPx(), y, x, y + 4.dp.toPx())
        quadraticTo(x - 4.dp.toPx(), y, x, y - 5.dp.toPx())
    }
    drawPath(path, dropColor)
}

private fun DrawScope.drawTearDrop(x: Float, y: Float, floatAnim: Float) {
    val tearY = y + (floatAnim * 12.dp.toPx())
    val alpha = (1f - floatAnim * 0.4f).coerceIn(0.4f, 1f)
    val tearColor = Color(0xFF60A5FA).copy(alpha = alpha)

    drawCircle(tearColor, radius = 2.5.dp.toPx(), center = Offset(x, tearY))
}
