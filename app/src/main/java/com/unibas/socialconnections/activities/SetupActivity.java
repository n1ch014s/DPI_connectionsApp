package com.unibas.socialconnections.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.unibas.socialconnections.KeyManager;
import com.unibas.socialconnections.R;
import com.unibas.socialconnections.SetupPrefs;
import com.unibas.socialconnections.storage.GraphStorage;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import connections.GraphUtil;

public class SetupActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private EditText usernameInput;
    private Button continueButton;
    private ProgressBar progressBar;

    private volatile KeyPair generatedKeyPair = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        usernameInput = findViewById(R.id.usernameInput);
        continueButton = findViewById(R.id.continueButton);
        progressBar = findViewById(R.id.progressBar);

        continueButton.setEnabled(false); // disabled until key is ready

        // Start key generation right away, in the background
        executor.execute(() -> {
            try {
                KeyPair keyPair = KeyManager.generateKeyPair();
                generatedKeyPair = keyPair;
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    continueButton.setEnabled(true);
                });
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(this, "Key generation failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        });

        continueButton.setOnClickListener(v -> onContinueClicked());
    }

    private void onContinueClicked() {
        String username = usernameInput.getText().toString().trim();

        if (username.isEmpty()) {
            usernameInput.setError("Please enter a name");
            return;
        }

        if (generatedKeyPair == null) {
            // Shouldn't happen since button is disabled until ready, but guard anyway
            Toast.makeText(this, "Still generating your key, please wait", Toast.LENGTH_SHORT).show();
            return;
        }

        finishSetup(username, generatedKeyPair);
    }

    private void finishSetup(String username, KeyPair keyPair) {
        continueButton.setEnabled(false);

        GraphUtil graph = new GraphUtil(username, keyPair.getPublic(), (PrivateKey) keyPair.getPrivate());
        GraphStorage graphStorage = new GraphStorage(getApplicationContext()); // updated constructor
        graphStorage.setGraphUtil(graph); // NEW
        graphStorage.saveNode(graph.getUserNode());

        SetupPrefs.setSetupComplete(getApplicationContext());

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
