package de.tuberlin.mcc.simra.app.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.Objects;


public class PermissionHelper {
    // Log tag
    private static final String TAG = "PermissionHelper_LOG";

    public static final BasePermission Storage = new BasePermission(1002, new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    });

    private static final BasePermission LocationAndStorage = new BasePermission(1004,new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    });
    public static final BasePermission Location = new BasePermission(1001, new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
    });
    private static final BasePermission LocationAndStorageAndroidR = new BasePermission(1004,new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    });
    public static final BasePermission LocationAndroidR = new BasePermission(1001, new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
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
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N && (Objects.equals(permission, Manifest.permission.WRITE_EXTERNAL_STORAGE) || Objects.equals(permission, Manifest.permission.READ_EXTERNAL_STORAGE))) {
                // Before version N the Storage permissions were granted on app install, so we should not check
                // https://developer.android.com/reference/android/Manifest.permission#READ_EXTERNAL_STORAGE
                return true;
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N && Objects.equals(permission, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {                // Before SDK29 the Background Location Permission was automatically granted with a location request:
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (!hasBasePermissions(activity)) {
                LocationAndStorage.requestPermissions(activity);
            } else if (!hasPermissions(Location.permissions, activity)) {
                Location.requestPermissions(activity);
            } else if (!hasPermissions(Storage.permissions, activity)) {
                Storage.requestPermissions(activity);
            }
        } else {
            if (!hasBasePermissions(activity)) {
                LocationAndStorageAndroidR.requestPermissions(activity);
            } else if (!hasPermissions(LocationAndroidR.permissions, activity)) {
                LocationAndroidR.requestPermissions(activity);
            } else if (!hasPermissions(Storage.permissions, activity)) {
                Storage.requestPermissions(activity);
            }
        }

    }

    public static boolean hasBasePermissions(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return hasPermissions(Location.permissions, context) && hasPermissions(Storage.permissions, context);
        } else {
            return hasPermissions(LocationAndroidR.permissions, context);
        }
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


