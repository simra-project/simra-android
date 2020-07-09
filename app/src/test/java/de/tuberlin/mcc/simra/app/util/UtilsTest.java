package de.tuberlin.mcc.simra.app.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UtilsTest {

    @Test
    public void isInTimeFrame_NoFrame() {
        assertThat(Utils.isInTimeFrame(null, null, 1)).isEqualTo(true);
    }

    @Test
    public void isInTimeFrame_FullFrame_InFrame() {
        assertThat(Utils.isInTimeFrame(100L, 300L, 200L)).isEqualTo(true);
    }

    @Test
    public void isInTimeFrame_FullFrame_OutOfFrame_Earlier() {
        assertThat(Utils.isInTimeFrame(100L, 300L, 1L)).isEqualTo(false);
    }

    @Test
    public void isInTimeFrame_FullFrame_OutOfFrame_Later() {
        assertThat(Utils.isInTimeFrame(100L, 300L, 400L)).isEqualTo(false);
    }

    @Test
    public void isInTimeFrame_PartialFrameStart_OutOfFrame_Earlier() {
        assertThat(Utils.isInTimeFrame(100L, null, 1)).isEqualTo(false);
    }

    @Test
    public void isInTimeFrame_PartialFrameStart_InFrame() {
        assertThat(Utils.isInTimeFrame(100L, null, 200L)).isEqualTo(true);
    }

    @Test
    public void isInTimeFrame_PartialFrameEnd_InFrame() {
        assertThat(Utils.isInTimeFrame(null, 200L, 100L)).isEqualTo(true);
    }

    @Test
    public void isInTimeFrame_PartialFrameEnd_OutOfFrame_Later() {
        assertThat(Utils.isInTimeFrame(null, 200L, 300L)).isEqualTo(false);
    }

}