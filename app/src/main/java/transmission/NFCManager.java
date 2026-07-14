package transmission;

/**
 Establishing a connection with NFC. Manages the sending and receiving of data.

 */
public class NFCManager{
    private final Sync sync;
    //Activate on button press

    // Phone A turns on reader mode while B is HostApduService
        // ig these are their own modes the users select, like on WhatsApp showing your qr code
    // A requests getPublicKey
    // B Responds 32-byte public key
    // then reverse

    NFCManager(Sync sync){
        this.sync = sync;
    }

    public void startHost(){

    }

    public void startClient(){

    }


    public void receive(String json){
        this.sync.processIncoming(json);

    }

    public void send(String msg){
        //SEND
    }


}