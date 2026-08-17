package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import com.example.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

enum class BgmTrack(val resId: Int) {
    NONE(0),
    HOME(R.raw.bgm_home),
    INCUBATOR(R.raw.bgm_incubator),
    SHOP(R.raw.bgm_shop),
    MINIGAME(R.raw.bgm_minigame),
    CELEBRATION(R.raw.bgm_celebration)
}

enum class SoundEffect(val resId: Int) {
    TAP(R.raw.sfx_tap),
    BUTTON(R.raw.sfx_button),
    PET_HAPPY(R.raw.sfx_pet_happy),
    PET_SAD(R.raw.sfx_pet_sad),
    PET_SICK(R.raw.sfx_pet_sick),
    FEED(R.raw.sfx_feed),
    DRINK(R.raw.sfx_drink),
    BATH(R.raw.sfx_bath),
    PLAY(R.raw.sfx_play),
    SLEEP(R.raw.sfx_sleep),
    WAKEUP(R.raw.sfx_wakeup),
    YAWN(R.raw.sfx_yawn),
    COIN(R.raw.sfx_coin),
    BUY(R.raw.sfx_buy),
    MISSION(R.raw.sfx_mission),
    ACHIEVEMENT(R.raw.sfx_achievement),
    LEVEL_UP(R.raw.sfx_level_up),
    EGG_CRACK(R.raw.sfx_egg_crack),
    BIRTH(R.raw.sfx_birth),
    EVOLUTION(R.raw.sfx_evolution)
}

class GameAudioManager private constructor(private val appContext: Context) {

    val audioPreferences = AudioPreferences(appContext)
    private val audioPrefs get() = audioPreferences

    // State flows for Compose UI observing
    private val _isMusicEnabled = MutableStateFlow(audioPrefs.isMusicEnabled)
    val isMusicEnabled: StateFlow<Boolean> = _isMusicEnabled.asStateFlow()

    private val _isSfxEnabled = MutableStateFlow(audioPrefs.isSfxEnabled)
    val isSfxEnabled: StateFlow<Boolean> = _isSfxEnabled.asStateFlow()

    private val _musicVolume = MutableStateFlow(audioPrefs.musicVolume)
    val musicVolume: StateFlow<Float> = _musicVolume.asStateFlow()

    private val _sfxVolume = MutableStateFlow(audioPrefs.sfxVolume)
    val sfxVolume: StateFlow<Float> = _sfxVolume.asStateFlow()

    // BGM Player
    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: BgmTrack = BgmTrack.NONE
    private var isAppInForeground: Boolean = true
    private var isDucked: Boolean = false

    // SoundPool for low-latency SFX
    private var soundPool: SoundPool? = null
    private val soundMap = ConcurrentHashMap<SoundEffect, Int>()
    private val loadedSoundIds = ConcurrentHashMap.newKeySet<Int>()
    private val lastPlayTimes = ConcurrentHashMap<SoundEffect, Long>()

    // Coroutine Scope for smooth fading & sequences
    private val audioScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        initSoundPool()
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSoundIds.add(sampleId)
            }
        }

        // Preload all SFX
        SoundEffect.values().forEach { sfx ->
            try {
                val soundId = soundPool?.load(appContext, sfx.resId, 1) ?: 0
                if (soundId > 0) {
                    soundMap[sfx] = soundId
                }
            } catch (e: Exception) {
                Log.e("GameAudioManager", "Error loading sound ${sfx.name}: ${e.message}")
            }
        }
    }

    // ==========================================
    // SFX CONTROLS
    // ==========================================

    fun playSfx(sfx: SoundEffect, minIntervalMs: Long = 60L) {
        if (!_isSfxEnabled.value || soundPool == null) return

        val now = System.currentTimeMillis()
        val lastTime = lastPlayTimes[sfx] ?: 0L
        if (now - lastTime < minIntervalMs) {
            return // Prevent spamming / distorted overlapping
        }
        lastPlayTimes[sfx] = now

        val soundId = soundMap[sfx] ?: return
        if (!loadedSoundIds.contains(soundId)) {
            // Sound sample is still loading asynchronously in SoundPool
            return
        }
        val volume = _sfxVolume.value.coerceIn(0f, 1f)

        try {
            soundPool?.play(soundId, volume, volume, 1, 0, 1.0f)
        } catch (e: Exception) {
            Log.e("GameAudioManager", "Failed to play SFX ${sfx.name}: ${e.message}")
        }
    }

    fun setSfxEnabled(enabled: Boolean) {
        audioPrefs.isSfxEnabled = enabled
        _isSfxEnabled.value = enabled
    }

    fun setSfxVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        audioPrefs.sfxVolume = clamped
        _sfxVolume.value = clamped
    }

    // ==========================================
    // BGM CONTROLS
    // ==========================================

    fun playBgm(track: BgmTrack, restartIfSame: Boolean = false) {
        if (track == BgmTrack.NONE) {
            stopBgm()
            return
        }

        if (currentTrack == track && mediaPlayer?.isPlaying == true && !restartIfSame) {
            return // Already playing this track
        }

        currentTrack = track

        if (!_isMusicEnabled.value || !isAppInForeground) {
            return
        }

        startTrackPlayback(track)
    }

    private fun startTrackPlayback(track: BgmTrack) {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
            mediaPlayer = null

            if (track.resId == 0) return

            mediaPlayer = MediaPlayer.create(appContext, track.resId)?.apply {
                isLooping = (track != BgmTrack.CELEBRATION)
                val effectiveVol = if (isDucked) _musicVolume.value * 0.25f else _musicVolume.value
                setVolume(effectiveVol, effectiveVol)
                setOnCompletionListener {
                    if (track == BgmTrack.CELEBRATION) {
                        // After celebration fanfare finishes, restore the home track
                        currentTrack = BgmTrack.HOME
                        if (_isMusicEnabled.value && isAppInForeground) {
                            startTrackPlayback(BgmTrack.HOME)
                        }
                    }
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("GameAudioManager", "Error starting BGM $track: ${e.message}")
        }
    }

    fun stopBgm() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
            mediaPlayer = null
            currentTrack = BgmTrack.NONE
        } catch (e: Exception) {
            Log.e("GameAudioManager", "Error stopping BGM: ${e.message}")
        }
    }

    fun pauseBgm() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e("GameAudioManager", "Error pausing BGM: ${e.message}")
        }
    }

    fun resumeBgm() {
        if (!_isMusicEnabled.value || !isAppInForeground) return

        try {
            if (mediaPlayer != null) {
                if (mediaPlayer?.isPlaying == false && currentTrack != BgmTrack.NONE) {
                    mediaPlayer?.start()
                }
            } else if (currentTrack != BgmTrack.NONE) {
                startTrackPlayback(currentTrack)
            }
        } catch (e: Exception) {
            Log.e("GameAudioManager", "Error resuming BGM: ${e.message}")
        }
    }

    fun setMusicEnabled(enabled: Boolean) {
        audioPrefs.isMusicEnabled = enabled
        _isMusicEnabled.value = enabled

        if (enabled) {
            if (isAppInForeground && currentTrack != BgmTrack.NONE) {
                startTrackPlayback(currentTrack)
            }
        } else {
            pauseBgm()
        }
    }

    fun setMusicVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        audioPrefs.musicVolume = clamped
        _musicVolume.value = clamped

        val effectiveVol = if (isDucked) clamped * 0.25f else clamped
        try {
            mediaPlayer?.setVolume(effectiveVol, effectiveVol)
        } catch (e: Exception) {
            Log.e("GameAudioManager", "Error setting music volume: ${e.message}")
        }
    }

    // ==========================================
    // SPECIAL AUDIO SEQUENCES
    // ==========================================

    fun playBirthSequence(onComplete: () -> Unit = {}) {
        audioScope.launch {
            // Duck BGM
            isDucked = true
            val duckVol = _musicVolume.value * 0.2f
            mediaPlayer?.setVolume(duckVol, duckVol)

            // Step 1: Cracks
            playSfx(SoundEffect.EGG_CRACK)
            delay(400)
            playSfx(SoundEffect.EGG_CRACK)
            delay(500)
            playSfx(SoundEffect.EGG_CRACK)
            delay(600)

            // Step 2: Birth Sparkle & Fanfare
            playSfx(SoundEffect.BIRTH)
            delay(1200)

            // Step 3: Celebration BGM
            playBgm(BgmTrack.CELEBRATION)
            isDucked = false
            onComplete()
        }
    }

    fun playEvolutionSequence(onComplete: () -> Unit = {}) {
        audioScope.launch {
            // Duck BGM
            isDucked = true
            val duckVol = _musicVolume.value * 0.2f
            mediaPlayer?.setVolume(duckVol, duckVol)

            // Evolution rising sound
            playSfx(SoundEffect.EVOLUTION)
            delay(1800)

            // Fanfare
            playSfx(SoundEffect.LEVEL_UP)
            delay(1000)

            // Celebration BGM
            playBgm(BgmTrack.CELEBRATION)
            isDucked = false
            onComplete()
        }
    }

    // ==========================================
    // LIFECYCLE MANAGEMENT
    // ==========================================

    fun onAppForeground() {
        isAppInForeground = true
        resumeBgm()
    }

    fun onAppBackground() {
        isAppInForeground = false
        pauseBgm()
    }

    fun release() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            soundPool?.release()
            soundPool = null
            soundMap.clear()
            loadedSoundIds.clear()
        } catch (e: Exception) {
            Log.e("GameAudioManager", "Error releasing GameAudioManager: ${e.message}")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: GameAudioManager? = null

        fun getInstance(context: Context): GameAudioManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GameAudioManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
