package de.tuberlin.mcc.simra.app.entities;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IncidentLogEntryTest {


    @Test
    public void parseEntryFromLine_EmptyString() {
        assertThat(IncidentLogEntry.parseEntryFromLine(""))
                .usingRecursiveComparison()
                .isEqualTo(IncidentLogEntry.newBuilder().build());
    }

    @Test
    public void parseEntryFromLine_Empty() {
        assertThat(IncidentLogEntry.parseEntryFromLine(",,,,,,,,,,,,,,,,,"))
                .usingRecursiveComparison()
                .isEqualTo(IncidentLogEntry.newBuilder().build());
    }

    @Test
    public void parseEntryFromLine_Full_Negative() {
        assertThat(IncidentLogEntry.parseEntryFromLine("0,52.4949566,13.3658506,1593886060502,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,string,0"))
                .usingRecursiveComparison()
                .isEqualTo(IncidentLogEntry.newBuilder().withKey(0).withBaseInformation(1593886060502L, 52.4949566, 13.3658506).withRideInformation(0, false, false, 0, 0, new IncidentLogEntry.InvolvedRoadUser(false, false, false, false, false, false, false, false, false, false), false, "string").build());
    }

    @Test
    public void parseEntryFromLine_Full_Positive() {
        assertThat(IncidentLogEntry.parseEntryFromLine("0,52.4949566,13.3658506,1593886060502,2,1,1,3,4,1,1,1,1,1,1,1,1,1,1,string,1"))
                .usingRecursiveComparison()
                .isEqualTo(IncidentLogEntry.newBuilder().withKey(0).withBaseInformation(1593886060502L, 52.4949566, 13.3658506).withRideInformation(2, true, true, 3, 4, new IncidentLogEntry.InvolvedRoadUser(true, true, true, true, true, true, true, true, true, true), true, "string").build());
    }

    @Test
    public void parseEntryFromLine_Test() {
        assertThat(IncidentLogEntry.parseEntryFromLine("0,52.4949566,13.3658506,1593886060502,0,0,0,0,,,,,,,,,,,,,"))
                .usingRecursiveComparison()
                .isEqualTo(IncidentLogEntry.newBuilder().withKey(0).withBaseInformation(1593886060502L, 52.4949566, 13.3658506).withRideInformation(0, false, false, 0, 0, new IncidentLogEntry.InvolvedRoadUser(false, false, false, false, false, false, false, false, false, false), false, "").build());
    }

    @Test
    public void stringifyDataLogEntry_Empty() {
        assertThat(IncidentLogEntry.newBuilder()
                .build().stringifyDataLogEntry())
                .isEqualTo(",,,,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,");
    }

    @Test
    public void stringifyDataLogEntry_Example() {
        assertThat(IncidentLogEntry.newBuilder()
                .withKey(0)
                .withBaseInformation(1561224261445L, 52.48022987, 13.35637859)
                .withRideInformation(
                        3,
                        false,
                        false,
                        2,
                        0,
                        new IncidentLogEntry.InvolvedRoadUser(
                                false,
                                false,
                                false,
                                false,
                                false,
                                false,
                                false,
                                false,
                                false,
                                false
                        ),
                        false,
                        ""

                )
                .build().stringifyDataLogEntry())
                .isEqualTo("0,52.48022987,13.35637859,1561224261445,3,0,0,2,0,0,0,0,0,0,0,0,0,0,0,0,");
    }

    @Test
    public void stringifyDataLogEntry_Full_Positive() {
        assertThat(IncidentLogEntry.newBuilder()
                .withKey(0)
                .withBaseInformation(1561224261445L, 52.48022987, 13.35637859)
                .withRideInformation(
                        1,
                        true,
                        true,
                        2,
                        3,
                        new IncidentLogEntry.InvolvedRoadUser(
                                true,
                                true,
                                true,
                                true,
                                true,
                                true,
                                true,
                                true,
                                true,
                                true
                        ),
                        true,
                        "string"

                )
                .build().stringifyDataLogEntry())
                .isEqualTo("0,52.48022987,13.35637859,1561224261445,1,1,1,2,3,1,1,1,1,1,1,1,1,1,1,1,string");
    }
}