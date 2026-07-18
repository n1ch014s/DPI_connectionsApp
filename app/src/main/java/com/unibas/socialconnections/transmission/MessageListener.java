package com.unibas.socialconnections.transmission;

import computer.iroh.Endpoint;
import computer.iroh.EndpointId;

public interface MessageListener {
    void onMessage(EndpointId peer, byte[] payload);
}
