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
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static app.com.example.android.octeight.Utils.fileExists;
import static app.com.example.android.octeight.Utils.lookUpIntSharedPrefs;
import static app.com.example.android.octeight.Utils.overWriteFile;

public class HistoryActivity extends BaseActivity {

    // Log tag
    private static final String TAG = "HistoryActivity_LOG";
    ImageButton backBtn;
    TextView toolbarTxt;

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
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        toolbarTxt = findViewById(R.id.toolbar_title);
        toolbarTxt.setText(R.string.title_activity_history);

        backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           finish();
                                       }
                                   }
        );

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
            /*
            for (int i = 0; i < ridesList.size(); i++) {
                ridesArr[Integer.valueOf(ridesList.get(i)[0])] = listToTextShape(ridesList.get(i));
            }
            */
            for (String[] i : ridesList) {
                ridesArr[((ridesList.size()) - Integer.parseInt(i[0])) - 1] = listToTextShape(i);
            }
            Log.d(TAG, "ridesArr: " + Arrays.toString(ridesArr));
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, ridesArr);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // gets the files in the directory
                    // lists all the files into an array
                    File[] dirFiles = getFilesDir().listFiles();
                    Log.d(TAG, "dirFiles: " + Arrays.deepToString(dirFiles));
                    String clicked = (String) listView.getItemAtPosition(position);
                    Log.d(TAG, "clicked: " + clicked);
                    String prefix = Constants.APP_PATH + "files/";
                    String key = clicked.replace("#", "").split(" ")[0];
                    clicked = prefix + key;
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
                                Log.d(TAG, "pathToAccGpsFile: " + intent.getStringExtra("PathToAccGpsFile"));
                                Log.d(TAG, "Duration: " + intent.getStringExtra("Duration"));
                                Log.d(TAG, "StartTime: " + intent.getStringExtra("StartTime"));
                                Log.d(TAG, "State: " + intent.getStringExtra("State"));

                                startActivity(intent);
                            }
                        }
                    }
                }
            });
        } else {

            Log.d(TAG, "metaData.csv doesn't exists");

            Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator_layout), (getString(R.string.noHistory)), Snackbar.LENGTH_LONG);
            snackbar.show();

        }

        RelativeLayout justUploadButton = findViewById(R.id.justUpload);
        justUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                File[] dirFiles = getFilesDir().listFiles();
                ArrayList<String> ridesToUpload = new ArrayList<>();
                int numberOfRides = 0;
                long duration = 0;
                int numberOfIncidents = 0;

                try {
                    BufferedReader br = new BufferedReader(new FileReader(metaDataFile));
                    // br.readLine() to skip the first line which contains the headers
                    String line = br.readLine();

                    while ((line = br.readLine()) != null) {
                        // Log.d(TAG, line);
                        String[] actualLine = line.split(",");
                        duration = duration + (Long.valueOf(actualLine[2]) - Long.valueOf(actualLine[1]));
                        numberOfRides = Integer.valueOf(actualLine[0]);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (dirFiles.length != 0) {
                    for (int i = 0; i < dirFiles.length; i++) {
                        String nameOfFileToBeRenamed = dirFiles[i].getName();
                        if (nameOfFileToBeRenamed.startsWith("accEvents")) {
                            try {
                                BufferedReader br = new BufferedReader(new FileReader(getFileStreamPath(nameOfFileToBeRenamed)));
                                // br.readLine() to skip the first line which contains the headers
                                String line = br.readLine();

                                while ((line = br.readLine()) != null) {
                                    // Log.d(TAG, line);
                                    String[] actualLine = line.split(",", -1);
                                    if ((!actualLine[4].equals("") && !actualLine[4].equals("0")) && !actualLine[6].equals("")) {
                                        numberOfIncidents++;
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
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
                if (ridesToUpload.size() > 0) {
                    String demographicHeader = "birth,gender,region,ber,lon,bike,loc,numberOfRides,duration,numberOfIncidents" + System.lineSeparator();
                    String demographics = getDemographics();
                    overWriteFile(demographicHeader + demographics + "," + numberOfRides + "," + duration + "," + numberOfIncidents, "profile.csv", HistoryActivity.this);
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
                                pd.setProgress(Math.round(100 - 100 * ((float) currentNumberOfTasks / (float) (ridesToUpload.size() * 2))));
                                Log.d(TAG, "currentNumberOfTasks: " + currentNumberOfTasks);
                                if (currentNumberOfTasks == 0) {
                                    unbindService(mUploadServiceConnection);
                                    pd.dismiss();
                                    Toast.makeText(HistoryActivity.this, getString(R.string.uploadRidesSuccessful), Toast.LENGTH_SHORT).show();
                                    handler.removeCallbacks(this);
                                    if (exitWhenDone) {
                                        finishAndRemoveTask();
                                    }
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
        uploadAndExitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitWhenDone = true;
                justUploadButton.performClick();
                HistoryActivity.this.moveTaskToBack(true);
            }
        });

        // getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Press immediately the button, if HistoryActivity was created automatically after the
        // recording of a route has finished
        if (getIntent().hasExtra("PathToAccGpsFile")) {
            startShowRouteWithSelectedRide();
        }

    }

    private String getDemographics() {

        int birth = lookUpIntSharedPrefs("Profile-Age", 0, "simraPrefs", this);
        int gender = lookUpIntSharedPrefs("Profile-Gender", 0, "simraPrefs", this);
        int region = lookUpIntSharedPrefs("Profile-Region", 0, "simraPrefs", this);
        int ber = lookUpIntSharedPrefs("Profile-berDistrict", 0, "simraPrefs", this);
        int lon = lookUpIntSharedPrefs("Profile-lonDistrict", 0, "simraPrefs", this);
        int bike = lookUpIntSharedPrefs("Profile-bikeType", 0, "simraPrefs", this);
        int loc = lookUpIntSharedPrefs("Profile-phoneLocation", 0, "simraPrefs", this);


        return birth + "," + gender + "," + region + "," + ber + "," + lon + "," + bike + "," + loc;
    }

    private void stopTask(Handler handler, Runnable runnable) {
        handler.removeCallbacks(runnable);
    }

    private String listToTextShape(String[] item) {
        Log.d(TAG, "listToTextShape item: " + Arrays.toString(item));
        String todo = getString(R.string.newRideInHistoryActivity);

        File[] dirFiles = getFilesDir().listFiles();
        if (dirFiles.length != 0) {
            for (int i = 0; i < dirFiles.length; i++) {
                if (dirFiles[i].getName().startsWith(item[0] + "_") && dirFiles[i].getName().endsWith("_2.csv")) {
                    Log.d(TAG, "dirFiles[i].getName().endsWith: " + dirFiles[i].getName());
                    todo = getString(R.string.rideUploadedInHistoryActivity);
                } else if (dirFiles[i].getName().startsWith(item[0] + "_") && dirFiles[i].getName().endsWith("_1.csv")) {
                    todo = getString(R.string.rideAnnotatedInHistoryActivity);

                }
            }
        }
        if (item[3].equals("2")) {
            todo = "";
        }


        long millis = Long.valueOf(item[2]) - Long.valueOf(item[1]);
        String prettyDuration = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));

        String startDateOfRide = DateUtils.formatDateTime(this, Long.valueOf(item[1]), DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE |
                DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NUMERIC_DATE);

        String result = "#" + item[0] + " " + todo + " " + startDateOfRide
                + " " + getString(R.string.ride_length) + " : " + prettyDuration;


        return result;
        // requires API 26 (Java 8) Date.from(Instant.ofEpochMilli( Long.getLong(item[0]) )).toString()
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

    public void startShowRouteWithSelectedRide() {

        Log.d(TAG, "onClick()");

        // Checks if HistoryActivity was started by the user or by the app after a route
        // recording was finished
        if (getIntent().hasExtra("PathToAccGpsFile")) {
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
        if (accGpsString != null && startTime != "") {
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
