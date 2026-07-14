package transmission;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;

import connections.GraphUtil;
import connections.Node;

/** The Peer to Peer synchronisation.
 *
 */
public class Sync{
    private final GraphUtil graph;
    private final NFCManager nfcManager;

    /**
     * Creates a Sync Manager which controls and processing of incoming and outgoing nodes
     * @param graph the graph which contains the friend data
     */
    public Sync(GraphUtil graph){
        this.graph = graph;
        this.nfcManager = new NFCManager(this);
    }

    /**
     * Parses and Processes incoming JSON data, turning them into nodes
     * @param info the incoming data, this is turned into a node with a friend list
     */
    public void processIncoming(String info){
        // info built like: public key || name || friend | friend | friend
        String[] data;
        data = info.split("\\|\\|");
        PublicKey pub = null;
        try {
            pub = decodeString(data[0]);
        }catch (Exception e){
            e.printStackTrace();
        }
        String name = data[1];
        String[] friendPubs = data[2].split("\\|");

        graph.addFriendToUser(pub, name);

    }

    /**
     * Process the outgoing messages by turning the usernode and the friendslist into a string which can be transmitted
     * @param userNode Users own node with its username and public key
     * @param nodeList nodelist of friends which are all to be transmitted as well
     */
    public void processOutgoing(Node userNode, HashMap<PublicKey, Node> nodeList){
        StringBuilder builder = new StringBuilder();
        String pubkeyString = Base64.getEncoder().encodeToString(userNode.publicKey.getEncoded());
        builder.append("||");
        builder.append(userNode.name);
        builder.append("||");
        for (PublicKey pk : nodeList.keySet()){
            String pkString = Base64.getEncoder().encodeToString(pk.getEncoded());
            builder.append(pkString);
            builder.append("|");
        }


        nfcManager.send(builder.toString());

    }

    /**
     * Translate a string into a public key
     *
     * @param pubString The String which is to be decoded into a public key
     * @return Public Key translated from the input
     * @throws Exception Thrown in case of an error
     */
    public PublicKey decodeString(String pubString) throws Exception{
        byte[] publicKeyBytes = Base64.getDecoder().decode(pubString.getBytes(StandardCharsets.UTF_8));

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePublic(keySpec);
    }

    public NFCManager getNfcManager() {
        return nfcManager;
    }
}
