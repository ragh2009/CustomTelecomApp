package com.telecom.app.application.service

import android.telecom.Connection
import android.telecom.ConnectionService
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class TestConnectionService(private val connection: Connection) : ConnectionService() {

    fun onCallReceived() {
        connection.setRinging()
    }

    fun onCallAnswered() {
        connection.setActive()
    }

    @Test
    fun testIncomingCallInConnectionService() {
        val mockConnection: Connection = mock(Connection::class.java)

        val connectionService = TestConnectionService(mockConnection)
        connectionService.onCallReceived()

        verify(mockConnection).setRinging()

        connectionService.onCallAnswered()
        verify(mockConnection).setActive()
    }
}