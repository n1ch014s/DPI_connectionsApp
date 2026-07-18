package com.unibas.socialconnections.transmission;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Gossip {
    private final IrohManager transport;

    // Messages we've already processed
    private final Set<UUID> seenMessages = ConcurrentHashMap.newKeySet();

    public Gossip(IrohManager transport) {
        this.transport = transport;
    }

    /**
     * Publish a brand-new message.
     * @param payload the message to be sent
     */
    public void publish(byte[] payload) {

        Packet packet = new Packet(UUID.randomUUID(), MessageType.UPDATE, payload);

        transport.broadcast(packet.toBytes());
    }


    /**
     * Called whenever iroh receives a datagram.
     * DEPRECATED at least i think...
     */
    /*
    private void onPacketReceived(byte[] bytes) {

        Packet packet = Packet.fromBytes(bytes);

        // Ignore duplicates
        if (!seenMessages.add(packet.getUUID())) {
            return;
        }

        // Deliver to app
        if (listener != null) {
            listener.onMessage(packet.getPayload());
        }

        // Forward onwards
        transport.broadcast(packet.toBytes());
    } */


}
