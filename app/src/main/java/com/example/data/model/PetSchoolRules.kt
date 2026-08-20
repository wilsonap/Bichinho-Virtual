package com.example.data.model

import com.example.data.local.PetEntity
import java.util.Calendar

/**
 * Regras da Escola do Bichinho (FILHOTE / JOVEM).
 * Não altera thresholds de fome/higiene/saúde — apenas janelas e elegibilidade.
 */
object PetSchoolRules {

    const val MIN_USEFUL_MINUTES = 15L
    const val REWARD_XP = 10
    const val REWARD_HAPPINESS = 5
    const val REWARD_COINS = 5

    enum class Shift(
        val displayName: String,
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int
    ) {
        MORNING("manhã", 8, 0, 12, 0),
        AFTERNOON("tarde", 13, 0, 17, 0);

        fun endLabel(): String =
            "%02d:%02d".format(endHour, endMinute)
    }

    sealed class Eligibility {
        data class Allowed(val shift: Shift, val endTimestamp: Long, val remainingMinutes: Long) : Eligibility()
        data class Denied(val reason: String) : Eligibility()
    }

    fun canAttendSchool(pet: PetEntity, now: Long = System.currentTimeMillis()): Eligibility {
        if (!pet.isHatched) {
            return Eligibility.Denied("O ovo ainda não chocou.")
        }
        if (pet.isAtSchool) {
            return Eligibility.Denied("Já está na escola.")
        }
        if (pet.isSleeping) {
            return Eligibility.Denied("Não pode ir à escola dormindo.")
        }

        val stage = try {
            PetStage.valueOf(pet.stage)
        } catch (_: Exception) {
            PetStage.FILHOTE
        }
        if (stage != PetStage.FILHOTE && stage != PetStage.JOVEM) {
            return Eligibility.Denied("Só Filhote e Jovem vão à escola.")
        }

        val shift = currentShift(now)
            ?: return Eligibility.Denied("Fora do horário escolar (08–12 ou 13–17).")

        val endTs = endTimestampForShift(now, shift)
        val remainingMs = endTs - now
        val remainingMin = remainingMs / (1000 * 60)
        if (remainingMin < MIN_USEFUL_MINUTES) {
            return Eligibility.Denied("Turno quase acabando — tente no próximo.")
        }
        return Eligibility.Allowed(shift, endTs, remainingMin)
    }

    fun currentShift(now: Long = System.currentTimeMillis()): Shift? {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val morningStart = Shift.MORNING.startHour * 60 + Shift.MORNING.startMinute
        val morningEnd = Shift.MORNING.endHour * 60 + Shift.MORNING.endMinute
        val afternoonStart = Shift.AFTERNOON.startHour * 60 + Shift.AFTERNOON.startMinute
        val afternoonEnd = Shift.AFTERNOON.endHour * 60 + Shift.AFTERNOON.endMinute
        return when {
            minutes in morningStart until morningEnd -> Shift.MORNING
            minutes in afternoonStart until afternoonEnd -> Shift.AFTERNOON
            else -> null
        }
    }

    fun endTimestampForShift(now: Long, shift: Shift): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, shift.endHour)
            set(Calendar.MINUTE, shift.endMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun isWithinSchoolSession(pet: PetEntity, timestamp: Long): Boolean {
        return pet.isAtSchool &&
            pet.schoolEndTimestamp > 0L &&
            timestamp < pet.schoolEndTimestamp
    }

    fun formatSchoolUntilLabel(schoolEndTimestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = schoolEndTimestamp }
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        return "📚 Na escola até %02d:%02d".format(h, m)
    }
}
