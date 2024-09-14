package com.telecom.app.application.service

import android.telecom.Connection
import android.telecom.ConnectionService
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class ConnectionServiceTest {

    private lateinit var connectionService: TestConnectionService
    private lateinit var mockConnection: Connection

    @Before
    fun setUp() {
        mockConnection = mock(Connection::class.java)
        connectionService = TestConnectionService(mockConnection)
    }

    @Test
    fun testIncomingCallInConnectionService() {
        // Simulate an incoming call
        connectionService.onCallReceived()

        // Verify that the connection is set to ringing state
        verify(mockConnection).setRinging()

        // Simulate answering the call
        connectionService.onCallAnswered()

        // Verify that the connection is set to active state
        verify(mockConnection).setActive()
    }

    // Inner class for testing purposes
    class TestConnectionService(private val connection: Connection) : ConnectionService() {
        fun onCallReceived() {
            connection.setRinging()
        }

        fun onCallAnswered() {
            connection.setActive()
        }
    }
}
