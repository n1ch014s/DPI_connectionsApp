package com.unibas.socialconnections.transmission

import computer.iroh.Connection
import computer.iroh.Endpoint
import computer.iroh.EndpointAddr
import computer.iroh.EndpointId
import computer.iroh.EndpointOptions
import computer.iroh.Incoming
import connections.GraphUtil

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.PublicKey
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.milliseconds

class IrohManager() {

    private var endpoint: Endpoint? = null
    private val connections = ConcurrentHashMap<EndpointId, Connection>()
    private val alreadyReceiving = ConcurrentHashMap.newKeySet<EndpointId>() // to see if a coroutine has already been started for this connection
    private val alpn = "social-connections/1".toByteArray()
    private lateinit var graph: GraphUtil


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
    private suspend fun connectInternal(ticket: String) {

        val ep = endpoint
            ?: throw IllegalStateException("Endpoint not started")


        val peerId = EndpointId.fromString(ticket)


        val addr = EndpointAddr(
            peerId,
            "https://euw1-1.relay.iroh.network/",
            emptyList()
        )

        val connection = ep.connect(
            addr,
            alpn
        )

        connections[peerId] = connection
    }

    /**
     * Accept the next connection from the peer.
     */
    private suspend fun acceptInternal(){
        val ep = endpoint
            ?: throw IllegalStateException("Endpoint not started")

        val incoming = ep.acceptNext()
            ?: throw IllegalStateException("No incoming connection")

        val accepting = incoming.accept()
        val connection = accepting.connect()
        val peerId = connection.remoteId()

        connections[peerId] = connection
    }


    fun getEndpointId(): String {
        val ep = endpoint
            ?: throw IllegalStateException("Endpoint not started")

        return ep.id().toString()
    }

    /**
     * Sends application data to a single peer (i.e. not in friends list)
     */
    fun send(peer : EndpointId , message: ByteArray) {
        val conn = connections[peer]
            ?: throw IllegalStateException("Not connected")

        conn.sendDatagram(message)
    }

    /**
     * Send variation with the peerId as a string instead of direct EndpointID because i could NOT be bothered to redo that
     */
    fun send(ticket : String, message: ByteArray) {
        val peer = EndpointId.fromString(ticket)
        val conn = connections[peer]
            ?: throw IllegalStateException("Not connected")

        conn.sendDatagram(message)
    }

    /**
     * Sends application data to ALL peers in friends list
     */
    fun broadcast(message : ByteArray) {
        connections.forEach { (peerId, connection) ->
            if(peerId in getFriendIds()) {
                try {
                    connection.sendDatagram(message)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }


    /**
     * Listens for incoming data
     */
    fun startReceiving(listener : MessageListener) {
        CoroutineScope(Dispatchers.IO).launch {
            while (true){
                connections.forEach() { (peerId, connection) ->
                    if(alreadyReceiving.add(peerId)) {
                        launch {
                            try {
                                while (true) {
                                    val message = connection.readDatagram()
                                    listener.onMessage(connection.remoteId(), message)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                disconnect(peerId)
                            }
                        }
                    }
                }
                kotlinx.coroutines.delay(50.milliseconds)
            }
        }
    }


    /**
     * Closes the connection and endpoint.
     */
    fun disconnect() {
        connections.values.forEach { connection ->
            connection.close()
        }
        connections.clear()
        alreadyReceiving.clear()

        endpoint?.close()
        endpoint = null
    }

    /**
     * Closes the connection to a specific peer.
     */
    fun disconnect(peer: EndpointId) {
        connections.remove(peer)?.close()
        alreadyReceiving.remove(peer)
    }


    fun getFriendIds() : List<EndpointId>{
        return graph.getFriendsListString()
            .map { publicKey -> EndpointId.fromString(publicKey) }
    }

    /*----------------------- Java Friendly calls -----------------------*/

    fun start(graphUtil : GraphUtil) {
        graph = graphUtil
        CoroutineScope(Dispatchers.IO).launch {
            startInternal()
        }
    }

    fun connect(ticket:String){
        CoroutineScope(Dispatchers.IO).launch {
            connectInternal(ticket)
        }
    }

    fun accept(){
        CoroutineScope(Dispatchers.IO).launch {
            acceptInternal()
        }
    }

}