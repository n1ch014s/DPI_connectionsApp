package com.unibas.socialconnections;

import android.content.Context;

public class SetupPrefs {
    private static final String PREFS_NAME = "setup_prefs";
    private static final String KEY_SETUP_COMPLETE = "setup_complete";

    public static boolean isSetupComplete(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SETUP_COMPLETE, false);
    }

    public static void setSetupComplete(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SETUP_COMPLETE, true)
                .apply();
    }
}
