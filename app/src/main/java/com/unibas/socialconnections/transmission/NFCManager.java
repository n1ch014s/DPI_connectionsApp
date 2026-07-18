package com.unibas.socialconnections.transmission;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 Establishing a connection with NFC. Manages the sending and receiving of data.

 */
public class NFCManager{
    private final Sync sync;
    private final NfcAdapter nfcAdapter;
    private boolean hosting = false;
    private boolean friendMode = false;
    //Activate on button press

    // Phone A turns on reader mode while B is HostApduService
        // ig these are their own modes the users select, like on WhatsApp showing your qr code
    // A requests getPublicKey
    // B Responds 32-byte public key
    // then reverse

    /**
     * NFC Manager Constructor.
     * @param sync The Sync Instance which parses data to and from
     * @param nfcAdapter The NFC adapter which is commands the hardware level
     */
    NFCManager(Sync sync, NfcAdapter nfcAdapter){
        this.sync = sync;
        this.nfcAdapter = nfcAdapter;
    }

    /**
     * Opens up hosting, only while Hosting == true does the device accept connections.
     * this runs on a timeout of 1 minute or until the host finds a reader.
     */
    public void startHost(){
        setHostingStatus(true);
        Toast.makeText(sync.getActivity(), "Hosting is opened", Toast.LENGTH_SHORT).show();
        Log.d("HOST", "Host opened");

        new Handler().postDelayed(() -> {
            if(getHostingStatus()) {
                setHostingStatus(false);
                Toast.makeText(sync.getActivity(), "Timeout: Hosting closed", Toast.LENGTH_SHORT).show();
                Log.d("HOST", "Host closed");
            }
        }, 60000);
    }

    /**
     * Starts the Client and therefore the Reader Mode to detect any nearby NFC tags.
     * Upon finding one it runs the callback which sends first the AID message and then the data, i.e. its pubkey and friends list.
     * TODO: should establish an iroh connection for MinDistance message and future updates
     */
    public void startClient(){

        Toast.makeText(sync.getActivity(), "Starting Client...", Toast.LENGTH_SHORT).show();
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
        Log.d("NFC", "Adapter = " + nfcAdapter);
        Log.d("NFC", "Enabled = " + nfcAdapter.isEnabled());

        nfcAdapter.enableReaderMode(
                    sync.getActivity(),
                tag -> { // Start of the callback, this is processed if and when a Tag is found

                    sync.getActivity().runOnUiThread(() ->
                            Toast.makeText(sync.getActivity(), "Tag found", Toast.LENGTH_SHORT).show()
                    );

                    IsoDep isoDep = IsoDep.get(tag);

                    if(isoDep == null){
                        Log.d("IsoDep", "no IsoDep!");
                        return;
                    }
                    try{
                        Log.d("NFC", "Tag discovered!: " + isoDep.getTag());
                        isoDep.connect();

                        byte[] connection = isoDep.transceive(selectAid);

                        if (connection[connection.length - 2] == (byte)0x90 &&
                                connection[connection.length - 1] == (byte)0x00) {
                            Log.d("NFC", "Connection Accepted");

                            String message = this.sync.processOutgoing();

                            // "tranceive" both sends data and then waits for a response
                            // therefore response is automatically the bytes that are responded from the host
                            byte[] response = isoDep.transceive(message.getBytes());
                            Log.d("NFC", "Repsonse Array:  " + Arrays.toString(response));

                            String reply = new String(response, StandardCharsets.UTF_8);
                            this.sync.processIncoming(reply);
                        }

                        isoDep.close();
                        nfcAdapter.disableReaderMode(sync.getActivity());

                    } catch (IOException e) {
                        sync.getActivity().runOnUiThread(() ->
                                Toast.makeText(sync.getActivity(), "An Error Occurred, please try again", Toast.LENGTH_SHORT).show()
                        );

                        throw new RuntimeException(e);

                    }
                },
                NfcAdapter.FLAG_READER_NFC_A |
                        NfcAdapter.FLAG_READER_NFC_B |
                        NfcAdapter.FLAG_READER_NFC_F |
                        NfcAdapter.FLAG_READER_NFC_V |
                        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
        );
        Log.d("NFC", "Reader mode enabled!");

    }

    /**
     * Returns the current hosting status of the Device
     * @return hosting status of device as a BOOLEAN
     */
    public boolean getHostingStatus(){
        return hosting;
    }


    /**
     * Allows the setting of the Hosting status of the device so that it can be shut off remotely.
     * @param b
     */
    public void setHostingStatus(boolean b) {
        hosting = b;
    }

    public void setFriendMode(boolean b) {
        friendMode = b;
    }

    public boolean getFriendStatus() {
        return friendMode;
    }
}