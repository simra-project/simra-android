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
                .isEqualTo(DataLogEntry.newBuilder().withGPS(1D, 2D, 3D).build());
    }

    @Test
    public void parseDataLogEntryFromLine_Accelerometer() {
        assertThat(DataLogEntry.parseDataLogEntryFromLine(",,1,2,3,,,,,"))
                .usingRecursiveComparison()
                .isEqualTo(DataLogEntry.newBuilder().withAccelerometer(1D, 2D, 3D).build());
    }

    @Test
    public void parseDataLogEntryFromLine_Gyroscope() {
        assertThat(DataLogEntry.parseDataLogEntryFromLine(",,,,,,,1,2,3"))
                .usingRecursiveComparison()
                .isEqualTo(DataLogEntry.newBuilder().withGyroscope(1D, 2D, 3D).build());
    }

    @Test
    public void parseDataLogEntryFromLine_Radmesser_One_Value() {
        assertThat(DataLogEntry.parseDataLogEntryFromLine(",,,,,,,,,,1,2"))
                .usingRecursiveComparison()
                .isEqualTo(DataLogEntry.newBuilder().withRadmesser(2, 1, null, null).build());
    }

    @Test
    public void parseDataLogEntryFromLine_Radmesser_Two_Value() {
        assertThat(DataLogEntry.parseDataLogEntryFromLine(",,,,,,,,,,1,2,3"))
                .usingRecursiveComparison()
                .isEqualTo(DataLogEntry.newBuilder().withRadmesser(2, 1, 3, null).build());
    }

    @Test
    public void parseDataLogEntryFromLine_Radmesser_Three_Value() {
        assertThat(DataLogEntry.parseDataLogEntryFromLine(",,,,,,,,,,1,2,3,4"))
                .usingRecursiveComparison()
                .isEqualTo(DataLogEntry.newBuilder().withRadmesser(2, 1, 3, 4).build());
    }

    @Test
    public void parseDataLogEntryFromLine_Radmesser_Four_Value() {
        assertThat(DataLogEntry.parseDataLogEntryFromLine(",,,,,,,,,,1,2,3,4"))
                .usingRecursiveComparison()
                .isEqualTo(DataLogEntry.newBuilder().withRadmesser(2, 1, 3, 4).build());
    }

    @Test
    public void parseDataLogEntryFromLine_Full() {
        assertThat(DataLogEntry.parseDataLogEntryFromLine("1,2,3,4,5,6,7,8,9,10,11,12,13,14"))
                .usingRecursiveComparison()
                .isEqualTo(DataLogEntry.newBuilder()
                        .withTimestamp(6L)
                        .withGPS(1D, 2D, 7D)
                        .withAccelerometer(3D, 4D, 5D)
                        .withGyroscope(8D, 9D, 10D)
                        .withRadmesser(11, 12, 13, 14).build());
    }

    @Test
    public void stringifyLogEntry_Full() {
        assertThat(DataLogEntry.stringifyDataLogEntry(
                DataLogEntry.newBuilder()
                        .withTimestamp(6L)
                        .withGPS(1D, 2D, 7D)
                        .withAccelerometer(3D, 4D, 5D)
                        .withGyroscope(8D, 9D, 10D)
                        .withRadmesser(11, 12, 13, 14).build()))
                .isEqualTo("1.0,2.0,3.0,4.0,5.0,6,7.0,8.0,9.0,10.0,11,12,13,14");
    }

    @Test
    public void stringifyLogEntry_Full_ExampleValues() {
        assertThat(DataLogEntry.stringifyDataLogEntry(
                DataLogEntry.newBuilder()
                        .withTimestamp(1592319028261L)
                        .withGPS(52.53949384561807D, 13.371213365189773, 6.0)
                        .withAccelerometer(-0.8885537, -9.369222, -2.433742)
                        .withGyroscope(0.008709193, 0.21959732, -0.057107173)
                        .withRadmesser(255, null, null, null).build()))
                .isEqualTo("52.53949384561807,13.371213365189773,-0.8885537,-9.369222,-2.433742,1592319028261,6.0,0.008709193,0.21959732,-0.057107173,255,,,");
    }

    @Test
    public void stringifyLogEntry_EmptyLine() {
        assertThat(DataLogEntry.stringifyDataLogEntry(
                DataLogEntry.newBuilder().build()))
                .isEqualTo(",,,,,,,,,,,,,");
    }
}