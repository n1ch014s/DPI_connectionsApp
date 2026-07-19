package com.unibas.socialconnections;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import com.unibas.socialconnections.storage.GraphStorage;

import connections.GraphUtil;
import connections.Node;

public class DebugGraphSeeder {

    // Fixed, shared "mutual friend" identities — SAME on both test phones.
    // Generated once via KeyPairGenerator, pasted here as constants.
    private static final String[] SHARED_MUTUAL_FRIENDS = {
            "MCowBQYDK2VwAyEAhntuScfeisNGQYCzMPRA6VqHv2YNoDakLrYH8UjkCxI=",
            "MCowBQYDK2VwAyEArX14UKr7tvdR1BHUDGxYXKEMuvRluLW8ABnztAanZzc=",
            "MCowBQYDK2VwAyEAObmEkRIaqi08R9F812WVHvlgrKLD6Wbf0JkhK8FDLE4=",
            "MCowBQYDK2VwAyEAj27Sbuw0mzX3QOPQvAkfy19evuqiIx6OTPmIrFPeSto="
    };

    private static PublicKey decodeFixedKey(String base64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
        KeyFactory kf = KeyFactory.getInstance("Ed25519", "BC");
        return kf.generatePublic(spec);
    }

    /**
     * Adds the shared mutual-friend identities as DIRECT friends of the user.
     * Since both phones add the SAME public keys as their own direct friends,
     * this creates real overlap: both users are "1 step" from these shared nodes,
     * so when they later run a real NFC exchange as non-friends, getMinPaths
     * finds multiple distance-2 paths through these shared mutual friends.
     */
    public static void seedSharedMutualFriends(GraphUtil graph, GraphStorage storage) throws Exception {
        int i = 0;
        for (String encoded : SHARED_MUTUAL_FRIENDS) {
            PublicKey sharedKey = decodeFixedKey(encoded);
            graph.addFriendToUser(sharedKey, "Mutual-" + i);
            Node node = graph.nodeList.get(sharedKey);
            storage.saveNode(node);
            storage.saveFriendship(graph.getUserNode(), node);
            i++;
        }
    }

    /**
     * Adds extra random, non-shared friends-of-friends for visual bulk —
     * these won't appear in the final minimal-path graph (since they don't
     * overlap with the other phone's network), but they make each phone's
     * own local friend list look more populated/realistic during testing.
     */
    public static void seedRandomFriendsOfFriends(GraphUtil graph, GraphStorage storage,
                                                  PublicKey existingFriendKey, int count) throws Exception {
        Node friendNode = graph.nodeList.get(existingFriendKey);
        if (friendNode == null || !friendNode.isFriend) {
            throw new IllegalArgumentException("Given key is not a direct friend in this graph");
        }

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519", "BC");
        for (int i = 0; i < count; i++) {
            PublicKey fakePub = kpg.generateKeyPair().getPublic();
            graph.addFriendToFriend(friendNode, fakePub);
            Node fakeNode = graph.nodeList.get(fakePub);
            fakeNode.name = "Extra-" + i;
            storage.saveNode(fakeNode);
            storage.saveFriendship(friendNode, fakeNode);
        }
    }
}