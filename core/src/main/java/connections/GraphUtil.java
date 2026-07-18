package connections;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;


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
        nodeList = new HashMap<>();
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
        for (PublicKey pub : pubKeys) {
            if (nodeList.containsKey(pub)) {
                node.addFriend(nodeList.get(pub));
            } else {
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
        for (PublicKey pub : pubKeys) {
            if (nodeList.containsKey(pub)) {
                node.addFriend(nodeList.get(pub));
            } else {
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
            nodeList.put(pub, friend);
            userNode.addFriend(friend);
        }
    }

    /**
     * Returns the distance to a Node with a given PublicKey
     * @param pub Node's PublicKey
     * @return Distance or -1 if PublicKey not found
     */
    public int getDistance(PublicKey pub){
        if(nodeList.containsKey(pub)) {
            Node node = nodeList.get(pub);
            if(node.isFriend) {
                return 1;
            }
            else {
                return 2;
            }
        }
        else {
            return -1;
        }
    }

    /**
     * Getter Method for list, which can be sent to other users
     * for finding common connection/path
     *
     * @return array of KeyDistTuples of all other nodes.
     */
    public KeyDistTuple[] getList() {
        KeyDistTuple[] list = new KeyDistTuple[nodeList.size()];
        int i = 0;
        for(PublicKey pub : nodeList.keySet()) {
            list[i] = new KeyDistTuple(pub, getDistance(pub));
            i++;
        }
        return list;
    }

    /**
     * Returns all paths with minimum possible distance.
     * @param list list from other user with all keys and distances to other nodes
     * @return LinkedList of all possible paths with same total length.
     * Longer paths or paths to unknown PublicKeys are omitted.
     */
    public LinkedList<PublicKey[]> getMinPaths(KeyDistTuple[] list, PublicKey pub){
        LinkedList<PublicKey[]> paths = new LinkedList<>();
        if(nodeList.containsKey(pub) && nodeList.get(pub).isFriend) {
            PublicKey[] path = {pub};
            paths.add(path);
            return paths;
        }
        int currMin = 100;
        //loop through received KeyDistTuple List
        for(int i = 0; i < list.length; i++) {
            KeyDistTuple t = list[i];
            int dist = getDistance(t.key);
            //if node with key exists
            if(dist > 0) {
                int totalDist = dist + t.distance;
                //System.out.println("TotalDist: " + totalDist + ", currMin: " + currMin);
                //new class of shorter paths found
                if(totalDist < currMin) {
                    currMin = totalDist;
                    paths.clear();
                    PublicKey[] path = new PublicKey[totalDist];
                    //node from list is a friend
                    if(dist == 1) {
                        path[0] = t.key;
                        paths.add(path);
                    }
                    //node from list is friend of friend
                    else {
                        //in case friend of friend is friend of several friends
                        for(PublicKey k : nodeList.get(t.key).friends.keySet()) {
                            if(nodeList.get(k).isFriend) {
                                path = new PublicKey[totalDist];
                                path[1] = k;
                                path[0] = t.key;
                                paths.add(path);
                            }
                        }
                    }
                }
                //all paths with minimum length are added
                else if(totalDist == currMin) {
                    PublicKey[] path = new PublicKey[currMin];
                    //node from list is a friend
                    if(dist == 1) {
                        path[0] = t.key;
                        paths.add(path);
                    }
                    //node from list is friend of friend
                    else {
                        //in case friend of friend is friend of several friends
                        for(PublicKey k : nodeList.get(t.key).friends.keySet()) {
                            if (nodeList.get(k).isFriend) {
                                path = new PublicKey[totalDist];
                                path[1] = k;
                                path[0] = t.key;
                                paths.add(path);
                            }
                        }
                    }

                }
            }
        }
        return paths;
    }

    /**
     * Fills the LinkedList of paths received from other user with all possible paths
     * @param paths received LinkedList of all paths of minimum length as connection
     * @param otherUser the PublicKey of the other user (added at the end for complete path)
     * @return  all possible paths of min length from this user to other user
     */
    public LinkedList<PublicKey[]> fillMinPaths(LinkedList<PublicKey[]> paths, PublicKey otherUser) {
        LinkedList<PublicKey[]> completedPaths = new LinkedList<>();
        if(paths.isEmpty()) {
            return paths;
        }
        if(paths.get(0).length == 1) {
            return paths;
        }
        for(int i = 0; i < paths.size(); i++) {
            PublicKey[] curr = paths.get(i);
            Node interfaceNode = nodeList.get(curr[0]);
            if(interfaceNode.isFriend) {
                PublicKey[] path = new PublicKey[curr.length];
                System.arraycopy(curr, 0, path, 0, curr.length);
                path[path.length-1] = otherUser;
                completedPaths.add(path);
            }
            else {
                for(PublicKey p : interfaceNode.friends.keySet()) {
                    if(nodeList.get(p).isFriend) {
                        PublicKey[] path = new PublicKey[curr.length];
                        path[0] = p;
                        int j = 0;
                        while(curr[j] != null) {
                            path[j+1] = curr[j];
                            j++;
                        }
                        path[path.length-1] = otherUser;
                        completedPaths.add(path);
                    }
                }
            }
        }
        return completedPaths;
    }

    public Node getUserNode() {
        return userNode;
    }

    public PublicKey[] getFriendsList() {
        PublicKey[] list = new PublicKey[userNode.friends.size()];
        int i = 0;
        for(PublicKey pub : userNode.friends.keySet()) {
            list[i] = pub;
            i++;
        }
        return list;
    }

    public String[] getFriendsListString(){
        String[] list = new String[userNode.friends.size()];
        int i = 0;
        for(PublicKey pub : getFriendsList()){
            list[i] = Base64.getEncoder().encodeToString(pub.getEncoded());
            i++;
        }
        return list;
    }

    public PublicKey[] getFriendsList(PublicKey pk) {
        if(nodeList.containsKey(pk)) {
            PublicKey[] list = new PublicKey[nodeList.get(pk).friends.size()];
            int i = 0;
            for (PublicKey pub : nodeList.get(pk).friends.keySet()) {
                list[i] = pub;
                i++;
            }
            return list;
        }
        else {
            return null;
        }
    }
}