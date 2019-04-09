package de.tuberlin.mcc.simra.app;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SimRAuthenticator {
    public static final SimpleDateFormat DATE_PATTERN_SHORT = new SimpleDateFormat("dd.MM.yyyy");
    public static final String UPLOAD_HASH_SUFFIX = "Ytjn5yv5xax6Dbhj";

    public static String getClientHash() {
        Date dateToday = new Date();
        return Integer.toHexString((DATE_PATTERN_SHORT.format(dateToday) + UPLOAD_HASH_SUFFIX).hashCode());
    }
}
