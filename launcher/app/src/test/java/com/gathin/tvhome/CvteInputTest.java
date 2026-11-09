package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class CvteInputTest {
    @Test public void usesHardwareIdRatherThanGuessedPortNumber() {
        assertEquals(Integer.valueOf(4), CvteInput.hardwareId("com.mediatek.tvinput/.HDMI/HW4"));
        assertEquals(Integer.valueOf(6), CvteInput.hardwareId("com.cvte.input/.Input/HW6"));
    }
    @Test public void refusesMalformedOverflowAndCecSuffixes() {
        for (String id : new String[]{null, "", "/HW4", "pkg/.Input", "pkg/.Input/HW-1",
                "pkg/.Input/HW65536", "pkg/.Input/HW9999999999", "pkg/.Input/HW4/CEC1"}) {
            assertNull(CvteInput.hardwareId(id));
        }
    }
}
