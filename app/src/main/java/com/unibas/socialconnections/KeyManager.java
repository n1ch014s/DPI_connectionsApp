package com.unibas.socialconnections;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;

public class KeyManager {
    private static final String ALIAS = "user_signing_key";

    public static boolean keyExists() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        return keyStore.containsAlias(ALIAS);
    }

    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
        kpg.initialize(new KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build());
        return kpg.generateKeyPair();
    }

    public static KeyPair getExistingKeyPair() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(ALIAS, null);
        PublicKey publicKey = keyStore.getCertificate(ALIAS).getPublicKey();
        return new KeyPair(publicKey, privateKey);
    }
}
