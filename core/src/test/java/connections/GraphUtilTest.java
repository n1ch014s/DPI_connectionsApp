package connections;

import org.junit.Before;
import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.LinkedList;

import static org.junit.Assert.*;
public class GraphUtilTest {

    private GraphUtil graph;
    private KeyPair userKeys;

    @Before
    public void setUp() throws Exception {
        userKeys = generateTestKeyPair();
        graph = new GraphUtil("TestUser", userKeys.getPublic(), userKeys.getPrivate());
    }

    private static KeyPair generateTestKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        return kpg.generateKeyPair();
    }

    @Test
    public void constructor_createsUserNode() {
        assertNotNull(graph.getUserNode());
        assertEquals("TestUser", graph.getUserNode().name);
        assertTrue(graph.nodeList.containsKey(userKeys.getPublic()));
    }

    @Test
    public void addFriendToUser_addsDirectFriend() throws Exception {
        KeyPair friendKeys = generateTestKeyPair();
        graph.addFriendToUser(friendKeys.getPublic(), "Alice");

        assertEquals(1, graph.getDistance(friendKeys.getPublic()));
        assertTrue(graph.nodeList.containsKey(friendKeys.getPublic()));
    }

    @Test
    public void getDistance_returnsMinusOne_forUnknownKey() throws Exception {
        KeyPair strangerKeys = generateTestKeyPair();
        assertEquals(-1, graph.getDistance(strangerKeys.getPublic()));
    }

    @Test
    public void getDistance_returnsTwo_forFriendOfFriend() throws Exception {
        KeyPair friendKeys = generateTestKeyPair();
        KeyPair fofKeys = generateTestKeyPair();

        graph.addFriendToUser(friendKeys.getPublic(), "Alice");
        Node friendNode = graph.nodeList.get(friendKeys.getPublic());
        graph.addFriendToFriend(friendNode, fofKeys.getPublic());

        assertEquals(2, graph.getDistance(fofKeys.getPublic()));
    }

    @Test
    public void getFriendsList_returnsOnlyDirectFriends() throws Exception {
        KeyPair friendKeys = generateTestKeyPair();
        KeyPair fofKeys = generateTestKeyPair();

        graph.addFriendToUser(friendKeys.getPublic(), "Alice");
        Node friendNode = graph.nodeList.get(friendKeys.getPublic());
        graph.addFriendToFriend(friendNode, fofKeys.getPublic());

        PublicKey[] friends = graph.getFriendsList();
        assertEquals(1, friends.length);
        assertEquals(friendKeys.getPublic(), friends[0]);
    }

    @Test
    public void getList_containsCorrectDistancesForAllNodes() throws Exception {
        KeyPair friendKeys = generateTestKeyPair();
        graph.addFriendToUser(friendKeys.getPublic(), "Alice");

        KeyDistTuple[] list = graph.getList();

        // Should contain entries for both the user's own node and the friend
        assertEquals(2, list.length);
    }

    @Test
    public void getMinPaths_findsDirectFriendPath() throws Exception {
        // Simulate: this user knows "friend", and "friend" is reported by the other
        // party as being at distance 0 from them (i.e., friend IS the other party)
        KeyPair friendKeys = generateTestKeyPair();
        graph.addFriendToUser(friendKeys.getPublic(), "Alice");

        KeyDistTuple[] otherPartyList = new KeyDistTuple[] {
                new KeyDistTuple(friendKeys.getPublic(), 0)
        };

        LinkedList<PublicKey[]> paths = graph.getMinPaths(otherPartyList);

        assertFalse(paths.isEmpty());
        assertEquals(1, paths.getFirst().length); // direct friend = 1-hop path
    }

    @Test
    public void addFriendToFriend_isSymmetric() throws Exception {
        KeyPair aKeys = generateTestKeyPair();
        KeyPair bKeys = generateTestKeyPair();

        graph.addFriendToUser(aKeys.getPublic(), "A");
        Node nodeA = graph.nodeList.get(aKeys.getPublic());
        graph.addFriendToFriend(nodeA, bKeys.getPublic());

        Node nodeB = graph.nodeList.get(bKeys.getPublic());

        // Verify the friendship was added on both sides
        assertTrue(nodeA.friends.containsKey(bKeys.getPublic()));
        assertTrue(nodeB.friends.containsKey(aKeys.getPublic()));
    }

}
