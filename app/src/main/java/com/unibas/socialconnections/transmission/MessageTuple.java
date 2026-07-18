package com.unibas.socialconnections.transmission;

import java.security.PublicKey;

import computer.iroh.EndpointId;

public class MessageTuple {
    private String sender;
    private byte[] message;

    public MessageTuple(String sender, byte[] message){
        this.sender = sender;
        this.message = message;
    }

    public String getSender(){
        return sender;
    }

    public byte[] getMessage(){
        return message;
    }
}
