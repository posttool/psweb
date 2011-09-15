package com.pagesociety.util;

import java.util.ArrayList;

public class ARRAY extends ArrayList<Object>
{
	private static final long serialVersionUID = -9108744819972018258L;

	public ARRAY(Object... args)
	{
		for (int i = 0; i < args.length; i++)
			add(args[i]);
	}

	public String toString()
	{
		StringBuilder b = new StringBuilder();
		b.append("[\n");
		for (int i = 0; i < size(); i++)
		{
			b.append(get(i));
			b.append("\n");
		}
		b.append("]\n");
		return b.toString();
	}

	public OBJECT O(int idx)
	{
		return (OBJECT) get(idx);
	}

	public ARRAY getarr(int idx)
	{
		return (ARRAY) get(idx);
	}
}