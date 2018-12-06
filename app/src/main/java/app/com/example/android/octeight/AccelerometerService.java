package app.com.example.android.octeight;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class AccelerometerService extends Service implements SensorEventListener {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Properties
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public Sensor myAccSensor;

    public SensorManager accSensorManager;

    private Context ctx;

    public ArrayList<Float> xList = new ArrayList<>();
    public ArrayList<Float> yList = new ArrayList<>();
    public ArrayList<Float> zList = new ArrayList<>();

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public void onSensorChanged(SensorEvent event) {

        // The accelerometer returns 3 values, one for each axis.
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // Add the accelerometer data to the respective ArrayLists.
        xList.add(x);
        yList.add(y);
        zList.add(z);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public AccelerometerService(Context ctx) {

        this.accSensorManager = (SensorManager) getSystemService(ctx.SENSOR_SERVICE);
        this.myAccSensor = this.accSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

    }

    public void saveRouteData() throws IOException {

        /**String xString = myAccService.xList.toString();
         create(this, "x_accelerometer.csv", xString);

         String yString = myAccService.yList.toString();
         create(this, "y_accelerometer.csv", yString);

         String zString = myAccService.zList.toString();
         create(this, "z_accelerometer.csv", zString);*/

        FileOutputStream fos = ctx.openFileOutput("accData.csv", Context.MODE_PRIVATE);

        try (OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            CSVPrinter csvPrinter = new CSVPrinter(osw, CSVFormat.DEFAULT.withHeader("x-axis", "y-axis",
                    "z-axis"));
            for(int i = 0; i < xList.size(); i++) {
                csvPrinter.printRecord(xList.get(i),
                        yList.get(i),
                        zList.get(i));

            }
            csvPrinter.flush();
            csvPrinter.close();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
