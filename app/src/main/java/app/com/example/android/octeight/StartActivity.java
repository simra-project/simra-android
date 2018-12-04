package app.com.example.android.octeight;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Shows general info about the app and starts the MainActivity once the okay-Button is pressed
 * or TIME_OUT/1000 seconds are passed.
 * TODO: migrate permission request and map loading from MainActivity to StartActivity
 */

public class StartActivity extends AppCompatActivity {

    private static int TIME_OUT = 10000; //Time to launch the another activity
    Button next;
    Runnable startActivityRunnable;
    Handler startActivityHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // Runnable that starts MainActivity after defined time (TIME_OUT)
        startActivityRunnable = new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(StartActivity.this, MainActivity.class);
                startActivity(i);
                // finish() to prevent going back to StartActivity, when the Back Button is pressed
                // in MainActivity
                finish();
            }
        };
        // create Handler and make it run the Runnable after TIME_OUT
        startActivityHandler = new Handler();
        startActivityHandler.postDelayed(startActivityRunnable, TIME_OUT);

        // start MainActivity when Button is clicked
        next = findViewById(R.id.nextBtn);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent (StartActivity.this, MainActivity.class);
                // remove the Callback of the Runnable to the Handler to prevent second start of
                // MainActivity
                startActivityHandler.removeCallbacks(startActivityRunnable);
                startActivity(intent);
                // finish() to prevent going back to StartActivity, when the Back Button is pressed
                // in MainActivity
                finish();
            }
        });
    }
}




