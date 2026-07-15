package connections;

import java.security.PublicKey;
import java.security.PrivateKey;
import java.util.HashMap;


public class Node {
    //User Node attributes
    public final boolean isUser;
    public final PrivateKey privateKey;

    //Friend Node attributes
    public boolean isFriend;
    public String name;

    //General attributes
    public final PublicKey publicKey;
    public HashMap<PublicKey, Node> friends;

    /**
     * Constructor for User Node
     *
     * @param pub User's public key
     * @param priv User's private key
     * @param n User's name
     */
    Node(PublicKey pub, String n, PrivateKey priv) {
        isUser = true;
        isFriend = false;
        publicKey = pub;
        privateKey = priv;
        name = n;
        friends = new HashMap<>();
    }

    /**
     * Constructor for Friend Node
     *
     * @param pub Friend's public key
     * @param n Friend's name
     */
    public Node(PublicKey pub, String n) {
        isUser = false;
        isFriend = true;
        publicKey = pub;
        privateKey = null;
        name = n;
        friends = new HashMap<>();
    }

    /**
     * Constructor for Friend of Friend Node
     *
     * @param pub User's public key
     */
    Node(PublicKey pub) {
        isUser = false;
        isFriend = false;
        publicKey = pub;
        privateKey = null;
        friends = new HashMap<>();
    }

    /**
     * Method for adding new Friend node to this node's friend map.
     * Also adds this node as new Friend to other node.
     *
     * @param friendNode new Friend node
     */
    public void addFriend(Node friendNode) {
        friends.put(friendNode.publicKey, friendNode);
        friendNode.friends.put(publicKey, this);
    }

    /**
     * Method for removing Friend node from this node's friend map.
     * Also removes this node as Friend from other node.
     *
     * @param friendNode Friend node to be removed
     */
    void removeFriend(Node friendNode) {
        friends.remove(friendNode.publicKey);
        friendNode.friends.remove(publicKey);
        if(friendNode.isFriend && isUser) {
            friendNode.isFriend = false;
            friendNode.name = null;
        }
    }

    public PublicKey getPublicKey(){
        return this.publicKey;
    }
}