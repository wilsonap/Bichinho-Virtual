package com.example.ui.components

import com.example.data.model.DayPeriod
import com.example.data.model.HouseRoom
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IndoorLightingTest {

    @Test
    fun nightDoesNotDarkenLivingRoom() {
        assertFalse(
            IndoorLighting.useDarkSleepPalette(HouseRoom.LIVING_ROOM, isSleeping = false)
        )
        assertTrue(
            IndoorLighting.artificialLightsOn(HouseRoom.LIVING_ROOM, isSleeping = false)
        )
    }

    @Test
    fun nightAwakeBedroomKeepsLightsOn() {
        // 19:20 — NIGHT visual, pet acordado
        assertFalse(IndoorLighting.useDarkSleepPalette(HouseRoom.BEDROOM, isSleeping = false))
        assertTrue(IndoorLighting.shouldApplyWarmGlow(HouseRoom.BEDROOM, DayPeriod.NIGHT, false))
    }

    @Test
    fun sleepingBedroomUsesDarkPalette() {
        // 22:30 — pet dormindo
        assertTrue(IndoorLighting.useDarkSleepPalette(HouseRoom.BEDROOM, isSleeping = true))
        assertFalse(IndoorLighting.shouldApplyWarmGlow(HouseRoom.BEDROOM, DayPeriod.NIGHT, true))
    }

    @Test
    fun kitchenBathroomStayLitAtNight() {
        assertTrue(IndoorLighting.artificialLightsOn(HouseRoom.KITCHEN, false))
        assertTrue(IndoorLighting.artificialLightsOn(HouseRoom.BATHROOM, false))
        assertFalse(IndoorLighting.useDarkSleepPalette(HouseRoom.KITCHEN, isSleeping = true))
        assertFalse(IndoorLighting.useDarkSleepPalette(HouseRoom.BATHROOM, isSleeping = true))
    }

    @Test
    fun backyardDarkensByPeriodNotSleep() {
        assertTrue(IndoorLighting.outdoorSceneDark(DayPeriod.NIGHT))
        assertFalse(IndoorLighting.outdoorSceneDark(DayPeriod.AFTERNOON))
        assertFalse(IndoorLighting.outdoorSceneDark(DayPeriod.EVENING))
    }

    @Test
    fun garageKeepsArtificialLight() {
        assertTrue(IndoorLighting.artificialLightsOn(HouseRoom.GARAGE, false))
        assertTrue(IndoorLighting.shouldApplyWarmGlow(HouseRoom.GARAGE, DayPeriod.NIGHT, false))
    }

    @Test
    fun dimInteriorIgnoresNightPeriod() {
        assertFalse(OutdoorAmbience.dimInterior(DayPeriod.NIGHT, isSleeping = false))
        assertTrue(OutdoorAmbience.dimInterior(DayPeriod.AFTERNOON, isSleeping = true))
    }
}
