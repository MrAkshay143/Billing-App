package com.billing.app.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.billing.app.R

class SoundHapticManager(private val context: Context) {
    private var soundPool: SoundPool? = null
    private var toneGenerator: ToneGenerator? = null
    private var scanSoundId: Int = 0
    private var isSoundLoaded: Boolean = false

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (_: Exception) {}

        try {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val pool = SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(attributes)
                .build()

            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0 && sampleId == scanSoundId) {
                    isSoundLoaded = true
                }
            }

            try {
                scanSoundId = pool.load(context, R.raw.scan, 1)
            } catch (_: Exception) {}

            soundPool = pool
        } catch (_: Exception) {}
    }

    fun triggerScanFeedback() {
        playBeep()
        vibrate()
    }

    fun playBeep() {
        try {
            if (isSoundLoaded) {
                soundPool?.play(scanSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
            } else {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            }
        } catch (_: Exception) {}
    }

    fun vibrate() {
        try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(100L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(100L)
                }
            }
        } catch (_: Exception) {}
    }

    fun release() {
        try { soundPool?.release() } catch (_: Exception) {}
        try { toneGenerator?.release() } catch (_: Exception) {}
        soundPool = null
        toneGenerator = null
    }
}
