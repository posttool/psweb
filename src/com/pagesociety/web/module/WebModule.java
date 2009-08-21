package com.pagesociety.web.module;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.PermissionsException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.permissions.DefaultPermissionsModule;
import com.pagesociety.web.module.permissions.PermissionEvaluator;
import com.pagesociety.web.module.permissions.PermissionsModule;



public abstract class WebModule extends Module
{
	public static final String SLOT_PERMISSIONS_MODULE  = "permissions-module";
	protected PermissionsModule permissions;
	
	public List<?> EMPTY_LIST = new ArrayList<Object>();
	
	public void system_init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.system_init(app, config);
		permissions = (PermissionsModule)getSlot(SLOT_PERMISSIONS_MODULE);
		exportPermissions();		
	}
	
	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);
	}
	
	
	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_PERMISSIONS_MODULE, PermissionsModule.class, false,getApplication().getDefaultPermissionsModule());	
	}

	protected void exportPermissions()
	{
		//do nothing by default//
	}
	
	protected void EXPORT_PERMISSION(String permission_id)
	{
		permissions.definePermission(getName(), permission_id);
	}
	
	protected void DEFINE_SLOT(String slot_name,Class<?> slot_type,boolean required)
	{
		super.defineSlot(slot_name, slot_type, required);
	}
	
	protected void DEFINE_SLOT(String slot_name,Class<?> slot_type,boolean required,Object default_val)
	{
		super.defineSlot(slot_name, slot_type, required,default_val);
	}
	
	public String GET_REQUIRED_CONFIG_PARAM(String name,Map<String,Object> config) throws InitializationException
	{
		Object val = config.get(name);
		if(val == null)
			throw new InitializationException("MISSING REQUIRED CONFIG PARAM..."+name);
		return (String)val;
	}

	public String GET_OPTIONAL_CONFIG_PARAM(String name,Map<String,Object> config) throws InitializationException
	{
		Object val = config.get(name);
		return (String)val;
	}

	
	protected void INFO(String message)
	{
		_application.INFO(getName()+": "+message);
	}
	
	protected void WARNING(String message)
	{
		_application.WARNING(getName()+": "+message);
	}
	
	protected void ERROR(String message)
	{
		_application.ERROR(getName()+": "+message);
	}

	protected void ERROR(Exception e)
	{
		_application.ERROR(e);
	}
	
	protected void ERROR(String message,Exception e)
	{
		_application.ERROR(getName()+": "+message,e);
	}
	
	protected void WAE(Exception e) throws WebApplicationException
	{
		throw new WebApplicationException(e.getMessage(),e);
	}

	protected String GUARD_INSTANCE = "instance";
	protected String GUARD_TYPE		= "entity_type";
	protected String GUARD_USER		= "user";
	protected void GUARD(Entity user,String permission_id,Object... flattened_context) throws PermissionsException,PersistenceException
	{
		Map<String,Object> context = new HashMap<String, Object>();
		for(int i = 0;i < flattened_context.length;i+=2)
			context.put((String)flattened_context[i], flattened_context[i+1]);
		
		boolean b = permissions.checkPermission(user, getName(), permission_id, context);
		if(!b)
			throw new PermissionsException("NO PERMISSION");
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
	
	protected  File GET_MODULE_DATA_DIRECTORY(WebApplication app)
	{
		File f =  new File(app.getConfig().getModuleDataDirectory(),getName()+"Data");
		if(!f.exists())
		{
			INFO("CREATING DATA DIRECTORY FOR MODULE: "+getName()+"\n\t"+f.getAbsolutePath());
			f.mkdirs();
		}
		return f;
	}
	
	protected  File GET_MODULE_DATA_FILE(WebApplication app,String filename,boolean create) throws WebApplicationException
	{
		File data_dir  =  GET_MODULE_DATA_DIRECTORY(app);
		File data_file = new File(data_dir,filename);
		if(!data_file.exists() && create)
		{
			System.out.println("CREATING DATA FILE FOR MODULE: "+getName()+"\n\t"+data_file.getAbsolutePath());
			CREATE_MODULE_DATA_FILE(app,filename);
		}
		else if(!data_file.exists() && ! create)
		{
			return null;
		}
		return data_file;
	}

	
	protected  FileReader GET_MODULE_DATA_FILE_READER(WebApplication app,String filename,boolean create) throws WebApplicationException
	{
		File data_dir  =  GET_MODULE_DATA_DIRECTORY(app);
		File data_file = new File(data_dir,filename);
		if(!data_file.exists() && create)
		{
			System.out.println("CREATING DATA FILE FOR MODULE: "+getName()+"\n\t"+data_file.getAbsolutePath());
			CREATE_MODULE_DATA_FILE(app,filename);
		}
		else if(!data_file.exists() && ! create)
		{
			return null;
		}
		FileReader ret;
		try{
			ret =  new FileReader(data_file);
		}catch(FileNotFoundException fnfe)
		{
			throw new WebApplicationException("COULD NOT OPEN READER FOR MODULE DATA FILE "+data_file.getAbsolutePath());
		}
		return ret;
	}

	protected File CREATE_MODULE_DATA_FILE(WebApplication app,String filename) throws WebApplicationException
	{
		File data_dir  =  GET_MODULE_DATA_DIRECTORY(app);
		File data_file = new File(data_dir,filename);
		try{
			data_file.createNewFile();
		}catch(IOException ioe)
		{
			throw new WebApplicationException("FAILED CREATING MODULE FILE "+data_file.getAbsolutePath());
		}
		return data_file;
	}
	

	protected static String GET_CONSOLE_INPUT(String prompt)throws WebApplicationException
	{
		return GET_CONSOLE_INPUT(5, prompt);
	}
	
	protected static String GET_CONSOLE_INPUT(int num_times,String prompt)throws WebApplicationException
	{
		while(num_times > 0)
		{
			if(prompt != null)
			{
				System.out.print(prompt);
				System.out.flush();
			}
	    
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String input = "";

			try{
				input = in.readLine();
			}catch(IOException ioe)
	        {
	    	   ioe.printStackTrace();
	    	   throw new WebApplicationException("ERROR READING CONSOLE INPUT "+ioe.getMessage());
	        }
			if(input == null || input.equals(""))
				num_times--;
			else
				return input;
		}
		throw new WebApplicationException("THE APP REQUIRES CONSOLE INPUT TO STARTUP.");
	}
	
	//string functions//
	protected String REMOVE_WHITE_SPACE(String s)
	{
		StringBuilder buf = new StringBuilder();
		byte[] bb = s.getBytes();
		for(int i=0;i < bb.length;i++)
		{
			char c = (char)bb[i];
			if(Character.isWhitespace(c))
				continue;
			else
				buf.append(c);
		}
		return buf.toString();
	}
	
	protected String[] GET_REQUIRED_LIST_PARAM(String name,Map<String,Object> config) throws InitializationException
	{
		String p = GET_REQUIRED_CONFIG_PARAM(name, config);
		p = REMOVE_WHITE_SPACE(p);
		return p.split(",");
	}
	
	
	protected void DISPATCH_EVENT(int event_type,Object... event_context) throws WebApplicationException
	{
		dispatchEvent(new ModuleEvent(event_type,event_context));
	}
	
	protected void DISPATCH_EVENT(int event_type,Map<String,Object> event_context) throws WebApplicationException
	{
		dispatchEvent(new ModuleEvent(event_type,event_context));
	}
	
	//EXPERIMENTAL currently used by recurring order module//

	File   current_log_file;
	Writer current_log_writer;
	private static final String LOG_EXTENSION = "log";
	private Writer get_current_log_file_writer()
	{
		Calendar now = Calendar.getInstance();
		
		int n_month = now.get(Calendar.MONTH)+1; 
		int n_day   = now.get(Calendar.DATE); 
		String year  = String.valueOf(now.get(Calendar.YEAR));
		String month = String.valueOf(n_month);
		String day   = String.valueOf(n_day);
		
		StringBuilder buf = new StringBuilder();
		buf.append(year);
		if(n_month < 10)
			buf.append('0');
		buf.append(month);
		if(n_day < 10)
			buf.append('0');
		buf.append(day);
		buf.append('.');
		buf.append(getName());
		buf.append('.');
		buf.append(LOG_EXTENSION);
		String current_log_filename = buf.toString();
		
		try{
			current_log_file = GET_MODULE_DATA_FILE(getApplication(), current_log_filename, false);
			if(current_log_file == null || current_log_writer == null)
			{
				current_log_file = GET_MODULE_DATA_FILE(getApplication(), current_log_filename, true);
				try{
					if(current_log_writer != null)
						current_log_writer.close();
					current_log_writer = new BufferedWriter(new FileWriter(current_log_file,true));
				}catch(IOException ioe)
				{
					ERROR("BIG TIME BARF ON LOG FILE SWITCHING.",ioe);
				}
			}
		}catch(WebApplicationException wae)
		{
			ERROR("PROBLEM GETTING CURRENT LOG FILE "+current_log_filename);
		}
		return current_log_writer;
	}
	
	protected void MODULE_LOG(String message)
	{
		MODULE_LOG(0, message);
    }

	protected void MODULE_LOG(int indent,String message)
	{
		Writer output = get_current_log_file_writer(); 
		Date now = new Date();
		
		try {
			if(message.startsWith("\n"))
			{
				output.write('\n');
				message = message.substring(1);
			}
			output.write(now+": ");
			for(int i = 0;i < indent;i++)
				output.write('\t');
			output.write(message+"\n");
			output.flush();
		}catch(IOException ioe)
		{
			ERROR("BARF ON MODULE_LOG() FUNCTION.MESSAGE WAS "+message,ioe);
		}
    }
	
	
	//just another name for a Map<String,Object> useful for talking to action script
	public class OBJECT extends HashMap<String,Object>
	{
		public OBJECT(Object... args)
		{
			for(int i = 0;i < args.length;i+=2)
				put((String)args[i],args[i+1]);			
		}
	}

	
}
