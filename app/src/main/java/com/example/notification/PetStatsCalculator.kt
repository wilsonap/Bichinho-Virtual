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
    val elapsedMinutes: Int,
    /** Estado escolar após a simulação offline. */
    val isAtSchool: Boolean = false,
    /**
     * Se a sessão escolar terminou nesta simulação, contém o schoolEndTimestamp
     * para conceder recompensa uma única vez.
     */
    val completedSchoolEndTimestamp: Long = 0L
)

/** Resultado do agendamento inteligente (delay + motivo para logs). */
data class ScheduleEstimate(
    val delayMinutes: Long,
    val reason: String
)

object PetStatsCalculator {

    const val HUNGER_THRESHOLD = PetHealthRules.HUNGER_DAMAGE_THRESHOLD
    const val HYGIENE_THRESHOLD = PetHealthRules.HYGIENE_DAMAGE_THRESHOLD
    const val ENERGY_THRESHOLD = 15
    const val HEALTH_THRESHOLD = PetHealthRules.HEALTH_DOENTE_MIN
    const val LONGING_THRESHOLD_MINUTES = 45

    /** Clamp padrão para cuidados comuns (fome/higiene/energia/saudade). */
    const val MIN_CARE_DELAY_MINUTES = 15L

    /**
     * Atraso mínimo para saúde urgente (Doente/Crítico/doença ativa) ainda pendente de alerta.
     * Compatível com WorkManager — sem polling contínuo após o alerta único.
     */
    const val MIN_CRITICAL_HEALTH_DELAY_MINUTES = 5L

    // Health damage while awake daytime: -1 every 10 minutes when hunger or hygiene <= 20
    private const val HEALTH_DAMAGE_INTERVAL_MIN = 10L
    private const val HUNGER_DECAY_INTERVAL_AWAKE = 5L
    private const val HYGIENE_DECAY_INTERVAL_AWAKE = 8L

    /**
     * Janela noturna protegida: 22:00 → 07:30.
     * Às 07:30 o sono noturno termina (mesmo com energia < 100%).
     */
    fun isNightTime(timestamp: Long = System.currentTimeMillis()): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        return hour >= 22 || hour < 7 || (hour == 7 && minute < 30)
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
     * Início do dia (fim do sono noturno): 07:30.
     */
    fun getNextDayStartTimestamp(fromTimestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = fromTimestamp
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        if (hour > 7 || (hour == 7 && minute >= 30)) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        cal.set(Calendar.HOUR_OF_DAY, 7)
        cal.set(Calendar.MINUTE, 30)
        return cal.timeInMillis
    }

    /**
     * Single source of truth for simulating offline pet stats over time.
     * Accurately splits time intervals between:
     * 1. Daytime (07:30 - 22:00): Active decay, disease risk exposure, and daytime exhaustion sleep.
     * 2. Nighttime (22:00 - 07:30): Protected night sleep (stays sleeping even at 100%, no health/hygiene/happiness penalties, no disease contraction).
     * 3. School session: attributes frozen until schoolEndTimestamp.
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
                elapsedMinutes = 0,
                isAtSchool = pet.isAtSchool
            )
        }

        val elapsedMs = targetTimestamp - pet.lastUpdateTimestamp
        val elapsedMinutes = max(0, (elapsedMs / (1000 * 60)).toInt())

        if (elapsedMinutes <= 0) {
            val currentlyNight = isNightTime(targetTimestamp)
            val stillAtSchool = pet.isAtSchool &&
                pet.schoolEndTimestamp > 0L &&
                targetTimestamp < pet.schoolEndTimestamp
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
                isSleeping = if (stillAtSchool) false else if (currentlyNight) true else pet.isSleeping,
                wasLonging = false,
                elapsedMinutes = 0,
                isAtSchool = stillAtSchool
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
        var isAtSchool = pet.isAtSchool
        var completedSchoolEndTimestamp = 0L
        val schoolEnd = pet.schoolEndTimestamp

        var currentTs = pet.lastUpdateTimestamp
        var wasNightInPreviousMinute = isNightTime(pet.lastUpdateTimestamp)

        for (m in 1..simMinutes) {
            currentTs += 60_000L

            // --- SCHOOL FREEZE: sem decay / doença / notificações de need ---
            if (isAtSchool && schoolEnd > 0L) {
                if (currentTs < schoolEnd) {
                    isSleeping = false
                    consecutiveDaytimeAwakeMinutes = 0
                    wasNightInPreviousMinute = isNightTime(currentTs)
                    continue
                }
                // Primeiro minuto >= fim da aula
                isAtSchool = false
                isSleeping = false
                if (pet.lastSchoolRewardEndTimestamp != schoolEnd) {
                    completedSchoolEndTimestamp = schoolEnd
                }
                // segue para lógica diurna/noturna neste mesmo minuto
            }

            val isNight = isNightTime(currentTs)

            if (isNight) {
                // --- NIGHTTIME (22:00 to 07:30): Protected Rest Period ---
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
                // --- DAYTIME (07:30 to 22:00): Active / Nap Period ---
                // If transitioning from Night to Day at 07:30:
                if (wasNightInPreviousMinute) {
                    // Night rest ends automatically at 07:30
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

        // Se ainda estava na escola no target e o fim já passou sem processar no loop (edge)
        if (isAtSchool && schoolEnd > 0L && targetTimestamp >= schoolEnd) {
            isAtSchool = false
            if (pet.lastSchoolRewardEndTimestamp != schoolEnd && completedSchoolEndTimestamp == 0L) {
                completedSchoolEndTimestamp = schoolEnd
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
            elapsedMinutes = elapsedMinutes,
            isAtSchool = isAtSchool,
            completedSchoolEndTimestamp = completedSchoolEndTimestamp
        )
    }

    /**
     * Calculates the estimated delay in minutes until the next critical notification threshold.
     * Between 22:00 and 07:30, checks are delayed until 07:30 (no normal alerts at night).
     * During school, checks are delayed until schoolEndTimestamp.
     *
     * @param allowCriticalHealthShortcut quando true e o pet já está doente/crítico/com doença
     *        e o alerta de saúde ainda não foi enviado, usa delay curto (5 min) em vez de 15.
     */
    fun estimateMinutesUntilNextThreshold(
        pet: PetEntity,
        fromTimestamp: Long = System.currentTimeMillis(),
        allowCriticalHealthShortcut: Boolean = false
    ): ScheduleEstimate {
        if (!pet.isHatched) {
            return ScheduleEstimate(60L, "EGG_OR_UNHATCHED")
        }

        val isCurrentlyNight = isNightTime(fromTimestamp)
        if (isCurrentlyNight) {
            val msUntilDayStart = getNextDayStartTimestamp(fromTimestamp) - fromTimestamp
            val minutesUntilDayStart = max(MIN_CARE_DELAY_MINUTES, (msUntilDayStart / (1000 * 60)))
            return ScheduleEstimate(minutesUntilDayStart, "QUIET_UNTIL_0730")
        }

        // Escola: suspende alertas de cuidado até o fim do turno
        if (pet.isAtSchool && pet.schoolEndTimestamp > fromTimestamp) {
            val msUntilSchoolEnd = pet.schoolEndTimestamp - fromTimestamp
            val minutesUntilSchoolEnd = max(1L, msUntilSchoolEnd / (1000 * 60))
            return ScheduleEstimate(minutesUntilSchoolEnd, "SCHOOL_UNTIL_END")
        }

        val msUntil2200 = getNextNightStartTimestamp(fromTimestamp) - fromTimestamp
        val minutesUntil2200 = max(MIN_CARE_DELAY_MINUTES, (msUntil2200 / (1000 * 60)))

        val simulated = calculateSimulatedStats(pet, fromTimestamp)
        val candidates = mutableListOf<Pair<Long, String>>()

        val disease = PetDisease.fromString(simulated.disease)
        val healthState = PetHealthState.fromHealth(simulated.health)
        val isHealthUrgent =
            healthState == PetHealthState.DOENTE ||
                healthState == PetHealthState.CRITICO ||
                disease != PetDisease.NONE

        if (allowCriticalHealthShortcut && isHealthUrgent) {
            candidates.add(MIN_CRITICAL_HEALTH_DELAY_MINUTES to "HEALTH_URGENT")
        }

        // 0. Daytime sleep awakening
        if (simulated.isSleeping && simulated.energy < 100) {
            val minutesUntilFull = (100 - simulated.energy) * 2L
            if (minutesUntilFull in 1 until minutesUntil2200) {
                candidates.add(minutesUntilFull to "SLEEP_WAKE")
            }
        }

        // 1. Hunger (<= 20)
        if (simulated.hunger > HUNGER_THRESHOLD) {
            val pointsToLose = simulated.hunger - HUNGER_THRESHOLD
            val minutesNeeded = if (simulated.isSleeping) pointsToLose * 10L else pointsToLose * HUNGER_DECAY_INTERVAL_AWAKE
            if (minutesNeeded < minutesUntil2200) {
                candidates.add(minutesNeeded to "HUNGER")
            }
        } else {
            candidates.add(0L to "HUNGER_NOW")
        }

        // 2. Hygiene (<= 20)
        if (!simulated.isSleeping) {
            if (simulated.hygiene > HYGIENE_THRESHOLD) {
                val pointsToLose = simulated.hygiene - HYGIENE_THRESHOLD
                val minutesNeeded = pointsToLose * HYGIENE_DECAY_INTERVAL_AWAKE
                if (minutesNeeded < minutesUntil2200) {
                    candidates.add(minutesNeeded to "HYGIENE")
                }
            } else {
                candidates.add(0L to "HYGIENE_NOW")
            }
        }

        // 3. Energy (<= 15)
        if (!simulated.isSleeping) {
            if (simulated.energy > ENERGY_THRESHOLD) {
                val pointsToLose = simulated.energy - ENERGY_THRESHOLD
                val minutesNeeded = pointsToLose * 6L
                if (minutesNeeded < minutesUntil2200) {
                    candidates.add(minutesNeeded to "ENERGY")
                }
            } else {
                candidates.add(0L to "ENERGY_NOW")
            }
        }

        // 4. Longing (45 min)
        val elapsedMinutes = ((fromTimestamp - pet.lastUpdateTimestamp) / (1000 * 60))
        if (elapsedMinutes < LONGING_THRESHOLD_MINUTES) {
            val minutesUntilLonging = LONGING_THRESHOLD_MINUTES - elapsedMinutes
            if (minutesUntilLonging < minutesUntil2200) {
                candidates.add(minutesUntilLonging to "LONGING")
            }
        } else if (simulated.wasLonging) {
            candidates.add(0L to "LONGING_NOW")
        }

        // 5. Health state transitions (INDISPOSTO / DOENTE / CRITICO)
        addHealthScheduleCandidates(
            simulated = simulated,
            minutesUntil2200 = minutesUntil2200,
            candidates = candidates
        )

        // 6. Night start
        candidates.add(minutesUntil2200 to "NIGHT_START")

        val best = candidates.minByOrNull { it.first } ?: (minutesUntil2200 to "NIGHT_START")
        val isCriticalReason = best.second.startsWith("HEALTH") &&
            (best.second == "HEALTH_URGENT" ||
                best.second == "HEALTH_DOENTE" ||
                best.second == "HEALTH_CRITICO" ||
                best.second == "HEALTH_DISEASE")

        val minClamp = if (isCriticalReason && allowCriticalHealthShortcut) {
            MIN_CRITICAL_HEALTH_DELAY_MINUTES
        } else if (best.second == "HEALTH_URGENT") {
            MIN_CRITICAL_HEALTH_DELAY_MINUTES
        } else {
            MIN_CARE_DELAY_MINUTES
        }

        // 0 = necessidade já atingida → verificar no mínimo clamp (não delay 0 infinito)
        val rawDelay = best.first.coerceAtLeast(0L)
        val delay = max(minClamp, if (rawDelay == 0L) minClamp else rawDelay)
        return ScheduleEstimate(delay, best.second)
    }

    /**
     * Estima minutos até a saúde cruzar limiares de comunicação, considerando
     * dano de -1/10 min quando fome ou higiene ≤ 20 (acordado de dia).
     */
    private fun addHealthScheduleCandidates(
        simulated: SimulatedPetState,
        minutesUntil2200: Long,
        candidates: MutableList<Pair<Long, String>>
    ) {
        val disease = PetDisease.fromString(simulated.disease)
        if (disease != PetDisease.NONE) {
            candidates.add(0L to "HEALTH_DISEASE")
        }

        val health = simulated.health
        val state = PetHealthState.fromHealth(health)
        when (state) {
            PetHealthState.DOENTE -> candidates.add(0L to "HEALTH_DOENTE")
            PetHealthState.CRITICO -> candidates.add(0L to "HEALTH_CRITICO")
            PetHealthState.INDISPOSTO -> candidates.add(0L to "HEALTH_INDISPOSTO")
            PetHealthState.SAUDAVEL -> { /* projeta abaixo */ }
        }

        if (simulated.isSleeping) return // dano à saúde só no ramo acordado diurno

        val minutesUntilDamage = minutesUntilHealthDamageStarts(simulated.hunger, simulated.hygiene)
        if (minutesUntilDamage >= minutesUntil2200) return

        fun addBoundary(targetHealthInclusive: Int, reason: String) {
            if (health <= targetHealthInclusive) return
            val pointsToLose = health - targetHealthInclusive
            val minutesDrop = pointsToLose * HEALTH_DAMAGE_INTERVAL_MIN
            val total = minutesUntilDamage + minutesDrop
            if (total in 1 until minutesUntil2200) {
                candidates.add(total to reason)
            }
        }

        // fromHealth: >=61 SAUDAVEL → INDISPOSTO em <=60; DOENTE <=40; CRITICO <=20
        addBoundary(60, "HEALTH_INDISPOSTO")
        addBoundary(PetHealthRules.HEALTH_DOENTE_MAX, "HEALTH_DOENTE")
        addBoundary(PetHealthRules.HEALTH_CRITICO_MAX, "HEALTH_CRITICO")
    }

    /** Minutos até fome ou higiene atingir ≤ 20 (acordado). 0 se já danificando. */
    fun minutesUntilHealthDamageStarts(hunger: Int, hygiene: Int): Long {
        if (hunger <= HUNGER_THRESHOLD || hygiene <= HYGIENE_THRESHOLD) return 0L
        val hungerMins = (hunger - HUNGER_THRESHOLD) * HUNGER_DECAY_INTERVAL_AWAKE
        val hygieneMins = (hygiene - HYGIENE_THRESHOLD) * HYGIENE_DECAY_INTERVAL_AWAKE
        return min(hungerMins, hygieneMins)
    }
}
