package de.tuberlin.mcc.simra.app.activities;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.databinding.ActivitySingleRideStatisticsBinding;
import de.tuberlin.mcc.simra.app.entities.MetaDataEntry;
import de.tuberlin.mcc.simra.app.util.SharedPref;

import static de.tuberlin.mcc.simra.app.entities.MetaData.getMetaDataEntryForRide;
import static de.tuberlin.mcc.simra.app.util.Utils.calculateCO2Savings;

public class SingleRideStatisticsActivity extends AppCompatActivity {
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Log tag
    private static final String TAG = "RideStatsActivity_LOG";
    private static final String EXTRA_RIDE_ID = "EXTRA_RIDE_ID";


    ImageButton backBtn;
    TextView toolbarTxt;
    int rideId;
    ActivitySingleRideStatisticsBinding binding;


    public static void startSingeRideStatisticsActivity(int rideId, Context context) {
        Intent intent = new Intent(context, SingleRideStatisticsActivity.class);
        intent.putExtra(EXTRA_RIDE_ID,rideId);
        context.startActivity(intent);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySingleRideStatisticsBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        toolbarTxt = findViewById(R.id.toolbar_title);
        toolbarTxt.setText(R.string.title_activity_statistics);

        backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(v -> finish());
        Boolean isImperialUnit = SharedPref.Settings.DisplayUnit.isImperial(this);

        if (!getIntent().hasExtra(EXTRA_RIDE_ID)) {
            throw new RuntimeException("Extra: " + EXTRA_RIDE_ID + " not defined.");
        }
        rideId = getIntent().getIntExtra(EXTRA_RIDE_ID, 0);

        MetaDataEntry metaDataEntry = getMetaDataEntryForRide(rideId, SingleRideStatisticsActivity.this);

        int distanceDivider = 0;
        String distanceUnit = "";
        String speedUnit = "";
        if (isImperialUnit) {
            distanceDivider = 1600;
            distanceUnit = " mi";
            speedUnit = " mph";
        } else {
            distanceDivider = 1000;
            distanceUnit = " km";
            speedUnit = " km/h";
        }

        // total distance of this ride
        TextView distanceOfRide = binding.distanceOfRideText;
        double distanceOfRideRaw = (((double)metaDataEntry.distance) / distanceDivider);
        distanceOfRide.setText(getText(R.string.distance) + " " + (Math.round(distanceOfRideRaw * 100.0) / 100.0) + distanceUnit);
        distanceOfRide.invalidate();

        // duration of this ride in HH:MM
        TextView durationOfRide = binding.durationOfRideText;
        //duration in ms
        long duration = metaDataEntry.endTime - metaDataEntry.startTime;
        long rideDurationHours = (duration) / 3600000;
        long rideDurationMinutes = ((duration) % 3600000) / 60000;
        String rideDurationH;
        String rideDurationM;
        if (rideDurationHours < 10) {
            rideDurationH = "0" + rideDurationHours;
        } else {
            rideDurationH = String.valueOf(rideDurationHours);
        }
        if (rideDurationMinutes < 10) {
            rideDurationM = "0" + rideDurationMinutes;
        } else {
            rideDurationM = String.valueOf(rideDurationMinutes);
        }
        durationOfRide.setText(getText(R.string.duration) + " " + rideDurationH + ":" + rideDurationM + " h");
        durationOfRide.invalidate();

        // total duration of waited time in this rides in HH:MM
        TextView durationOfWaitedTime = binding.durationOfIdleText;
        long waitDurationHours = (metaDataEntry.waitedTime / 3600);
        long waitDurationMinutes = (metaDataEntry.waitedTime % 3600) / 60;

        String waitDurationH;
        String waitDurationM;
        if (waitDurationHours < 10) {
            waitDurationH = "0" + waitDurationHours;
        } else {
            waitDurationH = String.valueOf(waitDurationHours);
        }
        if (waitDurationMinutes < 10) {
            waitDurationM = "0" + waitDurationMinutes;
        } else {
            waitDurationM = String.valueOf(waitDurationMinutes);
        }
        durationOfWaitedTime.setText(getText(R.string.idle) + " " + waitDurationH + ":" + waitDurationM + " h");
        durationOfWaitedTime.invalidate();

        // amount of co2 emissions saved by taking a bicycle instead of a car (138g/km)
        TextView co2SavingsText = binding.co2SavingsText;

        co2SavingsText.setText(getText(R.string.co2Savings) + " " + (Math.round((double) calculateCO2Savings(metaDataEntry.distance)) + " g"));

        co2SavingsText.invalidate();

        // average speed of per ride of all uploaded rides
        TextView averageSpeedText = binding.averageSpeedText;
        double averageSpeedRaw = ((((double) metaDataEntry.distance) / (double) distanceDivider) / ((((double) duration / (double) 1000) - ((double) metaDataEntry.waitedTime)) / (double) 3600));
        averageSpeedText.setText(getText(R.string.average_Speed) + " " + (Math.round(averageSpeedRaw * 100.0) / 100.0) + speedUnit);
        averageSpeedText.invalidate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
    }


}
