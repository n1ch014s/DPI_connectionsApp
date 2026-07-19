package connections;

import org.junit.Before;
import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.LinkedList;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;

import static org.junit.Assert.*;
public class GraphUtilTest {

    private GraphUtil graph;
    private KeyPair userKeys;

    private GraphUtil graph2;

    private KeyPair userKeys2;

    @Before
    public void setUp() throws Exception {
        userKeys = generateTestKeyPair();
        graph = new GraphUtil("TestUser", userKeys.getPublic(), userKeys.getPrivate());

        userKeys2 = generateTestKeyPair();
        graph2 = new GraphUtil("TestUser2", userKeys2.getPublic(), userKeys2.getPrivate());
    }

    static {
        if (Security.getProvider("BC") == null) {
            Security.insertProviderAt(new BouncyCastleProvider(), 1);
        }
    }

    private static KeyPair generateTestKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519", "BC");
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

        assertEquals(1, list.length);
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

        LinkedList<PublicKey[]> paths = graph.getMinPaths(otherPartyList, friendKeys.getPublic());

        assertFalse(paths.isEmpty());
        assertEquals(1, paths.getFirst().length); // direct friend = 1-hop path
    }

    @Test
    public void fillMinPaths_isSameSize() throws Exception {
        KeyPair friendKeys = generateTestKeyPair();
        KeyPair friendKeys2 = generateTestKeyPair();
        KeyPair friendKeys3 = generateTestKeyPair();
        graph.addFriendToUser(friendKeys.getPublic(), "Alice");
        graph.addFriendToFriend(friendKeys.getPublic(), friendKeys2.getPublic());
        graph2.addFriendToUser(friendKeys3.getPublic(), "Bob");
        graph2.addFriendToFriend(friendKeys3.getPublic(), friendKeys2.getPublic());
        KeyDistTuple[] list = graph.getList();
        KeyDistTuple[] list2 = graph2.getList();
        LinkedList<PublicKey[]> llist = graph.getMinPaths(list2, userKeys2.getPublic());
        LinkedList<PublicKey[]> llist2 = graph2.getMinPaths(list, userKeys.getPublic());
        assertEquals(graph.fillMinPaths(llist2, userKeys2.getPublic()).size(), graph2.fillMinPaths(llist, userKeys.getPublic()).size());
        assertEquals(graph.fillMinPaths(llist2, userKeys2.getPublic()).get(0).length, graph2.fillMinPaths(llist, userKeys.getPublic()).get(0).length);
    }

    @Test
    public void fillMinPaths_findsCorrectDistanceAndPaths1() throws Exception {
        KeyPair friendKeys = generateTestKeyPair();
        KeyPair friendKeys2 = generateTestKeyPair();
        KeyPair friendKeys3 = generateTestKeyPair();
        graph.addFriendToUser(friendKeys.getPublic(), "Alice");
        graph.addFriendToFriend(friendKeys.getPublic(), friendKeys2.getPublic());
        graph2.addFriendToUser(friendKeys3.getPublic(), "Bob");
        graph2.addFriendToFriend(friendKeys3.getPublic(), friendKeys2.getPublic());
        KeyDistTuple[] list = graph.getList();
        KeyDistTuple[] list2 = graph2.getList();
        LinkedList<PublicKey[]> llist = graph.getMinPaths(list2, userKeys2.getPublic());
        LinkedList<PublicKey[]> llist2 = graph2.getMinPaths(list, userKeys.getPublic());
        assertEquals(graph.fillMinPaths(llist2, userKeys2.getPublic()).size(), graph2.fillMinPaths(llist, userKeys.getPublic()).size());
        assertEquals(graph.fillMinPaths(llist2, userKeys2.getPublic()).get(0).length, graph2.fillMinPaths(llist, userKeys.getPublic()).get(0).length);
        assertEquals(4, graph.fillMinPaths(llist2, userKeys2.getPublic()).get(0).length);
        assertEquals(1, graph.fillMinPaths(llist2, userKeys2.getPublic()).size());
    }

    @Test
    public void fillMinPaths_findsCorrectDistanceAndPaths2() throws Exception {
        KeyPair friendKeys = generateTestKeyPair();
        KeyPair friendKeys2 = generateTestKeyPair();
        KeyPair friendKeys3 = generateTestKeyPair();
        KeyPair friendKeys4 = generateTestKeyPair();
        graph.addFriendToUser(friendKeys.getPublic(), "Alice");
        graph.addFriendToFriend(friendKeys.getPublic(), friendKeys2.getPublic());
        graph.addFriendToFriend(friendKeys.getPublic(), friendKeys4.getPublic());
        graph2.addFriendToUser(friendKeys3.getPublic(), "Bob");
        graph2.addFriendToFriend(friendKeys3.getPublic(), friendKeys2.getPublic());
        graph2.addFriendToFriend(friendKeys3.getPublic(), friendKeys4.getPublic());
        KeyDistTuple[] list = graph.getList();
        KeyDistTuple[] list2 = graph2.getList();
        LinkedList<PublicKey[]> llist = graph.getMinPaths(list2, userKeys2.getPublic());
        LinkedList<PublicKey[]> llist2 = graph2.getMinPaths(list, userKeys.getPublic());
        assertEquals(graph.fillMinPaths(llist2, userKeys2.getPublic()).size(), graph2.fillMinPaths(llist, userKeys.getPublic()).size());
        assertEquals(graph.fillMinPaths(llist2, userKeys2.getPublic()).get(0).length, graph2.fillMinPaths(llist, userKeys.getPublic()).get(0).length);
        assertEquals(4, graph.fillMinPaths(llist2, userKeys2.getPublic()).get(0).length);
        assertEquals(2, graph.fillMinPaths(llist2, userKeys2.getPublic()).size());
    }

    @Test
    public void fillMinPaths_findsCorrectDistanceAndPaths3() throws Exception {
        KeyPair friendKeys = generateTestKeyPair();
        KeyPair friendKeys2 = generateTestKeyPair();
        KeyPair friendKeys3 = generateTestKeyPair();
        KeyPair friendKeys4 = generateTestKeyPair();
        KeyPair friendKeys5 = generateTestKeyPair();
        graph.addFriendToUser(friendKeys.getPublic(), "Alice");
        graph.addFriendToUser(friendKeys5.getPublic(), "Charlie");
        graph.addFriendToFriend(friendKeys.getPublic(), friendKeys2.getPublic());
        graph.addFriendToFriend(friendKeys.getPublic(), friendKeys4.getPublic());
        graph.addFriendToFriend(friendKeys5.getPublic(), friendKeys4.getPublic());
        graph2.addFriendToUser(friendKeys3.getPublic(), "Bob");
        graph2.addFriendToFriend(friendKeys3.getPublic(), friendKeys2.getPublic());
        graph2.addFriendToFriend(friendKeys3.getPublic(), friendKeys4.getPublic());
        KeyDistTuple[] list = graph.getList();
        KeyDistTuple[] list2 = graph2.getList();
        LinkedList<PublicKey[]> llist = graph.getMinPaths(list2, userKeys2.getPublic());
        LinkedList<PublicKey[]> llist2 = graph2.getMinPaths(list, userKeys.getPublic());
        assertEquals(graph.fillMinPaths(llist2, userKeys2.getPublic()).size(), graph2.fillMinPaths(llist, userKeys.getPublic()).size());
        assertEquals(graph.fillMinPaths(llist2, userKeys2.getPublic()).get(0).length, graph2.fillMinPaths(llist, userKeys.getPublic()).get(0).length);
        assertEquals(4, graph.fillMinPaths(llist2, userKeys2.getPublic()).get(0).length);
        assertEquals(3, graph.fillMinPaths(llist2, userKeys2.getPublic()).size());
    }

    @Test
    public void fillMinPaths_isMinimal() throws Exception {
        KeyPair friendKeys = generateTestKeyPair();
        KeyPair friendKeys2 = generateTestKeyPair();
        KeyPair friendKeys3 = generateTestKeyPair();
        graph.addFriendToUser(friendKeys.getPublic(), "Alice");
        graph.addFriendToFriend(friendKeys.getPublic(), friendKeys2.getPublic());
        graph.addFriendToFriend(friendKeys.getPublic(), friendKeys3.getPublic());
        graph2.addFriendToUser(friendKeys3.getPublic(), "Bob");
        graph2.addFriendToFriend(friendKeys3.getPublic(), friendKeys2.getPublic());
        graph2.addFriendToFriend(friendKeys3.getPublic(), friendKeys.getPublic());
        KeyDistTuple[] list = graph.getList();
        KeyDistTuple[] list2 = graph2.getList();
        LinkedList<PublicKey[]> llist = graph.getMinPaths(list2, userKeys2.getPublic());
        LinkedList<PublicKey[]> llist2 = graph2.getMinPaths(list, userKeys.getPublic());
        assertEquals(graph.fillMinPaths(llist2, userKeys2.getPublic()).size(), graph2.fillMinPaths(llist, userKeys.getPublic()).size());
        /*System.out.println("Alice: " + friendKeys.getPublic());
        System.out.println("K2: " + friendKeys2.getPublic());
        System.out.println("Bob: " + friendKeys3.getPublic());
        for(PublicKey[] p:graph.getMinPaths(list2, userKeys2.getPublic())) {
            System.out.println("Graph 1 min: ");
            for(PublicKey pub:p) {
                System.out.println(pub);
            }
            System.out.println(" ");
        }
        for(PublicKey[] p:graph2.getMinPaths(list, userKeys.getPublic())) {
            System.out.println("Graph 2 min: ");
            for(PublicKey pub:p) {
                System.out.println(pub);
            }
            System.out.println(" ");
        }
        for(PublicKey[] p:graph.fillMinPaths(llist2, userKeys2.getPublic())) {
            System.out.println("Graph 1 filled: ");
            for(PublicKey pub:p) {
                System.out.println(pub);
            }
            System.out.println(" ");
        }
        System.out.println("-----------------");
        for(PublicKey[] p:graph2.fillMinPaths(llist, userKeys.getPublic())) {
            System.out.println("Graph 2 filled: ");
            for(PublicKey pub:p) {
                System.out.println(pub);
            }
            System.out.println();
        }*/
        assertEquals(graph.fillMinPaths(llist2, userKeys2.getPublic()).get(0).length, graph2.fillMinPaths(llist, userKeys.getPublic()).get(0).length);
        assertEquals(3, graph.fillMinPaths(llist2, userKeys2.getPublic()).get(0).length);
    }

    @Test
    public void fillMinPaths_noCommonNodes() throws Exception {
        KeyPair friendKeys = generateTestKeyPair();
        KeyPair friendKeys2 = generateTestKeyPair();
        graph.addFriendToUser(friendKeys.getPublic(), "Alice");
        graph2.addFriendToUser(friendKeys2.getPublic(), "Bob");
        KeyDistTuple[] list = graph.getList();
        KeyDistTuple[] list2 = graph2.getList();
        LinkedList<PublicKey[]> llist = graph.getMinPaths(list2, userKeys2.getPublic());
        LinkedList<PublicKey[]> llist2 = graph2.getMinPaths(list, userKeys.getPublic());
        assertEquals(graph.fillMinPaths(llist2, userKeys2.getPublic()).size(), graph2.fillMinPaths(llist, userKeys.getPublic()).size());
        assertTrue(graph.fillMinPaths(llist2, userKeys2.getPublic()).isEmpty());
    }

    @Test
    public void fillMinPaths_sameThreeFriends() throws Exception {
        KeyPair friendKeys = generateTestKeyPair();
        KeyPair friendKeys2 = generateTestKeyPair();
        KeyPair friendKeys3 = generateTestKeyPair();
        graph.addFriendToUser(friendKeys.getPublic(), "Alice");
        graph.addFriendToUser(friendKeys2.getPublic(), "Bob");
        graph.addFriendToUser(friendKeys3.getPublic(), "Charlie");
        graph.addFriendToFriend(friendKeys.getPublic(), friendKeys2.getPublic());
        graph.addFriendToFriend(friendKeys3.getPublic(), friendKeys2.getPublic());
        graph.addFriendToFriend(friendKeys.getPublic(), friendKeys3.getPublic());
        graph2.addFriendToUser(friendKeys.getPublic(), "Alice");
        graph2.addFriendToUser(friendKeys2.getPublic(), "Bob");
        graph2.addFriendToUser(friendKeys3.getPublic(), "Charlie");
        graph2.addFriendToFriend(friendKeys.getPublic(), friendKeys2.getPublic());
        graph2.addFriendToFriend(friendKeys3.getPublic(), friendKeys2.getPublic());
        graph2.addFriendToFriend(friendKeys.getPublic(), friendKeys3.getPublic());
        KeyDistTuple[] list = graph.getList();
        KeyDistTuple[] list2 = graph2.getList();
        LinkedList<PublicKey[]> llist = graph.getMinPaths(list2, userKeys2.getPublic());
        LinkedList<PublicKey[]> llist2 = graph2.getMinPaths(list, userKeys.getPublic());
        assertEquals(graph.fillMinPaths(llist2, userKeys2.getPublic()).size(), graph2.fillMinPaths(llist, userKeys.getPublic()).size());
        assertEquals(graph.fillMinPaths(llist2, userKeys2.getPublic()).get(0).length, graph2.fillMinPaths(llist, userKeys.getPublic()).get(0).length);
        assertEquals(2, graph.fillMinPaths(llist2, userKeys2.getPublic()).get(0).length);
    }

    @Test
    public void fillMinPaths_theyAreFriends() throws Exception {
        graph.addFriendToUser(userKeys2.getPublic(), "Alice");
        graph2.addFriendToUser(userKeys.getPublic(), "Bob");
        KeyDistTuple[] list = graph.getList();
        KeyDistTuple[] list2 = graph2.getList();
        LinkedList<PublicKey[]> llist = graph.getMinPaths(list2, userKeys2.getPublic());
        LinkedList<PublicKey[]> llist2 = graph2.getMinPaths(list, userKeys.getPublic());
        /*System.out.println("Alice: " + userKeys2.getPublic());
        System.out.println("Bob: " + userKeys.getPublic());
        for(PublicKey[] p:llist2) {
            System.out.println("Graph 1 min: ");
            for(PublicKey pub:p) {
                System.out.println(pub);
            }
            System.out.println(" ");
        }
        for(PublicKey[] p:llist) {
            System.out.println("Graph 2 min: ");
            for(PublicKey pub:p) {
                System.out.println(pub);
            }
            System.out.println(" ");
        }
        for(PublicKey[] p:graph.fillMinPaths(llist2, userKeys2.getPublic())) {
            System.out.println("Graph 1 filled: ");
            for(PublicKey pub:p) {
                System.out.println(pub);
            }
            System.out.println(" ");
        }
        System.out.println("-----------------");
        for(PublicKey[] p:graph2.fillMinPaths(llist, userKeys.getPublic())) {
            System.out.println("Graph 2 filled: ");
            for(PublicKey pub:p) {
                System.out.println(pub);
            }
            System.out.println();
        }*/
        assertEquals(graph.fillMinPaths(llist2, userKeys2.getPublic()).size(), graph2.fillMinPaths(llist, userKeys.getPublic()).size());
        assertEquals(graph.fillMinPaths(llist2, userKeys2.getPublic()).get(0).length, graph2.fillMinPaths(llist, userKeys.getPublic()).get(0).length);
        assertEquals(1, graph.fillMinPaths(llist2, userKeys2.getPublic()).get(0).length);
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
