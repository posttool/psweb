package com.pagesociety.util;

import java.awt.Color;
import java.util.StringTokenizer;

public class ColorParser
{
	public static Color getColor(String c)
	{
		if (c == null)
			return null;
		Color clr = null;
		if (c.startsWith("rgb"))
		{
			int[] rgb = new int[4];
			int count = 0;
			StringTokenizer st = new StringTokenizer(c, "(,)");
			while (st.hasMoreTokens() && count < 4)
			{
				int v = Text.toInt(st.nextToken());
				if (v != -1)
				{
					rgb[count] = v;
					count++;
				}
			}
			if (count == 3)
			{
				clr = new Color(rgb[0], rgb[1], rgb[2]);
			}
			else if (count == 4)
			{
				clr = new Color(rgb[0], rgb[1], rgb[2], rgb[3]);
			}
		}
		else
		{
			if (c.startsWith("#"))
			{
				c = c.substring(1);
			}
			c = c.toLowerCase();
			try
			{
				if (c.length() < 7)
					clr = new Color(Integer.parseInt(c, 16));
				else
					clr = new Color(Integer.parseInt(c, 16), true);
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
				return null;
			}
		}
		return clr;
	}
}
