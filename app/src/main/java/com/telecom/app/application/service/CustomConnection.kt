
package com.telecom.app.application.service

import android.telecom.Connection
import android.telecom.DisconnectCause
import android.util.Log

class CustomConnection : Connection() {

    override fun onAnswer() {
        Log.d("CustomConnection", "Call answered")
        setActive()
    }

    override fun onDisconnect() {
        Log.d("CustomConnection", "Call disconnected")
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }

    override fun onReject() {
        Log.d("CustomConnection", "Call rejected")
        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        destroy()
    }
}
