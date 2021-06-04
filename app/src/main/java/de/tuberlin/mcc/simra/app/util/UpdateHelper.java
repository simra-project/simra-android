package de.tuberlin.mcc.simra.app.util;
import android.content.Context;
import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.update.VersionUpdater;

public class UpdateHelper {

    /**
     * Migrate Shared Prefs Data from previous Versions to the current
     *
     * @param context
     */
    public static void migrate(Context context) {
        int lastAppVersion = SharedPref.lookUpIntSharedPrefs("App-Version", -1, "simraPrefs", context);
        VersionUpdater.updateToV27(context, lastAppVersion);
        VersionUpdater.updateToV30(context, lastAppVersion);
        VersionUpdater.updateToV31(context, lastAppVersion);
        VersionUpdater.updateToV32(context, lastAppVersion);
        VersionUpdater.updateToV39(context, lastAppVersion);
        VersionUpdater.updateToV50(context, lastAppVersion);
        VersionUpdater.updateToV52(context, lastAppVersion);
        VersionUpdater.updateToV58(context, lastAppVersion);
        SharedPref.writeIntToSharedPrefs("App-Version", BuildConfig.VERSION_CODE, "simraPrefs", context);
    }
}
