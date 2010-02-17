package com.pagesociety.web.module.script;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
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
		System.out.println("SCRIPTS IS "+scripts);
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
		StringBuilder buf = new StringBuilder();
		buf.append(source );
		ScriptEngine jsEngine = mgr.getEngineByName(JS_ENGINE_NAME);		 
		ScriptContext ctx = jsEngine.getContext();
		ctx.setAttribute("MODULE", this,ScriptContext.ENGINE_SCOPE );
		//check syntax//
		try {
			jsEngine.eval(buf.toString());
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
	

	//xtra stuff exported to javascript//
	
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
		
		System.out.println("NAME IS "+name+" VALUE IS "+params.get(name));
		return params.get(name);
	}
	
	public ArrayList<Object> NEW_LIST()
	{
		return new ArrayList<Object>();
	}
	
	public int MAX_INT()
	{
		return Integer.MAX_VALUE;
	}

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
