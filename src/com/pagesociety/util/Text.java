package com.pagesociety.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

public class Text
{
	private static final Logger logger = Logger.getLogger(Text.class);
	private static final char QUOTE = '\'';

	public static String fixQuotes(String s)
	{
		StringBuffer rs = new StringBuffer();
		if (s != null)
		{
			for (int i = 0; i < s.length(); i++)
			{
				char c = s.charAt(i);
				if ((c == 13) || ((c > 31) && (c < 128)) || c > 159)
				{
					char c1 = ' ';
					if (i > 0)
					{
						c1 = s.charAt(i - 1);
					}
					if ((c == '\'') && (c1 != '\\'))
					{
						rs.append("\\\'");
					}
					else
					{
						rs.append(c);
					}
				}
			}
		}
		return rs.toString();
	}

	public static String enquote(String value)
	{
		return QUOTE + fixQuotes(value) + QUOTE;
	}

	public static double toDouble(String s)
	{
		if (s == null)
		{
			return -1;
		}
		double d = -1;
		try
		{
			d = Double.parseDouble(s);
		}
		catch (Exception e)
		{
		}
		return d;
	}

	public static float toFloat(String s)
	{
		if (s == null)
		{
			return -1;
		}
		float f = -1;
		try
		{
			f = Float.parseFloat(s);
		}
		catch (Exception e)
		{
		}
		return f;
	}

	public static int toInt(String s)
	{
		if (s == null)
		{
			return -1;
		}
		int i = -1;
		try
		{
			i = Integer.parseInt(s);
		}
		catch (Exception e)
		{
		}
		return i;
	}
	
	public static long toLong(String s)
	{
		if (s == null)
		{
			return -1;
		}
		long l = -1;
		try
		{
			l = Long.parseLong(s);
		}
		catch (Exception e)
		{
		}
		return l;
	}

	public static String encodeURIComponent(String s)
	{
		if (s == null)
		{
			return "";
		}
		try
		{
			//return URLEncoder.encode(s, "UTF-8");
			return FastURLEncoder.encode(s);
		}
		catch (Exception e)
		{
			return "";
		}
	}

	public static String decode(String nt)
	{
		if (nt == null)
		{
			return "";
		}
		try
		{
			return URLDecoder.decode(nt, "UTF-8");
		}
		catch (Exception e)
		{
			return "";
		}
	}

	public static final String makeUrlSafe(String s)
	{
		String result = "";
		String encoded = encodeURIComponent(s.toLowerCase());
		boolean nextCharUpperCase = false;
		for (int i = 0; i < encoded.length(); i++)
		{
			if (encoded.charAt(i) == '%')
			{
				i += 2;
				continue;
			}
			if (encoded.charAt(i) == '+')
			{
				nextCharUpperCase = true;
				continue;
			}
			if (nextCharUpperCase)
			{
				String c = encoded.substring(i, i + 1);
				result += c.toUpperCase();
				nextCharUpperCase = false;
				continue;
			}
			result += encoded.charAt(i);
		}
		return result;
	}

	/**
	 * returns an int array for a number sequence formatted with commas and
	 * dashes. example: 1,2,3 or 1-10,16,18
	 * 
	 * @param numberString
	 * @return
	 */
	public static final int[] getNumbers(String numberString)
	{
		if (numberString == null)
			return new int[0];
		List<String> numbers = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(numberString, ",");
		while (st.hasMoreTokens())
		{
			String t = st.nextToken();
			int i = t.indexOf("-");
			if (i != -1)
			{
				int s = toInt(t.substring(0, i));
				int e = toInt(t.substring(i + 1)) + 1;
				for (int j = s; j < e; j++)
				{
					numbers.add(Integer.toString(j));
				}
			}
			else
			{
				numbers.add(t);
			}
		}
		int[] intArray = new int[numbers.size()];
		for (int i = 0; i < numbers.size(); i++)
		{
			intArray[i] = toInt(numbers.get(i));
		}
		return intArray;
	}

	/**
	 * Read an InputStream, return a String.
	 * 
	 * @param is
	 * @return
	 */
	public static String read(InputStream is)
	{
		StringBuffer sb = new StringBuffer();
		int c;
		try
		{
			while ((c = is.read()) != -1)
			{
				sb.append((char) c);
			}
		}
		catch (IOException e)
		{
			logger.error("read(InputStream)", e); //$NON-NLS-1$
			return null;
		}
		return sb.toString();
	}

	public static String stripTags(String s)
	{
		if(s == null)
			return null;
		StringBuffer b = new StringBuffer();
		boolean open = false;
		for (int i = 0; i < s.length(); i++)
		{
			if (s.charAt(i) == '<')
			{
				open = true;
			}
			if (!open)
			{
				b.append(s.charAt(i));
			}
			if (s.charAt(i) == '>')
			{
				open = false;
				b.append(" ");
			}
		}
		return b.toString().trim();
	}
	
	

	public static String shorten(String string, int i)
	{
		if (string.length() < i)
		{
			return string;
		}
		return string.subSequence(0, i) + "...";
	}

	public static String removeNonNumerics(String s)
	{
		StringBuffer b = new StringBuffer();
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (c > 47 && c < 58)
				b.append(c);
		}
		return b.toString();
	}

	
	
	// ///
	public static String[][] MONTHS = { { "1", "Jan" }, { "2", "Feb" }, { "3", "Mar" }, { "4", "Apr" }, { "5", "May" }, { "6", "Jun" }, { "7", "Jul" }, { "8", "Aug" }, { "9", "Sep" }, { "10", "Oct" }, { "11", "Nov" }, { "12", "Dec" } };
	//
	public static String[][] COUNTRIES = { { "US", "United States" }, { "AF", "Afghanistan" }, { "AL", "Albania" }, { "DZ", "Algeria" }, { "AS", "American Samoa" }, { "AD", "Andorra" }, { "AO", "Angola" }, { "AI", "Anguilla" }, { "AG", "Antigua and Barbuda" }, { "AR", "Argentina" }, { "AM", "Armenia" }, { "AW", "Aruba" }, { "AU", "Australia" }, { "AT", "Austria" }, { "AZ", "Azerbaijan" }, { "BS", "Bahamas" }, { "BH", "Bahrain" }, { "BD", "Bangladesh" }, { "BB", "Barbados" }, { "BY", "Belarus" }, { "BE", "Belgium" }, { "BZ", "Belize" }, { "BJ", "Benin" }, { "BM", "Bermuda" }, { "BT", "Bhutan" }, { "BO", "Bolivia" }, { "BA", "Bosnia and Herzegovina" }, { "BW", "Botswana" }, { "BV", "Bouvet Island" }, { "BR", "Brazil" }, { "IO", "British Indian Ocean Territory" }, { "VG", "British Virgin Islands" }, { "BN", "Brunei" }, { "BG", "Bulgaria" }, { "BF", "Burkina Faso" }, { "BI", "Burundi" }, { "KH", "Cambodia" }, { "CM", "Cameroon" }, { "CA", "Canada" }, { "CV", "Cape Verde" }, { "KY", "Cayman Islands" }, { "CF", "Central African Republic" }, { "TD", "Chad" }, { "CL", "Chile" }, { "CN", "China" }, { "CX", "Christmas Island" }, { "CC", "Cocos (Keeling) Islands" }, { "CO", "Colombia" }, { "KM", "Comoros" }, { "CG", "Congo" }, { "CD", "Congo - Democratic Republic of" }, { "CK", "Cook Islands" }, { "CR", "Costa Rica" }, { "CI", "Cote d'Ivoire" }, { "HR", "Croatia" }, { "CU", "Cuba" }, { "CY", "Cyprus" }, { "CZ", "Czech Republic" }, { "DK", "Denmark" }, { "DJ", "Djibouti" }, { "DM", "Dominica" }, { "DO", "Dominican Republic" }, { "TP", "East Timor" }, { "EC", "Ecuador" }, { "EG", "Egypt" }, { "SV", "El Salvador" }, { "GQ", "Equitorial Guinea" }, { "ER", "Eritrea" }, { "EE", "Estonia" }, { "ET", "Ethiopia" }, { "FK", "Falkland Islands (Islas Malvinas)" }, { "FO", "Faroe Islands" }, { "FJ", "Fiji" }, { "FI", "Finland" }, { "FR", "France" }, { "GF", "French Guyana" }, { "PF", "French Polynesia" }, { "TF", "French Southern and Antarctic Lands" }, { "GA", "Gabon" }, { "GM", "Gambia" }, { "GZ", "Gaza Strip" }, { "GE", "Georgia" }, { "DE", "Germany" }, { "GH", "Ghana" }, { "GI", "Gibraltar" }, { "GR", "Greece" }, { "GL", "Greenland" }, { "GD", "Grenada" }, { "GP", "Guadeloupe" }, { "GU", "Guam" }, { "GT", "Guatemala" }, { "GN", "Guinea" }, { "GW", "Guinea-Bissau" }, { "GY", "Guyana" }, { "HT", "Haiti" }, { "HM", "Heard Island and McDonald Islands" }, { "VA", "Holy See (Vatican City)" }, { "HN", "Honduras" }, { "HK", "Hong Kong" }, { "HU", "Hungary" }, { "IS", "Iceland" }, { "IN", "India" }, { "ID", "Indonesia" }, { "IR", "Iran" }, { "IQ", "Iraq" }, { "IE", "Ireland" }, { "IL", "Israel" }, { "IT", "Italy" }, { "JM", "Jamaica" }, { "JP", "Japan" }, { "JO", "Jordan" }, { "KZ", "Kazakhstan" }, { "KE", "Kenya" }, { "KI", "Kiribati" }, { "KW", "Kuwait" }, { "KG", "Kyrgyzstan" }, { "LA", "Laos" }, { "LV", "Latvia" }, { "LB", "Lebanon" }, { "LS", "Lesotho" }, { "LR", "Liberia" }, { "LY", "Libya" }, { "LI", "Liechtenstein" }, { "LT", "Lithuania" }, { "LU", "Luxembourg" }, { "MO", "Macau" }, { "MK", "Macedonia" }, { "MG", "Madagascar" }, { "MW", "Malawi" }, { "MY", "Malaysia" }, { "MV", "Maldives" }, { "ML", "Mali" }, { "MT", "Malta" }, { "MH", "Marshall Islands" }, { "MQ", "Martinique" }, { "MR", "Mauritania" }, { "MU", "Mauritius" }, { "YT", "Mayotte" }, { "MX", "Mexico" }, { "FM", "Micronesia " }, { "MD", "Moldova" }, { "MC", "Monaco" }, { "MN", "Mongolia" }, { "MS", "Montserrat" }, { "MA", "Morocco" }, { "MZ", "Mozambique" }, { "MM", "Myanmar" }, { "NA", "Namibia" }, { "NR", "Naura" }, { "NP", "Nepal" }, { "NL", "Netherlands" }, { "AN", "Netherlands Antilles" }, { "NC", "New Caledonia" }, { "NZ", "New Zealand" }, { "NI", "Nicaragua" }, { "NE", "Niger" }, { "NG", "Nigeria" }, { "NU", "Niue" }, { "NF", "Norfolk Island" }, { "KP", "North Korea" }, { "MP", "Northern Mariana Islands" }, { "NO", "Norway" }, { "OM", "Oman" }, { "PK", "Pakistan" }, { "PW", "Palau" }, { "PA", "Panama" }, { "PG", "Papua New Guinea" }, { "PY", "Paraguay" }, { "PE", "Peru" }, { "PH", "Philippines" }, { "PN", "Pitcairn Islands" }, { "PL", "Poland" }, { "PT", "Portugal" }, { "PR", "Puerto Rico" }, { "QA", "Qatar" }, { "RE", "Reunion" }, { "RO", "Romania" }, { "RU", "Russia" }, { "RW", "Rwanda" }, { "KN", "Saint Kitts and Nevis" }, { "LC", "Saint Lucia" }, { "VC", "Saint Vincent and the Grenadines" }, { "WS", "Samoa" }, { "SM", "San Marino" }, { "ST", "Sao Tome and Principe" }, { "SA", "Saudi Arabia" }, { "SN", "Senegal" }, { "CS", "Serbia and Montenegro" }, { "SC", "Seychelles" }, { "SL", "Sierra Leone" }, { "SG", "Singapore" }, { "SK", "Slovakia" }, { "SI", "Slovenia" }, { "SB", "Solomon Islands" }, { "SO", "Somalia" }, { "ZA", "South Africa" }, { "GS", "South Georgia" }, { "KR", "South Korea" }, { "ES", "Spain" }, { "LK", "Sri Lanka" }, { "SH", "St. Helena" }, { "PM", "St. Pierre and Miquelon" }, { "SD", "Sudan" }, { "SR", "Suriname" }, { "SJ", "Svalbard" }, { "SZ", "Swaziland" }, { "SE", "Sweden" }, { "CH", "Switzerland" }, { "SY", "Syria" }, { "TW", "Taiwan" }, { "TJ", "Tajikistan" }, { "TZ", "Tanzania" }, { "TH", "Thailand" }, { "TG", "Togo" }, { "TK", "Tokelau" }, { "TO", "Tonga" }, { "TT", "Trinidad and Tobago" }, { "TN", "Tunisia" }, { "TR", "Turkey" }, { "TM", "Turkmenistan" }, { "TC", "Turks and Caicos Islands" }, { "TV", "Tuvalu" }, { "UG", "Uganda" }, { "UA", "Ukraine" }, { "AE", "United Arab Emirates" }, { "GB", "United Kingdom" }, { "VI", "United States Virgin Islands" }, { "UY", "Uruguay" }, { "UZ", "Uzbekistan" }, { "VU", "Vanuatu" }, { "VE", "Venezuela" }, { "VN", "Vietnam" }, { "WF", "Wallis and Futuna" }, { "PS", "West Bank" }, { "EH", "Western Sahara" }, { "YE", "Yemen" }, { "ZM", "Zambia" }, { "ZW", "Zimbabwe" } };
}
