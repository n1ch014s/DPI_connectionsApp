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

    // Fixed, shared identities — SAME on both test phones.

    // Bridge friends that BOTH phones use as their OWN direct friend.
    // Mutual friends attached under these will be reached via a common bridge on both sides.
    private static final String[] SHARED_BRIDGE_FRIENDS = {
            "MCowBQYDK2VwAyEA+O0NrBZUBGYFgt8pLTr6Qq2Hpab8ssqQEzp/BGrZ1D8=",
            "MCowBQYDK2VwAyEA3hHHHZkqw/7fA5FWGjgP1Paar4ds1E7eDySuYrhJk4g="
    };

    // Mutual friends reached via a SHARED bridge (index-aligned with SHARED_BRIDGE_FRIENDS above)
    private static final String[] MUTUAL_VIA_SHARED_BRIDGE = {
            "MCowBQYDK2VwAyEAhntuScfeisNGQYCzMPRA6VqHv2YNoDakLrYH8UjkCxI=",
            "MCowBQYDK2VwAyEArX14UKr7tvdR1BHUDGxYXKEMuvRluLW8ABnztAanZzc="
    };

    // Mutual friends reached via a DIFFERENT bridge on each phone (bridges are NOT shared,
    // only the mutual friend itself is shared) — tests that overlap works even when the
    // intermediate hop differs between the two sides.
    private static final String[] MUTUAL_VIA_INDEPENDENT_BRIDGE = {
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
     * Seeds mutual friends at distance 2 from the user, via a mix of shared and
     * phone-specific bridge friends.
     */
    public static void seedMutualFriendsAtDistanceTwo(GraphUtil graph, GraphStorage storage) throws Exception {
        // --- Group 1: mutual friends reached via a bridge SHARED with the other phone ---
        /*for (int i = 0; i < SHARED_BRIDGE_FRIENDS.length; i++) {
            PublicKey bridgeKey = decodeFixedKey(SHARED_BRIDGE_FRIENDS[i]);
            PublicKey mutualKey = decodeFixedKey(MUTUAL_VIA_SHARED_BRIDGE[i]);

            // bridge becomes a direct friend of the user
            graph.addFriendToUser(bridgeKey, "Bridge-Shared-" + i);
            Node bridgeNode = graph.nodeList.get(bridgeKey);
            storage.saveNode(bridgeNode);
            storage.saveFriendship(graph.getUserNode(), bridgeNode);

            // mutual becomes a friend-of-friend, via that bridge
            graph.addFriendToFriend(bridgeNode, mutualKey);
            Node mutualNode = graph.nodeList.get(mutualKey);
            mutualNode.name = "Mutual-SharedBridge-" + i;
            storage.saveNode(mutualNode);
            storage.saveFriendship(bridgeNode, mutualNode);
        }*/

        // --- Group 2: mutual friends reached via a bridge that's DIFFERENT per phone ---
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519", "BC");
        for (int i = 0; i < MUTUAL_VIA_INDEPENDENT_BRIDGE.length; i++) {
            // this bridge is randomly generated PER PHONE — not shared
            KeyPair ownBridge = kpg.generateKeyPair();
            PublicKey bridgeKey = ownBridge.getPublic();
            PublicKey mutualKey = decodeFixedKey(MUTUAL_VIA_INDEPENDENT_BRIDGE[i]);

            graph.addFriendToUser(bridgeKey, "Bridge-Own-" + i);
            Node bridgeNode = graph.nodeList.get(bridgeKey);
            storage.saveNode(bridgeNode);
            storage.saveFriendship(graph.getUserNode(), bridgeNode);

            graph.addFriendToFriend(bridgeNode, mutualKey);
            Node mutualNode = graph.nodeList.get(mutualKey);
            mutualNode.name = "Mutual-OwnBridge-" + i;
            storage.saveNode(mutualNode);
            storage.saveFriendship(bridgeNode, mutualNode);
        }
    }
}