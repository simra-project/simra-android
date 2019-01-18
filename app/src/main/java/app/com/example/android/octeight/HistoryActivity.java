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

    String accGpsString;
    String date;
    int state;

    /**
     * @TODO: When this Activity gets started automatically after the route recording is finished,
     * the route gets shown immediately by calling ShowRouteActivity.
     * Otherwise, this activity has to scan for saved rides (maybe as files in the internal storage
     * or as entries in sharedPreference) and display them in a list.
     *
     * The user must be able to select a ride which should start the ShowRouteActivity with that ride.
     *
     */



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // This button will change. Every list item needs its own button (maybe they can
        // be created dynamically) where ShowRouteActivity gets started with the "Ride" (see Ride
        // class) the list item represents.
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Checks if HistoryActivity was started by the user or by the app after a route
                // recording was finished
                if(getIntent().hasExtra("AccGpsString")){
                    // AccGpsString contains the accelerometer and location data as well as time data
                    accGpsString = getIntent().getStringExtra("AccGpsString");
                    // Date in form of system date (day.month.year hour:minute:second if german)
                    date = getIntent().getStringExtra("Date");
                    // State can be 0 for server processing not started, 1 for started and pending
                    // and 2 for processed by server so the incidents can be annotated by the user
                    state = getIntent().getIntExtra("State", 0);
                }


                // Checks whether a ride was selected or not. Maybe it will be possible to select
                // mutliple rides and push a button to send them all to the server to be analyzed
                if(accGpsString != null && date != null) {
                    Snackbar.make(view, getString(R.string.selectedRideInfoDE) + date, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    // Start ShowRouteActivity with the selected Ride.
                    Intent intent = new Intent(HistoryActivity.this, ShowRouteActivity.class);
                    intent.putExtra("AccGpsString", accGpsString);
                    intent.putExtra("Date", date);
                    intent.putExtra("State", state);
                    startActivity(intent);
                } else {
                    Snackbar.make(view, getString(R.string.errorNoRideSelectedDE) + date, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }



            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Press immediately the button, if HistoryActivity was created automatically after the
        // recording of a route has finished
        if(getIntent().hasExtra("AccGpsString")){
            fab.performClick();
            fab.setPressed(true);
            fab.invalidate();
            fab.setPressed(false);
            fab.invalidate();
        }

    }

}