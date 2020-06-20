package de.tuberlin.mcc.simra.app.activities;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
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

import static de.tuberlin.mcc.simra.app.util.SharedPref.lookUpIntSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.SharedPref.writeIntToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.getAppVersionNumber;

public class SettingsActivity extends BaseActivity {

    private static final String TAG = "SettingsActivity_LOG";
    private final static int REQUEST_ENABLE_BT = 1;
    private final static int BLUETOOTH_SUCCESS = -1;
    ImageButton backBtn;
    TextView toolbarTxt;
    int child;
    int trailer;
    // Bike Type and Phone Location Items
    Spinner bikeTypeSpinner;
    Spinner phoneLocationSpinner;
    Switch radmesserConnectionSwitch;
    Button radmesserButton;

    // Sliders
    Slider distanceSlider;
    TextView distanceSliderTextLeft;
    TextView distanceSliderTextRight;

    private ServiceConnection radmesserServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Connecting to service");
            RadmesserService.LocalBinder myBinder = (RadmesserService.LocalBinder) service;
            RadmesserService radmesserService = myBinder.getService();
        }
    };

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
        toolbarTxt = findViewById(R.id.toolbar_title);
        toolbarTxt.setText(R.string.title_activity_settings);

        backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(v -> finish());

        // Unit Select Switch
        Switch unitSwitch = findViewById(R.id.unitSwitch);
        if (SharedPref.Settings.DisplayUnit.isImperial(this)) {
            unitSwitch.setChecked(true);
        }
        unitSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            UnitHelper.DISTANCE displayUnit = isChecked ? UnitHelper.DISTANCE.IMPERIAL : UnitHelper.DISTANCE.METRIC;
            SharedPref.Settings.DisplayUnit.setDisplayUnit(displayUnit, this);
            updatePrivacyDistanceSlider(displayUnit);
        });

        // Duration Slider
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

        // Distance Slider
        distanceSlider = findViewById(R.id.privacyDistanceSlider);
        distanceSliderTextLeft = findViewById(R.id.privacyDistanceTextLeft);
        distanceSliderTextRight = findViewById(R.id.privacyDistanceTextRight);
        updatePrivacyDistanceSlider(SharedPref.Settings.DisplayUnit.getDisplayUnit(this));
        distanceSlider.addOnChangeListener((slider, changeListener, touchChangeListener) -> {
            SharedPref.Settings.Ride.PrivacyDistance.setDistance(Math.round(slider.getValue()), SharedPref.Settings.DisplayUnit.getDisplayUnit(this), this);
        });

        // Bike Type and Phone Location Spinners
        bikeTypeSpinner = findViewById(R.id.bikeTypeSpinner);
        phoneLocationSpinner = findViewById(R.id.locationTypeSpinner);

        CheckBox childCheckBox = findViewById(R.id.childCheckBox);
        CheckBox trailerCheckBox = findViewById(R.id.trailerCheckBox);

        // Load previous bikeType and phoneLocation settings
        int bikeType = lookUpIntSharedPrefs("Settings-BikeType", 0, "simraPrefs", this);
        int phoneLocation = lookUpIntSharedPrefs("Settings-PhoneLocation", 0, "simraPrefs", this);

        bikeTypeSpinner.setSelection(bikeType);
        phoneLocationSpinner.setSelection(phoneLocation);

        // Load previous child and trailer settings
        child = lookUpIntSharedPrefs("Settings-Child", 0, "simraPrefs", this);
        trailer = lookUpIntSharedPrefs("Settings-Trailer", 0, "simraPrefs", this);

        if (child == 1) {
            childCheckBox.setChecked(true);
        }
        childCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                child = 1;
            } else {
                child = 0;
            }
        });

        if (trailer == 1) {
            trailerCheckBox.setChecked(true);
        }
        trailerCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                trailer = 1;
            } else {
                trailer = 0;
            }
        });


        TextView appVersionTextView = findViewById(R.id.appVersionTextView);
        appVersionTextView.setText("Version: " + getAppVersionNumber(this));

        // set radmesser connection
        boolean radmesserActivated = SharedPref.Settings.Radmesser.isEnabled(this);

        radmesserConnectionSwitch = findViewById(R.id.radmesserSwitch);
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            // Device does not support Bluetooth
            radmesserConnectionSwitch.setEnabled(false);
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

        radmesserButton = findViewById(R.id.radmesserButton);
        radmesserButton.setVisibility(radmesserActivated ? View.VISIBLE : View.GONE);
        radmesserButton.setOnClickListener(view -> startActivity(new Intent(this, RadmesserActivity.class)));

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
            // Try to start again?
            enableBluetooth();
        }
    }

    private void startRadmesserService() {
        Intent intent = new Intent(this, RadmesserService.class);
        startService(intent);
        bindService(intent, radmesserServiceConnection, Context.BIND_IMPORTANT);
    }

    private void enableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
        try {
            unbindService(radmesserServiceConnection);
        } catch (Exception e) {
        }
        writeIntToSharedPrefs("Settings-BikeType", bikeTypeSpinner.getSelectedItemPosition(), "simraPrefs", this);
        writeIntToSharedPrefs("Settings-PhoneLocation", phoneLocationSpinner.getSelectedItemPosition(), "simraPrefs", this);
        writeIntToSharedPrefs("Settings-Child", child, "simraPrefs", this);
        writeIntToSharedPrefs("Settings-Trailer", trailer, "simraPrefs", this);
    }

}
