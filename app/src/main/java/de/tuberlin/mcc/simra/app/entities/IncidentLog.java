package de.tuberlin.mcc.simra.app.entities;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.tuberlin.mcc.simra.app.util.IOUtils;

public class IncidentLog {
    public final static String INCIDENT_LOG_HEADER = "key,lat,lon,ts,bike,childCheckBox,trailerCheckBox,pLoc,incident,i1,i2,i3,i4,i5,i6,i7,i8,i9,scary,desc,i10";
    public final List<IncidentLogEntry> incidents = new ArrayList<>();

    IncidentLog(int id, Context context) {
        File incidentFile = IOUtils.Files.getEventsFile(id, false, context);
        if (incidentFile.exists()) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(incidentFile))) {
                // 59#1
                // lat,lon,X,Y,Z,timeStamp,acc,a,b,c
                bufferedReader.readLine();
                bufferedReader.readLine();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        incidents.add(IncidentLogEntry.parseEntryFromLine(line));
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String toFileString() {
        String incidentString = "";
        for (IncidentLogEntry incident : incidents) {
            incidentString += incident.stringifyDataLogEntry() + System.lineSeparator();
        }
        return IOUtils.Files.getFileInfoLine() + INCIDENT_LOG_HEADER + System.lineSeparator() + incidentString;
    }
}
