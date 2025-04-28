package org.saeta.digitalidentitysystem.util;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class DateUtils {

    public static boolean isWithinTimeRestriction(LocalDateTime dateTime, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        if (dateTime.getDayOfWeek() != dayOfWeek) {
            return false;
        }

        LocalTime time = dateTime.toLocalTime();
        return !time.isBefore(startTime) && !time.isAfter(endTime);
    }

    public static long daysBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return java.time.Duration.between(startDate, endDate).toDays();
    }
}
