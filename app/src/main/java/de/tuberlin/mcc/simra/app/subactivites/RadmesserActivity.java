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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_connection);
        initializeToolBar();
        rootLayout = findViewById(R.id.bluetooth_screen);
        // DEBUG
        Button b1 = new Button(this);
        b1.setText("Not connected");
        Button b2 = new Button(this);
        b2.setText("Connecting");
        Button b3 = new Button(this);
        b3.setText("Connected");


        Intent intent = new Intent(RadmesserActivity.this, RadmesserService.class);
        startService(intent);
        bindService(intent, mRadmesserServiceConnection, Context.BIND_IMPORTANT);

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBoundRadmesserService.setConnectionStatus(0);
            }
        });
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBoundRadmesserService.setConnectionStatus(1);
            }
        });
        b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBoundRadmesserService.setConnectionStatus(2);
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
            RadmesserService.MyBinder myBinder = (RadmesserService.MyBinder) service;
            mBoundRadmesserService= myBinder.getService();
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

