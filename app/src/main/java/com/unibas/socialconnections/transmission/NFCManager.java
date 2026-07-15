package com.unibas.socialconnections.transmission;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

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
        Toast.makeText(sync.getActivity(), "hosting = true", Toast.LENGTH_SHORT).show();
        Log.d("HOST", "Host opened");

        new Handler().postDelayed(() -> {
            hosting = false;
            Toast.makeText(sync.getActivity(), "hosting = false", Toast.LENGTH_SHORT).show();
            Log.d("HOST", "Host closed");
        }, 60000);
    }

    public void startClient(){

        Toast.makeText(sync.getActivity(), "starting client", Toast.LENGTH_SHORT).show();
        byte[] selectAid = new byte[] {
                (byte) 0x00,
                (byte) 0xA4,
                (byte) 0x04,
                (byte) 0x00,
                (byte) 0x07,
                (byte) 0xF0,
                (byte) 0x01,
                (byte) 0x02,
                (byte) 0x03,
                (byte) 0x04,
                (byte) 0x05,
                (byte) 0x06,
                (byte) 0x00
        };

        Log.d("NFC", "starting reader mode!");

        nfcAdapter.enableReaderMode(
                    sync.getActivity(),
                tag -> {

                    Log.d("NFC", "Tag discovered!");
                    Toast.makeText(sync.getActivity(), "Tag found", Toast.LENGTH_SHORT).show();
                    IsoDep isoDep = IsoDep.get(tag);

                    if(isoDep == null){
                        Log.d("IsoDep", "no IsoDep!");
                        Toast.makeText(sync.getActivity(), "IsoDep not supported", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try{
                        Log.d("IsoDep", "IsoDep exists!");
                        Toast.makeText(sync.getActivity(), "IsoDep trying to connect...", Toast.LENGTH_SHORT).show();
                        isoDep.connect();

                        byte[] connection = isoDep.transceive(selectAid);
                        Toast.makeText(sync.getActivity(), "connected", Toast.LENGTH_SHORT).show();


                        String message = this.sync.processOutgoing();
                        Toast.makeText(sync.getActivity(), "processed outgoing", Toast.LENGTH_SHORT).show();

                        // tranceive both sends data and then waits for a response
                        // therefore response is automatically the bytes that are responded from the host
                        byte[] response = isoDep.transceive(message.getBytes());
                        Toast.makeText(sync.getActivity(), "Tranceived", Toast.LENGTH_SHORT).show();


                        String reply = new String(response);
                        this.sync.processIncoming(reply);
                        Toast.makeText(sync.getActivity(), "incoming processed", Toast.LENGTH_SHORT).show();


                        isoDep.close();

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null
        );
        Log.d("NFC", "Reader mode enabled!");

    }

    public boolean getHostingStatus(){
        return hosting;
    }


    public void setHostingStatus(boolean b) {
        hosting = b;
    }
}