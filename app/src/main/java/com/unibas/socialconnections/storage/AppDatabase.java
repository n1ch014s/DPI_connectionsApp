package com.unibas.socialconnections.storage;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {NodeEntity.class, FriendshipEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract NodeDao nodeDao();
}
