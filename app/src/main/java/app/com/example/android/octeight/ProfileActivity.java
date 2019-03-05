package app.com.example.android.octeight;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import static app.com.example.android.octeight.Utils.lookUpIntSharedPrefs;
import static app.com.example.android.octeight.Utils.writeIntToSharePrefs;

public class ProfileActivity extends AppCompatActivity {
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Log tag
    private static final String TAG = "ProfileActivity_LOG";
    // SharedPreferences
    /*public static final String PREFERENCES = "simraPrefs" ;
    public static final String agePrefs = "Profile-Age";
    public static final String genderPrefs = "Profile-Gender";
    public static final String regionPrefs = "Profile-Region";
    public static final String berDistPrefs = "Profile-berDistrict";
    public static final String lonDistPrefs = "Profile-lonDistrict";
    public static final String bikePrefs = "Profile-bikeType";
    public static final String phoneLocPref = "Profile-phoneLocation";*/


    // Save and abort button at the bottom of the screen
    LinearLayout saveButton;
    LinearLayout abortButton;

    // Boolean to display a toast to inform the user whether changes were saved or not
    boolean profileSaved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Building the view
        Spinner ageGroupSpinner = findViewById(R.id.ageGroupSpinner);
        Spinner genderSpinner = findViewById(R.id.genderSpinner);
        Spinner regionSpinner = findViewById(R.id.regionSpinner);
        Spinner experienceSpinner = findViewById(R.id.experienceSpinner);

        // Get the previous saved settings
        int[] previousProfile = loadPreviousProfile();
        // working with android template
        /*SharedPreferences sharedpreferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        ageGroupSpinner.setSelection(sharedpreferences.getInt(agePrefs, 0));
        genderSpinner.setSelection(sharedpreferences.getInt(genderPrefs, 0));
        regionSpinner.setSelection(sharedpreferences.getInt(regionPrefs,0));
        berDistrictSpinner.setSelection(sharedpreferences.getInt(berDistPrefs, 0));
        lonDistrictSpinner.setSelection(sharedpreferences.getInt(lonDistPrefs, 0));
        bikeSpinner.setSelection(sharedpreferences.getInt(bikePrefs,0));
        locationTypeSpinner.setSelection(sharedpreferences.getInt(phoneLocPref,0));*/
        ageGroupSpinner.setSelection(previousProfile[0]);
        genderSpinner.setSelection(previousProfile[1]);
        regionSpinner.setSelection(previousProfile[2]);
        experienceSpinner.setSelection(previousProfile[3]);


        saveButton = findViewById(R.id.done_button);
        abortButton = findViewById(R.id.abort_button);

        // When the save button is clicked, the selected items are saved to simraPrefs,
        // this ProfileActivity gets finished and a toast is shown to the user.
        saveButton.setOnClickListener((View v) -> {

            int ageGroup = ageGroupSpinner.getSelectedItemPosition();
            int gender = genderSpinner.getSelectedItemPosition();
            int region = regionSpinner.getSelectedItemPosition();
            int experience = experienceSpinner.getSelectedItemPosition();

            /*SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putInt(agePrefs, ageGroup);
            editor.putInt(genderPrefs, gender);
            editor.putInt(regionPrefs, region);
            editor.putInt(berDistPrefs, berDistrict);
            editor.putInt(lonDistPrefs, lonDistrict);
            editor.putInt(bikePrefs, bike);
            editor.putInt(phoneLocPref, locationType);
            editor.commit();*/
            writeIntToSharePrefs("Profile-Age", ageGroup, "simraPrefs", ProfileActivity.this);
            writeIntToSharePrefs("Profile-Gender", gender, "simraPrefs", ProfileActivity.this);
            writeIntToSharePrefs("Profile-Region", region, "simraPrefs", ProfileActivity.this);
            writeIntToSharePrefs("Profile-Experience", experience, "simraPrefs", ProfileActivity.this);

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
        if (profileSaved) {
            Toast.makeText(this, getString(R.string.editingProfileCompleted), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.editingProfileAborted), Toast.LENGTH_SHORT).show();
        }
    }

    private int[] loadPreviousProfile() {
        // {ageGroup, gender, region, experience}
        int[] result = new int[5];
        result[0] = lookUpIntSharedPrefs("Profile-Age", 0, "simraPrefs", this);
        result[1] = lookUpIntSharedPrefs("Profile-Gender", 0, "simraPrefs", this);
        result[2] = lookUpIntSharedPrefs("Profile-Region", 0, "simraPrefs", this);
        result[3] = lookUpIntSharedPrefs("Profile-Experience", 0, "simraPrefs", this);
        return result;
    }

}
