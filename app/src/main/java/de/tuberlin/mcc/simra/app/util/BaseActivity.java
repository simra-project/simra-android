package de.tuberlin.mcc.simra.app.util;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
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

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        SystemBarInsets.install(this);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        SystemBarInsets.install(this);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        SystemBarInsets.install(this);
    }
}
