package de.tuberlin.mcc.simra.app.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import de.tuberlin.mcc.simra.app.activities.MainActivity;
import de.tuberlin.mcc.simra.app.activities.StartActivity;

import java.util.Objects;


public class PermissionHelper {
    public static final BasePermission Location = new BasePermission(1001, new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
    });

    public static final int REQUEST_CODE_CAMERA = 1003;
    public static final BasePermission Camera = new BasePermission(REQUEST_CODE_CAMERA, new String[]{
            Manifest.permission.CAMERA
    });

    /**
     * Checks if the Permission for this App where already granted by the user
     * or if they are given by default (e.g. for a previous SDK version)
     *
     * @param context
     * @return True if all Permissions were granted before
     */
    public static boolean hasPermissions(String[] permissions, Context context) {
        for (String permission : permissions) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N && Objects.equals(permission, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                // Before SDK29 the Background Location Permission was automatically granted with a location request:
                // https://developer.android.com/reference/android/Manifest.permission#ACCESS_BACKGROUND_LOCATION
                return true;

            } else {
                // Default Check
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void requestFirstBasePermissionsNotGranted( Activity activity) {
        if (!hasPermissions(Location.permissions, activity)) {
            Location.requestPermissions(activity);
        }
    }

    public static boolean hasBasePermissions(Context context) {
        return hasPermissions(Location.permissions, context);
    }


    public static class BasePermission {
        public int PERMISSION_REQUEST_CODE = 0;
        public String[] permissions = {};

        BasePermission(int permissionRequestCode, String[] ipermissions) {
            PERMISSION_REQUEST_CODE = permissionRequestCode;
            permissions = ipermissions;
        }

        public void requestPermissions(Activity activity) {
            ActivityCompat.requestPermissions(activity, permissions, PERMISSION_REQUEST_CODE);
        }

        public boolean hasPermission(Context context) {
            return hasPermissions(permissions, context);
        }

    }
}


