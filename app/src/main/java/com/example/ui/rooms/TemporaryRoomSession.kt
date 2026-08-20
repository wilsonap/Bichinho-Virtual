package com.example.ui.rooms

import com.example.data.model.HouseRoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Sessão de cômodo temporário com retorno automático para a Sala.
 * Espelha o padrão do Banheiro (timer + cancelamento), de forma reutilizável.
 * O Quarto (sono) não deve usar este controller.
 */
class TemporaryRoomSession(
    private val scope: CoroutineScope
) {
    var isSleeping: () -> Boolean = { false }
    var getCurrentRoom: () -> HouseRoom = { HouseRoom.LIVING_ROOM }
    var setCurrentRoom: (HouseRoom) -> Unit = {}

    private var timerJob: Job? = null
    private var activeTemporaryRoom: HouseRoom? = null

    fun enter(room: HouseRoom, timeoutMs: Long) {
        if (isSleeping()) return
        if (room == HouseRoom.BEDROOM || room == HouseRoom.LIVING_ROOM || room == HouseRoom.SCHOOL) return

        activeTemporaryRoom = room
        setCurrentRoom(room)
        restartTimer(timeoutMs)
    }

    /** Reinicia o temporizador do cômodo atual (nova alimentação/brincadeira). */
    fun bump(room: HouseRoom, timeoutMs: Long) {
        if (isSleeping()) return
        if (getCurrentRoom() != room) {
            enter(room, timeoutMs)
            return
        }
        activeTemporaryRoom = room
        restartTimer(timeoutMs)
    }

    fun goToWithoutTimer(room: HouseRoom) {
        cancelTimer()
        if (!isSleeping()) {
            setCurrentRoom(room)
        }
    }

    fun returnToLivingRoomNow() {
        cancelTimer()
        if (!isSleeping() && getCurrentRoom() != HouseRoom.BEDROOM && getCurrentRoom() != HouseRoom.SCHOOL) {
            setCurrentRoom(HouseRoom.LIVING_ROOM)
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        activeTemporaryRoom = null
    }

    /** True enquanto há permanência temporária aguardando timeout. */
    fun hasActiveTimer(): Boolean = timerJob?.isActive == true

    private fun restartTimer(timeoutMs: Long) {
        timerJob?.cancel()
        val room = activeTemporaryRoom ?: return
        timerJob = scope.launch {
            delay(timeoutMs)
            if (!isSleeping() && getCurrentRoom() == room) {
                setCurrentRoom(HouseRoom.LIVING_ROOM)
            }
            activeTemporaryRoom = null
            timerJob = null
        }
    }

    companion object {
        /** Espera a animação de alimentação (~3.2s) + permanência (~5s). */
        const val KITCHEN_AFTER_FEED_MS = 3_200L + 5_000L

        /** Espera a animação de brincadeira (~3.4s) + permanência (~20s). */
        const val BACKYARD_AFTER_PLAY_MS = 3_400L + 20_000L
    }
}
