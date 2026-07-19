package com.unibas.socialconnections.transmission;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

public class AppHostApduService extends HostApduService {

    private static final String TAG = "HCE_SERVICE";

    @Override
    public void onCreate() {
        super.onCreate();
        //Log.d(TAG, "HCE service created");
    }

    @Override
    public byte[] processCommandApdu(byte[] command, Bundle bundle) {
        //Log.d("APDU", "Service started");

        if(isSelectAid(command)) {
            //Log.d("HCE", "Client selected app");

            return new byte[]{
                    (byte) 0x90,
                    (byte) 0x00
            };
        }

        Sync sync = Sync.getInstance();

        //Log.d("APDU", "found instance: " + sync);
        //Log.d("APDU", "Hosting Status: " + sync.getHostingStatus());

        if(sync != null && sync.getHostingStatus()) {
            //Log.d("HCE", "processcommand was called APDU service");

            // Client sends data

            String data = new String(command);

            new Thread(() -> {
                sync.processIncoming(data);
            }).start();

            // Send response
            String response = sync.processOutgoing();

            return response.getBytes();
        }
        else {
            return new byte[]{(byte) 0x69, (byte) 0x85};
        }
    }

    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "HCE deactivated: " + reason);
    }

    private boolean isSelectAid(byte[] command){
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
                (byte) 0x06
        };

        if (command.length < selectAid.length) {
            return false;
        }

        for (int i = 0; i < selectAid.length; i++) {
            if (command[i] != selectAid[i]) {
                return false;
            }
        }

        return true;
    }
}
