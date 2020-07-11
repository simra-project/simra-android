package de.tuberlin.mcc.simra.app.entities;

import de.tuberlin.mcc.simra.app.services.RadmesserService;

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
    public final Integer radmesserDistanceLeft1;
    public final Integer radmesserDistanceLeft2;
    public final Integer radmesserDistanceRight1;
    public final Integer radmesserDistanceRight2;
    public final String  radmesserClosePassEventType;
    public final String radmesserClosePassEventPayload1;
    public final String radmesserClosePassEventPayload2;

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
        this.radmesserDistanceLeft1 = dataLogEntryBuilder.radmesserDistanceLeft1;
        this.radmesserDistanceLeft2 = dataLogEntryBuilder.radmesserDistanceLeft2;
        this.radmesserDistanceRight1 = dataLogEntryBuilder.radmesserDistanceRight1;
        this.radmesserDistanceRight2 = dataLogEntryBuilder.radmesserDistanceRight2;
        this.radmesserClosePassEventType = dataLogEntryBuilder.radmesserClosePassEventType;
        this.radmesserClosePassEventPayload1 = dataLogEntryBuilder.radmesserClosePassEventPayload1;
        this.radmesserClosePassEventPayload2 = dataLogEntryBuilder.radmesserClosePassEventPayload2;
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

        dataLogEntryBuilder.withRadmesser(
                dataLogLine.length > 10 ? (!dataLogLine[10].isEmpty() ? Integer.parseInt(dataLogLine[10]) : null) : null,
                dataLogLine.length > 11 ? (!dataLogLine[11].isEmpty() ? Integer.parseInt(dataLogLine[11]) : null) : null,
                dataLogLine.length > 12 ? (!dataLogLine[12].isEmpty() ? Integer.parseInt(dataLogLine[12]) : null) : null,
                dataLogLine.length > 13 ? (!dataLogLine[13].isEmpty() ? Integer.parseInt(dataLogLine[13]) : null) : null
        );

         // TODO load radmesser close pass event correctly from string
        RadmesserService.ClosePassEvent event = new RadmesserService.ClosePassEvent("0;none;");
        event.eventType = dataLogLine.length > 14 && !dataLogLine[14].isEmpty() ? dataLogLine[14] : null;
        dataLogEntryBuilder.withRadmesserClosePassEvent(event);

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
                (radmesserDistanceLeft1 != null ? radmesserDistanceLeft1 : "") + "," +
                (radmesserDistanceLeft2 != null ? radmesserDistanceLeft2 : "") + "," +
                (radmesserDistanceRight1 != null ? radmesserDistanceRight1 : "") + "," +
                (radmesserDistanceRight2 != null ? radmesserDistanceRight2 : "") + "," +
                (radmesserClosePassEventType != null ? radmesserClosePassEventType : "") + "," +
                (radmesserClosePassEventPayload1 != null ? radmesserClosePassEventPayload1 : "") + "," +
                (radmesserClosePassEventPayload2 != null ? radmesserClosePassEventPayload2 : "");
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
        private Integer radmesserDistanceLeft1; // TODO: rename!
        private Integer radmesserDistanceLeft2;
        private Integer radmesserDistanceRight1;
        private Integer radmesserDistanceRight2;
        private String  radmesserClosePassEventType;
        private String radmesserClosePassEventPayload1;
        private String radmesserClosePassEventPayload2;

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

        public DataLogEntryBuilder withRadmesser(Integer vRadmesserDistanceLeft1, Integer vRadmesserDistanceLeft2, Integer vRadmesserDistanceRight1, Integer vRadmesserDistanceRight2) {
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

        public void withRadmesserClosePassEvent(RadmesserService.ClosePassEvent event) {
            radmesserClosePassEventType = event.eventType;
            radmesserClosePassEventPayload1 = event.payload.size() >= 1 ? event.payload.get(0) : "";
            radmesserClosePassEventPayload2 = event.payload.size() >= 2 ? event.payload.get(1) : "";
        }

        public DataLogEntry build() {
            return new DataLogEntry(this);
        }

    }
}
