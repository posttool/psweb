package com.pagesociety.web.module.dump;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PermissionsModule;
import com.pagesociety.web.module.RawUIModule;
import com.pagesociety.web.upload.MultipartForm;
import com.pagesociety.web.upload.MultipartFormException;


public class ExcelDumpModuleRawUI extends RawUIModule
{
	private static final String SLOT_DUMP_MODULE = "dump-module";
	protected ExcelDumpModule dump_module;
	private SimpleDateFormat filename_date_formatter;
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		dump_module = (ExcelDumpModule)getSlot(SLOT_DUMP_MODULE);
		filename_date_formatter = new SimpleDateFormat("yyyy_MM_dd_HHmmss");
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_DUMP_MODULE,ExcelDumpModule.class,true);
	}

	public void exec(UserApplicationContext uctx,Map<String,Object> params) throws WebApplicationException,FileNotFoundException,IOException,PersistenceException
	{
		String state = (String)params.get("state");
		if(state == null)
			state = "default";
		if(!PermissionsModule.IS_ADMIN((Entity)uctx.getUser()))
		{
			CALL_WITH_INFO(uctx,"UserModuleRawUI",RAW_SUBMODE_DEFAULT,RAW_SUBMODE_DEFAULT,"Excel Dump module requires admin login.");
		}
		else if(state.equals("excel_dump"))
		{
			do_excel_dump((HttpServletResponse)GET_RAW_COMMUNIQUE(uctx).getResponse());
		}
		else if(state.equals("restore"))
		{
			render_restore_screen(get_user_buf(uctx),null);
		}
		else if(state.equals("default"))
		{
			render_welcome_screen(get_user_buf(uctx),null);
		}
		else
		{
			ERROR_PAGE(uctx,new WebApplicationException("UNKNOW STATE "+state));
		}
	}
	
	@Export
	public String UploadAndRestoreDb(UserApplicationContext uctx,MultipartForm upload)
	{
		
		File tmp = new File(System.getProperty("java.io.tmpdir")+File.separator+"excel_dump_upload");
		tmp.mkdir();
		upload.setUploadDirectory(tmp);
		upload.setMaxUploadItemSize(0);//unlimited//
		try{
			upload.parse();
		}catch(MultipartFormException e)
		{
			ERROR(e);
			return ("<font color='red'>ERROR: "+e.getClass().getName()+" "+e.getMessage()+"</FONT>");
		}
		
		List<File> 	uploaded_files 	= upload.getFiles();
		File f 						= uploaded_files.get(0);
		
		StringBuilder buf = new StringBuilder();
		
		try{
			if(!f.getName().endsWith(".xls"))
			{
				render_restore_screen(buf,f.getName()+" NOT A VALID RESTORE SPREADSHEET.");
				return buf.toString();
			}
			dump_module.restoreDbFromExcelFile(f);
			f.delete();
		}catch(Exception e)
		{
			ERROR(e);
			return ("<font color='red'>ERROR: "+e.getClass().getName()+" <"+e.getMessage()+"</FONT>");
		}
		render_welcome_screen(buf,"DATABASE WAS RESTORED SUCCESSFULLY FROM FILE "+f.getName()+".");
		return buf.toString();
	}
	
	private void render_welcome_screen(StringBuilder buf, String message)
	{

		DOCUMENT_START(buf, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
		P(buf);
		SPAN(buf,"DATABASE EXCEL DUMP UTILITY.",18);
		P(buf);
		if(message != null)
		{
			SPAN(buf,message,RAW_UI_INFO_COLOR,16);
			P(buf);
		}
		NBSP(buf,4);A(buf,RAW_MODULE_ROOT()+"/Exec/.raw?state=excel_dump","[ DUMP DATABASE ]");
		P(buf);
		NBSP(buf,4);A(buf,RAW_MODULE_ROOT()+"/Exec/.raw?state=restore","[ RESTORE DATABASE ]");
		DOCUMENT_END(buf);
	}
	
	
	public void render_restore_screen(StringBuilder buf,String message)
	{
		DOCUMENT_START(buf, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
		if(message != null)
		{
			SPAN(buf,message,RAW_UI_ERROR_COLOR,16);
			P(buf);
		}
		MULTIPART_FORM_START(buf,RAW_MODULE_ROOT()+"/"+getName()+"/UploadAndRestoreDb/.form");
		SPAN(buf,"Please choose the file you wish to restore from:<br>",16);
		BR(buf);
		FILE_INPUT_FIELD(buf, "file");
		FORM_SUBMIT_BUTTON(buf, "Restore");
		FORM_END(buf);
		A(buf,RAW_MODULE_EXEC_ROOT()+"/Exec/.raw","[ BACK ]");
		DOCUMENT_END(buf);
	}


	
	
	public void do_excel_dump(HttpServletResponse response) throws IOException,PersistenceException
	{
		
		Calendar c = Calendar.getInstance();
		Date now = new Date();
	    response.setContentType("application/vnd.ms-excel");
	    response.setHeader("Content-Disposition", "attachment; filename=" + getApplication().getConfig().getName()+"_Backup_"+filename_date_formatter.format(now)+".xls"); 
	    response.setHeader("Pragma"," no-cache");
	    response.setHeader("Cache-Control"," no-cache,must-revalidate");
	    response.setStatus(response.SC_OK);
	    dump_module.dumpDbToExcelFormat(response.getOutputStream());
	}
	
}
