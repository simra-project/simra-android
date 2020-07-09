package de.tuberlin.mcc.simra.app.activities;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.google.android.material.slider.Slider;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.services.RadmesserService;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import de.tuberlin.mcc.simra.app.util.SharedPref;
import de.tuberlin.mcc.simra.app.util.UnitHelper;
import pl.droidsonroids.gif.GifImageView;

import static de.tuberlin.mcc.simra.app.util.Utils.getAppVersionNumber;

public class SettingsActivity extends BaseActivity {

    private static final String TAG = "SettingsActivity_LOG";
    private final static int REQUEST_ENABLE_BT = 1;
    private final static int BLUETOOTH_SUCCESS = -1;

    // Sliders
    Slider distanceSlider;
    TextView distanceSliderTextLeft;
    TextView distanceSliderTextRight;
    Button radmesserButton;
    Switch radmesserConnectionSwitch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Setup
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        TextView toolbarTxt = findViewById(R.id.toolbar_title);
        toolbarTxt.setText(R.string.title_activity_settings);

        ImageButton backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(v -> finish());

        // Slider: Duration
        Slider durationSlider = findViewById(R.id.privacyDurationSlider);
        TextView durationSliderTextLeft = findViewById(R.id.privacyDurationTextLeft);
        TextView durationSliderTextRight = findViewById(R.id.privacyDurationTextRight);
        durationSlider.setValueFrom(SharedPref.Settings.Ride.PrivacyDuration.getMinDuration());
        durationSlider.setValueTo(SharedPref.Settings.Ride.PrivacyDuration.getMaxDuration());
        durationSlider.setValue(SharedPref.Settings.Ride.PrivacyDuration.getDuration(this));
        durationSliderTextLeft.setText(SharedPref.Settings.Ride.PrivacyDuration.getMinDuration() + getString(R.string.seconds_short));
        durationSliderTextRight.setText(SharedPref.Settings.Ride.PrivacyDuration.getMaxDuration() + getString(R.string.seconds_short));
        durationSlider.addOnChangeListener((slider, changeListener, touchChangeListener) -> {
            SharedPref.Settings.Ride.PrivacyDuration.setDuration(Math.round(slider.getValue()), this);
        });

        // Slider: Distance
        distanceSlider = findViewById(R.id.privacyDistanceSlider);
        distanceSliderTextLeft = findViewById(R.id.privacyDistanceTextLeft);
        distanceSliderTextRight = findViewById(R.id.privacyDistanceTextRight);
        updatePrivacyDistanceSlider(SharedPref.Settings.DisplayUnit.getDisplayUnit(this));
        distanceSlider.addOnChangeListener((slider, changeListener, touchChangeListener) -> {
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


        // Switch: Radmesser device enabled
        boolean radmesserActivated = SharedPref.Settings.Radmesser.isEnabled(this);
        radmesserConnectionSwitch = findViewById(R.id.radmesserSwitch);
        radmesserButton = findViewById(R.id.radmesserButton);
        radmesserButton.setVisibility(radmesserActivated ? View.VISIBLE : View.GONE);
        radmesserButton.setOnClickListener(view -> startActivity(new Intent(this, RadmesserActivity.class)));

        if (BluetoothAdapter.getDefaultAdapter() == null) {
            // Device does not support Bluetooth
            radmesserConnectionSwitch.setEnabled(false);
            RadmesserService.terminateService(this);
        }
        radmesserConnectionSwitch.setChecked(radmesserActivated);
        radmesserConnectionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPref.Settings.Radmesser.setEnabled(isChecked, SettingsActivity.this);
            radmesserButton.setVisibility(isChecked ? View.VISIBLE : View.GONE);

            if (isChecked) {
                if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                    enableBluetooth();
                } else {
                    startRadmesserService();
                }
            } else {
                stopService(new Intent(this, RadmesserService.class));
            }
        });


        // Version Text
        TextView appVersionTextView = findViewById(R.id.appVersionTextView);
        appVersionTextView.setText("Version: " + getAppVersionNumber(this));

    }

    private void updatePrivacyDistanceSlider(UnitHelper.DISTANCE unit) {
        distanceSlider.setValueFrom(SharedPref.Settings.Ride.PrivacyDistance.getMinDistance(unit));
        distanceSlider.setValueTo(SharedPref.Settings.Ride.PrivacyDistance.getMaxDistance(unit));
        distanceSlider.setValue(SharedPref.Settings.Ride.PrivacyDistance.getDistance(unit, this));
        distanceSliderTextLeft.setText(SharedPref.Settings.Ride.PrivacyDistance.getMinDistance(unit) + UnitHelper.getShortTranslationForUnit(unit, this));
        distanceSliderTextRight.setText(SharedPref.Settings.Ride.PrivacyDistance.getMaxDistance(unit) + UnitHelper.getShortTranslationForUnit(unit, this));
    }

    private void showTutorialDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Verbindung mit Radmesser");
        alert.setMessage("\nBitte halten Sie Ihr Hand nah an den Abstandsensor für 3 Sekunden");

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
            startRadmesserService();
            showTutorialDialog();
        } else {
            SharedPref.Settings.Radmesser.setEnabled(false, this);
            radmesserConnectionSwitch.setChecked(false);
            RadmesserService.terminateService(this);
            radmesserButton.setVisibility(View.GONE);
        }
    }

    private void startRadmesserService() {
        Intent intent = new Intent(this, RadmesserService.class);
        startService(intent);
    }

    private void enableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
    }



}
