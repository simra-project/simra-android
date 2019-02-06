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

import java.io.Serializable;

class MyInfoWindow extends InfoWindow {

    private AccEvent mAccEvent;

    private Activity motherActivity;

    private String addressForLoc;

    private Ride ride;

    public MyInfoWindow(int layoutResId, MapView mapView, AccEvent mAccEvent,
                        String addressForLoc, Activity motherActivity, Ride ride) {
        super(layoutResId, mapView);
        this.mAccEvent = mAccEvent;
        this.addressForLoc = addressForLoc;
        this.motherActivity = motherActivity;
        this.ride = ride;
    }
    public void onClose() {
    }

    public void onOpen(Object arg0) {
        LinearLayout layout = (LinearLayout) mView.findViewById(R.id.bubble_layout);
        Button btnMoreInfo = (Button) mView.findViewById(R.id.bubble_moreinfo);
        TextView txtTitle = (TextView) mView.findViewById(R.id.bubble_title);
        TextView txtDescription = (TextView) mView.findViewById(R.id.bubble_description);
        TextView txtSubdescription = (TextView) mView.findViewById(R.id.bubble_subdescription);

        txtTitle.setText(motherActivity.getString(R.string.incidentDetectedDE));
        long millis = Long.valueOf(this.mAccEvent.getTimeStamp());
        String time = DateUtils.formatDateTime(motherActivity, millis, DateUtils.FORMAT_SHOW_TIME);

        txtDescription.setText(time + " " + addressForLoc);
        txtSubdescription.setText("You can also edit the subdescription");

        layout.setOnClickListener((View v) -> {

            Intent popUpIntent = new Intent(motherActivity,
                    IncidentPopUpActivity.class);

            popUpIntent.putExtra("Incident_latitude",
                    (Serializable) String.valueOf(this.mAccEvent.position.getLatitude()));

            popUpIntent.putExtra("Incident_longitude",
                    (Serializable) String.valueOf(this.mAccEvent.position.getLongitude()));

            // Log.d(TAG, "this.mAccEvent.date: " + this.mAccEvent.date);
            popUpIntent.putExtra("Incident_date",
                    (Serializable) String.valueOf(this.mAccEvent.getTimeStamp()));

            //popUpIntent.putExtra("Incident_accDat",
            //        (Serializable) String.valueOf(this.mAccEvent.sensorData.getAbsolutePath()));

            popUpIntent.putExtra("Incident_accDat",
                    (Serializable) "mockSensorDatForIncident.csv");

            motherActivity.startActivity(popUpIntent);

        });
    }
}