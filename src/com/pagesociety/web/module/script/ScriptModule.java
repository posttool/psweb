package com.pagesociety.web.module.script;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;


import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.util.RandomGUID;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.ecommerce.gateway.BillingGatewayException;



public class ScriptModule extends WebStoreModule 
{

	protected File[] scripts;
	protected File[] includes;
	protected File script_directory;
	protected File script_include_directory;
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);
		
		load_scripts(app,config);
	}
	
	private void load_scripts(WebApplication app, Map<String,Object> config)
	{
		
		script_directory 			= new File(GET_MODULE_DATA_DIRECTORY(app),"scripts");
		script_directory.mkdirs();
		script_include_directory 	= new File(GET_MODULE_DATA_DIRECTORY(app),"include");
		script_include_directory.mkdirs();
		scripts 					= script_directory.listFiles(new FilenameFilter() 
		{
			public boolean accept(File dir, String name) 
			{
				return !name.endsWith(".inputs");
			}
		});
		includes					= script_include_directory.listFiles();
		//sort by filename//
		//System.out.println("SCRIPTS IS "+scripts);
		 Arrays.sort( scripts, new Comparator()
		    {
		      public int compare(final Object o1, final Object o2) {
		        return ((File)o1).getName().compareTo(((File) o2).getName());
		      }
		    }); 
		 
		 Arrays.sort( includes, new Comparator()
		    {
		      public int compare(final Object o1, final Object o2) {
		        return ((File)o1).getName().compareTo(((File) o2).getName());
		      }
		    }); 

	}
	
	protected void defineSlots()
	{
		super.defineSlots();
	}

	public File[] getScripts()
	{
		load_scripts(getApplication(), getParams());
		return scripts;
	}

	public File[] getIncludes()
	{
		load_scripts(getApplication(), getParams());
		return includes;
	}
	
	public String getScript(String filename) throws WebApplicationException
	{
		File f = new File(script_directory,filename);
		if(!f.exists())
			return null;
		
		return READ_FILE_AS_STRING(f.getAbsolutePath());
	}
	
	public String getScriptInputs(String filename) throws WebApplicationException
	{
		File f = new File(script_directory,filename+".inputs");
		if(!f.exists())
			return null;
		
		return READ_FILE_AS_STRING(f.getAbsolutePath());
	}
	
	
	public String getInclude(String filename) throws WebApplicationException
	{
		File f = new File(script_include_directory,filename);
		if(!f.exists())
			return null;
		return READ_FILE_AS_STRING(f.getAbsolutePath());
	}

	public File setScript(String filename,String contents,boolean create) throws WebApplicationException
	{
		File f = new File(script_directory,filename);
		if(!f.exists() && !create)
			throw new WebApplicationException(filename+" does not exist.");
		try{
			FileWriter fw = new FileWriter(f, false);
			fw.write(contents);
			fw.close();
		}catch(IOException ioe)
		{
			throw new WebApplicationException("Problem writing file "+filename+" :"+ioe.getMessage());
		}
		return f;
	}
	
	public File setScriptInputs(String filename,String contents,boolean create) throws WebApplicationException
	{
		File f = new File(script_directory,filename+".inputs");
		if(!f.exists() && !create)
			throw new WebApplicationException(filename+".inputs"+" does not exist.");
		try{
			FileWriter fw = new FileWriter(f, false);
			fw.write(contents);
			fw.close();
		}catch(IOException ioe)
		{
			throw new WebApplicationException("Problem writing file "+filename+".inputs"+" :"+ioe.getMessage());
		}
		return f;
	}
	
	public File setInclude(String filename,String contents,boolean create) throws WebApplicationException
	{
		File f = new File(script_include_directory,filename);
		if(!f.exists() && !create)
			throw new WebApplicationException(filename+" does not exist.");
		f.getParentFile().mkdirs();
		try{
			FileWriter fw = new FileWriter(f, false);
			fw.write(contents);
			fw.close();
		}catch(IOException ioe)
		{
			throw new WebApplicationException("Problem writing file "+filename+" :"+ioe.getMessage());
		}
		return f;	
	}
	
	
	public File deleteScript(String filename) throws WebApplicationException
	{
		File f = new File(script_directory,filename);
		if(!f.exists())
			throw new WebApplicationException(filename+" does not exist.");
		f.delete();
		return f;
	}

	public File deleteInputs(String filename) throws WebApplicationException
	{
		File f = new File(script_directory,filename+".inputs");
		if(!f.exists())
			return f;
			//	throw new WebApplicationException(filename+" does not exist.");
		f.delete();
		return f;
	}

	public File deleteInclude(String filename) throws WebApplicationException
	{
		File f = new File(script_include_directory,filename);
		if(!f.exists())
			throw new WebApplicationException(filename+" does not exist.");
		f.delete();
		return f;	
	}
	
	
	
	public static final String JS_ENGINE_NAME = "JavaScript";
	public String validateScriptSource(String source) throws WebApplicationException
	{
		UserApplicationContext uctx = getApplication().getCallingUserContext();
		ScriptEngineManager mgr = new ScriptEngineManager();
		source = expand_includes(source,null);

		ScriptEngine jsEngine = mgr.getEngineByName(JS_ENGINE_NAME);		 
		ScriptContext ctx = jsEngine.getContext();
		ctx.setAttribute("$", this,ScriptContext.ENGINE_SCOPE );
		//check syntax//
		try {
			jsEngine.eval(source);
		} catch (ScriptException ex)
		{
			return new String("SYNTAX ERROR IN SCRIPT.\n"+ex.getMessage());
		}    
		return "VALIDATE OK";
	}
	
	
	private static final String USER_OUTPUT_BUF 	= "_user_output_buf_";
	private static final String USER_SCRIPT_PARAMS 	= "_user_script_params_";
	/* script is the source of the script not the name of a file.
	 * 
	 * params overrides other sources of inputs. 
	 * it would be used it a thread int he system were executing a script
	 * for instance.
	 */
	 private static ThreadLocal<Map<String,Object>> tl_params = new ThreadLocal<Map<String,Object>>();

	public String executeScript(String script,Map<String,Object> params) throws PersistenceException,WebApplicationException
	{	
		
		UserApplicationContext uctx = getApplication().getCallingUserContext();
		//System.out.println("EXECUTING SCRIPT WITH PARAMS "+params);
		script = expand_includes(script,null);
		
		if(params == null)
			params = new HashMap<String, Object>();
		if(uctx != null)
		{
			uctx.setProperty(USER_OUTPUT_BUF, new StringBuilder(new Date().toString()+"\n"));
			uctx.setProperty(USER_SCRIPT_PARAMS, params);
		}
		else
		{
			tl_params.set(params);
		}
		
		START_TRANSACTION();
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine jsEngine = mgr.getEngineByName(JS_ENGINE_NAME);		 
		ScriptContext ctx = jsEngine.getContext();
		ctx.setAttribute(ScriptEngine.FILENAME ,getName(),ScriptContext.ENGINE_SCOPE);
		ctx.setAttribute("$", this,ScriptContext.ENGINE_SCOPE );
		String output = "";
		try {

			jsEngine.eval(script);
		} catch (ScriptException ex)
		{
			ROLLBACK_TRANSACTION();
			ERROR(ex);
			return new String("EXCEPTION WHILE EXECUTING SCRIPT.\n"+ex.getMessage());
		}    
		
		Object ret = null;  
		try {
			  Invocable inv = (Invocable)jsEngine;
			  ret   = inv.invokeFunction("main",getApplication());
		   } catch (Exception ex) {
		      ROLLBACK_TRANSACTION();
		      if(uctx != null)
		      	{	
		    	  output = ((StringBuilder)uctx.getProperty(USER_OUTPUT_BUF)).toString();
		      	}
		      return new String(output+"\n"+"EXCEPTION WHILE EXECUTING SCRIPT.\n"+ex.getMessage());
		   }    
		   COMMIT_TRANSACTION();
		   if(uctx != null)
		      	{	
		    	  output = ((StringBuilder)uctx.getProperty(USER_OUTPUT_BUF)).toString();
				}
		   return output+"\n"+"EXECUTE OK";
	}
	
	
	public int translate_line_number_to_before_include_expand(String script,int no)
	{
		try{
		int length_before_include = 0;
		int num_includes = 0;
		BufferedReader br = new BufferedReader(new StringReader(script));
		String line;
		while((line = br.readLine()) != null)
		{
			length_before_include++;
			if(line.trim().startsWith("#include"))
			{
				num_includes++;
			}
		}
		
		int length_after_include = 0;
		script = expand_includes(script,null);
		br = new BufferedReader(new StringReader(script));
		while((line = br.readLine()) != null)
			length_after_include++;

		return no - (length_after_include - length_before_include);
		}catch(Exception e)
		{
			ERROR(e);
			return 0;
		}
	}
	
	private String expand_includes(String script,Map<String,String> included_files) throws WebApplicationException
	{
	
		BufferedReader br = new BufferedReader(new StringReader(script));
		String line = null;
		StringBuilder buf = new StringBuilder();
		if(included_files == null)
			included_files = new HashMap<String,String>();
		
		try{
			while((line = br.readLine()) != null)
			{
				String trimmed_line = line.trim();
				if(trimmed_line.startsWith("#include"))
				{
					String rest_of_line = trimmed_line.split("\\#include")[1];
					rest_of_line = rest_of_line.trim();
					byte[] bb = rest_of_line.getBytes();
					byte parse_until = 0;
					boolean system_include = false;
					if(bb[0] == (byte)'<')
					{
						parse_until = (byte)'>';
						system_include = true;
					}
					else if(bb[0] == (byte)'"')
						parse_until = (byte)'"';	
					StringBuilder include_filename = new StringBuilder();
					for(int i = 1;i < bb.length;i++)
					{
						byte b = bb[i];
						if(b == parse_until || b == (byte)'\n')
							break;
						include_filename.append((char)b);
					}
					String filename = include_filename.toString();
					String include_string = null;
					
					boolean already_included = included_files.get(filename) != null;
					if(already_included)
						continue;
					
					
					if(system_include)
						include_string = getInclude(filename);
					else
						include_string = READ_FILE_AS_STRING(filename);	
					
					include_string = expand_includes(include_string, included_files);					
					included_files.put(filename, "yes");
					
					buf.append(include_string);
				}
				else
				{
					buf.append(line+"\n");
				}
			}
		}catch(Exception e)
		{
			ERROR(e);
			throw new WebApplicationException("Exception WHILST EXPANDING INCLUDES. MESSAGE WAS: "+e.getMessage());
		}
		//System.out.println("AFTER INCLUDE FILE IS \n"+buf.toString());
		return buf.toString();
	}

	//xtra stuff exported to javascript//
	/*STD LIB*/
	public void PRINT(String message)
	{
		UserApplicationContext uctx = getApplication().getCallingUserContext();
		if(uctx!=null)
		{
			StringBuilder buf = (StringBuilder)uctx.getProperty(USER_OUTPUT_BUF);
			buf.append(message+"\n");
		}
		else
		{
			super.INFO(message);
		}
	}
	
	public void INFO(String message)
	{
		UserApplicationContext uctx = getApplication().getCallingUserContext();
		if(uctx!=null)
		{
			StringBuilder buf = (StringBuilder)uctx.getProperty(USER_OUTPUT_BUF);
			buf.append("<INFO> "+message+"\n");
		}
		else
		{
			super.INFO(message);
		}
	}
	
	public void WARNING(String message)
	{
		UserApplicationContext uctx = getApplication().getCallingUserContext();
		if(uctx!=null)
		{
			StringBuilder buf = (StringBuilder)uctx.getProperty(USER_OUTPUT_BUF);
			buf.append("<WARNING> "+message+"\n");
		}
		else
		{
			super.WARNING(message);
		}
	}
	
	public void ERROR(String message)
	{
		UserApplicationContext uctx = getApplication().getCallingUserContext();
		if(uctx!=null)
		{
			StringBuilder buf = (StringBuilder)uctx.getProperty(USER_OUTPUT_BUF);
			buf.append("<ERROR> "+message+"\n");
		}
		else
		{
			super.ERROR(message);
		}
	}
	
	public void ERROR(Exception e)
	{
		UserApplicationContext uctx = getApplication().getCallingUserContext();
		if(uctx!=null)
		{
			//should get stack trace as string //
			StringBuilder buf = (StringBuilder)uctx.getProperty(USER_OUTPUT_BUF);
			buf.append("<ERROR> "+e.getMessage()+"\n");
		}
		else
		{
			super.ERROR(e);
		}
	}
	
	public Object GET_INPUT(String name)
	{
		UserApplicationContext uctx = getApplication().getCallingUserContext();
		Map<String,Object> params;
		if(uctx!=null)
			params = (Map<String,Object>)uctx.getProperty(USER_SCRIPT_PARAMS);
		else
			params = tl_params.get();
		
		//System.out.println("NAME IS "+name+" VALUE IS "+params.get(name));
		return params.get(name);
	}
	
	public ArrayList<Object> NEW_LIST()
	{
		return new ArrayList<Object>();
	}
	
	public Map<Object,Object> NEW_MAP()
	{
		return new HashMap<Object,Object>();
	}

	public Entity NEW_ENTITY(String type)
	{
		 Entity e =  new Entity();
		 e.setType(type);
		 return e;
	}
	
	public int MAX_INT()
	{
		return Integer.MAX_VALUE;
	}

	public void SLEEP(long millis)
	{
		try{
			Thread.sleep(millis);
		}catch(Exception e)
		{
			ERROR(e);
		}
	}
	/* TYPE COERCE FUNCTIONS */
	public Integer INT(Object o)
	{
		if(o == null)
			return null;
		else if(o.getClass() == String.class)
			return Integer.parseInt((String)o);
		else if(o.getClass() == Double.class)
			return  new Integer((int)((Double)o).doubleValue());
		else if(o.getClass() == Integer.class)
			return (Integer)o;
		throw new RuntimeException("BAD INT CAST "+String.valueOf(o));
	}
	
	public Float FLOAT(Object o)
	{
		if(o == null)
			return null;
		else if(o.getClass() == String.class)
			return Float.parseFloat((String)o);
		else if(o.getClass() == Double.class)
			return  new Float((float)((Double)o).doubleValue());
		else if(o.getClass() == Float.class)
			return (Float)o;
		throw new RuntimeException("BAD FLOAT CAST "+String.valueOf(o));
	}

	public String STRING(Object o)
	{
		if(o == null)
			return null;
		else if(o.getClass() == String.class)
			return (String)o;
		else if(o.getClass() == Double.class)
			return  String.valueOf(((Double)o).doubleValue());
		else if(o.getClass() == Float.class)
			return String.valueOf((Float)o);
		throw new RuntimeException("BAD STRING CAST "+String.valueOf(o));
	}
	
	public Date DATE(String yyyy,String mm, String dd)
	{
		return DATE(yyyy,mm,dd,"0","0");
	}
	
	public Date DATE(String yyyy,String mm, String dd,String hh,String min)
	{
		 GregorianCalendar newGregCal = new GregorianCalendar(
			     Integer.parseInt(yyyy),
			     Integer.parseInt(mm) - 1,
			     Integer.parseInt(dd),
			     Integer.parseInt(hh),
			     Integer.parseInt(min)
			 );
		 Date d = newGregCal.getTime();
		 return d;
	}
	

	
	/*QUERY FUNCTIONS*/
	public Query NEW_QUERY(String entity_type)
	{
		return new Query(entity_type);
	}
	
	public Object VAL_GLOB()
	{
		return Query.VAL_GLOB;
	}
	
	public Object VAL_MIN()
	{
		return Query.VAL_MIN;
	}
	
	public Object VAL_MAX()
	{
		return Query.VAL_MAX;
	}

	public Object PRIMARY_IDX()
	{
		return Query.PRIMARY_IDX;
	}
	
	
	////////////////////////////////////////
	/////////////////E N D  M O D U L E   F U N C T I O N S/////////////////////////////////////////
		

	protected void defineEntities(Map<String,Object> config) throws PersistenceException,InitializationException
	{
	/*
		DEFINE_ENTITY(PROMOTION_ENTITY,
					  PROMOTION_FIELD_TITLE,Types.TYPE_STRING,"",
					  PROMOTION_FIELD_DESCRIPTION,Types.TYPE_STRING,"",
					  PROMOTION_FIELD_PROGRAM,Types.TYPE_STRING,null,
					  PROMOTION_FIELD_GR1,Types.TYPE_LONG,0L,
					  PROMOTION_FIELD_GR2,Types.TYPE_LONG,0L);
	
	 */
	}

	public static final String IDX_COUPON_PROMOTION_BY_PROMO_CODE   = "byPromoCode";
	public static final String IDX_GLOBAL_PROMOTION_BY_ACTIVE     	= "byActive";
	
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,InitializationException
	{
	/*	DEFINE_ENTITY_INDICES
		(
			GLOBAL_PROMOTION_ENTITY,
			ENTITY_INDEX(IDX_GLOBAL_PROMOTION_BY_ACTIVE , EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, GLOBAL_PROMOTION_FIELD_ACTIVE)
		);
		DEFINE_ENTITY_INDICES
		(
			COUPON_PROMOTION_ENTITY,
			ENTITY_INDEX(IDX_COUPON_PROMOTION_BY_PROMO_CODE , EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, COUPON_PROMOTION_FIELD_PROMO_CODE)
		);
	*/
	}
}
