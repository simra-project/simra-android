package de.tuberlin.mcc.simra.app.entities;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.tuberlin.mcc.simra.app.util.IOUtils;
import de.tuberlin.mcc.simra.app.util.Utils;

public class IncidentLog {
    public final static String INCIDENT_LOG_HEADER = "key,lat,lon,ts,bike,childCheckBox,trailerCheckBox,pLoc,incident,i1,i2,i3,i4,i5,i6,i7,i8,i9,scary,desc,i10";
    public final List<IncidentLogEntry> incidents;
    public final int rideId;

    private IncidentLog(int rideId, List<IncidentLogEntry> incidents) {
        this.rideId = rideId;
        this.incidents = incidents;
    }

    public static IncidentLog loadIncidentLog(int id, Context context) {
        File incidentFile = IOUtils.Files.getEventsFile(id, false, context);
        List<IncidentLogEntry> incidents = new ArrayList<>();
        if (incidentFile.exists()) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(incidentFile))) {
                // Skip first two line as they do only contain the Header
                bufferedReader.readLine();
                bufferedReader.readLine();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        incidents.add(IncidentLogEntry.parseEntryFromLine(line));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new IncidentLog(id, incidents);
    }

    public static void saveIncidentLog(IncidentLog incidentLog, Context context) {
        String incidentString = "";
        for (IncidentLogEntry incident : incidentLog.incidents) {
            incidentString += incident.stringifyDataLogEntry() + System.lineSeparator();
        }
        File newFile = IOUtils.Files.getEventsFile(incidentLog.rideId, false, context);
        // TODO: Merge Logic
        Utils.overwriteFile(IOUtils.Files.getFileInfoLine() + INCIDENT_LOG_HEADER + System.lineSeparator() + incidentString, newFile);
    }
}
