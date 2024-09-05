package com.telecom.app.application.service

import android.telecom.Call
import android.telecom.CallScreeningService

class MyCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setSkipNotification(true)
            .build()

        respondToCall(callDetails, response)
    }
}
