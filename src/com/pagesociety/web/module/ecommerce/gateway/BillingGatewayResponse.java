package com.pagesociety.web.module.ecommerce.gateway;

import java.util.HashMap;

public class BillingGatewayResponse 
{
	public static final String KEY_REFCODE = "REFCODE";
	public static final String KEY_MESSAGE = "MESSAGE";
	public static final String KEY_AUTHCODE = "AUTHCODE";
	
	private HashMap<String,Object> _props = new HashMap<String,Object>();
	
	public BillingGatewayResponse(String refcode)
	{
		
		_props.put(KEY_REFCODE, refcode);
	}
	
	public BillingGatewayResponse(String refcode,String message)
	{
		_props.put(KEY_REFCODE, refcode);
		_props.put(KEY_MESSAGE, message);
	}
	
	public BillingGatewayResponse(String refcode,String authcode,String message)
	{
		_props.put(KEY_REFCODE, refcode);
		_props.put(KEY_MESSAGE, message);
		_props.put(KEY_AUTHCODE, authcode);
	}
	
	public String getRefCode()
	{
		return (String)_props.get(KEY_REFCODE);
	}

	public String getAuthCode()
	{
		return (String)_props.get(KEY_AUTHCODE);
	}

	public String getMessage()
	{
		return (String)_props.get(KEY_MESSAGE);
	}

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
