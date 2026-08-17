package com.example.notification

import com.example.data.local.PetEntity
import com.example.data.model.PetDisease
import com.example.data.model.PetHealthRules
import com.example.data.model.PetHealthState
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

data class SimulatedPetState(
    val hunger: Int,
    val energy: Int,
    val happiness: Int,
    val hygiene: Int,
    val health: Int,
    val disease: String = PetDisease.NONE.name,
    val lowHygieneExposure: Int = 0,
    val exhaustionCount: Int = 0,
    val indigestionStreak: Int = 0,
    val isSleeping: Boolean,
    val wasLonging: Boolean,
    val elapsedMinutes: Int
)

object PetStatsCalculator {

    const val HUNGER_THRESHOLD = PetHealthRules.HUNGER_DAMAGE_THRESHOLD
    const val HYGIENE_THRESHOLD = PetHealthRules.HYGIENE_DAMAGE_THRESHOLD
    const val ENERGY_THRESHOLD = 15
    const val HEALTH_THRESHOLD = PetHealthRules.HEALTH_DOENTE_MIN
    const val LONGING_THRESHOLD_MINUTES = 45

    /**
     * Determines whether a given timestamp is within the protected nighttime window (22:00 to 08:00).
     */
    fun isNightTime(timestamp: Long = System.currentTimeMillis()): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour >= 22 || hour < 8
    }

    /**
     * Returns the timestamp when the next nighttime period starts (at 22:00).
     */
    fun getNextNightStartTimestamp(fromTimestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = fromTimestamp
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        if (currentHour >= 22) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 22)
        } else {
            cal.set(Calendar.HOUR_OF_DAY, 22)
        }
        return cal.timeInMillis
    }

    /**
     * Returns the timestamp when the next daytime period starts (at 08:00).
     */
    fun getNextDayStartTimestamp(fromTimestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = fromTimestamp
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        if (currentHour >= 8) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 8)
        } else {
            cal.set(Calendar.HOUR_OF_DAY, 8)
        }
        return cal.timeInMillis
    }

    /**
     * Single source of truth for simulating offline pet stats over time.
     * Accurately splits time intervals between:
     * 1. Daytime (08:00 - 22:00): Active decay, disease risk exposure, and daytime exhaustion sleep.
     * 2. Nighttime (22:00 - 08:00): Protected night sleep (stays sleeping even at 100%, no health/hygiene/happiness penalties, no disease contraction).
     */
    fun calculateSimulatedStats(pet: PetEntity, targetTimestamp: Long = System.currentTimeMillis()): SimulatedPetState {
        if (!pet.isHatched) {
            return SimulatedPetState(
                hunger = pet.hunger,
                energy = pet.energy,
                happiness = pet.happiness,
                hygiene = pet.hygiene,
                health = pet.health,
                disease = pet.disease,
                lowHygieneExposure = pet.lowHygieneExposure,
                exhaustionCount = pet.exhaustionCount,
                indigestionStreak = pet.indigestionStreak,
                isSleeping = pet.isSleeping,
                wasLonging = false,
                elapsedMinutes = 0
            )
        }

        val elapsedMs = targetTimestamp - pet.lastUpdateTimestamp
        val elapsedMinutes = max(0, (elapsedMs / (1000 * 60)).toInt())

        if (elapsedMinutes <= 0) {
            val currentlyNight = isNightTime(targetTimestamp)
            return SimulatedPetState(
                hunger = pet.hunger,
                energy = pet.energy,
                happiness = pet.happiness,
                hygiene = pet.hygiene,
                health = pet.health,
                disease = pet.disease,
                lowHygieneExposure = pet.lowHygieneExposure,
                exhaustionCount = pet.exhaustionCount,
                indigestionStreak = pet.indigestionStreak,
                isSleeping = if (currentlyNight) true else pet.isSleeping,
                wasLonging = false,
                elapsedMinutes = 0
            )
        }

        // Limit simulation to max 10080 minutes (7 days) for efficiency
        val simMinutes = min(10080, elapsedMinutes)

        var hunger = pet.hunger.toDouble()
        var energy = pet.energy.toDouble()
        var happiness = pet.happiness.toDouble()
        var hygiene = pet.hygiene.toDouble()
        var health = pet.health.toDouble()
        var disease = pet.disease
        var lowHygieneExposure = pet.lowHygieneExposure
        var exhaustionCount = pet.exhaustionCount
        var indigestionStreak = pet.indigestionStreak
        var isSleeping = pet.isSleeping
        var consecutiveDaytimeAwakeMinutes = 0
        var wasLonging = false

        var currentTs = pet.lastUpdateTimestamp
        var wasNightInPreviousMinute = isNightTime(pet.lastUpdateTimestamp)

        for (m in 1..simMinutes) {
            currentTs += 60_000L
            val isNight = isNightTime(currentTs)

            if (isNight) {
                // --- NIGHTTIME (22:00 to 08:00): Protected Rest Period ---
                isSleeping = true // Forced night rest
                consecutiveDaytimeAwakeMinutes = 0 // Reset daytime longing

                // Energy recovers (+1 every 2 min) up to 100
                if (m % 2 == 0) {
                    energy = min(100.0, energy + 1.0)
                }

                // Hunger decays at reduced sleeping rate (-1 every 10 min)
                if (m % 10 == 0) {
                    hunger = max(0.0, hunger - 1.0)
                }

                // Protected stats: hygiene and happiness do not decrease during night
                // Health & Disease: Protected! Zero damage and zero new diseases during night!
                wasNightInPreviousMinute = true
            } else {
                // --- DAYTIME (08:00 to 22:00): Active / Nap Period ---
                // If transitioning from Night to Day at 08:00:
                if (wasNightInPreviousMinute) {
                    // Night rest ends automatically at 08:00
                    isSleeping = false
                    wasNightInPreviousMinute = false
                    continue
                }

                if (isSleeping) {
                    // Daytime Nap / Exhaustion sleep
                    consecutiveDaytimeAwakeMinutes = 0

                    // Energy recovers (+1 every 2 min)
                    if (m % 2 == 0) {
                        energy = min(100.0, energy + 1.0)
                    }

                    // Hunger decays slower (-1 every 10 min)
                    if (m % 10 == 0) {
                        hunger = max(0.0, hunger - 1.0)
                    }

                    // Daytime auto-awakening rule: when energy reaches 100%, wake up!
                    if (energy >= 100.0) {
                        isSleeping = false
                    }
                } else {
                    // Daytime Awake Mode: standard daytime decay
                    consecutiveDaytimeAwakeMinutes++
                    if (consecutiveDaytimeAwakeMinutes >= LONGING_THRESHOLD_MINUTES) {
                        wasLonging = true
                    }

                    if (m % 5 == 0) hunger = max(0.0, hunger - 1.0) // -1 every 5 mins
                    if (m % 6 == 0) energy = max(0.0, energy - 1.0) // -1 every 6 mins
                    if (m % 8 == 0) hygiene = max(0.0, hygiene - 1.0) // -1 every 8 mins
                    if (m % 7 == 0) happiness = max(0.0, happiness - 1.0) // -1 every 7 mins

                    // Daytime exhaustion sleep: if energy drops to <= 5, pet falls asleep automatically!
                    if (energy <= 5.0) {
                        isSleeping = true
                        exhaustionCount++
                        if (exhaustionCount >= PetHealthRules.EXHAUSTION_COUNT_LIMIT && disease == PetDisease.NONE.name) {
                            disease = PetDisease.FADIGA.name
                        }
                    }

                    // Low hygiene exposure tracking
                    if (hygiene <= PetHealthRules.HYGIENE_DAMAGE_THRESHOLD.toDouble()) {
                        if (m % 5 == 0) {
                            lowHygieneExposure++
                            if (lowHygieneExposure >= PetHealthRules.LOW_HYGIENE_EXPOSURE_LIMIT && disease == PetDisease.NONE.name) {
                                disease = PetDisease.RESFRIADO.name
                            }
                        }
                    } else if (hygiene >= 60.0) {
                        lowHygieneExposure = 0
                    }

                    // Daytime health damage if starving (hunger <= 20) or dirty (hygiene <= 20)
                    if (hunger <= PetHealthRules.HUNGER_DAMAGE_THRESHOLD.toDouble() || hygiene <= PetHealthRules.HYGIENE_DAMAGE_THRESHOLD.toDouble()) {
                        if (m % 10 == 0) {
                            health = max(PetHealthRules.MIN_HEALTH.toDouble(), health - 1.0) // -1 every 10 mins
                        }
                    }
                }
                wasNightInPreviousMinute = false
            }
        }

        return SimulatedPetState(
            hunger = hunger.toInt().coerceIn(0, 100),
            energy = energy.toInt().coerceIn(0, 100),
            happiness = happiness.toInt().coerceIn(0, 100),
            hygiene = hygiene.toInt().coerceIn(0, 100),
            health = health.toInt().coerceIn(PetHealthRules.MIN_HEALTH, PetHealthRules.MAX_HEALTH),
            disease = disease,
            lowHygieneExposure = lowHygieneExposure,
            exhaustionCount = exhaustionCount,
            indigestionStreak = indigestionStreak,
            isSleeping = isSleeping,
            wasLonging = wasLonging,
            elapsedMinutes = elapsedMinutes
        )
    }

    /**
     * Calculates the estimated delay in minutes until the next critical notification threshold is reached.
     * Between 22:00 and 08:00, all care notifications are paused and the check is delayed until 08:00.
     */
    fun estimateMinutesUntilNextThreshold(pet: PetEntity, fromTimestamp: Long = System.currentTimeMillis()): Long {
        if (!pet.isHatched) return 60L

        val isCurrentlyNight = isNightTime(fromTimestamp)
        if (isCurrentlyNight) {
            // Between 22:00 and 08:00, no care notifications!
            val msUntil0800 = getNextDayStartTimestamp(fromTimestamp) - fromTimestamp
            val minutesUntil0800 = max(15L, (msUntil0800 / (1000 * 60)))
            return minutesUntil0800
        }

        // In daytime: find minutes until 22:00
        val msUntil2200 = getNextNightStartTimestamp(fromTimestamp) - fromTimestamp
        val minutesUntil2200 = max(15L, (msUntil2200 / (1000 * 60)))

        val simulated = calculateSimulatedStats(pet, fromTimestamp)
        val candidateDelays = mutableListOf<Long>()

        // 0. Daytime sleep awakening threshold (if sleeping, schedule check when energy reaches 100%)
        if (simulated.isSleeping && simulated.energy < 100) {
            val minutesUntilFull = (100 - simulated.energy) * 2L
            if (minutesUntilFull in 1 until minutesUntil2200) {
                candidateDelays.add(minutesUntilFull)
            }
        }

        // 1. Hunger threshold (<= 20)
        if (simulated.hunger > HUNGER_THRESHOLD) {
            val pointsToLose = simulated.hunger - HUNGER_THRESHOLD
            val minutesNeeded = if (simulated.isSleeping) pointsToLose * 10L else pointsToLose * 5L
            if (minutesNeeded < minutesUntil2200) {
                candidateDelays.add(minutesNeeded)
            }
        }

        // 2. Hygiene threshold (<= 20)
        if (!simulated.isSleeping && simulated.hygiene > HYGIENE_THRESHOLD) {
            val pointsToLose = simulated.hygiene - HYGIENE_THRESHOLD
            val minutesNeeded = pointsToLose * 8L
            if (minutesNeeded < minutesUntil2200) {
                candidateDelays.add(minutesNeeded)
            }
        }

        // 3. Energy threshold (<= 15)
        if (!simulated.isSleeping && simulated.energy > ENERGY_THRESHOLD) {
            val pointsToLose = simulated.energy - ENERGY_THRESHOLD
            val minutesNeeded = pointsToLose * 6L
            if (minutesNeeded < minutesUntil2200) {
                candidateDelays.add(minutesNeeded)
            }
        }

        // 4. Longing threshold (45 minutes from last interaction)
        val elapsedMinutes = ((fromTimestamp - pet.lastUpdateTimestamp) / (1000 * 60))
        if (elapsedMinutes < LONGING_THRESHOLD_MINUTES) {
            val minutesUntilLonging = LONGING_THRESHOLD_MINUTES - elapsedMinutes
            if (minutesUntilLonging < minutesUntil2200) {
                candidateDelays.add(minutesUntilLonging)
            }
        }

        // 5. Night start threshold at 22:00
        candidateDelays.add(minutesUntil2200)

        // Return the earliest critical threshold clamped to a safe minimum of 15 minutes
        val shortestDelay = candidateDelays.minOrNull() ?: minutesUntil2200
        return max(15L, shortestDelay)
    }
}
