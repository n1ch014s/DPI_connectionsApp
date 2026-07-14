package com.unibas.socialconnections.transmission;


import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;

public class AppHostApduService extends HostApduService {

    @Override
    public byte[] processCommandApdu(byte[] command, Bundle bundle) {

        Sync sync = Sync.getInstance();

        if(sync != null && sync.getHostingStatus()) {
            // Client sends data
            String data = new String(command);
            sync.processIncoming(data);

            // Send response
            String response = sync.processOutgoing();
            return response.getBytes();
        }
        else {
            return new byte[]{(byte) 0x69, (byte) 0x85};
        }
    }

    @Override
    public void onDeactivated(int i) {

    }
}
