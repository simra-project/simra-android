package de.tuberlin.mcc.simra.app.subactivites;


import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import de.tuberlin.mcc.simra.app.R;

import static de.tuberlin.mcc.simra.app.util.Utils.getProfile;
import static de.tuberlin.mcc.simra.app.util.Utils.lookUpSharedPrefs;
import static org.apache.commons.lang3.time.DateUtils.round;

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
        backBtn.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           finish();
                                       }
                                   }
        );
        String unit = lookUpSharedPrefs("Settings-Unit","m","simraPrefs",this);
        String locale = Resources.getSystem().getConfiguration().locale.getLanguage();
        // String[] profileValues = readContentFromFile("profile.csv", this).split(System.lineSeparator())[2].split(",");
        Object[] profileValues = getProfile(this);
        TextView numberOfRides = findViewById(R.id.numberOfRides);
        numberOfRides.setText(getText(R.string.uploaded_rides) + " " + profileValues[4]);

        TextView distanceOfRides = findViewById(R.id.distanceOfRides);
        if (unit.equals("ft")) {
            distanceOfRides.setText(getText(R.string.distance) + " " + (Math.round(((Double.valueOf((long) profileValues[8])/1600)*100.0))/100.0) + " mi");
        } else {
            distanceOfRides.setText(getText(R.string.distance) + " " + (Math.round(((Double.valueOf((long) profileValues[8])/1000)*100.0))/100.0) + " km");
        }

        TextView durationOfRides = findViewById(R.id.durationOfRides);
        long rideDurationHours = ((long) profileValues[5])/3600000;
        long rideDurationMinutes = (((long) profileValues[5])%3600000)/60000;
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
        durationOfRides.setText(getText(R.string.duration) + " " + rideDurationH + ":" + rideDurationM);

        TextView durationOfWaitedTime = findViewById(R.id.durationOfWaitedTime);
        long waitDurationHours = ((long) profileValues[7]/3600);
        long waitDurationMinutes = (((long) profileValues[7]%3600)/60);
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
        durationOfWaitedTime.setText(getText(R.string.idle) + " " + waitDurationH + ":" + waitDurationM);

        TextView averageSpeed = findViewById(R.id.averageSpeed);

        if (unit.equals("ft")) {
            averageSpeed.setText(getText(R.string.average_Speed) + " " + (int) ((Double.valueOf(((long) profileValues[8]))/1600.0)/(((((Double.valueOf((long) profileValues[5])/1000)) - (Double.valueOf((long) profileValues[7])))/3600))) + " mph");
        } else {
            averageSpeed.setText(getText(R.string.average_Speed) + " " + (int) ((Double.valueOf(((long) profileValues[8]))/1000.0)/(((((Double.valueOf((long) profileValues[5])/1000)) - (Double.valueOf((long) profileValues[7])))/3600))) + " km/h");
        }

        TextView numberOfIncidents = findViewById(R.id.numberOfIncidents);
        numberOfIncidents.setText(getText(R.string.incidents) + " " + profileValues[6]);

        TextView numberOfScary = findViewById(R.id.numberOfScary);
        numberOfScary.setText(getText(R.string.scary) + " " + profileValues[34]);


        TextView co2Savings = findViewById(R.id.co2Savings);
        if ((long)profileValues[9] > 10000) {
            co2Savings.setText(getText(R.string.co2Savings) + " " + ((Double.valueOf((long)profileValues[9])/1000)*10.0) + " kg");
        } else {
            co2Savings.setText(getText(R.string.co2Savings) + " " + profileValues[9] + " g");
        }


        chart = (BarChart) findViewById(R.id.timeBucketBarChart);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelsToSkip(0);
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 11; i < 34; i++) {
            entries.add(new BarEntry((float)profileValues[i],i-10));
            if(entries.get(i-11).getVal() == 0.0) {
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
        BarData data = new BarData(labels,bardataset);
        chart.setData(data); // set the data and list of lables into chart

        // bardataset.setColors(ColorTemplate.PASTEL_COLORS);
        bardataset.setColor(getResources().getColor(R.color.colorAccent,this.getTheme()));
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
