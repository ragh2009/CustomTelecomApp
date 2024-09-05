package com.telecom.app.util

import android.Manifest
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Audio Looping, uses AudioRecord and Audio track to loop audio from microphone back to output device
 * Used for testing microphones and speakers
 */
object AudioLoopSource {

    private const val SAMPLE_RATE = 48000

    /**
     * Opens the active mic and loops it back to the selected active audio device.
     * When the scope is closed the mic and audio will be closed
     *
     * @throws IllegalStateException if AudioRecord couldn't be initialized
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun openAudioLoop(preferredDevice: Flow<AudioDeviceInfo> = emptyFlow()) {
        // Init the recorder and audio
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val audioTrackBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val audioTrack =
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(audioTrackBufferSize)
                .build()
        val audioSampler = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
        audioTrack.playbackRate = SAMPLE_RATE

        try {
            // Launch in a new context the loop
            withContext(Dispatchers.IO) {
                // Collect changes of preferred device to loop the audio
                launch {
                    preferredDevice.collect { device ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            audioTrack.preferredDevice = device
                            audioSampler.preferredDevice = device
                        } else {
                            //Not required AudioManger will deal with routing in the PlatformAudioSource class
                        }
                    }
                }

                check(audioSampler.state == AudioRecord.STATE_INITIALIZED) {
                    "Audio recorder was not properly initialized"
                }
                audioSampler.startRecording()

                val audioData = ByteArray(bufferSize)
                audioTrack.play()

                while (isActive) {
                    val bytesRead = audioSampler.read(audioData, 0, bufferSize)
                    if (bytesRead > 0) {
                        audioTrack.write(audioData, 0, bytesRead)
                    }
                }
            }
        } finally {
            audioTrack.stop()
            audioSampler.stop()
        }
    }
}
