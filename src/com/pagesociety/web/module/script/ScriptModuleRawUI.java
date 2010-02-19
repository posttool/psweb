package com.pagesociety.web.module.script;


import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
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
		check_codemirror_installed();
	}

	private void check_codemirror_installed() throws InitializationException
	{
		File f = new File(getApplication().getConfig().getWebRootDir(),"/static/js/codemirror");
		if(!f.exists())
			throw new InitializationException("COULDNT FIND CODEMIRROR JAVASCRIP SUPPORT LIBRARY AT "+getApplication().getConfig().getWebRootDir()+"/static/js/codemirror");

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
				{
					script_module.deleteScript(script);
					script_module.deleteInputs(script);
				}
				SET_INFO(script+" was deleted OK.", params);
			}

			File[] scripts = null;
			if(managing_includes)
				scripts = script_module.getIncludes();
			else
				scripts = script_module.getScripts();
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,12,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			String wording = managing_includes?"Scripts":"Includes";
			SPAN(uctx, getApplication().getConfig().getName()+" Script Manager", 16);
			A_GET(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ Manage "+wording+" ]","managing_includes",!managing_includes);
			
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
						A_GET_CONFIRM(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ delete ]","delete",name,"managing_includes",managing_includes);
					TD_END(uctx);
				TR_END(uctx);
				
			}
			TR_START(uctx);
			TD(uctx, "");
			TD(uctx, "");
			TD_START(uctx);
			wording = managing_includes?"INCLUDE":"SCRIPT";
			A(uctx,getName(),RAW_SUBMODE_EDIT_SCRIPT,"[ + ADD "+wording+" ]","create",true,"managing_includes",managing_includes);
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
		DUMP_PARAMS(params);
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
		
		String output			= null;
		String code 			= null;
		String content 			= "";
		String input_content 	= "";
		String savename 		= "";
		boolean update 			= params.get("edit") != null;
		boolean create 			= !update;
		String s_code_editor_offset = (String)params.get("code_editor_offset");
		int code_editor_offset 		= 1;
		if(s_code_editor_offset != null)
				code_editor_offset = Integer.parseInt(s_code_editor_offset);

		if(update)
		{
			String script = (String)params.get("edit");
			if(managing_includes)
				content = script_module.getInclude(script);
			else
			{
				content 		= script_module.getScript(script);
				input_content 	= script_module.getScriptInputs(script);
			}
			savename = script;
		}
		if(create)
		{
			if(!managing_includes)
				content = "function main(application)\n{\n\n}\n";
			else
				content = "/*define the functions you would like to include*/";
			input_content = "/*DECLARE INPUTS HERE*/\n\n[\n\t{\n\t\t'name':'testvar',\n\t\t'type':'text',\n\t\t'value':2345\n\t}\n]";
			savename = "";
		}
		
		if(params.get("MANAGE SCRIPTS") != null || params.get("MANAGE INCLUDES") != null )
		{
			GOTO(uctx,RAW_SUBMODE_DEFAULT,"managing_includes",managing_includes);
			return;
		}
		else if(params.get("RUN") != null)
		{
			code 			= (String)params.get("code_editor");
			String inputs 	= (String)params.get("input_editor");
			Map<String,Object> values = new HashMap<String,Object>();
			if(inputs != null && !(inputs = inputs.trim()).equals(""))
			{
				try{

					JSONArray input_specs = new JSONArray(inputs);
					for(int i = 0;i < input_specs.length();i++)
					{
						JSONObject input_desc = input_specs.getJSONObject(i);
						String type = input_desc.getString("type");
						Object value = null;
						if(type == null)
							type = "string";
						
						String name = input_desc.getString("name");
						value = input_desc.get("value");
						//change into a date in the execute context//
						String svalue=null;
						try{
							svalue = (String)value;
						}catch(Exception e){values.put(name,value);}
						if(svalue != null && (svalue.trim().equals("") || svalue.equals("null")))
								continue;
						if("date".equals(type))
						{
								
							String mm 	= svalue.substring(0,2);
							String dd 	= svalue.substring(3,5);
							String yyyy = svalue.substring(6,10);
							String hh	= "0";
							String min	= "0";
							if(svalue.length() > 10)
							{
								hh  = svalue.substring(11,13);
								min = svalue.substring(14,16);
							}
							 GregorianCalendar newGregCal = new GregorianCalendar(
								     Integer.parseInt(yyyy),
								     Integer.parseInt(mm) - 1,
								     Integer.parseInt(dd),
								     Integer.parseInt(hh),
								     Integer.parseInt(min)
								 );
							 Date d = newGregCal.getTime();
							 value = d;
						}
						
						values.put(name, value);
					}
					output = script_module.executeScript(code,values);
				}catch(Exception e)
				{
					output = "PROBLEM WITH SETTING UP INPUTS:\n"+e.getMessage();
				}
			}
			else
			{
				//no input values so no need to worry about parsing them//
				output = script_module.executeScript(code,null);
			}
			input_content = inputs;	
			content = code;
		}
		else if(params.get("VERIFY") != null)
		{
			code 			= (String)params.get("code_editor");
			output 			= script_module.validateScriptSource(code)+"\n";
			content = code;
		}
		else if(params.get("JUMP TO LINE") != null)
		{
			try{
				code 			= (String)params.get("code_editor");
				code_editor_offset = script_module.translate_line_number_to_before_include_expand(code,Integer.parseInt((String)params.get("jump_to_offset")));
				String last_output = (String)params.get("last_output");
				if(last_output != null && !last_output.trim().equals(""))
					output = last_output;
				content = code;
			}catch(Exception e)
			{
				ERROR(e);
			}
		}
		else if(params.get("SAVE") != null)
		{
			savename = (String)params.get("save_name");
			savename = savename.replaceAll("\\s", "_");
			code 			= (String)params.get("code_editor");
			if(!managing_includes)
			{
				input_content 	= (String)params.get("input_editor");
				output = "INPUTS:\n\t"+script_module.validateScriptSource(input_content)+"\n";
			}
			else
				output = "";
			if(!managing_includes)
				output += "SCRIPT:\n\t "+script_module.validateScriptSource(code)+"\n\n";
			else
				output += "INCLUDE:\n\t "+script_module.validateScriptSource(code)+"\n\n";
			content = code;
			if(output.contains("ERROR"))
				SET_ERROR("Script or Inputs failed validation.",params);
			else if( savename == null || savename.trim().equals(""))
				SET_ERROR("Must provide a name to save a script or include.",params);
			else if(create && managing_includes && script_module.getInclude(savename) != null)
				SET_ERROR("Include with name "+savename+" already exists",params);
			else if(create && !managing_includes && script_module.getScript(savename) != null)
				SET_ERROR("Script with name "+savename+" already exists",params);
			else
			{
				String message = (create)?"created":"updated";
				File f = null;
				if(!output.contains("ERROR"))//this would be from failing the validate above//
				{
					if(managing_includes)
						f = script_module.setInclude(savename,code,!update);				
					else
					{
						f = script_module.setScript(savename,code,!update);				
						script_module.setScriptInputs(savename, input_content, !update);
					}
					SET_INFO(f.getAbsolutePath()+" "+message+" OK.",params);
					output += f.getAbsolutePath()+" "+message+" OK.";
					update = true;
					create = false;
				}
			}
		}

		//else if(params.get("VERIFY") != null)
		//{
		//	code = (String)params.get("code_editor");
		//	output = script_module.validateScriptSource(code);
		//	content=code;
		//}
		
		DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
		String wording = managing_includes?"Include":"Script";
		if(create)
		{
			SPAN(uctx, "Create "+wording, 16); 
			A_GET(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ Manage "+wording+"s ]","managing_includes",managing_includes);
		}
		if(update)
		{
			SPAN(uctx, "Edit: "+savename, 16);
			A_GET(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ Manage "+wording+"s ]","managing_includes",managing_includes);
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

		TABLE_START(uctx,1);
		if(!managing_includes)
		{
			TR_START(uctx);	
			TH(uctx,"code");
			TH(uctx,"inputs");
			TR_END(uctx);
		}
		TR_START(uctx);
		TD_START(uctx);
		FORM_TEXTAREA_FIELD(uctx, "code_editor", 60, 40,content);
		FORM_HIDDEN_FIELD(uctx,"code_editor_offset","1");
		TD_END(uctx);
		if(!managing_includes)
		{
			TD_START(uctx);
			FORM_TEXTAREA_FIELD(uctx, "input_editor", 60, 40,input_content);
			TD_END(uctx);
		}

		TR_END(uctx);
		//FORM_SUBMIT_BUTTON(uctx, "VERIFY");
		TABLE_END(uctx);
		
		if(managing_includes)
		{
			//FORM_SUBMIT_BUTTON(uctx, "MANAGE INCLUDES");
			FORM_SUBMIT_BUTTON(uctx, "VERIFY");
		}
		else
		{
			//FORM_SUBMIT_BUTTON(uctx, "MANAGE SCRIPTS");
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
		FORM_SUBMIT_BUTTON(uctx, "JUMP TO LINE");FORM_INPUT_FIELD(uctx, "jump_to_offset", 4, "");
		FORM_HIDDEN_FIELD(uctx, "last_output", "");
		FORM_END(uctx);
		String editor_setup = 
			  "<script>\nvar code_editor_mirror = CodeMirror.fromTextArea('code_editor_id', {\n"+
			  "\tparserfile: ['tokenizejavascript.js', 'parsejavascript.js'],\n"+
			  "\tpath: '/static/js/codemirror/js/',\n"+
			  "\tstylesheet: '/static/js/codemirror/css/jscolors.css',\n"+
			  "\theight:'480px',\n"+
			  "\ttextWrapping:false,\n"+
			  "\tinitCallback:function(mirror){mirror.jumpToLine("+code_editor_offset+");}\n"+
			"});\n"+
			"</script>\n";
		
		String input_editor_setup = 
			  "<script>\nvar input_editor = CodeMirror.fromTextArea('input_editor_id', {\n"+
			  "\tparserfile: ['tokenizejavascript.js', 'parsejavascript.js'],\n"+
			  "\tpath: '/static/js/codemirror/js/',\n"+
			  "\tstylesheet: '/static/js/codemirror/css/jscolors.css',\n"+
			  "\ttextWrapping:false,\n"+
			  "\theight:'480px'\n"+
			"});</script>\n";

		
		INSERT(uctx,editor_setup);
		if(!managing_includes)
			INSERT(uctx,input_editor_setup);
		HR(uctx);

		if(output != null)
		{
			FORM_TEXTAREA_FIELD(uctx, "output", 140, 10,output);
			//PRE(uctx,output);
		}
		FORM_END(uctx);
		String on_submit_crap = "<script>document.forms[0].onsubmit=on_form_submit;\n"+
		//"function dump(o){if(o==null)return;var s='';for(var key in o){s+=key+'='+o[key];}alert(s);}\n"+
		"function on_form_submit(){"+
		"/*alert(document.forms[0]['JUMP TO LINE'].value);*/"+
		"document.forms[0].code_editor_offset.value=code_editor_mirror.currentLine();"+
		"if(document.forms[0]['JUMP TO LINE'].clicked != null){\n"+
		"document.forms[0].last_output.value =  document.getElementById('output_id').value;}\n"+
		"}</script>";
			
		//String jump_to_crap = "<script>" +
		//"window.setTimeout(function(){"+
		//"code_editor_mirror.jumpToLine("+code_editor_offset+");\n"+
		//"alert('jumped to "+code_editor_offset+"');"+
		//"},10);"+
		//"</script>";
		INSERT(uctx, on_submit_crap);
		//INSERT(uctx, jump_to_crap);
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
			String script 				= (String)params.get("script");
			String output 				= null;

			if(script == null)
			{
				ERROR_PAGE(uctx, new Exception("MISSING PARAMETER 'script'"));
				return;
			}
			if(params.get("RUN") != null)
			{
				//make empty stuff from the form submission null
				//in the execution context. restore it later below
				Iterator<String> it = params.keySet().iterator();
				while(it.hasNext())
				{
					String key = it.next();
					String val = (String)params.get(key);
					if(val != null && (val.trim().equals("") || val.equals("null")))
					{
						params.put(key,null);
					}
				}
				
				String code = script_module.getScript(script);
				String inputs = script_module.getScriptInputs(script);
				List<String> date_keys = new ArrayList<String>();
				if(inputs != null && !(inputs = inputs.trim()).equals(""))
				{
					JSONArray input_specs = new JSONArray(inputs);

					for(int i = 0;i < input_specs.length();i++)
					{
						JSONObject input_desc = input_specs.getJSONObject(i);
						String type = input_desc.getString("type");
						String name = input_desc.getString("name");
	
						if("date".equals(type))
						{
							String date_val = (String)params.get(name); 						
							if(date_val == null)
							{
								params.put(name,null);
								continue;
							}
							String mm 	= date_val.substring(0,2);
							String dd 	= date_val.substring(3,5);
							String yyyy = date_val.substring(6,10);
							String hh	= "0";
							String min	= "0";
							if(date_val.length() > 10)
							{
								hh  = date_val.substring(11,13);
								min = date_val.substring(14,16);
							}
							 GregorianCalendar newGregCal = new GregorianCalendar(
							     Integer.parseInt(yyyy),
							     Integer.parseInt(mm) - 1,
							     Integer.parseInt(dd),
							     Integer.parseInt(hh),
							     Integer.parseInt(min)
							 );
							 Date d = newGregCal.getTime();
							 params.put(name, d);
							 params.put(name+"_old", date_val);
							 date_keys.add(name);
						}
						
					}	
				}
				//swap back the string version of the date so the form behaves correctly//
				output = script_module.executeScript(code,params);
				for(int i = 0;i < date_keys.size();i++)
					params.put(date_keys.get(i), params.get(date_keys.get(i)+"_old"));
			
				//make it the empty string for the sake of the forms default values//
				it = params.keySet().iterator();
				while(it.hasNext())
				{
					String key = it.next();
					String val = (String)params.get(key);
					if(val == null)
					{
						params.put(key,"");
					}
				}
			}
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			SPAN(uctx,"Execute "+script,16);
			A_GET(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ Manage Scripts ]","managing_includes",false);
			DISPLAY_INFO(uctx, params);
			DISPLAY_ERROR(uctx, params);
			HR(uctx);
			FORM_START(uctx,getName(),RAW_SUBMODE_RUN_SCRIPT);
			TABLE_START(uctx,1);
			String inputs = script_module.getScriptInputs(script);
			if(inputs != null && !(inputs = inputs.trim()).equals(""))
			{
				try{
					JSONArray input_specs = new JSONArray(inputs);
					for(int i = 0;i < input_specs.length();i++)
					{

						JSONObject input_desc = input_specs.getJSONObject(i);
						String type = input_desc.getString("type");
						Object value = null;
						if(type == null)
							type = "text";
						
						TR_START(uctx);						
						if("text".equalsIgnoreCase(type))
						{
							String name 	= input_desc.getString("name");
							//optional//
							int width = 30;
							try{
								width 	= input_desc.getInt("width");
							}catch(Exception e){}
							String desc = "";
							try{
								desc = input_desc.getString("description");
							}catch(Exception e){}
							String default_val = (String)params.get(name);
							if(default_val == null)
							{
								try{
									default_val = input_desc.getString("default_value");
								}catch(Exception e)
								{
									try{
										default_val = input_desc.getString("value");
									}catch(Exception ee)
									{
										default_val = "";
									}
								}
							}
							if(default_val.equals("null"))
								default_val = "";
							TD(uctx,name);
							TD_START(uctx);
							FORM_INPUT_FIELD(uctx,name,width,default_val);SPAN(uctx,desc,10);
							TD_END(uctx);
						}
						else if("textarea".equalsIgnoreCase(type))
						{
							String name 	= input_desc.getString("name");
							//optional//
							int rows= 5;
							try{
								rows 	= input_desc.getInt("rows");
							}catch(Exception e){}
							int cols= 30;
							try{
								cols 	= input_desc.getInt("cols");
							}catch(Exception e){}
							String desc = "";
							try{
								desc = input_desc.getString("description");
							}catch(Exception e){}
							String default_val = (String)params.get(name);
							if(default_val == null)
							{
								try{
									default_val = input_desc.getString("default_value");
								}catch(Exception e)
								{
									try{
										default_val = input_desc.getString("value");
									}catch(Exception ee)
									{
										default_val = "";
									}
								}
							}
							if(default_val.equals("null"))
								default_val = "";
							TD(uctx,name);
							TD_START(uctx);
							FORM_TEXTAREA_FIELD(uctx, name, cols, rows, default_val);SPAN(uctx,desc,10);
							TD_END(uctx);
						}
						else if("pulldown".equalsIgnoreCase(type) || "popup".equalsIgnoreCase(type))
						{
							String name 	= input_desc.getString("name");
							//optional//
							int rows= 5;
					
							String[] options	= input_desc.getString("options").split(",");
							String default_val = (String)params.get(name);
							if(default_val == null)
							{
								try{
									default_val = input_desc.getString("default_value");
								}catch(Exception e)
								{
									try{
										default_val = input_desc.getString("value");
									}catch(Exception ee)
									{
										default_val = "";
									}
								}
							}
							if(default_val.equals("null"))
								default_val = "";
							String desc = "";
							try{
								desc = input_desc.getString("description");
							}catch(Exception e){}
							TD(uctx,name);
							TD_START(uctx);
							StringBuilder pulldown = new StringBuilder();
							pulldown.append("<SELECT NAME='"+name+"' >\n");
							for(int ii = 0;ii < options.length;ii++)
							{
								String selected = "";
								String option = options[ii].trim();
								if(default_val.equals(option))
									selected = "SELECTED";
								pulldown.append("<OPTION "+selected+">"+option+"\n");
							}
							pulldown.append("</SELECT>\n");
							INSERT(uctx, pulldown.toString());SPAN(uctx,desc,10);
							TD_END(uctx);
						}
						else if("radio".equalsIgnoreCase(type))
						{
							String name 	= input_desc.getString("name");
							//optional//
							int rows= 5;
					
							String[] options	= input_desc.getString("options").split(",");
							String default_val = (String)params.get(name);
							if(default_val == null)
							{
								try{
									default_val = input_desc.getString("default_value");
								}catch(Exception e)
								{
									try{
										default_val = input_desc.getString("value");
									}catch(Exception ee)
									{
										default_val = "";
									}
								}
							}
							if(default_val.equals("null"))
								default_val = "";
							String desc = "";
							try{
								desc = input_desc.getString("description");
							}catch(Exception e){}

							TD(uctx,name);
							TD_START(uctx);
							StringBuilder radiogroup = new StringBuilder();
							for(int ii = 0;ii < options.length;ii++)
							{
								String selected = "";
								String option = options[ii].trim();
								if(default_val.equals(option))
									selected = "CHECKED";
								radiogroup.append("<INPUT TYPE='radio' NAME='"+name+"' value='"+option+"' "+selected+"> "+option+"&nbsp;\n");
							}

							INSERT(uctx, radiogroup.toString());SPAN(uctx,desc,10);
							
							TD_END(uctx);
						}
						else if("date".equalsIgnoreCase(type))
						{
							String name 	= input_desc.getString("name");
							String default_val = (String)params.get(name);
							if(default_val == null)
							{
								try{
									default_val = input_desc.getString("default_value");
								}catch(Exception e)
								{
									try{
										default_val = input_desc.getString("value");
									}catch(Exception ee)
									{
										default_val = "";
									}
								}
							}
							if(default_val.equals("null"))
								default_val = "";
							TD(uctx,name);
							TD_START(uctx);
							String desc = "";
							try{
								desc = input_desc.getString("description");
							}catch(Exception e){}
							desc = " mm-dd-yyyy [hh:mm]" + desc;
							StringBuilder date_input = new StringBuilder();
							date_input.append("<INPUT TYPE='text' name='"+name+"' value='"+default_val+"'SIZE=8>");
							INSERT(uctx, date_input.toString());SPAN(uctx,desc,10);
							TD_END(uctx);
						}
						TR_END(uctx);

					}
				}catch(Exception e)
				{
					output = "PROBLEM WITH SETTING UP INPUTS:\n"+e.getMessage();
				}
			}

			TABLE_END(uctx);
			FORM_HIDDEN_FIELD(uctx, "script", script);
			if(output != null)
			{
				FORM_TEXTAREA_FIELD(uctx, "output", 120, 10,output);
				//PRE(uctx,output);
			}
			HR(uctx);
			FORM_SUBMIT_BUTTON(uctx, "RUN");

			FORM_END(uctx);
			DOCUMENT_END(uctx);
		}catch(Exception e)
		{
			ERROR_PAGE(uctx,e);
		}
	}
}
