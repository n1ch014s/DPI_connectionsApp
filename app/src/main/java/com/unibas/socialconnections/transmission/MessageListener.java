package com.unibas.socialconnections.transmission;

import computer.iroh.Endpoint;
import computer.iroh.EndpointId;
import connections.Node;

public interface MessageListener {
    void onMessage(EndpointId peer, byte[] payload);
}
