package com.pagesociety.web.module.admin;

import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.LoginFailedException;
import com.pagesociety.web.module.PermissionsModule;
import com.pagesociety.web.module.RawUIModule;
import com.pagesociety.web.module.util.Util;

public class ServerStatisticsRawUI extends RawUIModule
{

	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	

	}

	protected void defineSlots()
	{
		super.defineSlots();
	}
	

	protected void declareSubmodes(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		try{
			declareSubmode(RAW_SUBMODE_DEFAULT,   "submode_default");
		}catch(Exception e)
		{
			ERROR(e);
			throw new InitializationException("FAILED BINDING SUBMODE "+e.getMessage());
		}
	}
	
	protected boolean canExecSubmode(Entity user,int submode,Map<String,Object> params)
	{
			return true;
	}
	
	public void submode_default(UserApplicationContext uctx,Map<String,Object> params)
	{
		
		Entity user = (Entity)uctx.getUser();
		if(!PermissionsModule.IS_ADMIN(user))
		{
			CALL_WITH_INFO(uctx,"UserModuleRawUI",RAW_SUBMODE_DEFAULT,RAW_SUBMODE_DEFAULT,"Statistics module requires login.");
		}
		else
		{
			Runtime r = Runtime.getRuntime();
			boolean do_gc = params.get("gc") != null;
			if(do_gc)
			{
				System.gc();
			}
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,"SERVER STATISTICS",18);
			DISPLAY_ERROR(uctx,params);
			DISPLAY_INFO(uctx,params);
			P(uctx);
			TABLE_START(uctx, 0, 400);
				TR_START(uctx);
				TD(uctx, "free memory:");TD(uctx,get_free_memory(r));
				TR_END(uctx);
				TR_START(uctx);
				TD(uctx, "total memory:");TD(uctx,get_total_memory(r));
				TR_END(uctx);
				TR_START(uctx);
				TD(uctx, "max memory:");TD(uctx,get_max_memory(r));
				TR_END(uctx);
				TR_START(uctx);
				TD(uctx, "num processors:");TD(uctx,get_num_processors(r));
				TR_END(uctx);
				TR_START(uctx);
				TD(uctx, "num threads:");TD(uctx,get_num_threads(r));
				TR_END(uctx);				
			TABLE_END(uctx);
			A_GET(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ Run Garbage Collector ]","gc",true,KEY_UI_MODULE_INFO_KEY,"Ran GC");
			DOCUMENT_END(uctx);
			
		}
	}
	


	   private String get_free_memory(Runtime r)
	   {
	       float freeMemory = (float) r.freeMemory()/1024;
	       Float freeMemoryF = new Float(freeMemory);
	       return String.valueOf(freeMemoryF.intValue()+" K");
	   }
	   
	   private String get_total_memory(Runtime r)
	   {
	       float totalMemory = (float) r.totalMemory()/1024;
	       Float totalMemoryF = new Float(totalMemory);
	       return String.valueOf(totalMemoryF.intValue()+" K");
	   }
	   
	   private String get_max_memory(Runtime r)
	   {
	       float maxMemory = (float) r.maxMemory()/1024;
	       Float maxMemoryF= new Float(maxMemory);
	       return String.valueOf(maxMemoryF.intValue()+" K");
	   }
	   
	   private String get_num_processors(Runtime r)
	   {
		   return String.valueOf(r.availableProcessors());
	   }
	   
	   private String get_num_threads(Runtime r)
	   {
		   return String.valueOf(Thread.activeCount());
	   }
	   
	    
	   
}
