
package com.telecom.app.application.service

import android.telecom.Connection
import android.telecom.ConnectionService
import android.telecom.ConnectionRequest
import android.telecom.PhoneAccountHandle
import android.util.Log

class TelephonyService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d("TelephonyService", "Incoming call received")
        val connection = CustomConnection()
        connection.setRinging()
        return connection
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d("TelephonyService", "Outgoing call initiated")
        val connection = CustomConnection()
        connection.setDialing()
        return connection
    }
}
