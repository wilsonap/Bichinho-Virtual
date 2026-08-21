package com.example.ui.components

import com.example.data.model.DayPeriod
import com.example.data.model.HouseRoom

/**
 * Iluminação artificial da casa — independente do ciclo visual externo ([DayPeriod]).
 *
 * - [DayPeriod] / Weather → janelas e áreas externas
 * - [IndoorLighting] → lâmpadas internas (móveis visíveis à noite)
 * - [isSleeping] → só o quarto usa paleta escura de sono
 */
object IndoorLighting {

    /**
     * Usa a paleta escura de “luzes apagadas / sono”.
     * Apenas o quarto, e somente quando o pet está dormindo.
     * NIGHT visual ≠ apagar as luzes da casa.
     */
    fun useDarkSleepPalette(room: HouseRoom, isSleeping: Boolean): Boolean =
        isSleeping && room == HouseRoom.BEDROOM

    /**
     * Temas decorativos do quarto (floresta/praia/espaço) também escurecem só no sono.
     */
    fun useDarkSleepPaletteForDecor(isSleeping: Boolean): Boolean = isSleeping

    /** Quintal e exteriores: escurecem pelo período visual, não pelo sono. */
    fun outdoorSceneDark(period: DayPeriod): Boolean =
        period == DayPeriod.NIGHT

    /** Luzes artificiais acesas (casa iluminada). */
    fun artificialLightsOn(room: HouseRoom, isSleeping: Boolean): Boolean =
        !useDarkSleepPalette(room, isSleeping)

    /** Brilho quente sutil de lâmpada (entardecer/noite com luzes acesas). */
    fun shouldApplyWarmGlow(
        room: HouseRoom,
        period: DayPeriod,
        isSleeping: Boolean
    ): Boolean {
        if (!artificialLightsOn(room, isSleeping)) return false
        return period == DayPeriod.EVENING || period == DayPeriod.NIGHT
    }
}
