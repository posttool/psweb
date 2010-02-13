package com.pagesociety.web.module.script;


import java.io.File;
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


	public static final int RAW_SUBMODE_EDIT_SCRIPT = 0x01;
	public static final int RAW_SUBMODE_RUN_SCRIPT = 0x02;
	protected void declareSubmodes(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		try{
			declareSubmode(RAW_SUBMODE_DEFAULT,   "submode_default");
			declareSubmode(RAW_SUBMODE_EDIT_SCRIPT,   "submode_edit_script");
			declareSubmode(RAW_SUBMODE_RUN_SCRIPT,   "submode_run_script");
			
		}catch(Exception e)
		{
			ERROR(e);
			throw new InitializationException("FAILED BINDING SUBMODE "+e.getMessage());
		}
	}
	
	public void submode_default(UserApplicationContext uctx,Map<String,Object> params) throws Exception
	{
		try{
			String s_managing_includes = (String)params.get("managing_includes");
			boolean managing_includes = false;
			if(s_managing_includes != null)
				managing_includes = Boolean.parseBoolean(s_managing_includes);
			
			Entity user = (Entity)uctx.getUser();
			if(!PermissionEvaluator.IS_ADMIN(user))
			{
				CALL_WITH_INFO(uctx,"UserModuleRawUI",RAW_SUBMODE_DEFAULT,RAW_SUBMODE_DEFAULT,"Script module requires admin login.");
				return;
			}
			
			if(params.get("delete") != null)
			{
				String script = (String)params.get("delete");
				if(managing_includes)
					script_module.deleteInclude(script);
				else
					script_module.deleteScript(script);
				SET_INFO(script+" was deleted OK.", params);
			}
			
			File[] scripts = null;
			if(managing_includes)
				scripts = script_module.getIncludes();
			else
				scripts = script_module.getScripts();
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,12,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			SPAN(uctx, getApplication().getConfig().getName()+" Script Manager", 16);
			DISPLAY_INFO(uctx, params);
			DISPLAY_ERROR(uctx, params);
			HR(uctx);
			P(uctx);
			TABLE_START(uctx, 0, 640);
			
			for(int i = 0;i < scripts.length;i++)
			{
				String name = scripts[i].getName();
				TR_START(uctx);
					TD_START(uctx);
						if(!managing_includes)
							A_GET(uctx,getName(),RAW_SUBMODE_RUN_SCRIPT,name,"script",name,"managing_includes",managing_includes);
						else
							SPAN(uctx,name);
					TD_END(uctx);
					TD_START(uctx);
						A_GET(uctx,getName(),RAW_SUBMODE_EDIT_SCRIPT,"[ edit ]","edit",name,"managing_includes",managing_includes);
					TD_END(uctx);
					TD_START(uctx);
						A_GET(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ delete ]","delete",name,"managing_includes",managing_includes);
					TD_END(uctx);
				TR_END(uctx);
				
			}
			TR_START(uctx);
			TD(uctx, "");
			TD(uctx, "");
			TD_START(uctx);
			String wording = managing_includes?"INCLUDE":"SCRIPT";
				A(uctx,getName(),RAW_SUBMODE_EDIT_SCRIPT,"[ + ADD "+wording+" ]","create",true,"managing_includes",managing_includes);
			TD_END(uctx);
			TR_END(uctx);
			wording = managing_includes?"Scripts":"Includes";
				TR_START(uctx);
					TD(uctx, "");
					TD(uctx, "");
					TD_START(uctx);
					A(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ Manage "+wording+" ]","managing_includes",!managing_includes);
					TD_END(uctx);
				TR_END(uctx);
			
			DOCUMENT_END(uctx);
			
		}catch(Exception e)
		{
			ERROR_PAGE(uctx, e);
		}
	}
	
	public void submode_edit_script(UserApplicationContext uctx,Map<String,Object> params) throws Exception
	{
		try{
			String s_managing_includes = (String)params.get("managing_includes");
			boolean managing_includes = false;
			if(s_managing_includes != null)
				managing_includes = Boolean.parseBoolean(s_managing_includes);
			
			Entity user = (Entity)uctx.getUser();
		if(!PermissionEvaluator.IS_ADMIN(user))
		{
			CALL_WITH_INFO(uctx,"UserModuleRawUI",RAW_SUBMODE_DEFAULT,RAW_SUBMODE_DEFAULT,"Script module requires admin login.");
			return;
		}
		String output=null;
		String code = null;
		String content = "";
		String savename = "";
		boolean update = params.get("edit") != null;
		boolean create = !update;
		
		DUMP_PARAMS(params);
		if(update)
		{
			String script = (String)params.get("edit");
			if(managing_includes)
				content = script_module.getInclude(script);
			else
				content = script_module.getScript(script);
			savename = script;
		}
		if(create)
		{
			content = "function main(application)\n{\n\n}\n";
			savename = "";
		}
		
		if(params.get("MANAGE SCRIPTS") != null || params.get("MANAGE INCLUDES") != null )
		{
			GOTO(uctx,RAW_SUBMODE_DEFAULT,"managing_includes",managing_includes);
			return;
		}
		else if(params.get("RUN") != null)
		{
			code = (String)params.get("code_editor");
			output = script_module.executeScript(code);
			content = code;
		}
		else if(params.get("VERIFY") != null)
		{
			code = (String)params.get("code_editor");
			output = script_module.validateScriptSource(code);
			content = code;
		}
		else if(params.get("SAVE") != null)
		{
			savename = (String)params.get("save_name");
			savename = savename.replaceAll("\\s", "_");
			code = (String)params.get("code_editor");
			content = code;

			if( savename == null || savename.trim().equals(""))
				SET_ERROR("Must provide a name to save a script or include.",params);
			else if(create && managing_includes && script_module.getInclude(savename) != null)
				SET_ERROR("Include with name "+savename+" already exists",params);
			else if(create && !managing_includes && script_module.getScript(savename) != null)
				SET_ERROR("Script with name "+savename+" already exists",params);
			else
			{
				String message = (create)?"created":"updated";
				File f = null;
				if(managing_includes)
					f = script_module.setInclude(savename,code,!update);				
				else
					f = script_module.setScript(savename,code,!update);				
				SET_INFO(f.getAbsolutePath()+" "+message+" OK.",params);
				update = true;
				create = false;
			}
		}

		//else if(params.get("VERIFY") != null)
		//{
		//	code = (String)params.get("code_editor");
		//	output = script_module.validateScriptSource(code);
		//	content=code;
		//}
		
		DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
		if(create)
		{
			String wording = managing_includes?"Include":"Script";
			SPAN(uctx, "Create "+wording, 16);
		}
		if(update)
		{
			SPAN(uctx, "Edit: "+savename, 16);
		}
		DISPLAY_INFO(uctx, params);
		DISPLAY_ERROR(uctx, params);
		HR(uctx);
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

		FORM_START(uctx,getName(),RAW_SUBMODE_EDIT_SCRIPT);
		FORM_TEXTAREA_FIELD(uctx, "code_editor", 60, 40,content);
		//FORM_SUBMIT_BUTTON(uctx, "VERIFY");

		
		if(managing_includes)
		{
			FORM_SUBMIT_BUTTON(uctx, "MANAGE INCLUDES");
			FORM_SUBMIT_BUTTON(uctx, "VERIFY");
		}
		else
		{
			FORM_SUBMIT_BUTTON(uctx, "MANAGE SCRIPTS");
			FORM_SUBMIT_BUTTON(uctx, "RUN");
		}
		FORM_SUBMIT_BUTTON(uctx, "SAVE");
		if(create)
		{
			FORM_INPUT_FIELD(uctx, "save_name", 30, savename);
			FORM_HIDDEN_FIELD(uctx, "managing_includes", String.valueOf(managing_includes));
		}
		else if(update)
		{
			FORM_HIDDEN_FIELD(uctx, "save_name", savename);
			FORM_HIDDEN_FIELD(uctx, "edit", savename);
			FORM_HIDDEN_FIELD(uctx, "managing_includes", String.valueOf(managing_includes));
		}
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
	
	public void submode_run_script(UserApplicationContext uctx,Map<String,Object> params) throws Exception
	{		
		try{
			Entity user = (Entity)uctx.getUser();
			if(!PermissionEvaluator.IS_ADMIN(user))
			{
				CALL_WITH_INFO(uctx,"UserModuleRawUI",RAW_SUBMODE_DEFAULT,RAW_SUBMODE_DEFAULT,"Script module requires admin login.");
				return;
			}
			String script = (String)params.get("script");
			String output = null;

			if(script == null)
			{
				ERROR_PAGE(uctx, new Exception("MISSING PARAMETER 'script'"));
				return;
			}
			if(params.get("LIST SCRIPTS") != null)
			{
				GOTO(uctx,RAW_SUBMODE_DEFAULT);
				return;
			}
			if(params.get("RUN") != null)
			{
				String code = script_module.getScript(script);
				output = script_module.executeScript(code);
			}
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			SPAN(uctx, getApplication().getConfig().getName()+" Script Manager", 16);
			DISPLAY_INFO(uctx, params);
			DISPLAY_ERROR(uctx, params);
			SPAN(uctx,"Execute "+script,16);
			HR(uctx);
			FORM_START(uctx,getName(),RAW_SUBMODE_RUN_SCRIPT);
			FORM_HIDDEN_FIELD(uctx, "script", script);
			FORM_SUBMIT_BUTTON(uctx, "LIST SCRIPTS");
			FORM_SUBMIT_BUTTON(uctx, "RUN");
			if(output != null)
			{
				FORM_TEXTAREA_FIELD(uctx, "output", 120, 10,output);
				//PRE(uctx,output);
			}
			FORM_END(uctx);
			DOCUMENT_END(uctx);
		}catch(Exception e)
		{
			ERROR_PAGE(uctx,e);
		}
	}
}
