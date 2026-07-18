package com.unibas.socialconnections;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyManager {
    private static final String PREFS_NAME = "secure_key_prefs";
    private static final String KEY_PRIVATE = "ed25519_private";
    private static final String KEY_PUBLIC = "ed25519_public";

    private static SharedPreferences getEncryptedPrefs(Context context) throws Exception {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    public static boolean keyExists(Context context) throws Exception {
        SharedPreferences prefs = getEncryptedPrefs(context);
        return prefs.contains(KEY_PRIVATE) && prefs.contains(KEY_PUBLIC);
    }

    public static KeyPair generateKeyPair(Context context) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519", "BC");
        KeyPair keyPair = kpg.generateKeyPair();
        Log.d("KEY", "Generated Keys");
        Log.d("KEY", "Public: " + keyPair.getPublic());
        Log.d("KEY", "Private: " + keyPair.getPrivate());

        SharedPreferences prefs = getEncryptedPrefs(context);
        prefs.edit()
                .putString(KEY_PRIVATE, Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()))
                .putString(KEY_PUBLIC, Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()))
                .apply();

        return keyPair;
    }

    public static KeyPair getExistingKeyPair(Context context) throws Exception {
        SharedPreferences prefs = getEncryptedPrefs(context);

        String privB64 = prefs.getString(KEY_PRIVATE, null);
        String pubB64 = prefs.getString(KEY_PUBLIC, null);
        Log.d("KEY", "retrieved: " + privB64 + " and " + pubB64);

        if (privB64 == null || pubB64 == null) {
            Log.d("KEY", "NO KEYS FOUND");
            throw new IllegalStateException("No key pair found — call generateKeyPair first");
        }

        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", "BC");

        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privB64));
        PrivateKey privateKey = keyFactory.generatePrivate(privSpec);

        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(Base64.getDecoder().decode(pubB64));
        PublicKey publicKey = keyFactory.generatePublic(pubSpec);

        return new KeyPair(publicKey, privateKey);
    }

    public static byte[] getIrohSecretKey(PrivateKey privateKey) {

        byte[] encoded = privateKey.getEncoded();

        // Ed25519 PKCS#8 stores the 32 byte seed at the end
        byte[] seed = new byte[32];

        System.arraycopy(
                encoded,
                encoded.length - 32,
                seed,
                0,
                32
        );

        return seed;
    }
}