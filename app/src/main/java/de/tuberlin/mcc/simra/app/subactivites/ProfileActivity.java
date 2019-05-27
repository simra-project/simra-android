package de.tuberlin.mcc.simra.app.subactivites;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import de.tuberlin.mcc.simra.app.R;

import static de.tuberlin.mcc.simra.app.util.Utils.lookUpIntSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.writeIntToSharedPrefs;

public class ProfileActivity extends AppCompatActivity {
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Log tag
    private static final String TAG = "ProfileActivity_LOG";



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
                                           writeIntToSharedPrefs("Profile-Age", ageGroup, "simraPrefs", ProfileActivity.this);
                                           writeIntToSharedPrefs("Profile-Gender", gender, "simraPrefs", ProfileActivity.this);
                                           writeIntToSharedPrefs("Profile-Region", region, "simraPrefs", ProfileActivity.this);
                                           writeIntToSharedPrefs("Profile-Experience", experience, "simraPrefs", ProfileActivity.this);
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
