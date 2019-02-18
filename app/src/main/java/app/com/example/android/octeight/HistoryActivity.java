package app.com.example.android.octeight;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.concurrent.TimeUnit;

import static app.com.example.android.octeight.Utils.fileExists;

public class HistoryActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    // Log tag
    private static final String TAG = "HistoryActivity_LOG";

    boolean exitWhenDone = false;
    String accGpsString = "";
    String pathToAccGpsFile = "";
    String date = "";
    int state = 0;
    String duration = "";
    String startTime = "";

    ListView listView;
    private File metaDataFile;
    ArrayList<String[]> ridesList = new ArrayList<>();
    String[] ridesArr;

    UploadService mBoundUploadService;


    /**
     * @TODO: When this Activity gets started automatically after the route recording is finished,
     * the route gets shown immediately by calling ShowRouteActivity.
     * Otherwise, this activity has to scan for saved rides (maybe as files in the internal storage
     * or as entries in sharedPreference) and display them in a list.
     * <p>
     * The user must be able to select a ride which should start the ShowRouteActivity with that ride.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        setContentView(R.layout.activity_history);

        //  Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //  Helmet
        ImageButton helmetButton = findViewById(R.id.helmet_icon);
        helmetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launchActivityIntent = new Intent(HistoryActivity.this,
                        MainActivity.class);
                startActivity(launchActivityIntent);
                finish();
            }
        });

        listView = findViewById(R.id.listView);

        if (fileExists("metaData.csv", this)) {

            metaDataFile = getFileStreamPath("metaData.csv");
            try {
                BufferedReader br = new BufferedReader(new FileReader(metaDataFile));
                // br.readLine() to skip the first line which contains the headers
                String line = br.readLine();

                while ((line = br.readLine()) != null) {
                    // Log.d(TAG, line);
                    ridesList.add(line.split(","));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //ridesArr = ridesList.toArray(new String[ridesList.size()]);
            ridesArr = new String[ridesList.size()];
            Log.d(TAG, "ridesList: " + Arrays.deepToString(ridesList.toArray()));
            for (String[] i : ridesList) {
                ridesArr[Integer.parseInt(i[0])] = listToTextShape(i);
            }
            Log.d(TAG, "ridesArr: " + Arrays.toString(ridesArr));
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, ridesArr);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener()

            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // gets the files in the directory
                    // lists all the files into an array
                    File[] dirFiles = getFilesDir().listFiles();
                    Log.d(TAG, "dirFiles: " + Arrays.deepToString(dirFiles));
                    String clicked = (String) listView.getItemAtPosition(position);
                    Log.d(TAG, "clicked: " + clicked);
                    String prefix = Constants.APP_PATH + "files/";
                    clicked = String.valueOf(clicked.charAt(1));
                    clicked = prefix + clicked;
                    if (dirFiles.length != 0) {
                        // loops through the array of files, outputting the name to console
                        for (int i = 0; i < dirFiles.length; i++) {

                            String fileOutput = dirFiles[i].toString();
                            Log.d(TAG, "fileOutput: " + fileOutput);


                            if (fileOutput.startsWith(clicked + "_")) {
                                // Start ShowRouteActivity with the selected Ride.
                                Intent intent = new Intent(HistoryActivity.this, ShowRouteActivity.class);
                                intent.putExtra("PathToAccGpsFile", dirFiles[i].getPath().replace(prefix, ""));
                                // Log.d(TAG, "onClick() date: " + date);
                                intent.putExtra("Duration", String.valueOf(Long.valueOf(ridesList.get(position)[2]) - Long.valueOf(ridesList.get(position)[1])));
                                intent.putExtra("StartTime", ridesList.get(position)[2]);
                                intent.putExtra("State", ridesList.get(position)[3]);
                                Log.d(TAG, intent.getStringExtra("PathToAccGpsFile"));
                                Log.d(TAG, intent.getStringExtra("Duration"));
                                Log.d(TAG, intent.getStringExtra("StartTime"));
                                Log.d(TAG, intent.getStringExtra("State"));

                                startActivity(intent);
                            }
                        }
                    }
                }

                ;

            });
        } else {

            Log.d(TAG, "metaData.csv don't exists");

            Snackbar snackbar = Snackbar.make(findViewById(R.id.drawer_layout), (getString(R.string.noHistory)), Snackbar.LENGTH_LONG);
            snackbar.show();

        }

        RelativeLayout justUploadButton = findViewById(R.id.justUpload);
        Log.d(TAG, "justUploadButton" + justUploadButton);
        justUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                File[] dirFiles = getFilesDir().listFiles();
                ArrayList<String> ridesToUpload = new ArrayList<>();
                if (dirFiles.length != 0) {
                    for (int i = 0; i < dirFiles.length; i++) {
                        String nameOfFileToBeRenamed = dirFiles[i].getName();
                        String newNameOfFile = nameOfFileToBeRenamed.replace("_1.csv", "_2.csv");
                        String path = Constants.APP_PATH + "files/";
                        Log.d(TAG, "nameOfFileToBeRenamed: " + nameOfFileToBeRenamed + " newNameOfFile: " + newNameOfFile);
                        if (nameOfFileToBeRenamed.endsWith("_1.csv")) {
                            Log.d(TAG, "Renaming");
                            dirFiles[i].renameTo(new File(path + newNameOfFile));
                            ridesToUpload.add(newNameOfFile);
                        }
                    }
                }

                if (ridesToUpload.size() > 0){
                    Intent intent = new Intent(HistoryActivity.this, UploadService.class);
                    intent.putStringArrayListExtra("RidesToUpload", ridesToUpload);
                    startService(intent);
                    bindService(intent, mUploadServiceConnection, Context.BIND_AUTO_CREATE);


                    ProgressDialog pd;

                    pd = new ProgressDialog(HistoryActivity.this);
                    pd.setTitle(getString(R.string.progressDialogTitle));
                    pd.setMessage(getString(R.string.progressDialogText));
                    pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    pd.setCancelable(false);
                    pd.setIndeterminate(false);
                    pd.setProgressPercentFormat(null);
                    pd.setProgressNumberFormat(null);
                    // Put a cancel button in progress dialog
                    pd.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.uploadInBackground), new DialogInterface.OnClickListener() {
                        // Set a click listener for progress dialog cancel button
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // dismiss the progress dialog
                            pd.dismiss();
                        }
                    });
                    pd.show();

                    // TODO: this runnable / handler never finishes
                    final Handler handler = new Handler();
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {

                            if (mBoundUploadService != null) {
                                int currentNumberOfTasks = mBoundUploadService.getNumberOfTasks();
                                pd.setProgress(Math.round(100 - 100 * ((float)currentNumberOfTasks / (float)ridesToUpload.size())));
                                if (currentNumberOfTasks == 0) {
                                    unbindService(mUploadServiceConnection);
                                    pd.dismiss();
                                    Toast.makeText(HistoryActivity.this, getString(R.string.uploadRidesSuccessful), Toast.LENGTH_SHORT).show();
                                    handler.removeCallbacks(this);
                                    if(exitWhenDone){
                                        finishAndRemoveTask();                                    }
                                } else {
                                    handler.postDelayed(this, 1000);
                                }

                            } else {
                                handler.postDelayed(this, 1000);
                            }
                        }
                    };
                    handler.post(runnable);

                } else {
                    Toast.makeText(HistoryActivity.this, getString(R.string.noFilesToBeUploaded), Toast.LENGTH_LONG).show();
                }
            }
        });


        RelativeLayout uploadAndExitButton = findViewById(R.id.uploadAndExit);
        Log.d(TAG, "uploadAndExitButton" + uploadAndExitButton);
        uploadAndExitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitWhenDone = true;
                justUploadButton.performClick();
                HistoryActivity.this.moveTaskToBack(true);
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Press immediately the button, if HistoryActivity was created automatically after the
        // recording of a route has finished
        if (getIntent().hasExtra("PathToAccGpsFile"))
        {

            startShowRouteWithSelectedRide();
        }

    }

    private void stopTask(Handler handler, Runnable runnable){
        handler.removeCallbacks(runnable);
    }

    private String listToTextShape (String[] item){
        Log.d(TAG, "listToTextShape item: " + Arrays.toString(item));
        String todo = getString(R.string.newRideInHistoryActivity);

        File[] dirFiles = getFilesDir().listFiles();
        if (dirFiles.length != 0) {
            for (int i = 0; i < dirFiles.length; i++) {
                if(dirFiles[i].getName().startsWith(item[0] + "_") && dirFiles[i].getName().endsWith("_2.csv")){
                    Log.d(TAG, "dirFiles[i].getName().endsWith: " + dirFiles[i].getName());
                    todo = "";
                }
            }
        }
        if (item[3].equals("2")){
            todo = "";
        }


        long millis = Long.valueOf(item[2]) - Long.valueOf(item[1]);
        String prettyDuration = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));

        String startDateOfRide = DateUtils.formatDateTime(this, Long.valueOf(item[1]), DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE |
                DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NUMERIC_DATE);

        String result = "#" + item[0] + " " + todo + " " + startDateOfRide
                + " " +getString(R.string.ride_length)+ " : " + prettyDuration;


        return result;
        // requires API 26 (Java 8) Date.from(Instant.ofEpochMilli( Long.getLong(item[0]) )).toString()
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Navigation Drawer
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {

            finish();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_main) {
            finish();
        }
        else if (id == R.id.nav_history) {
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            }
        } else if (id == R.id.nav_democraphic_data) {
            // src: https://stackoverflow.com/questions/2197741/how-can-i-send-emails-from-my-android-application
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.feedbackReceiver)});
            i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.demoDataHeader));
            i.putExtra(Intent.EXTRA_TEXT, getString(R.string.demoDataMail));
            try {
                startActivity(Intent.createChooser(i, "Send Data..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(HistoryActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
            }

        } else if (id == R.id.nav_feedback) {
            // src: https://stackoverflow.com/questions/2197741/how-can-i-send-emails-from-my-android-application
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.feedbackReceiver)});
            i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedbackHeader));
            i.putExtra(Intent.EXTRA_TEXT, getString(R.string.feedbackMail));
            try {
                startActivity(Intent.createChooser(i, "Send mail..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(HistoryActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
            }

        } else if (id == R.id.nav_setting) {
            Intent intent = new Intent (HistoryActivity.this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_infoMCC) {
            Intent intent = new Intent(HistoryActivity.this, WebActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_infoSimRa) {
            Intent intent = new Intent(HistoryActivity.this, StartActivity.class);
            intent.putExtra("caller", "MainActivity");
            startActivity(intent);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

  /*  private Long getMillis (String dateStr){
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss");
        Date date = null;
        try {
            date = (Date)formatter.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date.getTime();
    }*/

    public void startShowRouteWithSelectedRide(){

        Log.d(TAG, "onClick()");

        // Checks if HistoryActivity was started by the user or by the app after a route
        // recording was finished
        if(getIntent().hasExtra("PathToAccGpsFile")){
            // AccGpsString contains the accelerometer and location data as well as time data
            pathToAccGpsFile = getIntent().getStringExtra("PathToAccGpsFile");
            // TimeStamp is the duration of the ride in MS
            duration = getIntent().getStringExtra("Duration");
            // The time in which the ride started in ms from 1970
            startTime = getIntent().getStringExtra("StartTime");
            // State can be 0 for server processing not started, 1 for started and pending
            // and 2 for processed by server so the incidents can be annotated by the user
            state = getIntent().getIntExtra("State", 0);
        }
        // Log.d(TAG, "onCreate(): pathToAccGpsFile: " + pathToAccGpsFile + " date: " + date + " state: " + state);

        // Checks whether a ride was selected or not. Maybe it will be possible to select
        // multiple rides and push a button to send them all to the server to be analyzed
        if(accGpsString != null && startTime != "") {
            // Snackbar.make(view, getString(R.string.selectedRideInfo) + new Date(Long.valueOf(startTime)), Snackbar.LENGTH_LONG)
            //     .setAction("Action", null).show();
            // Start ShowRouteActivity with the selected Ride.
            Intent intent = new Intent(HistoryActivity.this, ShowRouteActivity.class);
            intent.putExtra("PathToAccGpsFile", pathToAccGpsFile);
            intent.putExtra("Duration", duration);
            intent.putExtra("StartTime", startTime);
            intent.putExtra("State", state);
            startActivity(intent);
        } else {
            //Snackbar.make(view, getString(R.string.errorNoRideSelected) + new Date(Long.valueOf(startTime)), Snackbar.LENGTH_LONG)
            //      .setAction("Action", null).show();
        }


    }
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // ServiceConnection for communicating with RecorderService
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private ServiceConnection mUploadServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected() called");
            UploadService.MyBinder myBinder = (UploadService.MyBinder) service;
            mBoundUploadService = myBinder.getService();
        }
    };
}
