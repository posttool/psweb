package com.pagesociety.web.module.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pagesociety.transcode.ImageMagick;
import com.pagesociety.util.FileInfo;
import com.pagesociety.web.exception.WebApplicationException;

public class PreviewUtil
{

	private static final String WIDTH = "width";
	private static final String HEIGHT = "height";
	private static final String TYPE = "type";

	public static void createPreview(File original, File dest, Map<String, String> options) throws WebApplicationException
	{
		int type = FileInfo.getSimpleType(original);
		switch (type)
		{
		case FileInfo.SIMPLE_TYPE_IMAGE:
			createImagePreview(original, dest, options);
			break;
		case FileInfo.SIMPLE_TYPE_DOCUMENT:
			throw new WebApplicationException("DOCUMENT PREVIEW NOT SUPPORTED YET " + original.getAbsolutePath());
		case FileInfo.SIMPLE_TYPE_SWF:
			throw new WebApplicationException("SWF PREVIEW NOT SUPPORTED YET " + original.getAbsolutePath());
		case FileInfo.SIMPLE_TYPE_VIDEO:
			throw new WebApplicationException("VIDEO PREVIEW NOT SUPPORTED YET " + original.getAbsolutePath());
		case FileInfo.SIMPLE_TYPE_AUDIO:
			throw new WebApplicationException("AUDIO PREVIEW NOT SUPPORTED YET " + original.getAbsolutePath());
		default:
			throw new WebApplicationException("UNKNOWN SIMPLE TYPE " + type + " " + original.getAbsolutePath());
		}
	}

	public static void createImagePreview(File original, File dest, Map<String, String> options) throws WebApplicationException
	{
		ImageMagick i = new ImageMagick(original, dest);
		i.setOptions(options);
		try
		{
			process(i.getCmd(), i.getArgs());
		} catch (Exception e)
		{
			throw new WebApplicationException("PROBLEM CREATING PREVIEW " + dest.getAbsolutePath(), e);
		}
	}

	public static String getOptionsSuffix(Map<String, String> options)
	{
		return getOptionsSuffix(options,null);
	}
	public static String getOptionsSuffix(Map<String, String> options, String ext)
	{
		int options_size = options.size();
		if (options.containsKey(TYPE))
		{
			ext = options.get(TYPE);
			options_size--;
		}
		if (options_size == 2 && options.containsKey(WIDTH) && options.containsKey(HEIGHT))
		{
			return '_' + options.get(WIDTH) + 'x' + options.get(HEIGHT) + '.' + ext;
		}
		List<String> sorted_kets = new ArrayList<String>();
		for (String k : options.keySet())
		{
			if (k.equals(TYPE))
				continue;
			sorted_kets.add(k);
		}
		Collections.sort(sorted_kets);
		StringBuilder b = new StringBuilder();
		for (String k : sorted_kets)
		{
			b.append(k);
			b.append('.');
			b.append(options.get(k));
			b.append('_');
		}
		return '_' + b.substring(0, b.length() - 1) + '.' + ext;
	}
	
	public static boolean isLikelyPreview(String filename)
	{
		String regexp1 = ".*\\d+x\\d+(\\.([Pp][Nn][Gg]|[Jj][Pp][Gg]|[Jj][Pp][Ee][Gg]))$"; //old format with extension
		String regexp2 = ".*(_\\w+\\.\\w+)+(\\.([Pp][Nn][Gg]|[Jj][Pp][Gg]|[Jj][Pp][Ee][Gg]))$"; //new options format with extension
		return filename.matches(regexp1) || filename.matches(regexp2);
	}

	public static Map<String, String> getOptions(int width, int height)
	{
		Map<String, String> options = new HashMap<String, String>();
		options.put(WIDTH, Integer.toString(width));
		options.put(HEIGHT, Integer.toString(height));
		return options;
	}

	public static Process EXEC(String cmd, String... args) throws WebApplicationException
	{

		Runtime rt = Runtime.getRuntime();
		String[] cmd_array = new String[1 + args.length];
		cmd_array[0] = cmd;
		for (int i = 0; i < args.length; i++)
		{
			cmd_array[i + 1] = args[i];
		}
		try
		{
			Process pr = rt.exec(cmd_array);
			return pr;
		} catch (IOException e)
		{
			StringBuilder buf = new StringBuilder();
			for (int i = 0; i < cmd_array.length; i++)
			{
				buf.append(cmd_array[i] + " ");
			}
			throw new WebApplicationException("FAILED EXECUTING CMD " + buf.toString());
		}

	}

	public static String process(String cmd, String... args) throws WebApplicationException, IOException
	{
		Process p = EXEC(cmd, args);
		try
		{
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

			if (ret != 0)
				throw new WebApplicationException("FAILED EXECUTING CMD WITH EXIT CODE " + ret);

			return b.toString();
		} catch (InterruptedException ie)
		{
			throw new WebApplicationException("PROCESS WAS INTERRUPTED UNABLE TO GET RETURN CODE");
		}

	}

	public static void main(String[] args)
	{
		Map<String, String> options = new HashMap<String, String>();
		options.put("heya", "45gfd");
		options.put("a", "aaaaaaa");
		options.put("y45gt", "fdst");
		System.out.println(getOptionsSuffix(options, "jpg"));
		options.clear();
		options.put("width", "111");
		options.put("height", "222");
		System.out.println(getOptionsSuffix(options, "png"));
	}
}
