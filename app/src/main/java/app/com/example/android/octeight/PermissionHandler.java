package app.com.example.android.octeight;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import static android.support.v4.app.ActivityCompat.shouldShowRequestPermissionRationale;

public class PermissionHandler {

    private static final int LOCATION_ACCESS_CODE = 1;

    public static synchronized boolean permissionGrantCheck(Context context) {

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            return false;

        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

            return false;

        }

        return true;

    }


    public static synchronized void askPermission(Activity activity) {

        final Activity helper = activity;

        if (!shouldShowRequestPermissionRationale(helper, Manifest.permission.ACCESS_FINE_LOCATION)) {
            showMessageOKCancel("Um fortzufahren, erlaube im folgenden Fenster bitte den Zugriff auf" +
                            " Deine Standortdaten.",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            helper.requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                                    LOCATION_ACCESS_CODE);
                        }
                    }, helper);
            return;
        }

        helper.requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_ACCESS_CODE);
        return;

    }

    private static synchronized void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener,
                                                         Activity activity) {
        new android.support.v7.app.AlertDialog.Builder(activity)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", okListener)
                .create()
                .show();
    }


}
