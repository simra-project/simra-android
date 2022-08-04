package de.tuberlin.mcc.simra.app.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.google.android.material.snackbar.Snackbar;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.databinding.ActivityHistoryBinding;
import de.tuberlin.mcc.simra.app.entities.DataLogEntry;
import de.tuberlin.mcc.simra.app.entities.MetaData;
import de.tuberlin.mcc.simra.app.entities.Profile;
import de.tuberlin.mcc.simra.app.services.UploadService;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import de.tuberlin.mcc.simra.app.util.IOUtils;
import de.tuberlin.mcc.simra.app.util.SharedPref;
import de.tuberlin.mcc.simra.app.entities.DataLog;

import static de.tuberlin.mcc.simra.app.util.SharedPref.lookUpBooleanSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.SharedPref.writeBooleanToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.fireProfileRegionPrompt;
import static de.tuberlin.mcc.simra.app.util.Utils.overwriteFile;

import org.json.*;

import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Track;
import io.jenetics.jpx.TrackSegment;
import io.jenetics.jpx.WayPoint;

import org.xml.sax.*;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


public class HistoryActivity extends BaseActivity {
    private static final String TAG = "HistoryActivity_LOG";
    ActivityHistoryBinding binding;
    boolean exitWhenDone = false;
    Uri UriPath;
    Context con;
    int rideId;

    String[] ridesArr;
    BroadcastReceiver br;

    public static void startHistoryActivity(Context context) {
        Intent intent = new Intent(context, HistoryActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        //  Toolbar
        setSupportActionBar(binding.toolbar.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        binding.toolbar.toolbar.setTitle("");
        binding.toolbar.toolbar.setSubtitle("");
        binding.toolbar.toolbarTitle.setText(R.string.title_activity_history);

        binding.toolbar.backButton.setOnClickListener(v -> finish());


        binding.listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            LinearLayout historyButtons = binding.buttons;
            boolean isUp = true;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (isUp && view.getLastVisiblePosition() + 1 == totalItemCount) {
                    historyButtons.animate().translationX(historyButtons.getWidth() / 2f);
                    isUp = false;
                } else if (!isUp && !(view.getLastVisiblePosition() + 1 == totalItemCount)) {
                    historyButtons.animate().translationX(0);
                    isUp = true;
                    // historyButtons.setVisibility(View.VISIBLE);
                }
            }
        });

        binding.upload.setOnClickListener(view -> {
            if (!lookUpBooleanSharedPrefs("uploadWarningShown", false, "simraPrefs", HistoryActivity.this)) {
                fireUploadPrompt();
            } else if (Profile.loadProfile(null, HistoryActivity.this).region == 0) {
                fireProfileRegionPrompt(SharedPref.App.Regions.getLastSeenRegionsID(HistoryActivity.this),HistoryActivity.this);
            } else {
                Intent intent = new Intent(HistoryActivity.this, UploadService.class);
                startService(intent);
                Toast.makeText(HistoryActivity.this, getString(R.string.upload_started), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshMyRides() {
        List<String[]> metaDataLines = new ArrayList<>();

        File metaDataFile = IOUtils.Files.getMetaDataFile(this);
        if (metaDataFile.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(metaDataFile));
                // br.readLine() to skip the first line which contains the headers
                br.readLine();
                br.readLine();
                String line;
                while (((line = br.readLine()) != null)) {
                    if (!line.startsWith("key") && !line.startsWith("null")) {
                        metaDataLines.add(line.split(","));
                    }
                }
                Log.d(TAG, "metaDataLines: " + Arrays.deepToString(metaDataLines.toArray()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            ridesArr = new String[metaDataLines.size()];
            for (int i = 0; i < metaDataLines.size(); i++) {
                String[] metaDataLine = metaDataLines.get(i);
                if (metaDataLine.length > 2 && !(metaDataLine[0].equals("key"))) {
                    ridesArr[((metaDataLines.size()) - i) - 1] = listToTextShape(metaDataLine);
                }
            }

            List<String> stringArrayList = new ArrayList<>(Arrays.asList(ridesArr));
            MyArrayAdapter myAdapter = new MyArrayAdapter(this, R.layout.row_icons, stringArrayList, metaDataLines);
            binding.listView.setAdapter(myAdapter);

        } else {

            Log.d(TAG, "metaData.csv doesn't exists");

            Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator_layout), (getString(R.string.noHistory)), Snackbar.LENGTH_LONG);
            snackbar.show();

        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        br = new MyBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("de.tuberlin.mcc.simra.app.UPLOAD_COMPLETE");
        this.registerReceiver(br, filter);
        refreshMyRides();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(br);
    }

    private String listToTextShape(String[] item) {
        String todo = getString(R.string.newRideInHistoryActivity);

        if (item[3].equals("1")) {
            todo = getString(R.string.rideAnnotatedInHistoryActivity);
        } else if (item[3].equals("2")) {
            todo = getString(R.string.rideUploadedInHistoryActivity);
        }

        long millis = Long.parseLong(item[2]) - Long.parseLong(item[1]);
        int minutes = Math.round((millis / 1000 / 60));
        Date dt = new Date(Long.parseLong(item[1]));
        Calendar localCalendar = Calendar.getInstance(TimeZone.getDefault());
        localCalendar.setTime(dt);
        Locale locale = Resources.getSystem().getConfiguration().locale;

        SimpleDateFormat wholeDateFormat = new SimpleDateFormat(getString(R.string.datetime_format), locale);
        String datetime = wholeDateFormat.format(dt);

        if (item.length > 6) {
            return "#" + item[0] + ";" + datetime + ";" + todo + ";" + minutes + ";" + item[3] + ";" + item[6];
        } else {
            return "#" + item[0] + ";" + datetime + ";" + todo + ";" + minutes + ";" + item[3] + ";" + 0;
        }
    }

    public void fireDeletePrompt(int position, MyArrayAdapter arrayAdapter) {
        AlertDialog.Builder alert = new AlertDialog.Builder(HistoryActivity.this);
        alert.setTitle(getString(R.string.warning));
        alert.setMessage(getString(R.string.delete_file_warning));
        alert.setPositiveButton(R.string.delete_ride_approve, (dialog, id) -> {
            File[] dirFiles = getFilesDir().listFiles();
            Log.d(TAG, "btnDelete.onClick() dirFiles: " + Arrays.deepToString(dirFiles));
            String clicked = (String) binding.listView.getItemAtPosition(position);
            Log.d(TAG, "btnDelete.onClick() clicked: " + clicked);
            clicked = clicked.replace("#", "").split(";")[0];
            if (dirFiles.length != 0) {
                for (File actualFile : dirFiles) {
                    if (actualFile.getName().startsWith(clicked + "_") || actualFile.getName().startsWith("accEvents" + clicked)) {

                        /* don't delete the following line! */
                        Log.i(TAG, actualFile.getName() + " deleted: " + actualFile.delete());
                    }
                }
            }
            MetaData.deleteMetaDataEntryForRide(Integer.parseInt(clicked), this);
            Toast.makeText(HistoryActivity.this, R.string.ride_deleted, Toast.LENGTH_SHORT).show();
            refreshMyRides();
        });
        alert.setNegativeButton(R.string.cancel, (dialog, id) -> {
        });
        alert.show();

    }

    public void fireUploadPrompt() {
        AlertDialog.Builder alert = new AlertDialog.Builder(HistoryActivity.this);
        alert.setTitle(getString(R.string.warning));
        alert.setMessage(getString(R.string.upload_file_warning));
        alert.setPositiveButton(R.string.upload, (dialog, id) -> {
            if (Profile.loadProfile(null, HistoryActivity.this).region == 0) {
                fireProfileRegionPrompt(SharedPref.App.Regions.getLastSeenRegionsID(HistoryActivity.this),HistoryActivity.this);
            } else {
                writeBooleanToSharedPrefs("uploadWarningShown", true, "simraPrefs", HistoryActivity.this);
                Intent intent = new Intent(HistoryActivity.this, UploadService.class);
                startService(intent);
                Toast.makeText(HistoryActivity.this, getString(R.string.upload_started), Toast.LENGTH_SHORT).show();
                if (exitWhenDone) {
                    HistoryActivity.this.moveTaskToBack(true);
                }
            }
        });
        alert.setNegativeButton(R.string.cancel, (dialog, id) -> {
        });
        alert.show();
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean uploadSuccessful = intent.getBooleanExtra("uploadSuccessful", false);
            boolean foundARideToUpload = intent.getBooleanExtra("foundARideToUpload", true);
            if (!foundARideToUpload) {
                Toast.makeText(getApplicationContext(), R.string.nothing_to_upload, Toast.LENGTH_LONG).show();
            } else if (!uploadSuccessful) {
                Toast.makeText(getApplicationContext(), R.string.upload_failed, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.upload_completed, Toast.LENGTH_LONG).show();
            }

            refreshMyRides();
        }
    }

    public class MyArrayAdapter extends ArrayAdapter<String> {
        String TAG = "MyArrayAdapter_LOG";

        Context context;
        int layoutResourceId;
        List<String> stringList;
        List<String[]> metaDataLines;

        public MyArrayAdapter(Context context, int layoutResourceId, List<String> stringList, List<String[]> metaDataLines) {

            super(context, layoutResourceId, stringList);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.stringList = stringList;
            this.metaDataLines = metaDataLines;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            Holder holder = null;

            if (row == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);
                holder = new Holder();
                holder.rideDate = row.findViewById(R.id.row_icons_ride_date);
                holder.rideTime = row.findViewById(R.id.row_ride_time);
                holder.duration = row.findViewById(R.id.row_duration);
                holder.distance = row.findViewById(R.id.row_distance);
                holder.distanceUnit = row.findViewById(R.id.row_distanceKM);
                holder.status = row.findViewById(R.id.statusBtn);
                holder.btnDelete = row.findViewById(R.id.deleteBtn);
                row.setTag(holder);
            } else {
                holder = (Holder) row.getTag();
            }
            String[] itemComponents = stringList.get(position).split(";");
            holder.rideDate.setText(itemComponents[1].split(",")[0]);
            holder.rideTime.setText(itemComponents[1].split(",")[1]);
            // holder.message.setText(itemComponents[2]);
            Log.d(TAG, "itemComponents: " + Arrays.toString(itemComponents));

            if (itemComponents[2].contains(getString(R.string.rideAnnotatedInHistoryActivity))) {
                holder.status.setBackground(getDrawable(R.drawable.ic_phone_android_black_24dp));
            } else if (itemComponents[2].contains(getString(R.string.rideUploadedInHistoryActivity))) {
                holder.status.setBackground(getDrawable(R.drawable.ic_cloud_done_black_24dp));
            } else {
                holder.status.setBackground(null);
            }
            holder.duration.setText(itemComponents[3]);
            if (SharedPref.Settings.DisplayUnit.isImperial(HistoryActivity.this)) {
                holder.distance.setText(String.valueOf(Math.round(((Double.parseDouble(itemComponents[5]) / 1600) * 100.0)) / 100.0));
                holder.distanceUnit.setText("mi");
            } else {
                holder.distance.setText(String.valueOf(Math.round(((Double.parseDouble(itemComponents[5]) / 1000) * 100.0)) / 100.0));
                holder.distanceUnit.setText("km");
            }
            if (!itemComponents[4].equals("2")) {
                holder.btnDelete.setVisibility(View.VISIBLE);
            } else {
                holder.btnDelete.setVisibility(View.INVISIBLE);
            }
            row.setOnClickListener(v -> {
                // gets the files in the directory
                // lists all the files into an array
                File[] dirFiles = new File(IOUtils.Directories.getBaseFolderPath(context)).listFiles();
                String clicked = (String) binding.listView.getItemAtPosition(position);
                Log.d(TAG, "dirFiles.length: " + dirFiles.length + " clicked: " + clicked + " position: " + position);
                clicked = clicked.replace("#", "").split(";")[0];
                if (dirFiles.length != 0) {
                    // loops through the array of files, outputting the name to console
                    for (File dirFile : dirFiles) {
                        String fileOutput = dirFile.getName();
                        Log.d(TAG, "fileOutput: " + fileOutput + " clicked: " + clicked + "_");
                        if (fileOutput.startsWith(clicked + "_")&&(fileOutput.endsWith("csv"))) {
                            ShowRouteActivity.startShowRouteActivity(Integer.parseInt(fileOutput.split("_", -1)[0]), Integer.parseInt(metaDataLines.get(metaDataLines.size() - position - 1)[3]), true, HistoryActivity.this);
                            break;
                        }
                    }
                }
            });
            row.setOnLongClickListener(v -> {
                con = context;
                String clicked = (String) binding.listView.getItemAtPosition(position);
                clicked = clicked.replace("#", "").split(";")[0];
                rideId = Integer.parseInt(clicked);
                registerForContextMenu(v);
                return false;
            });


            holder.btnDelete.setOnClickListener(v -> {
                Log.d(TAG, "Delete Button Clicked");
                fireDeletePrompt(position, MyArrayAdapter.this);
            });
            return row;
        }



        class Holder {
            TextView rideDate;
            TextView rideTime;
            TextView duration;
            TextView distance;
            TextView distanceUnit;
            ImageButton status;
            ImageButton btnDelete;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.clear();
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.exportfile_menu, menu);
    }


    public boolean onContextItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.csv_export:
                exportFile("text/csv","export csv file",0);
                return true;

            case R.id.geojson_export:
                makeGeoJsonFile();
                exportFile("text/geojson","export geojson file",1);
                return true;

            case R.id.kml_export:
                makeKMLFile();
                exportFile("text/kml", "export kml file", 2);
                return true;

            case R.id.gpx_export:
                makeGPXFile();
                exportFile("text/gpx","export gpx file",3);
                return true;
            default:
                return super.onContextItemSelected(item);
        }

    }

    //Creates GeoJson File to export
    public void makeGeoJsonFile(){
        JSONObject featureCollection = new JSONObject();
        try {
            DataLog allLogs = DataLog.loadDataLog(rideId, con);
            List<DataLogEntry> entries = allLogs.onlyGPSDataLogEntries;
            featureCollection.put("type", "FeatureCollection");
            JSONArray features = new JSONArray();
            for (DataLogEntry entry : entries) {
                JSONObject feature = new JSONObject();
                JSONArray JSONArrayCoord = new JSONArray();
                JSONObject geometry = new JSONObject();
                JSONObject Properties = new JSONObject();
                Date d = new Date(entry.timestamp);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                Properties.put("time", sdf.format(d));
                JSONArrayCoord.put(0, entry.longitude);
                JSONArrayCoord.put(1, entry.latitude);
                geometry.put("type", "Point");
                geometry.put("coordinates", JSONArrayCoord);
                feature.put("geometry", geometry);
                feature.put("properties", Properties);
                feature.put("type", "Feature");
                features.put(feature);
            }
            featureCollection.put("features", features);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        overwriteFile(featureCollection.toString(), IOUtils.Files.getGPSLogGeoJsonFile(rideId, false, this));
        }

    //Creates GPX File to export
    public void makeGPXFile(){
        final GPX.Builder gpxBuild;
        final GPX gpx;
        DataLog allLogs = DataLog.loadDataLog(rideId, con);
        List<DataLogEntry> entries = allLogs.onlyGPSDataLogEntries;
        List<WayPoint> Points = new ArrayList<>();
        for (DataLogEntry entry : entries) {
            Points.add(WayPoint.of(entry.latitude,entry.longitude,entry.timestamp));
        }
        gpxBuild = GPX.builder().addTrack(Track.builder().addSegment(TrackSegment.of(Points)).build());
        for(WayPoint Point: Points){
            gpxBuild.addWayPoint(Point);
        }
        gpx = gpxBuild.build();
        try {
            GPX.write(gpx, IOUtils.Files.getGPSLogGPXFile(rideId, false, this).toString());
        }catch(IOException e){
        }
    }
    //Creates KML File to export
    public void makeKMLFile(){

        DataLog allLogs = DataLog.loadDataLog(rideId, con);
        List<DataLogEntry> entries = allLogs.onlyGPSDataLogEntries;
        Document doc;
        Element kml;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.newDocument();
            kml = doc.createElementNS("http://www.opengis.net/kml/2.2", "kml");
            doc.appendChild(kml);
            Element root = doc.createElement("Document");
            kml.appendChild(root);
            Element placemark_path = doc.createElement("Placemark");
            root.appendChild(placemark_path);
            Element lineString = doc.createElement("LineString");
            placemark_path.appendChild(lineString);
            Element extrude = doc.createElement("extrude");
            extrude.appendChild(doc.createTextNode("1"));
            lineString.appendChild(extrude);
            Element tesselate = doc.createElement("tesselate");
            tesselate.appendChild(doc.createTextNode("1"));
            lineString.appendChild(tesselate);
            Element coords = doc.createElement("coordinates");
            String points = "";
            for(DataLogEntry entry:entries) {
                points += entry.longitude + "," + entry.latitude+"\n";
                Element placemark_point = doc.createElement("Placemark");
                root.appendChild(placemark_point);
                Element point = doc.createElement("Point");
                placemark_point.appendChild(point);
                Element Point_cord = doc.createElement("coordinates");
                Point_cord.appendChild(doc.createTextNode(entry.longitude + "," + entry.latitude));
                point.appendChild(Point_cord);
                Element TimeStamp = doc.createElement("TimeStamp");
                placemark_point.appendChild(TimeStamp);
                Element When = doc.createElement("When");
                TimeStamp.appendChild(When);
                Date d = new Date(entry.timestamp);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                When.appendChild(doc.createTextNode(sdf.format(d)));
            }
            coords.appendChild(doc.createTextNode(points));
            lineString.appendChild(coords);
            TransformerFactory factor = TransformerFactory.newInstance();
            Transformer transformer = factor.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource src = new DOMSource(doc);
            StreamResult out = new StreamResult(IOUtils.Files.getGPSLogKMLFile(rideId, false, this));
            transformer.transform(src, out);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Find the correct URI path for the desired file type
    public void findURI(int option){
        File[] dirFiles = new File(IOUtils.Directories.getBaseFolderPath(con)).listFiles();;
        if (dirFiles.length != 0) {
            for (File dirFile : dirFiles) {
                String fileOutput = dirFile.getName();
                if (fileOutput.startsWith(rideId + "_")&&(fileOutput.endsWith("csv"))&&(option == 0)) {
                    UriPath =  FileProvider.getUriForFile(con,"de.tuberlin.mcc.simra.app.fileprovider",dirFile);
                    return;
                }
                if(fileOutput.startsWith(rideId + "_")&&(fileOutput.endsWith("geojson"))&&(option == 1)) {
                     UriPath = FileProvider.getUriForFile(con,"de.tuberlin.mcc.simra.app.fileprovider",dirFile);
                     return;
                }
                if(fileOutput.startsWith(rideId + "_")&&(fileOutput.endsWith("kml"))&&(option == 2)) {
                    UriPath = FileProvider.getUriForFile(con, "de.tuberlin.mcc.simra.app.fileprovider", dirFile);
                    return;
                }
                if(fileOutput.startsWith(rideId + "_")&&(fileOutput.endsWith("gpx"))&&(option == 3)) {
                    UriPath = FileProvider.getUriForFile(con, "de.tuberlin.mcc.simra.app.fileprovider", dirFile);
                    return;
                }
            }
        }
    }

    //Exports the File
    public void exportFile(String type,String title, int option){
        findURI(option);
        Intent fileIntent = new Intent(Intent.ACTION_SEND);
        fileIntent.setType(type);
        fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        fileIntent.putExtra(Intent.EXTRA_STREAM,UriPath);
        Intent chooser = Intent.createChooser(fileIntent, "Share File");
        List<ResolveInfo> resInfoList = this.getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            this.grantUriPermission(packageName, UriPath, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        startActivity(chooser);
    }
}











