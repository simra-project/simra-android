package de.tuberlin.mcc.simra.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.util.Arrays;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.services.UploadService;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import de.tuberlin.mcc.simra.app.util.PermissionHelper;
import de.tuberlin.mcc.simra.app.util.VersionUpdater;

import static de.tuberlin.mcc.simra.app.util.LogHelper.showDataDirectory;
import static de.tuberlin.mcc.simra.app.util.LogHelper.showKeyPrefs;
import static de.tuberlin.mcc.simra.app.util.LogHelper.showMetaDataFile;
import static de.tuberlin.mcc.simra.app.util.LogHelper.showMetadata;
import static de.tuberlin.mcc.simra.app.util.LogHelper.showStatistics;
import static de.tuberlin.mcc.simra.app.util.SharedPref.lookUpBooleanSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.SharedPref.lookUpSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.SharedPref.writeBooleanToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.SharedPref.writeToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.deleteErrorLogsForVersion;
import static de.tuberlin.mcc.simra.app.util.Utils.fileExists;
import static de.tuberlin.mcc.simra.app.util.Utils.getAppVersionNumber;
import static de.tuberlin.mcc.simra.app.util.Utils.overWriteFile;

/**
 * Shows general info about the app, if the app is run the first time.
 * If not, MainActivity is started, except if a crash happened before.
 */

public class StartActivity extends BaseActivity {

    // Log tag
    private static final String TAG = "StartActivity_LOG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // Just some Logs
        Log.d(TAG, "getFilesDir(): " + Arrays.toString(new File(getFilesDir(), "../shared_prefs").listFiles()));
        Log.d(TAG, "onCreate() started");
        showKeyPrefs(this);
        showDataDirectory(this);
        showMetadata(this);
        showMetaDataFile(this);
        showStatistics(this);
        deleteErrorLogsForVersion(this, 26);

        // IMPORTANT: Do not remove!
        VersionUpdater.migrate(this);

        // Check Permissions

        navigateIfAllPermissionsGranted();

        Button next = findViewById(R.id.nextBtn);
        next.setOnClickListener(v -> {
            navigateIfAllPermissionsGranted();
        });

        isFirstTime();
    }

    public boolean allPermissionGranted() {
        if (
                privacyPolicyAccepted()
                        && !showUnsentErrorDialogPermitted()
                        && PermissionHelper.hasBasePermissions(this)
        ) {
            return true;
        }
        return false;
    }

    public void navigateIfAllPermissionsGranted() {
        if (allPermissionGranted()) {
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            startActivity(intent);
            // Call finish() to prevent going back to StartActivity,
            // when the Back Button is pressed in MainActivity
            finish();
        } else {
            PermissionHelper.requestFirstBasePermissionsNotGranted(StartActivity.this);
        }
    }


    /**
     * Checks if the user is opening the app for the first time.
     * Note that this method should be placed inside an activity and it can be called multiple times.
     *
     * @return boolean
     */
    private boolean isFirstTime() {
        boolean firstTime = lookUpBooleanSharedPrefs("firstTime", true, "simraPrefs", this);
        if (firstTime) {
            writeBooleanToSharedPrefs("firstTime", false, "simraPrefs", this);
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // META-FILE (one per user): contains ...
            // * the information required to display rides in the ride history (See RecorderService)
            //   (DATE,START TIME, END TIME, ANNOTATED TRUE/FALSE)
            // * the RIDE KEY which allows to identify the file containing the complete data for
            //   a ride. => Use case: user wants to view a ride from history - retrieve data
            // * one meta file per user, so we only want to create it if it doesn't exist yet.
            //   (fileExists and appendToFile can be found in the Utils.java class)
            if ((!fileExists("metaData.csv", this)) || (new File(getFilesDir() + "/metaData.csv").length() == 0)) {
                String fileInfoLine = getAppVersionNumber(this) + "#1" + System.lineSeparator();
                Log.d(TAG, "firstTime. Creating metaData.csv");
                overWriteFile((fileInfoLine + "key, startTime, endTime, annotated, distance, waitTime"
                        + System.lineSeparator()), "metaData.csv", this);

            }
        }
        return firstTime;
    }

    private boolean privacyPolicyAccepted() {
        boolean accepted = lookUpBooleanSharedPrefs("Privacy-Policy-Accepted", false, "simraPrefs", this);
        if (!accepted) {
            firePrivacyDialog();
        }
        return accepted;
    }

    // Look up whether to ask the user for a permission to
    // send the crash logs to the server. If the user gives permission, upload the crash report(s).
    private boolean showUnsentErrorDialogPermitted() {
        String crashSendState = lookUpSharedPrefs("SEND-CRASH", "UNKNOWN", "simraPrefs", this);
        boolean newCrash = lookUpBooleanSharedPrefs("NEW-UNSENT-ERROR", false, "simraPrefs", this);
        if (crashSendState.equals("UNKNOWN") && newCrash) {
            fireSendErrorDialog();
            return true;
        } else if (crashSendState.equals("ALWAYS-SEND") && newCrash) {
            Intent intent = new Intent(StartActivity.this, UploadService.class);
            intent.putExtra("CRASH_REPORT", true);
            startService(intent);
            return false;
        } else {
            return false;
        }
    }

    public void fireSendErrorDialog() {

        View checkBoxView = View.inflate(this, R.layout.checkbox, null);
        CheckBox checkBox = checkBoxView.findViewById(R.id.checkbox);
        final boolean[] rememberChoice = {false};
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> rememberChoice[0] = isChecked);
        checkBox.setText(getString(R.string.rememberMyChoice));
        AlertDialog.Builder alert = new AlertDialog.Builder(StartActivity.this);
        alert.setTitle(getString(R.string.sendErrorTitle));
        alert.setMessage(getString(R.string.sendErrorMessage));
        alert.setView(checkBoxView);
        alert.setPositiveButton(R.string.yes, (dialog, id) -> {
            if (rememberChoice[0]) {
                writeToSharedPrefs("SEND-CRASH", "ALWAYS-SEND", "simraPrefs", StartActivity.this);
            }
            Intent intent = new Intent(StartActivity.this, UploadService.class);
            intent.putExtra("CRASH_REPORT", true);
            startService(intent);
        });
        alert.setNegativeButton(R.string.no, (dialog, id) -> {
            if (rememberChoice[0]) {
                writeToSharedPrefs("SEND-CRASH", "NEVER-SEND", "simraPrefs", StartActivity.this);
            }
        });
        alert.show();
    }


    public void firePrivacyDialog() {
        View checkBoxView = View.inflate(this, R.layout.checkbox, null);
        CheckBox checkBox = checkBoxView.findViewById(R.id.checkbox);

        checkBox.setText(getString(R.string.iAccept));
        AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);

        // Linkify the message
        builder.setTitle(getString(R.string.privacyAgreementTitle));
        builder.setMessage(getResources().getText(R.string.privacyAgreementMessage));
        builder.setView(checkBoxView);
        builder.setPositiveButton(R.string.next, (dialog, id) -> writeBooleanToSharedPrefs("Privacy-Policy-Accepted", checkBox.isChecked(), "simraPrefs", StartActivity.this));
        builder.setNegativeButton(R.string.close_simra, (dialog, id) -> {
            writeBooleanToSharedPrefs("firstTime", true, "simraPrefs", StartActivity.this);
            finish();
            Toast.makeText(StartActivity.this, getString(R.string.simra_closed), Toast.LENGTH_LONG).show();
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
        ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());

        // Initially disable the button
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isChecked));
    }

}
