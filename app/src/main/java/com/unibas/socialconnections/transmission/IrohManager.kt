package com.unibas.socialconnections.transmission

import android.text.style.TabStopSpan
import android.util.Log
import computer.iroh.Connection
import computer.iroh.Endpoint
import computer.iroh.EndpointAddr
import computer.iroh.EndpointId
import computer.iroh.EndpointOptions
import computer.iroh.Incoming
import connections.GraphUtil
import connections.Node

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.security.PublicKey
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.milliseconds

class IrohManager() {

    private var endpoint: Endpoint? = null
    private val connections = ConcurrentHashMap<EndpointId, Connection>()
    private val pendingConnection = ConcurrentHashMap<EndpointId, ConnectionState>()
    private val alreadyReceiving = ConcurrentHashMap.newKeySet<EndpointId>() // to see if a coroutine has already been started for this connection
    private val alpn = "social-connections/1".toByteArray()
    private lateinit var graph: GraphUtil
    private var endpointReady = false

    private enum class ConnectionState{
        DISCONNECTED,
        CONNECTED,
        PENDING
    }

    /**
     * Creates and binds the local iroh endpoint.
     */
    suspend fun startInternal(privKey : ByteArray) {
        endpoint = Endpoint.bind(
            EndpointOptions(
                alpns = listOf(alpn)
                , secretKey = privKey
            )
        )
        endpointReady = true
        Log.d("Iroh", "Endpoint created: $EndpointId");

    }

    /**
     * Connects to another peer using an iroh ticket.
     */
    private suspend fun connectInternal(ticket: String) {

        val ep = endpoint
            ?: throw IllegalStateException("Endpoint not started")


        val peerId = EndpointId.fromString(ticket)
        pendingConnection[peerId] = ConnectionState.PENDING
        Log.d("Iroh", "Connecting to Peer: $peerId")


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
        pendingConnection[peerId] = ConnectionState.CONNECTED
        Log.d("iroh", "Connection Established: $connection")

    }

    /**
     * Accept the next connection from the peer.
     */
    private suspend fun acceptInternal(ticket: String){
        val expectedPeerId = EndpointId.fromString(ticket)

        pendingConnection[expectedPeerId] = ConnectionState.PENDING

        val ep = endpoint
            ?: throw IllegalStateException("Endpoint not started")

        Log.d("Iroh", "Endpoint available: ${ep.id()}")

        val incoming = ep.acceptNext()
            ?: throw IllegalStateException("No incoming connection")

        Log.d("Iroh", "incoming: $incoming")

        val accepting = incoming.accept()
        val connection = accepting.connect()
        val peerId = connection.remoteId()

        require(peerId == expectedPeerId) {
            "Accepted connection from unexpected peer: expected $expectedPeerId, got $peerId"
        }

        Log.d("Iroh", "connected: $peerId, $connection")

        connections[peerId] = connection
        pendingConnection[peerId] = ConnectionState.CONNECTED
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
    private suspend fun sendInternal(ticket : String, message: ByteArray) {
        val peer = EndpointId.fromString(ticket)
        Log.d("Iroh", "ConnectionState: "+ pendingConnection[peer])
        while (pendingConnection[peer] == ConnectionState.PENDING || connections[peer] == null){
            delay(10.milliseconds)
        }
        val conn = connections[peer]
            ?: throw IllegalStateException("Not connected")

        conn.sendDatagram(message)
        Log.d("Iroh", "Send Message: ${message.toString(StandardCharsets.UTF_8)}")
    }

    /**
     * Sends application data to ALL peers in friends list
     */
    fun broadcast(message : ByteArray) {
        if(connections.isNotEmpty()) {
            connections.forEach { (peerId, connection) ->
                if (peerId in getFriendIds()) {
                    try {
                        connection.sendDatagram(message)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }


    /**
     * Listens for incoming data
     */
    fun startReceiving(listener : MessageListener) {
        CoroutineScope(Dispatchers.IO).launch {
            while (!endpointReady){
                delay(10.milliseconds)
            }
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
        alreadyReceiving.remove(peer)
        pendingConnection[peer] = ConnectionState.DISCONNECTED
        connections.remove(peer)?.close()


    }


    fun getFriendIds() : List<EndpointId>{
        if (graph.getFriendsListString().isNotEmpty()) {
            val friendStrings = graph.getFriendsList()
            val friendpointIds = friendStrings.map { endpoint -> graph.nodeList[endpoint]?.getEndpointId()?.let { EndpointId.fromString(it) } }
            return friendpointIds as List<EndpointId>

        } else return emptyList();
    }

    /*----------------------- Java Friendly calls -----------------------*/

    fun start(graphUtil : GraphUtil, privKey : ByteArray) {
        graph = graphUtil
        CoroutineScope(Dispatchers.IO).launch {
            startInternal(privKey)
        }
    }

    fun connect(ticket:String){
        if (connections[EndpointId.fromString(ticket)] == null) {
            CoroutineScope(Dispatchers.IO).launch {
                connectInternal(ticket)
            }
        }else{
            Log.d("IROH", "Connection already established!")
        }
    }

    fun accept(ticket : String){
        if (connections[EndpointId.fromString(ticket)] == null) {
            CoroutineScope(Dispatchers.IO).launch {
                acceptInternal(ticket)
            }
        }else{
            Log.d("IROH", "Connection already established!")
        }
    }

    fun send(ticket : String, message: ByteArray){
        CoroutineScope(Dispatchers.IO).launch {
            sendInternal(ticket, message)
        }
    }

}