package com.unibas.socialconnections.transmission

import computer.iroh.Connection
import computer.iroh.Endpoint
import computer.iroh.EndpointAddr
import computer.iroh.EndpointId
import computer.iroh.EndpointOptions
import computer.iroh.IrohException
import java.security.PublicKey

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IrohManager() {

    private var endpoint: Endpoint? = null
    private var connection: Connection? = null

    private val alpn = "social-connections/1".toByteArray()

    /**
     * Creates and binds the local iroh endpoint.
     */
    suspend fun startInternal() {
        endpoint = Endpoint.bind(
            EndpointOptions(
                alpns = listOf(alpn)
            )
        )
    }

    /**
     * Connects to another peer using an iroh ticket.
     */
    suspend fun connectInternal(ticket: String) {
        val ep = endpoint
            ?: throw IllegalStateException("Endpoint not started")

        val peerId = EndpointId.fromString(ticket)

        val addr = EndpointAddr(
            peerId,
            "https://euw1-1.relay.iroh.network/",
            emptyList()
        )

        connection = ep.connect(
            addr,
            alpn
        )
    }

    fun getEndpointId(): String {
        val ep = endpoint
            ?: throw IllegalStateException("Endpoint not started")

        return ep.id().toString()
    }

    /**
     * Sends application data.
     */
    fun send(message: ByteArray) {
        val conn = connection
            ?: throw IllegalStateException("Not connected")

        conn.sendDatagram(message)
    }

    /**
     * Receives Data from connection
     */
    suspend fun receive(): ByteArray {
        val conn = connection
            ?: throw IllegalStateException("Not Connected")

        val message = conn.readDatagram()
        return message;
    }

    /**
     * Closes the connection and endpoint.
     */
    fun disconnect() {

        connection?.close()
        connection = null

        endpoint?.close()
        endpoint = null
    }

    /*----------------------- Java Friendly calls -----------------------*/

    fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            startInternal()
        }
    }

    fun connect(ticket:String){
        CoroutineScope(Dispatchers.IO).launch {
            connectInternal(ticket)
        }
    }
}