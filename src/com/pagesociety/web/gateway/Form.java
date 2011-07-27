package com.pagesociety.web.gateway;

import java.util.Map;
import java.util.Set;

public class Form
{
	private Map<String, String[]> _map;

	public Form(Map<String, String[]> parameterMap)
	{
		_map = parameterMap;
	}

	public String getParameter(String key)
	{
		String[] s = _map.get(key);
		if (s != null && s.length != 0)
			return s[0];
		else
			return null;
	}

	public String[] getParameters(String key)
	{
		return _map.get(key);
	}


	public Set<String> keys()
	{

			return _map.keySet();
	}
}
