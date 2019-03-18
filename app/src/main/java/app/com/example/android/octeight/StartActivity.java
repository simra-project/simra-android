package app.com.example.android.octeight;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import static app.com.example.android.octeight.Utils.appendToFile;
import static app.com.example.android.octeight.Utils.fileExists;
import static app.com.example.android.octeight.Utils.getAppVersionNumber;
import static app.com.example.android.octeight.Utils.lookUpBooleanSharedPrefs;
import static app.com.example.android.octeight.Utils.lookUpIntSharedPrefs;
import static app.com.example.android.octeight.Utils.overWriteFile;
import static app.com.example.android.octeight.Utils.showMessageOK;
import static app.com.example.android.octeight.Utils.writeBooleanToSharedPrefs;
import static app.com.example.android.octeight.Utils.writeIntToSharedPrefs;
import static app.com.example.android.octeight.Utils.writeLongToSharedPrefs;

/**
 * Shows general info about the app and starts the MainActivity once the okay-Button is pressed
 * or TIME_OUT/1000 seconds are passed.
 * TODO: migrate permission request and map loading from MainActivity to StartActivity
 */

public class StartActivity extends BaseActivity {

    private static int TIME_OUT = 100000; //Time to launch the another activity
    Button next;

    // For permission request
    private final int LOCATION_ACCESS_CODE = 1;

    // Log tag
    private static final String TAG = "StartActivity_LOG";
    public boolean sendErrorPermitted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Log.d(TAG, "onCreate() started");

        newUpdate();

        permissionRequest(Manifest.permission.ACCESS_FINE_LOCATION, StartActivity.this.getString(R.string.permissionRequestRationale), LOCATION_ACCESS_CODE);

        if ((!isFirstTime()) & (privacyPolicyAccepted()) & (!unsentErrors()) && (ContextCompat.checkSelfPermission(StartActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
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

    private void newUpdate() {
        int appVersion = lookUpIntSharedPrefs("App-Version", -1, "simraPrefs", this);

        if (appVersion < 6) {
            File[] dirFiles = getFilesDir().listFiles();
            String path;
            for (int i = 0; i < dirFiles.length; i++) {

                path = dirFiles[i].getName();
                Log.d(TAG, "path: " + path);
                if (!path.equals("profile.csv")) {
                    dirFiles[i].delete();
                }
                /*
                if(path.startsWith("accEvents")) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(getFileStreamPath(path)))) {
                        String[] line = reader.readLine().split(",", -1);
                        if (line.length < 20) {
                            ridesToDelete.add(line[0]);
                            dirFiles[i].delete();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                */
            }
            /*
            dirFiles = getFilesDir().listFiles();
            for (int i = 0; i < ridesToDelete.size(); i++) {
                for (int j = 0; j < dirFiles.length ; j++) {
                    path = dirFiles[j].getName();
                    if(path.split("_")[0].equals(ridesToDelete.get(i))){
                        dirFiles[j].delete();
                    }
                }
            }

            String newMetaData = "";
            try (BufferedReader reader = new BufferedReader(new FileReader
                    (getFileStreamPath("metaData.csv")))) {
                reader.readLine();

                String line;

                while ((line = reader.readLine()) != null) {
                    String[] actualRide = line.split(",", -1);

                }




            } catch (Exception e) {
                e.printStackTrace();
            }
        */
            String fileInfoLine = getAppVersionNumber(this) + "#1" + System.lineSeparator();

            overWriteFile((fileInfoLine + "key, startTime, endTime, annotated" + System.lineSeparator()), "metaData.csv", this);
            writeIntToSharedPrefs("RIDE-KEY", 0, "simraPrefs", this);
        }
        writeIntToSharedPrefs("App-Version", getAppVersionNumber(this), "simraPrefs", this);
    }


    private void permissionRequest(final String requestedPermission, String rationaleMessage, final int accessCode) {
        // Check whether FINE_LOCATION permission is not granted
        if (ContextCompat.checkSelfPermission(StartActivity.this, requestedPermission)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission for FINE_LOCATION is not granted. Show rationale why location permission is needed
            // in an AlertDialog and request access to FINE_LOCATION

            // The message to be shown in the AlertDialog
            //String rationaleMessage = "Diese App benötigt den Zugriff auf deine Standortdaten, um dich auf der Karte anzuzeigen" +
            //      "können und deine Fahrt zu speichern.";

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

                overWriteFile((fileInfoLine + "key, startTime, endTime, annotated"
                        + System.lineSeparator()), "metaData.csv", this);

            }

            Log.d(TAG, "firstTime. Creating profile.csv");
            if (!fileExists("profile.csv", this)) {
                String fileInfoLine = getAppVersionNumber(this) + "#1" + System.lineSeparator();

                overWriteFile((fileInfoLine + "birth,gender,region,experience,numberOfRides,duration,numberOfIncidents"
                        + System.lineSeparator()), "profile.csv", this);

            }

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

    // Look up whether there are unsent crash logs and ask the user for a permission to
    // send them to the server. If the user gives permission, upload the crash report(s).
    private boolean unsentErrors() {
        boolean positive = lookUpBooleanSharedPrefs("NEW-UNSENT-ERROR",false,"simraPrefs",this);
        if (positive) {
            fireSendErrorDialog();
        }
        return positive;
    }

    public void fireSendErrorDialog() {

        View checkBoxView = View.inflate(this, R.layout.checkbox, null);
        CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                writeBooleanToSharedPrefs("NEW-UNSENT-ERROR",!checkBox.isChecked(),"simraPrefs",StartActivity.this);
            }
        });
        checkBox.setText(getString(R.string.doNotShowAgain));
        AlertDialog.Builder alert = new AlertDialog.Builder(StartActivity.this);
        alert.setTitle(getString(R.string.sendErrorTitle));
        alert.setMessage(getString(R.string.sendErrorMessage));
        alert.setView(checkBoxView);
        alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent intent = new Intent(StartActivity.this, UploadService.class);
                intent.putExtra("CRASH_REPORT", sendErrorPermitted);
                startService(intent);
            }
        });
        alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

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
