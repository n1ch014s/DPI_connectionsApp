package connections;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;


public class GraphUtil {
    public HashMap<PublicKey, Node> nodeList;
    public Node userNode;

    /**
     * Constructor for GraphUtil.
     * Creates User Node and initializes nodeList of all Nodes.
     *
     * @param userName User's name
     * @param pub User's public key
     * @param priv User's private key
     */
    public GraphUtil(String userName, PublicKey pub, PrivateKey priv){
        userNode = new Node(pub, userName, priv);
        nodeList = new HashMap<PublicKey, Node>();
        nodeList.put(pub, userNode);
    }


    /**
     * Method for adding friends to a Node.
     *
     *
     * @param node Existing Friend node to add friend to
     * @param pub new friend's Public Key
     */
    public void addFriendToFriend(Node node, PublicKey pub) {
        if(nodeList.containsKey(pub)) {
            node.addFriend(nodeList.get(pub));
        }
        else {
            Node friend = new Node(pub);
            nodeList.put(pub, friend);
            node.addFriend(friend);
        }
    }

    /**
     * Method for adding list of friends to a Node.
     *
     *
     * @param node Existing Friend node to add list of friends to
     * @param pubKeys list of Public Keys of new friends
     */
    public void addFriendToFriend(Node node, PublicKey[] pubKeys) {
        for(int i = 0; i < pubKeys.length; i++) {
            PublicKey pub = pubKeys[i];
            if(nodeList.containsKey(pub)) {
                node.addFriend(nodeList.get(pub));
            }
            else {
                Node friend = new Node(pub);
                nodeList.put(pub, friend);
                node.addFriend(friend);
            }
        }
    }

    /**
     * Method for adding friends to a Node by their Public Key.
     *
     *
     * @param nodePub Existing Node's Public Key to add friend to
     * @param friendPub new friend's Public Key
     */
    public void addFriendToFriend(PublicKey nodePub, PublicKey friendPub) {
        if(nodeList.containsKey(friendPub)) {
            nodeList.get(nodePub).addFriend(nodeList.get(friendPub));
        }
        else {
            Node friend = new Node(friendPub);
            nodeList.put(friendPub, friend);
            nodeList.get(nodePub).addFriend(friend);
        }
    }

    /**
     * Method for adding list of friends to a Node by their Public Key.
     *
     *
     * @param nodePub Existing Node's Public Key to add list of friends to
     * @param pubKeys list of Public Keys of new friends
     */
    public void addFriendToFriend(PublicKey nodePub, PublicKey[] pubKeys) {
        Node node = nodeList.get(nodePub);
        for(int i = 0; i < pubKeys.length; i++) {
            PublicKey pub = pubKeys[i];
            if(nodeList.containsKey(pub)) {
                node.addFriend(nodeList.get(pub));
            }
            else {
                Node friend = new Node(pub);
                nodeList.put(pub, friend);
                node.addFriend(friend);
            }
        }
    }

    /**
     * Method for adding friends to a Node.
     *
     * @param pub new friend's Public Key
     * @param name new friend's username
     */
    public void addFriendToUser(PublicKey pub, String name) {
        if(nodeList.containsKey(pub)) {
            Node friend = nodeList.get(pub);
            friend.isFriend = true;
            friend.name = name;
            userNode.addFriend(friend);
        }
        else {
            Node friend = new Node(pub, name);
            userNode.addFriend(friend);
        }
    }
}