package de.tuberlin.mcc.simra.app.activities;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.databinding.ActivitySettingsBinding;
import de.tuberlin.mcc.simra.app.services.DebugUploadService;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import de.tuberlin.mcc.simra.app.util.ConnectionManager;
import de.tuberlin.mcc.simra.app.util.IOUtils;
import de.tuberlin.mcc.simra.app.util.SharedPref;
import de.tuberlin.mcc.simra.app.util.UnitHelper;

import static de.tuberlin.mcc.simra.app.util.IOUtils.Directories.getBaseFolderPath;
import static de.tuberlin.mcc.simra.app.util.IOUtils.importSimRaData;
import static de.tuberlin.mcc.simra.app.util.IOUtils.zipTo;
import static de.tuberlin.mcc.simra.app.util.PermissionHelper.REQUEST_ENABLE_BT;
import static de.tuberlin.mcc.simra.app.util.PermissionHelper.requestBlePermissions;
import static de.tuberlin.mcc.simra.app.util.Utils.activityResultLauncher;
import static de.tuberlin.mcc.simra.app.util.Utils.prepareDebugZip;
import static de.tuberlin.mcc.simra.app.util.Utils.showBluetoothNotEnableWarning;
import static de.tuberlin.mcc.simra.app.util.Utils.sortFileListLastModified;


public class SettingsActivity extends BaseActivity {

    private static final String TAG = "SettingsActivity_LOG";
    private final static int DIRECTORY_PICKER_EXPORT = 9999;
    private static final int FILE_PICKER_IMPORT = 8778;

    BroadcastReceiver br;
    ActivitySettingsBinding binding;

    private ActivityResultLauncher<Intent> activityResultLauncher;

    boolean obsLiteEnabled = false;
    UsbManager usbManager;
    UsbDeviceConnection obsLiteConnection;
    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){

                        }
                    }
                    else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadActivity();
    }

    private void loadActivity() {
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
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.importPromptTitle);
                builder.setMessage(R.string.importButtonText);
                builder.setPositiveButton(R.string.continueText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                        chooseFile.setType("application/zip");
                        chooseFile = Intent.createChooser(chooseFile, getString(R.string.importFile));
                        startActivityForResult(chooseFile, FILE_PICKER_IMPORT);
                        importZipFromLocation.launch(new String[]{"application/zip"});
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                builder.show();
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
                        exportZipToLocation.launch(Uri.parse(DocumentsContract.PROVIDER_INTERFACE));
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                builder.show();
            }
        });

        // Switch: OpenBikeSensor device enabled
        boolean obsActivated = SharedPref.Settings.OpenBikeSensor.isEnabled(this);
        binding.obsButton.setVisibility(obsActivated ? View.VISIBLE : View.GONE);
        activityResultLauncher = activityResultLauncher(SettingsActivity.this);
        binding.obsButton.setOnClickListener(view ->
        {
            requestBlePermissions(SettingsActivity.this, REQUEST_ENABLE_BT);
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                // Device does not support Bluetooth
                Toast.makeText(SettingsActivity.this, R.string.openbikesensor_bluetooth_incompatible, Toast.LENGTH_LONG)
                        .show();
            } else if (!mBluetoothAdapter.isEnabled()) {
                // Bluetooth is disabled
                showBluetoothNotEnableWarning(activityResultLauncher,SettingsActivity.this);
            } else {
                startActivity(new Intent(this, OpenBikeSensorActivity.class));
            }
        });

        if (BluetoothAdapter.getDefaultAdapter() == null) {
            // Device does not support Bluetooth
            binding.obsSwitch.setEnabled(false);
        }
        binding.obsSwitch.setChecked(obsActivated);
        binding.obsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPref.Settings.OpenBikeSensor.setEnabled(isChecked, SettingsActivity.this);
            binding.obsButton.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked && ConnectionManager.INSTANCE.getBleState() == ConnectionManager.BLESTATE.CONNECTED) {
                ConnectionManager.INSTANCE.disconnect(ConnectionManager.INSTANCE.getScanResult().getDevice());
            }
        });

        // Switch: OBS Lute device enabled
        boolean obsLiteActivated = SharedPref.Settings.OBSLite.isEnabled(this);
        binding.obsLiteButton.setVisibility(obsLiteActivated ? View.VISIBLE : View.GONE);
        activityResultLauncher = activityResultLauncher(SettingsActivity.this);
        binding.obsLiteButton.setOnClickListener(view ->
        { startActivity(new Intent(this, OBSLiteActivity.class)); });


        binding.obsLiteSwitch.setChecked(obsLiteActivated);
        binding.obsLiteSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPref.Settings.OBSLite.setEnabled(isChecked, SettingsActivity.this);
            binding.obsLiteButton.setVisibility(isChecked ? View.VISIBLE : View.GONE);
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
        br = new MyBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("de.tuberlin.mcc.simra.app.UPLOAD_COMPLETE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.registerReceiver(br, filter, RECEIVER_EXPORTED);
        } else {
            this.registerReceiver(br, filter);
        }
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
        Log.d(TAG, "fireDebugPrompt - files: " + files);
        double sizeAllInMB = 0;
        double size10InMB = 0;
        int i10 = 0;
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            Log.d(TAG, "fireDebugPrompt - file: " + file);
            try {
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
            } catch (Exception e) {
                Log.e(TAG, "fireDebugPrompt() - Exception: " + e.getMessage());
                Log.e(TAG, Arrays.toString(e.getStackTrace()));
            }

        }
        Log.d(TAG, "fireDebugPrompt - ridesAndAccEvents: " + ridesAndAccEvents);
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
                Intent intent = new Intent(SettingsActivity.this, DebugUploadService.class);
                startService(intent);
                // delete zip.zip after upload is finished
                new File(IOUtils.Directories.getBaseFolderPath(SettingsActivity.this) + "zip.zip").deleteOnExit();
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

    private final ActivityResultLauncher<Uri> exportZipToLocation =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(),
                    new ActivityResultCallback<Uri>() {
                        @Override
                        public void onActivityResult(Uri uri) {
                            boolean successfullyExported = zipTo(SettingsActivity.this.getFilesDir().getParent(),uri,SettingsActivity.this);
                            if (successfullyExported) {
                                Toast.makeText(SettingsActivity.this, R.string.exportSuccessToast, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(SettingsActivity.this, R.string.exportFailToast, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
    private final ActivityResultLauncher<String[]> importZipFromLocation =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    new ActivityResultCallback<Uri>() {
                        @Override
                        public void onActivityResult(Uri uri) {
                            boolean successfullyImported = importSimRaData(uri, SettingsActivity.this);
                            if (successfullyImported) {
                                Toast.makeText(SettingsActivity.this, R.string.importSuccess, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(SettingsActivity.this, R.string.importFail, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

    private void tryConnectOBSLite() {
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (availableDrivers.size() > 0) {
            UsbSerialDriver driver = availableDrivers.get(0);
            obsLiteConnection = usbManager.openDevice(driver.getDevice());

            if (obsLiteConnection == null) {
                IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    this.registerReceiver(usbReceiver, filter, RECEIVER_EXPORTED);
                } else {
                    this.registerReceiver(usbReceiver, filter);
                }
                PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
                usbManager.requestPermission(driver.getDevice(), permissionIntent);
            }
        }
    }
}
