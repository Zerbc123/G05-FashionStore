package vn.edu.fpt.fashionstore.util;

import java.util.Calendar;
import java.util.Date;

public class DateUtils {
    // earliest allowed birth date: January 1, 1900
    private static final Date MIN_DOB;

    static {
        // start at 1950-01-01 as requested by user
        Calendar cal = Calendar.getInstance();
        cal.set(1950, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        MIN_DOB = cal.getTime();
    }

    /**
     * Validates that a date of birth is between {@code 1900-01-01} and today (inclusive).
     *
     * @param dob the date to validate, may be null
     * @return true if dob is null or within the allowed range
     */
    public static boolean isValidDOB(Date dob) {
        if (dob == null) {
            // empty value is acceptable; other logic may enforce requiredness
            return true;
        }
        Date today = new Date();
        return !dob.before(MIN_DOB) && !dob.after(today);
    }

    /**
     * Returns the minimum allowed birth date (1900-01-01).
     */
    public static Date getMinDob() {
        return MIN_DOB;
    }
}
