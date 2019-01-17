package app.com.example.android.octeight;

import android.app.Activity;
import android.util.Log;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShowRouteUtils {
    final static String TAG = "ShowRouteUtils_LOG";

    public static String[] getFiles(Activity activity){
        String[] fileNames = activity.fileList();
        // Arrays.sort(fileNames);
        Log.d(TAG, Arrays.deepToString(fileNames));
        return fileNames;
    }

    public Polyline getRouteLine(File file){
        List<GeoPoint> geoPoints = new ArrayList<>();
        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            // br.readLine() to skip the first line which contains the headers
            String line= br.readLine();

            while ((line = br.readLine()) != null) {
                //Log.d(TAG, line);
                try {
                    String[] separatedLine = line.split(",");
                    double latitude = Double.valueOf(separatedLine[0]);
                    double longitude = Double.valueOf(separatedLine[1]);
                    geoPoints.add(new GeoPoint(latitude, longitude));
                } catch (Exception e){
                    e.printStackTrace();
                    continue;
                }
            }
            br.close();
        }
        catch (IOException e) {

            e.printStackTrace();
            Log.d(TAG, e.getMessage());
        }
        Polyline line = new Polyline();   //see note below!
        line.setPoints(geoPoints);
        return line;

    }

    public static String getCsvAsStringFromFile(Activity activity, String pathToFile){

        List<GeoPoint> geoPoints = new ArrayList<>();

        File gpsFile = activity.getFileStreamPath(pathToFile);

        //Read text from file
        String text = "";

        try {
            BufferedReader br = new BufferedReader(new FileReader(gpsFile));
            // br.readLine() to skip the first line which contains the headers
            String line= br.readLine();

            while ((line = br.readLine()) != null) {
                //Log.d(TAG, line);
                try {
                    String[] separatedLine = line.split(",");
                    String[] dateAndQ = separatedLine[7].split(":");
                    String date = dateAndQ[0]+":"+dateAndQ[1]+":"+dateAndQ[2].charAt(0)+dateAndQ[2].charAt(1);
                    String q = dateAndQ[2].substring(2);
                    String correctedLine = separatedLine[0]+","+separatedLine[1]+","+separatedLine[2]+","+
                            separatedLine[3]+","+separatedLine[4]+","+separatedLine[5]+","+separatedLine[6]+","+
                            date + "," + q + System.lineSeparator();

                    text += correctedLine;
                } catch (Exception e){
                    e.printStackTrace();
                    continue;
                }
            }
            br.close();
        }
        catch (IOException e) {

            e.printStackTrace();
            Log.d(TAG, e.getMessage());

        }

        return text;
    }


    }
