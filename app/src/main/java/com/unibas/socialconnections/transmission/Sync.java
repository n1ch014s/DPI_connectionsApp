package com.unibas.socialconnections.transmission;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.nfc.NfcAdapter;
import android.util.Log;
import android.content.Intent;

import com.unibas.socialconnections.GUI.PathGraphBuilder;
import com.unibas.socialconnections.GUI.PathHolder;
import com.unibas.socialconnections.activities.PathActivity;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;

import connections.GraphUtil;
import connections.KeyDistTuple;
import connections.Node;

/** The Peer to Peer synchronisation.
 *
 */
public class Sync{
    private final GraphUtil graph;
    private final NFCManager nfcManager;
    private final Activity activity;
    private final Node userNode;
    private final IrohManager irohManager;
    private static Sync instance;

    /**
     * Creates a Sync Manager which controls and processing of incoming and outgoing nodes
     * @param graph the graph which contains the friend data
     */
    public Sync(GraphUtil graph, NfcAdapter nfcAdapter, Activity activity, Node userNode){
        this.graph = graph;
        this.activity = activity;
        this.userNode = userNode;
        this.nfcManager = new NFCManager(this, nfcAdapter);
        this.irohManager = new IrohManager();
        irohManager.start();

    }

    /**
     * Parses and Processes incoming JSON data, turning them into nodes
     * @param info the incoming data, this is turned into a node with a friend list
     */
    public void processIncoming(String info) throws Exception {
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
        String friendMode = data[2];
        String[] friendPubs = data[3].split("\\|");

        if(friendMode.equals("1")) {
            graph.addFriendToUser(pub, name);
            //TODO:   irohManager.gossip(data[0]);
            Log.d("Iroh", "Gossip Established!");
        }else{
            //TODO: reveal name
            irohManager.connect(data[0]);
            Log.d("Iroh", "Connection Established!");
            KeyDistTuple[] nodeList = graph.getList();
            String encodedNodeList = encodeKeyDistList(nodeList);
            irohManager.send(encodedNodeList.getBytes(StandardCharsets.UTF_8));

            String recvNodeListStr = new String(irohManager.receive(), StandardCharsets.UTF_8);
            LinkedList<PublicKey[]> minPaths = graph.getMinPaths(decodeKeyDistList(recvNodeListStr));
            String encodedMinPaths = encodePaths(minPaths);
            irohManager.send(encodedMinPaths.getBytes(StandardCharsets.UTF_8));

            String encodedRecvMinPaths = new String(irohManager.receive(), StandardCharsets.UTF_8);
            LinkedList<PublicKey[]> filledMinPaths = graph.fillMinPaths(decodePaths(encodedRecvMinPaths), pub);
            PathGraphBuilder.GraphData graphData = PathGraphBuilder.build(filledMinPaths, graph, name);

            PathHolder.pendingData = graphData;
            activity.runOnUiThread(() -> {
                Intent intent = new Intent(activity, PathActivity.class);
                activity.startActivity(intent);
            });
        }

    }

    /**
     * Process the outgoing messages by turning the usernode and the friendslist into a string which can be transmitted
     * @return returns the message string generated from the usernode
     */
    public String processOutgoing(){
        PublicKey[] nodeList = graph.getFriendsList();
        StringBuilder builder = new StringBuilder();
        String pubkeyString = Base64.getEncoder().encodeToString(userNode.getPublicKey().getEncoded());
        builder.append(pubkeyString);
        builder.append("||");
        builder.append(userNode.getName());
        builder.append("||");
        if(nfcManager.getFriendStatus()){
            builder.append("1");
        } else{
            builder.append("0");
        }
        builder.append("||");
        for (PublicKey pk : nodeList) {
            String pkString = Base64.getEncoder().encodeToString(pk.getEncoded());
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
    public PublicKey decodeString(String pubString) throws Exception{
        byte[] publicKeyBytes = Base64.getDecoder().decode(pubString.getBytes(StandardCharsets.UTF_8));

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);

        KeyFactory keyFactory = KeyFactory.getInstance("EC");

        return keyFactory.generatePublic(keySpec);
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

        KeyFactory keyFactory = KeyFactory.getInstance("EC"); // matches your decodeString method

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

        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        String[] pathStrings = data.split(";");

        for (String pathStr : pathStrings) {
            String[] keyStrings = pathStr.split("\\|");
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
        return result;
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
