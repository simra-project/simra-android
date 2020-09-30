package de.tuberlin.mcc.simra.app.util;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new LoggingExceptionActivity(BaseActivity.this);
    }

}
