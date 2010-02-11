package com.pagesociety.web.module;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.PermissionsException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.gateway.RawCommunique;
import com.pagesociety.web.module.util.Util;

public class RawUIModule extends WebModule
{
	private boolean secure;
	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);
		setSecure(true);
		declareSubmodes(app,config);
	}
	
	public void setSecure(boolean b)
	{
		secure = b;
	}
	
	@Export
	public void Exec(UserApplicationContext uctx,RawCommunique c) 
	{
		DO_EXEC(uctx, c);
	}

	//this is just the default. you can override this if you want more submodes//
	protected void declareSubmodes(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		try{
			declareSubmode(RAW_SUBMODE_DEFAULT,  "exec");
		}catch(Exception e)
		{
			ERROR(e);
			throw new InitializationException("FAILED BINDING SUBMODE "+e.getMessage());
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
	protected static final String RAW_UI_INFO_COLOR    		= "#DDAAAA";

	
	protected void DOCUMENT_START(UserApplicationContext uctx,String title,String bgcolor,String font_family,String font_color,int font_size,String link_color,String link_hover_color)
	{
		DOCUMENT_START(get_user_buf(uctx), title, bgcolor, font_family, font_color, font_size,font_size, link_color, link_hover_color);
	}
	
	protected void DOCUMENT_START(UserApplicationContext uctx,String title,String bgcolor,String font_family,String font_color,int font_size,int table_font_size,String link_color,String link_hover_color)
	{
		DOCUMENT_START(get_user_buf(uctx), title, bgcolor, font_family, font_color, font_size,table_font_size, link_color, link_hover_color);
	}
	
	protected void DOCUMENT_START(StringBuilder buf,String title,String bgcolor,String font_family,String font_color,int font_size,String link_color,String link_hover_color)
	{
		DOCUMENT_START(buf, title, bgcolor, font_family, font_color, font_size,font_size, link_color, link_hover_color);
	}
	
	protected void DOCUMENT_START(StringBuilder buf,String title,String bgcolor,String font_family,String font_color,int font_size,int table_font_size,String link_color,String link_hover_color)
	{
		HTML_START(buf);
		HEAD_START(buf,title);
		STYLE(buf,bgcolor,font_family,font_color,font_size,table_font_size,link_color,link_hover_color);
		HEAD_END(buf);
		BODY_START(buf, bgcolor, font_family, font_color, font_size);
		
	}
	
	protected void DOCUMENT_END(UserApplicationContext uctx)
	{
		DOCUMENT_END(get_user_buf(uctx));
	}
	
	protected void DOCUMENT_END_RETURN_IN(UserApplicationContext uctx,int ms)
	{
		ui_module_stack_frame caller = pop_caller(uctx);
			
		String caller_module = getName();
		int caller_return_to_submode = RAW_SUBMODE_DEFAULT;
		if(caller != null)
		{
			caller_module 			 = caller.caller_module;
			caller_return_to_submode = caller.caller_return_to_submode;
			JS_TIMED_REDIRECT(uctx, caller_module, caller_return_to_submode,ms);
		}
		
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
		STYLE(get_user_buf(uctx), bgcolor, font_family, font_color, font_size, font_size,link_color, link_hover_color);
	}
	
	protected void STYLE(StringBuilder buf,String bgcolor,String font_family,String font_color,int font_size,int table_font_size,String link_color,String link_hover_color)
	{
		
		buf.append("<STYLE>\n");
		buf.append("body { background-color:"+bgcolor+";font-family:"+font_family+";color:"+font_color+";font-size:"+String.valueOf(font_size)+"px;}\n");
		buf.append("a{text-decoration:none;}");
		buf.append("a:link { color:"+link_color+";}\n");
		buf.append("a:visited { color:"+link_color+";}\n");
		buf.append("a:hover { background-color:"+link_hover_color+";font-weight:bold;}\n");	
		buf.append("th,td { font-family:"+font_family+";color:"+font_color+";font-size:"+String.valueOf(table_font_size)+"px;}\n");
	
		
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
	
	protected void SCRIPT_INCLUDE(UserApplicationContext uctx,String src)
	{
		SCRIPT_INCLUDE(get_user_buf(uctx),src);
	}
	
	protected void SCRIPT_INCLUDE(StringBuilder buf,String src)
	{
		buf.append("<script src='"+src+"' type='text/javascript'></script>\n");
	}
	
	protected void STYLE(UserApplicationContext uctx,String stylestring)
	{
		STYLE(get_user_buf(uctx),stylestring);
	}
	
	protected void STYLE(StringBuilder buf,String stylestring)
	{
		buf.append("<style type='text/css'>"+stylestring+"</style>\n");
	}
	
	protected void STYLE_INCLUDE(UserApplicationContext uctx,String src)
	{
		STYLE_INCLUDE(get_user_buf(uctx),src);
	}
	
	protected void STYLE_INCLUDE(StringBuilder buf,String src)
	{
		buf.append("<link rel='stylesheet' type='text/css' href='"+src+"'/>\n");
	}

	
	
	protected void BODY_START(UserApplicationContext uctx,String bgcolor,String font_family,String font_color,int font_size)
	{
		BODY_START(get_user_buf(uctx), bgcolor, font_family, font_color, font_size);
	}
	
	protected void BODY_START(StringBuilder buf,String bgcolor,String font_family,String font_color,int font_size)
	{
		if(font_family == null)
			font_family = "arial";
		buf.append("<BODY style='background-color:"+bgcolor+";font-family:"+font_family+";color:"+font_color+";font-size:"+String.valueOf(font_size)+"px'>\n");
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
		String action = RAW_MODULE_ROOT()+"/"+module_name+"/Exec/.raw";
		FORM_START(get_user_buf(uctx),action,"POST");
		FORM_HIDDEN_FIELD(uctx, KEY_UI_MODULE_SUBMODE_KEY, String.valueOf(submode));
		if(params != null)
		{
			Iterator<String> i = params.keySet().iterator();
			while(i.hasNext())
			{
				String key = i.next();
				FORM_HIDDEN_FIELD(uctx,key,String.valueOf(params.get(key)));
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
		String action = RAW_MODULE_ROOT()+"/"+module_name+"/Exec/.raw";
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
	
	protected void FORM_TEXTAREA_FIELD(UserApplicationContext uctx,String name,int cols,int rows)
	{
		FORM_TEXTAREA_FIELD(get_user_buf(uctx), name, cols,rows,null);
	}
	
	protected void FORM_TEXTAREA_FIELD(UserApplicationContext uctx,String name,int cols,int rows,String default_value)
	{
		FORM_TEXTAREA_FIELD(get_user_buf(uctx), name, cols,rows,default_value);
	}
	
	protected void FORM_TEXTAREA_FIELD(StringBuilder buf,String name,int cols,int rows,String default_value)
	{
		if(default_value == null)
			buf.append("<TEXTAREA name='"+name+"' id='"+name+"_id' cols='"+cols+"' rows='"+rows+"'></TEXTAREA>\n");
		else
			buf.append("<TEXTAREA name='"+name+"' id='"+name+"_id' cols='"+cols+"' rows='"+rows+"' >"+default_value+"</TEXTAREA>\n");
	}
	
	protected void FORM_PASSWORD_FIELD(UserApplicationContext uctx,String name,int size)
	{
		FORM_PASSWORD_FIELD(get_user_buf(uctx), name, size);
	}
	
	protected void FORM_PASSWORD_FIELD(StringBuilder buf,String name,int size)
	{
		buf.append("<INPUT TYPE='password' name='"+name+"' size='"+size+"'/>\n");
	}
	
	protected void FORM_PULLDOWN_MENU(UserApplicationContext uctx,String name,String[] options,String[] values)
	{
		FORM_PULLDOWN_MENU(get_user_buf(uctx), name,options,values,0,null);
	}
	protected void FORM_PULLDOWN_MENU(UserApplicationContext uctx,String name,String[] options,String[] values,int width,String selected_value)
	{
		FORM_PULLDOWN_MENU(get_user_buf(uctx), name,options,values,width,selected_value);
	}
	
	protected void FORM_PULLDOWN_MENU(StringBuilder buf,String name,String[] options,String[] values,int width,String selected_value)
	{
		if(width == 0)
			buf.append("<SELECT NAME=\""+name+"\">\n");
		else
			buf.append("<SELECT NAME=\""+name+"\" style=\"width:"+width+"px;\">\n");
		for(int i = 0;i < options.length;i++)
		{
			if(selected_value != null && selected_value.equals(values[i]))
				buf.append("<OPTION SELECTED value=\""+values[i]+"\" > "+options[i]+"</OPTION>\n");
			else
				buf.append("<OPTION value=\""+values[i]+"\" > "+options[i]+"</OPTION>\n");
		}
		buf.append("</SELECT>\n");		
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
		buf.append("<INPUT TYPE='submit' NAME='"+label+"' value='"+label+"'/>");
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
	
	protected void HR(UserApplicationContext uctx)
	{
		HR(get_user_buf(uctx));
	}
	
	protected void HR(StringBuilder buf)
	{
		buf.append("<HR/>\n");
	}
	
	protected void P(UserApplicationContext uctx)
	{
		P(get_user_buf(uctx));
	}
	
	protected void P(StringBuilder buf)
	{
		buf.append("<P/>\n");
	}

	protected void PRE(UserApplicationContext uctx,String text)
	{
		PRE(get_user_buf(uctx),text);
	}
	
	protected void PRE(UserApplicationContext uctx,String text,int width)
	{
		PRE(get_user_buf(uctx),text,width);
	}
	
	protected void PRE(StringBuilder buf,String text)
	{
		buf.append("<PRE>\n");
		buf.append(text);
		buf.append("</PRE>\n");
	}
	
	protected void PRE(StringBuilder buf,String text,int width)
	{
		buf.append("<PRE style='width:"+width+"px;'>\n");
		buf.append(text);
		buf.append("</PRE>\n");
	}

	
	protected void A(UserApplicationContext uctx,String module_name,int submode,String text,Object... params)
	{
		A(get_user_buf(uctx), module_name, submode, text, params);
	}
	
	protected void A(StringBuilder buf,String module_name,int submode,String text,Object... params)
	{

		String form_name   = "f_"+Util.getGUID().substring(0,8);
		String form_action = RAW_MODULE_ROOT()+"/"+module_name+"/Exec/.raw";
		
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
		
		A(buf,RAW_MODULE_ROOT()+"/"+module_name+"/Exec/.raw?"+KEY_UI_MODULE_SUBMODE_KEY+"="+String.valueOf(submode)+"&"+p_buf.toString(),text);
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
	
	protected void TH(UserApplicationContext uctx,String data)
	{
		TH(get_user_buf(uctx),data);
	}
	
	protected void TH(StringBuilder buf,String data)
	{
		buf.append("<TH>"+data+"</TH>\n");		
	}
	
	protected void TD(UserApplicationContext uctx,String data)
	{
		TD(get_user_buf(uctx),data);
	}
	
	protected void TD(StringBuilder buf,String data)
	{
		buf.append("<TD>"+data+"</TD>\n");		
	}

	protected void TD_LINK(UserApplicationContext uctx,String module_name,int submode,String text,Object... params)
	{
		TD_LINK(get_user_buf(uctx),module_name,submode,text,params);
	}
	
	protected void TD_LINK(StringBuilder buf,String module_name,int submode,String text,Object... params)
	{
		buf.append("<TD>");	
		A(buf,module_name,submode,text,params);
		buf.append("</TD>");
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
		buf.append("<SPAN style='color:"+color+";'>"+text+"</SPAN>\n");
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
	
	protected void IMG(UserApplicationContext uctx,String url)
	{
		IMG(get_user_buf(uctx),url);
	}
	
	protected void IMG(StringBuilder buf,String url)
	{
		buf.append("<img src='"+url+"'>\n");
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
	

	protected void JS_TIMED_REDIRECT(UserApplicationContext uctx,String module_name,int submode,int ms,Object... params)
	{
		String url = RAW_MODULE_ROOT()+"/"+module_name+"/Exec/.raw";
		if(submode != RAW_SUBMODE_DEFAULT)
			url += "?"+KEY_UI_MODULE_SUBMODE_KEY+"="+submode;
			
		if(params.length > 0 )
		{
			if(submode == RAW_SUBMODE_DEFAULT)
				url +="?";
			else
				url += "&";
		}
		
		StringBuilder p_buf = new StringBuilder();
		for(int i = 0;i< params.length;i+=2)
		{
			p_buf.append(params[i]+"="+String.valueOf(params[i+1]));
			p_buf.append("&");
		}
		if(params.length > 0)
			p_buf.setLength(p_buf.length()-1);
		url += p_buf.toString();
		JS_TIMED_REDIRECT(get_user_buf(uctx),url, ms);
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
	
	protected void INSERT(UserApplicationContext uctx,String contents)
	{
		INSERT(get_user_buf(uctx) , contents);
	}
	
	protected void INSERT(StringBuilder buf,String contents)
	{
		buf.append(contents);
	}
	
	protected void DIV_START(UserApplicationContext uctx,String classname,String id,String... styles)
	{
		DIV_START(get_user_buf(uctx) ,classname,id,styles);
	}
	
	protected void DIV_START(StringBuilder buf,String classname,String id,String... styles)
	{
		StringBuilder stylestring_buf = new StringBuilder();
		if(styles.length != 0)
		{
			stylestring_buf.append("style='");
			for(int i = 0;i < styles.length;i+=2)
			{
				stylestring_buf.append(styles[i]+":"+styles[i+1]+";");
			}
			stylestring_buf.append("'");
		}
		
		String class_string = "";
		if(classname != null)
			class_string = "class='"+classname+"'";
		
		String id_string = "";
		if(id != null)
			id_string = "id='"+id+"'";


			buf.append("<DIV "+class_string+" "+id_string+" "+stylestring_buf.toString()+">\n");	

	}
	
	protected void DIV_END(UserApplicationContext uctx)
	{
		DIV_END(get_user_buf(uctx));
	}
	
	protected void DIV_END(StringBuilder buf)
	{
		buf.append("</DIV>\n");
	}
	protected String RAW_MODULE_EXEC_ROOT()
	{
		return getApplication().getConfig().getWebRootUrl()+"/"+getName()+"/Exec/.raw";
	}
	
	protected String RAW_MODULE_ROOT()
	{
		if(secure)
			return getApplication().getConfig().getWebRootUrlSecure();
		else
			return getApplication().getConfig().getWebRootUrl();
	}
	
	
	protected void DISPLAY_ERROR(UserApplicationContext uctx,Map<String,Object> params)
	{
		String error = (String)params.get(KEY_UI_MODULE_ERROR_KEY);
		if(error == null)
			return;
		StringBuilder buf = get_user_buf(uctx);
		BR(uctx);
		SPAN(uctx, error,RAW_UI_ERROR_COLOR,10);
	}
	
	protected void DISPLAY_INFO(UserApplicationContext uctx,Map<String,Object> params)
	{
		String error = (String)params.get(KEY_UI_MODULE_INFO_KEY);
		if(error == null)
			return;
		StringBuilder buf = get_user_buf(uctx);
		BR(uctx);
		SPAN(uctx, error,RAW_UI_INFO_COLOR,10);

	}
	
	protected void ERROR_PAGE(UserApplicationContext uctx,Exception e)
	{
		ERROR(e);//log error//
		StringBuilder buf = get_user_buf(uctx);
		DOCUMENT_START(buf, getName()+"ErrorOccurred", RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_ERROR_COLOR, 14,14, RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
		SPAN(uctx, e.getMessage(), 16);
		DOCUMENT_END(buf);
	}
	
	public static final int RAW_SUBMODE_DEFAULT    = 0x00;
	private static final String KEY_UI_MODULE_STACK 			= "__ui_module_stack__";
	protected static final String KEY_UI_MODULE_OUTPUT_BUF   		= "__ui_module_output__";
	private static final String KEY_UI_MODULE_RAW_COMMUNIQUE   	= "__ui_module_raw_communique__";
	private static final String KEY_UI_MODULE_SUBMODE_KEY   	= "submode";
	protected static final String KEY_UI_MODULE_INFO_KEY   		= "_info_";
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
	
		if(canExecSubmode((Entity)uctx.getUser(),callee_submode,params))
			execute_submode(uctx, callee_submode, params);
		else
			ERROR_PAGE(uctx, new PermissionsException("NO PERMISSION."));
		
		HttpServletResponse response = (HttpServletResponse)c.getResponse();
		try{
			response.getWriter().println(get_user_buf(uctx).toString());
		}
		catch(IllegalStateException ise)
		{
			//swallow it. someone has already accessed the output stream.
			//of the RawCommunique. The only place this currently happens
			//is in exceldump module when we send the dump file back using
			//the response directly
		}
		catch(Exception e4)
		{
			ERROR(e4);
		}
	}
	

	protected RawCommunique GET_RAW_COMMUNIQUE(UserApplicationContext uctx)
	{
		return (RawCommunique)uctx.getProperty(KEY_UI_MODULE_RAW_COMMUNIQUE);
	}
	protected boolean canExecSubmode(Entity user,int submode,Map<String,Object> params)
	{
		return true;
	}
	
	protected Object execute_module_submode(UserApplicationContext uctx,String module_name,int submode,Map<String,Object> params)
	{
		return ((RawUIModule)getApplication().getModule(module_name)).execute_submode(uctx, submode, params);
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
		execute_module_submode(uctx, caller_module, caller_return_to_submode, new HashMap<String,Object>() );//could actually return params..probs name value pairs
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
		//System.out.println("PUSHING FRAME "+f);
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
			params.put((String)name_val_pairs[i], String.valueOf(name_val_pairs[i+1]));

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
			params.put((String)name_val_pairs[i], String.valueOf(name_val_pairs[i+1]));
		GOTO_WITH_ERROR(uctx, submode, error, params);
	}
	
	protected void GOTO_WITH_ERROR(UserApplicationContext uctx,int submode,String error,Map<String,Object> params)
	{
		params.put(KEY_UI_MODULE_ERROR_KEY, error);
		execute_submode(uctx, submode, params);	
	}
	
	protected void SET_ERROR(String error,Map<String,Object> params)
	{
		params.put(KEY_UI_MODULE_ERROR_KEY, error);
	}
	
	protected boolean IS_NULL(String s)
	{
		return s == null || s.trim().equals("");
	}
	
	protected String NORMALIZE(String s)
	{
		if(s == null) 
			return null;
		s = s.trim();
		if(s.equals(""))
			return null;
		return s;
	}
	
	protected void REQUIRED(String name,Object val) throws Exception
	{
		if(val == null)
			throw new Exception(name+" is required");
	}
	
	/* parse comma seperated string */
	protected List<String> PARSE_LIST(String s)
	{ 
		if(s == null) 
			return null;
		s = s.trim();
		if(s.equals(""))
			return null;
		String[] ss = s.split(",");
		List<String> ret = new ArrayList<String>();
		for(int i = 0;i < ss.length;i++)
			ret.add(ss[i].trim());
		return ret;
	}
	
	//TODO also need one that defaults return to submode//
	protected void CALL(UserApplicationContext uctx,String module_name,int submode,int return_to_submode,Object...name_val_pairs)
	{

		Map<String,Object> params = new HashMap<String,Object>();
		for(int i = 0;i < name_val_pairs.length;i+=2)
			params.put((String)name_val_pairs[i], name_val_pairs[i+1]);
		
		CALL(uctx, module_name, submode, return_to_submode, params);
	}
	
	protected void CALL(UserApplicationContext uctx,String module_name,int submode,int return_to_submode,Map<String,Object> params)
	{
		push_caller(uctx,getName(),return_to_submode);		
		execute_module_submode(uctx, module_name, submode, params);
	}
	
	//TODO also need one that defaults return to submode//
	protected void CALL_WITH_INFO(UserApplicationContext uctx,String module_name,int submode,int return_to_submode,String info_message,Object...name_val_pairs)
	{
		CALL(uctx,module_name,submode,return_to_submode,KEY_UI_MODULE_INFO_KEY,info_message);
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
	
	protected StringBuilder get_user_buf(UserApplicationContext uctx)
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
	
	
	public void DUMP_PARAMS(Map<String,Object> params)
	{
		

		INFO("DUMP PARAMS");
		Iterator<String> it = params.keySet().iterator();
		while(it.hasNext())
		{
			String key = it.next();
			INFO("\t"+key+" = "+String.valueOf(params.get(key)));
		}
	}
	
}
