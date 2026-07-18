package com.unibas.socialconnections.transmission;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class Packet {

    private final UUID id;
    private final byte[] payload;
    private final MessageType messageType;


    public Packet(UUID id, MessageType messageType, byte[] payload){
        this.id = id;
        this.messageType = messageType;
        this.payload = payload;
    }

    public byte[] toBytes() {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (DataOutputStream out = new DataOutputStream(bos)) {

            out.writeLong(id.getMostSignificantBits());
            out.writeLong(id.getLeastSignificantBits());

            out.write(messageType.ordinal());

            out.writeInt(payload.length);
            out.write(payload);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return bos.toByteArray();
    }

    public static Packet fromBytes(byte[] bytes) {
        try (DataInputStream in =
                     new DataInputStream(new ByteArrayInputStream(bytes))) {

            UUID id = new UUID(in.readLong(), in.readLong());

            int typeOrdinal = in.readUnsignedByte();
            MessageType messageType = MessageType.values()[typeOrdinal];

            int length = in.readInt();

            byte[] payload = new byte[length];
            in.readFully(payload);

            return new Packet(id, messageType, payload);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public UUID getUUID(){
        return id;
    }
    public MessageType getMessageType() {
        return messageType;
    }

    public byte[] getPayload(){
        return payload;
    }
}
