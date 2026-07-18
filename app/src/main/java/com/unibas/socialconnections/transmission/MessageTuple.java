package com.unibas.socialconnections.transmission;

import java.security.PublicKey;

public class MessageTuple {
    private PublicKey sender;
    private byte[] message;

    public MessageTuple(PublicKey sender, byte[] message){
        this.sender = sender;
        this.message = message;
    }

    public PublicKey getSender(){
        return sender;
    }

    public byte[] getMessage(){
        return message;
    }
}
