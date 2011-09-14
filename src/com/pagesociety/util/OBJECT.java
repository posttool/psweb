package com.pagesociety.util;

import java.util.HashMap;

import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.WebModule;

public class OBJECT extends HashMap<String,Object>
{
	public OBJECT(Object... args)
	{
		for(int i = 0;i < args.length;i+=2)
			put((String)args[i],args[i+1]);
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