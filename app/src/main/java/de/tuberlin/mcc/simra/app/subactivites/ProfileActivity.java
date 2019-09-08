package de.tuberlin.mcc.simra.app.subactivites;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import de.tuberlin.mcc.simra.app.R;

import static de.tuberlin.mcc.simra.app.util.Utils.getProfileDemographics;
import static de.tuberlin.mcc.simra.app.util.Utils.updateProfile;

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
    SeekBar behaviourSeekBar;

    ToggleButton activateBehaviourToggleButton;

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
                                           finish();
                                       }
                                   }
        );
        // Building the view
        ageGroupSpinner = findViewById(R.id.ageGroupSpinner);
        genderSpinner = findViewById(R.id.genderSpinner);
        regionSpinner = findViewById(R.id.regionSpinner);
        experienceSpinner = findViewById(R.id.experienceSpinner);
        behaviourSeekBar = (SeekBar) findViewById(R.id.behaviourSeekBar);
        activateBehaviourToggleButton = findViewById(R.id.activateBehaviourSeekBar);

        // Get the previous saved settings
        int[] previousProfile = getProfileDemographics(this);

        ageGroupSpinner.setSelection(previousProfile[0]);
        genderSpinner.setSelection(previousProfile[1]);
        regionSpinner.setSelection(previousProfile[2]);
        switch (previousProfile[2]) {
            // 1 = Berlin = setSelection(1)
            case 1:
                regionSpinner.setSelection(1);
                break;
            // 2 = London = setSelection(3)
            case 2:
                regionSpinner.setSelection(3);
                break;
            // 3 = Other = setSelection(5)
            case 3:
                regionSpinner.setSelection(5);
                break;
            // 4 = Bern = setSelection(2)
            case 4:
                regionSpinner.setSelection(2);
                break;
            // 5 = Pforzheim/Enzkreis = setSelection(4)
            case 5:
                regionSpinner.setSelection(4);
                break;
            // 0 = UNKNOWN = setSelection(0)
            default:
                regionSpinner.setSelection(0);
        }
        experienceSpinner.setSelection(previousProfile[3]);
        Log.d(TAG, "previousProfile[4]: " + previousProfile[4]);
        if (previousProfile[4] == -1) {
            behaviourSeekBar.setEnabled(false);
            activateBehaviourToggleButton.setChecked(false);
        } else {
            behaviourSeekBar.setEnabled(true);
            behaviourSeekBar.setProgress(previousProfile[4]);
            activateBehaviourToggleButton.setChecked(true);
        }

        activateBehaviourToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                behaviourSeekBar.setEnabled(isChecked);
            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() called");
        int ageGroup = ageGroupSpinner.getSelectedItemPosition();
        int gender = genderSpinner.getSelectedItemPosition();
        int region = -1;
        Log.d(TAG, "regionSpinner.getSelectedItem().toString(): " + regionSpinner.getSelectedItem().toString());
        switch (regionSpinner.getSelectedItem().toString()) {
            case "Berlin/Potsdam":
                region = 1;
                break;
            case "London":
                region = 2;
                break;
            case "Sonstiges":
            case "Other":
                region = 3;
                break;
            case "Bern":
                region = 4;
                break;
            case "Pforzheim/Enzkreis":
                region = 5;
                break;
            default:
                region = 0;
        }
        Log.d(TAG, "region " + region);
        int experience = experienceSpinner.getSelectedItemPosition();
        int behaviour = behaviourSeekBar.getProgress();
        // Log.d(TAG, "behaviour: " + behaviourSeekBar.getProgress() + " isEnabled: " + behaviourSeekBar.isEnabled());
        updateProfile(this,ageGroup,gender,region,experience,behaviour);
        if (!behaviourSeekBar.isEnabled()) {
            SharedPreferences mySPrefs = getApplicationContext()
                    .getSharedPreferences("Profile", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = mySPrefs.edit();
            editor.remove("Behaviour");
            editor.apply();
        }
    }

    /*
    private int[] loadPreviousProfile() {
        // {ageGroup, gender, region, experience, driving, behaviour}
        int[] result = new int[5];
        result[0] = lookUpIntSharedPrefs("Age", 0, "Profile", this);
        result[1] = lookUpIntSharedPrefs("Gender", 0, "Profile", this);
        result[2] = lookUpIntSharedPrefs("Region", 0, "Profile", this);
        result[3] = lookUpIntSharedPrefs("Experience", 0, "Profile", this);
        result[4] = lookUpIntSharedPrefs("Behaviour", -1, "Profile", this);
        return result;
    }
    */
    // OnSeekBarChangeListener to update the corresponding value (privacy duration or distance)
    private SeekBar.OnSeekBarChangeListener createOnSeekBarChangeListener() {
        SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };

        return onSeekBarChangeListener;
    }


}
