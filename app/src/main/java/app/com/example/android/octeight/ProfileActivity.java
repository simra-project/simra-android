package app.com.example.android.octeight;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
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


    ImageButton backBtn;
    TextView toolbarTxt;

    Spinner ageGroupSpinner;
    Spinner genderSpinner;
    Spinner regionSpinner;
    Spinner experienceSpinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        toolbarTxt = findViewById(R.id.toolbar_title);
        toolbarTxt.setText(R.string.title_activity_profile);

        backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           int ageGroup = ageGroupSpinner.getSelectedItemPosition();
                                           int gender = genderSpinner.getSelectedItemPosition();
                                           int region = regionSpinner.getSelectedItemPosition();
                                           int experience = experienceSpinner.getSelectedItemPosition();
                                           writeIntToSharePrefs("Profile-Age", ageGroup, "simraPrefs", ProfileActivity.this);
                                           writeIntToSharePrefs("Profile-Gender", gender, "simraPrefs", ProfileActivity.this);
                                           writeIntToSharePrefs("Profile-Region", region, "simraPrefs", ProfileActivity.this);
                                           writeIntToSharePrefs("Profile-Experience", experience, "simraPrefs", ProfileActivity.this);
                                           finish();
                                       }
                                   }
        );
        // Building the view
        ageGroupSpinner = findViewById(R.id.ageGroupSpinner);
        genderSpinner = findViewById(R.id.genderSpinner);
        regionSpinner = findViewById(R.id.regionSpinner);
        experienceSpinner = findViewById(R.id.experienceSpinner);

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
        Toast.makeText(this, getString(R.string.editingProfileCompleted), Toast.LENGTH_SHORT).show();

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
