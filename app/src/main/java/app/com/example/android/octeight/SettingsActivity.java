package app.com.example.android.octeight;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import static app.com.example.android.octeight.Utils.*;

public class SettingsActivity extends BaseActivity {

    // Log tag
    private static final String TAG = "SettingsActivity_LOG";
    ImageButton backBtn;
    TextView toolbarTxt;

    long privacyDuration;
    int privacyDistance;
    int child;
    int trailer;
    boolean showRideSettings;

    // Bike Type and Phone Location Items
    Spinner bikeTypeSpinner;
    Spinner phoneLocationSpinner;

    // Child and Trailer
    CheckBox childCheckBox;
    CheckBox trailerCheckBox;

    // Ride settings are shown before annotating a ride if checked
    CheckBox showRideSettingsCheckBox;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        toolbarTxt = findViewById(R.id.toolbar_title);
        toolbarTxt.setText(R.string.title_activity_settings);

        backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           writeLongToSharedPrefs("Privacy-Duration", privacyDuration, "simraPrefs", SettingsActivity.this);
                                           writeIntToSharedPrefs("Privacy-Distance", privacyDistance, "simraPrefs", SettingsActivity.this);
                                           writeIntToSharedPrefs("Settings-BikeType", bikeTypeSpinner.getSelectedItemPosition(), "simraPrefs", SettingsActivity.this);
                                           writeIntToSharedPrefs("Settings-PhoneLocation", phoneLocationSpinner.getSelectedItemPosition(), "simraPrefs", SettingsActivity.this);
                                           writeIntToSharedPrefs("Settings-Child", child, "simraPrefs", SettingsActivity.this);
                                           writeIntToSharedPrefs("Settings-Trailer", trailer, "simraPrefs", SettingsActivity.this);
                                           writeBooleanToSharedPrefs("Settings-ShowRideSettingsDialog",showRideSettingsCheckBox.isChecked(),"simraPrefs",SettingsActivity.this);
                                           finish();
                                       }
                                   }
        );



        // Set the distance measure unit according to locale (foot when english, meter when german)
        String unit = "m";
        String locale = Resources.getSystem().getConfiguration().locale.getLanguage();
        if (locale.equals(new Locale("en").getLanguage())) {
            unit = "ft";
        }

        // Set seekBars
        SeekBar durationSeekBar = (SeekBar) findViewById(R.id.privacyDurationSeekBar);
        SeekBar distanceSeekBar = (SeekBar) findViewById(R.id.privacyDistanceSeekBar);

        // Set textViews
        TextView durationTextView = (TextView) findViewById(R.id.privacyDurationSeekBarProgress);
        TextView distanceTextView = (TextView) findViewById(R.id.privacyDistanceSeekBarProgress);

        // Load the privacy option values
        privacyDuration = lookUpLongSharedPrefs("Privacy-Duration", 30, "simraPrefs", this);

        privacyDistance = lookUpIntSharedPrefs("Privacy-Distance", 30, "simraPrefs", this);
        if (unit.equals("ft")) {
            privacyDistance = (int) Math.round((lookUpIntSharedPrefs("Privacy-Distance", 30, "simraPrefs", this) * 3.28));
        }

        // Set the seekBars according to the privacy option values
        durationSeekBar.setProgress((int) privacyDuration);
        distanceSeekBar.setProgress(privacyDistance);

        // Set the textViews according to the values of the corresponding seekBars
        durationTextView.setText(durationSeekBar.getProgress() + "s/" + (durationSeekBar.getMax()) + "s");
        if (unit.equals("ft")) {
            distanceTextView.setText(Math.round(distanceSeekBar.getProgress() * 3.28) + unit + "/" + Math.round(distanceSeekBar.getMax() * 3.28) + unit);
        } else {
            distanceTextView.setText(distanceSeekBar.getProgress() + unit + "/" + distanceSeekBar.getMax() + unit);

        }

        // Create onSeekBarChangeListeners to change the corresponding options
        durationSeekBar.setOnSeekBarChangeListener(createOnSeekBarChangeListener(durationTextView, "s", "Privacy-Duration"));
        distanceSeekBar.setOnSeekBarChangeListener(createOnSeekBarChangeListener(distanceTextView, unit, "Privacy-Distance"));

        // Bike Type and Phone Location Spinners
        bikeTypeSpinner = findViewById(R.id.bikeTypeSpinner);
        phoneLocationSpinner = findViewById(R.id.locationTypeSpinner);

        childCheckBox = findViewById(R.id.childCheckBox);
        trailerCheckBox = findViewById(R.id.trailerCheckBox);

        // CheckBox for showing ride settings before annotating a ride.
        showRideSettingsCheckBox = findViewById(R.id.showRideSettingsCheckBox);

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
        childCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    child = 1;
                } else {
                    child = 0;
                }
            }
        });

        if (trailer == 1){
            trailerCheckBox.setChecked(true);
        }
        trailerCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    trailer = 1;
                } else {
                    trailer = 0;
                }
            }
        });

        showRideSettings = lookUpBooleanSharedPrefs("Settings-ShowRideSettingsDialog",true,"simraPrefs", SettingsActivity.this);

        if (!showRideSettings) {
            showRideSettingsCheckBox.setChecked(false);
        }

    }

    // OnSeekBarChangeListener to update the corresponding value (privacy duration or distance)
    private SeekBar.OnSeekBarChangeListener createOnSeekBarChangeListener(TextView tV, String unit, String privacyOption) {
        SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (unit.equals("s")) {
                    if (progress < 3) {
                        seekBar.setProgress(3);
                    }
                }
                if (unit.equals("ft")) {
                    tV.setText(Math.round(seekBar.getProgress() * 3.28) + unit + "/" + Math.round(seekBar.getMax() * 3.28) + unit);

                } else {
                    tV.setText(seekBar.getProgress() + unit + "/" + seekBar.getMax() + unit);

                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                if (unit.equals("s")) {
                    if (seekBar.getProgress() < 3) {
                        seekBar.setProgress(3);
                    }
                }
                if (unit.equals("ft")) {
                    tV.setText(Math.round(seekBar.getProgress() * 3.28) + unit + "/" + Math.round(seekBar.getMax() * 3.28) + unit);

                } else {
                    tV.setText(seekBar.getProgress() + unit + "/" + seekBar.getMax() + unit);

                }
                if (privacyOption.equals("Privacy-Duration")) {
                    privacyDuration = seekBar.getProgress();
                    //writeLongToSharedPrefs(privacyOption, (long) seekBar.getProgress(), "simraPrefs", SettingsActivity.this);
                } else if (privacyOption.equals("Privacy-Distance")) {
                    privacyDistance = seekBar.getProgress();
                    // writeIntToSharedPrefs(privacyOption, seekBar.getProgress(), "simraPrefs", SettingsActivity.this);
                } else {
                    Log.e(TAG, "onStopTrackingTouch unknown privacyOption: " + privacyOption);
                }
            }
        };

        return onSeekBarChangeListener;
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
        Toast.makeText(this, getString(R.string.settingsSaved), Toast.LENGTH_SHORT).show();

    }
}
