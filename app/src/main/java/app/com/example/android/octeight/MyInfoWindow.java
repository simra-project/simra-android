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

    private ShowRouteActivity motherActivity;

    private String addressForLoc;

    private String rideID;

    private String incidentKey;

    private boolean temp;



    public MyInfoWindow(int layoutResId, MapView mapView, AccEvent mAccEvent,
                        String addressForLoc, ShowRouteActivity motherActivity,
                        int key, boolean temp) {
        super(layoutResId, mapView);
        this.mAccEvent = mAccEvent;
        this.addressForLoc = addressForLoc;
        this.motherActivity = motherActivity;
        if (temp) {
            this.rideID = motherActivity.tempRide.getId();
        } else {
            this.rideID = motherActivity.ride.getId();
        }
        this.incidentKey = String.valueOf(key);
        this.temp = temp;
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
        if (millis > 1337) {
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

            popUpIntent.putExtra("Incident_bike",
                    String.valueOf(motherActivity.ride.bike));

            popUpIntent.putExtra("Incident_child",
                    String.valueOf(motherActivity.ride.child));

            popUpIntent.putExtra("Incident_trailer",
                    String.valueOf(motherActivity.ride.trailer));

            popUpIntent.putExtra("Incident_pLoc",
                    String.valueOf(motherActivity.ride.pLoc));

            //popUpIntent.putExtra("Incident_accDat",
            //        String.valueOf(this.mAccEvent.sensorData.getAbsolutePath()));

            popUpIntent.putExtra("Incident_accDat",
                    "mockSensorDatForIncident.csv");

            popUpIntent.putExtra("Ride_ID",
                    this.rideID);

            popUpIntent.putExtra("Incident_Key", this.incidentKey);

            popUpIntent.putExtra("Incident_temp", this.temp);

            motherActivity.startActivityForResult(popUpIntent, 1);

        });
    }
}