package com.unibas.socialconnections.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import connections.GraphUtil;
import connections.Node;

import com.unibas.socialconnections.KeyManager;
import com.unibas.socialconnections.R;
import com.unibas.socialconnections.SetupPrefs;
import com.unibas.socialconnections.storage.GraphStorage;
import com.unibas.socialconnections.transmission.NFCManager;
import com.unibas.socialconnections.transmission.Sync;


public class MainActivity extends AppCompatActivity {
    private NfcAdapter nfcAdapter;
    private NFCManager nfcManager;
    private Node userNode;
    private GraphUtil graph;

    /**
     * The OnCreate function is what is first run when the app is started and therefore generates all necessary things like the NFC Adapter and Manager
     * @param savedInstanceState the saved instance which is automatically passed upon creation
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SetupPrefs.isSetupComplete(this)) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
            Log.d("HCEfromMain", "HCE enabled");
            return;
        }

        loadGraphAndInitialize();
    }

    private void loadGraphAndInitialize() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        GraphStorage graphStorage = new GraphStorage(getApplicationContext());

        graphStorage.getStoredUsername(username -> {
            executor.execute(() -> {
                try {
                    KeyPair keyPair = KeyManager.getExistingKeyPair(getApplicationContext());
                    String finalUsername = (username != null) ? username : "Unknown";

                    GraphUtil graph = new GraphUtil(finalUsername, keyPair.getPublic(), (PrivateKey) keyPair.getPrivate());
                    graphStorage.setGraphUtil(graph);

                    graphStorage.loadGraphFromDatabase(() -> {
                        mainHandler.post(() -> {
                            this.graph = graph;
                            this.userNode = graph.getUserNode();
                            Sync sync = new Sync(graph, graphStorage, nfcAdapter, this, userNode);
                            this.nfcManager = sync.getNfcManager();
                            setupButtons();
                        });
                    });
                } catch (Exception e) {
                    mainHandler.post(() ->
                            Toast.makeText(this, "Failed to load your data: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
                }
            });
        });
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
        //nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }


    /**
     * sets up the buttons for the NFC connections
     */
    private void setupButtons(){
        Button hostButton = findViewById(R.id.hostButton);
        Button clientButton = findViewById(R.id.connectButton);
        Button clientFriend = findViewById(R.id.clientFriend);
        Button hostFriend = findViewById(R.id.hostFriend);

        hostButton.setOnClickListener(v -> {
            nfcManager.setFriendMode(false);
            nfcManager.startHost();

            Toast.makeText(this, "Waiting for connection...", Toast.LENGTH_SHORT).show();
        });

        hostFriend.setOnClickListener(v -> {
            nfcManager.setFriendMode(true);
            nfcManager.startHost();

            Toast.makeText(this, "Adding Friend: Host", Toast.LENGTH_SHORT).show();
        });

        clientButton.setOnClickListener(v -> {
            nfcManager.setFriendMode(false);
            nfcManager.startClient();

            Toast.makeText(this, "Connecting to Host...", Toast.LENGTH_SHORT).show();
        });

        clientFriend.setOnClickListener(v -> {
            nfcManager.setFriendMode(true);
            nfcManager.startClient();

            Toast.makeText(this, "Adding Friend: Client", Toast.LENGTH_SHORT).show();
        });

    }

}