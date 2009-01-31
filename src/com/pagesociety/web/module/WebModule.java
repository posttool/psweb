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
import java.util.Calendar;
import java.util.Date;
import java.util.Map;


import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.PermissionsException;
import com.pagesociety.web.exception.WebApplicationException;


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
	protected void DOCUMENT_START(StringBuilder buf,String title,String bgcolor,String font_family,String font_color,int font_size,String link_color,String link_hover_color)
	{
		HTML_START(buf);
		HEAD_START(buf,title);
		STYLE(buf,bgcolor,font_family,font_color,font_size,link_color,link_hover_color);
		HEAD_END(buf);
		BODY_START(buf, bgcolor, font_family, font_color, font_size);
		
	}
	
	protected void DOCUMENT_END(StringBuilder buf)
	{
		BODY_END(buf);
		HTML_END(buf);		
	}
	
	protected void HTML_START(StringBuilder buf)
	{
		buf.append("<HTML>\n");
	}
	
	protected void HEAD_START(StringBuilder buf,String title)
	{
		buf.append("<HEAD> \n<TITLE>"+title+"</TITLE>\n");
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
	
	protected void HEAD_END(StringBuilder buf)
	{
		buf.append("</HEAD>");
	}
	
	
	protected void BODY_START(StringBuilder buf,String bgcolor,String font_family,String font_color,int font_size)
	{
		if(font_family == null)
			font_family = "arial";
		buf.append("<BODY style='background-color:"+bgcolor+";font-family:"+font_family+";color:"+font_color+";font-size:"+String.valueOf(font_size)+"px'>");
	}
	
	protected void FORM_START(StringBuilder buf,String action,String method)
	{
		buf.append("<FORM ACTION='"+action+"' METHOD='"+method+"'>\n");
		
	}	  

	protected void MULTIPART_FORM_START(StringBuilder buf,String action)
	{
		buf.append("<FORM ACTION='"+action+"' METHOD='post' ENCTYPE='multipart/form-data'>\n");
	}
	protected void FILE_INPUT_FIELD(StringBuilder buf,String name)
	{
		buf.append("<INPUT TYPE='file' name='"+name+"'/>");
	}
	
	protected void FORM_INPUT_FIELD(StringBuilder buf,String name,int size)
	{
		buf.append("<INPUT TYPE='text' name='"+name+"' size='"+size+"'/>\n");
	}
	
	protected void FORM_PASSWORD_FIELD(StringBuilder buf,String name,int size)
	{
		buf.append("<INPUT TYPE='password' name='"+name+"' size='"+size+"'/>\n");
	}
	
	protected void FORM_HIDDEN_FIELD(StringBuilder buf,String name,String value)
	{
		buf.append("<INPUT TYPE='hidden' name='"+name+"' value='"+value+"'/>\n");
	}
	
	protected void FORM_SUBMIT_BUTTON(StringBuilder buf,String label)
	{
		buf.append("<INPUT TYPE='submit' value='"+label+"'/>");
	}
	
	protected void FORM_END(StringBuilder buf)
	{
		buf.append("</FORM>\n");
	}
	
	protected void BODY_END(StringBuilder buf)
	{
		buf.append("</BODY>\n");
	}
	protected void HTML_END(StringBuilder buf)
	{
		buf.append("</HTML>\n");
	}
    
	protected void BR(StringBuilder buf)
	{
		buf.append("<BR/>\n");
	}
	
	protected void P(StringBuilder buf)
	{
		buf.append("<P/>\n");
	}
	
	protected void A(StringBuilder buf,String url,String text)
	{
		buf.append("<A HREF='"+url+"'>"+text+"</A>");
	}
	
	protected void TABLE_START(StringBuilder buf,int border,int width)
	{
		buf.append("<TABLE BORDER='"+border+"' WIDTH='"+width+"'>\n");
	}
	
	protected void TR_START(StringBuilder buf)
	{
		buf.append("<TR>\n");
	}
	
	protected void TD(StringBuilder buf,String data)
	{
		buf.append("<TD>"+data+"</TD>\n");		
	}
	
	protected void TD_START(StringBuilder buf)
	{
		buf.append("<TD>\n");		
	}
	
	protected void TD_END(StringBuilder buf)
	{
		buf.append("</TD>\n");		
	}

	
	protected void TR_END(StringBuilder buf)
	{
		buf.append("</TR>\n");
	}
	
	protected void TABLE_END(StringBuilder buf)
	{
		buf.append("</TABLE>\n");				
	}

	protected void SPAN(StringBuilder buf,String text,String color,int size)
	{
		buf.append("<SPAN style='color:"+color+";font-size:"+size+"px'>"+text+"</SPAN>\n");
	}
	
	protected void SPAN(StringBuilder buf,String text, int size)
	{
		buf.append("<SPAN style='font-size:"+size+"px'>"+text+"</SPAN>\n");
	}
	
	protected void SPAN(StringBuilder buf,String text, String color)
	{
		buf.append("<SPAN style='color:"+color+";>"+text+"</SPAN>\n");
	}
	
	protected void SPAN(StringBuilder buf,String text)
	{
		buf.append("<SPAN>"+text+"</SPAN>\n");
	}
	
	protected void NBSP(StringBuilder buf,int no)
	{
		for(int i = 0;i < no;i++)
		{
			NBSP(buf);
		}
	}
	protected void NBSP(StringBuilder buf)
	{
		buf.append("&nbsp;");
	}
	
	protected void JS_REDIRECT(StringBuilder buf,String url)
	{
		buf.append("<script type='text/javascript'>\n");
		buf.append("window.location = "+url+"\n");
		buf.append("</script>\n");
	}
	protected String RAW_MODULE_ROOT()
	{
		return getApplication().getConfig().getWebRootUrl()+"/"+getName();
	}

	
}
