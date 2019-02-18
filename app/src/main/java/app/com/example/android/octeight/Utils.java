package app.com.example.android.octeight;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.content.Context.MODE_APPEND;

public class Utils {

    public static void appendToFile(String content, String fileName, Context context) {
        try {
            FileOutputStream writer = context.openFileOutput(fileName, MODE_APPEND);
            writer.write(content.getBytes());
            writer.flush();
            writer.close();
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    public static void overWriteFile(String content, String fileName,  Context context) {
        try {
            FileOutputStream writer = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            writer.write(content.getBytes());
            writer.flush();
            writer.close();
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    public static boolean fileExists(String fileName, Context context) {
        return context.getFileStreamPath(fileName).exists();
    }

    public static String lookUpSharedPrefs(String key, String defValue, String sharedPrefName, Context context){
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        return sharedPrefs.getString(key, defValue);
    }

    public static void writeToSharePrefs(String key, String value, String sharedPrefName, Context context){
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getUniqueUserID(Context context){
        String id = lookUpSharedPrefs("USER-ID", "0", "simraPrefs", context);
        if (id.equals("0")) {
            id = String.valueOf(System.currentTimeMillis());
            writeToSharePrefs("USER-ID", id, "simraPrefs", context);
        }
        return id;
    }


    // Check if an accEvent has already been annotated based on one line of the accEvents csv file.

    public static boolean checkForAnnotation(String[] incidentProps) {

        // Only checking for empty strings, which means we are retaining
        // events that were labeled as 'nothing happened'

        if((incidentProps[4] == "") && (incidentProps[5] == "") &&
                (incidentProps[6] == "")) {

            return false;

        } else {

            return true;

        }

    }

}
