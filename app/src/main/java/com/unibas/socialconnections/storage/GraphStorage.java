package com.unibas.socialconnections.storage;

import android.os.AsyncTask;
import androidx.room.Room;
import android.content.Context;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import connections.GraphUtil;
import connections.Node;

public class GraphStorage {

    private static final String KEY_ALGORITHM = "Ed25519";
    private final NodeDao nodeDao;
    private GraphUtil graphUtil;

    public GraphStorage(Context context) {
        AppDatabase db = Room.databaseBuilder(
                context.getApplicationContext(), AppDatabase.class, "graph-database").build();
        this.nodeDao = db.nodeDao();
    }

    public void setGraphUtil(GraphUtil graphUtil) {
        this.graphUtil = graphUtil;
    }

    // ---------- Conversion helpers ----------

    private static String encodeKey(PublicKey pub) {
        return Base64.getEncoder().encodeToString(pub.getEncoded());
    }

    private static PublicKey decodeKey(String base64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
        KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM, "BC");
        return factory.generatePublic(spec);
    }

    // ---------- Loading from disk into GraphUtil (on startup) ----------

    public void loadGraphFromDatabase(Runnable onComplete) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                List<NodeEntity> allNodes = nodeDao.getAllNodes();

                // First pass: recreate every Node and put it in nodeList
                for (NodeEntity entity : allNodes) {
                    try {
                        PublicKey pub = decodeKey(entity.publicKeyBase64);

                        // Skip re-adding the user's own node — that one is created
                        // in GraphUtil's constructor from the Keystore-backed key pair
                        if (graphUtil.nodeList.containsKey(pub)) continue;

                        Node node = new Node(pub, entity.name);
                        node.isFriend = entity.isFriend;
                        node.endpointId = entity.endpointId;
                        graphUtil.nodeList.put(pub, node);
                    } catch (Exception e) {
                        e.printStackTrace(); // consider proper logging
                    }
                }

                // Second pass: reconnect friendships now that all nodes exist
                for (NodeEntity entity : allNodes) {
                    try {
                        PublicKey ownerKey = decodeKey(entity.publicKeyBase64);
                        List<NodeEntity> friends = nodeDao.getFriendsOf(entity.publicKeyBase64);
                        Node ownerNode = graphUtil.nodeList.get(ownerKey);

                        for (NodeEntity friendEntity : friends) {
                            PublicKey friendKey = decodeKey(friendEntity.publicKeyBase64);
                            Node friendNode = graphUtil.nodeList.get(friendKey);
                            if (ownerNode != null && friendNode != null) {
                                ownerNode.addFriend(friendNode);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                if (onComplete != null) onComplete.run();
            }
        }.execute();
    }

    // ---------- Saving changes to disk ----------

    public void saveNode(Node node) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                NodeEntity entity = new NodeEntity(
                        encodeKey(node.publicKey),
                        node.name,
                        node.isFriend,
                        node.isUser,
                        node.endpointId
                );
                nodeDao.insertNode(entity);
                return null;
            }
        }.execute();
    }

    public void saveFriendship(Node owner, Node friend) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                // Make sure both nodes exist in the DB first
                nodeDao.insertNode(new NodeEntity(
                        encodeKey(owner.publicKey), owner.name, owner.isFriend, owner.isUser, owner.endpointId));
                nodeDao.insertNode(new NodeEntity(
                        encodeKey(friend.publicKey), friend.name, friend.isFriend, friend.isUser, friend.endpointId));

                nodeDao.insertFriendship(new FriendshipEntity(
                        encodeKey(owner.publicKey),
                        encodeKey(friend.publicKey)
                ));
                return null;
            }
        }.execute();
    }

    public interface UsernameCallback {
        void onResult(String username); // null if not found
    }

    public void getStoredUsername(UsernameCallback callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                NodeEntity entity = nodeDao.getUserNodeEntity();
                return (entity != null) ? entity.name : null;
            }

            @Override
            protected void onPostExecute(String username) {
                callback.onResult(username);
            }
        }.execute();
    }
}