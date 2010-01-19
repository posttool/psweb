package com.pagesociety.web.module.qa;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.util.RandomGUID;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.module.WebStoreModule;



public class BugReporterModule extends WebStoreModule
{
	//this is where images will be uploaded to and served from//

	private File 	bug_image_directory_f 	= null;

	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);
		bug_image_directory_f = new File(GET_MODULE_DATA_DIRECTORY(app),"bug-screenshots");
		if(!bug_image_directory_f.exists())
			bug_image_directory_f.mkdirs();
	}	

	protected void defineSlots()
	{
		super.defineSlots();
		//DEFINE_SLOT(SLOT_EMAIL_MODULE,IEmailModule.class,true);
	}

	
	/////////////////BEGIN  M O D U L E   F U N C T I O N S/////////////////////////////////////////
	public Entity createBug(Entity creator,String submitter,String description,File screenshot) throws PersistenceException
	{
			try{
				START_TRANSACTION();
				String ext 		= "";
				String filename = null;
				if(screenshot != null)
				{
					filename = screenshot.getName();
					int dot_idx = filename.indexOf('.');
					if( dot_idx != -1)
						ext = filename.substring(dot_idx).toLowerCase();

					filename = RandomGUID.getGUID()+ext;
					COPY(screenshot,bug_image_directory_f,filename);
				}
				
				Entity bug = NEW(PS_BUG_ENTITY,
								creator,
								PS_BUG_FIELD_SUBMITTER,submitter,
								PS_BUG_FIELD_DESCRIPTION,description,
								PS_BUG_FIELD_SCREENSHOT,filename);
					
				COMMIT_TRANSACTION();
				return bug;  
				
			}catch(PersistenceException e)
			{
				ROLLBACK_TRANSACTION();
				throw e;
			}
	}
	
	public Entity createAnnotation(long bug_id,String annotation) throws PersistenceException
	{
		
		Entity bug = getBugById(bug_id);
		List<String> annotations = (List<String>)bug.getAttribute(PS_BUG_FIELD_ANNOTATIONS);
		if(annotations == null)
			annotations = new ArrayList<String>();
		annotations.add("("+(new Date().toString())+") "+annotation);
		return UPDATE(bug,
					  PS_BUG_FIELD_ANNOTATIONS,annotations);
		
	}
	
	
	public List<Entity> getAllBugs() throws PersistenceException
	{
		Query q = new Query(PS_BUG_ENTITY);
		q.idx(Query.PRIMARY_IDX);
		q.eq(Query.VAL_GLOB);
		q.orderBy(FIELD_LAST_MODIFIED,Query.DESC);
		QueryResult results = QUERY(q);
		return results.getEntities();
	}
	
	public Entity getBugById(long id) throws PersistenceException
	{
		return GET(PS_BUG_ENTITY,id);
	}
	
	public void deleteBug(long id) throws PersistenceException
	{
		Entity bug = GET(PS_BUG_ENTITY,id);
		String screenshot = (String)bug.getAttribute(PS_BUG_FIELD_SCREENSHOT);
		if(screenshot != null)
		{
			File ss = new File(bug_image_directory_f,screenshot);
			ss.delete();
		}
		DELETE(bug);
	}
	
	public File getBugScreenshot(long id) throws PersistenceException
	{
		Entity bug = GET(PS_BUG_ENTITY,id);;	
		String ss  = (String)bug.getAttribute(PS_BUG_FIELD_SCREENSHOT);
		if(ss == null)
			return null;
		return new File(bug_image_directory_f,ss);
	}
	
	
	
	/////////////////E N D  M O D U L E   F U N C T I O N S/////////////////////////////////////////
		
	public static String PS_BUG_ENTITY 	  		   = "PSBug";
	public static String PS_BUG_FIELD_SUBMITTER    = "title";
	public static String PS_BUG_FIELD_DESCRIPTION  = "description";
	public static String PS_BUG_FIELD_SCREENSHOT   = "screenshot_filename";
	public static String PS_BUG_FIELD_ANNOTATIONS   = "annotations";


	protected void defineEntities(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY(PS_BUG_ENTITY,
				PS_BUG_FIELD_SUBMITTER,Types.TYPE_STRING,null,
				PS_BUG_FIELD_DESCRIPTION,Types.TYPE_STRING,null,
				PS_BUG_FIELD_SCREENSHOT,Types.TYPE_STRING,null,
				PS_BUG_FIELD_ANNOTATIONS,Types.TYPE_STRING | Types.TYPE_ARRAY,new ArrayList<String>()
				);
	
	}

	//public static final String IDX_BY_USER_BY_PREFERRED = "byUserByPreferred";
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		//DEFINE_ENTITY_INDICES
		//(
		//		BILLINGRECORD_ENTITY,
		//		ENTITY_INDEX(IDX_BY_USER_BY_PREFERRED , EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, FIELD_CREATOR,BILLINGRECORD_FIELD_PREFERRED)
		//);
	}
}
