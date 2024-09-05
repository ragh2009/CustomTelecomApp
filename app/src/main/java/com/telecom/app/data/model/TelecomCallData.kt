package com.telecom.app.data.model

import android.os.ParcelUuid
import android.telecom.DisconnectCause
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallEndpointCompat
import com.telecom.app.domain.usecase.TelecomCallAction
import kotlinx.coroutines.channels.Channel

sealed class TelecomCallData {
    object None : TelecomCallData()

    data class Registered(
        val id: ParcelUuid,
        val callAttributes: CallAttributesCompat,
        val isActive: Boolean,
        val isOnHold: Boolean,
        val isMuted: Boolean,
        val errorCode: Int?,
        val currentCallEndpoint: CallEndpointCompat?,
        val availableCallEndpoints: List<CallEndpointCompat>,
        internal val actionSource: Channel<TelecomCallAction>,
    ) : TelecomCallData() {

        fun isIncoming() = callAttributes.direction == CallAttributesCompat.DIRECTION_INCOMING

        fun processAction(action: TelecomCallAction) = actionSource.trySend(action).isSuccess
    }

    data class Unregistered(
        val id: ParcelUuid,
        val callAttributes: CallAttributesCompat,
        val disconnectCause: DisconnectCause,
    ) : TelecomCallData()
}
