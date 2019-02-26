package app.com.example.android.octeight;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

import static app.com.example.android.octeight.Utils.getUniqueUserID;
import static app.com.example.android.octeight.Utils.lookUpIntSharedPrefs;
import static app.com.example.android.octeight.Utils.lookUpLongSharedPrefs;
import static app.com.example.android.octeight.Utils.writeIntToSharePrefs;
import static app.com.example.android.octeight.Utils.writeLongToSharePrefs;


public class SettingsActivity extends BaseActivity {

    // Log tag
    private static final String TAG = "SettingsActivity_LOG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Unique user id (bottom right of the screen)
        TextView tv1 = findViewById(R.id.textViewId);
        tv1.setText("id: " + getUniqueUserID(this));

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
        long privacyDuration = lookUpLongSharedPrefs("Privacy-Duration", 30, "simraPrefs", this);

        int privacyDistance = lookUpIntSharedPrefs("Privacy-Distance", 30, "simraPrefs", this);
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
                    writeLongToSharePrefs(privacyOption, (long) seekBar.getProgress(), "simraPrefs", SettingsActivity.this);
                } else if (privacyOption.equals("Privacy-Distance")) {
                    writeIntToSharePrefs(privacyOption, seekBar.getProgress(), "simraPrefs", SettingsActivity.this);
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
        Log.d(TAG, "onDestroy()");
    }
}
