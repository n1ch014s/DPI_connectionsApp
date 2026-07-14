package transmission;

import android.nfc.NfcAdapter;

/**
 Establishing a connection with NFC. Manages the sending and receiving of data.

 */
public class NFCManager{
    //Activate on button press

    // Phone A turns on reader mode while B is HostApduService
        // ig these are their own modes the users select, like on WhatsApp showing your qr code
    // A requests getPublicKey
    // B Responds 32-byte public key
    // then reverse
    NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);

    public interface Callback {
        void onDataReceived(String json);
    }
    private Callback callback;

    NFCManager(){
        setCallback(callback);
    }


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