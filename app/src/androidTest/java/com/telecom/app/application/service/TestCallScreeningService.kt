package com.telecom.app.application.service

import android.net.Uri
import android.telecom.Call
import android.telecom.CallScreeningService
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class TestCallScreeningService : CallScreeningService() {
    private var blocked = false

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.handle == Uri.parse("tel:+918527561611")) {
            respondToCall(callDetails, CallResponse.Builder().setDisallowCall(true).build())
            blocked = true
        }
    }

    @Test
    fun testScreeningOfBlockedNumber(): Unit {
        val callDetails: Call.Details = mock(Call.Details::class.java)
        val phoneNumber = Uri.parse("tel:+918527561611")

        `when`(callDetails.handle).thenReturn(phoneNumber)

        val callScreeningService = TestCallScreeningService()
        callScreeningService.onScreenCall(callDetails)

        assertTrue(callScreeningService.isCallBlocked())
    }

    fun isCallBlocked(): Boolean = blocked
}
