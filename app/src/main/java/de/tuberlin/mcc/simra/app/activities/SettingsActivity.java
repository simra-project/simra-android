package de.tuberlin.mcc.simra.app.activities;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.databinding.ActivitySettingsBinding;
import de.tuberlin.mcc.simra.app.services.OBSService;
import de.tuberlin.mcc.simra.app.services.UploadService;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import de.tuberlin.mcc.simra.app.util.IOUtils;
import de.tuberlin.mcc.simra.app.util.SharedPref;
import de.tuberlin.mcc.simra.app.util.UnitHelper;
import pl.droidsonroids.gif.GifImageView;

import static de.tuberlin.mcc.simra.app.util.Constants.ZOOM_LEVEL;
import static de.tuberlin.mcc.simra.app.util.SharedPref.writeBooleanToSharedPrefs;

public class SettingsActivity extends BaseActivity {

    private static final String TAG = "SettingsActivity_LOG";
    private final static int REQUEST_ENABLE_BT = 1;
    private final static int BLUETOOTH_SUCCESS = -1;

    ActivitySettingsBinding binding;
    String[] ridesArr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySettingsBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        // Setup
        setSupportActionBar(binding.toolbar.toolbar);
        try {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        } catch (NullPointerException ignored){
            Log.d(TAG, "NullPointerException");
        }
        binding.toolbar.toolbar.setTitle("");
        binding.toolbar.toolbar.setSubtitle("");
        binding.toolbar.toolbarTitle.setText(R.string.title_activity_settings);
        binding.toolbar.backButton.setOnClickListener(v -> finish());

        // Slider: Duration
        binding.privacyDurationSlider.setValueFrom(SharedPref.Settings.Ride.PrivacyDuration.getMinDuration());
        binding.privacyDurationSlider.setValueTo(SharedPref.Settings.Ride.PrivacyDuration.getMaxDuration());
        binding.privacyDurationSlider.setValue(SharedPref.Settings.Ride.PrivacyDuration.getDuration(this));
        binding.privacyDurationTextLeft.setText(SharedPref.Settings.Ride.PrivacyDuration.getMinDuration() + getString(R.string.seconds_short));
        binding.privacyDurationTextRight.setText(SharedPref.Settings.Ride.PrivacyDuration.getMaxDuration() + getString(R.string.seconds_short));
        binding.privacyDurationSlider.addOnChangeListener((slider, changeListener, touchChangeListener) -> {
            SharedPref.Settings.Ride.PrivacyDuration.setDuration(Math.round(slider.getValue()), this);
        });

        // Slider: Distance
        updatePrivacyDistanceSlider(SharedPref.Settings.DisplayUnit.getDisplayUnit(this));
        binding.privacyDistanceSlider.addOnChangeListener((slider, changeListener, touchChangeListener) -> {
            SharedPref.Settings.Ride.PrivacyDistance.setDistance(Math.round(slider.getValue()), SharedPref.Settings.DisplayUnit.getDisplayUnit(this), this);
        });

        // Select Menu: Bike Type
        Spinner bikeTypeSpinner = findViewById(R.id.bikeTypeSpinner);
        bikeTypeSpinner.setSelection(SharedPref.Settings.Ride.BikeType.getBikeType(this));
        bikeTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPref.Settings.Ride.BikeType.setBikeType(position, SettingsActivity.this);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Select Menu: Phone Location
        Spinner phoneLocationSpinner = findViewById(R.id.locationTypeSpinner);
        phoneLocationSpinner.setSelection(SharedPref.Settings.Ride.PhoneLocation.getPhoneLocation(this));
        phoneLocationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPref.Settings.Ride.PhoneLocation.setPhoneLocation(position, SettingsActivity.this);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // CheckBox: Child on Bicycle
        CheckBox childOnBikeCheckBox = findViewById(R.id.childCheckBox);
        childOnBikeCheckBox.setChecked(SharedPref.Settings.Ride.ChildOnBoard.isChildOnBoard(this));
        childOnBikeCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPref.Settings.Ride.ChildOnBoard.setChildOnBoard(isChecked, this);
        });

        // CheckBox: Bicycle with Trailer
        CheckBox trailerCheckBox = findViewById(R.id.trailerCheckBox);
        trailerCheckBox.setChecked(SharedPref.Settings.Ride.BikeWithTrailer.hasTrailer(this));
        trailerCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPref.Settings.Ride.BikeWithTrailer.setTrailer(isChecked, this);
        });

        // Switch: Unit Select
        Switch unitSwitch = findViewById(R.id.unitSwitch);
        if (SharedPref.Settings.DisplayUnit.isImperial(this)) {
            unitSwitch.setChecked(true);
        }
        unitSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            UnitHelper.DISTANCE displayUnit = isChecked ? UnitHelper.DISTANCE.IMPERIAL : UnitHelper.DISTANCE.METRIC;
            SharedPref.Settings.DisplayUnit.setDisplayUnit(displayUnit, this);
            updatePrivacyDistanceSlider(displayUnit);
        });

        // Switch: Buttons for adding incidents during ride
        if (SharedPref.Settings.IncidentsButtonsDuringRide.getIncidentButtonsEnabled(this)) {
            binding.switchButtons.setChecked(true);
        }
        binding.switchButtons.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    SharedPref.Settings.IncidentsButtonsDuringRide.setIncidentButtonsEnabled(isChecked,this);
                    if (isChecked) {
                        fireIncidentButtonsEnablePrompt();
                    }
                });

        // Switch: AI Select
        if (SharedPref.Settings.IncidentGenerationAIActive.getAIEnabled(this)) {
            binding.switchAI.setChecked(true);
        }
        binding.switchAI.setOnCheckedChangeListener((buttonView, isChecked) -> SharedPref.Settings.IncidentGenerationAIActive.setAIEnabled(isChecked,this));


        // Data Export
        Button exportBtn = findViewById(R.id.exportButton);
        exportBtn.setOnClickListener(v -> {
            Log.d(TAG, "exportBtn clicked ");
            //String basePath = IOUtils.Directories.getBaseFolderPath(SettingsActivity.this);
            String basePath = IOUtils.Directories.getBaseFolderPath(getApplicationContext());
            Log.d(TAG, "basePath = " + basePath);
            // folder.listFiles();

            Log.d(TAG, "Fill String array");

            ridesArr = new String[5];

            ridesArr[0] = "/data/de.tuberlin.mcc.simra.app/files/0_accGps.csv";
            //ridesArr[0] = "/data/data/de.tuberlin.mcc.simra.app/files/0_accGps.csv";
            //ridesArr[0] = basePath + "/0_accGps.csv";
            ridesArr[1] = basePath + "/accEvents0.csv";
            ridesArr[2] = basePath + "/metaData.csv";
            ridesArr[3] = basePath + "/simRa_news_de.config";
            ridesArr[4] = basePath + "/simRa_regions.config";


            Log.d(TAG,  "String array filled");

            zip(ridesArr, IOUtils.Directories.getExternalBaseDirectoryPath() + "/zipFile/");
            });


        // Switch: OpenBikeSensor device enabled
        boolean obsActivated = SharedPref.Settings.OpenBikeSensor.isEnabled(this);
        binding.obsButton.setVisibility(obsActivated ? View.VISIBLE : View.GONE);
        binding.obsButton.setOnClickListener(view -> startActivity(new Intent(this, OpenBikeSensorActivity.class)));

        if (BluetoothAdapter.getDefaultAdapter() == null) {
            // Device does not support Bluetooth
            binding.obsSwitch.setEnabled(false);
        }
        binding.obsSwitch.setChecked(obsActivated);
        binding.obsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPref.Settings.OpenBikeSensor.setEnabled(isChecked, SettingsActivity.this);
            binding.obsButton.setVisibility(isChecked ? View.VISIBLE : View.GONE);

            if (isChecked) {
                if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                    enableBluetooth();
                } else {
                    startOBSService();
                }
            } else {
                OBSService.terminateService(this);
            }
        });


        // Version Text
        TextView appVersionTextView = findViewById(R.id.appVersionTextView);
        appVersionTextView.setText("Version: " + BuildConfig.VERSION_CODE + "(" + BuildConfig.VERSION_NAME + ")");

    }

    private void updatePrivacyDistanceSlider(UnitHelper.DISTANCE unit) {
        binding.privacyDistanceSlider.setValueFrom(SharedPref.Settings.Ride.PrivacyDistance.getMinDistance(unit));
        binding.privacyDistanceSlider.setValueTo(SharedPref.Settings.Ride.PrivacyDistance.getMaxDistance(unit));
        binding.privacyDistanceSlider.setValue(SharedPref.Settings.Ride.PrivacyDistance.getDistance(unit, this));
        binding.privacyDistanceTextLeft.setText(SharedPref.Settings.Ride.PrivacyDistance.getMinDistance(unit) + UnitHelper.getShortTranslationForUnit(unit, this));
        binding.privacyDistanceTextRight.setText(SharedPref.Settings.Ride.PrivacyDistance.getMaxDistance(unit) + UnitHelper.getShortTranslationForUnit(unit, this));
    }

    private void showTutorialDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.obsConnecting));
        alert.setMessage(getString(R.string.obsTutorial));

        LinearLayout gifLayout = new LinearLayout(this);
        LinearLayout.LayoutParams gifMargins = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        GifImageView gif = new GifImageView(this);
        gif.setImageResource(R.drawable.tutorial);
        gif.setVisibility(View.VISIBLE);
        gifMargins.setMargins(50, 0, 50, 0);
        gif.setLayoutParams(gifMargins);
        gifLayout.addView(gif);
        alert.setView(gifLayout);
        alert.setPositiveButton("Ok", (dialog, whichButton) -> {
        });
        alert.show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == BLUETOOTH_SUCCESS && requestCode == REQUEST_ENABLE_BT) {
            startOBSService();
            showTutorialDialog();
        } else {
            SharedPref.Settings.OpenBikeSensor.setEnabled(false, this);
            binding.obsSwitch.setChecked(false);
            OBSService.terminateService(this);
            binding.obsButton.setVisibility(View.GONE);
        }
    }

    private void startOBSService() {
        Intent intent = new Intent(this, OBSService.class);
        startService(intent);
    }

    private void enableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    public void fireIncidentButtonsEnablePrompt() {
        androidx.appcompat.app.AlertDialog.Builder alert = new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this);
        alert.setTitle(getString(R.string.warning));
        alert.setMessage(getString(R.string.incident_buttons_during_ride_warning));
        alert.setNeutralButton("Ok", (dialog, id) -> { });
        alert.show();
    }


    // comparable to refreshMyRides in HistoryActivity

    private void getMyRides() {
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
            Log.d(TAG, "getMyRides(): metaDataLines: " + Arrays.deepToString(metaDataLines.toArray()));
            for (int i = 0; i < metaDataLines.size(); i++) {
                String[] metaDataLine = metaDataLines.get(i);
                if (metaDataLine.length > 2 && !(metaDataLine[0].equals("key"))) {
                    //ridesArr[((metaDataLines.size()) - i) - 1] = listToTextShape(metaDataLine);
                }
            }

            Log.d(TAG, "ridesArr: " + Arrays.toString(ridesArr));
            List<String> stringArrayList = new ArrayList<>(Arrays.asList(ridesArr));


        } else {

            Log.d(TAG, "metaData.csv doesn't exists");

            Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator_layout), (getString(R.string.noHistory)), Snackbar.LENGTH_LONG);
            snackbar.show();

        }

    }

    // _files : Array of files to zip
    // Buffer can be used for limiting the buffer memory size while reading and writing data it to the zip stream

    public void zip(String[] _files, String zipFileName) {
        try {
            BufferedInputStream origin = null;
            int Buffer = 0;
            FileOutputStream dest = new FileOutputStream(zipFileName);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            byte data[] = new byte[Buffer];

            Log.v(TAG, "primarily for loop");

            for (int i = 0; i < _files.length; i++) {
                Log.v(TAG, "Adding: " + _files[i]);
                FileInputStream fi = new FileInputStream(_files[i]);
                Log.v(TAG, "InputStream: " + fi.equals(null));
                origin = new BufferedInputStream(fi, Buffer);
                Log.v(TAG, "origin: " + origin.equals(null));

                ZipEntry entry = new ZipEntry(_files[i].substring(_files[i].lastIndexOf("/") + 1));
                out.putNextEntry(entry);
                int count;

                while ((count = origin.read(data, 0, Buffer)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }

            out.close();
        } catch (Exception e) {
            String exce = e.toString();
            Log.d(TAG, "export catched:/n "+ exce);
            e.printStackTrace();
        }

    }
}
