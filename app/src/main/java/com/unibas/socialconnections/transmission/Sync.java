package com.unibas.socialconnections.transmission;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.nfc.NfcAdapter;
import android.util.Log;
import android.content.Intent;

import com.unibas.socialconnections.GUI.PathGraphBuilder;
import com.unibas.socialconnections.GUI.PathHolder;
import com.unibas.socialconnections.KeyManager;
import com.unibas.socialconnections.activities.PathActivity;
import com.unibas.socialconnections.storage.GraphStorage;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import computer.iroh.EndpointId;
import connections.GraphUtil;
import connections.KeyDistTuple;
import connections.Node;

/** The Peer to Peer synchronisation.
 *
 */
public class Sync implements MessageListener{
    private final GraphUtil graph;
    private final GraphStorage graphStorage;
    private final NFCManager nfcManager;
    private final Activity activity;
    private final Node userNode;
    private final IrohManager irohManager;
    private final Gossip gossip;
    private static Sync instance;

    private ConcurrentHashMap<UUID, Packet> receivedPackets = new ConcurrentHashMap<>();
    private MessageTuple recvNodeListBytes;
    private MessageTuple encodedRecvMinPathBytes;
    private MessageTuple updateBytes;


    /**
     * Creates a Sync Manager which controls and processing of incoming and outgoing nodes
     * @param graph the graph which contains the friend data
     * @param graphStorage
     * @param nfcAdapter
     * @param activity
     * @param userNode
     */
    public Sync(GraphUtil graph, GraphStorage graphStorage, NfcAdapter nfcAdapter, Activity activity, Node userNode){
        this.graph = graph;
        this.graphStorage = graphStorage;
        this.activity = activity;
        this.userNode = userNode;
        this.nfcManager = new NFCManager(this, nfcAdapter);
        this.irohManager = new IrohManager();
        irohManager.start(graph, KeyManager.getIrohSecretKey(userNode.getPrivateKey()));
        irohManager.startReceiving(this);

        this.gossip = new Gossip(irohManager);
        instance = this;

    }

    /**
     * Parses and Processes incoming JSON data, turning them into nodes
     * @param info the incoming data, this is turned into a node with a friend list
     */
    public void processIncoming(String info){
        // info built like: public key || name || Iroh Endpoint || friendModeBit || friend | friend | friend
        String[] data;
        data = info.split("\\|\\|");
        PublicKey pub = null;
        try {
            //Log.d("NFC", "Data Received: " + info);
            pub = decodeString(data[0]);
        }catch (Exception e){
            e.printStackTrace();
        }
        String name = data[1];
        String ticket = data[2];
        String friendMode = data[3];
        String friendPubs = null;
        if(data.length > 4 ){
            friendPubs = data[4];
        }

        if(getHostingStatus()){
            irohManager.connect(ticket);
            setHostingStatus(false);
            //Log.d("HOST", "Host closed");
        }else {
            //Log.d("Iroh", "Accepting next connection...");
            irohManager.accept(ticket);

        }

        if(friendMode.equals("1")) {
            try {
                //Log.d("Iroh", "Gossip Established!");
                graph.addFriendToUser(pub, name);
                graph.nodeList.get(pub).setEndpointId(ticket);
                if(!graph.getKeyList().containsKey(ticket)){
                    graph.addToKeyList(ticket, pub);
                }
                graphStorage.saveNode(graph.nodeList.get(pub));
                graphStorage.saveFriendship(graph.getUserNode(), graph.nodeList.get(pub));
                PublicKey[] friendsList = graph.getFriendsList();
                gossip.publish(encodePublicKeyArray(friendsList).getBytes(StandardCharsets.UTF_8));

                //String encodedRecvFriendsList = new String(gossip.receive(data[0]), StandardCharsets.UTF_8); This is unnecessary as weve already passed the friend list over nfc

                if(friendPubs != null) {
                    //Log.d("Sync", "FriendsList: " + friendPubs);
                    PublicKey[] decodedRecvFriendsList = decodePublicKeyArray(friendPubs);
                    graph.addFriendToFriend(pub, decodedRecvFriendsList);
                    for(PublicKey p:decodedRecvFriendsList) {
                        graphStorage.saveNode(graph.nodeList.get(p));
                        graphStorage.saveFriendship(graph.nodeList.get(pub), graph.nodeList.get(p));
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }else{
            try {
                //Log.d("Iroh", "Connection Established!");

                Node contact = new Node(pub, name, ticket);
                //Log.d("SYNC", "New Node with ticket: " + ticket);

                /**
                 * compare node lists
                 */
                KeyDistTuple[] nodeList = graph.getList();
                /*Log.d("DEBUG SYNC", "User pub: " + graph.getUserNode().publicKey + ", Other pub: " + pub);
                for(KeyDistTuple kdt : nodeList) {
                    Log.d("DEBUG SYNC", "Friend: " + kdt.key + ", Distance: " + kdt.distance);
                }*/
                String encodedNodeList = encodeKeyDistList(nodeList);
                Packet nodePacket = new Packet(UUID.randomUUID(), MessageType.NODE_LIST, encodedNodeList.getBytes(StandardCharsets.UTF_8));
                irohManager.send(ticket, nodePacket.toBytes());

                //Log.d("SYNC", "Waiting for Node List from: " + contact);
                waitFor(contact, MessageType.NODE_LIST);
                String recvNodeListStr = new String(recvNodeListBytes.getMessage(), StandardCharsets.UTF_8);


                /*
                 * compare min paths
                 */
                LinkedList<PublicKey[]> minPaths = graph.getMinPaths(decodeKeyDistList(recvNodeListStr), pub);
                //Log.d("MINPATH", "Minpath: " + minPaths);
                String encodedMinPaths = encodePaths(minPaths);
                Packet minPathPacket = new Packet(UUID.randomUUID(), MessageType.MIN_PATH, encodedMinPaths.getBytes(StandardCharsets.UTF_8));
                irohManager.send(ticket, minPathPacket.toBytes());
                //Log.d("SYNC", "sent min paths");

                waitFor(contact, MessageType.MIN_PATH);
                String encodedRecvMinPaths = new String(encodedRecvMinPathBytes.getMessage(), StandardCharsets.UTF_8);


                /*
                 * build with min path
                 */
                LinkedList<PublicKey[]> filledMinPaths = graph.fillMinPaths(decodePaths(encodedRecvMinPaths), pub);
                /*Log.d("DEBUG SYNC", "filledMinPaths: ");
                int bibedibu = 0;
                for(PublicKey[] pks: filledMinPaths) {
                    Log.d("DEBUG SYNC", "Path " + bibedibu++ + ":");
                    int babedibe = 0;
                    for(PublicKey publicoKeko:pks) {
                        Log.d("DEBUG SYNC", "Friend: " + publicoKeko + ", Step: " + babedibe++);
                    }
                }*/
                PathGraphBuilder.GraphData graphData = PathGraphBuilder.build(filledMinPaths, graph, name, pub);

                encodedRecvMinPathBytes = null;
                recvNodeListBytes = null;

                PathHolder.pendingData = graphData;
                PathHolder.otherUsername = name;
                activity.runOnUiThread(() -> {
                    Intent intent = new Intent(activity, PathActivity.class);
                    activity.startActivity(intent);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Process the outgoing messages by turning the usernode and the friendslist into a string which can be transmitted
     * @return returns the message string generated from the usernode
     */
    public String processOutgoing(){
        PublicKey[] nodeList = graph.getFriendsList();
        StringBuilder builder = new StringBuilder();
        String pubkeyString = encodeString(userNode.getPublicKey());
        builder.append(pubkeyString);
        builder.append("||");
        builder.append(userNode.getName());
        builder.append("||");
        builder.append(irohManager.getEndpointId());
        builder.append("||");
        if(nfcManager.getFriendStatus()){
            builder.append("1");
        } else{
            builder.append("0");
        }
        builder.append("||");
        for (PublicKey pk : nodeList) {
            String pkString = encodeString(pk);
            builder.append(pkString);
            builder.append("|");
        }


        return builder.toString();
    }

    /**
     * Translate a string into a public key
     *
     * @param pubString The String which is to be decoded into a public key
     * @return Public Key translated from the input
     * @throws Exception Thrown in case of an error
     */
    public PublicKey decodeString(String pubString) throws Exception {
        byte[] publicKeyBytes = Base64.getDecoder().decode(pubString.getBytes(StandardCharsets.UTF_8));
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", "BC");
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * Translate a publicKey into a string. it isnt a crazy method, i just got sick of typing that whole thing
     * @param pub the publickey which is to be encoded
     * @return encoded String
     */
    public String encodeString(PublicKey pub){
        return Base64.getEncoder().encodeToString(pub.getEncoded());
    }

    public static String encodePublicKeyArray(PublicKey[] keys) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            sb.append(Base64.getEncoder().encodeToString(keys[i].getEncoded()));
            if (i < keys.length - 1) sb.append("|");
        }
        return sb.toString();
    }

    public static PublicKey[] decodePublicKeyArray(String data) throws Exception {
        if (data == null || data.isEmpty()) return new PublicKey[0];

        //Log.d("Sync", "Key Data: " + data);
        String[] keyStrings = data.split("\\|");
        PublicKey[] result = new PublicKey[keyStrings.length];

        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", "BC");

        //Log.d("Sync", "KeyBytes: " + Arrays.toString(keyStrings));

        for (int i = 0; i < keyStrings.length; i++) {

            byte[] keyBytes = Base64.getDecoder().decode(keyStrings[i]);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            result[i] = keyFactory.generatePublic(spec);
        }
        return result;
    }

    public static String encodeKeyDistList(KeyDistTuple[] list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.length; i++) {
            String keyStr = Base64.getEncoder().encodeToString(list[i].key.getEncoded());
            sb.append(keyStr).append(",").append(list[i].distance);
            if (i < list.length - 1) sb.append(";");
        }
        return sb.toString();
    }

    public static KeyDistTuple[] decodeKeyDistList(String data) throws Exception {
        if (data == null || data.isEmpty()) return new KeyDistTuple[0];

        String[] entries = data.split(";");
        KeyDistTuple[] result = new KeyDistTuple[entries.length];

        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", "BC");

        for (int i = 0; i < entries.length; i++) {
            String[] parts = entries[i].split(",");
            byte[] keyBytes = Base64.getDecoder().decode(parts[0]);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            PublicKey pub = keyFactory.generatePublic(spec);
            int dist = Integer.parseInt(parts[1]);
            result[i] = new KeyDistTuple(pub, dist);
        }
        return result;
    }

    public static String encodePaths(LinkedList<PublicKey[]> paths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            PublicKey[] path = paths.get(i);
            for (int j = 0; j < path.length; j++) {
                if (path[j] != null) {
                    sb.append(Base64.getEncoder().encodeToString(path[j].getEncoded()));
                }
                if (j < path.length - 1) sb.append("|");
            }
            if (i < paths.size() - 1) sb.append(";");
        }
        return sb.toString();
    }

    public static LinkedList<PublicKey[]> decodePaths(String data) throws Exception {
        LinkedList<PublicKey[]> result = new LinkedList<>();
        if (data == null || data.isEmpty()) return result;

        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", "BC");
        String[] pathStrings = data.split(";");

        for (String pathStr : pathStrings) {
            String[] keyStrings = pathStr.split("\\|", -1);
            PublicKey[] path = new PublicKey[keyStrings.length];
            for (int i = 0; i < keyStrings.length; i++) {
                if (!keyStrings[i].isEmpty()) {
                    byte[] keyBytes = Base64.getDecoder().decode(keyStrings[i]);
                    X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                    path[i] = keyFactory.generatePublic(spec);
                }
            }
            result.add(path);
        }
        //Log.d("DEBUG_MIN", "Debug decoded Min Path: " + result);
        return result;
    }



    /**
     * receiving messages over Iroh
     * @param sender The sender of the received packet
     * @param payload The received message to be parsed
     */
    @Override
    public void onMessage(EndpointId sender, byte[] payload) {

        //Log.d("MSG", "Message Received: " + sender + " | " + payload);

        Packet packet = Packet.fromBytes(payload);
        UUID uuid = packet.getUUID();
        //receivedPackets.put(uuid, packet);

        MessageType messageType = packet.getMessageType();
        Log.d("MSG", "MessageType: " + messageType);
        byte[] message = packet.getPayload();
        String senderTicket = null;

        try {
            senderTicket = sender.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        switch (messageType){
            case UPDATE:
                updateBytes = new MessageTuple(senderTicket, message);
                handleUpdate(senderTicket, message);
                break;
            case NODE_LIST:
                recvNodeListBytes = new MessageTuple(senderTicket, message);
                //Log.d("MSG", "Node List received: " + senderTicket);
                //Log.d("MSG", "NODE LIST SET: " + recvNodeListBytes);
                break;
            case MIN_PATH:
                encodedRecvMinPathBytes = new MessageTuple(senderTicket, message);
                //Log.d("MSG", "Min List received: " + senderTicket);
                //Log.d("MSG", "MIN LIST SET: " + encodedRecvMinPathBytes);
                break;
        }

    }

    private void handleUpdate(String senderTicket, byte[] message){

        PublicKey pub = graph.getKeyList().get(senderTicket);
        String friendslist = new String(message, StandardCharsets.UTF_8);
        PublicKey[] decodedRecvFriendsList = null;
        try {
            decodedRecvFriendsList  = decodePublicKeyArray(friendslist);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Only add those that are not already in the list!

        if(decodedRecvFriendsList.length > graph.getFriendsList(pub).length){
            int over = decodedRecvFriendsList.length - graph.getFriendsList(pub).length;
            PublicKey[] toBeAdded = new PublicKey[over];
            for(int i = 0 ; i < over ; i++){
                //Log.d("UPDATE", "Added Friend: " + decodedRecvFriendsList[graph.getFriendsList(pub).length + i]);
                toBeAdded[i] = decodedRecvFriendsList[graph.getFriendsList(pub).length + i];
            }
            graph.addFriendToFriend(pub, decodedRecvFriendsList);
        }//else {Log.d("UPDATE", "all friends already added");}

        for(PublicKey p:decodedRecvFriendsList) {
            graphStorage.saveNode(graph.nodeList.get(p));
        }
    }

    private void waitFor(Node sender, MessageType type) throws TimeoutException, InterruptedException {
        MessageTuple tuple = null;
        if(type.equals(MessageType.NODE_LIST)){
           tuple = recvNodeListBytes;
        }else {
            tuple = encodedRecvMinPathBytes;
        }

        long timeout = System.currentTimeMillis() + 30000;
        while (tuple == null || !tuple.getSender().equals(sender.getEndpointId())){
            if(type.equals(MessageType.NODE_LIST)){
                tuple = recvNodeListBytes;
            }else {
                tuple = encodedRecvMinPathBytes;
            }
            if(System.currentTimeMillis() > timeout){
                TimeoutException e = new TimeoutException("" + type + " not received in time");
                e.printStackTrace();
                break;
            }
            Thread.sleep(50);
        }
    }


    /*----------------- Getters ---------------------------*/
    public NFCManager getNfcManager() {
        return nfcManager;
    }

    public Activity getActivity() {
        return activity;
    }

    public static Sync getInstance() {
        return instance;
    }

    public boolean getHostingStatus(){
        return nfcManager.getHostingStatus();
    }

    public void setHostingStatus(boolean b) {
        nfcManager.setHostingStatus(b);
    }


}
