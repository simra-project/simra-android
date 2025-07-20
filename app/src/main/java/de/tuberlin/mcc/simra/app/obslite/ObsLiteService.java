package de.tuberlin.mcc.simra.app.obslite;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import de.tuberlin.mcc.simra.app.services.RecorderService;

public class ObsLiteService extends Service {
    public static final String TAG = "ObsLiteService_LOG:";
    private final IBinder mBinder = new ObsLiteService.MyBinder();

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        return mBinder;
    }

    public class MyBinder extends Binder {
        public ObsLiteService getService() {
            return ObsLiteService.this;
        }
    }
}
