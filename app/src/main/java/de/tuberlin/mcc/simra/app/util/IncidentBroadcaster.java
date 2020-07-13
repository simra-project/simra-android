package de.tuberlin.mcc.simra.app.util;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;


public class IncidentBroadcaster {

    private static final String MANUAL_INCIDENT = "de.tuberlin.mcc.simra.app.incidentbroadcaster.manualincident";
    private static final String EXTRA_INCIDENT_TYPE = "EXTRA_INCIDENT_TYPE";

    public static void broadcastIncident(Context ctx, int type) {
        Intent intent = new Intent(ctx, IncidentBroadcaster.class);
        intent.setAction(MANUAL_INCIDENT);
        intent.putExtra(EXTRA_INCIDENT_TYPE, type);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
    }

    public static BroadcastReceiver recieveIncidents(Context ctx, IncidentCallbacks callbacks) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                callbacks.onManualIncident(intent.getIntExtra(EXTRA_INCIDENT_TYPE, IncidentLogEntry.INCIDENT_TYPE.NOTHING));
            }
        };
        LocalBroadcastManager.getInstance(ctx).registerReceiver(receiver, new IntentFilter(MANUAL_INCIDENT));
        return receiver;
    }

    public abstract static class IncidentCallbacks {
        public void onManualIncident(int incidentType) {
        }

    }

}
