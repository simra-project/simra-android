package app.com.example.android.octeight;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

/**
 * Shows general info about the app and starts the MainActivity once the okay-Button is pressed
 * or TIME_OUT/1000 seconds are passed.
 * TODO: migrate permission request and map loading from MainActivity to StartActivity
 */

public class StartActivity extends AppCompatActivity {

    private static int TIME_OUT = 10000; //Time to launch the another activity
    Button next;
    Runnable startActivityRunnable;
    Handler startActivityHandler;

    // For permission request
    private final int LOCATION_ACCESS_CODE = 1;

    // Log tag
    private static final String TAG = "StartActivity_LOG";

    private Boolean firstTime = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);


        SharedPreferences pref = getSharedPreferences("first_time", MODE_PRIVATE);

        Log.d(TAG, "isFirstTime: " + String.valueOf(isFirstTime()));

        // first start, show your dialog | first-run code goes here
        permissionRequest(Manifest.permission.ACCESS_FINE_LOCATION, StartActivity.this.getString(R.string.permissionRequestRationaleDE), LOCATION_ACCESS_CODE);

        if (!(isFirstTime())&&(ContextCompat.checkSelfPermission(StartActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)){
            Log.d(TAG, "isFirstTime: " + String.valueOf(isFirstTime()));
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {



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
                    startActivity(intent);
                    // finish() to prevent going back to StartActivity, when the Back Button is pressed
                    // in MainActivity
                    finish();
                }
            });
        }
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
}




