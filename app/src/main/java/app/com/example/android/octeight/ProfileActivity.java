package app.com.example.android.octeight;

import android.content.res.Resources;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.Locale;

import static app.com.example.android.octeight.Utils.lookUpIntSharedPrefs;
import static app.com.example.android.octeight.Utils.lookUpSharedPrefs;
import static app.com.example.android.octeight.Utils.writeIntToSharePrefs;

public class ProfileActivity extends AppCompatActivity {
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Log tag
    private static final String TAG = "ProfileActivity_LOG";

    String[] ageGroups = new String[6];
    String[] genders = new String[4];
    String[] regions = new String[3];
    String[] berDistricts = new String[13];
    String[] lonDistricts = new String[34];
    String[] bikeTypes = new String[5];
    String[] phoneLocations = new String[7];

    LinearLayout saveButton;
    LinearLayout abortButton;

    boolean profileSaved = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ageGroups = getResources().getStringArray(R.array.ageGroupList);
        genders = getResources().getStringArray(R.array.genderList);
        regions = getResources().getStringArray(R.array.regionList);
        berDistricts = getResources().getStringArray(R.array.districtBERList);
        lonDistricts = getResources().getStringArray(R.array.districtLONList);
        bikeTypes = getResources().getStringArray(R.array.bikeTypeList);
        phoneLocations = getResources().getStringArray(R.array.locations);

        Spinner ageGroupSpinner =  findViewById(R.id.ageGroupSpinner);
        Spinner genderSpinner =  findViewById(R.id.genderSpinner);
        Spinner regionSpinner =  findViewById(R.id.regionSpinner);
        Space regionDistrictSpace = findViewById(R.id.regionDistrictSpace);
        TextView districtTitle = findViewById(R.id.districtSpinnerTitle);
        Spinner berDistrictSpinner =  findViewById(R.id.districtBERSpinner);
        Spinner lonDistrictSpinner =  findViewById(R.id.districtLONSpinner);
        Spinner bikeSpinner =  findViewById(R.id.bikeTypeSpinner);
        Spinner locationTypeSpinner = findViewById(R.id.locationTypeSpinner);

        // regionDistrictSpace.setVisibility(View.VISIBLE);
        // regionSpinner.setOnItemClickListener();



        int[] previousProfile = loadPreviousProfile();

        saveButton = findViewById(R.id.done_button);
        abortButton = findViewById(R.id.abort_button);

        ageGroupSpinner.setSelection(previousProfile[0]);
        genderSpinner.setSelection(previousProfile[1]);
        regionSpinner.setSelection(previousProfile[2]);

        berDistrictSpinner.setSelection(previousProfile[3]);
        lonDistrictSpinner.setSelection(previousProfile[4]);
        bikeSpinner.setSelection(previousProfile[5]);
        locationTypeSpinner.setSelection(previousProfile[6]);

        Log.d(TAG, "previousProfile[2]: " + previousProfile[2]);
        if(previousProfile[2] == 1){
            regionDistrictSpace.setVisibility(View.VISIBLE);
            districtTitle.setVisibility(View.VISIBLE);
            berDistrictSpinner.setVisibility(View.VISIBLE);
        } else if (previousProfile[2] == 2){
            regionDistrictSpace.setVisibility(View.VISIBLE);
            districtTitle.setVisibility(View.VISIBLE);
            lonDistrictSpinner.setVisibility(View.VISIBLE);
        }

        regionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                // TODO Auto-generated method stub
                if(regionSpinner.getSelectedItemId()==1){
                    regionDistrictSpace.setVisibility(View.VISIBLE);
                    districtTitle.setVisibility(View.VISIBLE);
                    berDistrictSpinner.setVisibility(View.VISIBLE);
                    lonDistrictSpinner.setVisibility(View.GONE);
                    lonDistrictSpinner.setSelection(0);
                } else if (regionSpinner.getSelectedItemId()==2){
                    regionDistrictSpace.setVisibility(View.VISIBLE);
                    districtTitle.setVisibility(View.VISIBLE);
                    lonDistrictSpinner.setVisibility(View.VISIBLE);
                    berDistrictSpinner.setVisibility(View.GONE);
                    berDistrictSpinner.setSelection(0);
                } else {
                    regionDistrictSpace.setVisibility(View.GONE);
                    districtTitle.setVisibility(View.GONE);
                    berDistrictSpinner.setVisibility(View.GONE);
                    berDistrictSpinner.setSelection(0);
                    lonDistrictSpinner.setVisibility(View.GONE);
                    lonDistrictSpinner.setSelection(0);
                }
                String mSupplier=regionSpinner.getSelectedItem().toString();

                Log.d(TAG,"Selected item :" + mSupplier);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }
        });

        saveButton.setOnClickListener((View v) -> {

            int ageGroup = ageGroupSpinner.getSelectedItemPosition();
            int gender = genderSpinner.getSelectedItemPosition();
            int region = regionSpinner.getSelectedItemPosition();
            int berDistrict = berDistrictSpinner.getSelectedItemPosition();
            int lonDistrict = lonDistrictSpinner.getSelectedItemPosition();
            int bike = bikeSpinner.getSelectedItemPosition();
            int locationType = locationTypeSpinner.getSelectedItemPosition();

            writeIntToSharePrefs("Profile-Age", ageGroup,"simraPrefs", ProfileActivity.this);
            writeIntToSharePrefs("Profile-Gender", gender,"simraPrefs", ProfileActivity.this);
            writeIntToSharePrefs("Profile-Region", region,"simraPrefs", ProfileActivity.this);
            writeIntToSharePrefs("Profile-berDistrict", berDistrict,"simraPrefs", ProfileActivity.this);
            writeIntToSharePrefs("Profile-lonDistrict", lonDistrict,"simraPrefs", ProfileActivity.this);
            writeIntToSharePrefs("Profile-bikeType", bike,"simraPrefs", ProfileActivity.this);
            writeIntToSharePrefs("Profile-phoneLocation", locationType,"simraPrefs", ProfileActivity.this);

            /*
            // Instead of writing the String selected items in the spinner,
            // we use an int to save disk space and bandwidth

            int ageIndex = 0;
            for (int i = 0; i < ageGroups.length; i++) {
                if (ageGroup == (Integer.valueOf(ageGroups[i]))) {
                    ageIndex = i;
                }
            }

            writeIntToSharePrefs("Profile-Age", ageIndex,"simraPrefs", ProfileActivity.this);

            int genderIndex = 0;
            for (int i = 0; i < genders.length; i++) {
                if (gender == (Integer.valueOf(genders[i]))) {
                    genderIndex = i;
                }
            }

            writeIntToSharePrefs("Profile-Gender", genderIndex,"simraPrefs", ProfileActivity.this);


            int regionIndex = 0;
            for (int i = 0; i < regions.length; i++) {
                if (region == (Integer.valueOf(regions[i]))) {
                    regionIndex = i;
                }
            }

            writeIntToSharePrefs("Profile-Region", regionIndex,"simraPrefs", ProfileActivity.this);


            int berDistrictIndex = 0;
            for (int i = 0; i < berDistricts.length; i++) {
                if (berDistrict == (Integer.valueOf(berDistricts[i]))) {
                    berDistrictIndex = i;
                }
            }

            writeIntToSharePrefs("Profile-berDistrict", berDistrictIndex,"simraPrefs", ProfileActivity.this);

            int lonDistrictIndex = 0;
            for (int i = 0; i < lonDistricts.length; i++) {
                if (lonDistrict == (Integer.valueOf(lonDistricts[i]))) {
                    lonDistrictIndex = i;
                }
            }

            writeIntToSharePrefs("Profile-lonDistrict", lonDistrictIndex,"simraPrefs", ProfileActivity.this);


            int bikeIndex = 0;
            for (int i = 0; i < bikeTypes.length; i++) {
                if (bike == (Integer.valueOf(bikeTypes[i]))) {
                    bikeIndex = i;
                }
            }

            writeIntToSharePrefs("Profile-bikeType", bikeIndex,"simraPrefs", ProfileActivity.this);


            int locationIndex = 0;
            for (int i = 0; i < phoneLocations.length; i++) {
                if (locationType == (Integer.valueOf(phoneLocations[i]))) {
                    locationIndex = i;
                }
            }

            writeIntToSharePrefs("Profile-phoneLocation", locationIndex,"simraPrefs", ProfileActivity.this);
            */
            profileSaved = true;
            finish();
        });

        // Return without saving the profile
        abortButton.setOnClickListener((View v) -> {
            profileSaved = false;
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
        if(profileSaved){
            Toast.makeText(this, getString(R.string.editingProfileCompleted), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.editingProfileAborted), Toast.LENGTH_SHORT).show();
        }
    }

    private int[] loadPreviousProfile() {
        // {ageGroup, gender, region, berDistrict, lonDistrict, bikeType, phoneLocation}
        int[] result = new int[7];

        // String defaultValue = getResources().getString(R.string.pleaseChoose);

        result[0] = lookUpIntSharedPrefs("Profile-Age",0,"simraPrefs", this);
        result[1] = lookUpIntSharedPrefs("Profile-Gender",0,"simraPrefs", this);
        result[2] = lookUpIntSharedPrefs("Profile-Region",0,"simraPrefs", this);
        result[3] = lookUpIntSharedPrefs("Profile-berDistrict",0,"simraPrefs", this);
        result[4] = lookUpIntSharedPrefs("Profile-lonDistrict",0,"simraPrefs", this);
        result[5] = lookUpIntSharedPrefs("Profile-bikeType",0,"simraPrefs", this);
        result[6] = lookUpIntSharedPrefs("Profile-phoneLocation",0,"simraPrefs", this);
        return result;
    }

}
