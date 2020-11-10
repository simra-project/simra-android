package de.tuberlin.mcc.simra.app.util;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SimRAuthenticator {
    public static final SimpleDateFormat DATE_PATTERN_SHORT = new SimpleDateFormat("dd.MM.yyyy");

    public static String getClientHash(Context context) {
        Date dateToday = new Date();
        String hashSuffix = "";
        try {
            InputStream hashInput = new BufferedInputStream(context.getResources().getAssets().open("hash-suffix.h"));
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = hashInput.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            // StandardCharsets.UTF_8.name() > JDK 7
            hashSuffix =  result.toString("UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Integer.toHexString((DATE_PATTERN_SHORT.format(dateToday) + hashSuffix).hashCode());
    }
}
