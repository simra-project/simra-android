package app.com.example.android.octeight;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import static app.com.example.android.octeight.Utils.appendToFile;

public class IncidentPopUpActivity extends AppCompatActivity {

    String[] incidentTypes = new String[4];
    String[] locations = new String[5];
    LinearLayout doneButton;
    LinearLayout backButton;
    Boolean incidentSaved = false;

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
        locations = getResources().getStringArray(R.array.locations);


        final Spinner incidentTypeSpinner =  findViewById(R.id.incidentTypeSpinner);

        final Spinner locationTypeSpinner = findViewById(R.id.locationSpinner);

        final EditText incidentDescription = findViewById(R.id.EditTextDescriptionBody);

        doneButton = findViewById(R.id.save_button);
        backButton = findViewById(R.id.back_button);

        if (getIntent().getExtras() != null) {

            String lat = getIntent().getStringExtra("Incident_latitude");
            String lon = getIntent().getStringExtra("Incident_longitude");
            String date = getIntent().getStringExtra("Incident_timeStamp");
            String pathToAccDat = getIntent().getStringExtra("Incident_accDat");
            String key = getIntent().getStringExtra("ID");


            // onClick-behavior for 'Done inserting description'-button: save incident
            // data to file.

            doneButton.setOnClickListener((View v) -> {

                String incidentType = incidentTypeSpinner.getSelectedItem().toString();
                String locationType = locationTypeSpinner.getSelectedItem().toString();
                String description = incidentDescription.getText().toString();

                // Instead of writing the String selected items in the spinner,
                // we use an int to save disk space and bandwidth
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

                // Write the incident to incidentData.csv
                appendToFile(key + "," + lat + "," + lon + "," + date + "," + pathToAccDat
                        + "," + incidentIndex + "," + locationIndex + "," + description
                        + System.lineSeparator(), "incidentData.csv", this);

                incidentSaved = true;
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
        if(incidentSaved){
            Toast.makeText(this, getString(R.string.editingIncidentCompleted), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.editingIncidentAborted), Toast.LENGTH_SHORT).show();
        }
    }
}
