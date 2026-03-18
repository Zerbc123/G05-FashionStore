package vn.edu.fpt.fashionstore.util;

import java.time.LocalDate;
import java.time.ZoneId;

public class DateUtils {
    // earliest allowed birth date: January 1, 1950 (as previously requested)
    private static final LocalDate MIN_DOB = LocalDate.of(1950, 1, 1);

    /**
     * Validates that a date of birth is between {@code 1950-01-01} and today (inclusive).
     *
     * @param dob the date to validate, may be null
     * @return true if dob is null or within the allowed range
     */
    public static boolean isValidDOB(LocalDate dob) {
        if (dob == null) {
            // empty value is acceptable; other logic may enforce requiredness
            return true;
        }
        LocalDate today = LocalDate.now();
        return !dob.isBefore(MIN_DOB) && !dob.isAfter(today);
    }

    /**
     * Returns the minimum allowed birth date (1950-01-01).
     */
    public static LocalDate getMinDob() {
        return MIN_DOB;
    }
}
