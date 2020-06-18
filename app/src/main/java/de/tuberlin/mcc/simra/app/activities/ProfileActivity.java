package de.tuberlin.mcc.simra.app.activities;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import de.tuberlin.mcc.simra.app.R;

import static de.tuberlin.mcc.simra.app.util.Utils.getProfileDemographics;
import static de.tuberlin.mcc.simra.app.util.Utils.getRegions;
import static de.tuberlin.mcc.simra.app.util.Utils.updateProfile;

public class ProfileActivity extends AppCompatActivity {
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Log tag
    private static final String TAG = "ProfileActivity_LOG";

    String[] simRa_regions_config;

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
        backBtn.setOnClickListener(v -> finish());

        simRa_regions_config = getRegions(this);

        // Building the view
        ageGroupSpinner = findViewById(R.id.ageGroupSpinner);
        genderSpinner = findViewById(R.id.genderSpinner);
        regionSpinner = findViewById(R.id.regionSpinner);
        experienceSpinner = findViewById(R.id.experienceSpinner);
        behaviourSeekBar = findViewById(R.id.behaviourSeekBar);
        activateBehaviourToggleButton = findViewById(R.id.activateBehaviourSeekBar);

        String locale = Resources.getSystem().getConfiguration().locale.getLanguage();
        List<String> regionContentArray = new ArrayList<>();
        boolean languageIsEnglish = locale.equals(new Locale("en").getLanguage());
        for (String s : simRa_regions_config) {
            if (!(s.startsWith("!") || s.startsWith("Please Choose"))) {
                if (languageIsEnglish) {
                    regionContentArray.add(s.split("=")[0]);
                } else {
                    regionContentArray.add(s.split("=")[1]);
                }
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, regionContentArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Collections.sort(regionContentArray);
        regionContentArray.add(0, getText(R.string.pleaseChoose).toString());
        regionSpinner.setAdapter(adapter);

        // Get the previous saved settings
        int[] previousProfile = getProfileDemographics(this);
        ageGroupSpinner.setSelection(previousProfile[0]);
        genderSpinner.setSelection(previousProfile[1]);
        String region = simRa_regions_config[previousProfile[2]];
        if (!region.startsWith("!")) {
            if (languageIsEnglish) {
                regionSpinner.setSelection(regionContentArray.indexOf(region.split("=")[0]));
            } else {
                regionSpinner.setSelection(regionContentArray.indexOf(region.split("=")[1]));
            }
        } else {
            regionSpinner.setSelection(0);
        }
        experienceSpinner.setSelection(previousProfile[3]);
        if (previousProfile[4] == -1) {
            behaviourSeekBar.setEnabled(false);
            activateBehaviourToggleButton.setChecked(false);
        } else {
            behaviourSeekBar.setEnabled(true);
            behaviourSeekBar.setProgress(previousProfile[4]);
            activateBehaviourToggleButton.setChecked(true);
        }

        activateBehaviourToggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> behaviourSeekBar.setEnabled(isChecked));
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() called");
        int ageGroup = ageGroupSpinner.getSelectedItemPosition();
        int gender = genderSpinner.getSelectedItemPosition();
        int region = -1;
        String selectedRegion = regionSpinner.getSelectedItem().toString();
        for (int i = 0; i < simRa_regions_config.length; i++) {
            if (selectedRegion.equals(simRa_regions_config[i].split("=")[0]) || selectedRegion.equals(simRa_regions_config[i].split("=")[1])) {
                region = i;
                break;
            }
        }
        Log.d(TAG, "regionSpinner.getSelectedItem().toString(): " + regionSpinner.getSelectedItem().toString());
        Log.d(TAG, "region index: " + region);

        /*
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
            case "Augsburg":
                region = 6;
                break;
            case "Ruhrgebiet":
                region = 7;
                break;
            case "Stuttgart":
                region = 8;
                break;
            default:
                region = 0;
        }*/
        int experience = experienceSpinner.getSelectedItemPosition();
        int behaviour = behaviourSeekBar.getProgress();
        // Log.d(TAG, "behaviour: " + behaviourSeekBar.getProgress() + " isEnabled: " + behaviourSeekBar.isEnabled());
        updateProfile(true, this, ageGroup, gender, region, experience, behaviour);
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

        return new SeekBar.OnSeekBarChangeListener() {

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
    }
}
