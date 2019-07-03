package de.tuberlin.mcc.simra.app.main;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.net.UploadService;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import de.tuberlin.mcc.simra.app.util.Utils;
import okhttp3.internal.Util;

import static de.tuberlin.mcc.simra.app.util.Constants.PROFILE_HEADER;
import static de.tuberlin.mcc.simra.app.util.Utils.appendToFile;
import static de.tuberlin.mcc.simra.app.util.Utils.fileExists;
import static de.tuberlin.mcc.simra.app.util.Utils.getAppVersionNumber;
import static de.tuberlin.mcc.simra.app.util.Utils.lookUpBooleanSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.lookUpIntSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.lookUpSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.overWriteFile;
import static de.tuberlin.mcc.simra.app.util.Utils.showKeyPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.showMessageOK;
import static de.tuberlin.mcc.simra.app.util.Utils.updateToV18;
import static de.tuberlin.mcc.simra.app.util.Utils.updateToV24;
import static de.tuberlin.mcc.simra.app.util.Utils.writeBooleanToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.writeIntToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.writeLongToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.writeToSharedPrefs;

/**
 * Shows general info about the app, if the app is run the first time.
 * If not, MainActivity is started, except if a crash happened before.
 */

public class StartActivity extends BaseActivity {

    Button next;

    // Log tag
    private static final String TAG = "StartActivity_LOG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Log.d(TAG, "onCreate() started");
        writeIntToSharedPrefs("App-Version", getAppVersionNumber(this), "simraPrefs", this);
        showKeyPrefs(StartActivity.this);
        updateToV18(StartActivity.this);
        updateToV24(StartActivity.this);

        // For permission request
        int LOCATION_ACCESS_CODE = 1;
        permissionRequest(Manifest.permission.ACCESS_FINE_LOCATION, StartActivity.this.getString(R.string.permissionRequestRationale), LOCATION_ACCESS_CODE);

        if ((!isFirstTime()) & (privacyPolicyAccepted()) & (!showUnsentErrorDialogPermitted()) && (ContextCompat.checkSelfPermission(StartActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)) {
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




    private void permissionRequest(final String requestedPermission, String rationaleMessage, final int accessCode) {
        // Check whether FINE_LOCATION permission is not granted
        if (ContextCompat.checkSelfPermission(StartActivity.this, requestedPermission)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission for FINE_LOCATION is not granted. Show rationale why location permission is needed
            // in an AlertDialog and request access to FINE_LOCATION

            // The OK-Button fires a requestPermissions
            DialogInterface.OnClickListener rationaleOnClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(StartActivity.this,
                            new String[]{requestedPermission}, accessCode);
                }
            };
            showMessageOK(rationaleMessage, rationaleOnClickListener, StartActivity.this);
        }
    }

    /**
     * Checks if the user is opening the app for the first time.
     * Note that this method should be placed inside an activity and it can be called multiple times.
     *
     * @return boolean
     */
    private boolean isFirstTime() {
        boolean firstTime = lookUpBooleanSharedPrefs("firstTime",true,"simraPrefs",this);
        if(firstTime) {
            writeBooleanToSharedPrefs("firstTime",false,"simraPrefs", this);
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // META-FILE (one per user): contains ...
            // * the information required to display rides in the ride history (See RecorderService)
            //   (DATE,START TIME, END TIME, ANNOTATED TRUE/FALSE)
            // * the RIDE KEY which allows to identify the file containing the complete data for
            //   a ride. => Use case: user wants to view a ride from history - retrieve data
            // * one meta file per user, so we only want to create it if it doesn't exist yet.
            //   (fileExists and appendToFile can be found in the Utils.java class)
            Log.d(TAG, "firstTime. Creating metaData.csv");
            if (!fileExists("metaData.csv", this)) {
                String fileInfoLine = getAppVersionNumber(this) + "#1" + System.lineSeparator();

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
            writeLongToSharedPrefs("Privacy-Duration",30,"simraPrefs",this);
            // don't start to record the ride, until user 30 seconds passed
            // from recording start time.
            writeIntToSharedPrefs("Privacy-Distance",30,"simraPrefs",this);
        }
        return firstTime;
    }

    private boolean privacyPolicyAccepted() {
        boolean accepted = lookUpBooleanSharedPrefs("Privacy-Policy-Accepted",false,"simraPrefs", this);
        if (!accepted) {
            firePrivacyDialog();
        }
        return accepted;
    }

    // Look up whether to ask the user for a permission to
    // send the crash logs to the server. If the user gives permission, upload the crash report(s).
    private boolean showUnsentErrorDialogPermitted() {
        String crashSendState = lookUpSharedPrefs("SEND-CRASH","UNKNOWN","simraPrefs",this);
        boolean newCrash = lookUpBooleanSharedPrefs("NEW-UNSENT-ERROR",false,"simraPrefs", this);
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
                    writeToSharedPrefs("SEND-CRASH","ALWAYS-SEND","simraPrefs",StartActivity.this);
                }
                Intent intent = new Intent(StartActivity.this, UploadService.class);
                intent.putExtra("CRASH_REPORT", true);
                startService(intent);
            }
        });
        alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (rememberChoice[0]) {
                    writeToSharedPrefs("SEND-CRASH", "NEVER-SEND", "simraPrefs",StartActivity.this);
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
                writeBooleanToSharedPrefs("Privacy-Policy-Accepted",checkBox.isChecked(),"simraPrefs",StartActivity.this);
            }
        });
        builder.setNegativeButton(R.string.close_simra, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                writeBooleanToSharedPrefs("firstTime",true,"simraPrefs",StartActivity.this);
                finish();
                Toast.makeText(StartActivity.this,getString(R.string.simra_closed),Toast.LENGTH_LONG).show();
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
        ((TextView)dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());

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
