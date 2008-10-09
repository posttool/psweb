package com.pagesociety.web.module;


import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.AuthenticationException;
import com.pagesociety.web.InitializationException;
import com.pagesociety.web.PermissionsException;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.WebApplicationException;


public abstract class WebModule extends Module
{

	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);
	}
	
	protected void DEFINE_SLOT(String slot_name,Class<?> slot_type,boolean required)
	{
		super.defineSlot(slot_name, slot_type, required);
	}
	
	protected void DEFINE_SLOT(String slot_name,Class<?> slot_type,boolean required,Class<?> default_implementation)
	{
		super.defineSlot(slot_name, slot_type, required,default_implementation);
	}
	
	public String GET_REQUIRED_CONFIG_PARAM(String name,Map<String,Object> config) throws InitializationException
	{
		Object val = config.get(name);
		if(val == null)
			throw new InitializationException("MISSING REQUIRED CONFIG PARAM..."+name);
		return (String)val;
	}
	
	
	protected static void LOG(String message)
	{
		System.out.println(message);
	}
	
	protected static void ERROR(String message)
	{
		System.err.println(message);
	}

	protected static void ERROR(Exception e)
	{
		e.printStackTrace();
	}
	
	protected static void ERROR(String message,Exception e)
	{
		System.err.println(message);
		e.printStackTrace();
	}

	protected static void GUARD(boolean b) throws PermissionsException
	{
		try{
			if(b)
				return;
			else
				throw new PermissionsException("INADEQUATE PERMISSIONS");
		}catch(PermissionsException pe)
		{/* if permissions exception happens in guard just forward it */
			throw pe;
		}
		
	}

	
}
