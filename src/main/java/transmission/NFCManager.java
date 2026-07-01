package transmission;

import android.nfc.NfcAdapter;

/**
 Establishing a connection with NFC. Manages the sending and receiving of data.

 */
public class NFCManager{
    //Activate on button press

    // Phone A turns on reader mode while B is HostApduService
        // ig these are their own modes the users select, like on whatsapp showing your qr code
    // A requests getPublicKey
    // B Responds 32-byte public key
    // then reverse

    public interface Callback {
        void onDataReceived(String json);
    }

    private Callback callback;

    public void setCallback(Callback callback){
        this.callback = callback;
    }

    public void handleIncomingData(String json){
        if(callback != null){
            callback.onDataReceived(json);
        }
    }

    public void handleOutgoingData(String msg){
        //TODO
    }

}