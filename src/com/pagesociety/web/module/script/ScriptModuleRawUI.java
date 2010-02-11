package com.pagesociety.web.module.script;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import javax.servlet.http.HttpServletResponse;



import com.pagesociety.persistence.Entity;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.module.RawUIModule;
import com.pagesociety.web.module.permissions.PermissionEvaluator;



public class ScriptModuleRawUI extends RawUIModule 
{	
	public static final String SLOT_SCRIPT_MODULE = "script_module";
	
	private ScriptModule script_module;
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		script_module = (ScriptModule)getSlot(SLOT_SCRIPT_MODULE);
		
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_SCRIPT_MODULE,ScriptModule.class,true);
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
	
	public void submode_default(UserApplicationContext uctx,Map<String,Object> params) throws Exception
	{
		try{
			Entity user = (Entity)uctx.getUser();
		if(!PermissionEvaluator.IS_ADMIN(user))
		{
			CALL_WITH_INFO(uctx,"UserModuleRawUI",RAW_SUBMODE_DEFAULT,RAW_SUBMODE_DEFAULT,"Coupon promotion manager module requires admin login.");
			return;
		}
		if(params.get("do_full_backup") != null)
		{
		//	String id = pp.doFullBackup();
		//	last_backup_map.put(id, new Date().toString());
		//	write_backup_map();
		//	DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
		//	P(uctx);
		//	SPAN(uctx,pp.getName()+" BACKUP OK",18);
		//	P(uctx);
		//	JS_TIMED_REDIRECT(uctx, getName(),RAW_SUBMODE_DEFAULT,1000);
		}
		DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
		SCRIPT(uctx,"static/js/codemirror.js");
		FORM_START(uctx,getName(),RAW_SUBMODE_DEFAULT);
		
		FORM_END(uctx);
		
		String editor_setup = "<script>\nvar editor = CodeMirror.fromTextArea('inputfield', {\n"+
			  "\tparserfile: ['tokenizejavascript.js', 'parsejavascript.js'],"+
			  "\tpath: 'lib/codemirror/js/',\n"+
			  "\tstylesheet: 'lib/codemirror/css/jscolors.css'\n"+
			"});</script>\n";
		INSERT(uctx,editor_setup);
		
		}catch(Exception e)
		{
			e.printStackTrace();
			ERROR_PAGE(uctx, e);
		}
	}
	
	
	
	
}
