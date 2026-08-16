package com.example.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioSystemTest {

    private lateinit var context: Context
    private lateinit var preferences: AudioPreferences
    private lateinit var audioManager: GameAudioManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        audioManager = GameAudioManager.getInstance(context)
        preferences = audioManager.audioPreferences
        preferences.resetDefaults()
        audioManager.setMusicEnabled(true)
        audioManager.setSfxEnabled(true)
        audioManager.setMusicVolume(AudioPreferences.DEFAULT_MUSIC_VOLUME)
        audioManager.setSfxVolume(AudioPreferences.DEFAULT_SFX_VOLUME)
    }

    @Test
    fun testDefaultAudioPreferences() {
        preferences.resetDefaults()
        // By default: Music: on, SFX: on, Music Volume: 50%, SFX Volume: 80%
        assertTrue("Music should be enabled by default", preferences.isMusicEnabled)
        assertTrue("SFX should be enabled by default", preferences.isSfxEnabled)
        assertEquals("Music volume should default to 0.5f", 0.5f, preferences.musicVolume, 0.01f)
        assertEquals("SFX volume should default to 0.8f", 0.8f, preferences.sfxVolume, 0.01f)
    }

    @Test
    fun testToggleMusicPreference() {
        preferences.isMusicEnabled = false
        assertFalse(preferences.isMusicEnabled)

        preferences.isMusicEnabled = true
        assertTrue(preferences.isMusicEnabled)
    }

    @Test
    fun testToggleSfxPreference() {
        preferences.isSfxEnabled = false
        assertFalse(preferences.isSfxEnabled)

        preferences.isSfxEnabled = true
        assertTrue(preferences.isSfxEnabled)
    }

    @Test
    fun testVolumeClamping() {
        preferences.musicVolume = 1.5f
        assertEquals(1.0f, preferences.musicVolume, 0.01f)

        preferences.musicVolume = -0.5f
        assertEquals(0.0f, preferences.musicVolume, 0.01f)

        preferences.sfxVolume = 2.0f
        assertEquals(1.0f, preferences.sfxVolume, 0.01f)

        preferences.sfxVolume = -1.0f
        assertEquals(0.0f, preferences.sfxVolume, 0.01f)
    }

    @Test
    fun testAudioManagerStateSync() {
        audioManager.setMusicEnabled(false)
        assertFalse(audioManager.isMusicEnabled.value)
        assertFalse(preferences.isMusicEnabled)

        audioManager.setMusicVolume(0.35f)
        assertEquals(0.35f, audioManager.musicVolume.value, 0.01f)
        assertEquals(0.35f, preferences.musicVolume, 0.01f)

        audioManager.setSfxEnabled(false)
        assertFalse(audioManager.isSfxEnabled.value)
        assertFalse(preferences.isSfxEnabled)

        audioManager.setSfxVolume(0.95f)
        assertEquals(0.95f, audioManager.sfxVolume.value, 0.01f)
        assertEquals(0.95f, preferences.sfxVolume, 0.01f)
    }

    @Test
    fun testPlaySoundEffectsWithoutCrashing() {
        audioManager.setSfxEnabled(true)
        SoundEffect.entries.forEach { effect ->
            audioManager.playSfx(effect)
        }
        assertTrue("All sound effects played without crashing", true)
    }

    @Test
    fun testPlaySoundEffectsWhenDisabled() {
        audioManager.setSfxEnabled(false)
        audioManager.playSfx(SoundEffect.FEED)
        assertFalse(audioManager.isSfxEnabled.value)
    }

    @Test
    fun testBgmTrackSwitching() {
        audioManager.setMusicEnabled(true)
        audioManager.playBgm(BgmTrack.HOME)
        audioManager.playBgm(BgmTrack.SHOP)
        audioManager.playBgm(BgmTrack.MINIGAME)
        audioManager.playBgm(BgmTrack.INCUBATOR)
        audioManager.stopBgm()
        assertTrue("BGM track switching handled gracefully", true)
    }

    @Test
    fun testLifecycleTransitions() {
        // App goes to background
        audioManager.onAppBackground()
        // App returns to foreground
        audioManager.onAppForeground()
        // App destroyed
        audioManager.release()
        assertTrue(true)
    }
}
