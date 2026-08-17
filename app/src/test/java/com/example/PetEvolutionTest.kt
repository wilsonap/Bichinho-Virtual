package com.example

import com.example.data.local.PetEntity
import com.example.data.model.PetEvolutionCalculator
import com.example.data.model.PetStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class PetEvolutionTest {

    private val oneDayMs = TimeUnit.DAYS.toMillis(1)

    @Test
    fun testNonRegression_adultPetWithInsufficientDays_doesNotRegressToYouth() {
        // Pet Coruja Adulto Nv. 27 com 3 dias de vida (migrado do sistema antigo)
        val birthTimestamp = System.currentTimeMillis() - (3 * oneDayMs)
        val now = System.currentTimeMillis()
        val daysAlive = PetEvolutionCalculator.calculateDaysAlive(birthTimestamp, now)

        val stage = PetEvolutionCalculator.evaluateEligibleStage(
            currentStage = PetStage.ADULTO,
            daysAlive = daysAlive,
            level = 27,
            isHatched = true
        )

        // Deve permanecer ADULTO, NUNCA regredir para JOVEM ou FILHOTE
        assertEquals(PetStage.ADULTO, stage)
    }

    @Test
    fun testFilhote_withInsufficientDays_doesNotEvolve() {
        // Nv. 10, mas apenas 1 dia de vida (< 2 dias)
        val stage = PetEvolutionCalculator.evaluateEligibleStage(
            currentStage = PetStage.FILHOTE,
            daysAlive = 1,
            level = 10,
            isHatched = true
        )
        assertEquals(PetStage.FILHOTE, stage)
    }

    @Test
    fun testFilhote_withInsufficientLevel_doesNotEvolve() {
        // 5 dias de vida, mas apenas Nv. 3 (< Nv. 5)
        val stage = PetEvolutionCalculator.evaluateEligibleStage(
            currentStage = PetStage.FILHOTE,
            daysAlive = 5,
            level = 3,
            isHatched = true
        )
        assertEquals(PetStage.FILHOTE, stage)
    }

    @Test
    fun testFilhote_withMetRequirements_evolvesToJovem() {
        // 2 dias de vida e Nv. 5
        val stage = PetEvolutionCalculator.evaluateEligibleStage(
            currentStage = PetStage.FILHOTE,
            daysAlive = 2,
            level = 5,
            isHatched = true
        )
        assertEquals(PetStage.JOVEM, stage)
    }

    @Test
    fun testJovem_withInsufficientDays_doesNotEvolveToAdulto() {
        // Nv. 15, mas apenas 5 dias de vida (< 7 dias)
        val stage = PetEvolutionCalculator.evaluateEligibleStage(
            currentStage = PetStage.JOVEM,
            daysAlive = 5,
            level = 15,
            isHatched = true
        )
        assertEquals(PetStage.JOVEM, stage)
    }

    @Test
    fun testJovem_withInsufficientLevel_doesNotEvolveToAdulto() {
        // 10 dias de vida, mas apenas Nv. 11 (< Nv. 12)
        val stage = PetEvolutionCalculator.evaluateEligibleStage(
            currentStage = PetStage.JOVEM,
            daysAlive = 10,
            level = 11,
            isHatched = true
        )
        assertEquals(PetStage.JOVEM, stage)
    }

    @Test
    fun testJovem_withMetRequirements_evolvesToAdulto() {
        // 7 dias de vida e Nv. 12
        val stage = PetEvolutionCalculator.evaluateEligibleStage(
            currentStage = PetStage.JOVEM,
            daysAlive = 7,
            level = 12,
            isHatched = true
        )
        assertEquals(PetStage.ADULTO, stage)
    }

    @Test
    fun testAdulto_withMetRequirements_evolvesToIdoso() {
        // 30 dias de vida e Nv. 25
        val stage = PetEvolutionCalculator.evaluateEligibleStage(
            currentStage = PetStage.ADULTO,
            daysAlive = 30,
            level = 25,
            isHatched = true
        )
        assertEquals(PetStage.IDOSO, stage)
    }

    @Test
    fun testExistingAdultOwl_whenReaching30DaysAndLevel27_evolvesToIdoso() {
        // A Coruja Adulto Nv. 27 com 30 dias de vida completa os requisitos para IDOSO
        val stage = PetEvolutionCalculator.evaluateEligibleStage(
            currentStage = PetStage.ADULTO,
            daysAlive = 30,
            level = 27,
            isHatched = true
        )
        assertEquals(PetStage.IDOSO, stage)
    }

    @Test
    fun testIdoso_isPermanentMaxStage_neverChanges() {
        // Idoso com 100 dias e Nv. 99 permanece IDOSO
        val stage = PetEvolutionCalculator.evaluateEligibleStage(
            currentStage = PetStage.IDOSO,
            daysAlive = 100,
            level = 99,
            isHatched = true
        )
        assertEquals(PetStage.IDOSO, stage)
    }

    @Test
    fun testEgg_doesNotEvolve() {
        val stage = PetEvolutionCalculator.evaluateEligibleStage(
            currentStage = PetStage.OVO,
            daysAlive = 50,
            level = 50,
            isHatched = false
        )
        assertEquals(PetStage.OVO, stage)
    }

    @Test
    fun testEvolutionProgress_detailedRequirements() {
        val now = System.currentTimeMillis()
        val pet = PetEntity(
            id = 1,
            name = "Coruja",
            speciesId = "coruja",
            stage = "ADULTO",
            level = 27,
            birthTimestamp = now - (14 * oneDayMs),
            isHatched = true
        )

        val progress = PetEvolutionCalculator.getEvolutionProgress(pet, now)

        assertEquals(14, progress.daysAlive)
        assertEquals(27, progress.currentLevel)
        assertEquals(PetStage.ADULTO, progress.currentStage)
        assertEquals(PetStage.IDOSO, progress.nextStage)
        assertFalse(progress.isDaysRequirementMet) // 14 < 30
        assertTrue(progress.isLevelRequirementMet) // 27 >= 25
        assertFalse(progress.isReadyToEvolve) // Só evolui quando cumprir ambos
    }

    @Test
    fun testEvolutionProgress_whenMaxStageReached() {
        val now = System.currentTimeMillis()
        val pet = PetEntity(
            id = 1,
            name = "Coruja",
            speciesId = "coruja",
            stage = "IDOSO",
            level = 30,
            birthTimestamp = now - (35 * oneDayMs),
            isHatched = true
        )

        val progress = PetEvolutionCalculator.getEvolutionProgress(pet, now)

        assertEquals(PetStage.IDOSO, progress.currentStage)
        assertNull(progress.nextStage)
        assertTrue(progress.nextStage == null)
        assertFalse(progress.isReadyToEvolve)
    }

    @Test
    fun testFormatAge_formattingRules() {
        val now = System.currentTimeMillis()

        // Less than 24 hours (0 days)
        val bornToday = now - (4 * 3600 * 1000L) // 4 hours ago
        assertEquals("Hoje", PetEvolutionCalculator.formatAge(bornToday, now))

        // Exactly 1 day (between 24h and 48h)
        val born1DayAgo = now - (28 * 3600 * 1000L) // 28 hours ago
        assertEquals("1 dia", PetEvolutionCalculator.formatAge(born1DayAgo, now))

        // 2 days
        val born2DaysAgo = now - (2 * oneDayMs)
        assertEquals("2 dias", PetEvolutionCalculator.formatAge(born2DaysAgo, now))

        // 14 days (Coruja Valorante example)
        val born14DaysAgo = now - (14 * oneDayMs)
        assertEquals("14 dias", PetEvolutionCalculator.formatAge(born14DaysAgo, now))

        // 30 days
        val born30DaysAgo = now - (30 * oneDayMs)
        assertEquals("30 dias", PetEvolutionCalculator.formatAge(born30DaysAgo, now))
    }
}
