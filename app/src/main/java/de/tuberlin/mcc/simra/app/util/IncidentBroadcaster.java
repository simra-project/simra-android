package de.tuberlin.mcc.simra.app.util;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class IncidentBroadcaster {

    final static String MANUAL_INCIDENT = "de.tuberlin.mcc.simra.app.incidentbroadcaster.manualincident";

    public static void broadcastIncident(Context ctx){
        Intent intent = new Intent(ctx, IncidentBroadcaster.class);
        intent.setAction(MANUAL_INCIDENT);
        System.out.println("BROADCASTING");
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
    }

    public abstract static class IncidentCallbacks {
        public void onManualIncident() {
        }

    }

    public static BroadcastReceiver recieveIncidents(Context ctx, IncidentCallbacks callbacks) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MANUAL_INCIDENT);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                System.out.println("AQUI RECIEVER");
                switch (intent.getAction()){
                    case MANUAL_INCIDENT:
                        callbacks.onManualIncident();
                        break;
                    default:
                        break;
                }

            }
        };
        LocalBroadcastManager.getInstance(ctx).registerReceiver(receiver, filter);
        return receiver;
    }

}
