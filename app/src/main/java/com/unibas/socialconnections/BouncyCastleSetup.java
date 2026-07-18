package com.unibas.socialconnections;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;

public class BouncyCastleSetup {
    public static void register() {
        if (Security.getProvider("BC") == null) {
            Security.insertProviderAt(new BouncyCastleProvider(), 1);
        }
    }
}