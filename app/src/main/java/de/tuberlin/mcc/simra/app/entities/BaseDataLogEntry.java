package de.tuberlin.mcc.simra.app.entities;

public class BaseDataLogEntry {
    public final float accelerometerX;
    public final float accelerometerY;
    public final float accelerometerZ;
    public final long timestamp;

    public BaseDataLogEntry(float accelerometerX, float accelerometerY, float accelerometerZ, long timestamp) {
        this.accelerometerX = accelerometerX;
        this.accelerometerY = accelerometerY;
        this.accelerometerZ = accelerometerZ;
        this.timestamp = timestamp;
    }
}
