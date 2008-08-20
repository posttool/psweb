package com.pagesociety.util;

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.pagesociety.web.upload.MultipartFormConstants;

/**
 * Converts ISO-8859-1 strings to UTF-8
 */
public class HttpFormDecoder
{
	public static final String ISO_8859_1 = "ISO-8859-1";
	public static final String UTF_8 = "UTF-8";

	@SuppressWarnings("unchecked")
	public static Map<String, String> decode(HttpServletRequest request)
			throws Exception
	{
		String contentType = request.getHeader(MultipartFormConstants.CONTENT_TYPE);
		boolean multipart = (contentType != null && contentType.indexOf(MultipartFormConstants.MULTI_PART) != -1);
		if (multipart)
		{
			throw new RuntimeException("decodeFields doesn't work with multipart");
		}
		Map<String, String> map = new HashMap<String, String>();
		Enumeration<String> e = request.getParameterNames();
		while (e.hasMoreElements())
		{
			String name = e.nextElement();
			map.put(name, decode(request.getParameter(name)));
		}
		return map;
	}

	public static String decode(String param)
	{
		byte[] bytes;
		try
		{
			bytes = param.getBytes(ISO_8859_1);
		}
		catch (UnsupportedEncodingException e1)
		{
			return param;
		}
		try
		{
			return new String(bytes, UTF_8);
		}
		catch (UnsupportedEncodingException e)
		{
			return param;
		}
	}
}
