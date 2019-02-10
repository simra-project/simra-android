package app.com.example.android.octeight;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class IncidentPopUpActivity extends AppCompatActivity {

    String[] incidentTypes = new String[4];
    String[] locations = new String[5];
    LinearLayout doneButton;
    LinearLayout backButton;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Log tag
    private static final String TAG = "IncidentPopUpActivity_LOG";

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
        setContentView(R.layout.incident_popup_layout);

        // Scale the activity so that it is smaller than the activity which called it
        // (ShowRouteActivity), floating on top of it (ShowRouteActivity is still visible
        // in the background thanks to the custom style 'PopUpWindow' in styles.xml)

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        incidentTypes = getResources().getStringArray(R.array.incidenttypelist);
        locations = getResources().getStringArray(R.array.locations);


        final Spinner incidentTypeSpinner = (Spinner) findViewById(R.id.incidentTypeSpinner);

        final Spinner locationTypeSpinner = (Spinner) findViewById(R.id.locationSpinner);

        final EditText incidentDescription = (EditText) findViewById(R.id.EditTextDescriptionBody);

        doneButton = findViewById(R.id.save_button);
        backButton = findViewById(R.id.back_button);

        if (getIntent().getExtras() != null) {

            String lat = (String) getIntent().getExtras().getSerializable("Incident_latitude");

            String lon = (String) getIntent().getExtras().getSerializable("Incident_longitude");

            String date = (String) getIntent().getExtras().getSerializable("Incident_date");

            String pathToAccDat = (String) getIntent().getExtras().getSerializable("Incident_accDat");

            String key = getIntent().getStringExtra("ID");


            // onClick-behavior for 'Done inserting description'-button: save incident
            // data to file.

            doneButton.setOnClickListener((View v) -> {

                String incidentType = incidentTypeSpinner.getSelectedItem().toString();
                String locationType = locationTypeSpinner.getSelectedItem().toString();
                String description = incidentDescription.getText().toString();

                // Instead of writing the String selected items in the spinner,
                // we use an int tosave disk space and bandwidth
                int incidentIndex = 0;
                for (int i = 0; i < incidentTypes.length; i++) {
                    if (incidentType.equals(incidentTypes[i])) {
                        incidentIndex = i;
                    }
                }
                int locationIndex = 0;
                for (int i = 0; i < locations.length; i++) {
                    if (locationType.equals(locations[i])) {
                        locationIndex = i;
                    }
                }


                try {

                    incidentFile = getFileStreamPath("incidentData.csv");

                    if (!fileExists("incidentData.csv")) {

                        incidentFile.createNewFile();

                        appendToFile("key,lat,lon,date,path_to_AccFile,incidentType,phoneLocation,description"
                                + System.lineSeparator(), incidentFile);

                    }

                    appendToFile(key + "," + lat + "," + lon + "," + date + ","
                            + pathToAccDat + "," + incidentIndex + "," + locationIndex + "," + description
                            + System.lineSeparator(), incidentFile);

                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                Toast.makeText(this, getString(R.string.editingIncidentCompletedDE), Toast.LENGTH_SHORT).show();
                finish();

            });


            backButton.setOnClickListener((View v) -> {
                Toast.makeText(this, getString(R.string.editingIncidentAbortedDE), Toast.LENGTH_SHORT).show();

                finish();
            });

        } else {
            Log.i("TAG", "getIntent().getExtras() == null");
            doneButton.setOnClickListener((View v) -> {
                Toast.makeText(this, getString(R.string.noIntentExtrasDE), Toast.LENGTH_SHORT).show();
                return;
            });
        }


    }

    public boolean fileExists(String fname) {
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
