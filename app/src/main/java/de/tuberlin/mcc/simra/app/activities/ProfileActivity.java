package de.tuberlin.mcc.simra.app.activities;


import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.databinding.ActivityProfileBinding;
import de.tuberlin.mcc.simra.app.entities.Profile;

import static de.tuberlin.mcc.simra.app.util.Utils.getRegions;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity_LOG";

    ActivityProfileBinding binding;

    String[] simRa_regions_config;

    Profile profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityProfileBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        binding.toolbar.toolbar.setTitle("");
        binding.toolbar.toolbar.setSubtitle("");
        binding.toolbar.toolbarTitle.setText(R.string.title_activity_profile);
        binding.toolbar.backButton.setOnClickListener(v -> finish());

        profile = Profile.loadProfile(null, this);
        simRa_regions_config = getRegions(this);

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
        binding.profileContent.regionSpinner.setAdapter(adapter);

        // Get the previous saved settings
        binding.profileContent.ageGroupSpinner.setSelection(profile.ageGroup);
        binding.profileContent.genderSpinner.setSelection(profile.gender);
        String region = simRa_regions_config[profile.region];
        if (!region.startsWith("!")) {
            if (languageIsEnglish) {
                binding.profileContent.regionSpinner.setSelection(regionContentArray.indexOf(region.split("=")[0]));
            } else {
                binding.profileContent.regionSpinner.setSelection(regionContentArray.indexOf(region.split("=")[1]));
            }
        } else {
            binding.profileContent.regionSpinner.setSelection(0);
        }
        binding.profileContent.experienceSpinner.setSelection(profile.experience);
        if (profile.behaviour == -1) {
            binding.profileContent.behaviourSeekBar.setEnabled(false);
            binding.profileContent.activateBehaviourSeekBarButton.setChecked(false);
        } else {
            binding.profileContent.behaviourSeekBar.setEnabled(true);
            binding.profileContent.behaviourSeekBar.setProgress(profile.behaviour);
            binding.profileContent.activateBehaviourSeekBarButton.setChecked(true);
        }

        binding.profileContent.activateBehaviourSeekBarButton.setOnCheckedChangeListener((buttonView, isChecked) -> binding.profileContent.behaviourSeekBar.setEnabled(isChecked));
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() called");


        profile.ageGroup = binding.profileContent.ageGroupSpinner.getSelectedItemPosition();
        profile.gender = binding.profileContent.genderSpinner.getSelectedItemPosition();
        int region = -1;
        String selectedRegion = binding.profileContent.regionSpinner.getSelectedItem().toString();
        for (int i = 0; i < simRa_regions_config.length; i++) {
            if (selectedRegion.equals(simRa_regions_config[i].split("=")[0]) || selectedRegion.equals(simRa_regions_config[i].split("=")[1])) {
                region = i;
                break;
            }
        }
        profile.region = region;
        profile.experience = binding.profileContent.experienceSpinner.getSelectedItemPosition();

        if (!binding.profileContent.behaviourSeekBar.isEnabled()) {
            profile.behaviour = -1;
        } else {
            profile.behaviour = binding.profileContent.behaviourSeekBar.getProgress();
        }
        Profile.saveProfile(profile, null, this);
    }

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
