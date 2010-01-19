package com.pagesociety.web.module.qa;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.gateway.RawCommunique;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.RawUIModule;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.permissions.PermissionEvaluator;
import com.pagesociety.web.upload.MultipartForm;
import com.pagesociety.web.upload.MultipartFormException;


public class BugReporterRawUI extends RawUIModule
{
	private static final String SLOT_BUG_REPORTER_MODULE = "bug-reporter-module";
	protected BugReporterModule bug_reporter_module;

	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		bug_reporter_module = (BugReporterModule)getSlot(SLOT_BUG_REPORTER_MODULE);
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_BUG_REPORTER_MODULE,BugReporterModule.class,true);
	}

	private static final int RAW_SUBMODE_VIEW_BUGS  = 0x01;
	private static final int RAW_SUBMODE_VIEW_BUG	= 0x02;

	protected void declareSubmodes(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		try{
			declareSubmode(RAW_SUBMODE_DEFAULT,    "submode_default");
			declareSubmode(RAW_SUBMODE_VIEW_BUGS,  "submode_view_bugs");
			declareSubmode(RAW_SUBMODE_VIEW_BUG,   "submode_view_bug");
		}catch(Exception e)
		{
			ERROR(e);
			throw new InitializationException("FAILED BINDING SUBMODE "+e.getMessage());
		}
	}
	
	
	@Export
	public String CreateBug(UserApplicationContext uctx,MultipartForm upload)
	{
		StringBuilder buf = new StringBuilder();
		uctx.setProperty(RawUIModule.KEY_UI_MODULE_OUTPUT_BUF,buf);
		File tmp = new File(System.getProperty("java.io.tmpdir")+File.separator+"bugzz_img_upload");
		tmp.mkdir();
		
		File f 					= null;
		FileOutputStream fos 	= null;
		String upload_filename  = NORMALIZE(upload.getFileName());
		if(upload_filename != null)
		{
			try{
				f = new File(tmp,upload.getFileName());
				fos = new FileOutputStream(f);
			}catch(Exception e){e.printStackTrace();}
			try{
				
				upload.parse(fos);
			}catch(MultipartFormException e)
			{
				ERROR(e);
				GOTO_WITH_ERROR(uctx, RAW_SUBMODE_DEFAULT, "Bug Submit ERROR:"+e.getMessage());
				return buf.toString();
			}
		}
		
		String submitter 	= upload.getParameter("submitter");
		String description 	= upload.getParameter("description");
		if(description == null)
			description = "";
		Entity bug = null;
		try{
			bug = bug_reporter_module.createBug(null,submitter , description, f);
		}catch(Exception e)
		{
			ERROR(e);
			GOTO_WITH_ERROR(uctx, RAW_SUBMODE_DEFAULT, "Bug Submit ERROR:"+e.getMessage());
			return buf.toString();
		}
		

		GOTO_WITH_INFO(uctx, RAW_SUBMODE_DEFAULT, "Bug Submit OK. Thank you!","show_bug",bug.getId());
		return buf.toString();
	}
	

	public void submode_default(UserApplicationContext uctx,Map<String,Object> params)
	{
		try{
			if(params.get("show_bug") != null)
			{
				long bid = Long.parseLong((String)params.get("show_bug"));
				Entity bug = bug_reporter_module.getBugById(bid);
				
				String submitter 	= (String)bug.getAttribute(BugReporterModule.PS_BUG_FIELD_SUBMITTER);
				String description 	= ((String)bug.getAttribute(BugReporterModule.PS_BUG_FIELD_DESCRIPTION)).replaceAll("\n", "<br/>");
				String screenshot 	= (String)bug.getAttribute(BugReporterModule.PS_BUG_FIELD_SCREENSHOT);
				String url = "/BugReporterRawUI/Image/.raw?bid="+String.valueOf(bid);
				DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR,RAW_UI_FONT_SIZE, 14,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
				P(uctx);
				SPAN(uctx,"Bugz =]",18);
				DISPLAY_ERROR(uctx,params);
				DISPLAY_INFO(uctx,params);
				P(uctx);
				TABLE_START(uctx,0,1000);
					TR_START(uctx);
						TD(uctx,"Submitted By:");TD(uctx,submitter);
					TR_END(uctx);
					TR_START(uctx);
						TD(uctx,"Description:");TD(uctx,description);
					TR_END(uctx);
					TR_START(uctx);
						TD(uctx,"Screen Shot:");TD_START(uctx);
						if(screenshot!=null)
							IMG(uctx,url);
						else
							NBSP(uctx);
						TD_END(uctx);
					TR_END(uctx);
				TABLE_END(uctx);
				HR(uctx);
				A(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ Back ]");
				DOCUMENT_END(uctx);
				return;
			}
			
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,"REPORT A BUG",18);
			DISPLAY_ERROR(uctx,params);
			DISPLAY_INFO(uctx,params);
			P(uctx);
			MULTIPART_FORM_START(uctx,RAW_MODULE_ROOT()+"/"+getName()+"/CreateBug/.form");
			TABLE_START(uctx,0,800);
			TR_START(uctx);
				TD_START(uctx);SPAN(uctx,"Reported By:",16);TD_END(uctx);
				TD_START(uctx);FORM_INPUT_FIELD(uctx, "submitter", 32, "");TD_END(uctx);
			TR_END(uctx);
			TR_START(uctx);
				TD_START(uctx);SPAN(uctx,"Description:",16);TD_END(uctx);
				TD_START(uctx);FORM_TEXTAREA_FIELD(uctx, "description", 60, 16);TD_END(uctx);
			TR_END(uctx);
				TR_START(uctx);
					TD_START(uctx);SPAN(uctx,"Screen Shot:",16);TD_END(uctx);
					TD_START(uctx);FILE_INPUT_FIELD(uctx, "file");TD_END(uctx);
				TR_END(uctx);
			TABLE_END(uctx);
			P(uctx);
			FORM_SUBMIT_BUTTON(uctx, "+ Create Bug");
			FORM_END(uctx);
			HR(uctx);
			A(uctx,getName(),RAW_SUBMODE_VIEW_BUGS,"[ View Bugs ]");
			DOCUMENT_END(uctx);
		}catch(Exception e)
		{
			ERROR_PAGE(uctx,e);
		}
	}
	
	public void submode_view_bugs(UserApplicationContext uctx,Map<String,Object> params)
	{
		Entity user = (Entity)uctx.getUser();
		if(!PermissionEvaluator.IS_ADMIN(user))
		{
			CALL_WITH_INFO(uctx,"UserModuleRawUI",RAW_SUBMODE_DEFAULT,RAW_SUBMODE_VIEW_BUGS,"Viewing bugs requires admin login.");
			return;
		}
	try{
			if(params.get("delete_bid") != null)
			{
				long bid =Long.parseLong((String)params.get("delete_bid"));
				bug_reporter_module.deleteBug(bid);
			}	
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR,RAW_UI_FONT_SIZE, 10,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,"Bugz =]",18);
			DISPLAY_ERROR(uctx,params);
			DISPLAY_INFO(uctx,params);
			P(uctx);
			TABLE_START(uctx, 1, 1000);
			TR_START(uctx);
			TH(uctx,"ID");
			TH(uctx,"Reporter");
			TH(uctx,"Date");
			TH(uctx,"Description");
			TH(uctx,"Num Annotations");
			TH(uctx,"");
			TH(uctx,"");
			TR_END(uctx);
			List<Entity> bugs = bug_reporter_module.getAllBugs();
			for(int i = 0;i < bugs.size();i++)
			{
				
				Entity bug = bugs.get(i);
				String submitter = (String) bug.getAttribute(BugReporterModule.PS_BUG_FIELD_SUBMITTER); 
				String description 	= ((String)bug.getAttribute(BugReporterModule.PS_BUG_FIELD_DESCRIPTION));
				int no_annotations =	bug.getAttribute(BugReporterModule.PS_BUG_FIELD_ANNOTATIONS)==null?0:((List<String>)bug.getAttribute(BugReporterModule.PS_BUG_FIELD_ANNOTATIONS)).size();
				if(description.length() > 65)
					description = description.substring(0, 65);
				String id = String.valueOf(bug.getId());

				TR_START(uctx);
					TD(uctx,id);
					TD(uctx,submitter);
					TD(uctx,String.valueOf(bug.getAttribute(WebStoreModule.FIELD_LAST_MODIFIED)));
					TD(uctx,description);
					TD(uctx,String.valueOf(no_annotations));
					TD_LINK(uctx, getName(), RAW_SUBMODE_VIEW_BUG, "[ view ]", "bid",bug.getId());
					TD_LINK(uctx, getName(), RAW_SUBMODE_VIEW_BUGS, "[ delete ]", "delete_bid",bug.getId());
				TR_END(uctx);
			}
			
			TABLE_END(uctx);
			HR(uctx);
			A(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ Back ]");
			DOCUMENT_END(uctx);
			
		}catch(Exception e)
		{
			ERROR_PAGE(uctx, e);
		}
	}
	
	public void submode_view_bug(UserApplicationContext uctx,Map<String,Object> params)
	{
		Entity user = (Entity)uctx.getUser();
		if(!PermissionEvaluator.IS_ADMIN(user))
		{
			CALL_WITH_INFO(uctx,"UserModuleRawUI",RAW_SUBMODE_DEFAULT,RAW_SUBMODE_VIEW_BUGS,"Coupon promotion manager module requires admin login.");
			return;
		}
		try{
			if(params.get("add_annotation") != null)
			{
				long bid =Long.parseLong((String)params.get("bid"));
				String annotation = (String)params.get("annotation");
				bug_reporter_module.createAnnotation(bid,annotation);
			}
	
			long bid =Long.parseLong((String)params.get("bid"));
			Entity bug = bug_reporter_module.getBugById(bid);
			String submitter 	= (String)bug.getAttribute(BugReporterModule.PS_BUG_FIELD_SUBMITTER);
			String description 	= ((String)bug.getAttribute(BugReporterModule.PS_BUG_FIELD_DESCRIPTION)).replaceAll("\n", "<br/>");
			String screenshot 	= (String)bug.getAttribute(BugReporterModule.PS_BUG_FIELD_SCREENSHOT);
			String url = "/BugReporterRawUI/Image/.raw?bid="+String.valueOf(bid);
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR,RAW_UI_FONT_SIZE, 14,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,"Bugz =]",18);
			DISPLAY_ERROR(uctx,params);
			DISPLAY_INFO(uctx,params);
			P(uctx);
			TABLE_START(uctx,0,1000);
				TR_START(uctx);
					TD(uctx,"Submitted By:");TD(uctx,submitter);
				TR_END(uctx);
				TR_START(uctx);
					TD(uctx,"Description:");TD(uctx,description);
				TR_END(uctx);
				TR_START(uctx);
					TD(uctx,"Screen Shot:");TD_START(uctx);
					if(screenshot!=null)
						IMG(uctx,url);
					else
						NBSP(uctx);
					TD_END(uctx);
				TR_END(uctx);
			TABLE_END(uctx);
		
			P(uctx);
			HR(uctx);
			SPAN(uctx, "Annotations:");
			BR(uctx);
			List<String> annotations = (List<String>)bug.getAttribute(BugReporterModule.PS_BUG_FIELD_ANNOTATIONS);
			if(annotations != null)
			{
				for(int i = 0;i < annotations.size();i++)
				{
					String annotation = annotations.get(i);
					if(annotation != null)
						annotation = annotation.replaceAll("\n", "<br/>");
					SPAN(uctx, annotations.get(i),14);
					P(uctx);
				}
			
			}
			FORM_START(uctx,getName(),RAW_SUBMODE_VIEW_BUG,"bid",bid,"add_annotation",true);
			FORM_TEXTAREA_FIELD(uctx, "annotation", 60, 8);
			BR(uctx);
			FORM_SUBMIT_BUTTON(uctx,"+ Add Annotation");
			FORM_END(uctx);
			HR(uctx);
			A(uctx,getName(),RAW_SUBMODE_VIEW_BUGS,"[ Back ]");
			DOCUMENT_END(uctx);
		}catch(Exception e)
		{
			ERROR_PAGE(uctx, e);
		}
	}
	
	@Export
	public void Image(UserApplicationContext uctx, RawCommunique c) throws PersistenceException
	{
		HttpServletRequest request = (HttpServletRequest)c.getRequest();
		HttpServletResponse response = (HttpServletResponse)c.getResponse();
		long bid = Long.parseLong(request.getParameter("bid"));
		File f = bug_reporter_module.getBugScreenshot(bid);
		if(f == null)
		{
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		String filename = f.getName();
		String content_type = null;
		if(filename.endsWith("jpg") || filename.endsWith("jpeg"))
			content_type = "image/jpeg";
		else if(filename.endsWith("png"))
			content_type = "image/png";
		else if(filename.endsWith("pic") || filename.endsWith("pict"))
			content_type = "image/pict";
		else if(filename.endsWith("gif"))
			content_type = "image/gif";

        response.setContentType(content_type); /* set the MIME type */
    	byte[] arBytes = new byte[(int) f.length()];
    	try{
    		FileInputStream is= new FileInputStream(f);
    		is.read(arBytes);
    		OutputStream os = response.getOutputStream();
    		os.write(arBytes);
    		os.flush();
    	}catch(Exception e)
    	{
    		e.printStackTrace();
    	}
	}
	
}
