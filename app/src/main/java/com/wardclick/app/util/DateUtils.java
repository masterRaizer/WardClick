package com.wardclick.app.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class DateUtils {
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public static String formatIsoToDisplay(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "";
        try {
            LocalDate date = LocalDate.parse(isoDate, ISO_FORMATTER);
            return date.format(DISPLAY_FORMATTER);
        } catch (DateTimeParseException e) {
            return isoDate;
        }
    }

    public static String formatDisplayToIso(String displayDate) {
        if (displayDate == null || displayDate.isEmpty()) return "";
        try {
            LocalDate date = LocalDate.parse(displayDate, DISPLAY_FORMATTER);
            return date.format(ISO_FORMATTER);
        } catch (DateTimeParseException e) {
            return displayDate;
        }
    }

    public static long calculateNights(String checkInIso, String checkOutIso) {
        if (checkInIso == null || checkOutIso == null) return 0;
        try {
            LocalDate checkIn = LocalDate.parse(checkInIso, ISO_FORMATTER);
            LocalDate checkOut = LocalDate.parse(checkOutIso, ISO_FORMATTER);
            long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
            return Math.max(0, nights);
        } catch (DateTimeParseException e) {
            return 0;
        }
    }

    public static boolean isDateRangeValid(String checkInIso, String checkOutIso) {
        if (checkInIso == null || checkOutIso == null) return false;
        try {
            LocalDate checkIn = LocalDate.parse(checkInIso, ISO_FORMATTER);
            LocalDate checkOut = LocalDate.parse(checkOutIso, ISO_FORMATTER);
            return !checkOut.isBefore(checkIn) && !checkIn.isBefore(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
