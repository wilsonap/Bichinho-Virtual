package com.example.time

import com.example.data.model.DayPeriod
import com.example.data.model.HouseRoom
import com.example.data.model.PlayLocationRules
import com.example.data.model.WeatherState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class GameTimeManagerTest {

    @Before
    fun setUp() {
        GameTimeManager.weatherOverride = WeatherState.CLEAR
    }

    @After
    fun tearDown() {
        GameTimeManager.clock = { System.currentTimeMillis() }
        GameTimeManager.weatherOverride = null
    }

    private fun ts(hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.AUGUST)
            set(Calendar.DAY_OF_MONTH, 21)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun period_0729_isNight() {
        assertEquals(DayPeriod.NIGHT, GameTimeManager.periodAt(ts(7, 29)))
    }

    @Test
    fun period_0730_isMorning() {
        assertEquals(DayPeriod.MORNING, GameTimeManager.periodAt(ts(7, 30)))
    }

    @Test
    fun period_1200_isAfternoon() {
        assertEquals(DayPeriod.AFTERNOON, GameTimeManager.periodAt(ts(12, 0)))
    }

    @Test
    fun period_1729_isAfternoon() {
        assertEquals(DayPeriod.AFTERNOON, GameTimeManager.periodAt(ts(17, 29)))
    }

    @Test
    fun period_1730_isEvening() {
        assertEquals(DayPeriod.EVENING, GameTimeManager.periodAt(ts(17, 30)))
    }

    @Test
    fun period_1829_isEvening() {
        assertEquals(DayPeriod.EVENING, GameTimeManager.periodAt(ts(18, 29)))
    }

    @Test
    fun period_1830_isNight() {
        assertEquals(DayPeriod.NIGHT, GameTimeManager.periodAt(ts(18, 30)))
    }

    @Test
    fun period_1910_isNight() {
        assertEquals(DayPeriod.NIGHT, GameTimeManager.periodAt(ts(19, 10)))
    }

    @Test
    fun period_2159_isNight() {
        assertEquals(DayPeriod.NIGHT, GameTimeManager.periodAt(ts(21, 59)))
    }

    @Test
    fun period_2200_isNight() {
        assertEquals(DayPeriod.NIGHT, GameTimeManager.periodAt(ts(22, 0)))
    }

    @Test
    fun boundary_0729_to_0730() {
        assertEquals(ts(7, 30), GameTimeManager.nextPeriodBoundaryTimestamp(ts(7, 29)))
    }

    @Test
    fun boundary_1159_to_1200() {
        assertEquals(ts(12, 0), GameTimeManager.nextPeriodBoundaryTimestamp(ts(11, 59)))
    }

    @Test
    fun boundary_1729_to_1730() {
        assertEquals(ts(17, 30), GameTimeManager.nextPeriodBoundaryTimestamp(ts(17, 29)))
    }

    @Test
    fun boundary_1829_to_1830() {
        assertEquals(ts(18, 30), GameTimeManager.nextPeriodBoundaryTimestamp(ts(18, 29)))
    }

    @Test
    fun boundary_2159_nextIsTomorrowMorning() {
        // 22:00 não é fronteira visual; próxima mudança é 07:30 do dia seguinte
        assertEquals(
            Calendar.getInstance().apply {
                timeInMillis = ts(21, 59)
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 7)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis,
            GameTimeManager.nextPeriodBoundaryTimestamp(ts(21, 59))
        )
    }

    @Test
    fun boundary_2200_nextIsTomorrowMorning() {
        assertEquals(
            Calendar.getInstance().apply {
                timeInMillis = ts(22, 0)
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 7)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis,
            GameTimeManager.nextPeriodBoundaryTimestamp(ts(22, 0))
        )
    }

    @Test
    fun play_day_goesToBackyard() {
        val dest = PlayLocationRules.resolve(
            DayPeriod.MORNING,
            WeatherState.CLEAR,
            isSleeping = false,
            inMandatorySleepHours = false
        )
        assertTrue(dest is PlayLocationRules.Destination.Go)
        assertEquals(HouseRoom.BACKYARD, (dest as PlayLocationRules.Destination.Go).room)
    }

    @Test
    fun play_evening_goesToGarage() {
        val dest = PlayLocationRules.resolve(
            DayPeriod.EVENING,
            WeatherState.CLEAR,
            isSleeping = false,
            inMandatorySleepHours = false
        )
        assertTrue(dest is PlayLocationRules.Destination.Go)
        val go = dest as PlayLocationRules.Destination.Go
        assertEquals(HouseRoom.GARAGE, go.room)
        assertTrue(go.message!!.contains("garagem"))
    }

    @Test
    fun play_visualNight_awake_goesToGarage() {
        // 19:00 visual = NIGHT, mas pet acordado até 22:00
        val dest = PlayLocationRules.resolve(
            DayPeriod.NIGHT,
            WeatherState.CLEAR,
            isSleeping = false,
            inMandatorySleepHours = false
        )
        assertTrue(dest is PlayLocationRules.Destination.Go)
        assertEquals(HouseRoom.GARAGE, (dest as PlayLocationRules.Destination.Go).room)
    }

    @Test
    fun play_rain_goesIndoor() {
        val dest = PlayLocationRules.resolve(
            DayPeriod.AFTERNOON,
            WeatherState.RAIN,
            isSleeping = false,
            inMandatorySleepHours = false
        )
        assertTrue(dest is PlayLocationRules.Destination.Go)
        assertEquals(HouseRoom.GARAGE, (dest as PlayLocationRules.Destination.Go).room)
        assertTrue(goMessage(dest).contains("chovendo"))
    }

    @Test
    fun play_mandatorySleepHours_blocked() {
        val dest = PlayLocationRules.resolve(
            DayPeriod.NIGHT,
            WeatherState.CLEAR,
            isSleeping = false,
            inMandatorySleepHours = true
        )
        assertTrue(dest is PlayLocationRules.Destination.Blocked)
    }

    @Test
    fun play_sleeping_blocked() {
        val dest = PlayLocationRules.resolve(
            DayPeriod.AFTERNOON,
            WeatherState.CLEAR,
            isSleeping = true,
            inMandatorySleepHours = false
        )
        assertTrue(dest is PlayLocationRules.Destination.Blocked)
    }

    @Test
    fun refresh_onlyEmitsOnPeriodChange() {
        GameTimeManager.clock = { ts(10, 0) }
        GameTimeManager.forceRefresh()
        val a = GameTimeManager.snapshot.value
        GameTimeManager.refresh(ts(10, 30)) // still morning
        assertEquals(a.period, GameTimeManager.snapshot.value.period)
        GameTimeManager.refresh(ts(12, 0))
        assertEquals(DayPeriod.AFTERNOON, GameTimeManager.snapshot.value.period)
    }

    @Test
    fun visualNightCoversAwakeAndSleepWindows() {
        // Visual NIGHT: 18:30–07:29 (pet pode estar acordado até 22:00)
        assertEquals(DayPeriod.NIGHT, GameTimeManager.periodAt(ts(18, 30)))
        assertEquals(DayPeriod.NIGHT, GameTimeManager.periodAt(ts(19, 0)))
        assertEquals(DayPeriod.NIGHT, GameTimeManager.periodAt(ts(21, 30)))
        assertEquals(DayPeriod.NIGHT, GameTimeManager.periodAt(ts(22, 0)))
        assertEquals(DayPeriod.NIGHT, GameTimeManager.periodAt(ts(3, 0)))
        assertEquals(DayPeriod.NIGHT, GameTimeManager.periodAt(ts(7, 29)))
        assertFalse(GameTimeManager.periodAt(ts(7, 30)) == DayPeriod.NIGHT)
    }

    private fun goMessage(dest: PlayLocationRules.Destination): String =
        (dest as PlayLocationRules.Destination.Go).message.orEmpty()
}
