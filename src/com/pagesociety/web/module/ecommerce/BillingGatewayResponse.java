package com.pagesociety.web.module.ecommerce;

import java.util.HashMap;

public class BillingGatewayResponse 
{
	private HashMap<String,Object> _props;
	
	public void setProperty(String name,Object value)
	{
		_props.put(name,value);
	}
	
	public Object getProperty(String name)
	{
		return _props.get(name);
	}
	
	public String getPropertyAsString(String name)
	{
		return (String)_props.get(name);
	}
	
	public Integer getPropertyAsInt(String name)
	{
		return (Integer)_props.get(name);
	}
	
	public Float getPropertyAsFloat(String name)
	{
		return (Float)_props.get(name);
	}
	
	
}
