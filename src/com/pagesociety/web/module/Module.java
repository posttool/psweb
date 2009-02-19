package com.pagesociety.web.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SlotException;


public abstract class Module
{
	protected WebApplication 		_application;
	protected Map<String, Object> 	_config;
	protected String 				_name;
	
	/* slot management stuff ...called as a course of bootstrap */
	private Map<String,SlotDescriptor>  _slot_descriptor_map  = new HashMap<String,SlotDescriptor>();
	private List<SlotDescriptor>  		_slot_descriptor_list = new ArrayList<SlotDescriptor>();
	private Map<String,Object>   		_slot_instance_map    = new HashMap<String,Object>();
	private List<IEventListener>		_event_listeners	  = new ArrayList<IEventListener>();

	private List<Class<?>> dependencies = new ArrayList<Class<?>>();
	public List<Class<?>> dependencies()
	{
		return dependencies;
	}
	
	public void system_init(WebApplication app, Map<String, Object> config) throws InitializationException
	{

	}
	
	public void pre_init(WebApplication app, Map<String, Object> config) throws InitializationException
	{
	
	}
	
	public void init(WebApplication app, Map<String, Object> config) throws InitializationException
	{
		_application = app;
		_config = config;
	}

	public void setup_slots() throws InitializationException
	{
		defineSlots(); 
	}
	
	public String getName()
	{
		return _name;
	}
	
	/* this is called when the module is bootstrapped */
	public void setName(String name)
	{
		_name = name;
	}
	
	
	public WebApplication getApplication()
	{
		return _application;
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

/* slot stuff */
	protected void defineSlots()
	{
		//do nothing by default//
	}
	
	protected void defineSlot(String slot_name,Class<?> slot_type,boolean required)
	{
		defineSlot(slot_name, slot_type, required, null);
	}
	
	protected void defineSlot(String slot_name,Class<?> slot_type,boolean required,Object default_slot_val)
	{
		SlotDescriptor d = new SlotDescriptor();
		d.slot_name = slot_name;
		d.slot_type = slot_type;
		d.required  = required;
		d.default_slot_val = default_slot_val;
		_slot_descriptor_map.put(slot_name,d);
		_slot_descriptor_list.add(d);
	}

	public List<SlotDescriptor> getSlotDescriptors()
	{
		return _slot_descriptor_list;
	}
	
	public SlotDescriptor getSlotDescriptor(String slot_name)
	{
		return _slot_descriptor_map.get(slot_name);
	}
	
	public void setSlot(String slot_name,Object slot_instance) throws SlotException
	{
		SlotDescriptor d = _slot_descriptor_map.get(slot_name);
		if(d == null)
			throw new SlotException("NO SLOT NAMED "+slot_name);
		if(slot_instance == null)
		{
			if(_slot_instance_map.get(slot_name) != null)
				_slot_instance_map.remove(slot_name);
		}
		Class<?> c = d.slot_type;
		try{
			_slot_instance_map.put(slot_name,c.cast(slot_instance));
		}catch(ClassCastException cce)
		{
			throw new SlotException("SLOT "+slot_name+" IN MODULE "+getName()+"EXPECTS AN INSTANCE OF "+c.getName()+". YOU PROVIDED AN INSTANCE OF "+slot_instance.getClass().getName());
		}
	}
	
	public Object getSlot(String slot_name)
	{
		return _slot_instance_map.get(slot_name);
	}
	
	public class SlotDescriptor
	{
		public String 	slot_name;
		public Class<?> slot_type;
		public boolean 	required;
		public Object default_slot_val;/* if not required can have a default */
	}


	public void addEventListener(IEventListener listener)
	{
		_event_listeners.add(listener);
	}
	
	public void removeEventListener(IEventListener listener)
	{
		_event_listeners.remove(listener);
	}
	
	public void dispatchEvent(ModuleEvent e)
	{
		int s = _event_listeners.size();
		for(int i = 0;i < s;i++)
		{
			IEventListener l = _event_listeners.get(i);
			l.onEvent(this,e);
		}
	}

	private boolean is_initialized = false;
	public void setInitialized(boolean b)
	{
		is_initialized = b;
	}
	public boolean isInitialized()
	{
		return is_initialized;
	}

}
