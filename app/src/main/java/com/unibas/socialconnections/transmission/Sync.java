package com.unibas.socialconnections.transmission;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.nfc.NfcAdapter;

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
    private final Activity activity;
    private final Node userNode;
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

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePublic(keySpec);
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
