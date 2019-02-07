package app.com.example.android.octeight;

import android.app.Application;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        new LoggingExceptionHandler(this);
    }
}