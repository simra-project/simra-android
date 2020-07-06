package de.tuberlin.mcc.simra.app.util;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import de.tuberlin.mcc.simra.app.BuildConfig;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.DEBUG) {
            return;
        }
        new LoggingExceptionActivity(BaseActivity.this);
    }

}
