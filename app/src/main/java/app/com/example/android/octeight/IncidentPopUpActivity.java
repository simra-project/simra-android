package app.com.example.android.octeight;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import static app.com.example.android.octeight.Utils.fileExists;
import static app.com.example.android.octeight.Utils.lookUpIntSharedPrefs;
import static app.com.example.android.octeight.Utils.overWriteFile;

public class IncidentPopUpActivity extends AppCompatActivity {

    String[] incidentTypes = new String[9];
    String[] locations = new String[7];
    LinearLayout doneButton;
    LinearLayout backButton;
    Boolean incidentSaved = false;
    String rideID;
    String incidentKey;
    String[] previousAnnotation;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Log tag
    private static final String TAG = "IncidentPopUpAct_LOG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.incident_popup_layout);

        // Scale the activity so that it is smaller than the activity which called it
        // (ShowRouteActivity), floating on top of it (ShowRouteActivity is still visible
        // in the background thanks to the custom style 'PopUpWindow' in styles.xml)

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);

        incidentTypes = getResources().getStringArray(R.array.incidenttypelist);
        locations = getResources().getStringArray(R.array.phoneLocations);

        rideID = getIntent().getStringExtra("Ride_ID");

        incidentKey = getIntent().getStringExtra("Incident_Key");

        previousAnnotation = loadPreviousAnnotation
                (rideID, incidentKey);

        final Spinner incidentTypeSpinner = findViewById(R.id.incidentTypeSpinner);
        final LinearLayout involvedCheckBoxesLinearLayout = findViewById(R.id.involvedCheckboxes);
        final EditText incidentDescription = findViewById(R.id.EditTextDescriptionBody);
        final Switch scarinessSwitch = findViewById(R.id.scarinessSwitch);
        String[] checkBoxValues = {"0","0","0","0","0","0","0","0","0"};
        if (previousAnnotation != null) {
            incidentTypeSpinner.setSelection(Integer.valueOf(previousAnnotation[4]));

            // Load checkboxValues
            int count = involvedCheckBoxesLinearLayout.getChildCount();
            // Iterate through all children of involvedCheckBoxesLinearLayout
            for (int i = 0; i < count; i++) {
                CheckBox cb = (CheckBox) involvedCheckBoxesLinearLayout.getChildAt(i);
                // if (v instanceof CheckBox) {
                    if (previousAnnotation[9+i].length()>0) {
                        if (previousAnnotation[9 + i].equals("1")) {
                            cb.setChecked(true);
                            checkBoxValues[i] = "1";
                        }
                    }
                // }
            }
            if(previousAnnotation[18].length()>0){
                if(previousAnnotation[18].equals("1")){
                    scarinessSwitch.setChecked(true);
                }
            }
            incidentDescription.setText(previousAnnotation[19]);
        }

        doneButton = findViewById(R.id.save_button);
        backButton = findViewById(R.id.back_button);

        if (getIntent().getExtras() != null) {

            String lat = getIntent().getStringExtra("Incident_latitude");
            String lon = getIntent().getStringExtra("Incident_longitude");
            String ts = getIntent().getStringExtra("Incident_timeStamp");
            String bike = getIntent().getStringExtra("Incident_bike");
            String child = getIntent().getStringExtra("Incident_child");
            String trailer = getIntent().getStringExtra("Incident_trailer");
            String pLoc = getIntent().getStringExtra("Incident_pLoc");
            String pathToAccDat = getIntent().getStringExtra("Incident_accDat");

            // onClick-behavior for 'Done inserting description'-button: save incident
            // data to file.

            doneButton.setOnClickListener((View v) -> {

                // Instead of writing the String selected items in the spinner,
                // we use an int to save disk space and bandwidth
                int incidentType = incidentTypeSpinner.getSelectedItemPosition();
                String description = incidentDescription.getText().toString();
                int scariness = 0;
                if (scarinessSwitch.isChecked()){
                    scariness = 1;
                }

                /*
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
                */
                overwriteIncidentFile(rideID, incidentKey, incidentKey + "," + lat + "," + lon + "," + ts + ","
                        + bike + "," + child + "," + trailer + "," + pLoc + "," + incidentType + "," + Arrays.toString(checkBoxValues).replace(" ", "").replace("[","").replace("]","") + "," + scariness + "," + description);

                incidentSaved = true;

                String incidentString = incidentKey + "," + lat + "," + lon + "," + ts + ","
                        + "," + incidentType + "," + locationType + "," + description;

                // writeIntToSharePrefs("Settings-PhoneLocation", locationType, "simraPrefs", this);

                Intent returnIntent = new Intent();
                returnIntent.putExtra("result", incidentString);
                setResult(Activity.RESULT_OK, returnIntent);
                //setResult(Activity.RESULT_CANCELED, returnIntent);

                finish();

            });

            // Return to ShowRouteActivity without saving the annotated incidents
            backButton.setOnClickListener((View v) -> {
                incidentSaved = false;
                finish();
            });

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
        if (incidentSaved) {
            Toast.makeText(this, getString(R.string.editingIncidentCompleted), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.editingIncidentAborted), Toast.LENGTH_SHORT).show();
        }
    }

    public String[] loadPreviousAnnotation(String rideID, String incidentKey) {

        String[] result = null;
        Log.d(TAG, "loadPreviousAnnotation rideID: " + rideID + " incidentKey: " + incidentKey);

        String pathToIncidents = "accEvents" + rideID + ".csv";

        if (fileExists(pathToIncidents, this)) {

            BufferedReader reader = null;

            try {

                reader = new BufferedReader(new FileReader(getFileStreamPath(pathToIncidents)));

            } catch (FileNotFoundException fnfe) {

                fnfe.printStackTrace();
                Log.i(TAG, "Incident file not found");

            }
            try {

                reader.readLine(); // this will read the first line

                String line = null;

                while ((line = reader.readLine()) != null) { //loop will run from 2nd line

                    String[] incidentProps = line.split(",", -1);

                    if (incidentProps[0].equals(incidentKey)) {

                        result = incidentProps;

                    }

                }


            } catch (IOException ioe) {
                ioe.printStackTrace();
                Log.i(TAG, "Problems reading AccEvents file");

            }

        } else {
            Log.d(TAG, "didn't enter if, " + pathToIncidents + " doesn't exist");
        }

        Log.d(TAG, "loadPreviousAnnotation() result: " + Arrays.toString(result));
        return result;

    }

    public void overwriteIncidentFile(String rideID, String incidentKey, String newAnnotation) {

        String path = "accEvents" + rideID + ".csv";

        String contentOfNewFile = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(getFileStreamPath(path)))) {

            contentOfNewFile += reader.readLine();
            contentOfNewFile += System.lineSeparator();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] oldIncident = line.split(",", -1);
                if (oldIncident[0].equals(incidentKey)) {
                    contentOfNewFile += newAnnotation;
                    contentOfNewFile += System.lineSeparator();
                    Log.d(TAG, "overwriting \"" + line + "\" with \"" + contentOfNewFile);
                } else {
                    contentOfNewFile += line;
                    contentOfNewFile += System.lineSeparator();
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        overWriteFile(contentOfNewFile, path, this);

    }

}
