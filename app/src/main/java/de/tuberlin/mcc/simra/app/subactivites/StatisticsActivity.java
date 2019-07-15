package de.tuberlin.mcc.simra.app.subactivites;


import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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

import de.tuberlin.mcc.simra.app.R;

import static de.tuberlin.mcc.simra.app.util.Utils.getProfile;
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
        String locale = Resources.getSystem().getConfiguration().locale.getLanguage();
        // String[] profileValues = readContentFromFile("profile.csv", this).split(System.lineSeparator())[2].split(",");
        Object[] profileValues = getProfile(this);
        TextView numberOfRides = findViewById(R.id.numberOfRides);
        numberOfRides.setText(getText(R.string.uploaded_rides) + " " + profileValues[4]);

        TextView distanceOfRides = findViewById(R.id.distanceOfRides);
        if (locale.equals(new Locale("en").getLanguage())) {
            distanceOfRides.setText(getText(R.string.distance) + " " + Math.round((Float.valueOf(String.valueOf(profileValues[8])))/1600) + " mi");
        } else {
            distanceOfRides.setText(getText(R.string.distance) + " " + Math.round((Float.valueOf(String.valueOf(profileValues[8])))/1000) + " km");
        }

        TextView durationOfRides = findViewById(R.id.durationOfRides);
        durationOfRides.setText(getText(R.string.duration) + " " + ((new BigDecimal((long)profileValues[5])).divide(new BigDecimal(3600000),2,BigDecimal.ROUND_CEILING) + " h"));

        TextView durationOfWaitedTime = findViewById(R.id.durationOfWaitedTime);
        durationOfWaitedTime.setText(getText(R.string.idle) + " " + ((new BigDecimal((long)profileValues[7]).divide(new BigDecimal(3600),2,BigDecimal.ROUND_CEILING) + " h")));

        TextView numberOfIncidents = findViewById(R.id.numberOfIncidents);
        numberOfIncidents.setText(getText(R.string.incidents) + " " + profileValues[6]);

        TextView co2Savings = findViewById(R.id.co2Savings);
        co2Savings.setText(getText(R.string.co2Savings) + " " + profileValues[9] + " g");



        chart = (BarChart) findViewById(R.id.timeBucketBarChart);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelsToSkip(0);
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 10; i < 33; i++) {
            entries.add(new BarEntry((float)profileValues[i],i-10));
            if(entries.get(i-10).getVal() == 0.0) {
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
