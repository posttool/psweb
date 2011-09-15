package com.pagesociety.util;

import java.io.IOException;
import java.io.Serializable;
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
	
	public OBJECT O(String name)
	{
		return (OBJECT)get(name);
	}
	public ARRAY A(String name)
	{
		return (ARRAY)get(name);
	}
	public Object find(String path)
	{
		return find(this,path);
	}
	
	public static Object find(OBJECT o, String path)
	{
		Object target = o;
		String[] p = path.split("\\.");
		for (String pp : p)
		{
			int array_char_idx = pp.indexOf("[");
			if (array_char_idx != -1 && pp.endsWith("]"))
			{
				int idx = Integer.parseInt(pp.substring(array_char_idx+1, pp.length()-1));
				ARRAY ta = ((OBJECT) target).A(pp.substring(0,array_char_idx));
				target = ta.get(idx);
			}
			else
			{
				target = ((OBJECT) target).get(pp);
			}
		}
		return target;
	}
	
	public static String encode(Serializable o) throws WebApplicationException
	{
		try
		{
			return Base64.encodeObject(o);
		} catch (IOException e)
		{
			throw new WebApplicationException("Can't encode",e);
		}
	}
	
	public static OBJECT decode(String s) throws WebApplicationException
	{
		if (s==null)
			return null;
		try
		{
			return (OBJECT) Base64.decodeToObject(s);
		} catch (Exception e)
		{
			throw new WebApplicationException("Can't decode",e);
		}
	}
	
	public static void main(String[] args)
	{
		OBJECT o = new OBJECT(
				"x",new ARRAY(1,2,"you will"), 
				"y", new OBJECT(
					"a$a", 17,
					"b$b", new OBJECT(
						"start", 1010101,
						"end", "sometimes"
					),
					"ccc", new ARRAY("wonder","get me", new OBJECT("or","stop")
					)
				));
		System.out.println(o.find("x[2]"));
		System.out.println(o.find("y.b$b.end"));
		System.out.println(o.find("y.ccc[2].or"));
		
	}

}