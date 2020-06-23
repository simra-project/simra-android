package de.tuberlin.mcc.simra.app.annotation;

import android.content.Intent;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.entities.AccEvent;

public class MyInfoWindow extends InfoWindow {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Log tag
    private static final String TAG = "MyInfoWindow_LOG";

    private AccEvent mAccEvent;
    private ShowRouteActivity motherActivity;
    private String addressForLoc;
    private String rideID;
    private String accEventKey;
    private boolean temp;
    private int state;


    public MyInfoWindow(int layoutResId, MapView mapView, AccEvent mAccEvent,
                        String addressForLoc, ShowRouteActivity motherActivity,
                        int accEventKey, boolean temp, int state) {
        super(layoutResId, mapView);
        this.mAccEvent = mAccEvent;
        this.addressForLoc = addressForLoc;
        this.motherActivity = motherActivity;
        if (temp) {
            this.rideID = motherActivity.tempRide.getKey();
        } else {
            this.rideID = motherActivity.ride.getKey();
        }
        this.accEventKey = String.valueOf(accEventKey);
        this.temp = temp;
        this.state = state;
    }

    public void onClose() {
    }

    public void onOpen(Object arg0) {
        LinearLayout layout = mView.findViewById(R.id.bubble_layout);
        Button btnMoreInfo = mView.findViewById(R.id.bubble_moreinfo);
        TextView txtTitle = mView.findViewById(R.id.bubble_title);
        TextView txtDescription = mView.findViewById(R.id.bubble_description);
        TextView txtSubdescription = mView.findViewById(R.id.bubble_subdescription);

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

                // TODO: What is this?                    
            popUpIntent.putExtra("Incident_accDat",
                    "mockSensorDatForIncident.csv");

            popUpIntent.putExtra("Ride_ID",
                    this.rideID);

            popUpIntent.putExtra("Incident_Key", this.accEventKey);

            popUpIntent.putExtra("Incident_temp", this.temp);

            popUpIntent.putExtra("State", state);

            motherActivity.startActivityForResult(popUpIntent, 1);

        });
    }
}