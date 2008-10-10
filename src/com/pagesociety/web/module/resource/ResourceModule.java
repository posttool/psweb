package com.pagesociety.web.module.resource;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;


import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Types;
import com.pagesociety.util.FileInfo;
import com.pagesociety.util.Text;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.upload.MultipartForm;
import com.pagesociety.web.upload.MultipartFormException;
import com.pagesociety.web.upload.UploadProgressInfo;

public class ResourceModule extends WebStoreModule 
{

	private static final String PARAM_UPLOAD_TEMP_DIR		 = "upload-temp-dir";
	private static final String PARAM_UPLOAD_MAX_FILE_SIZE	 = "upload-max-file-size";	
	private static final String KEY_PENDING_UPLOAD_EXCEPTION = "_resource_pending_upload_expection";

	private static final String KEY_CURRENT_UPLOAD_MAP   = "_current_upload_map_";
	private static final String FORM_ELEMENT_CHANNEL     = "channel";
	private static final String FORM_ELEMENT_RESOURCE_ID = "resource_id";

	private String SLOT_PATH_PROVIDER = "resource-path-provider";
	private String SLOT_GUARD		  = "resource-guard";
	
	private File				 	upload_temp_dir;
	private long				 	upload_max_file_size;
	private IResourcePathProvider 	path_provider;
	private IResourceGuard		 	guard;

	private static final List<UploadProgressInfo> EMPTY_UPLOAD_PROGRESS_LIST = new ArrayList<UploadProgressInfo>(0);

	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		path_provider = (IResourcePathProvider)getSlot(SLOT_PATH_PROVIDER);
		guard		  = (IResourceGuard)getSlot(SLOT_GUARD);
		set_parameters(config);
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_PATH_PROVIDER,IResourcePathProvider.class,true);
		DEFINE_SLOT(SLOT_GUARD,IResourceGuard.class,false,DefaultResourceGuard.class);
	}	
	
	private void set_parameters(Map<String,Object> config) throws InitializationException
	{

		String temp_dir = (String)config.get(PARAM_UPLOAD_TEMP_DIR);
		if(temp_dir == null)
			upload_temp_dir 	= new File(System.getProperty("java.io.tmpdir"));
		else
		{
			try{
				upload_temp_dir = new File(temp_dir);
				upload_temp_dir.mkdirs();
			}catch(Exception e)
			{
				throw new InitializationException("RESOURCE MODULE UNABLE TO INIT UPLOAD TEMP DIR "+upload_temp_dir);
			}
		}
		
		String s_upload_max_file_size = (String)config.get(PARAM_UPLOAD_MAX_FILE_SIZE);
		if(s_upload_max_file_size == null)
		{
			upload_max_file_size = 0;//0 means unlimited upload size
		}
		else
		{
			try{
				upload_max_file_size = Long.parseLong(s_upload_max_file_size);
			}catch(Exception e)
			{
				throw new InitializationException(PARAM_UPLOAD_MAX_FILE_SIZE+" MUST BE SPECIFIED AS AN INTEGER. NUMBER OF BYTES.");
			}
		}
	}

	/////////////////BEGIN  M O D U L E   F U N C T I O N S/////////////////////////////////////////


	
	@Export
	public boolean CreateResource(UserApplicationContext uctx,MultipartForm upload) throws WebApplicationException,PersistenceException
	{	
		Entity user = (Entity)uctx.getUser();
		try{
			GUARD(guard.canCreateResource(user));
		}catch(Exception e)
		{
			uctx.setProperty(KEY_PENDING_UPLOAD_EXCEPTION, e);
			return false;
		}
		return do_upload(uctx, upload, false, null);
	}
	

	@Export
	public boolean UpdateResource(UserApplicationContext uctx,MultipartForm upload) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();

		Entity update_resource;
		long resource_id = upload.getLongParameter(FORM_ELEMENT_RESOURCE_ID);
		if(resource_id == -1)
		{
			Exception e = new WebApplicationException("UPDATING RESOURCE FILE REQUIRES A PARAMETER NAMED 'resource_id' IN THE QUERY STRING OF THE POST URL.");
			e.printStackTrace();
			uctx.setProperty(KEY_PENDING_UPLOAD_EXCEPTION, e);
			return false;	
		}
		update_resource = GET(RESOURCE_ENTITY,resource_id);
		
		try{
			GUARD(guard.canUpdateResource(user,update_resource));
		}catch(Exception e)
		{
			uctx.setProperty(KEY_PENDING_UPLOAD_EXCEPTION, e);
			return false;
		}
		return do_upload(uctx, upload, true, update_resource);
	}
	
	@Export
	public String GetResourceURL(UserApplicationContext uctx,long resource_id) throws WebApplicationException,PersistenceException
	{	
		//check to make sure it exists//
		Entity user = (Entity)uctx.getUser();
		Entity resource = GET(RESOURCE_ENTITY,resource_id);
		GUARD(guard.canGetResourceURL(user,resource));
		return getResourceURL(resource);
	}
	
	
	public String getResourceURL(Entity resource) throws WebApplicationException
	{
		String path_token = (String)resource.getAttribute(RESOURCE_FIELD_PATH_TOKEN);
		if(path_token == null)
			throw new WebApplicationException("THE RESOURCE EXISTS BUT HAS NO PATH TOKEN.");
		return path_provider.getUrl(path_token);		
	}
	
	@Export
	public List<String> GetResourceURLs(UserApplicationContext uctx,List<Integer> resource_ids) throws WebApplicationException,PersistenceException
	{	
		//check to make sure it exists//
		Entity user = (Entity)uctx.getUser();
		List<Entity> resources = INT_IDS_TO_ENTITIES(RESOURCE_ENTITY,resource_ids);
		int s = resources.size();
		List<String> urls = new ArrayList<String>(s);
		//move the file to the right place
		for (int i=0; i<s; i++)
		{
			Entity resource = resources.get(i);
			GUARD(guard.canGetResourceURL(user,resource));
			urls.add( getResourceURL(resource));
		}
		return urls;
	}

	
	@Export
	public String GetResourceURLWithDim(UserApplicationContext uctx,long resource_id,int w, int h) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity resource = GET(RESOURCE_ENTITY,resource_id);
		GUARD(guard.canGetResourceURL(user,resource));
		return getResourceUrlWithDim( resource, w, h);
	
	}
	
	public String getResourceUrlWithDim(Entity resource,int w,int h) throws WebApplicationException
	{
		String path_token = (String)resource.getAttribute(RESOURCE_FIELD_PATH_TOKEN);
		if(path_token == null)
			throw new WebApplicationException("THE RESOURCE EXISTS BUT HAS NO PATH TOKEN.");
		return path_provider.getPreviewUrl(path_token,w,h);		
	}


	@Export
	public List<String> GetResourceURLsWithDim(UserApplicationContext uctx,List<Integer> resource_ids,int w, int h) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		//check to make sure it exists//
		List<Entity> resources = INT_IDS_TO_ENTITIES(RESOURCE_ENTITY,resource_ids);
		int s = resources.size();
		List<String> urls = new ArrayList<String>(s);
		//move the file to the right place
		for (int i=0; i<s; i++)
		{
			Entity resource = resources.get(i);
			GUARD(guard.canGetResourceURL(user, resource));
			urls.add( getResourceUrlWithDim(resource, w, h));
		}
		return urls;
	}
	
	@Export
	public List<UploadProgressInfo> GetUploadProgress(UserApplicationContext uctx,String channel_name) throws PersistenceException,WebApplicationException
	{
		System.out.println("GET PROGRESS SESSION ID IS "+uctx.getId());
		check_exceptions(uctx);
		Entity user = (Entity)uctx.getUser();
		GUARD(guard.canGetUploadProgress(user,channel_name));
		
		Map<String,MultipartForm> channel_upload_map = (Map<String,MultipartForm>)uctx.getProperty(KEY_CURRENT_UPLOAD_MAP);
		if(channel_upload_map == null)
			return EMPTY_UPLOAD_PROGRESS_LIST;
		
		MultipartForm channel_upload = channel_upload_map.get(channel_name);
		if(channel_upload == null)
			return EMPTY_UPLOAD_PROGRESS_LIST;
		
		List<UploadProgressInfo> ret = channel_upload.getUploadProgress();
		if(channel_upload.isComplete())
		{
			System.out.println("UPLOAD IS COMPLETE FOR SESSION"+uctx.getId());
			channel_upload_map.remove(channel_name);
		}
		return ret;
	}
	
	@Export
	public List<UploadProgressInfo> CancelUpload(UserApplicationContext ctx,String channel_name)throws WebApplicationException
	{
		check_exceptions(ctx);
		Entity user = (Entity)ctx.getUser();
		GUARD(guard.canGetUploadProgress(user,channel_name));
		
		Map<String,MultipartForm> channel_upload_map = (Map<String,MultipartForm>)ctx.getProperty(KEY_CURRENT_UPLOAD_MAP);
		if(channel_upload_map == null)
			return EMPTY_UPLOAD_PROGRESS_LIST;
		
		MultipartForm channel_upload = channel_upload_map.get(channel_name);
		if(channel_upload == null)
			return EMPTY_UPLOAD_PROGRESS_LIST;
	
		List<UploadProgressInfo> ret = channel_upload.getUploadProgress();
		try{
			channel_upload.cancel();
		}catch(IOException e)
		{
			throw new WebApplicationException("COULDN'T CANCEL");
		}
		channel_upload_map.remove(KEY_CURRENT_UPLOAD_MAP);
		return ret;
	}
	
	
	
	@Export
	public Entity GetResource(UserApplicationContext uctx,long resource_id) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity resource = GET(RESOURCE_ENTITY,resource_id);	 
		GUARD(guard.canGetResource(user,resource));
		return resource;
	}
	
	public Entity getResource(long resource_id) throws WebApplicationException,PersistenceException
	{
		return GET(RESOURCE_ENTITY,resource_id);	 
	}
	
	///////////////////////////////////END MODULE FUNCTIONS *//
	private boolean do_upload(UserApplicationContext uctx,MultipartForm upload,boolean update,Entity update_resource) throws WebApplicationException,PersistenceException
	{
		System.out.println("INITIATING UPLOAD FOR SESSION"+uctx.getId());
		Entity user = (Entity)uctx.getUser();		
		String channel_name = upload.getParameter(FORM_ELEMENT_CHANNEL);
		if(channel_name == null)
		{
			Exception e = new WebApplicationException("UPLOAD MUST SPECIFY CHANNEL NAME AS A PARAMETER NAMED 'channel' IN THE QUERY STRING OF THE POST URL.");
			e.printStackTrace();
			uctx.setProperty(KEY_PENDING_UPLOAD_EXCEPTION, e);
			return false;
		}
		
		Map<String,MultipartForm> current_upload_map = (Map<String,MultipartForm>)uctx.getProperty(KEY_CURRENT_UPLOAD_MAP);
		if(current_upload_map == null)
		{
			current_upload_map = new HashMap<String,MultipartForm>();
			uctx.setProperty(KEY_CURRENT_UPLOAD_MAP,current_upload_map);
		}
		
		MultipartForm current_upload = current_upload_map.get(channel_name);
		if(current_upload != null && !current_upload.isComplete())
		{
		
			Exception e = new WebApplicationException("CURRENT UPLOAD IN CHANNEL "+channel_name+" IS NOT FINISHED. CANCEL EXISITING UPLOAD FIRST.");
			e.printStackTrace();
			uctx.setProperty(KEY_PENDING_UPLOAD_EXCEPTION, e);
			return false;
		}

		upload.setUploadDirectory(upload_temp_dir);
		upload.setMaxUploadItemSize(upload_max_file_size);
		
		current_upload = upload;
		current_upload_map.put(channel_name, current_upload);
		
		try{
			current_upload.parse();
		}catch(MultipartFormException e)
		{
			if(current_upload.isCancelled())
				return false;
			
			e.printStackTrace();
			current_upload_map.remove(channel_name);
			
			Exception ee = new WebApplicationException("PROBLEM PARSING FORM ON CHANNEL "+channel_name+" "+e.getMessage(),e);
			uctx.setProperty(KEY_PENDING_UPLOAD_EXCEPTION, ee);
			return false;
		}
		
		List<File> 	uploaded_files 					 = current_upload.getFiles();
		List<UploadProgressInfo> uploaded_files_info = current_upload.getUploadProgress();
		int s = uploaded_files.size();
		for(int i = 0;i < s;i++)
		{
			File uploaded_file 			 = uploaded_files.get(i);
			UploadProgressInfo file_info = uploaded_files_info.get(i); 
			String content_type	 		 = file_info.getContentType();
			String simple_type	 		 = FileInfo.getSimpleTypeAsString(uploaded_file);
			long file_size				 = file_info.getFileSize();
			String file_name 			 = Text.makeUrlSafe(file_info.getFileName());	
			String ext 					 = FileInfo.getExtension(file_name);		
			
			if(ext != null && ext.toLowerCase().equals("zip"))
			{
				if(update)
				{
					Exception ee = new WebApplicationException("YOU CANT UPDATE A RESOURCE FILE WITH A ZIP. IT NEEDS TO BE A SINGLE FILE.SINCE A RESOURCE HOLDS ONE FILE."); 
					ee.printStackTrace();
					uctx.setProperty(KEY_PENDING_UPLOAD_EXCEPTION, ee);
					return false;
				}
				
				List<Entity> resources;
				//zip insert will set filename and title
				try{
					resources = add_resources_from_zip(user,uploaded_file);
				}catch(Exception e)
				{
					e.printStackTrace();
					uctx.setProperty(KEY_PENDING_UPLOAD_EXCEPTION, e);
					return false;
				}
				file_info.setCompletionObject(resources);
				System.out.println("ADDED RESOURCES "+resources);
			}
			else
			{
				Entity resource = null;
				String path_token;
				try{
					path_token = path_provider.save(uploaded_file);
					/* we don't delete the file. it is up to the path provider to 
					 * make this descion as it may just want to rename it
					 */
				}catch(Exception e)
				{
					uploaded_file.delete();
					Exception ee = new WebApplicationException("PATH PROVIDER FAILED WRITING FILE",e);
					e.printStackTrace();
					uctx.setProperty(KEY_PENDING_UPLOAD_EXCEPTION, ee);
					return false;
				}
				
				try{
					if(update)
					{
						do_update_resource(update_resource, content_type, simple_type, file_name, ext, file_size, path_token);
						file_info.setCompletionObject(update_resource);
					}
					else
					{
						resource = do_add_resource(user,content_type,simple_type,file_name,ext,file_size,path_token);					  
						file_info.setCompletionObject(resource);
					}
				}catch(PersistenceException pe)
				{
					/* if we cant create a resource we need to delete the upload */
					try{
						path_provider.delete(path_token);
					}catch(Exception e){e.printStackTrace();}//swallow this one
					
					pe.printStackTrace();
					Exception ee = new WebApplicationException("COULDNT CREATE RESOURCE FOR FILE UPLOAD",pe);
					uctx.setProperty(KEY_PENDING_UPLOAD_EXCEPTION, ee);
					return false;
				}
				
				if(update)
					System.out.println("UPDATED RESOURCE "+update_resource);
				else
					System.out.println("ADDED RESOURCE "+resource);
			}
		}
		return true;	
	}
	
	private List<Entity> add_resources_from_zip(Entity creator,File zip) throws WebApplicationException,PersistenceException
	{
		ZipFile zipFile = null;
		try {
			Enumeration entries = null;
			zipFile = new ZipFile(zip);
			entries = zipFile.entries();
			List<Entity> resources = new ArrayList<Entity>();
			while(entries.hasMoreElements()) 
			{
				ZipEntry entry = (ZipEntry)entries.nextElement();
				if(entry.isDirectory()) {
					// Assume directories are stored parents first then children.
					System.err.println("Ignoring directory: " + entry.getName());
					// This is not robust, just for demonstration purposes.
					//  (new File(entry.getName())).mkdir();
					continue;
	        }

	        System.err.println("Extracting file: " + entry.getName());
		    String filename = Text.makeUrlSafe(entry.getName());
	        if(filename.startsWith(".") || filename.startsWith("__macosx"))
	        	continue;
	        
	        String ext = FileInfo.getExtension(filename);
	        File uncompressed_entry = new File (upload_temp_dir,filename);
	        copy_input_stream(zipFile.getInputStream(entry),
	           new BufferedOutputStream(new FileOutputStream(uncompressed_entry)));
	       
	        String content_type = "????";
	        String simple_type = FileInfo.getSimpleTypeAsString(uncompressed_entry);
	        long file_size 		= uncompressed_entry.length();
	        String path_token 	= path_provider.save(uncompressed_entry);
	        Entity resource 	= do_add_resource(creator, content_type,simple_type, filename, ext, file_size, path_token);
	        resources.add(resource);
	      }

	      zipFile.close();
	      zip.delete();
	      return resources;
		} catch (ZipException e) {
			e.printStackTrace();
			try{
			  zipFile.close();
		      zip.delete();
			}catch(Exception ee)
			{
				ee.printStackTrace();
			}
			throw new WebApplicationException("PROBLEM WITH ZIP. ZIP EXCEPTION. NO RESOURCES WERE CREATED");
			
		} catch (IOException e) {
			 e.printStackTrace();
			try{  
				zipFile.close();
				zip.delete();
		    }catch(Exception ee)
		    {
		    	ee.printStackTrace();
		    }
		     
			throw new WebApplicationException("PROBLEM WITH ZIP. IO EXCEPTION. NO RESOURCES WERE CREATED");
			
		}	
	}

	private Entity do_add_resource(Entity creator,String content_type,String simple_type,String filename,String ext,long file_size,String path_token) throws PersistenceException
	{
		Entity resource = NEW(RESOURCE_ENTITY,
				creator,
				RESOURCE_FIELD_CONTENT_TYPE,content_type,
				RESOURCE_FIELD_SIMPLE_TYPE,simple_type,
				RESOURCE_FIELD_FILENAME,filename,
				RESOURCE_FIELD_EXTENSION,ext,
				RESOURCE_FIELD_FILE_SIZE,file_size,
				RESOURCE_FIELD_PATH_TOKEN,path_token);
			
		return resource;
	}
	
	private Entity do_update_resource(Entity resource,String content_type,String simple_type,String filename,String ext,long file_size,String path_token) throws WebApplicationException,PersistenceException
	{
		path_provider.delete((String)resource.getAttribute(RESOURCE_FIELD_PATH_TOKEN));
		Entity r = UPDATE(resource,
				RESOURCE_FIELD_CONTENT_TYPE,content_type,
				RESOURCE_FIELD_SIMPLE_TYPE,simple_type,
				RESOURCE_FIELD_FILENAME,filename,
				RESOURCE_FIELD_EXTENSION,ext,
				RESOURCE_FIELD_FILE_SIZE,file_size,
				RESOURCE_FIELD_PATH_TOKEN,path_token);
			
		return r;
	}
	
	private static final void copy_input_stream(InputStream in, OutputStream out) throws IOException
	{
	    byte[] buffer = new byte[1024];
	    int len;
	
	    while((len = in.read(buffer)) >= 0)
	      out.write(buffer, 0, len);
	
	    in.close();
	    out.close();
	}

	public static void check_exceptions(UserApplicationContext uctx) throws WebApplicationException
	{
		WebApplicationException e = (WebApplicationException)uctx.getProperty(KEY_PENDING_UPLOAD_EXCEPTION);
		if(e != null)
		{
			uctx.setProperty(KEY_PENDING_UPLOAD_EXCEPTION, null);
			throw e;
		}
	}
	
	/////////////////E N D  M O D U L E   F U N C T I O N S/////////////////////////////////////////

	
	//BEGIN UTIL FUNTIONS
	//END UTIL FUNCTIONS //
	
	//ENTITY BOOTSTRAP STUFF //
	public static String RESOURCE_ENTITY 				= 	"Resource";
	public static String RESOURCE_FIELD_CONTENT_TYPE 	= 	"content-type";
	public static String RESOURCE_FIELD_SIMPLE_TYPE 	= 	"simple-type";
	public static String RESOURCE_FIELD_FILENAME 		=   "filename";
	public static String RESOURCE_FIELD_EXTENSION 		=   "extension";
	public static String RESOURCE_FIELD_FILE_SIZE 		=   "filesize";
	public static String RESOURCE_FIELD_PATH_TOKEN 		=   "path-token";


	protected void defineEntities(Map<String,Object> config) throws PersistenceException,SyncException
	{

		DEFINE_ENTITY(RESOURCE_ENTITY,
					  RESOURCE_FIELD_CONTENT_TYPE,	Types.TYPE_STRING,null, 
					  RESOURCE_FIELD_SIMPLE_TYPE,	Types.TYPE_STRING,null, 
					  RESOURCE_FIELD_FILENAME,		Types.TYPE_STRING,null,
					  RESOURCE_FIELD_EXTENSION, 	Types.TYPE_STRING,null,
					  RESOURCE_FIELD_FILE_SIZE,		Types.TYPE_LONG,null,
					  RESOURCE_FIELD_PATH_TOKEN,	Types.TYPE_STRING,null);
	}

	public static String INDEX_BY_EMAIL				 	= 	"byEmail";
	public static String INDEX_BY_LOCK					=   "byLock";
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,SyncException
	{
		 //DEFINE_ENTITY_INDEX(USER_ENTITY,INDEX_BY_EMAIL, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, FIELD_EMAIL);
		 //DEFINE_ENTITY_INDEX(USER_ENTITY,INDEX_BY_LOCK, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, FIELD_LOCK);
	}


}
