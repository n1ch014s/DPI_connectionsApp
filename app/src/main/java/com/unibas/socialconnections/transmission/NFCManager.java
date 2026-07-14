package com.unibas.socialconnections.transmission;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.os.Handler;

import java.io.IOException;

/**
 Establishing a connection with NFC. Manages the sending and receiving of data.

 */
public class NFCManager{
    private final Sync sync;
    private final NfcAdapter nfcAdapter;
    private boolean hosting = false;
    //Activate on button press

    // Phone A turns on reader mode while B is HostApduService
        // ig these are their own modes the users select, like on WhatsApp showing your qr code
    // A requests getPublicKey
    // B Responds 32-byte public key
    // then reverse

    NFCManager(Sync sync, NfcAdapter nfcAdapter){
        this.sync = sync;
        this.nfcAdapter = nfcAdapter;
    }

    public void startHost(){
        hosting = true;

        new Handler().postDelayed(() -> {
            hosting = false;
        }, 60000);
    }

    public void startClient(){

        nfcAdapter.enableReaderMode(
                    sync.getActivity(),
                tag -> {
                    IsoDep isoDep = IsoDep.get(tag);

                    try{
                        isoDep.connect();

                        String message = this.sync.processOutgoing();

                        // tranceive both sends data and then waits for a response
                        // therefore response is automatically the bytes that are responded from the host
                        byte[] response = isoDep.transceive(message.getBytes());


                        String reply = new String(response);
                        this.sync.processIncoming(reply);

                        isoDep.close();

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                NfcAdapter.FLAG_READER_NFC_A, null
        );
    }


    public void receive(String json){
        this.sync.processIncoming(json);

    }

    public void send(String msg){
        //SEND
    }

    public boolean getHostingStatus(){
        return hosting;
    }


}