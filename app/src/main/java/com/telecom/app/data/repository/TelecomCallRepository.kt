package com.telecom.app.data.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import android.telecom.DisconnectCause
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import com.telecom.app.data.model.TelecomCallData
import com.telecom.app.domain.usecase.TelecomCallAction
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class TelecomCallRepository(private val callsManager: CallsManager) {

    companion object {
        var instance: TelecomCallRepository? = null
            private set

        fun create(context: Context): TelecomCallRepository {
            check(instance == null) {
                "CallRepository instance already created"
            }

            val callsManager = CallsManager(context).apply {
                registerAppWithTelecom(
                    capabilities = CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING and
                            CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING,
                )
            }

            return TelecomCallRepository(
                callsManager = callsManager,
            ).also {
                instance = it
            }
        }
    }

    // Keeps track of the current state
    private val _currentCall: MutableStateFlow<TelecomCallData> = MutableStateFlow(TelecomCallData.None)
    val currentCall = _currentCall.asStateFlow()

    suspend fun registerCall(displayName: String, address: Uri, isIncoming: Boolean) {
        check(_currentCall.value !is TelecomCallData.Registered) {
            "There cannot be more than one call at the same time."
        }

        val attributes = CallAttributesCompat(
            displayName = displayName,
            address = address,
            direction = if (isIncoming) {
                CallAttributesCompat.DIRECTION_INCOMING
            } else {
                CallAttributesCompat.DIRECTION_OUTGOING
            },
            callType = CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
            callCapabilities = (CallAttributesCompat.SUPPORTS_SET_INACTIVE
                    or CallAttributesCompat.SUPPORTS_STREAM
                    or CallAttributesCompat.SUPPORTS_TRANSFER),
        )

        val actionSource = Channel<TelecomCallAction>()
        try {
            callsManager.addCall(
                attributes,
                onIsCallAnswered,
                onIsCallDisconnected,
                onIsCallActive,
                onIsCallInactive
            ) {
                launch {
                    processCallActions(actionSource.consumeAsFlow())
                }

                _currentCall.value = TelecomCallData.Registered(
                    id = getCallId(),
                    isActive = false,
                    isOnHold = false,
                    callAttributes = attributes,
                    isMuted = false,
                    errorCode = null,
                    currentCallEndpoint = null,
                    availableCallEndpoints = emptyList(),
                    actionSource = actionSource,
                )

                launch {
                    currentCallEndpoint.collect {
                        updateCurrentCall {
                            copy(currentCallEndpoint = it)
                        }
                    }
                }
                launch {
                    availableEndpoints.collect {
                        updateCurrentCall {
                            copy(availableCallEndpoints = it)
                        }
                    }
                }
                launch {
                    isMuted.collect {
                        updateCurrentCall {
                            copy(isMuted = it)
                        }
                    }
                }
            }
        } finally {
            _currentCall.value = TelecomCallData.None
        }
    }

    private suspend fun CallControlScope.processCallActions(actionSource: Flow<TelecomCallAction>) {
        actionSource.collect { action ->
            when (action) {
                is TelecomCallAction.Answer -> {
                    doAnswer()
                }

                is TelecomCallAction.Disconnect -> {
                    doDisconnect(action)
                }

                is TelecomCallAction.SwitchAudioEndpoint -> {
                    doSwitchEndpoint(action)
                }

                is TelecomCallAction.TransferCall -> {
                    val call = _currentCall.value as? TelecomCallData.Registered
                    val endpoints = call?.availableCallEndpoints?.firstOrNull {
                        it.identifier == action.endpointId
                    }
                    requestEndpointChange(
                        endpoint = endpoints ?: return@collect,
                    )
                }

                TelecomCallAction.Hold -> {
                    when (val result = setInactive()) {
                        is CallControlResult.Success -> {
                            onIsCallInactive()
                        }

                        is CallControlResult.Error -> {
                            updateCurrentCall {
                                copy(errorCode = result.errorCode)
                            }
                        }
                    }
                }

                TelecomCallAction.Activate -> {
                    when (val result = setActive()) {
                        is CallControlResult.Success -> {
                            onIsCallActive()
                        }

                        is CallControlResult.Error -> {
                            updateCurrentCall {
                                copy(errorCode = result.errorCode)
                            }
                        }
                    }
                }

                is TelecomCallAction.ToggleMute -> {
                    updateCurrentCall {
                        copy(isMuted = !isMuted)
                    }
                }
            }
        }
    }

    private fun updateCurrentCall(transform: TelecomCallData.Registered.() -> TelecomCallData) {
        _currentCall.update { call ->
            if (call is TelecomCallData.Registered) {
                call.transform()
            } else {
                call
            }
        }
    }

    private suspend fun CallControlScope.doSwitchEndpoint(action: TelecomCallAction.SwitchAudioEndpoint) {
        val endpoints = (_currentCall.value as TelecomCallData.Registered).availableCallEndpoints
        val newEndpoint = endpoints.firstOrNull { it.identifier == action.endpointId }

        if (newEndpoint != null) {
            requestEndpointChange(newEndpoint).also {
                Log.d("RR", "Endpoint ${newEndpoint.name} changed: $it")
            }
        }
    }

    private suspend fun CallControlScope.doDisconnect(action: TelecomCallAction.Disconnect) {
        disconnect(action.cause)
        onIsCallDisconnected(action.cause)
    }

    private suspend fun CallControlScope.doAnswer() {
        when (answer(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)) {
            is CallControlResult.Success -> {
                onIsCallAnswered(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)
            }

            is CallControlResult.Error -> {
                updateCurrentCall {
                    TelecomCallData.Unregistered(
                        id = id,
                        callAttributes = callAttributes,
                        disconnectCause = DisconnectCause(DisconnectCause.BUSY),
                    )
                }
            }
        }
    }

    val onIsCallAnswered: suspend(type: Int) -> Unit = {
        updateCurrentCall {
            copy(isActive = true, isOnHold = false)
        }
    }

    val onIsCallDisconnected: suspend (cause: DisconnectCause) -> Unit = {
        updateCurrentCall {
            TelecomCallData.Unregistered(id, callAttributes, it)
        }
    }

    val onIsCallActive: suspend () -> Unit = {
        updateCurrentCall {
            copy(
                errorCode = null,
                isActive = true,
                isOnHold = false,
            )
        }
    }

    val onIsCallInactive: suspend () -> Unit = {
        updateCurrentCall {
            copy(
                errorCode = null,
                isOnHold = true)
        }
    }
}