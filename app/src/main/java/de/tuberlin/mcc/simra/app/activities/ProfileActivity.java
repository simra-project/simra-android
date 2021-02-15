package de.tuberlin.mcc.simra.app.activities;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;

import androidx.core.content.ContextCompat;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.databinding.ActivityProfileBinding;
import de.tuberlin.mcc.simra.app.entities.Profile;
import de.tuberlin.mcc.simra.app.util.PermissionHelper;
import de.tuberlin.mcc.simra.app.util.SharedPref;

import static de.tuberlin.mcc.simra.app.util.Utils.getCorrectRegionName;
import static de.tuberlin.mcc.simra.app.util.Utils.getCorrectRegionNames;
import static de.tuberlin.mcc.simra.app.util.Utils.getRegions;
import static de.tuberlin.mcc.simra.app.util.Utils.isLocationServiceOff;
import static de.tuberlin.mcc.simra.app.util.Utils.nearestRegionsToThisLocation;
import static de.tuberlin.mcc.simra.app.util.Utils.regionsDecoder;
import static de.tuberlin.mcc.simra.app.util.Utils.regionEncoder;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity_LOG";

    ActivityProfileBinding binding;

    String[] simRa_regions_config;

    Profile profile;

    private static final String EXTRA_PROFILE = "EXTRA_PROFILE";

    public static void startProfileActivityForChooseRegion(Context context) {
        Intent intent = new Intent(context, ProfileActivity.class);
        intent.putExtra(EXTRA_PROFILE, true);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityProfileBinding.inflate(LayoutInflater.from(this));

        setSupportActionBar(binding.toolbar.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        binding.toolbar.toolbar.setTitle("");
        binding.toolbar.toolbar.setSubtitle("");
        binding.toolbar.toolbarTitle.setText(R.string.title_activity_profile);
        binding.toolbar.backButton.setOnClickListener(v -> finish());

        profile = Profile.loadProfile(null, this);
        simRa_regions_config = getRegions(this);

        ArrayList<String> regionContentArray = new ArrayList<>();
        for (String s : simRa_regions_config) {
            if (!(s.startsWith("!") || s.startsWith("Please Choose"))) {
                regionContentArray.add(getCorrectRegionName(s));
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, regionContentArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Collections.sort(regionContentArray);
        regionContentArray.add(0, getText(R.string.pleaseChoose).toString());
        binding.profileContent.regionSpinner.setAdapter(adapter);

        // Get the previous saved settings
        binding.profileContent.ageGroupSpinner.setSelection(profile.ageGroup); //Age
        binding.profileContent.genderSpinner.setSelection(profile.gender); // Gender
        // Region:
        if(getIntent().hasExtra(EXTRA_PROFILE)) {
            binding.profileContent.profileRegionLinearLayout.setBackground(ContextCompat.getDrawable(ProfileActivity.this,R.drawable.profile_region_border));
        } else {
            binding.profileContent.profileRegionLinearLayout.setPadding(0,0,0,0);
        }
        toggleRegionSwitch(regionContentArray);
        binding.profileContent.switchRegion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPref.Settings.RegionSetting.setRegionDetectionViaGPSEnabled(binding.profileContent.switchRegion.isChecked(),ProfileActivity.this);
                toggleRegionSwitch(regionContentArray);
            }
        });

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

    private void toggleRegionSwitch(ArrayList<String> regionContentArray) {
        String region = getCorrectRegionName(simRa_regions_config[profile.region]);
        if (!region.startsWith("!")) {
            binding.profileContent.regionSpinner.setSelection(regionContentArray.indexOf(getCorrectRegionName(region)));
        } else {
            binding.profileContent.regionSpinner.setSelection(0);
        }
        if(SharedPref.Settings.RegionSetting.getRegionDetectionViaGPSEnabled(ProfileActivity.this)) {
            binding.profileContent.regionSpinner.setVisibility(View.GONE);
            binding.profileContent.switchRegion.setChecked(true);
            binding.profileContent.regionAutomaticRelativeLayout.setVisibility(View.VISIBLE);
            binding.profileContent.detectedRegionTextView.setText(getText(R.string.selectedRegion) + getCorrectRegionName(region));
            binding.profileContent.regionAutomaticButton.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("MissingPermission")
                @Override
                public void onClick(View v) {
                    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    if (!PermissionHelper.hasBasePermissions(ProfileActivity.this)) {
                        PermissionHelper.requestFirstBasePermissionsNotGranted(ProfileActivity.this);
                    } else {
                        // notify the user if the location service is disabled
                        if (isLocationServiceOff(locationManager)) {
                            new AlertDialog.Builder(ProfileActivity.this).setMessage((R.string.locationServiceisOff + " " + R.string.enableToDetectRegion))
                                    .setPositiveButton(android.R.string.ok,
                                            (paramDialogInterface, paramInt) -> ProfileActivity.this
                                                    .startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                                    .setNegativeButton(R.string.cancel, null).show();
                        // get the three nearest regions and let the user choose one of them or cancel in an AlertDialog
                        } else {
                            int[] nearestRegionCodes = nearestRegionsToThisLocation(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude(),
                                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude(),
                                    ProfileActivity.this);
                            String[] nearestRegionNames = getCorrectRegionNames(regionsDecoder(nearestRegionCodes, ProfileActivity.this));
                            AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);//.setMessage(R.string.selectRegion);
                            createButtons(builder, nearestRegionNames, regionContentArray, profile, ProfileActivity.this);
                            builder.setNegativeButton(R.string.cancel,null);
                            builder.create().show();
                        }
                    }
                }
            });
        } else {
            binding.profileContent.regionAutomaticRelativeLayout.setVisibility(View.GONE);
            binding.profileContent.regionSpinner.setVisibility(View.VISIBLE);
        }
        setContentView(binding.getRoot());
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() called");

        profile.ageGroup = binding.profileContent.ageGroupSpinner.getSelectedItemPosition();
        profile.gender = binding.profileContent.genderSpinner.getSelectedItemPosition();
        String selectedRegion = binding.profileContent.regionSpinner.getSelectedItem().toString();
        profile.region = regionEncoder(selectedRegion,ProfileActivity.this);
        profile.experience = binding.profileContent.experienceSpinner.getSelectedItemPosition();

        if (!binding.profileContent.behaviourSeekBar.isEnabled()) {
            profile.behaviour = -1;
        } else {
            profile.behaviour = binding.profileContent.behaviourSeekBar.getProgress();
        }
        Profile.saveProfile(profile, null, this);
    }

    // creates buttons for the automatic region detector
    private void createButtons(AlertDialog.Builder builder, String[] nearestRegionNames, ArrayList<String> regionContentArray, Profile profile, ProfileActivity context) {
        builder.setItems(nearestRegionNames,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        profile.region = regionEncoder(nearestRegionNames[which], context);
                        binding.profileContent.regionSpinner.setSelection(regionContentArray.indexOf(nearestRegionNames[which]));
                        toggleRegionSwitch(regionContentArray);
                    }
                });
    }
}
