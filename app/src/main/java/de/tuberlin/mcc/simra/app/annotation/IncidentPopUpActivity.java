package de.tuberlin.mcc.simra.app.annotation;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import de.tuberlin.mcc.simra.app.R;

import static de.tuberlin.mcc.simra.app.util.Utils.checkForAnnotation;
import static de.tuberlin.mcc.simra.app.util.Utils.fileExists;
import static de.tuberlin.mcc.simra.app.util.Utils.getAppVersionNumber;
import static de.tuberlin.mcc.simra.app.util.Utils.overWriteFile;

public class IncidentPopUpActivity extends AppCompatActivity {

    String[] incidentTypes = new String[9];
    String[] locations = new String[7];
    LinearLayout doneButton;
    LinearLayout backButton;
    RelativeLayout exitButton;
    Boolean incidentSaved = false;
    String rideID;
    String incidentKey;
    String[] previousAnnotation;
    boolean temp;
    int state = 0;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Log tag
    private static final String TAG = "IncidentPopUpAct_LOG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rideID = getIntent().getStringExtra("Ride_ID");

        incidentKey = getIntent().getStringExtra("Incident_Key");

        temp = getIntent().getBooleanExtra("Incident_temp", false);

        state = getIntent().getIntExtra("State",0);
        if (state < 2) {
            setContentView(R.layout.incident_popup_layout);
        } else {
            setContentView(R.layout.incident_popup_layout_uneditable_incident);
        }

        // Scale the activity so that it is smaller than the activity which called it
        // (ShowRouteActivity), floating on top of it (ShowRouteActivity is still visible
        // in the background thanks to the custom style 'PopUpWindow' in styles.xml)

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);

        incidentTypes = getResources().getStringArray(R.array.incidenttypelist);
        locations = getResources().getStringArray(R.array.phoneLocations);

        previousAnnotation = loadPreviousAnnotation();

        Spinner incidentTypeSpinner = findViewById(R.id.incidentTypeSpinner);
        EditText incidentDescription = findViewById(R.id.EditTextDescriptionBody);
        CheckBox scarinessCheckBox = findViewById(R.id.scarinessCheckBox);
        CheckBox involvedType1CheckBox = findViewById(R.id.involvedType1);
        CheckBox involvedType2CheckBox = findViewById(R.id.involvedType2);
        CheckBox involvedType3CheckBox = findViewById(R.id.involvedType3);
        CheckBox involvedType4CheckBox = findViewById(R.id.involvedType4);
        CheckBox involvedType5CheckBox = findViewById(R.id.involvedType5);
        CheckBox involvedType6CheckBox = findViewById(R.id.involvedType6);
        CheckBox involvedType7CheckBox = findViewById(R.id.involvedType7);
        CheckBox involvedType8CheckBox = findViewById(R.id.involvedType8);
        CheckBox involvedType9CheckBox = findViewById(R.id.involvedType9);
        CheckBox involvedType10CheckBox = findViewById(R.id.involvedType10);

        if (state == 2) {
            incidentTypeSpinner.setEnabled(false);
            incidentDescription.setEnabled(false);
            scarinessCheckBox.setEnabled(false);
            involvedType1CheckBox.setEnabled(false);
            involvedType2CheckBox.setEnabled(false);
            involvedType3CheckBox.setEnabled(false);
            involvedType4CheckBox.setEnabled(false);
            involvedType5CheckBox.setEnabled(false);
            involvedType6CheckBox.setEnabled(false);
            involvedType7CheckBox.setEnabled(false);
            involvedType8CheckBox.setEnabled(false);
            involvedType9CheckBox.setEnabled(false);
            involvedType10CheckBox.setEnabled(false);
        }
        if (previousAnnotation != null && previousAnnotation.length > 7) {

            if (previousAnnotation[8].length() > 0) {
                incidentTypeSpinner.setSelection(Integer.valueOf(previousAnnotation[8]));
            }
            if (previousAnnotation[9].equals("1")) {
                involvedType1CheckBox.setChecked(true);
            }
            if (previousAnnotation[10].equals("1")) {
                involvedType2CheckBox.setChecked(true);
            }
            if (previousAnnotation[11].equals("1")) {
                involvedType3CheckBox.setChecked(true);
            }
            if (previousAnnotation[12].equals("1")) {
                involvedType4CheckBox.setChecked(true);
            }
            if (previousAnnotation[13].equals("1")) {
                involvedType5CheckBox.setChecked(true);
            }
            if (previousAnnotation[14].equals("1")) {
                involvedType6CheckBox.setChecked(true);
            }
            if (previousAnnotation[15].equals("1")) {
                involvedType7CheckBox.setChecked(true);
            }
            if (previousAnnotation[16].equals("1")) {
                involvedType8CheckBox.setChecked(true);
            }
            if (previousAnnotation[17].equals("1")) {
                involvedType9CheckBox.setChecked(true);
            }

            if (previousAnnotation[18].length() > 0) {
                if (previousAnnotation[18].equals("1")) {
                    scarinessCheckBox.setChecked(true);
                }
            }
            incidentDescription.setText(previousAnnotation[19].replaceAll(";linebreak;", System.lineSeparator()).replaceAll(";komma;", ","));
            if (previousAnnotation.length>20 && previousAnnotation[20].equals("1")) {
                involvedType10CheckBox.setChecked(true);
            }
        }
        if (state < 2) {
            doneButton = findViewById(R.id.save_button);
            backButton = findViewById(R.id.back_button);

            doneButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        doneButton.setElevation(0.0f);
                        doneButton.setBackground(getDrawable(R.drawable.button_pressed));
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        doneButton.setElevation(2 * IncidentPopUpActivity.this.getResources().getDisplayMetrics().density);
                        doneButton.setBackground(getDrawable(R.drawable.button_unpressed));
                    }
                    return false;
                }

            });

            backButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        backButton.setElevation(0.0f);
                        backButton.setBackground(getDrawable(R.drawable.button_pressed));
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        backButton.setElevation(2 * IncidentPopUpActivity.this.getResources().getDisplayMetrics().density);
                        backButton.setBackground(getDrawable(R.drawable.button_unpressed));
                    }
                    return false;
                }

            });

            if (getIntent().getExtras() != null) {

                String lat = getIntent().getStringExtra("Incident_latitude");
                String lon = getIntent().getStringExtra("Incident_longitude");
                String ts = getIntent().getStringExtra("Incident_timeStamp");
                String bike = getIntent().getStringExtra("Incident_bike");
                String child = getIntent().getStringExtra("Incident_child");
                String trailer = getIntent().getStringExtra("Incident_trailer");
                String pLoc = getIntent().getStringExtra("Incident_pLoc");

                // onClick-behavior for 'Done inserting description'-button: save incident
                // data to file.

                doneButton.setOnClickListener((View v) -> {

                    // Instead of writing the String selected items in the spinner,
                    // we use an int to save disk space and bandwidth
                    int incidentType = incidentTypeSpinner.getSelectedItemPosition();
                    String description = incidentDescription.getText().toString().replace(System.lineSeparator(), ";linebreak;").replace(",", ";komma;");
                    int scariness = 0;
                    if (scarinessCheckBox.isChecked()) {
                        scariness = 1;
                    }
                    String[] checkBoxValuesWithouti10 = {"0", "0", "0", "0", "0", "0", "0", "0", "0"};

                    if (involvedType1CheckBox.isChecked()) {
                        checkBoxValuesWithouti10[0] = "1";
                    }
                    if (involvedType2CheckBox.isChecked()) {
                        checkBoxValuesWithouti10[1] = "1";
                    }
                    if (involvedType3CheckBox.isChecked()) {
                        checkBoxValuesWithouti10[2] = "1";
                    }
                    if (involvedType4CheckBox.isChecked()) {
                        checkBoxValuesWithouti10[3] = "1";
                    }
                    if (involvedType5CheckBox.isChecked()) {
                        checkBoxValuesWithouti10[4] = "1";
                    }
                    if (involvedType6CheckBox.isChecked()) {
                        checkBoxValuesWithouti10[5] = "1";
                    }
                    if (involvedType7CheckBox.isChecked()) {
                        checkBoxValuesWithouti10[6] = "1";
                    }
                    if (involvedType8CheckBox.isChecked()) {
                        checkBoxValuesWithouti10[7] = "1";
                    }
                    if (involvedType9CheckBox.isChecked()) {
                        checkBoxValuesWithouti10[8] = "1";
                    }
                    String i10Value = "0";
                    if (involvedType10CheckBox.isChecked()) {
                        i10Value = "1";
                    }


                    String incidentString = incidentKey + "," + lat + "," + lon + "," + ts + ","
                            + bike + "," + child + "," + trailer + "," + pLoc + "," + incidentType + "," + Arrays.toString(checkBoxValuesWithouti10).replace(" ", "").replace("[", "").replace("]", "") + "," + scariness + "," + description + "," + i10Value;

                    overwriteIncidentFile(rideID, incidentKey, incidentKey + "," + lat + "," + lon + "," + ts + ","
                            + bike + "," + child + "," + trailer + "," + pLoc + "," + incidentType + "," + Arrays.toString(checkBoxValuesWithouti10).replace(" ", "").replace("[", "").replace("]", "") + "," + scariness + "," + description + "," + i10Value);

                    incidentSaved = true;
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result", incidentString);
                    returnIntent.putExtra("temp",temp);
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                });

                // Return to ShowRouteActivity without saving the annotated incidents
                backButton.setOnClickListener((View v) -> {
                    incidentSaved = false;
                    finish();
                });

            }
        } else {
            exitButton = findViewById(R.id.exitButton);
            exitButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        exitButton.setElevation(0.0f);
                        exitButton.setBackground(getDrawable(R.drawable.button_pressed));
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        exitButton.setElevation(2 * IncidentPopUpActivity.this.getResources().getDisplayMetrics().density);
                        exitButton.setBackground(getDrawable(R.drawable.button_unpressed));
                    }
                    return false;
                }

            });
            exitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    incidentSaved = false;
                    finish();
                }
            });

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
        if (state < 2) {
            if (incidentSaved) {
                Toast.makeText(this, getString(R.string.editingIncidentCompleted), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editingIncidentAborted), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public String[] loadPreviousAnnotation() {

        String[] result = null;
        Log.d(TAG, "loadPreviousAnnotation rideID: " + rideID + " incidentKey: " + incidentKey);

        String pathToIncidents = "accEvents" + rideID + ".csv";
        if (temp) {
            pathToIncidents = "Temp" + pathToIncidents;
        }

        if (fileExists(pathToIncidents, this)) {

            BufferedReader reader = null;

            try {

                reader = new BufferedReader(new FileReader(getFileStreamPath(pathToIncidents)));

            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
                Log.d(TAG, "Incident file not found");
            }
            try {

                reader.readLine(); // this will read the first line
                reader.readLine();

                String line = null;

                while ((line = reader.readLine()) != null) { //loop will run from 2nd line

                    String[] incidentProps = line.split(",", -1);

                    if (incidentProps[0].equals(incidentKey)) {

                        result = incidentProps;

                    }

                }


            } catch (IOException ioe) {
                ioe.printStackTrace();
                Log.d(TAG, "Problems reading AccEvents file");

            }

        } else {
            Log.d(TAG, pathToIncidents + " doesn't exist");
        }

        Log.d(TAG, "loadPreviousAnnotation() result: " + Arrays.toString(result));
        return result;

    }

    public void overwriteIncidentFile(String rideID, String incidentKey, String newAnnotation) {

        String path = "accEvents" + rideID + ".csv";
        if (temp) {
            path = "Temp" + path;
        }

        String contentOfNewFile = "";
        int appVersion = getAppVersionNumber(IncidentPopUpActivity.this);
        String fileVersion = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(getFileStreamPath(path)))) {
            contentOfNewFile += reader.readLine();
            contentOfNewFile += System.lineSeparator();

            if (contentOfNewFile.contains("#")) {
                String[] fileInfoArray = contentOfNewFile.split("#");
                fileVersion = fileInfoArray[1];
            }

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
        String fileInfoLine = appVersion + "#" + fileVersion + System.lineSeparator();
        Log.d(TAG, "fileInfoLine: " + fileInfoLine + " contentOfNewFile: " + contentOfNewFile);
        overWriteFile((contentOfNewFile), path, this);

    }

}
