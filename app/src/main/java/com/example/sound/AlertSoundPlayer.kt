package com.example.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.util.Log
import com.example.data.model.AlertSoundType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object AlertSoundPlayer {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun play(context: Context, soundType: AlertSoundType) {
        scope.launch {
            try {
                when (soundType) {
                    AlertSoundType.RADAR_PING -> playToneSequence(listOf(880 to 80, 1760 to 180))
                    AlertSoundType.SIREN -> playToneSequence(listOf(1200 to 100, 800 to 100, 1200 to 100, 800 to 120))
                    AlertSoundType.BELL -> playToneSequence(listOf(523 to 120, 659 to 120, 784 to 250))
                    AlertSoundType.CASH_REGISTER -> playToneSequence(listOf(1046 to 70, 1318 to 70, 2093 to 220))
                    AlertSoundType.ALARM_PULSE -> playToneSequence(listOf(1500 to 80, 0 to 40, 1500 to 80, 0 to 40, 1500 to 150))
                }
            } catch (e: Exception) {
                // Fallback to ToneGenerator
                fallbackTone()
            }
        }
    }

    private fun playToneSequence(tones: List<Pair<Int, Int>>) {
        val sampleRate = 44100
        var totalSamples = 0
        tones.forEach { (_, durationMs) ->
            totalSamples += (sampleRate * durationMs / 1000)
        }

        val generatedSnd = ShortArray(totalSamples)
        var cursor = 0

        tones.forEach { (freq, durationMs) ->
            val numSamples = (sampleRate * durationMs / 1000)
            if (freq == 0) {
                for (i in 0 until numSamples) {
                    generatedSnd[cursor++] = 0
                }
            } else {
                for (i in 0 until numSamples) {
                    val angle = 2.0 * Math.PI * i / (sampleRate.toDouble() / freq)
                    // High amplitude for clear loudness
                    val sample = (sin(angle) * 32000).toInt().toShort()
                    generatedSnd[cursor++] = sample
                }
            }
        }

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(generatedSnd.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(generatedSnd, 0, generatedSnd.size)
        audioTrack.play()
    }

    private fun fallbackTone() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
        } catch (e: Exception) {
            Log.e("AlertSoundPlayer", "Failed to play fallback tone", e)
        }
    }
}
