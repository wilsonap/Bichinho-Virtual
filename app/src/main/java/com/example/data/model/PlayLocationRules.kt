package com.example.data.model

import com.example.notification.PetStatsCalculator
import com.example.time.GameTimeManager

/**
 * Destino da ação Brincar conforme período visual + clima.
 * O bloqueio por “noite” usa o sono obrigatório (22:00–07:30),
 * não o DayPeriod.NIGHT visual (que começa às 18:30).
 */
object PlayLocationRules {

    sealed class Destination {
        data class Go(val room: HouseRoom, val message: String?) : Destination()
        data class Blocked(val message: String) : Destination()
    }

    fun resolve(
        period: DayPeriod = GameTimeManager.periodAt(),
        weather: WeatherState = GameTimeManager.weatherAt(),
        isSleeping: Boolean,
        /** Janela de sono obrigatório 22:00–07:30; injetável nos testes. */
        inMandatorySleepHours: Boolean = PetStatsCalculator.isNightTime()
    ): Destination {
        if (isSleeping || inMandatorySleepHours) {
            return Destination.Blocked("🌙 Está muito tarde para brincar lá fora.")
        }

        if (weather == WeatherState.RAIN) {
            return Destination.Go(
                room = HouseRoom.GARAGE,
                message = "🌧️ Está chovendo. Vamos brincar dentro de casa!"
            )
        }

        return when (period) {
            DayPeriod.MORNING, DayPeriod.AFTERNOON ->
                Destination.Go(HouseRoom.BACKYARD, null)
            // Entardecer e noite visual (pet ainda acordado): brinca dentro
            DayPeriod.EVENING, DayPeriod.NIGHT ->
                Destination.Go(
                    room = HouseRoom.GARAGE,
                    message = "Vamos brincar um pouco na garagem! 🚗"
                )
        }
    }
}
