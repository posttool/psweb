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
		int s = size();
		StringBuilder b = new StringBuilder();
		b.append("[");
		for (int i = 0; i < s; i++)
		{
			b.append(get(i));
			if (i!=s-1)
				b.append(", ");
		}
		b.append("]");
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