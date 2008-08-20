package com.pagesociety.web.module;

import java.util.Map;

import com.pagesociety.web.WebApplication;

public abstract class Module
{
	protected WebApplication _application;
	protected Map<String, Object> _config;

	public void init(WebApplication web_app, Map<String, Object> config)
	{
		_application = web_app;
		_config = config;
	}

	public String getName()
	{
		return getClass().getSimpleName();
	}
	
	public Map<String, Object> getProperties()
	{
		return _config;
	}

	public void setProperty(String key, String value)
	{
		_config.put(key, value);
	}

	public void destroy()
	{
		_application = null;
		_config = null;
	}
}
