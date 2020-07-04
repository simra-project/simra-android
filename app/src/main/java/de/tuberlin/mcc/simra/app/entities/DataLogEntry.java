package de.tuberlin.mcc.simra.app.entities;

public class DataLogEntry {
    public final Double latitude;
    public final Double longitude;
    public final Double accelerometerX;
    public final Double accelerometerY;
    public final Double accelerometerZ;
    public final Long timestamp;
    public final Double GPSAccuracy;
    public final Double gyroscopeA;
    public final Double gyroscopeB;
    public final Double gyroscopeC;
    public final Integer RadmesserDistanceLeft1;
    public final Integer RadmesserDistanceLeft2;
    public final Integer RadmesserDistanceRight1;
    public final Integer RadmesserDistanceRight2;

    private DataLogEntry(Builder builder) {
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.accelerometerX = builder.accelerometerX;
        this.accelerometerY = builder.accelerometerY;
        this.accelerometerZ = builder.accelerometerZ;
        this.timestamp = builder.timestamp;
        this.GPSAccuracy = builder.GPSAccuracy;
        this.gyroscopeA = builder.gyroscopeA;
        this.gyroscopeB = builder.gyroscopeB;
        this.gyroscopeC = builder.gyroscopeC;
        RadmesserDistanceLeft1 = builder.radmesserDistanceLeft1;
        RadmesserDistanceLeft2 = builder.radmesserDistanceLeft2;
        RadmesserDistanceRight1 = builder.radmesserDistanceRight1;
        RadmesserDistanceRight2 = builder.radmesserDistanceRight2;
    }

    public static DataLogEntry parseDataLogEntryFromLine(String string) {
        String[] dataLogLine = string.split(",", -1);
        Builder dataLogEntry = DataLogEntry.newBuilder();

        if (dataLogLine.length >= 6 && !dataLogLine[0].isEmpty() && !dataLogLine[1].isEmpty() && !dataLogLine[6].isEmpty()) {
            dataLogEntry.withGPS(
                    Double.parseDouble(dataLogLine[0]),
                    Double.parseDouble(dataLogLine[1]),
                    Double.parseDouble(dataLogLine[6])
            );
        }

        if (dataLogLine.length >= 4 && !dataLogLine[2].isEmpty() && !dataLogLine[3].isEmpty() && !dataLogLine[4].isEmpty()) {
            dataLogEntry.withAccelerometer(
                    Double.parseDouble(dataLogLine[2]),
                    Double.parseDouble(dataLogLine[3]),
                    Double.parseDouble(dataLogLine[4])
            );
        }

        if (dataLogLine.length >= 5 && !dataLogLine[5].isEmpty()) {
            dataLogEntry.withTimestamp(
                    Long.parseLong(dataLogLine[5])
            );
        }

        if (dataLogLine.length >= 9 && !dataLogLine[7].isEmpty() && !dataLogLine[8].isEmpty() && !dataLogLine[9].isEmpty()) {
            dataLogEntry.withGyroscope(
                    Double.parseDouble(dataLogLine[7]),
                    Double.parseDouble(dataLogLine[8]),
                    Double.parseDouble(dataLogLine[9])
            );
        }

        dataLogEntry.withRadmesser(
                dataLogLine.length > 10 ? (!dataLogLine[10].isEmpty() ? Integer.parseInt(dataLogLine[10]) : null) : null,
                dataLogLine.length > 11 ? (!dataLogLine[11].isEmpty() ? Integer.parseInt(dataLogLine[11]) : null) : null,
                dataLogLine.length > 12 ? (!dataLogLine[12].isEmpty() ? Integer.parseInt(dataLogLine[12]) : null) : null,
                dataLogLine.length > 13 ? (!dataLogLine[13].isEmpty() ? Integer.parseInt(dataLogLine[13]) : null) : null
        );

        return dataLogEntry.build();
    }

    public static String stringifyDataLogEntry(DataLogEntry dataLogEntry) {
        return (dataLogEntry.latitude != null ? dataLogEntry.latitude : "") + "," +
                (dataLogEntry.longitude != null ? dataLogEntry.longitude : "") + "," +
                (dataLogEntry.accelerometerX != null ? dataLogEntry.accelerometerX : "") + "," +
                (dataLogEntry.accelerometerY != null ? dataLogEntry.accelerometerY : "") + "," +
                (dataLogEntry.accelerometerZ != null ? dataLogEntry.accelerometerZ : "") + "," +
                (dataLogEntry.timestamp != null ? dataLogEntry.timestamp : "") + "," +
                (dataLogEntry.GPSAccuracy != null ? dataLogEntry.GPSAccuracy : "") + "," +
                (dataLogEntry.gyroscopeA != null ? dataLogEntry.gyroscopeA : "") + "," +
                (dataLogEntry.gyroscopeB != null ? dataLogEntry.gyroscopeB : "") + "," +
                (dataLogEntry.gyroscopeC != null ? dataLogEntry.gyroscopeC : "") + "," +
                (dataLogEntry.RadmesserDistanceLeft1 != null ? dataLogEntry.RadmesserDistanceLeft1 : "") + "," +
                (dataLogEntry.RadmesserDistanceLeft2 != null ? dataLogEntry.RadmesserDistanceLeft2 : "") + "," +
                (dataLogEntry.RadmesserDistanceRight1 != null ? dataLogEntry.RadmesserDistanceRight1 : "") + "," +
                (dataLogEntry.RadmesserDistanceRight2 != null ? dataLogEntry.RadmesserDistanceRight2 : "");
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    static final class Builder {

        private Double latitude;
        private Double longitude;
        private Double accelerometerX;
        private Double accelerometerY;
        private Double accelerometerZ;
        private Long timestamp;
        private Double GPSAccuracy;
        private Double gyroscopeA;
        private Double gyroscopeB;
        private Double gyroscopeC;
        private Integer radmesserDistanceLeft1;
        private Integer radmesserDistanceLeft2;
        private Integer radmesserDistanceRight1;
        private Integer radmesserDistanceRight2;

        private Builder() {
        }

        public Builder withTimestamp(Long vTimestamp) {
            timestamp = vTimestamp;
            return this;
        }

        public Builder withAccelerometer(Double vAccelerometerX, Double vAccelerometerY, Double vAccelerometerZ) {
            accelerometerX = vAccelerometerX;
            accelerometerY = vAccelerometerY;
            accelerometerZ = vAccelerometerZ;
            return this;
        }

        public Builder withGPS(Double vLatitude, Double vLongitude, Double vGPSAccuracy) {
            latitude = vLatitude;
            longitude = vLongitude;
            GPSAccuracy = vGPSAccuracy;
            return this;
        }

        public Builder withGyroscope(Double vGyroscopeA, Double vGyroscopeB, Double vGyroscopeC) {
            gyroscopeA = vGyroscopeA;
            gyroscopeB = vGyroscopeB;
            gyroscopeC = vGyroscopeC;
            return this;
        }

        public Builder withRadmesser(Integer vRadmesserDistanceLeft1, Integer vRadmesserDistanceLeft2, Integer vRadmesserDistanceRight1, Integer vRadmesserDistanceRight2) {
            if (vRadmesserDistanceLeft1 != null) {
                radmesserDistanceLeft1 = vRadmesserDistanceLeft1;
            }
            if (vRadmesserDistanceLeft2 != null) {
                radmesserDistanceLeft2 = vRadmesserDistanceLeft2;
            }
            if (vRadmesserDistanceRight1 != null) {
                radmesserDistanceRight1 = vRadmesserDistanceRight1;
            }
            if (vRadmesserDistanceRight2 != null) {
                radmesserDistanceRight2 = vRadmesserDistanceRight2;
            }
            return this;
        }

        public DataLogEntry build() {
            return new DataLogEntry(this);
        }

    }
}
