package com.pagesociety.web.module.comment;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.gateway.Form;
import com.pagesociety.web.gateway.RawCommunique;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.RawUIModule;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.permissions.PermissionEvaluator;
import com.pagesociety.web.module.qa.BugReporterModule;
import com.pagesociety.web.upload.MultipartForm;
import com.pagesociety.web.upload.MultipartFormException;


public class CommentApproveRawUI extends RawUIModule
{
	private static final String SLOT_COMMENT_MODULE = "comment-module";
	protected CommentModule comment_module;


	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);
		comment_module = (CommentModule)getSlot(SLOT_COMMENT_MODULE);
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_COMMENT_MODULE,CommentModule.class,true);
	}



	protected void declareSubmodes(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		try{
			declareSubmode(RAW_SUBMODE_DEFAULT,    "submode_default");
		}catch(Exception e)
		{
			ERROR(e);
			throw new InitializationException("FAILED BINDING SUBMODE "+e.getMessage());
		}
	}


	@Export
	public String Submit(UserApplicationContext uctx,Form form)
	{
		Entity user = (Entity)uctx.getUser();
		if(!PermissionEvaluator.IS_ADMIN(user))
		{
			return "NO PERMISSION";
		}
		StringBuilder buf = new StringBuilder();
		uctx.setProperty(RawUIModule.KEY_UI_MODULE_OUTPUT_BUF,buf);
		Set<String> keys = form.keys();
	    Iterator<String> iter = keys.iterator();
	    try{
		    while (iter.hasNext())
		    {
		    	String key 		= iter.next();
		    	if(key.startsWith("comment_"))
		    	{
			    	String value 	= form.getParameter(key);
		    		Long id = Long.parseLong(key.substring(8));
		    		if(value.equals("Delete"))
		    		{
		    			comment_module.DeleteComment(uctx, id);
		    		}
		    		else if(value.equals("Approve"))
		    		{

		    			comment_module.ApproveComment(uctx, id);

		    		}
		    	}
		    }
		    GOTO_WITH_INFO(uctx, RAW_SUBMODE_DEFAULT, "Action OK. ");
		    return buf.toString();
	    }catch(Exception e)
		{
			ERROR(e);
			GOTO_WITH_ERROR(uctx, RAW_SUBMODE_DEFAULT, "ERROR:"+e.getMessage());
			return buf.toString();
		}
		/*
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


		GOTO_WITH_INFO(uctx, RAW_SUBMODE_DEFAULT, "Action OK. ");
	*/


	}

	private static final String[] ACTION_NAMES = new String[]{"Approve","Delete"};
	public void submode_default(UserApplicationContext uctx,Map<String,Object> params)
	{
		try{
			Entity user = (Entity)uctx.getUser();
			if(!PermissionEvaluator.IS_ADMIN(user))
			{
				CALL_WITH_INFO(uctx,"UserModuleRawUI",RAW_SUBMODE_DEFAULT,RAW_SUBMODE_DEFAULT,getName()+" manager module requires admin login.");
				return;
			}
			if(params.get("do_delete_all") != null)
			{
				List<Entity> cc = comment_module.getAllComments(0, Query.ALL_RESULTS).getEntities();
				for(int i = 0;i < cc.size();i++)
				{
					comment_module.DeleteComment(uctx,cc.get(i));
				}

			}
			if(params.get("do_unapprove_all") != null)
			{
				comment_module.unapproveAllComments();
			}
			PagingQueryResult result = comment_module.getAllUnapprovedComments(  0, Query.ALL_RESULTS);
			List<Entity> comments = result.getEntities();

			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,"MANAGE COMMENTS",16);
			DISPLAY_ERROR(uctx,params);
			DISPLAY_INFO(uctx,params);
			P(uctx);
			FORM_START(uctx,RAW_MODULE_ROOT()+"/"+getName()+"/Submit/.form","POST");
			TABLE_START(uctx,1,800);
			TH(uctx, "Comment");
			TH(uctx, "Action");

			for(int i = 0;i < comments.size();i++)
			{
				Entity comment = comments.get(i);
				TR_START(uctx);
					TD_START(uctx);SPAN(uctx,comment.getAttribute(CommentModule.COMMENT_FIELD_TITLE)+"<br/><br/>"+comment.getAttribute(CommentModule.COMMENT_FIELD_COMMENT),10);TD_END(uctx);
					TD_START(uctx);FORM_RADIO_GROUP(uctx, "comment_"+comment.getId(), ACTION_NAMES, ACTION_NAMES,ACTION_NAMES, ACTION_NAMES[1],12);TD_END(uctx);
				TR_END(uctx);
			}
			TR_START(uctx);
				TD_START(uctx);TD_END(uctx);
				TD_START(uctx);INSERT(uctx,"<span onclick='mark_all_for_delete()' style='cursor:pointer;font-size:12px;'>[ Mark All For Delete ]</span><br/><span onclick='mark_all_for_approve()' style='cursor:pointer;font-size:12px;'>[ Mark All For Approve ]</span>");TD_END(uctx);
			TR_END(uctx);
			TABLE_END(uctx);
			P(uctx);
			FORM_SUBMIT_BUTTON(uctx, "Submit");
			FORM_END(uctx);
			HR(uctx);
			A_GET(uctx, getName(),RAW_SUBMODE_DEFAULT , "[ unapprove all comments] ", "do_unapprove_all","true");
			BR(uctx);
			A_GET(uctx, getName(),RAW_SUBMODE_DEFAULT , "[ delete all comments] ", "do_delete_all","true");

			StringBuilder get_elements_by_class_js = new StringBuilder();
			get_elements_by_class_js.append("function getElementsByClass(clazz)\n");
			get_elements_by_class_js.append("{\n");
			get_elements_by_class_js.append("var itemsfound = new Array();\n");
			get_elements_by_class_js.append("var elements = document.getElementsByTagName('*');\n");
			get_elements_by_class_js.append("for(var i=0;i<elements.length;i++)\n");
			get_elements_by_class_js.append("{\n");
			get_elements_by_class_js.append("if(elements[i].className == clazz)\n");
			get_elements_by_class_js.append("{\n");
			get_elements_by_class_js.append("itemsfound.push(elements[i]);\n");
			get_elements_by_class_js.append("}\n");
			get_elements_by_class_js.append("}\n");
			get_elements_by_class_js.append("return itemsfound;\n");
			get_elements_by_class_js.append("}\n");


			INSERT(uctx,"<script>\n");
			INSERT(uctx,get_elements_by_class_js.toString());
			INSERT(uctx,"function mark_all_for_delete(){var deletees = getElementsByClass('Delete');for(var i=0;i < deletees.length;i++){deletees[i].checked=true;}}\n");
			INSERT(uctx,"function mark_all_for_approve(){var approvees = getElementsByClass('Approve');for(var i=0;i < approvees.length;i++){approvees[i].checked=true;}}\n");
			INSERT(uctx,"</script>\n");
			DOCUMENT_END(uctx);

		}catch(Exception e)
		{
			ERROR_PAGE(uctx,e);
		}
	}

	public void submode_view_bugs(UserApplicationContext uctx,Map<String,Object> params)
	{
		/*
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
		*/
	}





}
