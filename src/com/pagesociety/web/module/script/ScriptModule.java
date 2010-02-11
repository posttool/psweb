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
			System.out.println("!!!!GONNA RUN STORE SCRIPT!!!!");
			run_store_script("/test.js");
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
	private void validate_promotion_source(String promotion_name,String source) throws WebApplicationException
	{
		ScriptEngineManager mgr = new ScriptEngineManager();
		StringBuilder buf = new StringBuilder();
		buf.append(PROMO_PROGRAM_WRAPPER_HEADER);
		buf.append(source );
		buf.append(PROMO_PROGRAM_WRAPPER_FOOTER);
		 
		ScriptEngine jsEngine = mgr.getEngineByName(JS_ENGINE_NAME);		 
		ScriptContext ctx = jsEngine.getContext();
		ctx.setAttribute(ScriptEngine.FILENAME ,promotion_name, ScriptContext.ENGINE_SCOPE);
		ctx.setAttribute("MODULE", this,ScriptContext.ENGINE_SCOPE );

		
		//check syntax//
		try {
			jsEngine.eval(buf.toString());
		} catch (ScriptException ex)
		{
			throw new WebApplicationException("SYNTAX ERROR IN PROMOTIONS SCRIPT.\n"+ex.getMessage(),ex);
		}    
			  
	}
	
	private Object run_store_script(String filename_abs_path) throws PersistenceException,WebApplicationException
	{	
		START_TRANSACTION();
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine jsEngine = mgr.getEngineByName(JS_ENGINE_NAME);		 
		ScriptContext ctx = jsEngine.getContext();
		ctx.setAttribute(ScriptEngine.FILENAME ,filename_abs_path,ScriptContext.ENGINE_SCOPE);
		ctx.setAttribute("$", this,ScriptContext.ENGINE_SCOPE );
		FileReader f_reader = null;
		try{
			f_reader = new FileReader(filename_abs_path);
		}catch(FileNotFoundException fnfe)
		{
			throw new WebApplicationException("UNABLE TO FIND SCRIPT FILE "+filename_abs_path);
		}
		try {

			jsEngine.eval(f_reader);
		} catch (ScriptException ex)
		{
			throw new WebApplicationException("SYNTAX ERROR IN PROMOTIONS SCRIPT.\n"+ex.getMessage(),ex);
		}    
		
		  //apply promotion//
		Object ret = null;  
		try {
			  Invocable inv = (Invocable)jsEngine;
			  ret   = inv.invokeFunction("main",getApplication());
		   } catch (Exception ex) {
		      ex.printStackTrace();
		      ROLLBACK_TRANSACTION();
		   }    
		   COMMIT_TRANSACTION();
		   return ret;
	}
	
	//xtra stuff exported to javascript//
	public void INFO(String message)
	{
		super.INFO(message);
	}
	
	public void WARNING(String message)
	{
		super.WARNING(message);
	}
	
	public void ERROR(String message)
	{
		super.ERROR(message);
	}
	
	public void ERROR(Exception e)
	{
		super.ERROR(e);
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
