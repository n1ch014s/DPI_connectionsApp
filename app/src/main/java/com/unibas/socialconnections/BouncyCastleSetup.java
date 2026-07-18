package com.unibas.socialconnections;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;

public class BouncyCastleSetup {
    public static void register() {
        Security.removeProvider("BC");
        Security.insertProviderAt(new BouncyCastleProvider(), 1);

    }
}