package com.unibas.socialconnections.storage;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNode(NodeEntity node);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFriendship(FriendshipEntity friendship);

    @Query("SELECT * FROM nodes")
    List<NodeEntity> getAllNodes();

    @Query("SELECT n.* FROM nodes n " +
            "INNER JOIN friendships f ON n.publicKeyBase64 = f.friendKey " +
            "WHERE f.nodeKey = :ownerKey")
    List<NodeEntity> getFriendsOf(String ownerKey);

    @Query("SELECT * FROM nodes WHERE publicKeyBase64 = :key LIMIT 1")
    NodeEntity getNode(String key);

    @Query("SELECT * FROM nodes WHERE isUser = 1 LIMIT 1") // NEW
    NodeEntity getUserNodeEntity();
}