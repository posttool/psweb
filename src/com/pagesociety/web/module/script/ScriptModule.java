package com.pagesociety.web.module.script;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;
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

	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);

	}
	
	public void loadbang(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		try{
			//System.out.println("!!!!GONNA RUN STORE SCRIPT!!!!");
			//run_store_script("/test.js");
		}catch(Exception e)
		{
			throw new InitializationException(e.getMessage());
		}
	}


	
	protected void defineSlots()
	{
		super.defineSlots();
	
	}




	public static final String PROMO_PROGRAM_WRAPPER_HEADER = "function apply_promotion(order){\n";
	public static final String PROMO_PROGRAM_WRAPPER_FOOTER = "\n}\n";
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
	
	public String executeScript(String script) throws PersistenceException,WebApplicationException
	{	
		
		UserApplicationContext uctx = getApplication().getCallingUserContext();
		if(uctx != null)
		{
			uctx.setProperty(USER_OUTPUT_BUF, new StringBuilder(new Date().toString()+"\n"));
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
	private static final String USER_OUTPUT_BUF = "_user_output_buf_";

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
	
	
	public ArrayList<Object> NEW_LIST()
	{
		return new ArrayList<Object>();
	}
	
	public int MAX_INT()
	{
		return Integer.MAX_VALUE;
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
