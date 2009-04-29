package com.pagesociety.web.module;

import java.util.HashMap;
import java.util.Map;

public class ModuleEvent 
{
	public  int 			   type;
	private Map<String,Object> event_context = new HashMap<String,Object>();
	
	public ModuleEvent(int type,Object... key_value_pairs_for_context)
	{
		this(type,new HashMap<String,Object>());
		for(int i = 0;i < key_value_pairs_for_context.length;i+=2)
			set_property((String)key_value_pairs_for_context[i],key_value_pairs_for_context[i+1]);
		
	}
	
	public ModuleEvent(int type,Map<String,Object> event_context)
	{
		this.type 		   = type;
		this.event_context = event_context;
	}
	
	public Object getProperty(String prop)
	{
		return event_context.get(prop);
	}
	
	private void set_property(String prop,Object val)
	{
		event_context.put(prop,val);
	}
	
	
	public String toString()
	{
		return "EVENT: "+type+"\nCONTEXT: "+event_context;
	}
}
