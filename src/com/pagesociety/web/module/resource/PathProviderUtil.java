package com.pagesociety.web.module.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pagesociety.web.exception.WebApplicationException;

public class PathProviderUtil
{
	public static final String HEIGHT = "height";
	public static final String WIDTH = "width";

	public static String getOptionsSuffix(Map<String, String> options)
	{
		if (options.size() == 2 && options.containsKey(WIDTH) && options.containsKey(HEIGHT))
		{
			return '_' + options.get(WIDTH) + 'x' + options.get(HEIGHT);
		}
		List<String> sorted_kets = new ArrayList<String>();
		for (String k : options.keySet())
			sorted_kets.add(k);
		Collections.sort(sorted_kets);
		StringBuilder b = new StringBuilder();
		for (String k : sorted_kets)
		{
			b.append(k);
			b.append('.');
			b.append(options.get(k));
			b.append('_');
		}
		return '_' + b.substring(0, b.length() - 1);
	}
	
	public static Process EXEC(String cmd,String... args) throws WebApplicationException
	{
		
		Runtime rt = Runtime.getRuntime();
		String[] cmd_array = new String[1+args.length];
		cmd_array[0] = cmd;
		for(int i = 0;i < args.length;i++)
		{
			cmd_array[i+1] = args[i];
		}
		try{
			Process pr = rt.exec(cmd_array);
			return pr;
		}catch(IOException e)
		{
			StringBuilder buf = new StringBuilder();
			for(int i = 0;i < cmd_array.length;i++)
			{
				buf.append(cmd_array[i]+" ");
			}
			throw new WebApplicationException("FAILED EXECUTING CMD "+buf.toString());
		}

	}
	
	public static String process(String cmd,String...args) throws WebApplicationException, IOException
	{
		Process p = EXEC(cmd, args);
		try{
			int ret = p.waitFor();
			StringBuilder b = new StringBuilder();
			InputStream is = p.getInputStream();
			byte[] buf = new byte[1024];
			int nr = is.read(buf);
			while (nr != -1)
			{
				b.append(nr);
				nr = is.read(buf);
			}
			
			if(ret != 0)
				throw new WebApplicationException("REWRITE ZONE FIELD PROCESS FAILED WITH EXIT CODE "+ret);
			
			return b.toString();
		}catch(InterruptedException ie)
		{
			throw new WebApplicationException("ZONE REWRITE PROCESS WAS INTERRUPTED UNABLE TO GET RETURN CODE");
		}

	}
	
	
	public static void main(String[] args)
	{
		Map<String, String> options = new HashMap<String,String>();
		options.put("heya", "45gfd");
		options.put("a", "aaaaaaa");
		options.put("y45gt", "fdst");
		System.out.println(getOptionsSuffix(options ));
		options.clear();
		options.put("width", "111");
		options.put("height", "222");
		System.out.println(getOptionsSuffix(options ));
	}
}
