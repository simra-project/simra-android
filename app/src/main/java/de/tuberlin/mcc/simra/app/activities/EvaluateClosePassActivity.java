package de.tuberlin.mcc.simra.app.activities;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.entities.DataLog;
import de.tuberlin.mcc.simra.app.entities.DataLogEntry;
import de.tuberlin.mcc.simra.app.entities.IncidentLog;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import de.tuberlin.mcc.simra.app.util.IOUtils;

public class EvaluateClosePassActivity extends AppCompatActivity {
    private static final String TAG = "EvaluateClosePass";
    private static final String EXTRA_RIDE_ID = "EXTRA_RIDE_ID";
    ImageButton backBtn;
    TextView toolbarTxt;
    ImageView closePassPicture;
    TextView currentDistanceValue;
    List<File> imageQueue;
    DataLogEntry currentDataLogEntry;
    IncidentLog incidentLog;
    DataLog dataLog;

    public static void startEvaluateClosePassActivity(int rideId, Context context) {
        Intent intent = new Intent(context, EvaluateClosePassActivity.class);
        intent.putExtra(EXTRA_RIDE_ID, rideId);
        context.startActivity(intent);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromFile(String path,
                                                     int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getIntent().hasExtra(EXTRA_RIDE_ID)) {
            throw new RuntimeException("Extra: " + EXTRA_RIDE_ID + " not defined.");
        }
        setContentView(R.layout.activity_evaluate_closepass);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        toolbarTxt = findViewById(R.id.toolbar_title);
        toolbarTxt.setText("Evaluate Close Passes");

        backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(v -> finish());

        closePassPicture = findViewById(R.id.closePassPicture);
        currentDistanceValue = findViewById(R.id.closePassCurrentValue);


        int rideId = getIntent().getIntExtra(EXTRA_RIDE_ID, 0);
        incidentLog = IncidentLog.loadIncidentLog(rideId, this);
        dataLog = DataLog.loadDataLog(rideId, this);


        MaterialButton closePassConfirmButton = findViewById(R.id.closePassConfirmButton);
        closePassConfirmButton.setOnClickListener(view -> {
            incidentLog.updateOrAddIncident(IncidentLogEntry.newBuilder()
                    .withBaseInformation(
                            currentDataLogEntry.timestamp,
                            currentDataLogEntry.latitude, currentDataLogEntry.longitude
                    )
                    .withIncidentType(IncidentLogEntry.INCIDENT_TYPE.CLOSE_PASS)
                    .build());
            removePicture(true);
        });
        MaterialButton closePassDisapproveButton = findViewById(R.id.closePassDisapproveButton);
        closePassDisapproveButton.setOnClickListener(view -> {
            removePicture(true);
        });

        imageQueue = new LinkedList<>(Arrays.asList(new File(IOUtils.Directories.getPictureCacheDirectoryPath()).listFiles()));
        updateView();
    }

    public void updateView() {
        boolean foundPicture = false;
        for (DataLogEntry d : dataLog.dataLogEntries) {
            if (d.timestamp == Long.parseLong(imageQueue.get(0).getName().split("\\.")[0])) {
                currentDistanceValue.setText(String.valueOf(((DataLogEntry) d).radmesserDistanceLeft1));
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                closePassPicture.setImageBitmap(decodeSampledBitmapFromFile(imageQueue.get(0).getAbsolutePath(), displayMetrics.widthPixels, displayMetrics.heightPixels));
                foundPicture = true;
            }
            // Find next DataLogEntry with GPS Location attached
            if (foundPicture == true && d.longitude != null) {
                currentDataLogEntry = d;
                break;
            }
        }
        // If Picture Timestamp does not match with ride timestamps it is not part of the ride.
        if (!foundPicture) {
            removePicture(false);
        }

    }

    public void removePicture(boolean delete) {
        if (delete) {
            imageQueue.get(0).delete();
        }
        imageQueue.remove(0);
        if (imageQueue.isEmpty()) {
            IncidentLog.saveIncidentLog(incidentLog, this);
            finish();
        } else {
            updateView();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() called");
    }
}
