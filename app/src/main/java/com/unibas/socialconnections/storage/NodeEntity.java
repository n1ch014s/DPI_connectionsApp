package com.unibas.socialconnections.storage;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "nodes")
public class NodeEntity {
    @PrimaryKey
    @NonNull
    public String publicKeyBase64;
    public String name;
    public boolean isFriend;
    public boolean isUser;

    public NodeEntity(@NonNull String publicKeyBase64, String name, boolean isFriend, boolean isUser) {
        this.publicKeyBase64 = publicKeyBase64;
        this.name = name;
        this.isFriend = isFriend;
        this.isUser = isUser;
    }
}