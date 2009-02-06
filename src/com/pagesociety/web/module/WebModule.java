package com.pagesociety.web.module;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jws.soap.SOAPBinding.Use;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bouncycastle.asn1.ocsp.Request;

import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;


import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.PermissionsException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.gateway.RawCommunique;
import com.pagesociety.web.module.util.Util;


public abstract class WebModule extends Module
{
	public void pre_init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.pre_init(app, config);
	}
	
	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);
	}
	
	protected void DEFINE_SLOT(String slot_name,Class<?> slot_type,boolean required)
	{
		super.defineSlot(slot_name, slot_type, required);
	}
	
	protected void DEFINE_SLOT(String slot_name,Class<?> slot_type,boolean required,Class<?> default_implementation)
	{
		super.defineSlot(slot_name, slot_type, required,default_implementation);
	}
	
	public String GET_REQUIRED_CONFIG_PARAM(String name,Map<String,Object> config) throws InitializationException
	{
		Object val = config.get(name);
		if(val == null)
			throw new InitializationException("MISSING REQUIRED CONFIG PARAM..."+name);
		return (String)val;
	}
	
	
	protected void LOG(String message)
	{
		_application.LOG(getName()+": "+message);
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
		File f =  new File(app.getConfig().getWebRootDir()+File.separator+".."+File.separator+"ModuleData"+File.separator+getName()+"Data");
		if(!f.exists())
		{
			LOG("CREATING DATA DIRECTORY FOR MODULE: "+getName()+"\n\t"+f.getAbsolutePath());
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
	

	protected String GET_CONSOLE_INPUT(String prompt)throws WebApplicationException
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
	       
	   return input;
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
	
	protected String[] GET_REQUIRED_LIST_PARAM(String name,Map<String,Object> config) throws InitializationException
	{
		String p = GET_REQUIRED_CONFIG_PARAM(name, config);
		p = REMOVE_WHITE_SPACE(p);
		return p.split(",");
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
	
	
	///HTML MACROS FOR RAW MODULE CALLS//
	
	protected static final String RAW_UI_BACKGROUND_COLOR 	= "white";
	protected static final String RAW_UI_FONT_COLOR 		= "black";
	protected static final String RAW_UI_FONT_FAMILY 		= "arial";
	protected static final int	  RAW_UI_FONT_SIZE 			= 16;
	protected static final String RAW_UI_LINK_COLOR 		= "#444444";
	protected static final String RAW_UI_LINK_HOVER_COLOR 	= "#CCFFFF";
	protected static final String RAW_UI_ERROR_COLOR		= "red";
	protected static final String RAW_UI_INFO_COLOR    	= "#DDAAAA";

	
	protected void DOCUMENT_START(UserApplicationContext uctx,String title,String bgcolor,String font_family,String font_color,int font_size,String link_color,String link_hover_color)
	{
		DOCUMENT_START(get_user_buf(uctx), title, bgcolor, font_family, font_color, font_size, link_color, link_hover_color);
	}
	
	protected void DOCUMENT_START(StringBuilder buf,String title,String bgcolor,String font_family,String font_color,int font_size,String link_color,String link_hover_color)
	{
		HTML_START(buf);
		HEAD_START(buf,title);
		STYLE(buf,bgcolor,font_family,font_color,font_size,link_color,link_hover_color);
		HEAD_END(buf);
		BODY_START(buf, bgcolor, font_family, font_color, font_size);
		
	}
	
	protected void DOCUMENT_END(UserApplicationContext uctx)
	{
		DOCUMENT_END(get_user_buf(uctx));
	}
	
	protected void DOCUMENT_END(StringBuilder buf)
	{
		BODY_END(buf);
		HTML_END(buf);		
	}
	
	protected void HTML_START(UserApplicationContext uctx)
	{
		HTML_START(get_user_buf(uctx));
	}
	
	protected void HTML_START(StringBuilder buf)
	{
		buf.append("<HTML>\n");
	}
	
	protected void HEAD_START(UserApplicationContext uctx,String title)
	{
		HEAD_START(uctx, title);
	}
	
	protected void HEAD_START(StringBuilder buf,String title)
	{
		buf.append("<HEAD> \n<TITLE>"+title+"</TITLE>\n");
	}
	
	protected void STYLE(UserApplicationContext uctx,String bgcolor,String font_family,String font_color,int font_size,String link_color,String link_hover_color)
	{
		STYLE(get_user_buf(uctx), bgcolor, font_family, font_color, font_size, link_color, link_hover_color);
	}
	
	protected void STYLE(StringBuilder buf,String bgcolor,String font_family,String font_color,int font_size,String link_color,String link_hover_color)
	{
		buf.append("<STYLE>\n");
		buf.append("body { background-color:"+bgcolor+";font-family:"+font_family+";color:"+font_color+";font-size:"+String.valueOf(font_size)+"px;}\n");
		buf.append("a{text-decoration:none;}");
		buf.append("a:link { color:"+link_color+";}\n");
		buf.append("a:visited { color:"+link_color+";}\n");
		buf.append("a:hover { background-color:"+link_hover_color+";font-weight:bold;}\n");
		buf.append("</STYLE>\n");
		
	}
	
	protected void HEAD_END(UserApplicationContext uctx)
	{
		HEAD_END(get_user_buf(uctx));
	}
	
	protected void HEAD_END(StringBuilder buf)
	{
		buf.append("</HEAD>");
	}
	
	protected void BODY_START(UserApplicationContext uctx,String bgcolor,String font_family,String font_color,int font_size)
	{
		BODY_START(get_user_buf(uctx), bgcolor, font_family, font_color, font_size);
	}
	
	protected void BODY_START(StringBuilder buf,String bgcolor,String font_family,String font_color,int font_size)
	{
		if(font_family == null)
			font_family = "arial";
		buf.append("<BODY style='background-color:"+bgcolor+";font-family:"+font_family+";color:"+font_color+";font-size:"+String.valueOf(font_size)+"px'>");
	}
	
	protected void FORM_START(UserApplicationContext uctx,String module_name,int submode,Object... name_val_pairs)
	{
		Map<String,Object> params = new HashMap<String,Object>();
		for(int i = 0;i < name_val_pairs.length;i+=2)
			params.put((String)name_val_pairs[i], name_val_pairs[i+1]);
		FORM_START(uctx, module_name, submode, params);
	}
	
	protected void FORM_START(UserApplicationContext uctx,String module_name,int submode,Map<String,Object> params)
	{
		String action = RAW_MODULE_ROOT()+"/"+module_name+"/Exec2/.raw";
		FORM_START(get_user_buf(uctx),action,"POST");
		FORM_HIDDEN_FIELD(uctx, KEY_UI_MODULE_SUBMODE_KEY, String.valueOf(submode));
		if(params != null)
		{
			Iterator<String> i = params.keySet().iterator();
			while(i.hasNext())
			{
				String key = i.next();
				FORM_HIDDEN_FIELD(uctx,key,(String)params.get(key));
			}
		}
	}
	
	protected void MULTIPART_FORM_START(UserApplicationContext uctx,String module_name,int submode,Object... name_val_pairs)
	{
		Map<String,Object> params = new HashMap<String,Object>();
		for(int i = 0;i < name_val_pairs.length;i+=2)
			params.put((String)name_val_pairs[i], name_val_pairs[i+1]);
		MULTIPART_FORM_START(uctx, module_name, submode, params);
	}
	
	protected void MULTIPART_FORM_START(UserApplicationContext uctx,String module_name,int submode,Map<String,Object> params)
	{
		String action = RAW_MODULE_ROOT()+"/"+module_name+"/Exec2/.raw";
		MULTIPART_FORM_START(get_user_buf(uctx),action);
		FORM_HIDDEN_FIELD(uctx, KEY_UI_MODULE_SUBMODE_KEY, String.valueOf(submode));
		if(params != null)
		{
			Iterator<String> i = params.keySet().iterator();
			while(i.hasNext())
			{
				String key = i.next();
				FORM_HIDDEN_FIELD(uctx,key,(String)params.get(key));
			}
		}
	}
	
	protected void FORM_START(UserApplicationContext uctx,String action,String method)
	{
		FORM_START(get_user_buf(uctx),action,method);
	}
	
	protected void FORM_START(StringBuilder buf,String action,String method)
	{
		buf.append("<FORM ACTION='"+action+"' METHOD='"+method+"'>\n");
	}	  

	protected void MULTIPART_FORM_START(UserApplicationContext uctx,String action)
	{
		MULTIPART_FORM_START(get_user_buf(uctx), action);
	}
	
	protected void MULTIPART_FORM_START(StringBuilder buf,String action)
	{
		buf.append("<FORM ACTION='"+action+"' METHOD='post' ENCTYPE='multipart/form-data'>\n");
	}
	
	protected void FILE_INPUT_FIELD(UserApplicationContext uctx,String name)
	{
		FILE_INPUT_FIELD(get_user_buf(uctx), name);
	}
	
	protected void FILE_INPUT_FIELD(StringBuilder buf,String name)
	{
		buf.append("<INPUT TYPE='file' name='"+name+"'/>");
	}
	
	protected void FORM_INPUT_FIELD(UserApplicationContext uctx,String name,int size,String default_val)
	{
		FORM_INPUT_FIELD(get_user_buf(uctx), name, size,default_val);
	}
	
	protected void FORM_INPUT_FIELD(StringBuilder buf,String name,int size,String default_val)
	{
		buf.append("<INPUT TYPE='text' name='"+name+"' size='"+size+"' value='"+((default_val == null)?"":default_val)+"'/>\n");
	}
	
	protected void FORM_PASSWORD_FIELD(UserApplicationContext uctx,String name,int size)
	{
		FORM_PASSWORD_FIELD(get_user_buf(uctx), name, size);
	}
	
	protected void FORM_PASSWORD_FIELD(StringBuilder buf,String name,int size)
	{
		buf.append("<INPUT TYPE='password' name='"+name+"' size='"+size+"'/>\n");
	}
	
	protected void FORM_HIDDEN_FIELD(UserApplicationContext uctx,String name,String value)
	{
		FORM_HIDDEN_FIELD(get_user_buf(uctx), name, value);
	}
	
	protected void FORM_HIDDEN_FIELD(StringBuilder buf,String name,Object value)
	{
		buf.append("<INPUT TYPE='hidden' name='"+name+"' value='"+String.valueOf(value)+"'/>\n");
	}
	
	protected void FORM_SUBMIT_BUTTON(UserApplicationContext uctx,String label)
	{
		FORM_SUBMIT_BUTTON(get_user_buf(uctx), label);
	}
	
	protected void FORM_SUBMIT_BUTTON(StringBuilder buf,String label)
	{
		buf.append("<INPUT TYPE='submit' value='"+label+"'/>");
	}
	
	protected void FORM_END(UserApplicationContext uctx)
	{
		FORM_END(get_user_buf(uctx));
	}
	
	protected void FORM_END(StringBuilder buf)
	{
		buf.append("</FORM>\n");
	}
	
	protected void BODY_END(UserApplicationContext uctx)
	{
		BODY_END(get_user_buf(uctx));
	}
	
	protected void BODY_END(StringBuilder buf)
	{
		buf.append("</BODY>\n");
	}
	
	protected void HTML_END(UserApplicationContext uctx)
	{
		HTML_END(get_user_buf(uctx));
	}
	
	protected void HTML_END(StringBuilder buf)
	{
		buf.append("</HTML>\n");
	}
    
	protected void BR(UserApplicationContext uctx)
	{
		BR(get_user_buf(uctx));
	}
	
	protected void BR(StringBuilder buf)
	{
		buf.append("<BR/>\n");
	}
	
	protected void P(UserApplicationContext uctx)
	{
		P(get_user_buf(uctx));
	}
	
	protected void P(StringBuilder buf)
	{
		buf.append("<P/>\n");
	}
	
	protected void A(UserApplicationContext uctx,String module_name,int submode,String text,Object... params)
	{
		A(get_user_buf(uctx), module_name, submode, text, params);
	}
	
	protected void A(StringBuilder buf,String module_name,int submode,String text,Object... params)
	{

		String form_name   = "f_"+Util.getGUID().substring(0,8);
		String form_action = RAW_MODULE_ROOT()+"/"+module_name+"/Exec2/.raw";
		
		buf.append("<script type='text/javascript'>\n");
		buf.append("function submitform_"+form_name+"()\n");
		buf.append("{\n");
		buf.append("document."+form_name+".submit();\n");
		buf.append("}\n");
		buf.append("</script>\n"); 
		
		buf.append("<FORM name='"+form_name+"' ACTION='"+form_action+"' METHOD='POST'>\n");
		FORM_HIDDEN_FIELD(buf,KEY_UI_MODULE_SUBMODE_KEY,submode);
		for(int i = 0;i < params.length;i+=2)
			FORM_HIDDEN_FIELD(buf,(String)params[i],params[i+1]);				
		buf.append("<A href='javascript:submitform_"+form_name+"()'>"+text+"</A>\n");
		buf.append("</FORM>\n");

			
	}
	
	protected void A_GET(UserApplicationContext uctx,String module_name,int submode,String text,Object... params)
	{
		A_GET(get_user_buf(uctx), module_name, submode, text, params);
	}
	
	protected void A_GET(StringBuilder buf,String module_name,int submode,String text,Object... params)
	{
		StringBuilder p_buf = new StringBuilder();
		for(int i = 0;i< params.length;i+=2)
		{
			p_buf.append(params[i]+"="+params[i+1]);
			p_buf.append("&");
		}
		if(params.length > 0)
			p_buf.setLength(p_buf.length()-1);
		
		A(buf,RAW_MODULE_ROOT()+"/"+module_name+"/Exec2/.raw?"+KEY_UI_MODULE_SUBMODE_KEY+"="+String.valueOf(submode)+"&"+p_buf.toString(),text);
	}
	
	protected void A(UserApplicationContext uctx,String url,String text)
	{
		A(get_user_buf(uctx),url,text);
	}
	
	protected void A(StringBuilder buf,String url,String text)
	{
		buf.append("<A HREF='"+url+"'>"+text+"</A>");
	}
	
	protected void TABLE_START(UserApplicationContext uctx,int border,int width)
	{
		TABLE_START(get_user_buf(uctx),border,width);
	}
	
	protected void TABLE_START(StringBuilder buf,int border,int width)
	{
		buf.append("<TABLE BORDER='"+border+"' WIDTH='"+width+"'>\n");
	}
	
	protected void TR_START(UserApplicationContext uctx)
	{
		TR_START(get_user_buf(uctx));
	}
	
	protected void TR_START(StringBuilder buf)
	{
		buf.append("<TR>\n");
	}
	
	protected void TD(UserApplicationContext uctx,String data)
	{
		TD(get_user_buf(uctx),data);
	}
	
	protected void TD(StringBuilder buf,String data)
	{
		buf.append("<TD>"+data+"</TD>\n");		
	}
	
	protected void TD_START(UserApplicationContext uctx)
	{
		TD_START(get_user_buf(uctx));
	}
	
	protected void TD_START(StringBuilder buf)
	{
		buf.append("<TD>\n");		
	}
	
	protected void TD_END(UserApplicationContext uctx)
	{
		TD_END(get_user_buf(uctx));
	}
	
	protected void TD_END(StringBuilder buf)
	{
		buf.append("</TD>\n");		
	}

	protected void TR_END(UserApplicationContext uctx)
	{
		TR_END(get_user_buf(uctx));
	}
	
	protected void TR_END(StringBuilder buf)
	{
		buf.append("</TR>\n");
	}
	
	protected void TABLE_END(UserApplicationContext uctx)
	{
		TABLE_END(get_user_buf(uctx));
	}
	
	protected void TABLE_END(StringBuilder buf)
	{
		buf.append("</TABLE>\n");				
	}

	protected void SPAN(UserApplicationContext uctx,String text,String color,int size)
	{
		SPAN(get_user_buf(uctx),text,color,size);
	}
	
	protected void SPAN(StringBuilder buf,String text,String color,int size)
	{
		buf.append("<SPAN style='color:"+color+";font-size:"+size+"px'>"+text+"</SPAN>\n");
	}
	
	protected void SPAN(UserApplicationContext uctx,String text, int size)
	{
		SPAN(get_user_buf(uctx),text,size);
	}
	
	protected void SPAN(StringBuilder buf,String text, int size)
	{
		buf.append("<SPAN style='font-size:"+size+"px'>"+text+"</SPAN>\n");
	}
	
	protected void SPAN(UserApplicationContext uctx,String text, String color)
	{
		SPAN(get_user_buf(uctx),text,color);
	}
	
	protected void SPAN(StringBuilder buf,String text, String color)
	{
		buf.append("<SPAN style='color:"+color+";>"+text+"</SPAN>\n");
	}
	
	protected void SPAN(UserApplicationContext uctx,String text)
	{
		SPAN(get_user_buf(uctx),text);
	}
	
	protected void SPAN(StringBuilder buf,String text)
	{
		buf.append("<SPAN>"+text+"</SPAN>\n");
	}
	
	protected void NBSP(UserApplicationContext uctx,int no)
	{
		NBSP(get_user_buf(uctx),no);
	}
	
	protected void NBSP(StringBuilder buf,int no)
	{
		for(int i = 0;i < no;i++)
		{
			NBSP(buf);
		}
	}
	
	protected void NBSP(UserApplicationContext uctx)
	{
		NBSP(get_user_buf(uctx));
	}
	
	protected void NBSP(StringBuilder buf)
	{
		buf.append("&nbsp;");
	}
	
	protected void JS_REDIRECT(UserApplicationContext uctx,String url)
	{
		JS_REDIRECT(get_user_buf(uctx), url);
	}
	
	protected void JS_REDIRECT(StringBuilder buf,String url)
	{
		buf.append("<script type='text/javascript'>\n");
		buf.append("window.location = '"+url+"';\n");
		buf.append("</script>\n");
	}

	
	protected void JS_TIMED_REDIRECT(UserApplicationContext uctx,String url,int ms)
	{
		JS_TIMED_REDIRECT(get_user_buf(uctx),url, ms);
	}
	
	protected void JS_TIMED_REDIRECT(StringBuilder buf,String url,int ms)
	{
		buf.append("<script type='text/javascript'>\n");
		buf.append("setTimeout(\"window.location = '"+url+"'\","+ms+");\n");
		buf.append("</script>\n");
	}

	protected String RAW_MODULE_EXEC_ROOT()
	{
		return getApplication().getConfig().getWebRootUrl()+"/"+getName()+"/Exec/.raw";
	}
	
	protected String RAW_MODULE_ROOT()
	{
		return getApplication().getConfig().getWebRootUrl();
	}
	
	protected void DISPLAY_ERROR(UserApplicationContext uctx,Map<String,Object> params)
	{
		String error = (String)params.get(KEY_UI_MODULE_ERROR_KEY);
		if(error == null)
			return;
		StringBuilder buf = get_user_buf(uctx);
		SPAN(uctx, error,RAW_UI_ERROR_COLOR,10);
	}
	
	protected void DISPLAY_INFO(UserApplicationContext uctx,Map<String,Object> params)
	{
		String error = (String)params.get(KEY_UI_MODULE_INFO_KEY);
		if(error == null)
			return;
		StringBuilder buf = get_user_buf(uctx);
		SPAN(uctx, error,RAW_UI_INFO_COLOR,10);

	}
	
	protected void ERROR_PAGE(UserApplicationContext uctx,Exception e)
	{
		ERROR(e);//log error//
		StringBuilder buf = get_user_buf(uctx);
		DOCUMENT_START(buf, getName()+"ErrorOccurred", RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_ERROR_COLOR, 14, RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
		SPAN(uctx, e.getMessage(), 16);
		DOCUMENT_END(buf);
	}
	
	public static final int RAW_SUBMODE_DEFAULT    = 0x00;
	private static final String KEY_UI_MODULE_STACK 			= "__ui_module_stack__";
	private static final String KEY_UI_MODULE_OUTPUT_BUF   		= "__ui_module_output__";
	private static final String KEY_UI_MODULE_RAW_COMMUNIQUE   	= "__ui_module_raw_communique__";
	private static final String KEY_UI_MODULE_SUBMODE_KEY   	= "submode";
	private static final String KEY_UI_MODULE_INFO_KEY   		= "_info_";
	private static final String KEY_UI_MODULE_ERROR_KEY   		= "_error_";
	
	protected void DO_EXEC(UserApplicationContext uctx,RawCommunique c) 
	{
		List<ui_module_stack_frame> stack = (List<ui_module_stack_frame>)uctx.getProperty(KEY_UI_MODULE_STACK);
		if(stack == null)
		{
			stack = new ArrayList<ui_module_stack_frame>();
			uctx.setProperty(KEY_UI_MODULE_STACK, stack);
		}
		
		uctx.setProperty(KEY_UI_MODULE_RAW_COMMUNIQUE, c);
		uctx.setProperty(KEY_UI_MODULE_OUTPUT_BUF, new StringBuilder());
		
		HttpServletRequest request = (HttpServletRequest)c.getRequest();
		
		Map<String,Object>params = new HashMap<String,Object>();
		Enumeration e = request.getParameterNames();
		while(e.hasMoreElements())
		{
			String pname = (String)e.nextElement();
			Object val 	 = request.getParameter(pname);
			params.put(pname, val);
		}

		int callee_submode = RAW_SUBMODE_DEFAULT;
		try{
			callee_submode = Integer.parseInt((String)params.get(KEY_UI_MODULE_SUBMODE_KEY));
		}catch(Exception ee){}
	
		if(canExecSubmode(uctx,callee_submode,params))
			execute_submode(uctx, callee_submode, params);

		HttpServletResponse response = (HttpServletResponse)c.getResponse();
		try{
			response.getWriter().println(get_user_buf(uctx).toString());
		}catch(Exception e4)
		{
			ERROR(e4);
		}
	}
	
	protected boolean canExecSubmode(UserApplicationContext uctx,int submode,Map<String,Object> params)
	{
		return true;
	}
	
	protected Object execute_module_submode(UserApplicationContext uctx,String module_name,int submode,Map<String,Object> params)
	{
		return ((WebModule)getApplication().getModule(module_name)).execute_submode(uctx, submode, params);
	}
	
	
	protected Object execute_submode(UserApplicationContext uctx,int submode,Map<String,Object> params)
	{
		Method m = submode_map.get(submode);
		Object ret = null;
		try{
			ret = m.invoke(this, uctx,params);
		}catch(Exception eee)
		{
			eee.printStackTrace();
			get_user_buf(uctx).append("<font color='"+RAW_UI_ERROR_COLOR+"'>"+eee.getMessage()+"</font>");
		}
		return ret;
	}

	protected void RETURN(UserApplicationContext uctx)
	{
		List<ui_module_stack_frame> stack = get_user_stack(uctx);
		ui_module_stack_frame caller = pop_caller(uctx);
			
		String caller_module = getName();
		int caller_return_to_submode = RAW_SUBMODE_DEFAULT;
		if(caller != null)
		{
			caller_module 			 = caller.caller_module;
			caller_return_to_submode = caller.caller_return_to_submode;
		}
		execute_module_submode(uctx, caller_module, caller_return_to_submode, null);//could actually return params..probs name value pairs
	}
	
	private Map<Integer,Method> submode_map = new HashMap<Integer,Method>();
	protected void declareSubmode(int submode_id,String method_name) throws WebApplicationException
	{
		Method[] mm = this.getClass().getDeclaredMethods();
		for(int i = 0;i < mm.length;i++)
		{
			Method m = mm[i];
			if(m.getName().equals(method_name))
			{
				m.setAccessible(true);
				submode_map.put(submode_id,m);
				break;
			}
		}
	}

	private void push_stack_frame(UserApplicationContext uctx,ui_module_stack_frame f)
	{
		System.out.println("PUSHING FRAME "+f);
		get_user_stack(uctx).add(f);
	}
	
	private ui_module_stack_frame pop_stack_frame(UserApplicationContext uctx)
	{
		List<ui_module_stack_frame> stack = get_user_stack(uctx);
		if(stack.size() == 0)
			return null;
		return stack.remove(stack.size()-1);
	}
	
	//this is goto//
	protected void GOTO(UserApplicationContext uctx,int submode,Map<String,Object> params)
	{
		if(params == null)
			params = new HashMap<String,Object>();
		execute_submode(uctx, submode, params);
	}
	
	protected void GOTO(UserApplicationContext uctx,int submode,Object...name_val_pairs)
	{
		Map<String,Object> params = new HashMap<String,Object>();

		for(int i = 0;i < name_val_pairs.length;i+=2)
			params.put((String)name_val_pairs[i], name_val_pairs[i+1]);
		execute_submode(uctx, submode, params);
	}
	
	protected void GOTO_WITH_INFO(UserApplicationContext uctx,int submode,String info,Object...name_val_pairs)
	{
		Map<String,Object> params = new HashMap<String,Object>();
		for(int i = 0;i < name_val_pairs.length;i+=2)
			params.put((String)name_val_pairs[i], name_val_pairs[i+1]);

		GOTO_WITH_INFO(uctx, submode, info, params);
	}
	
	protected void GOTO_WITH_INFO(UserApplicationContext uctx,int submode,String info,Map<String,Object> params)
	{
		params.put(KEY_UI_MODULE_INFO_KEY, info);
		execute_submode(uctx, submode, params);		
	}
	
	protected void GOTO_WITH_ERROR(UserApplicationContext uctx,int submode,String error,Object...name_val_pairs)
	{
		Map<String,Object> params = new HashMap<String,Object>();
		for(int i = 0;i < name_val_pairs.length;i+=2)
			params.put((String)name_val_pairs[i], name_val_pairs[i+1]);
		GOTO_WITH_ERROR(uctx, submode, error, params);
	}
	
	protected void GOTO_WITH_ERROR(UserApplicationContext uctx,int submode,String error,Map<String,Object> params)
	{
		params.put(KEY_UI_MODULE_ERROR_KEY, error);
		execute_submode(uctx, submode, params);	
	}
	
	//TODO also need one that defaults return to submode//
	protected void CALL(UserApplicationContext uctx,String module_name,int submode,int return_to_submode,Object...name_val_pairs)
	{

		Map<String,Object> params = new HashMap<String,Object>();
		for(int i = 0;i < name_val_pairs.length;i+=2)
			params.put((String)name_val_pairs[i], name_val_pairs[i+1]);
		
		push_caller(uctx,getName(),return_to_submode);		
		execute_module_submode(uctx, module_name, submode, params);
	}
	
	private void push_caller(UserApplicationContext uctx,String module_name,int return_to_submode)
	{
		List<ui_module_stack_frame> caller_stack = get_user_stack(uctx);
		ui_module_stack_frame f 	= new ui_module_stack_frame();
		f.caller_module 		    = module_name;
		f.caller_return_to_submode  = return_to_submode;
		caller_stack.add(f);
	}
	
	private ui_module_stack_frame pop_caller(UserApplicationContext uctx)
	{
		List<ui_module_stack_frame> caller_stack = get_user_stack(uctx);
		if(caller_stack.size() == 0)
			return null;
		else
			return caller_stack.remove(caller_stack.size()-1);
	}
		
	private List<ui_module_stack_frame> get_user_stack(UserApplicationContext uctx)
	{
		return (List<ui_module_stack_frame>)uctx.getProperty(KEY_UI_MODULE_STACK);
	}
	
	private StringBuilder get_user_buf(UserApplicationContext uctx)
	{
		return (StringBuilder)uctx.getProperty(KEY_UI_MODULE_OUTPUT_BUF);
	}
	
	private ui_module_stack_frame get_current_frame(UserApplicationContext uctx)
	{
		List<ui_module_stack_frame> stack;
		stack = get_user_stack(uctx);
		int ss = stack.size();
		if(ss == 0)
			return null;
		else
			return stack.get(stack.size()-1);
	}
	
	class ui_module_stack_frame
	{
//		public String 	calling_module;
//		public int	   	calling_submode;
		public String 	caller_module;
		public int 		caller_return_to_submode;
		public Map<String,Object> params;
	
		public String toString()
		{
			return 
			(
//			"calling_module:  "+calling_module+"\n"+
//			"calling_submode: "+calling_submode+"\n"+
			"\ncallee_module:   "+caller_module+"\n"+
			"callee_submode:  "+caller_return_to_submode+"\n"+
			"params:          "+params+"\n"
			);
		}
	}

}
