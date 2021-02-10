package de.tuberlin.mcc.simra.app.activities;


import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.entities.Profile;
import de.tuberlin.mcc.simra.app.util.SharedPref;

public class StatisticsActivity extends AppCompatActivity {
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Log tag
    private static final String TAG = "StatisticsActivity_LOG";

    ImageButton backBtn;
    TextView toolbarTxt;
    BarChart chart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

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
        String locale = Resources.getSystem().getConfiguration().locale.getLanguage();

        Profile profile = Profile.loadProfile(null, this);
        int ridesCount = profile.numberOfRides;

        // number of uploaded rides
        TextView numberOfRides = findViewById(R.id.numberOfRidesText);
        numberOfRides.setText(getText(R.string.uploaded_rides) + " " + profile.numberOfRides);
        numberOfRides.invalidate();

        // amount of co2 emissions saved by taking a bicycle instead of a car (138g/km)
        TextView co2Savings = findViewById(R.id.co2SavingsText);

        co2Savings.setText(getText(R.string.co2Savings) + " " + (Math.round(((double) (long) profile.co2 / 1000.0) * 100.0) / 100.0) + " kg");

        co2Savings.invalidate();

        // number of non-nothing incidents in uploaded rides
        TextView numberOfIncidents = findViewById(R.id.numberOFIncidentsText);
        numberOfIncidents.setText(getText(R.string.incidents) + " " + profile.numberOfIncidents);
        numberOfIncidents.invalidate();

        // number of scary non-nothing incidents in uploaded rides
        TextView numberOfScary = findViewById(R.id.numberOfScaryIncidentsText);
        numberOfScary.setText(getText(R.string.scary) + " " + profile.numberOfScaryIncidents);
        numberOfScary.invalidate();

        // total distance of all uploaded rides
        TextView distanceOfRides = findViewById(R.id.distanceOfRidesText);
        if (isImperialUnit) {
            distanceOfRides.setText(getText(R.string.distance) + " " + (Math.round(((profile.distance / 1600) * 100.0)) / 100.0) + " mi");
        } else {
            distanceOfRides.setText(getText(R.string.distance) + " " + (Math.round(((profile.distance / 1000) * 100.0)) / 100.0) + " km");
        }
        distanceOfRides.invalidate();

        // average distance per ride of all uploaded rides
        TextView avgDistanceOfRides = findViewById(R.id.averageDistanceOfRidesText);
        if (isImperialUnit && ridesCount > 0) {
            avgDistanceOfRides.setText(getText(R.string.avgDistance) + " " + (Math.round(((profile.distance / 1600 / ridesCount) * 100.0)) / 100.0) + " mi");
        } else if (isImperialUnit && ridesCount > 0) {
            avgDistanceOfRides.setText(getText(R.string.avgDistance) + " " + (Math.round(((profile.distance / 1000 / ridesCount) * 100.0)) / 100.0) + " km");
        } else {
            avgDistanceOfRides.setText(getText(R.string.avgDistance) + " - ");
        }

        avgDistanceOfRides.invalidate();

        // duration of all uploaded rides in HH:MM
        TextView durationOfRides = findViewById(R.id.durationOfRidesText);
        long rideDurationHours = ((long) profile.duration) / 3600000;
        long rideDurationMinutes = (((long) profile.duration) % 3600000) / 60000;
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
        durationOfRides.setText(getText(R.string.duration) + " " + rideDurationH + ":" + rideDurationM + " h");
        durationOfRides.invalidate();

        // average speed of per ride of all uploaded rides
        TextView averageSpeed = findViewById(R.id.averageSpeedText);
        if (isImperialUnit) {
            averageSpeed.setText(getText(R.string.average_Speed) + " " + (int) (((double) ((long) profile.distance) / 1600.0) / ((((((long) profile.duration / 1000)) - ((double) (long) profile.waitedTime)) / 3600))) + " mph");
        } else {
            averageSpeed.setText(getText(R.string.average_Speed) + " " + (int) (((double) ((long) profile.distance) / 1000.0) / ((((((long) profile.duration / 1000)) - ((double) (long) profile.waitedTime)) / 3600))) + " km/h");
        }
        averageSpeed.invalidate();

        // total duration of waited time in all uploaded rides in HH:MM
        TextView durationOfWaitedTime = findViewById(R.id.durationOfIdleText);
        long waitDurationHours = ((long) profile.waitedTime / 3600);
        long waitDurationMinutes = ((long) profile.waitedTime % 3600) / 60;
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

        TextView averageDurationOfWaitedTime = findViewById(R.id.averageDurationOfIdleText);
        long avgWaitDurationMinutes = 0L;
        if (ridesCount > 0) {
            avgWaitDurationMinutes = ((long) profile.waitedTime / 60 / ridesCount);
        }
        averageDurationOfWaitedTime.setText(getText(R.string.avgIdle) + " " + avgWaitDurationMinutes + " min");
        averageDurationOfWaitedTime.invalidate();

        chart = findViewById(R.id.timeBucketBarChart);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelsToSkip(0);
        List<BarEntry> entries = new ArrayList<>();
        int counter = 0;
        for (Float f : profile.timeDistribution) {
            entries.add(new BarEntry(f, counter));
            if (f == 0.0) {
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
            }
            counter++;
        }

        BarDataSet bardataset = new BarDataSet(entries, null);

        List<String> labels = new ArrayList<>();

        if (locale.equals(new Locale("en").getLanguage())) {
            labels.add("12am");
            labels.add("01");
            labels.add("02");
            labels.add("03");
            labels.add("04");
            labels.add("05");
            labels.add("06");
            labels.add("07");
            labels.add("08");
            labels.add("09");
            labels.add("10");
            labels.add("11");
            labels.add("12");
            labels.add("01pm");
            labels.add("02");
            labels.add("03");
            labels.add("04");
            labels.add("05");
            labels.add("06");
            labels.add("07");
            labels.add("08");
            labels.add("09");
            labels.add("10");
            labels.add("11");
        } else {
            labels.add("00");
            labels.add("01");
            labels.add("02");
            labels.add("03");
            labels.add("04");
            labels.add("05");
            labels.add("06");
            labels.add("07");
            labels.add("08");
            labels.add("09");
            labels.add("10");
            labels.add("11");
            labels.add("12");
            labels.add("13");
            labels.add("14");
            labels.add("15");
            labels.add("16");
            labels.add("17");
            labels.add("18");
            labels.add("19");
            labels.add("20");
            labels.add("21");
            labels.add("22");
            labels.add("23");
        }
        BarData data = new BarData(labels, bardataset);
        chart.setData(data); // set the data and list of lables into chart

        // bardataset.setColors(ColorTemplate.PASTEL_COLORS);
        bardataset.setColor(getResources().getColor(R.color.colorAccent, this.getTheme()));
        bardataset.setDrawValues(false);

        chart.animateY(2000);
        chart.getLegend().setEnabled(false);
        chart.setDescription(null);
        chart.setPinchZoom(false);
        chart.setDrawBarShadow(false);
        chart.setDrawGridBackground(false);
        chart.getAxisLeft().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setHighlightPerTapEnabled(false);
        chart.setHighlightPerDragEnabled(false);

        chart.invalidate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
    }


}
