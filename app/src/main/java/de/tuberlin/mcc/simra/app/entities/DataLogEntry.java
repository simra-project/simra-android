package de.tuberlin.mcc.simra.app.entities;

public class DataLogEntry {
    public final Integer rideId;
    public final Double latitude;
    public final Double longitude;
    public final Float accelerometerX;
    public final Float accelerometerY;
    public final Float accelerometerZ;
    public final Long timestamp;
    public final Float GPSAccuracy;
    public final Float gyroscopeA;
    public final Float gyroscopeB;
    public final Float gyroscopeC;
    public final Integer obsDistanceLeft1;
    public final Integer obsDistanceLeft2;
    public final Integer obsDistanceRight1;
    public final Integer obsDistanceRight2;

    private DataLogEntry(DataLogEntryBuilder dataLogEntryBuilder) {
        this.rideId = dataLogEntryBuilder.rideId;
        this.latitude = dataLogEntryBuilder.latitude;
        this.longitude = dataLogEntryBuilder.longitude;
        this.accelerometerX = dataLogEntryBuilder.accelerometerX;
        this.accelerometerY = dataLogEntryBuilder.accelerometerY;
        this.accelerometerZ = dataLogEntryBuilder.accelerometerZ;
        this.timestamp = dataLogEntryBuilder.timestamp;
        this.GPSAccuracy = dataLogEntryBuilder.GPSAccuracy;
        this.gyroscopeA = dataLogEntryBuilder.gyroscopeA;
        this.gyroscopeB = dataLogEntryBuilder.gyroscopeB;
        this.gyroscopeC = dataLogEntryBuilder.gyroscopeC;
        this.obsDistanceLeft1 = dataLogEntryBuilder.obsDistanceLeft1;
        this.obsDistanceLeft2 = dataLogEntryBuilder.obsDistanceLeft2;
        this.obsDistanceRight1 = dataLogEntryBuilder.obsDistanceRight1;
        this.obsDistanceRight2 = dataLogEntryBuilder.obsDistanceRight2;
    }

    public static DataLogEntry parseDataLogEntryFromLine(String string) {
        String[] dataLogLine = string.split(",", -1);
        DataLogEntryBuilder dataLogEntryBuilder = DataLogEntry.newBuilder();

        if (dataLogLine.length >= 6 && !dataLogLine[0].isEmpty() && !dataLogLine[1].isEmpty() && !dataLogLine[6].isEmpty()) {
            dataLogEntryBuilder.withGPS(
                    Double.parseDouble(dataLogLine[0]),
                    Double.parseDouble(dataLogLine[1]),
                    Float.parseFloat(dataLogLine[6])
            );
        }

        if (dataLogLine.length >= 4 && !dataLogLine[2].isEmpty() && !dataLogLine[3].isEmpty() && !dataLogLine[4].isEmpty()) {
            dataLogEntryBuilder.withAccelerometer(
                    Float.parseFloat(dataLogLine[2]),
                    Float.parseFloat(dataLogLine[3]),
                    Float.parseFloat(dataLogLine[4])
            );
        }

        if (dataLogLine.length >= 5 && !dataLogLine[5].isEmpty()) {
            dataLogEntryBuilder.withTimestamp(
                    Long.parseLong(dataLogLine[5])
            );
        }

        if (dataLogLine.length >= 9 && !dataLogLine[7].isEmpty() && !dataLogLine[8].isEmpty() && !dataLogLine[9].isEmpty()) {
            dataLogEntryBuilder.withGyroscope(
                    Float.parseFloat(dataLogLine[7]),
                    Float.parseFloat(dataLogLine[8]),
                    Float.parseFloat(dataLogLine[9])
            );
        }

        dataLogEntryBuilder.withOBS(
                dataLogLine.length > 10 ? (!dataLogLine[10].isEmpty() ? Integer.parseInt(dataLogLine[10]) : null) : null,
                dataLogLine.length > 11 ? (!dataLogLine[11].isEmpty() ? Integer.parseInt(dataLogLine[11]) : null) : null,
                dataLogLine.length > 12 ? (!dataLogLine[12].isEmpty() ? Integer.parseInt(dataLogLine[12]) : null) : null,
                dataLogLine.length > 13 ? (!dataLogLine[13].isEmpty() ? Integer.parseInt(dataLogLine[13]) : null) : null
        );

        return dataLogEntryBuilder.build();
    }

    public static DataLogEntryBuilder newBuilder() {
        return new DataLogEntryBuilder();
    }

    /**
     * Stringifies the DataLogEntry Object to a CSV Log Line
     *
     * @return Log Line without new line separator
     */
    public String stringifyDataLogEntry() {
        return (latitude != null ? latitude : "") + "," +
                (longitude != null ? longitude : "") + "," +
                (accelerometerX != null ? accelerometerX : "") + "," +
                (accelerometerY != null ? accelerometerY : "") + "," +
                (accelerometerZ != null ? accelerometerZ : "") + "," +
                (timestamp != null ? timestamp : "") + "," +
                (GPSAccuracy != null ? GPSAccuracy : "") + "," +
                (gyroscopeA != null ? gyroscopeA : "") + "," +
                (gyroscopeB != null ? gyroscopeB : "") + "," +
                (gyroscopeC != null ? gyroscopeC : "") + "," +
                (obsDistanceLeft1 != null ? obsDistanceLeft1 : "") + "," +
                (obsDistanceLeft2 != null ? obsDistanceLeft2 : "") + "," +
                (obsDistanceRight1 != null ? obsDistanceRight1 : "") + "," +
                (obsDistanceRight2 != null ? obsDistanceRight2 : "");
    }

    public static final class DataLogEntryBuilder {
        private Integer rideId;
        private Double latitude;
        private Double longitude;
        private Float accelerometerX;
        private Float accelerometerY;
        private Float accelerometerZ;
        private Long timestamp;
        private Float GPSAccuracy;
        private Float gyroscopeA;
        private Float gyroscopeB;
        private Float gyroscopeC;
        private Integer obsDistanceLeft1;
        private Integer obsDistanceLeft2;
        private Integer obsDistanceRight1;
        private Integer obsDistanceRight2;

        private DataLogEntryBuilder() {
        }

        public DataLogEntryBuilder withRideId(Integer rideId) {
            this.rideId = rideId;
            return this;
        }

        public DataLogEntryBuilder withTimestamp(Long vTimestamp) {
            timestamp = vTimestamp;
            return this;
        }

        public DataLogEntryBuilder withAccelerometer(Float vAccelerometerX, Float vAccelerometerY, Float vAccelerometerZ) {
            accelerometerX = vAccelerometerX;
            accelerometerY = vAccelerometerY;
            accelerometerZ = vAccelerometerZ;
            return this;
        }

        public DataLogEntryBuilder withGPS(Double vLatitude, Double vLongitude, Float vGPSAccuracy) {
            latitude = vLatitude;
            longitude = vLongitude;
            GPSAccuracy = vGPSAccuracy;
            return this;
        }

        public DataLogEntryBuilder withGyroscope(Float vGyroscopeA, Float vGyroscopeB, Float vGyroscopeC) {
            gyroscopeA = vGyroscopeA;
            gyroscopeB = vGyroscopeB;
            gyroscopeC = vGyroscopeC;
            return this;
        }

        public DataLogEntryBuilder withOBS(Integer vOBSDistanceLeft1, Integer vOBSDistanceLeft2, Integer vOBSDistanceRight1, Integer vOBSDistanceRight2) {
            if (vOBSDistanceLeft1 != null) {
                obsDistanceLeft1 = vOBSDistanceLeft1;
            }
            if (vOBSDistanceLeft2 != null) {
                obsDistanceLeft2 = vOBSDistanceLeft2;
            }
            if (vOBSDistanceRight1 != null) {
                obsDistanceRight1 = vOBSDistanceRight1;
            }
            if (vOBSDistanceRight2 != null) {
                obsDistanceRight2 = vOBSDistanceRight2;
            }
            return this;
        }

        public DataLogEntry build() {
            return new DataLogEntry(this);
        }

    }
}
