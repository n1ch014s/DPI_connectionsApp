package com.unibas.socialconnections;

import android.app.Application;

public class SocialConnectionsApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        BouncyCastleSetup.register();
    }
}