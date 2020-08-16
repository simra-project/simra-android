package de.tuberlin.mcc.simra.app.util;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;

import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.update.VersionUpdater;

public class UpdateHelper {
    private static final Integer UPDATE_REQUEST_CODE = 44;
    private static int HIGH_PRIORITY_UPDATE = 4;
    private static InstallStateUpdatedListener installStateUpdatedListener;
    private static boolean displayed = false;

    public static void checkForUpdates(Activity activity) {
        if (displayed) {
            return;
        }
        displayed = true;
        // Creates instance of the manager.
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(activity);

        installStateUpdatedListener = state -> {
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                // After the update is downloaded, show a notification
                // and request user confirmation to restart the app.
                popupSnackbarForCompleteUpdate(appUpdateManager, activity);
                if (installStateUpdatedListener != null) {
                    appUpdateManager.unregisterListener(installStateUpdatedListener);
                }
            }
        };

        // Before starting an update, register a listener for updates.
        appUpdateManager.registerListener(installStateUpdatedListener);

        // Checks that the platform will allow the specified type of update.
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            // Check if there is an outstanding update to complete (for Flexible Update Strategy)
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackbarForCompleteUpdate(appUpdateManager, activity);
                return;
            }
            // Check if there is an outstanding update to complete (for Immediate Update Strategy)
            if (appUpdateInfo.updateAvailability()
                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            activity,
                            UPDATE_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
                return;
            }

            // Check if the update should be done immediately or in the background.
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.updatePriority() < HIGH_PRIORITY_UPDATE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.FLEXIBLE,
                            activity,
                            UPDATE_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.updatePriority() >= HIGH_PRIORITY_UPDATE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            activity,
                            UPDATE_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        });
    }

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

    private static void popupSnackbarForCompleteUpdate(AppUpdateManager appUpdateManager, Activity activity) {
        Snackbar snackbar = Snackbar.make(
                activity.getWindow().getDecorView().findViewById(android.R.id.content),
                "Update ready to install.", Snackbar.LENGTH_INDEFINITE
        );
        snackbar.setAction("RESTART", view -> appUpdateManager.completeUpdate());
        snackbar.setActionTextColor(ContextCompat.getColor(activity, R.color.colorAccent));
        snackbar.show();
    }
}
