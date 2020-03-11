package de.tuberlin.mcc.simra.app.main;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.net.UploadService;
import de.tuberlin.mcc.simra.app.annotation.ShowRouteActivity;
import de.tuberlin.mcc.simra.app.util.BaseActivity;

import static de.tuberlin.mcc.simra.app.util.Utils.fileExists;
import static de.tuberlin.mcc.simra.app.util.Utils.lookUpBooleanSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.lookUpIntSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.lookUpSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.overWriteFile;
import static de.tuberlin.mcc.simra.app.util.Utils.writeBooleanToSharedPrefs;

public class HistoryActivity extends BaseActivity {

    // Log tag
    private static final String TAG = "HistoryActivity_LOG";
    ImageButton backBtn;
    TextView toolbarTxt;

    boolean exitWhenDone = false;
    String accGpsString = "";
    String pathToAccGpsFile = "";
    int state = 0;
    String duration = "";
    String startTime = "";

    ListView listView;
    String[] ridesArr;

    UploadService mBoundUploadService;

    boolean privacyAgreement = false;

    String unit;
    int dateFormat;


    BroadcastReceiver br;

    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean uploadSuccessful = intent.getBooleanExtra("uploadSuccessful",false);
            boolean foundARideToUpload = intent.getBooleanExtra("foundARideToUpload", true);
            if (!foundARideToUpload) {
                Toast.makeText(getApplicationContext(), R.string.nothing_to_upload, Toast.LENGTH_LONG).show();
            } else if (!uploadSuccessful) {
                Toast.makeText(getApplicationContext(), R.string.upload_failed, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.upload_completed, Toast.LENGTH_LONG).show();
            }

            refreshMyRides();
        }
    }

    /**
     * When this Activity gets started automatically after the route recording is finished,
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

        privacyAgreement = lookUpBooleanSharedPrefs("Privacy-Policy-Accepted", false, "simraPrefs", this);

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

        unit = lookUpSharedPrefs("Settings-Unit","m","simraPrefs",HistoryActivity.this);
        dateFormat = lookUpIntSharedPrefs("Settings-DateFormat", 0, "simraPrefs", HistoryActivity.this);

        listView = findViewById(R.id.listView);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }
            LinearLayout historyButtons = findViewById(R.id.historyButtons);
            boolean isUp = true;

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // Log.d(TAG, view.getLastVisiblePosition() + " " + firstVisibleItem + " " + visibleItemCount + " " + totalItemCount);
                if ( isUp && view.getLastVisiblePosition() + 1 == totalItemCount ) {
                    Log.d(TAG, "hide buttons");
                    historyButtons.animate().translationX(historyButtons.getWidth()/2f);
                    isUp = false;
                    // historyButtons.setVisibility(View.INVISIBLE);
                } else if (!isUp && !(view.getLastVisiblePosition() + 1 == totalItemCount)) {
                    historyButtons.animate().translationX(0);
                    isUp = true;
                    // historyButtons.setVisibility(View.VISIBLE);
                }
            }
        });

        RelativeLayout justUploadButton = findViewById(R.id.justUpload);
        justUploadButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    justUploadButton.setElevation(0.0f);
                    justUploadButton.setBackground(getDrawable(R.drawable.button_pressed));
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    justUploadButton.setElevation(2 * HistoryActivity.this.getResources().getDisplayMetrics().density);
                    justUploadButton.setBackground(getDrawable(R.drawable.button_unpressed));
                }
                return false;
            }


        });
        justUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!lookUpBooleanSharedPrefs("uploadWarningShown",false,"simraPrefs",HistoryActivity.this)){
                    fireUploadPrompt();
                } else {
                    Intent intent = new Intent(HistoryActivity.this, UploadService.class);
                    startService(intent);
                    Toast.makeText(HistoryActivity.this,getString(R.string.upload_started),Toast.LENGTH_SHORT).show();
                }
            }
        });


        RelativeLayout uploadAndExitButton = findViewById(R.id.uploadAndExit);
        uploadAndExitButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    uploadAndExitButton.setElevation(0.0f);
                    uploadAndExitButton.setBackground(getDrawable(R.drawable.button_pressed));
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    uploadAndExitButton.setElevation(2 * HistoryActivity.this.getResources().getDisplayMetrics().density);
                    uploadAndExitButton.setBackground(getDrawable(R.drawable.button_unpressed));
                }
                return false;
            }

        });
        uploadAndExitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitWhenDone = true;
                if(!lookUpBooleanSharedPrefs("uploadWarningShown",false,"simraPrefs",HistoryActivity.this)){
                    fireUploadPrompt();
                } else {
                    justUploadButton.performClick();
                    HistoryActivity.this.moveTaskToBack(true);
                }

            }
        });


        // Press immediately the button, if HistoryActivity was created automatically after the
        // recording of a route has finished
        if (getIntent().hasExtra("PathToAccGpsFile")) {
            startShowRouteWithSelectedRide();
        }

    }

    private void refreshMyRides() {
        ArrayList<String[]> metaDataLines = new ArrayList<>();


        if (fileExists("metaData.csv", this)) {

            File metaDataFile = getFileStreamPath("metaData.csv");
            try {
                BufferedReader br = new BufferedReader(new FileReader(metaDataFile));
                // br.readLine() to skip the first line which contains the headers
                String line = br.readLine();
                line = br.readLine();

                while (((line = br.readLine()) != null)) {
                    if (!line.startsWith("key") && !line.startsWith("null")) {
                        metaDataLines.add(line.split(","));
                    }
                }
                Log.d(TAG, "metaDataLines: " + Arrays.deepToString(metaDataLines.toArray()));
            }catch (IOException e) {
                e.printStackTrace();
            }

            ridesArr = new String[metaDataLines.size()];
            Log.d(TAG, "refreshMyRides(): metaDataLines: " + Arrays.deepToString(metaDataLines.toArray()));
            for (int i = 0; i < metaDataLines.size(); i++) {
                String[] metaDataLine = metaDataLines.get(i);
                if(metaDataLine.length > 2 && !(metaDataLine[0].equals("key"))) {
                    ridesArr[((metaDataLines.size()) - i) - 1] = listToTextShape(metaDataLine);
                }
            }

            Log.d(TAG, "ridesArr: " + Arrays.toString(ridesArr));
            ArrayList<String> stringArrayList = new ArrayList<>(Arrays.asList(ridesArr));
            MyArrayAdapter myAdapter = new MyArrayAdapter(this, R.layout.row_icons, stringArrayList, metaDataLines);
            listView.setAdapter(myAdapter);

        } else {

            Log.d(TAG, "metaData.csv doesn't exists");

            Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator_layout), (getString(R.string.noHistory)), Snackbar.LENGTH_LONG);
            snackbar.show();

        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        br = new MyBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("de.tuberlin.mcc.simra.app.UPLOAD_COMPLETE");
        this.registerReceiver(br, filter);
        refreshMyRides();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(br);
    }

    private String listToTextShape(String[] item) {
        Log.d(TAG, "listToTextShape item: " + Arrays.toString(item));
        String todo = getString(R.string.newRideInHistoryActivity);

        if (item[3].equals("1")) {
            todo = getString(R.string.rideAnnotatedInHistoryActivity);
        } else if (item[3].equals("2")) {
            todo = getString(R.string.rideUploadedInHistoryActivity);
        }

        long millis = Long.valueOf(item[2]) - Long.valueOf(item[1]);
        int minutes = Math.round((millis/1000/60));
        Date dt = new Date(Long.valueOf(item[1]));
        Calendar localCalendar = Calendar.getInstance(TimeZone.getDefault());
        localCalendar.setTime(dt);
        String day = String.valueOf(localCalendar.get(Calendar.DATE));
        String month = String.valueOf(localCalendar.get(Calendar.MONTH) + 1);
        String year = String.valueOf(localCalendar.get(Calendar.YEAR)).substring(2,4);
        Locale locale = Resources.getSystem().getConfiguration().locale;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", locale);
        if (dateFormat == 1) {
            sdf = new SimpleDateFormat("hh:mm aa", locale);
        }
        String time = sdf.format(dt);
        if (month.length() < 2) {
           month = "0" + month;
        }
        if (day.length() < 2) {
            day = "0" + day;
        }


        String startDateOfRide = day + "." + month + "." + year + ", " + time + "h";
        if (dateFormat == 1) {
            startDateOfRide = month + "/" + day + "/" + year + ", " + time;
        }

        if(item.length>6) {
            return "#" + item[0] + ";" + startDateOfRide + ";" + todo + ";" + minutes + ";" + item[3] + ";" + item[6];
        } else {
            return "#" + item[0] + ";" + startDateOfRide + ";" + todo + ";" + minutes + ";" + item[3] + ";" + 0;
        }
    }

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

        // Checks whether a ride was selected or not. Maybe it will be possible to select
        // multiple rides and push a button to send them all to the server to be analyzed
        if (accGpsString != null && !startTime.equals("")) {
            // Start ShowRouteActivity with the selected Ride.
            Intent intent = new Intent(HistoryActivity.this, ShowRouteActivity.class);
            intent.putExtra("PathToAccGpsFile", pathToAccGpsFile);
            intent.putExtra("Duration", duration);
            intent.putExtra("StartTime", startTime);
            intent.putExtra("State", state);
            startActivity(intent);
        }


    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // ServiceConnection for communicating with RecorderService
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private ServiceConnection mUploadServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            refreshMyRides();
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected() called");
            UploadService.MyBinder myBinder = (UploadService.MyBinder) service;
            mBoundUploadService = myBinder.getService();
        }
    };

    public class MyArrayAdapter extends ArrayAdapter<String> {
        String TAG = "MyArrayAdapter_LOG";

        Context context;
        int layoutResourceId;
        ArrayList<String> stringArrayList = new ArrayList<String>();
        ArrayList<String[]> metaDataLines = new ArrayList<String[]>();

        public MyArrayAdapter(Context context, int layoutResourceId,
                              ArrayList<String> stringArrayList, ArrayList<String[]> metaDataLines ) {

            super(context, layoutResourceId, stringArrayList);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.stringArrayList = stringArrayList;
            this.metaDataLines = metaDataLines;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            Holder holder = null;

            if (row == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);
                holder = new Holder();
                holder.rideDate = (TextView) row.findViewById(R.id.row_icons_ride_date);
                holder.rideTime = (TextView) row.findViewById(R.id.row_ride_time);
                /*if (dateFormat == 1) {
                    holder.rideDate.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 13));
                    row.findViewById(R.id.duration_relativeLayout).setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 10));
                }*/
                holder.duration = (TextView) row.findViewById(R.id.row_duration);
                holder.distance = (TextView) row.findViewById(R.id.row_distance);
                holder.distanceUnit = (TextView) row.findViewById(R.id.row_distanceKM);
                holder.status = (ImageButton) row.findViewById(R.id.statusBtn);
                holder.btnDelete = (ImageButton) row.findViewById(R.id.deleteBtn);
                row.setTag(holder);
            } else {
                holder = (Holder) row.getTag();
            }
            String[] itemComponents = stringArrayList.get(position).split(";");
            holder.rideDate.setText(itemComponents[1].split(",")[0]);
            holder.rideTime.setText(itemComponents[1].split(",")[1]);
            // holder.message.setText(itemComponents[2]);
            Log.d(TAG, "itemComponents: " + Arrays.toString(itemComponents));

            if(itemComponents[2].contains(getString(R.string.rideAnnotatedInHistoryActivity))){
                holder.status.setBackground(getDrawable(R.drawable.ic_phone_android_black_24dp));
            }else if(itemComponents[2].contains(getString(R.string.rideUploadedInHistoryActivity))){
                holder.status.setBackground(getDrawable(R.drawable.ic_cloud_done_black_24dp));
            } else {
                holder.status.setBackground(null);
            }
            holder.duration.setText(itemComponents[3]);
            if (unit.equals("ft")) {
                holder.distance.setText(String.valueOf(Math.round(((Double.valueOf(itemComponents[5])/1600)*100.0))/100.0));
                holder.distanceUnit.setText("mi");
            } else {
                holder.distance.setText(String.valueOf(Math.round(((Double.valueOf(itemComponents[5])/1000)*100.0))/100.0));
                holder.distanceUnit.setText("km");
            }
            if (!itemComponents[4].equals("2")) {
                holder.btnDelete.setVisibility(View.VISIBLE);
            } else {
                holder.btnDelete.setVisibility(View.INVISIBLE);
            }
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // gets the files in the directory
                    // lists all the files into an array
                    File[] dirFiles = getFilesDir().listFiles();
                    String clicked = (String) listView.getItemAtPosition(position);
                    Log.d(TAG, "dirFiles.length: " + dirFiles.length + " clicked: " + clicked + " position: " + position);
                    clicked = clicked.replace("#", "").split(";")[0];
                    if (dirFiles.length != 0) {
                        // loops through the array of files, outputting the name to console
                        for (int i = 0; i < dirFiles.length; i++) {

                            String fileOutput = dirFiles[i].getName();
                            Log.d(TAG, "fileOutput: " + fileOutput + " clicked: " + clicked + "_");
                            if (fileOutput.startsWith(clicked + "_")) {
                                // Start ShowRouteActivity with the selected Ride.
                                Intent intent = new Intent(HistoryActivity.this, ShowRouteActivity.class);
                                intent.putExtra("PathToAccGpsFile", dirFiles[i].getName());
                                intent.putExtra("Duration", String.valueOf(Long.valueOf(metaDataLines.get(metaDataLines.size() - position - 1)[2]) - Long.valueOf(metaDataLines.get(metaDataLines.size() - position - 1)[1])));
                                intent.putExtra("StartTime", metaDataLines.get(metaDataLines.size() - position - 1)[1]);
                                intent.putExtra("State", Integer.valueOf(metaDataLines.get(metaDataLines.size() - position - 1)[3]));
                                Log.d(TAG, "pathToAccGpsFile: " + dirFiles[i].getName());
                                Log.d(TAG, "Duration: " + String.valueOf(Long.valueOf(metaDataLines.get(metaDataLines.size() - position - 1)[2]) - Long.valueOf(metaDataLines.get(metaDataLines.size() - position - 1)[1])));
                                Log.d(TAG, "StartTime: " + metaDataLines.get(metaDataLines.size() - position - 1)[1]);
                                Log.d(TAG, "State: " + metaDataLines.get(metaDataLines.size() - position - 1)[3]);

                                startActivity(intent);
                            }
                        }
                    }
                }
            });

            holder.btnDelete.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Log.d(TAG,"Delete Button Clicked");
                    fireDeletePrompt(position, MyArrayAdapter.this);
                }
            });
            return row;
        }

        class Holder {
            TextView rideDate;
            TextView rideTime;
            TextView duration;
            TextView distance;
            TextView distanceUnit;
            TextView message;
            ImageButton status;
            ImageButton btnDelete;
        }
    }

    public void fireDeletePrompt(int position, MyArrayAdapter arrayAdapter) {
        AlertDialog.Builder alert = new AlertDialog.Builder(HistoryActivity.this);
        alert.setTitle(getString(R.string.warning));
        alert.setMessage(getString(R.string.delete_file_warning));
        alert.setPositiveButton(R.string.delete_ride_approve, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                File[] dirFiles = getFilesDir().listFiles();
                Log.d(TAG, "btnDelete.onClick() dirFiles: " + Arrays.deepToString(dirFiles));
                String clicked = (String) listView.getItemAtPosition(position);
                Log.d(TAG, "btnDelete.onClick() clicked: " + clicked);
                clicked = clicked.replace("#", "").split(";")[0];
                if (dirFiles.length != 0) {
                    for (int i = 0; i < dirFiles.length; i++) {
                        File actualFile = dirFiles[i];
                        if (actualFile.getName().startsWith(clicked + "_") || actualFile.getName().startsWith("accEvents"+clicked)) {

                            /** don't delete the following line! */
                            Log.i(TAG, actualFile.getName() + " deleted: " + actualFile.delete());
                        }
                    }
                }
                String content = "";
                try (BufferedReader br = new BufferedReader(new FileReader(HistoryActivity.this.getFileStreamPath("metaData.csv")))) {
                    String line;

                    while ((line = br.readLine()) != null) {
                        if (!line.split(",")[0].equals(clicked)) {
                            content += line += System.lineSeparator();
                        }
                    }
                    overWriteFile(content,"metaData.csv", HistoryActivity.this);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                Toast.makeText(HistoryActivity.this,R.string.ride_deleted,Toast.LENGTH_SHORT).show();
                refreshMyRides();
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });
        alert.show();

    }

    public void fireUploadPrompt() {
        AlertDialog.Builder alert = new AlertDialog.Builder(HistoryActivity.this);
        alert.setTitle(getString(R.string.warning));
        alert.setMessage(getString(R.string.upload_file_warning));
        alert.setPositiveButton(R.string.upload, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                writeBooleanToSharedPrefs("uploadWarningShown",true,"simraPrefs",HistoryActivity.this);
                Intent intent = new Intent(HistoryActivity.this, UploadService.class);
                startService(intent);
                Toast.makeText(HistoryActivity.this,getString(R.string.upload_started),Toast.LENGTH_SHORT).show();
                if (exitWhenDone) {
                    HistoryActivity.this.moveTaskToBack(true);
                }
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });
        alert.show();

    }
}
