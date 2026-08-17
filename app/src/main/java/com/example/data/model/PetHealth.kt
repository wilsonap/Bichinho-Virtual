package com.example.data.model

enum class PetHealthState(val displayName: String, val icon: String) {
    SAUDAVEL("Saudável", "💚"),
    INDISPOSTO("Indisposto", "💛"),
    DOENTE("Doente", "🩹"),
    CRITICO("Crítico", "🚨");

    companion object {
        fun fromHealth(health: Int): PetHealthState {
            return when {
                health >= 61 -> SAUDAVEL
                health in 41..60 -> INDISPOSTO
                health in 21..40 -> DOENTE
                else -> CRITICO // 10..20
            }
        }
    }
}

enum class PetDisease(
    val displayName: String,
    val description: String,
    val causeDescription: String,
    val treatmentRecommendation: String,
    val iconEmoji: String,
    val curativeItemId: String
) {
    NONE(
        displayName = "Nenhuma",
        description = "O bichinho está saudável e sem nenhuma enfermidade.",
        causeDescription = "Nenhum problema de saúde detectado.",
        treatmentRecommendation = "Continue alimentando, dando banho e carinho diariamente!",
        iconEmoji = "✨",
        curativeItemId = ""
    ),
    INDIGESTAO(
        displayName = "Indigestão",
        description = "Desconforto estomacal causado por excesso de doces ou sobrealimentação.",
        causeDescription = "Consumo repetido de doces ou alimentação com a barriga cheia.",
        treatmentRecommendation = "Remédio Digestivo, alimentação leve e descanso.",
        iconEmoji = "🤢",
        curativeItemId = "med_digestive"
    ),
    RESFRIADO(
        displayName = "Resfriado",
        description = "Bichinho resfriado com espirros decorrente de higiene baixa prolongada.",
        causeDescription = "Permanecer sujo ou com higiene crítica por muito tempo.",
        treatmentRecommendation = "Tomar um bom banho morno, Remédio para Resfriado e repouso.",
        iconEmoji = "🤧",
        curativeItemId = "med_cold"
    ),
    FADIGA(
        displayName = "Fadiga Extrema",
        description = "Exaustão física após atingir o esgotamento de energia repetidas vezes.",
        causeDescription = "Ciclos frequentes de energia crítica (<= 5) sem dormir.",
        treatmentRecommendation = "Dormir uma boa soneca, Vitamina Fortalecedora e descanso.",
        iconEmoji = "🥱",
        curativeItemId = "med_vitamin"
    );

    val recommendedCure: String get() = treatmentRecommendation

    companion object {
        fun fromString(name: String?): PetDisease {
            return try {
                if (name.isNullOrBlank()) NONE else valueOf(name.trim().uppercase())
            } catch (_: Exception) {
                NONE
            }
        }
    }
}

object PetHealthRules {
    const val MIN_HEALTH = 10
    const val MAX_HEALTH = 100

    const val HUNGER_DAMAGE_THRESHOLD = 20
    const val HYGIENE_DAMAGE_THRESHOLD = 20

    const val HEALTH_SAUDAVEL_MIN = 61
    const val HEALTH_INDISPOSTO_MIN = 41
    const val HEALTH_DOENTE_MIN = 21
    const val HEALTH_DOENTE_MAX = 40
    const val HEALTH_CRITICO_MAX = 20

    const val DOCTOR_COOLDOWN_MS = 6 * 60 * 60 * 1000L // 6 hours
    const val DOCTOR_PAID_COST = 30 // coins when on cooldown
    const val DOCTOR_PAID_COST_COINS = 30

    fun getHealthState(health: Int): PetHealthState = PetHealthState.fromHealth(health)

    // Exposure limits to prevent instant random disease
    const val LOW_HYGIENE_EXPOSURE_LIMIT = 6 // 6 cycles/minutes in critical hygiene
    const val EXHAUSTION_COUNT_LIMIT = 3 // 3 cycles/occurrences of zero/critical energy
    const val INDIGESTION_STREAK_LIMIT = 3 // 3 sweets or overfeeding actions in a row
}

sealed class DoctorCheckupResult {
    data class Success(val isFree: Boolean, val message: String) : DoctorCheckupResult()
    data class Cooldown(val remainingMs: Long, val costCoins: Int) : DoctorCheckupResult()
    data class InsufficientCoins(val costCoins: Int, val playerCoins: Int) : DoctorCheckupResult()
}
