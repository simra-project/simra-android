package de.tuberlin.mcc.simra.app.activities;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
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
import de.tuberlin.mcc.simra.app.entities.DataLogEntry;
import de.tuberlin.mcc.simra.app.entities.RideImpl;
import de.tuberlin.mcc.simra.app.util.IOUtils;

public class EvaluateClosePassActivity extends AppCompatActivity {
    private static final String TAG = "EvaluateClosePass";

    ImageButton backBtn;
    TextView toolbarTxt;

    ImageView closePassPicture;
    TextView currentDistanceValue;

    List<File> imageQueue;
    RideImpl ride;

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

        String fileName = getIntent().getStringExtra("PathToAccGpsFile").split("_")[0];
        ride = RideImpl.loadRideById(Integer.parseInt(fileName), this);
        Log.i("TEST", "" + ride.rideID);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ride.dataPoints.forEach(a -> {
                Log.i("Test", a.timestamp + "");
            });
        }


        MaterialButton closePassConfirmButton = findViewById(R.id.closePassConfirmButton);
        closePassConfirmButton.setOnClickListener(view -> {
            // TODO: Add incident
            removePicture();
        });
        MaterialButton closePassDisapproveButton = findViewById(R.id.closePassDisapproveButton);
        closePassDisapproveButton.setOnClickListener(view -> {
            removePicture();
        });

        imageQueue = new LinkedList<>(Arrays.asList(new File(IOUtils.Directories.getPictureCacheDirectoryPath()).listFiles()));
        updateView();
    }

    public void updateView() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        closePassPicture.setImageBitmap(decodeSampledBitmapFromFile(imageQueue.get(0).getAbsolutePath(), displayMetrics.widthPixels, displayMetrics.heightPixels));
        for (DataLogEntry d : ride.dataPoints) {
            if (d.timestamp == Long.parseLong(imageQueue.get(0).getName().split("\\.")[0])) {
                currentDistanceValue.setText(String.valueOf(((DataLogEntry) d).RadmesserDistanceLeft1));
            }
        }

    }

    public void removePicture() {
        imageQueue.remove(0);
        if (imageQueue.isEmpty()) {
            IOUtils.deleteDirectoryContent(IOUtils.Directories.getPictureCacheDirectoryPath());
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
