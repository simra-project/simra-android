package de.tuberlin.mcc.simra.app.activities;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.databinding.ActivitySettingsBinding;
import de.tuberlin.mcc.simra.app.services.OBSService;
import de.tuberlin.mcc.simra.app.services.UploadService;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import de.tuberlin.mcc.simra.app.util.SharedPref;
import de.tuberlin.mcc.simra.app.util.UnitHelper;
import pl.droidsonroids.gif.GifImageView;

import static de.tuberlin.mcc.simra.app.util.SharedPref.writeBooleanToSharedPrefs;

public class SettingsActivity extends BaseActivity {

    private static final String TAG = "SettingsActivity_LOG";
    private final static int REQUEST_ENABLE_BT = 1;
    private final static int BLUETOOTH_SUCCESS = -1;

    ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySettingsBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        // Setup
        setSupportActionBar(binding.toolbar.toolbar);
        try {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        } catch (NullPointerException ignored){
            Log.d(TAG, "NullPointerException");
        }
        binding.toolbar.toolbar.setTitle("");
        binding.toolbar.toolbar.setSubtitle("");
        binding.toolbar.toolbarTitle.setText(R.string.title_activity_settings);
        binding.toolbar.backButton.setOnClickListener(v -> finish());

        // Slider: Duration
        binding.privacyDurationSlider.setValueFrom(SharedPref.Settings.Ride.PrivacyDuration.getMinDuration());
        binding.privacyDurationSlider.setValueTo(SharedPref.Settings.Ride.PrivacyDuration.getMaxDuration());
        binding.privacyDurationSlider.setValue(SharedPref.Settings.Ride.PrivacyDuration.getDuration(this));
        binding.privacyDurationTextLeft.setText(SharedPref.Settings.Ride.PrivacyDuration.getMinDuration() + getString(R.string.seconds_short));
        binding.privacyDurationTextRight.setText(SharedPref.Settings.Ride.PrivacyDuration.getMaxDuration() + getString(R.string.seconds_short));
        binding.privacyDurationSlider.addOnChangeListener((slider, changeListener, touchChangeListener) -> {
            SharedPref.Settings.Ride.PrivacyDuration.setDuration(Math.round(slider.getValue()), this);
        });

        // Slider: Distance
        updatePrivacyDistanceSlider(SharedPref.Settings.DisplayUnit.getDisplayUnit(this));
        binding.privacyDistanceSlider.addOnChangeListener((slider, changeListener, touchChangeListener) -> {
            SharedPref.Settings.Ride.PrivacyDistance.setDistance(Math.round(slider.getValue()), SharedPref.Settings.DisplayUnit.getDisplayUnit(this), this);
        });

        // Select Menu: Bike Type
        Spinner bikeTypeSpinner = findViewById(R.id.bikeTypeSpinner);
        bikeTypeSpinner.setSelection(SharedPref.Settings.Ride.BikeType.getBikeType(this));
        bikeTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPref.Settings.Ride.BikeType.setBikeType(position, SettingsActivity.this);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Select Menu: Phone Location
        Spinner phoneLocationSpinner = findViewById(R.id.locationTypeSpinner);
        phoneLocationSpinner.setSelection(SharedPref.Settings.Ride.PhoneLocation.getPhoneLocation(this));
        phoneLocationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPref.Settings.Ride.PhoneLocation.setPhoneLocation(position, SettingsActivity.this);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // CheckBox: Child on Bicycle
        CheckBox childOnBikeCheckBox = findViewById(R.id.childCheckBox);
        childOnBikeCheckBox.setChecked(SharedPref.Settings.Ride.ChildOnBoard.isChildOnBoard(this));
        childOnBikeCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPref.Settings.Ride.ChildOnBoard.setChildOnBoard(isChecked, this);
        });

        // CheckBox: Bicycle with Trailer
        CheckBox trailerCheckBox = findViewById(R.id.trailerCheckBox);
        trailerCheckBox.setChecked(SharedPref.Settings.Ride.BikeWithTrailer.hasTrailer(this));
        trailerCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPref.Settings.Ride.BikeWithTrailer.setTrailer(isChecked, this);
        });

        // Switch: Unit Select
        Switch unitSwitch = findViewById(R.id.unitSwitch);
        if (SharedPref.Settings.DisplayUnit.isImperial(this)) {
            unitSwitch.setChecked(true);
        }
        unitSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            UnitHelper.DISTANCE displayUnit = isChecked ? UnitHelper.DISTANCE.IMPERIAL : UnitHelper.DISTANCE.METRIC;
            SharedPref.Settings.DisplayUnit.setDisplayUnit(displayUnit, this);
            updatePrivacyDistanceSlider(displayUnit);
        });

        // Switch: Buttons for adding incidents during ride
        if (SharedPref.Settings.IncidentsButtonsDuringRide.getIncidentButtonsEnabled(this)) {
            binding.switchButtons.setChecked(true);
        }
        binding.switchButtons.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    SharedPref.Settings.IncidentsButtonsDuringRide.setIncidentButtonsEnabled(isChecked,this);
                    if (isChecked) {
                        fireIncidentButtonsEnablePrompt();
                    }
                });
        /*
        // Switch: AI Select
        if (SharedPref.Settings.IncidentGenerationAIActive.getAIEnabled(this)) {
            binding.switchAI.setChecked(true);
        }
        binding.switchAI.setOnCheckedChangeListener((buttonView, isChecked) -> SharedPref.Settings.IncidentGenerationAIActive.setAIEnabled(isChecked,this));
        */

        // Switch: OpenBikeSensor device enabled
        boolean obsActivated = SharedPref.Settings.OpenBikeSensor.isEnabled(this);
        binding.obsButton.setVisibility(obsActivated ? View.VISIBLE : View.GONE);
        binding.obsButton.setOnClickListener(view -> startActivity(new Intent(this, OpenBikeSensorActivity.class)));

        if (BluetoothAdapter.getDefaultAdapter() == null) {
            // Device does not support Bluetooth
            binding.obsSwitch.setEnabled(false);
        }
        binding.obsSwitch.setChecked(obsActivated);
        binding.obsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPref.Settings.OpenBikeSensor.setEnabled(isChecked, SettingsActivity.this);
            binding.obsButton.setVisibility(isChecked ? View.VISIBLE : View.GONE);

            if (isChecked) {
                if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                    enableBluetooth();
                } else {
                    startOBSService();
                }
            } else {
                OBSService.terminateService(this);
            }
        });


        // Version Text
        TextView appVersionTextView = findViewById(R.id.appVersionTextView);
        appVersionTextView.setText("Version: " + BuildConfig.VERSION_CODE + "(" + BuildConfig.VERSION_NAME + ")");

    }

    private void updatePrivacyDistanceSlider(UnitHelper.DISTANCE unit) {
        binding.privacyDistanceSlider.setValueFrom(SharedPref.Settings.Ride.PrivacyDistance.getMinDistance(unit));
        binding.privacyDistanceSlider.setValueTo(SharedPref.Settings.Ride.PrivacyDistance.getMaxDistance(unit));
        binding.privacyDistanceSlider.setValue(SharedPref.Settings.Ride.PrivacyDistance.getDistance(unit, this));
        binding.privacyDistanceTextLeft.setText(SharedPref.Settings.Ride.PrivacyDistance.getMinDistance(unit) + UnitHelper.getShortTranslationForUnit(unit, this));
        binding.privacyDistanceTextRight.setText(SharedPref.Settings.Ride.PrivacyDistance.getMaxDistance(unit) + UnitHelper.getShortTranslationForUnit(unit, this));
    }

    private void showTutorialDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.obsConnecting));
        alert.setMessage(getString(R.string.obsTutorial));

        LinearLayout gifLayout = new LinearLayout(this);
        LinearLayout.LayoutParams gifMargins = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        GifImageView gif = new GifImageView(this);
        gif.setImageResource(R.drawable.tutorial);
        gif.setVisibility(View.VISIBLE);
        gifMargins.setMargins(50, 0, 50, 0);
        gif.setLayoutParams(gifMargins);
        gifLayout.addView(gif);
        alert.setView(gifLayout);
        alert.setPositiveButton("Ok", (dialog, whichButton) -> {
        });
        alert.show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == BLUETOOTH_SUCCESS && requestCode == REQUEST_ENABLE_BT) {
            startOBSService();
            showTutorialDialog();
        } else {
            SharedPref.Settings.OpenBikeSensor.setEnabled(false, this);
            binding.obsSwitch.setChecked(false);
            OBSService.terminateService(this);
            binding.obsButton.setVisibility(View.GONE);
        }
    }

    private void startOBSService() {
        Intent intent = new Intent(this, OBSService.class);
        startService(intent);
    }

    private void enableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    public void fireIncidentButtonsEnablePrompt() {
        androidx.appcompat.app.AlertDialog.Builder alert = new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this);
        alert.setTitle(getString(R.string.warning));
        alert.setMessage(getString(R.string.incident_buttons_during_ride_warning));
        alert.setNeutralButton("Ok", (dialog, id) -> { });
        alert.show();
    }
}
