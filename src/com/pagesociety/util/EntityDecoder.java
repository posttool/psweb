package com.pagesociety.util;

import java.util.HashMap;
import java.util.Map;

public class EntityDecoder
{
	public static final String unescapeHTML(String s)
	{
		Map<String, String> escape = new HashMap<String, String>();
		escape.put("<", "<");
		escape.put(">", ">");
		escape.put("&", "&");
		escape.put("\"", "\"");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put("�", "�");
		escape.put(" ", " ");
		escape.put("�", "\u00a9");
		escape.put("�", "\u00ae");
		escape.put("�", "\u20a0");
		int i, j, k, l;
		i = s.indexOf("&");
		if (i > -1)
		{
			j = s.indexOf(";");
			if (j > i)
			{
				// ok this is not most optimized way to
				// do it, a StringBuffer would be better,
				// this is left as an exercise to the reader!
				String temp = s.substring(i, j + 1);
				// search for temp is there
				if (escape.containsKey(temp))
				{
					s = s.substring(0, i) + (String) escape.get(temp) + s.substring(j + 1);
					return unescapeHTML(s); // recursive call
				}
				// ok must be escaped unicode
				if (temp.length() == 7)
				{
					try{
					s = s.substring(0, i) + (char) Integer.parseInt(temp.substring(2, 6)) + s.substring(j + 1);
					}catch(NumberFormatException e)
					{
						System.out.println("BARF WAS "+temp.substring(2, 6));
						throw e;
					}
					return unescapeHTML(s); // recursive call
				}
			}
		}
		return s;
	}
}
