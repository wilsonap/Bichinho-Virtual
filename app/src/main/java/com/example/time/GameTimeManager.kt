package com.example.time

import com.example.data.model.DayPeriod
import com.example.data.model.WeatherState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * Fonte única do período do dia e clima visual.
 * Não recalcula a cada frame — [refresh] só emite quando período/clima mudam.
 */
object GameTimeManager {

    data class Snapshot(
        val period: DayPeriod,
        val weather: WeatherState,
        val timestamp: Long
    )

    /** Relógio injetável para testes. */
    @Volatile
    var clock: () -> Long = { System.currentTimeMillis() }

    /** Override opcional de clima (testes / debug). null = clima determinístico do dia. */
    @Volatile
    var weatherOverride: WeatherState? = null

    private val _snapshot = MutableStateFlow(capture(clock()))
    val snapshot: StateFlow<Snapshot> = _snapshot.asStateFlow()

    fun periodAt(timestamp: Long = clock()): DayPeriod {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        // Ciclo VISUAL apenas — sono obrigatório continua 22:00–07:30 (PetStatsCalculator).
        return when {
            minutes in (7 * 60 + 30) until (12 * 60) -> DayPeriod.MORNING      // 07:30–11:59
            minutes in (12 * 60) until (17 * 60 + 30) -> DayPeriod.AFTERNOON // 12:00–17:29
            minutes in (17 * 60 + 30) until (18 * 60 + 30) -> DayPeriod.EVENING // 17:30–18:29
            else -> DayPeriod.NIGHT // 18:30–07:29
        }
    }

    /**
     * Clima visual local estável no mesmo dia civil (não afeta gameplay).
     * Distribuição: maioria CLEAR, alguns CLOUDY/RAIN.
     */
    fun weatherAt(timestamp: Long = clock()): WeatherState {
        weatherOverride?.let { return it }
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val seed = cal.get(Calendar.YEAR) * 400 + cal.get(Calendar.DAY_OF_YEAR)
        return when (seed % 7) {
            0 -> WeatherState.CLOUDY
            1 -> WeatherState.RAIN
            else -> WeatherState.CLEAR
        }
    }

    fun refresh(now: Long = clock()): Snapshot {
        val next = capture(now)
        val cur = _snapshot.value
        if (cur.period != next.period || cur.weather != next.weather || cur.timestamp == 0L) {
            _snapshot.value = next
        }
        return _snapshot.value
    }

    /** Força emissão (ex.: ao voltar do background). */
    fun forceRefresh(now: Long = clock()): Snapshot {
        val next = capture(now)
        _snapshot.value = next
        return next
    }

    /**
     * Ms até a próxima fronteira visual (07:30, 12:00, 17:30, 18:30).
     */
    fun millisUntilNextPeriodChange(fromTimestamp: Long = clock()): Long {
        val next = nextPeriodBoundaryTimestamp(fromTimestamp)
        return (next - fromTimestamp).coerceAtLeast(1L)
    }

    fun nextPeriodBoundaryTimestamp(fromTimestamp: Long = clock()): Long {
        fun at(dayOffset: Int, hour: Int, minute: Int): Long =
            Calendar.getInstance().apply {
                timeInMillis = fromTimestamp
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

        val today = listOf(
            at(0, 7, 30),
            at(0, 12, 0),
            at(0, 17, 30),
            at(0, 18, 30)
        )
        today.firstOrNull { it > fromTimestamp }?.let { return it }
        return at(1, 7, 30)
    }

    private fun capture(now: Long) = Snapshot(
        period = periodAt(now),
        weather = weatherAt(now),
        timestamp = now
    )
}
