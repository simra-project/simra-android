package de.tuberlin.mcc.simra.app.activities;


import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import java.util.Arrays;
import java.util.Locale;

import de.tuberlin.mcc.simra.app.R;

import static de.tuberlin.mcc.simra.app.util.SharedPref.lookUpSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.getGlobalProfile;

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
        String unit = lookUpSharedPrefs("Settings-Unit", "m", "simraPrefs", this);
        String locale = Resources.getSystem().getConfiguration().locale.getLanguage();
        // String[] profileValues = readContentFromFile("profile.csv", this).split(System.lineSeparator())[2].split(",");
        Object[] profileValues = getGlobalProfile(this);

        // number of uploaded rides
        TextView numberOfRides = findViewById(R.id.numberOfRidesText);
        int ridesCount = (int) profileValues[4];
        numberOfRides.setText(getText(R.string.uploaded_rides) + " " + ridesCount);
        numberOfRides.invalidate();

        // amount of co2 emissions saved by taking a bicycle instead of a car (138g/km)
        TextView co2Savings = findViewById(R.id.co2SavingsText);

        co2Savings.setText(getText(R.string.co2Savings) + " " + (Math.round((Double.valueOf((long) profileValues[9]) / 1000.0) * 100.0) / 100.0) + " kg");

        co2Savings.invalidate();

        // number of non-nothing incidents in uploaded rides
        TextView numberOfIncidents = findViewById(R.id.numberOFIncidentsText);
        numberOfIncidents.setText(getText(R.string.incidents) + " " + profileValues[6]);
        numberOfIncidents.invalidate();

        // number of scary non-nothing incidents in uploaded rides
        TextView numberOfScary = findViewById(R.id.numberOfScaryIncidentsText);
        numberOfScary.setText(getText(R.string.scary) + " " + profileValues[34]);
        numberOfScary.invalidate();

        // total distance of all uploaded rides
        TextView distanceOfRides = findViewById(R.id.distanceOfRidesText);
        double distance = Double.valueOf((long) profileValues[8]);
        if (unit.equals("ft")) {
            distanceOfRides.setText(getText(R.string.distance) + " " + (Math.round(((distance / 1600) * 100.0)) / 100.0) + " mi");
        } else {
            distanceOfRides.setText(getText(R.string.distance) + " " + (Math.round(((distance / 1000) * 100.0)) / 100.0) + " km");
        }
        distanceOfRides.invalidate();

        // average distance per ride of all uploaded rides
        TextView avgDistanceOfRides = findViewById(R.id.averageDistanceOfRidesText);
        if (unit.equals("ft") && ridesCount > 0) {
            avgDistanceOfRides.setText(getText(R.string.avgDistance) + " " + (Math.round(((distance / 1600 / ridesCount) * 100.0)) / 100.0) + " mi");
        } else if (unit.equals("m") && ridesCount > 0) {
            avgDistanceOfRides.setText(getText(R.string.avgDistance) + " " + (Math.round(((distance / 1000 / ridesCount) * 100.0)) / 100.0) + " km");
        } else {
            avgDistanceOfRides.setText(getText(R.string.avgDistance) + " - ");
        }

        avgDistanceOfRides.invalidate();

        // duration of all uploaded rides in HH:MM
        TextView durationOfRides = findViewById(R.id.durationOfRidesText);
        long rideDurationHours = ((long) profileValues[5]) / 3600000;
        long rideDurationMinutes = (((long) profileValues[5]) % 3600000) / 60000;
        // Log.d(TAG, "rideDurationHours: " + rideDurationHours + " rideDurationMinutes: " + rideDurationMinutes);
        //(new BigDecimal((long)profileValues[5])).divide(new BigDecimal(3600000),2,BigDecimal.ROUND_CEILING) + " h")
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
        if (unit.equals("ft")) {
            averageSpeed.setText(getText(R.string.average_Speed) + " " + (int) ((Double.valueOf(((long) profileValues[8])) / 1600.0) / (((((Double.valueOf((long) profileValues[5]) / 1000)) - (Double.valueOf((long) profileValues[7]))) / 3600))) + " mph");
        } else {
            averageSpeed.setText(getText(R.string.average_Speed) + " " + (int) ((Double.valueOf(((long) profileValues[8])) / 1000.0) / (((((Double.valueOf((long) profileValues[5]) / 1000)) - (Double.valueOf((long) profileValues[7]))) / 3600))) + " km/h");
        }
        averageSpeed.invalidate();

        // total duration of waited time in all uploaded rides in HH:MM
        TextView durationOfWaitedTime = findViewById(R.id.durationOfIdleText);
        long waitDurationHours = ((long) profileValues[7] / 3600);
        long waitDurationMinutes = (((long) profileValues[7] % 3600) / 60);
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
            avgWaitDurationMinutes = ((long) profileValues[7] / 60 / ridesCount);
        }
        averageDurationOfWaitedTime.setText(getText(R.string.avgIdle) + " " + avgWaitDurationMinutes + " min");
        averageDurationOfWaitedTime.invalidate();

        chart = (BarChart) findViewById(R.id.timeBucketBarChart);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelsToSkip(0);
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 11; i < 34; i++) {
            entries.add(new BarEntry((float) profileValues[i], i - 10));
            if (entries.get(i - 11).getVal() == 0.0) {
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
            }
        }

        Log.d(TAG, "entries.size(): " + entries.size() + " " + Arrays.toString(entries.toArray()));

        BarDataSet bardataset = new BarDataSet(entries, null);

        ArrayList<String> labels = new ArrayList<String>();

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
