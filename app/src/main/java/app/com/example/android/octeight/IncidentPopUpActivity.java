package app.com.example.android.octeight;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class IncidentPopUpActivity extends AppCompatActivity {

    Spinner incidentTypSpinner;
    String[] incidentTypes = {"kein Vorfall", "offene Türe", "Bodenunebenheit", "Unfall", "parkendes Auto", "Personen"};
    // String[] incidentTypes = getResources().getStringArray(R.array.incidentTypesDE);
    TextView incidentTypeTextView;
    Spinner locationSpinner;
    String[] locations = {"Hosentasche", "Lenker", "Jackentasche", "Hand", "Rucksack"};
    // String[] locations = getResources().getStringArray(R.array.locationsDE);
    TextView locationTextView;
    SeekBar seekBar;
    TextView seekBarTextView;
    EditText incidentDescription;
    RelativeLayout doneButton;
    RelativeLayout backButton;
    // Strings for the File
    String incTypStr;
    String locStr;
    String incDescStr = " ";
    // Bools to check if ready
    Boolean ready = false;
    Boolean incTypBool = false;
    Boolean locBool = false;

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
        setContentView(R.layout.activity_incident_pop_up);

        // Scale the activity so that it is smaller than the activity which called it
        // (ShowRouteActivity), floating on top of it (ShowRouteActivity is still visible
        // in the background thanks to the custom style 'PopUpWindow' in styles.xml)

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        // int width = dm.widthPixels;
        // int height = dm.heightPixels;

        getWindow().setLayout((int) (dm.widthPixels * .8), (int) (dm.heightPixels * .8));

        Log.i("WIDTH_DP", String.valueOf(IncidentPopUpActivity.pxToDp(dm.widthPixels) * .8));

        Log.i("HEIGHT_DP", String.valueOf(IncidentPopUpActivity.pxToDp(dm.heightPixels) * .8));

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        incTypStr = incidentTypes[0];
        locStr = locations[0];

        //init the spinners
        incidentTypSpinner = (Spinner) findViewById(R.id.incidentTypeSpinner);
        incidentTypeTextView = (TextView) findViewById(R.id.incidentTypeText);
        seekBarTextView = (TextView) findViewById(R.id.seekBarText);
        seekBarTextView.setText("Intensität: 0 von 5");
        ArrayAdapter<String> typAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, incidentTypes);
        incidentTypSpinner.setAdapter(typAdapter);
        incidentTypSpinner.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                incTypStr = incidentTypes[position];
                incidentTypeTextView.setText("Typ:\t" + incTypStr);
                seekBar = (SeekBar) findViewById(R.id.seekBar);
                seekBarTextView.setText("Intensität:\t0 von 5");
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (position != 0) {
                            seekBarTextView.setText("Intensität:\t" + (progress + 35) / 25 + " von 5");
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                incTypBool = true;
                if (locBool && incTypBool == true ) ready = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                incTypBool = false;
                ready = false;
            }
        });

        locationSpinner = (Spinner) findViewById(R.id.locationSpinner);
        locationTextView = (TextView) findViewById(R.id.locationText);
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, locations);
        locationSpinner.setAdapter(locationAdapter);
        AdapterView.OnItemSelectedListener locationListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // String locTxt = getString(R.string.locationSpinnerDE) + locations[position];
                // Log.i("TAG", "locationTextView.setText : " + locTxt);
                locStr = locations[position];
                locationTextView.setText(getString(R.string.locationSpinnerDE) + "\t" + locStr);
                locBool = true;
                if (incTypBool && locBool == true ) ready = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                locBool = false;
                ready = false;
            }
        };
        locationSpinner.setOnItemSelectedListener(locationListener);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        /*
        // Initialize sharedPrefs & editor (required for obtaining current ride key)
        sharedPrefs = getApplicationContext()
                .getSharedPreferences("simraPrefs", Context.MODE_PRIVATE);

        editor = sharedPrefs.edit();
        */
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // Find relevant views, set onClickListener

        incidentDescription = findViewById(R.id.ziel_eingabe);

        doneButton = findViewById(R.id.speichern_button);
        backButton = findViewById(R.id.zurück_button);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        /** Get all the incident information we want to store in our file from different sources:
         + ride identification key from shared preferences
         + incident description: from editText (user input)
         + latitude, longitude, date, path to file containing relevant acc data from intent extras
         */

        // String key = String.valueOf(sharedPrefs.getInt("RIDE-KEY", 0));

        // Bundle incData = getIntent().getExtras();

        if (getIntent().getExtras() != null) {

            String lat = (String) getIntent().getExtras().getSerializable("Incident_latitude");

            String lon = (String) getIntent().getExtras().getSerializable("Incident_longitude");

            String date = (String) getIntent().getExtras().getSerializable("Incident_date");

            String pathToAccDat = (String) getIntent().getExtras().getSerializable("Incident_accDat");

            String key = getIntent().getStringExtra("ID");


            // onClick-behavior for 'Done inserting description'-button: save incident
            // data to file.

            doneButton.setOnClickListener((View v) -> {

                incDescStr = incidentDescription.getText().toString();

                try {

                    if (!fileExists("incidentData.csv")) {

                        incidentFile = getFileStreamPath("incidentData.csv");

                        incidentFile.createNewFile();

                        appendToFile("key, lat, lon, date, path_to_AccFile, incidentType, Smartphone Location, description"
                                + System.lineSeparator(), incidentFile);

                        appendToFile(key + "," + lat + "," + lon + "," + date + ","
                                + pathToAccDat + "," + incTypStr + "," + locStr+ "," + incDescStr
                                + System.lineSeparator(), incidentFile);

                    } else {

                        incidentFile = getFileStreamPath("incidentData.csv");

                        appendToFile(key + "," + lat + "," + lon + "," + date + ","
                                + pathToAccDat + "," + incTypStr + "," + locStr+ "," + incDescStr
                                + System.lineSeparator(), incidentFile);

                    }

                } catch (IOException ioe) {


                }
                if (ready){
                    finish();
                }else{
                    Toast.makeText(this, getString(R.string.notReadyDE), Toast.LENGTH_SHORT).show();
                    ready = true;
                }

            });



        }else{
            Log.i("TAG", "getIntent().getExtras() == null");
            doneButton.setOnClickListener((View v) -> {
                Toast.makeText(this, getString(R.string.noIntentExtrasDE), Toast.LENGTH_SHORT);
                return;
            });
        }

        backButton.setOnClickListener((View v) -> {

            Toast.makeText(this, getString(R.string.notsafeDE), Toast.LENGTH_SHORT).show();
            finish();

        });

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~



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


    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

}
