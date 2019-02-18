package app.com.example.android.octeight;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import java.util.LinkedList;

import static app.com.example.android.octeight.Utils.appendToFile;
import static app.com.example.android.octeight.Utils.getUniqueUserID;
import static java.util.stream.IntStream.concat;
import static java.util.stream.IntStream.range;
import static org.apache.commons.lang3.ArrayUtils.toArray;

public class DemographicActivity extends AppCompatActivity {

    //TODO: XML Objects
    //TODO: age Spinner

    Spinner ageSpinner;
    String[] ageArray =new String[70];
    RadioGroup genderRadio;
    Spinner countrySpinner;
    String[] countries = new String[2];
    Spinner districtSpinner;
    String[] districts = new String[12];
    Spinner bicycleSpinner;
    String[] bicycleType = new String[5];
    FloatingActionButton saveBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demographic);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        saveBtn = findViewById(R.id.saveFab);
        //fab.setOnClickListener(new View.OnClickListener() {            public void onClick(View view) {                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show(); }});
        countries = getResources().getStringArray(R.array.countries);
        districts = getResources().getStringArray(R.array.districts);
        bicycleType = getResources().getStringArray(R.array.bicycleType);


        ageSpinner = findViewById(R.id.ageSpinner);
        for(int i = 0;i<70;i++){
            ageArray[i] = String.valueOf(i+1950);
        }

        ArrayAdapter<String> ageArrayAdapter = new ArrayAdapter<String> (this, android.R.layout.simple_spinner_item,  ageArray);
        ageArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ageSpinner.setAdapter(ageArrayAdapter);
        countrySpinner = findViewById(R.id.countrySpinner);

        districtSpinner = findViewById(R.id.districtSpinner);

        bicycleSpinner = findViewById(R.id.bicycleTypeSpinner);

        genderRadio = findViewById(R.id.genderRadio);


        saveBtn.setOnClickListener((View v) -> {

            String yoBirth = ageSpinner.getSelectedItem().toString();
            int selectedId = genderRadio.getCheckedRadioButtonId();
            RadioButton radioButton = (RadioButton) findViewById(selectedId);
            String gender = radioButton.getText().toString();
            String city = countrySpinner.getSelectedItem().toString();
            String district = districtSpinner.getSelectedItem().toString();

            // Write the demographics to demograpicData.csv
            appendToFile(getUniqueUserID(this) + "," + yoBirth + "," + gender + "," + city + "," + district + ","
                    + System.lineSeparator(), "incidentData.csv", this);


            finish();

        });


    }
}
