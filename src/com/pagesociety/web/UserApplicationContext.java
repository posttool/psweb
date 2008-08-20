package com.pagesociety.web;

import java.util.HashMap;
import java.util.Map;

public class UserApplicationContext
{
	private Object _id;
	private Object _user;
	private int _inc;
	private Map<String, Object> _props;

	public UserApplicationContext()
	{
		_props = new HashMap<String, Object>();
		_inc = 0;
	}

	public void setId(Object id)
	{
		_id = id;
	}

	public Object getId()
	{
		return _id;
	}

	public void setUser(Object user)
	{
		_user = user;
	}

	public Object getUser()
	{
		return _user;
	}

	public Map<String, Object> getProperties()
	{
		return _props;
	}

	public Object getProperty(String key)
	{
		return _props.get(key);
	}

	public void setProperty(String key, Object value)
	{
		_props.put(key, value);
	}

	public void removeProperty(String key)
	{
		_props.remove(key);
	}

	public void increment()
	{
		_inc++;
	}

	public String toString()
	{
		return "UserApplicationContext { id=" + _id + " user=" + _user + " props=" + _props + " inc=" + _inc + " }";
	}
}
