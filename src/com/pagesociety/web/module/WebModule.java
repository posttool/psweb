package com.pagesociety.web.module;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
	
	public String GET_OPTIONAL_CONFIG_PARAM(String name,String default_val,Map<String,Object> config) throws InitializationException
	{
		Object val = config.get(name);
		if(val == null)
			return default_val;
		return (String)val;
	}

	public int GET_REQUIRED_INT_CONFIG_PARAM(String name,Map<String,Object> config) throws InitializationException
	{
		Object val = config.get(name);
		if(val == null)
			if(val == null)
				throw new InitializationException("MISSING REQUIRED CONFIG PARAM..."+name);
		try
		{
			if(val.getClass() == Integer.class || val.getClass() == int.class)
				return (Integer)val;
			else
				return Integer.parseInt((String)val);
		}catch(NumberFormatException nfe)
		{
			throw new InitializationException("REQUIRED CONFIG PARAM "+name+" SHOULD BE OF TYPE INT.");
		}
	
	}
	
	public int GET_OPTIONAL_INT_CONFIG_PARAM(String name,int default_val,Map<String,Object> config) throws InitializationException
	{
		Object val = config.get(name);
		if(val == null)
			return default_val;
		return Integer.parseInt((String)val);
	}
	
	public boolean GET_OPTIONAL_BOOLEAN_CONFIG_PARAM(String name,boolean default_val,Map<String,Object> config) throws InitializationException
	{
		Object val = config.get(name);
		if(val == null)
			return default_val;
		return Boolean.parseBoolean((String)val);
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
	
	protected void WAE(String prefix,Exception e) throws WebApplicationException
	{
		throw new WebApplicationException(prefix+" "+e.getMessage(),e);
	}

	protected String GUARD_INSTANCE 		= "instance";
	protected String GUARD_TYPE				= "entity_type";
	protected String GUARD_USER				= "user";
	protected String GUARD_BROWSE_INDEX		= "browse_index";
	protected String GUARD_BROWSE_OP		= "browse_op";
	protected String GUARD_BROWSE_VALUE		= "browse_value";
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
			//if(input == null || input.equals(""))
			//	num_times--;
			//else
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
	
	public String STRIP_TO_ALPHA_NUMERIC(String s)
	{
		if(s == null)
			return null;
		StringBuilder buf = new StringBuilder();
		char[] cc = new char[s.length()];
		s.getChars(0, s.length(), cc, 0);
		for(int i = 0;i < cc.length;i++)
		{
			char c = cc[i];
			if(Character.isLetterOrDigit(c))
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
	
	protected String[] GET_OPTIONAL_LIST_PARAM(String name,Map<String,Object> config) throws InitializationException
	{
		String p = GET_OPTIONAL_CONFIG_PARAM(name, config);
		if(p==null)
			return null;
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


	protected void COPY(File file, File destination_directory,String filename) throws PersistenceException
	{
		FileChannel ic = null;
		FileChannel oc =  null;
		try
		{
			ic = new FileInputStream(file).getChannel();
			oc = new FileOutputStream(new File(destination_directory, (filename!=null)?filename:file.getName())).getChannel();
			ic.transferTo(0, ic.size(), oc);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new PersistenceException("Cant make archive copy!",e);
		}
		finally
		{
			try
			{
				if (ic!=null)
					ic.close();
				if (oc!=null)
					oc.close();
			} 
			catch (Exception e)
			{
				e.printStackTrace();
				throw new PersistenceException("Cant make archive copy!",e);
			}
		}

	}
	
    protected void COPY_DIR(File sourceLocation , File targetLocation)throws IOException {
        
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            
            String[] children = sourceLocation.list();
            for (int i=0; i<children.length; i++) {
                COPY_DIR(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {
            
            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);
            
            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }
    
    public static String READ_FILE_AS_STRING(String filename) throws WebApplicationException
    {
    	File f = new File(filename);
    	return READ_FILE_AS_STRING(f);
    }
    
    public static String READ_FILE_AS_STRING(File f) throws WebApplicationException
    {
        byte[] buffer = new byte[(int)f.length()];
        BufferedInputStream ff;
		try {
			ff = new BufferedInputStream(new FileInputStream(f));
			ff.read(buffer);
		} catch (Exception e) {
			throw new WebApplicationException("PROBLEM READING FILE "+f.getAbsolutePath()+" :"+e.getMessage());
		}

        return new String(buffer);
    }


    ///UTIL STUFF//
    
    public static String PREPARE_REQUIRED_USER_INPUT(String fieldname ,String s ) throws WebApplicationException
    {
    	if(s == null )
    		throw new WebApplicationException(fieldname+" is required.");
    	s = s.trim();
    	if("".equals(s))
    		throw new WebApplicationException(fieldname+" is required.");
    	return s;
    }
    
    
    public static Map<String,Object> KEY_VALUE_PAIRS_TO_MAP(Object... kvp)
    {
    	Map<String,Object> map = new HashMap<String,Object>();
    	for(int i = 0;i < kvp.length;i+=2)
    	{
    		map.put((String)kvp[i],kvp[i+1]);
    	}
    	return map;
    }
    
    public static Object[] MAP_TO_KEY_VALUE_PAIRS(Map<String,Object> map)
    {
    	int size = 	map.entrySet().size();
    	Object[] ret = new Object[size*2];
    	Iterator<String> it = map.keySet().iterator();
    	int i = 0;
    	while(it.hasNext())
    	{
    		String key = it.next();
    		Object val = map.get(key);
    		ret[i++] = key;
    		ret[i++] = val;
    	}
    	return ret;
    }
    
    
    public static Object[] JOIN_KVP(Object[] kvp,Object... kvp2)
    {
    	Object[] ret = new Object[kvp.length + kvp2.length];
    	System.arraycopy(kvp, 0, ret, 0, kvp.length);
    	System.arraycopy(kvp2, 0, ret, kvp.length, kvp2.length);
    	return ret;
    }
    
    
    public Map<String,Object> JSON_CONFIG_TO_MAP(File f) throws InitializationException
	{

		Map<String,Object> map = null;
		try{ 
			String contents = READ_FILE_AS_STRING(f.getAbsolutePath());
			JSONObject o = new JSONObject(contents);
			map = json_to_map(new LinkedHashMap<String,Object>(), o);
		}catch(Exception e)
		{
			ERROR(e);
			throw new InitializationException("PROBLEM READING CONFIG FILE: "+f.getAbsolutePath());
		}
		return map;
	}
	
	private Map<String,Object> json_to_map(Map<String,Object> ret, JSONObject o) throws InitializationException
	{
		try{
			String[] keys = JSONObject.getNames(o);
			if(keys == null)
				return ret;
			for(int i = 0;i < keys.length;i++)
			{
				Object val = o.get(keys[i]);
				if(val instanceof JSONObject)
				{
				
					Map<String,Object> o_map = new LinkedHashMap<String,Object>();
					ret.put(keys[i], json_to_map(o_map,(JSONObject)val));
					
				}
				else if(val instanceof JSONArray)
				{
					JSONArray L = (JSONArray)val;
					List<Object> list = new ArrayList<Object>(L.length());
					ret.put(keys[i], parse_json_list(list, L));
				}
				else
				{
					ret.put(keys[i],val);
				}
			}
			return ret;
		}catch(Exception e)
		{
			ERROR(e);
			throw new InitializationException("PROBLEM CONVERTING JSON FILE TO MAP");
		}
		
	}
	
	public List<Object> parse_json_list(List<Object> list,JSONArray L) throws JSONException,InitializationException
	{
		
		for(int ii = 0;ii < L.length();ii++)
		{
				Object vv = L.get(ii);
				if(vv instanceof JSONObject)
				{
					Map<String,Object> o_map = new LinkedHashMap<String,Object>();
					list.add(json_to_map(o_map,(JSONObject)vv));
				}
				else if(vv instanceof JSONArray)
				{
					List<Object> ll = new ArrayList<Object>(((JSONArray)vv).length());
					list.add(parse_json_list(ll, (JSONArray)vv));
				}
				else
				{
					list.add(vv);	
				}
		}
		return list;
	}
	
	
	 public static String getStackTrace(Throwable aThrowable) {
		    final Writer result = new StringWriter();
		    final PrintWriter printWriter = new PrintWriter(result);
		    aThrowable.printStackTrace(printWriter);
		    return result.toString();
		  }

    
}
