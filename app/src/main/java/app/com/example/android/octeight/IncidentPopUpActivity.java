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
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import static app.com.example.android.octeight.Utils.fileExists;
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

        Spinner incidentTypeSpinner = findViewById(R.id.incidentTypeSpinner);
        LinearLayout involvedCheckBoxesLinearLayout = findViewById(R.id.involvedCheckboxes);
        EditText incidentDescription = findViewById(R.id.EditTextDescriptionBody);
        ToggleButton scarinessSwitch = findViewById(R.id.scarinessToggle);
        CheckBox involvedType1CheckBox = findViewById(R.id.involvedType1);
        CheckBox involvedType2CheckBox = findViewById(R.id.involvedType2);
        CheckBox involvedType3CheckBox = findViewById(R.id.involvedType3);
        CheckBox involvedType4CheckBox = findViewById(R.id.involvedType4);
        CheckBox involvedType5CheckBox = findViewById(R.id.involvedType5);
        CheckBox involvedType6CheckBox = findViewById(R.id.involvedType6);
        CheckBox involvedType7CheckBox = findViewById(R.id.involvedType7);
        CheckBox involvedType8CheckBox = findViewById(R.id.involvedType8);
        CheckBox involvedType9CheckBox = findViewById(R.id.involvedType9);

        if (previousAnnotation != null) {

            if(previousAnnotation[8].length()>0) {
                incidentTypeSpinner.setSelection(Integer.valueOf(previousAnnotation[8]));
            }
            if(previousAnnotation[9].equals("1")){
                involvedType1CheckBox.setChecked(true);
            }
            if(previousAnnotation[10].equals("1")){
                involvedType2CheckBox.setChecked(true);
            }
            if(previousAnnotation[11].equals("1")){
                involvedType3CheckBox.setChecked(true);
            }
            if(previousAnnotation[12].equals("1")){
                involvedType4CheckBox.setChecked(true);
            }
            if(previousAnnotation[13].equals("1")){
                involvedType5CheckBox.setChecked(true);
            }
            if(previousAnnotation[14].equals("1")){
                involvedType6CheckBox.setChecked(true);
            }
            if(previousAnnotation[15].equals("1")){
                involvedType7CheckBox.setChecked(true);
            }
            if(previousAnnotation[16].equals("1")){
                involvedType8CheckBox.setChecked(true);
            }
            if(previousAnnotation[17].equals("1")){
                involvedType9CheckBox.setChecked(true);
            }



            /*
            // Load checkboxValues
            int count = involvedCheckBoxesLinearLayout.getChildCount();
            // Iterate through all children of involvedCheckBoxesLinearLayout
            for (int i = 0; i < count; i++) {
                View v = involvedCheckBoxesLinearLayout.getChildAt(i);
                Log.d(TAG, "v.getClass(): " + v.getClass().getName());
                if (v instanceof CheckBox) {
                    if (previousAnnotation[9+i].length()>0) {
                        if (previousAnnotation[9 + i].equals("1")) {
                            Log.d(TAG, ((CheckBox) v).getText().toString()+" was checked before.");
                            ((CheckBox)v).setChecked(true);
                        }
                    }
                }
            }
            */
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
                String[] checkBoxValues = {"0","0","0","0","0","0","0","0","0"};

                if (involvedType1CheckBox.isChecked()){
                    checkBoxValues[0] = "1";
                }
                if (involvedType2CheckBox.isChecked()){
                    checkBoxValues[1] = "1";
                }
                if (involvedType3CheckBox.isChecked()){
                    checkBoxValues[2] = "1";
                }
                if (involvedType4CheckBox.isChecked()){
                    checkBoxValues[3] = "1";
                }
                if (involvedType5CheckBox.isChecked()){
                    checkBoxValues[4] = "1";
                }
                if (involvedType6CheckBox.isChecked()){
                    checkBoxValues[5] = "1";
                }
                if (involvedType7CheckBox.isChecked()){
                    checkBoxValues[6] = "1";
                }
                if (involvedType8CheckBox.isChecked()){
                    checkBoxValues[7] = "1";
                }
                if (involvedType9CheckBox.isChecked()){
                    checkBoxValues[8] = "1";
                }

                /*
                // Load checkboxValues
                int count = involvedCheckBoxesLinearLayout.getChildCount();
                // Iterate through all children of involvedCheckBoxesLinearLayout
                for (int i = 0; i < count; i++) {
                    View w = involvedCheckBoxesLinearLayout.getChildAt(i);
                    Log.d(TAG, "w.getClass(): " + w.getClass().getName());
                    if (w instanceof CheckBox && ((CheckBox) w).isChecked()) {
                        Log.d(TAG, ((CheckBox) v).getText().toString()+" is checked now.");
                        checkBoxValues[i] = "1";
                    }
                }
                */
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


                String incidentString = incidentKey + "," + lat + "," + lon + "," + ts + ","
                        + bike + "," + child + "," + trailer + "," + pLoc + "," + incidentType + "," + Arrays.toString(checkBoxValues).replace(" ", "").replace("[","").replace("]","") + "," + scariness + "," + description;

                overwriteIncidentFile(rideID, incidentKey, incidentKey + "," + lat + "," + lon + "," + ts + ","
                        + bike + "," + child + "," + trailer + "," + pLoc + "," + incidentType + "," + Arrays.toString(checkBoxValues).replace(" ", "").replace("[","").replace("]","") + "," + scariness + "," + description);

                incidentSaved = true;
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
