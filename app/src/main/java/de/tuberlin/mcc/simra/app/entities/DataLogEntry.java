package de.tuberlin.mcc.simra.app.entities;

public class DataLogEntry extends BaseDataLogEntry {
    public final float latitude;
    public final float longitude;
    public final float GPSAccuracy;
    public final float gyroscopeA;
    public final float gyroscopeB;
    public final float gyroscopeC;
    public final long RadmesserTimeStamp;
    public final float RadmesserDistanceLeft1;
    public final float RadmesserDistanceLeft2;
    public final float RadmesserDistanceRight1;
    public final float RadmesserDistanceRight2;

    public DataLogEntry(float latitude, float longitude, float accelerometerX, float accelerometerY, float accelerometerZ, long timestamp, float GPSAccuracy, float gyroscopeA, float gyroscopeB, float gyroscopeC, long radmesserTimeStamp, float radmesserDistanceLeft1, float radmesserDistanceLeft2, float radmesserDistanceRight1, float radmesserDistanceRight2) {
        super(accelerometerX, accelerometerY, accelerometerZ, timestamp);
        this.latitude = latitude;
        this.longitude = longitude;
        this.GPSAccuracy = GPSAccuracy;
        this.gyroscopeA = gyroscopeA;
        this.gyroscopeB = gyroscopeB;
        this.gyroscopeC = gyroscopeC;
        RadmesserTimeStamp = radmesserTimeStamp;
        RadmesserDistanceLeft1 = radmesserDistanceLeft1;
        RadmesserDistanceLeft2 = radmesserDistanceLeft2;
        RadmesserDistanceRight1 = radmesserDistanceRight1;
        RadmesserDistanceRight2 = radmesserDistanceRight2;
    }

    public static BaseDataLogEntry parseDataLogEntryFromLine(String string) {
        String[] dataLogLine = string.split(",", -1);
        if (dataLogLine.length > 11) {
            // No Radmesser Data appended
            return new DataLogEntry(
                    Float.parseFloat(dataLogLine[0]),
                    Float.parseFloat(dataLogLine[1]),
                    Float.parseFloat(dataLogLine[2]),
                    Float.parseFloat(dataLogLine[3]),
                    Float.parseFloat(dataLogLine[4]),
                    Long.parseLong(dataLogLine[5]),
                    Float.parseFloat(dataLogLine[6]),
                    Float.parseFloat(dataLogLine[7]),
                    Float.parseFloat(dataLogLine[8]),
                    Float.parseFloat(dataLogLine[9]),
                    0,
                    0,
                    0,
                    0,
                    0
            );

        }
        return new BaseDataLogEntry(
                Float.parseFloat(dataLogLine[2]),
                Float.parseFloat(dataLogLine[3]),
                Float.parseFloat(dataLogLine[4]),
                Long.parseLong(dataLogLine[5])
        );
    }
}
