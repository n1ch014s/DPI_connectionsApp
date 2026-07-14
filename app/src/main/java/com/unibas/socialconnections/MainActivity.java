package com.unibas.socialconnections;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import java.security.PrivateKey;
import java.security.PublicKey;

import connections.GraphUtil;
import connections.Node;

import com.unibas.socialconnections.transmission.NFCManager;
import com.unibas.socialconnections.transmission.Sync;


public class MainActivity extends AppCompatActivity {
    private NfcAdapter nfcAdapter;
    private NFCManager nfcManager;
    private Node userNode;


    /**
     * The OnCreate function is what is first run when the app is started and therefore generates all necessary things like the NFC Adapter and Manager
     * @param savedInstanceState the saved instance which is automatically passed upon creation
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
        }

        //These are temporary until we can import them fully from the system
        PublicKey key = new PublicKey() {
            @Override
            public String getAlgorithm() {
                return "";
            }

            @Override
            public String getFormat() {
                return "";
            }

            @Override
            public byte[] getEncoded() {
                return new byte[0];
            }
        };

        PrivateKey pkey = new PrivateKey() {
            @Override
            public String getAlgorithm() {
                return "";
            }

            @Override
            public String getFormat() {
                return "";
            }

            @Override
            public byte[] getEncoded() {
                return new byte[0];
            }
        };

        GraphUtil graph = new GraphUtil("default", key, pkey);
        Sync sync = new Sync(graph, nfcAdapter, this);
        this.nfcManager = sync.getNfcManager();

        setupButtons();
    }

    /**
     * OnResume is what is run every time the (previously created) App is started up again after an interruption.
     *
     */
    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }


    /**
     * sets up the buttons for the NFC connections
     */
    private void setupButtons(){
        Button hostButton = findViewById(R.id.hostButton);
        Button clientButton = findViewById(R.id.connectButton);


        hostButton.setOnClickListener(v -> {
            nfcManager.startHost();

            Toast.makeText(this, "Waiting for connection", Toast.LENGTH_SHORT).show();
        });

        clientButton.setOnClickListener(v -> {
            nfcManager.startClient();

            Toast.makeText(this, "Connecting to Host", Toast.LENGTH_SHORT).show();
        });
    }


}