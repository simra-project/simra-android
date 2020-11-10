package de.tuberlin.mcc.simra.app.entities;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DataLogEntryTest {

    @Test
    public void parseDataLogEntryFromLine_NoData() {
        assertThat(DataLogEntry.parseDataLogEntryFromLine(""))
                .usingRecursiveComparison()
                .isEqualTo(DataLogEntry.newBuilder().build());
    }

    @Test
    public void parseDataLogEntryFromLine_Timestamp() {
        assertThat(DataLogEntry.parseDataLogEntryFromLine(",,,,,6,,,,"))
                .usingRecursiveComparison()
                .isEqualTo(DataLogEntry.newBuilder().withTimestamp(6L).build());
    }

    @Test
    public void parseDataLogEntryFromLine_GPS() {
        assertThat(DataLogEntry.parseDataLogEntryFromLine("1,2,,,,,3,,,"))
                .usingRecursiveComparison()
                .isEqualTo(DataLogEntry.newBuilder().withGPS(1D, 2D, 3F).build());
    }

    @Test
    public void parseDataLogEntryFromLine_Accelerometer() {
        assertThat(DataLogEntry.parseDataLogEntryFromLine(",,1,2,3,,,,,"))
                .usingRecursiveComparison()
                .isEqualTo(DataLogEntry.newBuilder().withAccelerometer(1F, 2F, 3F).build());
    }

    @Test
    public void parseDataLogEntryFromLine_Gyroscope() {
        assertThat(DataLogEntry.parseDataLogEntryFromLine(",,,,,,,1,2,3"))
                .usingRecursiveComparison()
                .isEqualTo(DataLogEntry.newBuilder().withGyroscope(1F, 2F, 3F).build());
    }

    @Test
    public void parseDataLogEntryFromLine_OBS_One_Value() {
        assertThat(DataLogEntry.parseDataLogEntryFromLine(",,,,,,,,,,1"))
                .usingRecursiveComparison()
                .isEqualTo(DataLogEntry.newBuilder().withOBS(1, null, null, null).build());
    }

    @Test
    public void parseDataLogEntryFromLine_OBS_Two_Value() {
        assertThat(DataLogEntry.parseDataLogEntryFromLine(",,,,,,,,,,1,2"))
                .usingRecursiveComparison()
                .isEqualTo(DataLogEntry.newBuilder().withOBS(1, 2, null, null).build());
    }

    @Test
    public void parseDataLogEntryFromLine_OBS_Three_Value() {
        assertThat(DataLogEntry.parseDataLogEntryFromLine(",,,,,,,,,,1,2,3"))
                .usingRecursiveComparison()
                .isEqualTo(DataLogEntry.newBuilder().withOBS(1, 2, 3, null).build());
    }

    @Test
    public void parseDataLogEntryFromLine_OBS_Four_Value() {
        assertThat(DataLogEntry.parseDataLogEntryFromLine(",,,,,,,,,,1,2,3,4"))
                .usingRecursiveComparison()
                .isEqualTo(DataLogEntry.newBuilder().withOBS(1, 2, 3, 4).build());
    }

    @Test
    public void parseDataLogEntryFromLine_Full() {
        assertThat(DataLogEntry.parseDataLogEntryFromLine("1,2,3,4,5,6,7,8,9,10,11,12,13,14"))
                .usingRecursiveComparison()
                .isEqualTo(DataLogEntry.newBuilder()
                        .withTimestamp(6L)
                        .withGPS(1D, 2D, 7F)
                        .withAccelerometer(3F, 4F, 5F)
                        .withGyroscope(8F, 9F, 10F)
                        .withOBS(11, 12, 13, 14).build());
    }

    @Test
    public void stringifyLogEntry_Full() {
        assertThat(DataLogEntry.newBuilder()
                .withTimestamp(6L)
                .withGPS(1D, 2D, 7F)
                .withAccelerometer(3F, 4F, 5F)
                .withGyroscope(8F, 9F, 10F)
                .withOBS(11, 12, 13, 14).build().stringifyDataLogEntry())
                .isEqualTo("1.0,2.0,3.0,4.0,5.0,6,7.0,8.0,9.0,10.0,11,12,13,14");
    }

    @Test
    public void stringifyLogEntry_Full_ExampleValues() {
        assertThat(DataLogEntry.newBuilder()
                .withTimestamp(1592319028261L)
                .withGPS(52.53949384561807D, 13.371213365189773, 6.0F)
                .withAccelerometer(-0.8885537F, -9.369222F, -2.433742F)
                .withGyroscope(0.008709193F, 0.21959732F, -0.057107173F)
                .withOBS(255, null, null, null).build().stringifyDataLogEntry())
                .isEqualTo("52.53949384561807,13.371213365189773,-0.8885537,-9.369222,-2.433742,1592319028261,6.0,0.008709193,0.21959732,-0.057107173,255,,,");
    }

    @Test
    public void stringifyLogEntry_EmptyLine() {
        assertThat(DataLogEntry.newBuilder().build().stringifyDataLogEntry())
                .isEqualTo(",,,,,,,,,,,,,");
    }
}