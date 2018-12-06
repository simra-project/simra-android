package app.com.example.android.octeight;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class AccelerometerFunct implements SensorEventListener {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Properties
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public Context ctx;

    public Sensor myAccSensor;

    public SensorManager accSensorManager;

    boolean recording;

    public ArrayList<Float> xList = new ArrayList<>();
    public ArrayList<Float> yList = new ArrayList<>();
    public ArrayList<Float> zList = new ArrayList<>();

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public void onSensorChanged(SensorEvent event) {

        if(recording) {

            // The accelerometer returns 3 values, one for each axis.
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Add the accelerometer data to the respective ArrayLists.
            xList.add(x);
            yList.add(y);
            zList.add(z);

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public AccelerometerFunct(Context ctx) {
        this.ctx = ctx;
        this.accSensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        this.myAccSensor = this.accSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void register(){
        accSensorManager.registerListener(this, myAccSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void unregister(){
        accSensorManager.unregisterListener(this);
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

}
