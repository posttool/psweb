package com.pagesociety.web.module;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;


import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.PermissionsException;
import com.pagesociety.web.exception.WebApplicationException;


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
	
	
	protected void LOG(String message)
	{
		_application.LOG(getName()+": "+message);
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
		File f =  new File(app.getConfig().getWebRootDir()+File.separator+".."+File.separator+"ModuleData"+File.separator+getName()+"Data");
		if(!f.exists())
		{
			LOG("CREATING DATA DIRECTORY FOR MODULE: "+getName()+"\n\t"+f.getAbsolutePath());
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
	

	protected String GET_CONSOLE_INPUT(String prompt)throws WebApplicationException
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
	       
	   return input;
	}
}
