package de.tuberlin.mcc.simra.app.net;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.tuberlin.mcc.simra.app.BuildConfig;

public class SimRAuthenticator {
    public static final SimpleDateFormat DATE_PATTERN_SHORT = new SimpleDateFormat("dd.MM.yyyy");

    public static String getClientHash() {
        Date dateToday = new Date();
        return Integer.toHexString((DATE_PATTERN_SHORT.format(dateToday) + BuildConfig.API_SECRET).hashCode());
    }
}
