package app.com.example.android.octeight;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class IncidentPopUp extends AppCompatActivity {

    EditText incidentDescription;

    RelativeLayout doneButton;

    // File containing incident data (one per user, analogous to meta file)

    private File incidentFile;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // SharedPrefs (same as in MainActivity) to enable continuously increasing unique
    // code for each ride => connection between incident file and individual ride files
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    SharedPreferences sharedPrefs;

    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incident_pop_up);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // Initialize sharedPrefs & editor (required for obtaining current ride key)
        sharedPrefs = getApplicationContext()
                .getSharedPreferences("simraPrefs", Context.MODE_PRIVATE);

        editor = sharedPrefs.edit();

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // Scale the activity so that it is smaller than the activity which called it
        // (ShowRouteActivity), floating on top of it (ShowRouteActivity is still visible
        // in the background thanks to the custom style 'PopUpWindow' in styles.xml)

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        getWindow().setLayout((int)(width*.8), (int) (height*.6));

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // Find relevant views, set onClickListener

        incidentDescription = findViewById(R.id.ziel_eingabe);

        doneButton = findViewById(R.id.weiter_button);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        /** Get all the incident information we want to store in our file from different sources:
                + ride identification key from shared preferences
                + incident description: from editText (user input)
                + latitude, longitude, date, path to file containing relevant acc data from intent extras
         */

        String key = String.valueOf(sharedPrefs.getInt("RIDE-KEY", 0));

        Bundle incData = getIntent().getExtras();

        if (incData == null) {

            return;

        }

        String lat = (String) getIntent().getExtras().getSerializable("Incident_latitude");

        String lon = (String) getIntent().getExtras().getSerializable("Incident_longitude");

        String date = (String) getIntent().getExtras().getSerializable("Incident_date");

        String pathToAccDat = (String) getIntent().getExtras().getSerializable("Incident_accDat");

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // onClick-behavior for 'Done inserting description'-button: save incident
        // data to file.

        doneButton.setOnClickListener((View v) -> {

            String incDesc = incidentDescription.getText().toString();

            try {

                if (!fileExists("incidentData.csv")) {

                    incidentFile = getFileStreamPath("incidentData.csv");

                    incidentFile.createNewFile();

                    appendToFile("key, lat, lon, date, path_to_AccFile, description"
                            + System.lineSeparator(), incidentFile);

                    appendToFile(key + "," + lat + "," + lon + "," + date + ","
                            + pathToAccDat + "," + incDesc
                            + System.lineSeparator(), incidentFile);

                } else {

                    incidentFile = getFileStreamPath("incidentData.csv");

                    appendToFile(key + "," + lat + "," + lon + "," + date + ","
                            + pathToAccDat + "," + incDesc
                            + System.lineSeparator(), incidentFile);

                }

            } catch (IOException ioe) {


            }

        });

    }

    public boolean fileExists(String fname){
        File file = getBaseContext().getFileStreamPath(fname);
        return file.exists();
    }

    private void appendToFile(String str, File file) throws IOException {
        FileOutputStream writer = openFileOutput(file.getName(), MODE_APPEND);
        writer.write(str.getBytes());
        //writer.write(System.getProperty("line.separator").getBytes());
        writer.flush();
        writer.close();
    }


}
