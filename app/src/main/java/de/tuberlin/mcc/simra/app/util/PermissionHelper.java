package de.tuberlin.mcc.simra.app.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class PermissionHelper {
    public static BasePermission Location = new BasePermission(1001, new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
    });
    public static BasePermission Storage = new BasePermission(1002, new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    });
    public static BasePermission Camera = new BasePermission(1003, new String[]{
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
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N && (permission == Manifest.permission.WRITE_EXTERNAL_STORAGE || permission == Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Before version N the Storage permissions were granted on app install, so we should not check
                // https://developer.android.com/reference/android/Manifest.permission#READ_EXTERNAL_STORAGE
                return true;
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION) {
                // Before SDK29 the Background Location Permission was automatically granted with a location request:
                // https://developer.android.com/reference/android/Manifest.permission#ACCESS_BACKGROUND_LOCATION
                return true;

            } else {
                // Default Check
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                ) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void requestFirstBasePermissionsNotGranted(Activity activity) {
        if (!hasPermissions(Location.permissions, activity)) {
            Location.requestPermissions(activity);
            return;
        }
        if (!hasPermissions(Storage.permissions, activity)) {
            Storage.requestPermissions(activity);
            return;
        }
    }

    public static boolean hasBasePermissions(Context context) {
        return hasPermissions(Location.permissions, context) && hasPermissions(Storage.permissions, context);
    }


    public static class BasePermission {
        public int PERMISSION_REQQUEST_CODE = 0;
        public String[] permissions = {};

        BasePermission(int permissionRequestCode, String[] ipermissions) {
            PERMISSION_REQQUEST_CODE = permissionRequestCode;
            permissions = ipermissions;
        }

        public void requestPermissions(Activity activity) {
            ActivityCompat.requestPermissions(activity, permissions, PERMISSION_REQQUEST_CODE);
        }

        public boolean hasPermission(Context context) {
            return hasPermissions(permissions, context);
        }

    }
}


