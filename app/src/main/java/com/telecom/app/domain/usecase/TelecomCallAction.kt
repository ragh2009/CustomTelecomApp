package com.telecom.app.domain.usecase

import android.os.ParcelUuid
import android.os.Parcelable
import android.telecom.DisconnectCause
import kotlinx.parcelize.Parcelize

sealed interface TelecomCallAction : Parcelable {
    @Parcelize
    object Answer : TelecomCallAction

    @Parcelize
    data class Disconnect(val cause: DisconnectCause) : TelecomCallAction

    @Parcelize
    object Hold : TelecomCallAction

    @Parcelize
    object Activate : TelecomCallAction

    @Parcelize
    data class ToggleMute(val isMute: Boolean) : TelecomCallAction

    @Parcelize
    data class SwitchAudioEndpoint(val endpointId: ParcelUuid) : TelecomCallAction

    @Parcelize
    data class TransferCall(val endpointId: ParcelUuid) : TelecomCallAction
}
