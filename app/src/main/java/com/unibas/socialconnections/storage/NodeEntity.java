package com.unibas.socialconnections.storage;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import computer.iroh.EndpointId;

@Entity(tableName = "nodes")
public class NodeEntity {
    @PrimaryKey
    @NonNull
    public String publicKeyBase64;
    public String name;
    public String endpointId;
    public boolean isFriend;
    public boolean isUser;

    public NodeEntity(@NonNull String publicKeyBase64, String name, boolean isFriend, boolean isUser, String endpointId) {
        this.publicKeyBase64 = publicKeyBase64;
        this.name = name;
        this.isFriend = isFriend;
        this.isUser = isUser;
        this.endpointId = endpointId;
    }
}