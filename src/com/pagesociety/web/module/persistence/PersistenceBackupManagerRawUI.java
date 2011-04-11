package com.pagesociety.web.module.persistence;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


import javax.servlet.http.HttpServletResponse;



import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.util.Text;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.RawUIModule;
import com.pagesociety.web.module.permissions.PermissionEvaluator;



public class PersistenceBackupManagerRawUI extends RawUIModule 
{	
	private static final String SLOT_STORE = "store";
	private static final String PARAM_FULL_BACKUP_TIME 		  		= "full-backup-time";
	private static final String PARAM_MAX_DAYS		     	  		= "max-days-to-keep-backup";
	private static final String PARAM_INCREMENTAL_BACKUP_INTERVAL 	= "incremental-backup-interval";
	
	protected IPersistenceProvider pp;
	
	private File data_file;
	private SortedMap<String,BackupInfo> last_backup_map;
	private int backup_time_hr;
	private int backup_time_min;
	private int inc_backup_interval;//TODO
	private int max_days;

	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		pp = (IPersistenceProvider)getSlot(SLOT_STORE);
		try
		{
			init_last_backup_map();
		} catch (Exception e)
		{
			throw new InitializationException("Couldnt read init backup map",e);
		}
		
		max_days = GET_OPTIONAL_INT_CONFIG_PARAM(PARAM_MAX_DAYS, 14, config);
		try
		{
			remove_old_backups();
		} catch (Exception e)
		{
			throw new InitializationException("Couldnt remove old backups",e);
		}
		
		String[] backup_time	= GET_OPTIONAL_LIST_PARAM(PARAM_FULL_BACKUP_TIME, config);
		if (backup_time != null)
		{
			backup_time_hr	= Integer.parseInt(backup_time[0]);
			backup_time_min	= Integer.parseInt(backup_time[1]);
			String incremental_backup_interval = GET_REQUIRED_CONFIG_PARAM(PARAM_INCREMENTAL_BACKUP_INTERVAL,config);
			if (incremental_backup_interval != null)
				inc_backup_interval	= Integer.parseInt(incremental_backup_interval);
			start_backup_thread();
		}
	}

	private void init_last_backup_map() throws WebApplicationException, IOException, PersistenceException 
	{
		data_file = GET_MODULE_DATA_FILE(getApplication(), "last_backup_map", true);
		try{
			read_backup_map();
		}catch(Exception e)
		{
			last_backup_map = new TreeMap<String, BackupInfo>();
			write_backup_map();
		}
		List<String> on_disk_ids = pp.getBackupIdentifiers();
		Map<String,BackupInfo> add = new HashMap<String,BackupInfo>();
		INFO("ON DISK");
		for (String s : on_disk_ids)
		{
			BackupInfo b = new BackupInfo(s);
			boolean in_map = last_backup_map.containsKey(b.id());
			INFO(s+" in map="+in_map);
			if (!in_map)
				add.put(b.id(), b);
		}
		INFO("IN MAP");
		List<String> remove = new ArrayList<String>();
		for (String k : last_backup_map.keySet())
		{
			if (last_backup_map.get(k) instanceof BackupInfo)
			{
				BackupInfo b = last_backup_map.get(k);
				boolean on_disk = on_disk_ids.contains(b.path);
				INFO(k+" on disk="+on_disk);
				if (!on_disk)
					remove.add(k);
			}
			else
			{
				remove.add(k);
				INFO("?old map value?"+k+"="+last_backup_map.get(k));
			}
		}
		if (!remove.isEmpty() || !add.isEmpty())
		{
			for (String k : remove)
				last_backup_map.remove(k);
			for (String k : add.keySet())
				last_backup_map.put(add.get(k).id(),add.get(k));
			write_backup_map();
		}
		INFO("BACKUP MAP INIT COMPLETE");
	}
	
	private void remove_old_backups() throws PersistenceException, IOException
	{
		int c = 0;
		int s = last_backup_map.size() - max_days ;
		if (s<1)
			return;
		List<String> delete_list = new ArrayList<String>();
		for (String k : last_backup_map.keySet())
		{
			if (c < s)
				delete_list.add(k);
			c++;
		}
		for (String k : delete_list)
			delete_backup_by_id(k);
	}
	
	private void delete_backup_by_id(String id) throws PersistenceException, IOException
	{
		INFO("DELETING BACKUP "+id);
		
		pp.deleteBackup(last_backup_map.get(id).path);
		last_backup_map.remove(id);
		write_backup_map();
		
	}
	
	private void delete_backup_by_path(String path) throws PersistenceException, IOException
	{
		BackupInfo b = new BackupInfo(path);
		INFO("DELETING BACKUP "+b.id());
		
		pp.deleteBackup(path);
		last_backup_map.remove(b.id());
		write_backup_map();
		
	}

	private void write_backup_map() throws IOException
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
		 last_backup_map = (TreeMap<String,BackupInfo>) o;
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
		try{
		Entity user = (Entity)uctx.getUser();
		if(!PermissionEvaluator.IS_ADMIN(user))
		{
			CALL_WITH_INFO(uctx,"UserModuleRawUI",RAW_SUBMODE_DEFAULT,RAW_SUBMODE_DEFAULT,"Coupon promotion manager module requires admin login.");
			return;
		}
		if(params.get("do_full_backup") != null)
		{
			String id = pp.doFullBackup();
			BackupInfo b = new BackupInfo(id);
			last_backup_map.put(b.id(), b);
			write_backup_map();
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,pp.getName()+" BACKUP OK",18);
			P(uctx);
			JS_TIMED_REDIRECT(uctx, getName(),RAW_SUBMODE_DEFAULT,1000);
		}
		else if (params.get("clean_log_files") != null)
		{
			BDBPersistenceModule bdbpp = (BDBPersistenceModule) pp;
			int howmany = bdbpp.removeUnusedLogFiles();
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,pp.getName()+" REMOVED "+howmany+" UNUSED LOG FILES",18);
			P(uctx);
			JS_TIMED_REDIRECT(uctx, getName(),RAW_SUBMODE_DEFAULT,1000);
		}
		else if(params.get("do_delete") != null)
		{
			String path = (String)params.get("id");
			delete_backup_by_path(path);
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
			Collections.sort(backup_identifiers);
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
				BackupInfo b = new BackupInfo(backup_identifiers.get(i));
				TR_START(uctx);
					TD_START(uctx);
						SPAN(uctx, b.id());
					TD_END(uctx);
					TD_START(uctx);
						SPAN(uctx, b.dateString());
					TD_END(uctx);
					TD_START(uctx);
						A_GET(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ download ]","do_download",true,"id",b.path);
					TD_END(uctx);
					TD_START(uctx);
						A_GET(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ restore ]","do_restore",true,"id",b.path);
					TD_END(uctx);
					TD_START(uctx);
						A_GET(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ delete ]","do_delete",true,"id",b.path);
					TD_END(uctx);
				TR_END(uctx);
			}
			TABLE_END(uctx);
			P(uctx);
			A(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ + DO FULL BACKUP ]","do_full_backup",true);
			if (pp instanceof BDBPersistenceModule)
				A(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ - CLEAN UNUSED LOG FILES ]","clean_log_files",true);

			DOCUMENT_END(uctx);
		}	
		}catch(Exception e)
		{
			e.printStackTrace();
			ERROR_PAGE(uctx, e);
		}
	}
	
	private boolean backup_thread_is_running = false;
	private boolean full_backup_for_day_is_done = false;

	private void start_backup_thread()
	{
		Thread t = new Thread("PersistenceBackupManager")
		{
			public void run()
			{
				while (true)
				{

					backup_thread_is_running = true;
					BackupInfo b = new BackupInfo();

					int now_hr = b.date.get(Calendar.HOUR_OF_DAY);
					int now_min = b.date.get(Calendar.MINUTE);
					full_backup_for_day_is_done = last_backup_map.get(b.id()) != null;

					if (now_hr >= backup_time_hr && now_min >= backup_time_min && !full_backup_for_day_is_done)
					{
						try
						{
							String id = pp.doFullBackup();
							full_backup_for_day_is_done = true;
							b = new BackupInfo(id);
							last_backup_map.put(b.id(), b);
							write_backup_map();
						} catch (Exception pe)
						{
							pe.printStackTrace();
						}
						try
						{
							remove_old_backups();
						} catch (Exception e)
						{
							ERROR("Couldnt remove old backup", e);
						}
					}

					backup_thread_is_running = false;
					try
					{
						Thread.sleep(60000);
					} catch (InterruptedException ie)
					{
						ie.printStackTrace();
						// not much to do.
					}
				}

			}
		};
		t.setDaemon(true);
		t.start();

	}
	
	class BackupInfo implements Serializable {

		private static final long serialVersionUID = -5810397801922315766L;
		public Calendar date;
		public String path;
		public BackupInfo()
		{
			this.date = Calendar.getInstance();
		}
		
		public BackupInfo(String path)
		{
			this.path = path;
			this.date = Calendar.getInstance();
			get_date_from_path();
		}
		
		private void get_date_from_path()
		{
			//String[] parts = path.split(File.separator);
			File f = new File(path);
			//String dir_name = parts[parts.length-1];
			String dir_name = f.getName();
			
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss"); // from bdbstore dobackup
		    Date d;
			try
			{
				d = formatter.parse(dir_name);
			    this.date.setTime(d);
			} catch (ParseException e)
			{
				e.printStackTrace();
			}
		}

		public String id()
		{
			String doy = "000"+date.get(Calendar.DAY_OF_YEAR);
			doy = doy.substring(doy.length()-3);
			return date.get(Calendar.YEAR) + "_" + doy;
		}
		
		public String dateString()
		{
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy MM dd HH:mm"); 
			return formatter.format(date.getTime());
		}

		private void writeObject(ObjectOutputStream out) throws IOException
		{
			out.writeObject(date);
			out.writeObject(path);
		}

		private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
		{
			this.date = (Calendar) in.readObject();
			this.path = (String) in.readObject();
		}
		
		
	}
	
}
