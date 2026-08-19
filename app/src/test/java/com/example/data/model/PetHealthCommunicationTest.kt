package com.example.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetHealthCommunicationTest {

    @Test
    fun announceOnlyOnTransitionIntoIllness() {
        assertTrue(
            PetHealthRules.shouldAnnounceIllnessTransition(
                PetHealthState.INDISPOSTO,
                PetHealthState.DOENTE
            )
        )
        assertTrue(
            PetHealthRules.shouldAnnounceIllnessTransition(
                PetHealthState.SAUDAVEL,
                PetHealthState.CRITICO
            )
        )
        assertFalse(
            PetHealthRules.shouldAnnounceIllnessTransition(
                PetHealthState.DOENTE,
                PetHealthState.DOENTE
            )
        )
        assertFalse(
            PetHealthRules.shouldAnnounceIllnessTransition(
                PetHealthState.DOENTE,
                PetHealthState.CRITICO
            )
        )
        assertFalse(
            PetHealthRules.shouldAnnounceIllnessTransition(null, PetHealthState.DOENTE)
        )
    }

    @Test
    fun recoveryDetectedForNotificationReset() {
        assertTrue(
            PetHealthRules.isRecoveredFromIllness(
                PetHealthState.DOENTE,
                PetHealthState.INDISPOSTO
            )
        )
        assertTrue(
            PetHealthRules.isRecoveredFromIllness(
                PetHealthState.CRITICO,
                PetHealthState.SAUDAVEL
            )
        )
        assertFalse(
            PetHealthRules.isRecoveredFromIllness(
                PetHealthState.DOENTE,
                PetHealthState.CRITICO
            )
        )
    }
}
