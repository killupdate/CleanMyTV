package com.gathin.tvhome;

/** Parses a registered TV input's hardware suffix; never assumes HDMI 1 == HW4. */
final class CvteInput {
    private CvteInput() { }

    static Integer hardwareId(String inputId) {
        if (inputId == null) return null;
        int suffix = inputId.lastIndexOf("/HW");
        if (suffix <= 0) return null;
        String number = inputId.substring(suffix + 3);
        if (!number.matches("[0-9]{1,5}")) return null;
        int value = Integer.parseInt(number);
        return value <= 65535 ? value : null;
    }
}
