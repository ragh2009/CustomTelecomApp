package com.telecom.app

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.util.UUID

class CallManager {
    @Test
    fun testInitiateCallUsingTelecomManager() {
        val telecomManager: TelecomManager = mock(TelecomManager::class.java)
        val phoneNumber = Uri.parse("tel:+918527561611")
        verify(telecomManager).placeCall(eq(phoneNumber), any(Bundle::class.java))
    }

    @Test
    fun testPhoneAccountRegistration() {
        val telecomManager: TelecomManager = mock(TelecomManager::class.java)
        val phoneAccount = PhoneAccount.builder(
            PhoneAccountHandle(ComponentName("com.telecom.app", "TelephonyService"), UUID.randomUUID().toString()),
            "My Telecom App"
        ).build()

        verify(telecomManager).registerPhoneAccount(phoneAccount)
    }
}