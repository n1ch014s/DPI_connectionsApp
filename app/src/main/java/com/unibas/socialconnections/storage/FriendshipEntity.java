package com.unibas.socialconnections.storage;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "friendships", primaryKeys = {"nodeKey", "friendKey"})
public class FriendshipEntity {
    @NonNull
    public String nodeKey;   // owner node's public key (base64)

    @NonNull
    public String friendKey; // friend's public key (base64)

    public FriendshipEntity(@NonNull String nodeKey, @NonNull String friendKey) {
        this.nodeKey = nodeKey;
        this.friendKey = friendKey;
    }
}