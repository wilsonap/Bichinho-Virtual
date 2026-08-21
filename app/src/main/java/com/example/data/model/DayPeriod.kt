package com.example.data.model

/**
 * Período do dia baseado no horário local do dispositivo.
 * Fonte de verdade: [com.example.time.GameTimeManager].
 */
enum class DayPeriod {
    /** Visual 07:30 – 11:59 */
    MORNING,
    /** Visual 12:00 – 17:29 */
    AFTERNOON,
    /** Visual 17:30 – 18:29 */
    EVENING,
    /**
     * Visual 18:30 – 07:29.
     * Independente do sono obrigatório (22:00–07:30 em [PetStatsCalculator]).
     */
    NIGHT
}

/**
 * Clima visual local (não afeta saúde/gameplay nesta etapa).
 */
enum class WeatherState {
    CLEAR,
    CLOUDY,
    RAIN
}
