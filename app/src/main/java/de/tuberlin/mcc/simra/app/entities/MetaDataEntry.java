package de.tuberlin.mcc.simra.app.entities;

public class MetaDataEntry {
    public Integer rideId;
    public Long startTime;
    public Long endTime;
    public Integer state;
    public Integer numberOfIncidents;
    public Long waitedTime;
    public Long distance;
    public Integer numberOfScaryIncidents;
    public Integer region;

    public MetaDataEntry(Integer rideId, Long startTime, Long endTime, Integer state, Integer numberOfIncidents, Long waitedTime, Long distance, Integer numberOfScaryIncidents, Integer region) {
        this.rideId = rideId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.state = state != null ? state : MetaData.STATE.JUST_RECORDED;
        this.numberOfIncidents = numberOfIncidents != null ? numberOfIncidents : 0;
        this.waitedTime = waitedTime != null ? waitedTime : 0;
        this.distance = distance != null ? distance : 0;
        this.numberOfScaryIncidents = numberOfScaryIncidents != null ? numberOfScaryIncidents : 0;
        this.region = region != null ? region : 0;
    }

    public static MetaDataEntry parseEntryFromLine(String string) {
        String[] dataLogLine = string.split(",", -1);
        return new MetaDataEntry(
                Integer.parseInt(dataLogLine[0]),
                Long.parseLong(dataLogLine[1]),
                Long.parseLong(dataLogLine[2]),
                Integer.parseInt(dataLogLine[3]),
                Integer.parseInt(dataLogLine[4]),
                Long.parseLong(dataLogLine[5]),
                Long.parseLong(dataLogLine[6]),
                Integer.parseInt(dataLogLine[7]),
                Integer.parseInt(dataLogLine[8])
        );
    }


    /**
     * Stringifies the MetaDataEntry Object to a CSV Log Line
     *
     * @return Log Line without new line separator
     */
    public String stringifyMetaDataEntry() {
        return rideId + "," + startTime + "," + endTime + "," + state + "," + numberOfIncidents + "," + waitedTime + "," + distance + "," + numberOfScaryIncidents + "," + region;
    }
}
