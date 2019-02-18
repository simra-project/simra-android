package app.com.example.android.octeight;

import android.app.Activity;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

class MyInfoWindow extends InfoWindow {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Log tag
    private static final String TAG = "MyInfoWindow_LOG";

    private AccEvent mAccEvent;

    private Activity motherActivity;

    private String addressForLoc;

    private String rideID;

    private String incidentKey;

    public MyInfoWindow(int layoutResId, MapView mapView, AccEvent mAccEvent,
                        String addressForLoc, Activity motherActivity, String rideID,
                        int key) {
        super(layoutResId, mapView);
        this.mAccEvent = mAccEvent;
        this.addressForLoc = addressForLoc;
        this.motherActivity = motherActivity;
        this.rideID = rideID;
        this.incidentKey = String.valueOf(key);
    }
    public void onClose() {
    }

    public void onOpen(Object arg0) {
        LinearLayout layout = (LinearLayout) mView.findViewById(R.id.bubble_layout);
        Button btnMoreInfo = (Button) mView.findViewById(R.id.bubble_moreinfo);
        TextView txtTitle = (TextView) mView.findViewById(R.id.bubble_title);
        TextView txtDescription = (TextView) mView.findViewById(R.id.bubble_description);
        TextView txtSubdescription = (TextView) mView.findViewById(R.id.bubble_subdescription);

        txtTitle.setText(motherActivity.getString(R.string.incidentDetected));
        long millis = this.mAccEvent.getTimeStamp();
        String time = "";
        if (millis > 1337){
            time = DateUtils.formatDateTime(motherActivity, millis, DateUtils.FORMAT_SHOW_TIME);
        }

        txtDescription.setText(time + " " + addressForLoc);
        txtSubdescription.setText("You can also edit the subdescription");

        layout.setOnClickListener((View v) -> {

            Intent popUpIntent = new Intent(motherActivity,
                    IncidentPopUpActivity.class);

            popUpIntent.putExtra("Incident_latitude",
                    String.valueOf(this.mAccEvent.position.getLatitude()));

            popUpIntent.putExtra("Incident_longitude",
                    String.valueOf(this.mAccEvent.position.getLongitude()));

            popUpIntent.putExtra("Incident_timeStamp",
                    String.valueOf(this.mAccEvent.getTimeStamp()));

            //popUpIntent.putExtra("Incident_accDat",
            //        String.valueOf(this.mAccEvent.sensorData.getAbsolutePath()));

            popUpIntent.putExtra("Incident_accDat",
                     "mockSensorDatForIncident.csv");

            popUpIntent.putExtra("Ride_ID",
                    this.rideID);

            popUpIntent.putExtra("Incident_Key", this.incidentKey);

            motherActivity.startActivityForResult(popUpIntent, 1);

        });
    }
}