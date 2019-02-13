package app.com.example.android.octeight;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import static app.com.example.android.octeight.Utils.appendToFile;
import static app.com.example.android.octeight.Utils.fileExists;

/**
 * Shows general info about the app and starts the MainActivity once the okay-Button is pressed
 * or TIME_OUT/1000 seconds are passed.
 * TODO: migrate permission request and map loading from MainActivity to StartActivity
 */

public class StartActivity extends BaseActivity {

    private static int TIME_OUT = 10000; //Time to launch the another activity
    Button next;
    Runnable startActivityRunnable;
    Handler startActivityHandler;

    // For permission request
    private final int LOCATION_ACCESS_CODE = 1;

    // Log tag
    private static final String TAG = "StartActivity_LOG";

    private Boolean firstTime = null;

    private String caller = null;

    public boolean sendErrorPermitted = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Log.d(TAG, "onCreate() started");

        caller = getIntent().getStringExtra("caller");
        if (caller == null){
            caller = "NoCaller";
        }

        SharedPreferences sharedPrefs = getApplicationContext()
                .getSharedPreferences("simraPrefs", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPrefs.edit();

        // Look up whether there are unsent crash logs and ask the user for a permission to
        // send them to the server. If the user gives permission, upload the crash report(s).
        if (sharedPrefs.contains("NEW-UNSENT-ERROR")) {
            boolean newErrorsExist = sharedPrefs.getBoolean("NEW-UNSENT-ERROR", true);
            if(newErrorsExist){
                sendErrorPermitted = getDialogValueBack(this);
                if (sendErrorPermitted) {
                    Intent intent = new Intent(this, UploadService.class);
                    intent.putExtra("CRASH_REPORT", sendErrorPermitted);
                    startService(intent);
                }

            }

        } else {
            editor.putBoolean("NEW-UNSENT-ERROR", false);
            editor.commit();
        }


        permissionRequest(Manifest.permission.ACCESS_FINE_LOCATION, StartActivity.this.getString(R.string.permissionRequestRationaleDE), LOCATION_ACCESS_CODE);

        if (!(isFirstTime())&&(ContextCompat.checkSelfPermission(StartActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)&&(!caller.equals("MainActivity"))){
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {

            // First start, show your dialog | first-run code goes here
            if (isFirstTime() && !caller.equals("MainActivity")) {
                if (!fileExists("incidentData.csv", this)) {

                    appendToFile("key,lat,lon,ts,path_to_AccFile,incidentType,phoneLocation,description"
                            + System.lineSeparator(), "incidentData.csv", this);

                }

                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                // META-FILE (one per user): contains ...
                // * the information required to display rides in the ride history (See RecorderService)
                //   (DATE,START TIME, END TIME, ANNOTATED TRUE/FALSE)
                // * the RIDE KEY which allows to identify the file containing the complete data for
                //   a ride. => Use case: user wants to view a ride from history - retrieve data
                // * one meta file per user, so we only want to create it if it doesn't exist yet.
                //   (fileExists and appendToFile can be found in the Utils.java class)

                if(!fileExists("metaData.csv", this)) {
                    appendToFile("key, startTime, endTime, annotated"
                            +System.lineSeparator(), "metaData.csv", this);

                }

                // Runnable that starts MainActivity after defined time (TIME_OUT)
                startActivityRunnable = new Runnable() {
                    @Override
                    public void run() {
                        Intent i = new Intent(StartActivity.this, MainActivity.class);
                        startActivity(i);
                        // finish() to prevent going back to StartActivity, when the Back Button is pressed
                        // in MainActivity
                        finish();
                    }
                };
            }
            // create Handler and make it run the Runnable after TIME_OUT
            startActivityHandler = new Handler();
            startActivityHandler.postDelayed(startActivityRunnable, TIME_OUT);

            // start MainActivity when Button is clicked
            next = findViewById(R.id.nextBtn);
            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(StartActivity.this, MainActivity.class);
                    // remove the Callback of the Runnable to the Handler to prevent second start of
                    // MainActivity
                    startActivityHandler.removeCallbacks(startActivityRunnable);
                    if(caller.equals("MainActivity")){
                        returnToMain();
                    } else {
                        startActivity(intent);
                    }
                    // finish() to prevent going back to StartActivity, when the Back Button is pressed
                    // in MainActivity
                    finish();
                }
            });
        }
    }



    public void returnToMain(){
        super.onBackPressed();
    }


    private void permissionRequest(final String requestedPermission, String rationaleMessage, final int accessCode){
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
            showMessageOK(rationaleMessage, rationaleOnClickListener);
        }
    }


    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    // Create an AlertDialog with an OK Button displaying a message
    private void showMessageOK(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(StartActivity.this)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", okListener)
                .create()
                .show();
    }


    // Create an AlertDialog with an Ok and Cancel Button displaying a message
    private void askUserToSendError() {
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ups!");
        builder.setMessage("Bei der letzten Ausführung der App ist es wohl zu einem Fehler gekommen. Möchten Sie den Fehlerbericht an SimRa schicken, damit wir die App verbessern können?");

        // add the buttons
        builder.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendErrorPermitted = true;
            }
        });
        // builder.setNeutralButton("Immer", null);
        builder.setNegativeButton("Nein", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendErrorPermitted = false;
            }
        });

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        Log.d(TAG, "showing dialog");
        dialog.show();
    }

    /**
     * Checks if the user is opening the app for the first time.
     * Note that this method should be placed inside an activity and it can be called multiple times.
     * @return boolean
     */
    private boolean isFirstTime() {
        if (firstTime == null) {
            SharedPreferences mPreferences = this.getSharedPreferences("first_time", Context.MODE_PRIVATE);
            firstTime = mPreferences.getBoolean("firstTime", true);
            if (firstTime) {
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean("firstTime", false);
                editor.commit();
            }
        }
        return firstTime;
    }

        public boolean getDialogValueBack(Context context) {

            final Handler handler = new Handler()
            {
                @Override
                public void handleMessage(Message mesg)
                {
                    throw new RuntimeException();
                }
            };

            AlertDialog.Builder alert = new AlertDialog.Builder(context);
            alert.setTitle("Ups!");
            alert.setMessage("Bei der letzten Ausführung der App ist es wohl zu einem Fehler gekommen. Möchten Sie den Fehlerbericht an SimRa schicken, damit wir die App verbessern können?");
            alert.setPositiveButton("Ja", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    sendErrorPermitted = true;
                    handler.sendMessage(handler.obtainMessage());
                }
            });
            alert.setNegativeButton("Nein", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    sendErrorPermitted = false;
                    handler.sendMessage(handler.obtainMessage());
                }
            });
            alert.show();

            try{ Looper.loop(); }
            catch(RuntimeException e){}

            return sendErrorPermitted;


    }
}




