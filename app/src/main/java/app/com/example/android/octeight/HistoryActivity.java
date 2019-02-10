package app.com.example.android.octeight;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import java.util.Date;

public class HistoryActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // Log tag
    private static final String TAG = "HistoryActivity_LOG";

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

        listView = (ListView) findViewById(R.id.listView);

        if (fileExists("metaData.csv")) {

            metaDataFile = getFileStreamPath("metaData.csv");
            Log.d(TAG, "fileExists: metaData.csv: " + metaDataFile.toString());
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
            Log.d(TAG, "ridesArr: " + Arrays.toString(ridesArr));
            Log.d(TAG, "ridesList: " + Arrays.deepToString(ridesList.toArray()));
            for (
                    String[] i : ridesList)

            {
                Log.d(TAG, "String[] i : ridesList: " + Arrays.toString(i));
                ridesArr[Integer.parseInt(i[0])] = listToTextShape(i);
                Log.d(TAG, "ridesArr: " + Arrays.toString(ridesArr));
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

                    String prefix = "/data/user/0/app.com.example.android.octeight/files/";
                    clicked = clicked.split("ID: ")[1].split(" ")[0];
                    clicked = prefix + clicked;
                    Log.d(TAG, "clicked: " + clicked);
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

            //Code für einen Testfall in dem automatisch Daten für das metaData File erstellt werden:
            /*
            try {

                metaDataFile = getFileStreamPath("metaData.csv");
                metaDataFile.createNewFile();

                appendToFile("key, startTime, endTime, annotated"
                        + System.lineSeparator(), metaDataFile);

                appendToFile("0,1549113603,1549114203,false"
                        + System.lineSeparator(), metaDataFile);

                appendToFile("1,1549114203,1549114503,false"
                        + System.lineSeparator(), metaDataFile);

                appendToFile("2,1549373703,1549374003,true"
                        + System.lineSeparator(), metaDataFile);

                appendToFile("3,1549374003,1549374123,false"
                        + System.lineSeparator(), metaDataFile);

            } catch (IOException e) {
                e.printStackTrace();
            }
            */

            Snackbar snackbar = Snackbar.make(findViewById(R.id.drawer_layout), (getString(R.string.noHistoryDE)), Snackbar.LENGTH_LONG).setAction("refresh", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO
                    // searching for accData and generating / upadting the metaData File
                    Toast.makeText(getApplicationContext(), getString(R.string.nextPatchDE), Toast.LENGTH_LONG).show();

                }
            });
            snackbar.show();

        }


        // This button will change. Every list item needs its own button (maybe they can
        // be created dynamically) where ShowRouteActivity gets started with the "Ride" (see Ride
        // class) the list item represents.
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        Log.d(TAG, "fab" + fab);
        fab.setOnClickListener(new View.OnClickListener()

        {

            @Override
            public void onClick(View view) {

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
                    Snackbar.make(view, getString(R.string.selectedRideInfoDE) + new Date(Long.valueOf(startTime)), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    // Start ShowRouteActivity with the selected Ride.
                    Intent intent = new Intent(HistoryActivity.this, ShowRouteActivity.class);
                    intent.putExtra("PathToAccGpsFile", pathToAccGpsFile);
                    intent.putExtra("Duration", duration);
                    intent.putExtra("StartTime", startTime);
                    intent.putExtra("State", state);
                    startActivity(intent);
                } else {
                    Snackbar.make(view, getString(R.string.errorNoRideSelectedDE) + new Date(Long.valueOf(startTime)), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }

            }
        });

        getSupportActionBar().

                setDisplayHomeAsUpEnabled(true);

        // Press immediately the button, if HistoryActivity was created automatically after the
        // recording of a route has finished
        if (

                getIntent().

                        hasExtra("PathToAccGpsFile"))

        {
            // Log.d(TAG, "getIntent.hasExtra(\"PathToAccGpsFile\")");
            fab.performClick();
            fab.setPressed(true);
            fab.invalidate();
            fab.setPressed(false);
            fab.invalidate();
        }

    }

    private void appendToFile(String str, File file) throws IOException {
        FileOutputStream writer = openFileOutput(file.getName(), MODE_APPEND);
        writer.write(str.getBytes());
        //writer.write(System.getProperty("line.separator").getBytes());
        writer.flush();
        writer.close();
    }

    public boolean fileExists(String fname) {
        File file = getBaseContext().getFileStreamPath(fname);
        return file.exists();
    }

    private String listToTextShape(String[] item) {
        String todo = "Muss noch kommentiert werden\n";
        if (item[3].contains("true")) {
            todo = "Fertig kommentiert\n";
        }
        String result = todo + new Date(Long.valueOf(item[1])).toString()
                + "\tID: " + item[0]
                + " Länge: " + (Long.valueOf(item[2]) - Long.valueOf(item[1])) / 1000
                + "Sec";

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
            Intent intent = new Intent(HistoryActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_history) {
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
            Toast.makeText(getApplicationContext(), getString(R.string.nextPatchDE), Toast.LENGTH_LONG).show();

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
}
