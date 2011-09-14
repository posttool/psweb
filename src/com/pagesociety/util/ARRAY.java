package com.pagesociety.util;

import java.util.ArrayList;

import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.WebModule;

public class ARRAY extends ArrayList<Object>
{
	public ARRAY(Object... args)
	{
		for(int i = 0;i < args.length;i++)
			add(args[i]);
	}
	
	public String toString()
	{
		try
		{
			return WebModule.ENCODE(this);
		} catch (WebApplicationException e)
		{
			return e.getMessage();
		}
	}
}