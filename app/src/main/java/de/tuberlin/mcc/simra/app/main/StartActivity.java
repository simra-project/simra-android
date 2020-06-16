package de.tuberlin.mcc.simra.app.main;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.Arrays;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.net.UploadService;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import de.tuberlin.mcc.simra.app.util.Utils;

import static de.tuberlin.mcc.simra.app.util.Utils.deleteErrorLogsForVersion;
import static de.tuberlin.mcc.simra.app.util.Utils.fileExists;
import static de.tuberlin.mcc.simra.app.util.Utils.getAppVersionNumber;
import static de.tuberlin.mcc.simra.app.util.Utils.lookUpBooleanSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.lookUpIntSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.lookUpSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.overWriteFile;
import static de.tuberlin.mcc.simra.app.util.Utils.readContentFromFile;
import static de.tuberlin.mcc.simra.app.util.Utils.showDataDirectory;
import static de.tuberlin.mcc.simra.app.util.Utils.showKeyPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.showMetadata;
import static de.tuberlin.mcc.simra.app.util.Utils.showStatistics;
import static de.tuberlin.mcc.simra.app.util.Utils.writeBooleanToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.writeIntToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.writeLongToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.writeToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.VersionUpdater.updateToV27;
import static de.tuberlin.mcc.simra.app.util.VersionUpdater.updateToV30;
import static de.tuberlin.mcc.simra.app.util.VersionUpdater.updateToV31;
import static de.tuberlin.mcc.simra.app.util.VersionUpdater.updateToV32;
import static de.tuberlin.mcc.simra.app.util.VersionUpdater.updateToV39;
import static de.tuberlin.mcc.simra.app.util.VersionUpdater.updateToV50;
import static de.tuberlin.mcc.simra.app.util.VersionUpdater.updateToV52;
import static de.tuberlin.mcc.simra.app.util.VersionUpdater.updateToV58;

/**
 * Shows general info about the app, if the app is run the first time.
 * If not, MainActivity is started, except if a crash happened before.
 */

public class StartActivity extends BaseActivity {

    // Log tag
    private static final String TAG = "StartActivity_LOG";
    Button next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        Log.d(TAG, "getFilesDir(): " + Arrays.toString(new File(getFilesDir(), "../shared_prefs").listFiles()));
        Log.d(TAG, "onCreate() started");
        // writeIntToSharedPrefs("App-Version", getAppVersionNumber(this), "simraPrefs", this);
        showKeyPrefs(this);
        showDataDirectory(this);
        showMetadata(this);
        Log.d(TAG, "===========================V=metaData=V===========================");
        Log.d(TAG, "metaData.csv: " + readContentFromFile("metaData.csv", this));
        Log.d(TAG, "===========================Λ=metaData=Λ===========================");
        showStatistics(this);
        deleteErrorLogsForVersion(this, 26);
        int lastAppVersion = lookUpIntSharedPrefs("App-Version", -1, "simraPrefs", this);

        updateToV27(this, lastAppVersion);
        updateToV30(this, lastAppVersion);
        updateToV31(this, lastAppVersion);
        updateToV32(this, lastAppVersion);
        updateToV39(this, lastAppVersion);
        updateToV50(this, lastAppVersion);
        updateToV52(this, lastAppVersion);
        updateToV58(this, lastAppVersion);
        writeIntToSharedPrefs("App-Version", getAppVersionNumber(this), "simraPrefs", this);

        // For permission request
        int LOCATION_ACCESS_CODE = 1;
        String[] locationPermissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION};
        Utils.permissionRequest(StartActivity.this, locationPermissions, StartActivity.this.getString(R.string.locationPermissionRequestRationale), LOCATION_ACCESS_CODE);
        // Android 10 and above need external storage rights to show OSM
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int STORAGE_ACCESS_CODE = 2;
            String[] storagePermissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            Utils.permissionRequest(StartActivity.this, storagePermissions, StartActivity.this.getString(R.string.storagePermissionRequestRationale), STORAGE_ACCESS_CODE);
        }

        if ((!isFirstTime()) & (privacyPolicyAccepted()) & (!showUnsentErrorDialogPermitted()) && (ContextCompat.checkSelfPermission(StartActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || (ContextCompat.checkSelfPermission(StartActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED))) {
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            // start MainActivity when Button is clicked
            next = findViewById(R.id.nextBtn);
            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(StartActivity.this, MainActivity.class);
                    // remove the Callback of the Runnable to the Handler to prevent second start of
                    // MainActivity
                    startActivity(intent);
                    // finish() to prevent going back to StartActivity, when the Back Button is pressed
                    // in MainActivity
                    finish();
                }
            });
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
            /*
            Log.d(TAG, "firstTime. Creating profile.csv");
            if (!fileExists("profile.csv", this)) {
                String fileInfoLine = getAppVersionNumber(this) + "#1" + System.lineSeparator();

                overWriteFile((fileInfoLine + PROFILE_HEADER), "profile.csv", this);

            }
            */

            // Write the default values for privacy duration and distance. These values are
            // used to determine whether a ride should be saved or not.

            // don't start to record the ride, until user is 30 meters away
            // from his starting position.
            writeLongToSharedPrefs("Privacy-Duration", 30, "simraPrefs", this);
            // don't start to record the ride, until user 30 seconds passed
            // from recording start time.
            writeIntToSharedPrefs("Privacy-Distance", 30, "simraPrefs", this);
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
        CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
        final boolean[] rememberChoice = {false};
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                rememberChoice[0] = isChecked;
            }
        });
        checkBox.setText(getString(R.string.rememberMyChoice));
        AlertDialog.Builder alert = new AlertDialog.Builder(StartActivity.this);
        alert.setTitle(getString(R.string.sendErrorTitle));
        alert.setMessage(getString(R.string.sendErrorMessage));
        alert.setView(checkBoxView);
        alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (rememberChoice[0]) {
                    writeToSharedPrefs("SEND-CRASH", "ALWAYS-SEND", "simraPrefs", StartActivity.this);
                }
                Intent intent = new Intent(StartActivity.this, UploadService.class);
                intent.putExtra("CRASH_REPORT", true);
                startService(intent);
            }
        });
        alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (rememberChoice[0]) {
                    writeToSharedPrefs("SEND-CRASH", "NEVER-SEND", "simraPrefs", StartActivity.this);
                }
            }
        });
        alert.show();
    }


    public void firePrivacyDialog() {
        View checkBoxView = View.inflate(this, R.layout.checkbox, null);
        CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);

        checkBox.setText(getString(R.string.iAccept));
        AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);

        // Linkify the message
        builder.setTitle(getString(R.string.privacyAgreementTitle));
        builder.setMessage(getResources().getText(R.string.privacyAgreementMessage));
        builder.setView(checkBoxView);
        builder.setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                writeBooleanToSharedPrefs("Privacy-Policy-Accepted", checkBox.isChecked(), "simraPrefs", StartActivity.this);
            }
        });
        builder.setNegativeButton(R.string.close_simra, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                writeBooleanToSharedPrefs("firstTime", true, "simraPrefs", StartActivity.this);
                finish();
                Toast.makeText(StartActivity.this, getString(R.string.simra_closed), Toast.LENGTH_LONG).show();
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
        ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());

        // Initially disable the button
        ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isChecked);

            }
        });
    }

}
