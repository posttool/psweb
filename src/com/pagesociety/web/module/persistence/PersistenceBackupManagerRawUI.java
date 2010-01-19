package com.pagesociety.web.module.persistence;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import javax.servlet.http.HttpServletResponse;



import com.pagesociety.persistence.Entity;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.module.RawUIModule;
import com.pagesociety.web.module.permissions.PermissionEvaluator;



public class PersistenceBackupManagerRawUI extends RawUIModule 
{	
	private static final String SLOT_STORE = "store";
	private static final String PARAM_FULL_BACKUP_TIME 		  		= "full-backup-time";
	private static final String PARAM_INCREMENTAL_BACKUP_INTERVAL 	= "incremental-backup-interval";
	
	protected IPersistenceProvider pp;
	
	private File data_file;
	private Map<String,String> last_backup_map;
	private int backup_time_hr;
	private int backup_time_min;
	private int inc_backup_interval;

	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		pp = (IPersistenceProvider)getSlot(SLOT_STORE);
		try{
			init_last_backup_map();
		}catch(Exception e)
		{
			throw new InitializationException(e.getMessage());
		}
		String[] backup_time 	= GET_REQUIRED_LIST_PARAM(PARAM_FULL_BACKUP_TIME, config);
		backup_time_hr 			= Integer.parseInt(backup_time[0]);
		backup_time_min 		= Integer.parseInt(backup_time[1]);
		inc_backup_interval 	= Integer.parseInt(GET_REQUIRED_CONFIG_PARAM(PARAM_INCREMENTAL_BACKUP_INTERVAL,config));
		start_backup_thread();
	}

	private void init_last_backup_map() throws Exception
	{
		data_file = GET_MODULE_DATA_FILE(getApplication(), "last_backup_map", true);
		try{
			read_backup_map();
		}catch(Exception e)
		{
			last_backup_map = new HashMap<String, String>();
			write_backup_map();
		}
	}
	
	private void write_backup_map()throws Exception
	{
		FileOutputStream fos = new FileOutputStream(data_file);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(last_backup_map);
		oos.close();
	}
	
	private void read_backup_map() throws Exception
	{
		 ObjectInputStream ois = new ObjectInputStream(new FileInputStream(data_file));
		 Object o = ois.readObject();
		 last_backup_map = (HashMap<String,String>) o;
	}
	
	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_STORE,IPersistenceProvider.class,true);
	}


	protected void declareSubmodes(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		try{
			declareSubmode(RAW_SUBMODE_DEFAULT,   "submode_default");
		}catch(Exception e)
		{
			ERROR(e);
			throw new InitializationException("FAILED BINDING SUBMODE "+e.getMessage());
		}
	}
	
	public void submode_default(UserApplicationContext uctx,Map<String,Object> params) throws Exception
	{
		
		Entity user = (Entity)uctx.getUser();
		if(!PermissionEvaluator.IS_ADMIN(user))
		{
			CALL_WITH_INFO(uctx,"UserModuleRawUI",RAW_SUBMODE_DEFAULT,RAW_SUBMODE_DEFAULT,"Coupon promotion manager module requires admin login.");
			return;
		}
		if(params.get("do_full_backup") != null)
		{
			String id = pp.doFullBackup();
			last_backup_map.put(id, new Date().toString());
			write_backup_map();
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,pp.getName()+" BACKUP OK",18);
			P(uctx);
			JS_TIMED_REDIRECT(uctx, getName(),RAW_SUBMODE_DEFAULT,1000);
		}
		else if(params.get("do_delete") != null)
		{
			String id = (String)params.get("id");
			pp.deleteBackup(id);
			last_backup_map.remove(id);
			write_backup_map();
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,pp.getName()+" DELETED BACKUP OK",18);
			P(uctx);
			JS_TIMED_REDIRECT(uctx, getName(),RAW_SUBMODE_DEFAULT,1000);
		}
		else if(params.get("do_download") != null)
		{
			String id = (String)params.get("id");
			File zip = pp.getBackupAsZipFile(id);
			
			InputStream  is  = new FileInputStream(zip);
			HttpServletResponse response = (HttpServletResponse)GET_RAW_COMMUNIQUE(uctx).getResponse();
			OutputStream os  = response.getOutputStream();
		    response.setContentType("application/octet-stream");
		    response.setHeader("Content-Disposition", "attachment; filename=" + zip.getName()); 
		    response.setHeader("Content-Length",String.valueOf(zip.length())); 
		    response.setHeader("Pragma"," no-cache,public");
		   response.setHeader("Cache-Control", "max-age=0");
		    response.setStatus(response.SC_OK);
		    
		    byte[] buf = new byte[16484];
		    int l = 0;
		    int total_bytes = 0;
        	while((l = is.read(buf)) != -1)
        	{
        		os.write(buf, 0, l);
        		total_bytes+=l;
        	}
        	is.close();
        	os.close();
		}
		else if(params.get("do_restore") != null)
		{
			String id = (String)params.get("id");
			pp.restoreFromBackup(id);
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,pp.getName()+" RESTORED FROM "+id+" OK",18);
			P(uctx);
			JS_TIMED_REDIRECT(uctx, getName(),RAW_SUBMODE_DEFAULT,1000);
		}
		else
		{
			List<String> backup_identifiers = pp.getBackupIdentifiers();
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,pp.getName()+" BACKUP MANAGER",18);
			DISPLAY_ERROR(uctx,params);
			DISPLAY_INFO(uctx,params);
			P(uctx);
			TABLE_START(uctx, 1, 650);
			TR_START(uctx);
				TH(uctx, "Identifier");
				TH(uctx, "Last Backup");
				TH(uctx, "");
				TH(uctx, "");
				TH(uctx, "");
			TD_END(uctx);
		TR_END(uctx);
			for(int i = 0;i < backup_identifiers.size();i++ )
			{
				String identifier = backup_identifiers.get(i);
				String last_backup_date = last_backup_map.get(identifier);
				if(last_backup_date == null)
					last_backup_date = "";
				String short_identifier=null;
				if(identifier.lastIndexOf('\\') != -1)
					short_identifier = identifier.substring(identifier.lastIndexOf('\\')+1);
				if(identifier.lastIndexOf('/') != -1)
					short_identifier = identifier.substring(identifier.lastIndexOf('/')+1);
				TR_START(uctx);
					TD_START(uctx);
						SPAN(uctx, short_identifier);
					TD_END(uctx);
					TD_START(uctx);
						SPAN(uctx, last_backup_date);
					TD_END(uctx);
					TD_START(uctx);
						A_GET(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ download ]","do_download",true,"id",identifier);
					TD_END(uctx);
					TD_START(uctx);
						A_GET(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ restore ]","do_restore",true,"id",identifier);
					TD_END(uctx);
					TD_START(uctx);
						A_GET(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ delete ]","do_delete",true,"id",identifier);
					TD_END(uctx);
				TR_END(uctx);
			}
			TABLE_END(uctx);
			P(uctx);
			A(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ + DO FULL BACKUP ]","do_full_backup",true);

			DOCUMENT_END(uctx);
		}	
	
	}
	
	private boolean backup_thread_is_running = false;
	private boolean full_backup_for_day_is_done = false;
	private void start_backup_thread()
	{
		Thread t = new Thread()
		{
			public void run()
			{
				while(true)
				{
					
					
					backup_thread_is_running = true;
					Calendar now 	= Calendar.getInstance();
					int now_hr  	= now.get(Calendar.HOUR_OF_DAY);
					int now_min 	= now.get(Calendar.MINUTE);
					int year 		= now.get(Calendar.YEAR);
					int day_of_year = now.get(Calendar.DAY_OF_YEAR);
					String backup_for_day_key = String.valueOf(year)+"_"+String.valueOf(day_of_year);
					full_backup_for_day_is_done = last_backup_map.get(backup_for_day_key)!=null;
					
					
					if(now_hr >= backup_time_hr && now_min >= backup_time_min && !full_backup_for_day_is_done)
					{
						try{
							String id = pp.doFullBackup();
							full_backup_for_day_is_done = true;
							last_backup_map.put(id,new Date().toString());
							last_backup_map.put(backup_for_day_key,"true");
							write_backup_map();
						}catch(Exception pe)
						{
							pe.printStackTrace();
						}
					}
					

					backup_thread_is_running = false;
					try{
						Thread.sleep(60000);
					}catch(InterruptedException ie)
					{
						ie.printStackTrace();
						//not much to do.
					}
				}	
				
		
			}
		};
		t.setDaemon(true);
		t.start();
		
	}
	
	
}
