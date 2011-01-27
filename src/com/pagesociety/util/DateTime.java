package com.pagesociety.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;

public class DateTime {
	private static SimpleDateFormat dateFormatDB = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static SimpleDateFormat dateFormatMessage = new SimpleDateFormat("HH:mm, d MMM yyyy");
	private static SimpleDateFormat timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss");
	private static SimpleDateFormat timestamp_no_underscore = new SimpleDateFormat("yyyyMMddHHmmss");

	public static Date toDate(String s, boolean dayFirst) {
		String sep = " ";
		if (s.indexOf(".") != -1) {
			sep = ".";
		}
		if (s.indexOf("/") != -1) {
			sep = "/";
		}
		if (s.indexOf("-") != -1) {
			sep = "-";
		}
		int c = 0;
		String m = "1";
		String d = "1";
		String y = "1";
		StringTokenizer st = new StringTokenizer(s, sep);
		while (st.hasMoreTokens()) {
			String q = st.nextToken();
			if (c == 0) {
				if (dayFirst) {
					d = q;
				} else {
					m = q;
				}
			}
			if (c == 1) {
				if (dayFirst) {
					m = q;
				} else {
					d = q;
				}
			}
			if (c == 2) {
				y = q;
			}
			c++;
		}
		if (y.length() == 2) {
			y = "19" + y;
		}
		Calendar cal = new GregorianCalendar();
		cal.set(Text.toInt(y), Text.toInt(m), Text.toInt(d), 0, 0, 0);
		return cal.getTime();
	}

	public static String format(Date date) {
		return dateFormatMessage.format(date);
	}

	public static String formatDB(Date date) {
		return dateFormatDB.format(date);
	}
	
	public static String getTimeStamp()
	{
		return timestamp.format(new Date());
	}
	
	public static String getTimeStampNoUnderscore()
	{
		return timestamp_no_underscore.format(new Date());
	}
}
