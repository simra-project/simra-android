package de.tuberlin.mcc.simra.app.net;

import de.tuberlin.mcc.simra.app.BuildConfig;

public class SimRAuthenticator {
    public static String getClientHash() {
        return BuildConfig.API_SECRET;
    }
}
