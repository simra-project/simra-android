package de.tuberlin.mcc.simra.app.annotation;

import android.text.format.DateUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.activities.ShowRouteActivity;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;

public class MyInfoWindow extends InfoWindow {
    private ShowRouteActivity motherActivity;
    private String addressForLoc;
    private int state;
    private IncidentLogEntry incidentLogEntry;


    public MyInfoWindow(int layoutResId, MapView mapView,
                        String addressForLoc, ShowRouteActivity motherActivity,
                        int state, IncidentLogEntry incidentLogEntry) {

        super(layoutResId, mapView);
        this.addressForLoc = addressForLoc;
        this.motherActivity = motherActivity;
        this.incidentLogEntry = incidentLogEntry;
        this.state = state;
    }

    public void onClose() {
    }

    public void onOpen(Object item) {
        LinearLayout layout = mView.findViewById(R.id.bubble_layout);
        TextView txtTitle = mView.findViewById(R.id.bubble_title);
        TextView txtDescription = mView.findViewById(R.id.bubble_description);

        txtTitle.setText(motherActivity.getString(R.string.incidentDetected));
        long millis = this.incidentLogEntry.timestamp;
        String time = "";
        if (millis > 1337) {
            time = DateUtils.formatDateTime(motherActivity, millis, DateUtils.FORMAT_SHOW_TIME);
        }

        txtDescription.setText(time + " " + addressForLoc);

        layout.setOnClickListener((View v) -> {
            IncidentPopUpActivity.startIncidentPopUpActivity(incidentLogEntry, state, motherActivity);
            close();
        });
    }
}