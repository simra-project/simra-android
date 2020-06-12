package de.tuberlin.mcc.simra.app.subactivites;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.services.RadmesserService;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


public class RadmesserActivity extends AppCompatActivity {

    LinearLayout rootLayout;
    RadmesserService mBoundRadmesserService;
    Button b1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_connection);
        initializeToolBar();
        rootLayout = findViewById(R.id.bluetooth_screen);

        Intent intent = new Intent(RadmesserActivity.this, RadmesserService.class);
        startService(intent);
        bindService(intent, mRadmesserServiceConnection, Context.BIND_IMPORTANT);

        // DEBUG
        b1 = new Button(this);
        Button b2 = new Button(this);
        b2.setText("Connecting");
        Button b3 = new Button(this);

        b3.setText("Connected");




        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //mBoundRadmesserService.setConnectionStatus(2);
            }
        });

        rootLayout.addView(b1);
        rootLayout.addView(b2);
        rootLayout.addView(b3);



    }

    private ServiceConnection mRadmesserServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RadmesserService.LocalBinder myBinder = (RadmesserService.LocalBinder) service;
            mBoundRadmesserService = myBinder.getService();
            b1.setText(mBoundRadmesserService.getCurrentConnectionStatus().toString());

        }
    };

    private void initializeToolBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        TextView toolbarTxt = findViewById(R.id.toolbar_title);
        toolbarTxt.setText("Radmesser");

        ImageButton backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           finish();
                                       }
                                   }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}

