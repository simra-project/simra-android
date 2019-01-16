package app.com.example.android.octeight;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Array;
import java.util.Arrays;

public class HistoryActivity extends AppCompatActivity {

    // Log tag
    private static final String TAG = "HistoryActivity_LOG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String accGpsString = getIntent().getStringExtra("AccGpsString");
        String date = getIntent().getStringExtra("Date");
        Ride ride = new Ride(accGpsString, date);

        if (date != null) {
            ride = new Ride(accGpsString, date);

            Log.d(TAG, "ride.getTimeStamp(): " + ride.getTimeStamp());
            Log.d(TAG, "ride.getRouteLine(): " + Arrays.toString(ride.getRoute().getPoints().toArray()));
        }

        Intent intent = new Intent (HistoryActivity.this, ShowRouteActivity.class);
        // Log.d(TAG, "point = " + line.getPoints().get(0).toString());
        intent.putExtra("AccGpsString", accGpsString);
        intent.putExtra("Date", date);
        startActivity(intent);
    }

}
