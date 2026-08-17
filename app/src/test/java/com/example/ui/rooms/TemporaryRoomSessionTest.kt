package com.example.ui.rooms

import com.example.data.model.HouseRoom
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TemporaryRoomSessionTest {

    @Test
    fun kitchen_returnsToLivingRoom_afterTimeout() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        var room = HouseRoom.LIVING_ROOM
        val session = TemporaryRoomSession(scope).apply {
            isSleeping = { false }
            getCurrentRoom = { room }
            setCurrentRoom = { room = it }
        }

        session.enter(HouseRoom.KITCHEN, TemporaryRoomSession.KITCHEN_AFTER_FEED_MS)
        assertEquals(HouseRoom.KITCHEN, room)
        assertTrue(session.hasActiveTimer())

        advanceTimeBy(TemporaryRoomSession.KITCHEN_AFTER_FEED_MS - 1)
        assertEquals(HouseRoom.KITCHEN, room)

        advanceTimeBy(1)
        assertEquals(HouseRoom.LIVING_ROOM, room)
        assertFalse(session.hasActiveTimer())
    }

    @Test
    fun backyard_bump_restartsTimer() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        var room = HouseRoom.LIVING_ROOM
        val session = TemporaryRoomSession(scope).apply {
            isSleeping = { false }
            getCurrentRoom = { room }
            setCurrentRoom = { room = it }
        }

        session.enter(HouseRoom.BACKYARD, TemporaryRoomSession.BACKYARD_AFTER_PLAY_MS)
        advanceTimeBy(10_000)
        session.bump(HouseRoom.BACKYARD, TemporaryRoomSession.BACKYARD_AFTER_PLAY_MS)

        advanceTimeBy(TemporaryRoomSession.BACKYARD_AFTER_PLAY_MS - 1)
        assertEquals(HouseRoom.BACKYARD, room)

        advanceTimeBy(1)
        assertEquals(HouseRoom.LIVING_ROOM, room)
    }

    @Test
    fun bedroom_isIgnored_byEnter() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        var room = HouseRoom.LIVING_ROOM
        val session = TemporaryRoomSession(scope).apply {
            isSleeping = { false }
            getCurrentRoom = { room }
            setCurrentRoom = { room = it }
        }

        session.enter(HouseRoom.BEDROOM, 1_000)
        assertEquals(HouseRoom.LIVING_ROOM, room)
        assertFalse(session.hasActiveTimer())
    }

    @Test
    fun sleeping_blocksReturnToLivingRoom() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        var sleeping = false
        var room = HouseRoom.KITCHEN
        val session = TemporaryRoomSession(scope).apply {
            isSleeping = { sleeping }
            getCurrentRoom = { room }
            setCurrentRoom = { room = it }
        }

        session.enter(HouseRoom.KITCHEN, 100)
        sleeping = true
        advanceTimeBy(100)
        assertEquals(HouseRoom.KITCHEN, room)
    }
}
