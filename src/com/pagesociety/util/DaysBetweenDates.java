package com.pagesociety.util;


public class DaysBetweenDates
{
	//Doug Bell, Oct 6, 2001
	/**
	 * Calculates the number of days between two calendar days in a manner
	 * which is independent of the Calendar type used.
	 *
	 * @param d1    The first date.
	 * @param d2    The second date.
	 *
	 * @return      The number of days between the two dates.  Zero is
	 *              returned if the dates are the same, one if the dates are
	 *              adjacent, etc.  The order of the dates
	 *              does not matter, the value returned is always >= 0.
	 *              If Calendar types of d1 and d2
	 *              are different, the result may not be accurate.
	 */
	public static int getDaysBetween (java.util.Calendar d1, java.util.Calendar d2) {
	    if (d1.after(d2))
	    {  // swap dates so that d1 is start and d2 is end
	        java.util.Calendar swap = d1;
	        d1 = d2;
	        d2 = swap;
	    }
	    int days = d2.get(java.util.Calendar.DAY_OF_YEAR) -
	               d1.get(java.util.Calendar.DAY_OF_YEAR);
	    int y2 = d2.get(java.util.Calendar.YEAR);
	    if (d1.get(java.util.Calendar.YEAR) != y2) 
	    {
	        d1 = (java.util.Calendar) d1.clone();
	        do 
	        {
	            days += d1.getActualMaximum(java.util.Calendar.DAY_OF_YEAR);
	            d1.add(java.util.Calendar.YEAR, 1);
	        } while (d1.get(java.util.Calendar.YEAR) != y2);
	    }
	    return days;
	} // getDaysBetween()
}