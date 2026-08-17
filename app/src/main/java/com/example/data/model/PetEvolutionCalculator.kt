package com.example.data.model

import com.example.data.local.PetEntity

data class EvolutionProgress(
    val currentStage: PetStage,
    val nextStage: PetStage?,
    val daysAlive: Int,
    val currentLevel: Int,
    val requiredDays: Int,
    val requiredLevel: Int,
    val isDaysRequirementMet: Boolean,
    val isLevelRequirementMet: Boolean,
    val isReadyToEvolve: Boolean
)

object PetEvolutionCalculator {

    /**
     * Calcula os dias inteiros de vida decorridos a partir do birthTimestamp.
     * Retorna >= 0. (Se now < birthTimestamp por inconsistência de relógio, retorna 0).
     */
    fun calculateDaysAlive(birthTimestamp: Long, now: Long = System.currentTimeMillis()): Int {
        if (birthTimestamp <= 0L) return 0
        val diffMillis = (now - birthTimestamp).coerceAtLeast(0L)
        return (diffMillis / (1000L * 60 * 60 * 24)).toInt()
    }

    /**
     * Avalia qual o estágio máximo elegível segundo as regras híbridas (tempo real + nível mínimo).
     * Aplica a Regra de Ouro da Não-Regressão: o estágio resultante NUNCA será inferior ao [currentStage].
     */
    fun evaluateEligibleStage(
        currentStage: PetStage,
        daysAlive: Int,
        level: Int,
        isHatched: Boolean
    ): PetStage {
        if (!isHatched || currentStage == PetStage.OVO) {
            return PetStage.OVO
        }

        // Determina o estágio morfológico baseado estritamente nas regras híbridas
        val calculatedStage = when {
            PetStage.IDOSO.meetsRequirements(daysAlive, level) -> PetStage.IDOSO
            PetStage.ADULTO.meetsRequirements(daysAlive, level) -> PetStage.ADULTO
            PetStage.JOVEM.meetsRequirements(daysAlive, level) -> PetStage.JOVEM
            else -> PetStage.FILHOTE
        }

        // REGRA DE NÃO REGRESSÃO:
        // Se o pet já alcançou um estágio no passado (ex: Adulto com 3 dias e Nv. 27),
        // ele nunca regride para um ordinal menor.
        return if (calculatedStage.ordinal >= currentStage.ordinal) {
            calculatedStage
        } else {
            currentStage
        }
    }

    /**
     * Retorna o próximo estágio após o estágio atual, se houver.
     */
    fun getNextStage(currentStage: PetStage): PetStage? {
        return currentStage.nextStage()
    }

    /**
     * Retorna o relatório completo de progresso para a próxima evolução do pet.
     */
    fun getEvolutionProgress(pet: PetEntity, now: Long = System.currentTimeMillis()): EvolutionProgress {
        val currentStage = try {
            PetStage.valueOf(pet.stage)
        } catch (_: Exception) {
            if (pet.isHatched) PetStage.FILHOTE else PetStage.OVO
        }
        val daysAlive = calculateDaysAlive(pet.birthTimestamp, now)
        val level = pet.level
        val next = currentStage.nextStage()

        val reqDays = next?.minDaysAlive ?: currentStage.minDaysAlive
        val reqLevel = next?.minLevel ?: currentStage.minLevel

        val isDaysMet = next == null || daysAlive >= reqDays
        val isLevelMet = next == null || level >= reqLevel
        val isReady = next != null && isDaysMet && isLevelMet

        return EvolutionProgress(
            currentStage = currentStage,
            nextStage = next,
            daysAlive = daysAlive,
            currentLevel = level,
            requiredDays = reqDays,
            requiredLevel = reqLevel,
            isDaysRequirementMet = isDaysMet,
            isLevelRequirementMet = isLevelMet,
            isReadyToEvolve = isReady
        )
    }

    /**
     * Retorna a formatação de idade padronizada do bichinho baseada no birthTimestamp:
     * - Menos de 24 horas: "Hoje"
     * - 1 dia: "1 dia"
     * - 2 ou mais dias: "X dias"
     */
    fun formatAge(birthTimestamp: Long, now: Long = System.currentTimeMillis()): String {
        val days = calculateDaysAlive(birthTimestamp, now)
        return formatDays(days)
    }

    /**
     * Formata uma quantidade inteira de dias:
     * - 0 dias: "Hoje"
     * - 1 dia: "1 dia"
     * - >= 2 dias: "X dias"
     */
    fun formatDays(days: Int): String {
        return when {
            days <= 0 -> "Hoje"
            days == 1 -> "1 dia"
            else -> "$days dias"
        }
    }

    /**
     * Retorna a string completa descritiva da idade para visualização detalhada.
     */
    fun getAgeDisplay(pet: PetEntity, now: Long = System.currentTimeMillis()): String {
        return formatAge(pet.birthTimestamp, now)
    }
}
