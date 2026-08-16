package com.example.audio

import android.content.Context
import android.content.SharedPreferences

class AudioPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "bichinho_audio_prefs"

        private const val KEY_MUSIC_ENABLED = "key_music_enabled"
        private const val KEY_SFX_ENABLED = "key_sfx_enabled"
        private const val KEY_MUSIC_VOLUME = "key_music_volume"
        private const val KEY_SFX_VOLUME = "key_sfx_volume"

        const val DEFAULT_MUSIC_VOLUME = 0.50f // 50% default
        const val DEFAULT_SFX_VOLUME = 0.80f   // 80% default
    }

    var isMusicEnabled: Boolean
        get() = prefs.getBoolean(KEY_MUSIC_ENABLED, true)
        set(value) { prefs.edit().putBoolean(KEY_MUSIC_ENABLED, value).commit() }

    var isSfxEnabled: Boolean
        get() = prefs.getBoolean(KEY_SFX_ENABLED, true)
        set(value) { prefs.edit().putBoolean(KEY_SFX_ENABLED, value).commit() }

    var musicVolume: Float
        get() = prefs.getFloat(KEY_MUSIC_VOLUME, DEFAULT_MUSIC_VOLUME)
        set(value) { prefs.edit().putFloat(KEY_MUSIC_VOLUME, value.coerceIn(0f, 1f)).commit() }

    var sfxVolume: Float
        get() = prefs.getFloat(KEY_SFX_VOLUME, DEFAULT_SFX_VOLUME)
        set(value) { prefs.edit().putFloat(KEY_SFX_VOLUME, value.coerceIn(0f, 1f)).commit() }

    fun resetDefaults() {
        prefs.edit()
            .putBoolean(KEY_MUSIC_ENABLED, true)
            .putBoolean(KEY_SFX_ENABLED, true)
            .putFloat(KEY_MUSIC_VOLUME, DEFAULT_MUSIC_VOLUME)
            .putFloat(KEY_SFX_VOLUME, DEFAULT_SFX_VOLUME)
            .commit()
    }
}
