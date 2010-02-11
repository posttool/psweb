package com.pagesociety.web.module.script;


import java.util.Map;






import com.pagesociety.persistence.Entity;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.module.RawUIModule;
import com.pagesociety.web.module.permissions.PermissionEvaluator;



public class ScriptModuleRawUI extends RawUIModule 
{	
	public static final String SLOT_SCRIPT_MODULE = "script-module";
	
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
		String output=null;
		String code = null;
		String content = "";
		DUMP_PARAMS(params);
		if(params.get("RUN") != null)
		{
			code = (String)params.get("code_editor");
			output = script_module.executeScript(code);
			content = code;
		}
		else if(params.get("VERIFY") != null)
		{
			code = (String)params.get("code_editor");
			output = script_module.validateScriptSource(code);
			content=code;
		}
		
		DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
		STYLE(uctx,"pre{font-size:12px;}");
		SCRIPT_INCLUDE(uctx,"/static/js/codemirror/js/codemirror.js");
		
		//
		//STYLE(uctx, ".CodeMirror-wrapping{outline:1px solid #777;}");
		STYLE(uctx, 
	    ".CodeMirror-line-numbers{"+
			"background-color:#EEEEEE;"+
			"color:#AAAAAA;"+
			"font-family:monospace;"+
			"font-size:10pt;"+
			"margin-right:0.3em;"+
			"padding-right:0.3em;"+
			"padding-top:0;"+
			"text-align:right;"+
			"width:2.2em;"+
			"}"
		);
		
		FORM_START(uctx,getName(),RAW_SUBMODE_DEFAULT);
		FORM_TEXTAREA_FIELD(uctx, "code_editor", 60, 40,content);
		FORM_SUBMIT_BUTTON(uctx, "VERIFY");
		FORM_SUBMIT_BUTTON(uctx, "RUN");
		FORM_SUBMIT_BUTTON(uctx, "SAVE");
		FORM_INPUT_FIELD(uctx, "save_name", 30, "");
		FORM_END(uctx);
		String editor_setup = 
			  "<script>\nvar editor = CodeMirror.fromTextArea('code_editor_id', {\n"+
			  "\tparserfile: ['tokenizejavascript.js', 'parsejavascript.js'],\n"+
			  "\tpath: '/static/js/codemirror/js/',\n"+
			  "\tstylesheet: '/static/js/codemirror/css/jscolors.css',\n"+
			  "\theight:'600px'\n"+
			"});</script>\n";
		INSERT(uctx,editor_setup);
		HR(uctx);

		if(output != null)
		{
			FORM_TEXTAREA_FIELD(uctx, "output", 120, 10,output);
			//PRE(uctx,output);
		}
		DOCUMENT_END(uctx);
		}catch(Exception e)
		{
			e.printStackTrace();
			ERROR_PAGE(uctx, e);
		}
	}
	
	
	
	
}
