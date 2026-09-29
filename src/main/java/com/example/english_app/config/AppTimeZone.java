package com.example.english_app.config;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Business time zone of the app. Every "today", streak, daily goal and SRS due time is computed in this zone,
 * and DB columns (TIMESTAMP without time zone) store wall-clock time in it.
 */
public final class AppTimeZone {
    public static final String ID = "Asia/Ho_Chi_Minh";
    public static final ZoneId ZONE = ZoneId.of(ID);

    private AppTimeZone() {
    }

    /** Makes LocalDate.now()/LocalDateTime.now() use the business zone, whatever the server/container TZ is. */
    public static void applyAsJvmDefault() {
        TimeZone.setDefault(TimeZone.getTimeZone(ID));
    }
}
