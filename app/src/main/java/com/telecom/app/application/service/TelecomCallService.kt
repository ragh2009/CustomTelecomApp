package com.telecom.app.application.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.telecom.InCallService
import androidx.annotation.RequiresApi
import androidx.core.content.PermissionChecker
import com.telecom.app.data.model.TelecomCallData
import com.telecom.app.data.repository.TelecomCallRepository
import com.telecom.app.domain.usecase.TelecomCallAction
import com.telecom.app.notification.TelecomCallNotificationManager
import com.telecom.app.util.AudioLoopSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class TelecomCallService : InCallService() {
    companion object {
        internal const val EXTRA_NAME: String = "extra_name"
        internal const val EXTRA_URI: String = "extra_uri"
        internal const val ACTION_INCOMING_CALL = "incoming_call"
        internal const val ACTION_OUTGOING_CALL = "outgoing_call"
        internal const val ACTION_UPDATE_CALL = "update_call"
    }

    private lateinit var notificationManager: TelecomCallNotificationManager
    private lateinit var telecomRepository: TelecomCallRepository

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob())
    private var audioJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = TelecomCallNotificationManager(applicationContext)
        telecomRepository =
            TelecomCallRepository.instance ?: TelecomCallRepository.create(applicationContext)

        telecomRepository.currentCall
            .onEach { call ->
                updateServiceState(call)
            }
            .onCompletion {
                stopSelf()
            }
            .launchIn(scope)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        notificationManager.updateCallNotification(TelecomCallData.None)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        }

        when (intent.action) {
            ACTION_INCOMING_CALL -> registerCall(intent = intent, incoming = true)
            ACTION_OUTGOING_CALL -> registerCall(intent = intent, incoming = false)
            ACTION_UPDATE_CALL -> updateServiceState(telecomRepository.currentCall.value)

            else -> throw IllegalArgumentException("Unknown action")
        }

        return START_STICKY
    }

    private fun registerCall(intent: Intent, incoming: Boolean) {
        if (telecomRepository.currentCall.value is TelecomCallData.Registered) {
            return
        }

        val name = intent.getStringExtra(EXTRA_NAME)!!
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_URI, Uri::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_URI)!!
        }

        scope.launch {
            if (incoming) {
                delay(2000)
            }

            launch {
                telecomRepository.registerCall(name, uri, incoming)
            }

            if (!incoming) {
                delay(2000)
                (telecomRepository.currentCall.value as? TelecomCallData.Registered)?.processAction(
                    TelecomCallAction.Activate,
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateServiceState(call: TelecomCallData) {
        notificationManager.updateCallNotification(call)

        when (call) {
            is TelecomCallData.None -> {
                audioJob?.cancel()
            }

            is TelecomCallData.Registered -> {
                if (call.isActive && !call.isOnHold && !call.isMuted && hasMicPermission()) {
                    if (audioJob == null || audioJob?.isActive == false) {
                        audioJob = scope.launch {
                            AudioLoopSource.openAudioLoop()
                        }
                    }
                } else {
                    audioJob?.cancel()
                }
            }

            is TelecomCallData.Unregistered -> {
                stopSelf()
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun hasMicPermission() =
        PermissionChecker.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PermissionChecker.PERMISSION_GRANTED

}
