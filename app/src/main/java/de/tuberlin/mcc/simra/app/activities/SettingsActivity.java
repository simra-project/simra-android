package de.tuberlin.mcc.simra.app.activities;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.databinding.ActivitySettingsBinding;
import de.tuberlin.mcc.simra.app.services.OBSService;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import de.tuberlin.mcc.simra.app.util.SharedPref;
import de.tuberlin.mcc.simra.app.util.UnitHelper;
import pl.droidsonroids.gif.GifImageView;

import static de.tuberlin.mcc.simra.app.util.IOUtils.Directories.getBaseFolderPath;
import static de.tuberlin.mcc.simra.app.util.IOUtils.Directories.getSharedPrefsDirectory;
import static de.tuberlin.mcc.simra.app.util.IOUtils.zip;
import static de.tuberlin.mcc.simra.app.util.IOUtils.zipto;

import static de.tuberlin.mcc.simra.app.util.Utils.prepareDebugZip;
import static de.tuberlin.mcc.simra.app.util.Utils.sortFileListLastModified;


public class SettingsActivity extends BaseActivity {

    private static final String TAG = "SettingsActivity_LOG";
    private final static int REQUEST_ENABLE_BT = 1;
    private final static int BLUETOOTH_SUCCESS = -1;

    //public static final int REQUEST_CODE = 1;
    //private static final int CHOOSE_FILE_REQUESTCODE = 8777;
    private static final int PICKFILE_RESULT_CODE = 8778;
    BroadcastReceiver br;
    ActivitySettingsBinding binding;
    List<File> ridesArr = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySettingsBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        // Setup
        setSupportActionBar(binding.toolbar.toolbar);
        try {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        } catch (NullPointerException ignored) {
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
                    SharedPref.Settings.IncidentsButtonsDuringRide.setIncidentButtonsEnabled(isChecked, this);
                    if (isChecked) {
                        fireIncidentButtonsEnablePrompt();
                    }
                });

        // Switch: AI Select
        if (SharedPref.Settings.IncidentGenerationAIActive.getAIEnabled(this)) {
            binding.switchAI.setChecked(true);
        }
        binding.switchAI.setOnCheckedChangeListener((buttonView, isChecked) -> SharedPref.Settings.IncidentGenerationAIActive.setAIEnabled(isChecked, this));


        binding.importButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {


                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                Log.d(TAG,"Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT");
                chooseFile.setType("*/*");
                Log.d(TAG,"chooseFile.setType(*/*)");
                chooseFile = Intent.createChooser(chooseFile, "Choose a file");
                Log.d(TAG,"chooseFile = Intent.createChooser(chooseFile, \"Choose a file\");");
                startActivityForResult(chooseFile, PICKFILE_RESULT_CODE);
                Log.d(TAG, "startActivityForResult(chooseFile, PICKFILE_RESULT_CODE);");

                // wrong destination, need to be @Override  protected void
                // onActivityResult(PICKFILE_RESULT_CODE, REQUEST_CODE,chooseFile);

                Log.d(TAG, "onActivityResult(PICKFILE_RESULT_CODE, REQUEST_CODE,chooseFile);");

                Log.d(TAG, "before - chooseFile.getData().getPath()" + chooseFile.getData().getPath());

             //   Uri uri = chooseFile.getData();



                int i= 0;
                Uri uri = null;

                while (uri == null){
                    i+=1;
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            // Actions to do after delay
                            Log.d(TAG, "waited enough");

                        }
                    }, 1000);
                    uri = chooseFile.getData();
                    Log.d(TAG, i + ". chooseFile.getData()" + uri);
                }
                if(uri == null){
                    String src = uri.getPath();
                    Log.d(TAG, "src = uri.getPath()" + src);
                }else{
                    Log.d(TAG, "else - chooseFile.getData()" + chooseFile.getData());
                    uri = chooseFile.getData();
                    Log.d(TAG, "else - uri = chooseFile.getData()" + uri);
                    Log.d(TAG, "else - uri.getPath()" + uri.getPath());
                    String src = uri.getPath();
                    Log.d(TAG, "else - src = uri.getPath()" + src);

                }




             /*   try {
                    IOUtils.unpackZip(SettingsActivity.this.getFilesDir().getParent() + "/" , "zip3.zip");
                    Log.d(TAG, getBaseFolderPath(SettingsActivity.this) + "/zip3.zip");

                    Log.d(TAG,"unpack 3");

                }catch (Exception e){
                    Log.d(TAG,"problem 2");

                } /*try {
                    IOUtils.unpackZip("/data/data/de.tuberlin.mcc.simra.app.mark/files/" ,"zip2.zip");
                    Log.d(TAG,"unpack 1");

                }catch (Exception e){
                    Log.d(TAG,"problem 1");

                }*/
                Log.d(TAG, "import button last line");

            }
        });

        binding.exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.exportPromptTitle);
                builder.setMessage(R.string.exportButtonText);
                builder.setPositiveButton(R.string.continueText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //IOUtils.zipFolder("/data/data/de.tuberlin.mcc.simra.app.mark/shared_prefs","/data/data/de.tuberlin.mcc.simra.app.mark/files");
                        //IOUtils.zipFolder("/data/data/de.tuberlin.mcc.simra.app.mark/files","/data/data/de.tuberlin.mcc.simra.app.mark/shared_prefs");
                        // non static approach
                        Log.d(TAG, SettingsActivity.this.getFilesDir().getParent());
                        zipto(SettingsActivity.this.getFilesDir().getParent(),getBaseFolderPath(SettingsActivity.this) + "/zip3.zip",SettingsActivity.this);
                        // zipto("/data/data/de.tuberlin.mcc.simra.app.mark/files","/data/data/de.tuberlin.mcc.simra.app.mark/shared_prefs");

                         // fireExportPrompt();
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                builder.show();
            }
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

        binding.appVersionTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.debugPromptTitle1);
                builder.setMessage(R.string.debugButtonText);
                builder.setPositiveButton(R.string.continueText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fireDebugPrompt();
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                builder.show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        br = new SettingsActivity.MyBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("de.tuberlin.mcc.simra.app.UPLOAD_COMPLETE");
        this.registerReceiver(br, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(br);
    }


    private void fireDebugPrompt() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.debugPromptTitle2);
        File[] dirFiles = new File(getBaseFolderPath(SettingsActivity.this)).listFiles();
        List<File> files = new ArrayList<File>(Arrays.asList(dirFiles));
        List<File> ridesAndAccEvents = new ArrayList<>();
        sortFileListLastModified(files);
        double sizeAllInMB = 0;
        double size10InMB = 0;
        int i10 = 0;
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            if (file.getName().contains("accGps")) {
                int id = Integer.parseInt(file.getName().split("_")[0]);
                String path = file.getParent() + File.separator + "accEvents" + id + ".csv";
                File accEvents = new File(path);
                sizeAllInMB += file.length() / 1024.0 / 1024.0;
                ridesAndAccEvents.add(file);
                if (accEvents.exists()) {
                    sizeAllInMB += accEvents.length() / 1024.0 / 1024.0;
                    ridesAndAccEvents.add(accEvents);
                }
                if (i10 < 10) {
                    size10InMB = sizeAllInMB;
                    i10++;
                }
            }
        }
        sizeAllInMB = Math.round(sizeAllInMB / 3.0 * 100.0) / 100.0;
        size10InMB = Math.round(size10InMB / 3.0 * 100.0) / 100.0;
        final int[] clicked = {2};
        CharSequence[] array;
        if (files.size() > 10) {
            array = new CharSequence[]{getText(R.string.debugSendAllRides) + " (" + sizeAllInMB + " MB)", getText(R.string.debugSend10Rides) + " (" + size10InMB + " MB)", getText(R.string.debugDoNotSendRides)};
        } else {
            array = new CharSequence[]{getText(R.string.debugSendAllRides) + " (" + sizeAllInMB + " MB)", getText(R.string.debugDoNotSendRides)};
        }
        builder.setSingleChoiceItems(array, 2, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                clicked[0] = which;
            }
        });
        builder.setPositiveButton(R.string.upload, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                prepareDebugZip(clicked[0], ridesAndAccEvents, SettingsActivity.this);
                //  Intent intent = new Intent(SettingsActivity.this, DebugUploadService.class);
                //  startService(intent);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
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
        alert.setNeutralButton("Ok", (dialog, id) -> {
        });
        alert.show();
    }


    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean uploadSuccessful = intent.getBooleanExtra("uploadSuccessful", false);
            if (!uploadSuccessful) {
                Toast.makeText(getApplicationContext(), R.string.upload_failed, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.upload_completed, Toast.LENGTH_LONG).show();
            }
        }
    }

    /*
    public String getPath(Uri uri) {

        String path = null;
        String[] projection = { MediaStore.Files.FileColumns.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if(cursor == null){
            path = uri.getPath();
        }
        else{
            cursor.moveToFirst();
            int column_index = cursor.getColumnIndexOrThrow(projection[0]);
            path = cursor.getString(column_index);
            cursor.close();
        }

        return ((path == null || path.isEmpty()) ? (uri.getPath()) : path);
    }*/

}
