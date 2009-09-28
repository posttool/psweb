package com.pagesociety.web.module.resource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.FileUtils;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.util.DateTime;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.RawUIModule;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.S3.PSS3PathProvider;
import com.pagesociety.web.module.S3.amazon.ListEntry;
import com.pagesociety.web.module.permissions.PermissionEvaluator;
import com.pagesociety.web.module.persistence.IPersistenceProvider;

public class ResourceModuleRawUI extends RawUIModule
{
	protected String SLOT_STORE = "store";
	protected String SLOT_PATH_PROVIDER = "resource-path-provider";
	public static final int SM_DELETE_PREVIEWS_ALL = 0x01;
	public static final int SM_LIST_PREVIEWS_FOR_PATH = 0x02;
	public static final int SM_LIST_RESOURCES = 0x03;
	public static final int SM_DELETE_PREVIEWS_FOR_PATH = 0x04;
	public static final int SM_LIST_S3 = 0x05;
	public static final int SM_DELETE_S3_NO_RECORD_FILES = 0x06;
	public static final int SM_BACKUP_S3 = 0x07;
	public static final String PROP_PT = "resource_module_raw_ui_path_token";
	public static final String PROP_DEL_ALL_STATE = "resource_module_raw_ui_delete_all_state";
	public static final String PROP_S3_DB_COMPARE = "resource_module_raw_ui_s3_db_compare";
	public static final String PROP_S3_BACKUP = "resource_module_raw_ui_s3_backup";
	private IPersistenceProvider store;
	private IResourcePathProvider path_provider;
	private String resource_entity_name;
	private File s3_backup_dir;

	public void init(WebApplication app, Map<String, Object> config)
			throws InitializationException
	{
		if (resource_entity_name == null)
			resource_entity_name = ResourceModule.RESOURCE_ENTITY;
		super.init(app, config);
		store = (IPersistenceProvider) getSlot(SLOT_STORE);
		path_provider = (IResourcePathProvider) getSlot(SLOT_PATH_PROVIDER);
		s3_backup_dir = new File(GET_MODULE_DATA_DIRECTORY(app), "s3");
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_STORE, IPersistenceProvider.class, true);
		DEFINE_SLOT(SLOT_PATH_PROVIDER, IResourcePathProvider.class, true);
	}

	protected void declareSubmodes(WebApplication app, Map<String, Object> config)
			throws InitializationException
	{
		try
		{
			declareSubmode(RAW_SUBMODE_DEFAULT, "submode_default");
			declareSubmode(SM_LIST_RESOURCES, "list_db");
			declareSubmode(SM_LIST_PREVIEWS_FOR_PATH, "list_previews_for_path");
			declareSubmode(SM_DELETE_PREVIEWS_ALL, "delete_previews_all");
			declareSubmode(SM_DELETE_PREVIEWS_FOR_PATH, "delete_previews_for_path");
			declareSubmode(SM_LIST_S3, "list_s3");
			declareSubmode(SM_DELETE_S3_NO_RECORD_FILES, "delete_no_record_files");
			declareSubmode(SM_BACKUP_S3, "backup_s3");
		}
		catch (Exception e)
		{
			ERROR(e);
			throw new InitializationException("FAILED BINDING SUBMODE " + e.getMessage());
		}
	}

	protected boolean canExecSubmode(Entity user, int submode, Map<String, Object> params)
	{
		return true;
	}

	public void submode_default(UserApplicationContext uctx, Map<String, Object> params)
	{
		Entity user = (Entity) uctx.getUser();
		if (!PermissionEvaluator.IS_ADMIN(user))
		{
			CALL_WITH_INFO(uctx, "UserModuleRawUI", RAW_SUBMODE_DEFAULT, RAW_SUBMODE_DEFAULT, "ResourceModuleRawUI requires login.");
		}
		else
		{
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE, RAW_UI_LINK_COLOR, RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx, "RESOURCE MODULE", 18);
			DISPLAY_ERROR(uctx, params);
			DISPLAY_INFO(uctx, params);
			P(uctx);
			NAV(uctx);
			P(uctx);
			DOCUMENT_END(uctx);
		}
	}

	private void NAV(UserApplicationContext uctx)
	{
		P(uctx);
		P(uctx);
		A_GET(uctx, getName(), RAW_SUBMODE_DEFAULT, "[ HOME ]");
		BR(uctx);
		A_GET(uctx, getName(), SM_DELETE_PREVIEWS_ALL, "[ DELETE ALL PREVIEWS ]");
		BR(uctx);
		A_GET(uctx, getName(), SM_LIST_RESOURCES, "[ LIST RESOURCES ]");
		BR(uctx);
		if (path_provider instanceof PSS3PathProvider)
		{
			A_GET(uctx, getName(), SM_LIST_S3, "[ LIST S3 FILES WITHOUT RECORDS ]");
			BR(uctx);
			A_GET(uctx, getName(), SM_BACKUP_S3, "[ BACKUP S3 ]");
			BR(uctx);
		}
	}

	public void delete_previews_all(UserApplicationContext uctx,
			Map<String, Object> params) throws PersistenceException,
			WebApplicationException
	{
		Entity user = (Entity) uctx.getUser();
		if (!PermissionEvaluator.IS_ADMIN(user))
		{
			CALL_WITH_INFO(uctx, "UserModuleRawUI", RAW_SUBMODE_DEFAULT, RAW_SUBMODE_DEFAULT, "ResourceModuleRawUI requires login.");
		}
		else
		{
			del_previews_all_task del_op = (del_previews_all_task) uctx.getProperty(PROP_DEL_ALL_STATE);
			if (del_op == null)
			{
				del_op = new del_previews_all_task(uctx);
				uctx.setProperty(PROP_DEL_ALL_STATE, del_op);
			}
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE, RAW_UI_LINK_COLOR, RAW_UI_LINK_HOVER_COLOR);
			SPAN(uctx, "DELETE ALL RESOURCES", 18);
			P(uctx);
			SPAN(uctx, "phase=" + del_op.phase + " offset=" + del_op.offset + " total=" + del_op.total, 12);
			P(uctx);
			if (del_op.last_deleted_previews != null)
			{
				for (String s : del_op.last_deleted_previews)
				{
					SPAN(uctx, "... deleting " + s, 10);
					BR(uctx);
				}
			}
			P(uctx);
			if (del_op.phase == 1)
			{
				uctx.removeProperty(PROP_DEL_ALL_STATE);
				SPAN(uctx, "COMPLETE", 14);
			}
			else
			{
				SPAN(uctx, "WORKING...", 14);
				JS_TIMED_REDIRECT(uctx, getName(), SM_DELETE_PREVIEWS_ALL, 1500);
			}
			P(uctx);
			NAV(uctx);
			DOCUMENT_END(uctx);
		}
	}

	private class del_previews_all_task extends TimerTask
	{
		public UserApplicationContext uctx;
		public Timer timer;
		public int phase = 0;
		public int offset = 0;
		public int pagesize = 20;
		public int total = -1;
		public List<String> last_deleted_previews;

		public del_previews_all_task(UserApplicationContext u)
		{
			this.uctx = u;
			timer = new Timer();
			timer.schedule(this, 0, 700);
		}

		public void next() throws PersistenceException, WebApplicationException
		{
			PagingQueryResult res = get_resources(offset, pagesize);
			total = res.getTotalCount();
			for (Entity r : res.getEntities())
			{
				String path_token = (String) r.getAttribute(ResourceModule.RESOURCE_FIELD_PATH_TOKEN);
				last_deleted_previews = path_provider.listPreviews(path_token);
				path_provider.deletePreviews(path_token);
			}
			offset += pagesize;
			if (offset > total)
			{
				timer.cancel();
				phase = 1;
			}
		}

		public void run()
		{
			try
			{
				next();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public void list_previews_for_path(UserApplicationContext uctx,
			Map<String, Object> params) throws WebApplicationException
	{
		Entity user = (Entity) uctx.getUser();
		if (!PermissionEvaluator.IS_ADMIN(user))
		{
			CALL_WITH_INFO(uctx, "UserModuleRawUI", RAW_SUBMODE_DEFAULT, RAW_SUBMODE_DEFAULT, "ResourceModuleRawUI requires login.");
		}
		else
		{
			String path_token = (String) params.get("path_token");
			if (path_token != null)
				uctx.setProperty(PROP_PT, path_token);
			else
				path_token = (String) uctx.getProperty(PROP_PT);
			List<String> p = path_provider.listPreviews(path_token);
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE, RAW_UI_LINK_COLOR, RAW_UI_LINK_HOVER_COLOR);
			SPAN(uctx, "RESOURCE MODULE", 18);
			P(uctx);
			SPAN(uctx, "path_token=" + path_token);
			P(uctx);
			for (int i = 0; i < p.size(); i++)
			{
				SPAN(uctx, p.get(i));
				BR(uctx);
			}
			A_GET(uctx, getName(), SM_DELETE_PREVIEWS_FOR_PATH, "[ DELETE ]");
			NAV(uctx);
			DOCUMENT_END(uctx);
		}
	}

	public void delete_previews_for_path(UserApplicationContext uctx,
			Map<String, Object> params) throws WebApplicationException
	{
		Entity user = (Entity) uctx.getUser();
		if (!PermissionEvaluator.IS_ADMIN(user))
		{
			CALL_WITH_INFO(uctx, "UserModuleRawUI", RAW_SUBMODE_DEFAULT, RAW_SUBMODE_DEFAULT, "ResourceModuleRawUI requires login.");
		}
		else
		{
			String path_token = (String) uctx.getProperty(PROP_PT);
			path_provider.deletePreviews(path_token);
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE, RAW_UI_LINK_COLOR, RAW_UI_LINK_HOVER_COLOR);
			SPAN(uctx, "DELETING PREVIEWS FOR " + path_token, 18);
			JS_TIMED_REDIRECT(uctx, getName(), SM_LIST_PREVIEWS_FOR_PATH, 1500);
			DOCUMENT_END(uctx);
		}
	}

	public void list_db(UserApplicationContext uctx, Map<String, Object> params)
			throws PersistenceException
	{
		Entity user = (Entity) uctx.getUser();
		if (!PermissionEvaluator.IS_ADMIN(user))
		{
			CALL_WITH_INFO(uctx, "UserModuleRawUI", RAW_SUBMODE_DEFAULT, RAW_SUBMODE_DEFAULT, "ResourceModuleRawUI requires login.");
		}
		else
		{
			String os = (String) params.get("offset");
			int offset = os == null ? 0 : Integer.parseInt(os);
			PagingQueryResult res = get_resources(offset, 15);
			List<Entity> entities = res.getEntities();
			//
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE, RAW_UI_LINK_COLOR, RAW_UI_LINK_HOVER_COLOR);
			SPAN(uctx, "LIST OF DB RESOURCES", 18);
			TABLE_START(uctx, 0, 800);
			for (int i = 0; i < entities.size(); i++)
			{
				TR_START(uctx);
				TD(uctx, String.valueOf(entities.get(i).getId()));
				TD(uctx, (String) entities.get(i).getAttribute(ResourceModule.RESOURCE_FIELD_PATH_TOKEN));
				TD_START(uctx);
				A_GET(uctx, getName(), SM_LIST_PREVIEWS_FOR_PATH, "[ LIST PREVIEWS ]", "path_token", entities.get(i).getAttribute(ResourceModule.RESOURCE_FIELD_PATH_TOKEN));
				TD_END(uctx);
				TR_END(uctx);
			}
			TABLE_END(uctx);
			int pages = res.getTotalCount() / res.getPageSize() + 1;
			for (int i = 0; i < pages; i++)
			{
				int o = i * res.getPageSize();
				A_GET(uctx, getName(), SM_LIST_RESOURCES, String.valueOf(o), "offset", o);
				SPAN(uctx, " | ", 12);
			}
			NAV(uctx);
			DOCUMENT_END(uctx);
		}
	}

	// S3 DB compare / DELETE no record files
	public void list_s3(UserApplicationContext uctx, Map<String, Object> params)
			throws WebApplicationException
	{
		s3_resources s3 = (s3_resources) uctx.getProperty(PROP_S3_DB_COMPARE);
		if (s3 == null)
		{
			s3 = new s3_resources((PSS3PathProvider) path_provider, true);
			uctx.setProperty(PROP_S3_DB_COMPARE, s3);
		}
		//
		String os = (String) params.get("offset");
		int offset = os == null ? 0 : Integer.parseInt(os);
		int pagesize = 20;
		//
		DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE, RAW_UI_LINK_COLOR, RAW_UI_LINK_HOVER_COLOR);
		SPAN(uctx, "LIST OF S3 FILES WITH DB RESOURCE", 18);
		TABLE_START(uctx, 0, 800);
		for (int i = offset; i < offset + pagesize; i++)
		{
			String key = s3.s3list.get(i);
			TR_START(uctx);
			TD(uctx, key);
			TD(uctx, s3.getResourceId(key));
			TR_END(uctx);
		}
		TABLE_END(uctx);
		int pages = s3.s3list.size() / pagesize + 1;
		for (int i = 0; i < pages; i++)
		{
			int o = i * pagesize;
			A_GET(uctx, getName(), SM_LIST_S3, String.valueOf(o), "offset", o);
			SPAN(uctx, " | ", 12);
		}
		P(uctx);
		P(uctx);
		P(uctx);
		A_GET(uctx, getName(), SM_DELETE_S3_NO_RECORD_FILES, "[ DELETE ALL NO RECORD FILES ]");
		NAV(uctx);
		DOCUMENT_END(uctx);
	}

	public void delete_no_record_files(UserApplicationContext uctx,
			Map<String, Object> params) throws WebApplicationException
	{
		s3_resources s3 = (s3_resources) uctx.getProperty(PROP_S3_DB_COMPARE);
		DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE, RAW_UI_LINK_COLOR, RAW_UI_LINK_HOVER_COLOR);
		SPAN(uctx, "DELETE S3 FILE WITHOUT RESOURCE", 18);
		P(uctx);
		for (int i = 0; i < s3.s3list.size(); i++)
		{
			String key = s3.s3list.get(i);
			if (key.endsWith(".xml"))
				continue;
			if (!s3.containsPath(key))
			{
				path_provider.delete(key);
				SPAN(uctx, " ... delete " + key);
				BR(uctx);
			}
		}
		NAV(uctx);
		DOCUMENT_END(uctx);
		uctx.removeProperty(PROP_S3_DB_COMPARE);
	}

	private class s3_resources
	{
		PSS3PathProvider s3pp;
		public PagingQueryResult resources;
		public Map<String, Entity> res_map;
		private List<String> s3list;

		public s3_resources(PSS3PathProvider pathProvider, boolean get_db_too)
		{
			s3pp = pathProvider;
			try
			{
				String marker = "";
				int max_keys = 200;
				s3list = new ArrayList<String>();
				while (true)
				{
					List<ListEntry> r = s3pp.list("", marker, max_keys);
					if (r.isEmpty())
						break;
					for (ListEntry l : r)
						s3list.add(l.key);
					marker = r.get(r.size() - 1).key;
					System.out.println("GOT " + s3list.size() + " RESULTS ... ");
				}
			}
			catch (WebApplicationException e)
			{
				e.printStackTrace();
			}
			if (get_db_too)
			{
				try
				{
					resources = get_resources(0, Query.ALL_RESULTS);
					res_map = new HashMap<String, Entity>();
					for (Entity e : resources.getEntities())
					{
						res_map.put((String) e.getAttribute(ResourceModule.RESOURCE_FIELD_PATH_TOKEN), e);
					}
				}
				catch (PersistenceException e)
				{
					e.printStackTrace();
				}
			}
		}

		public Boolean containsPath(String path_token)
		{
			return res_map.containsKey(path_token);
		}

		public String getResourceId(String path_token)
		{
			if (containsPath(path_token))
				return String.valueOf(res_map.get(path_token).getId());
			else
				return "-";
		}
	}

	// BACKUP
	public void backup_s3(UserApplicationContext uctx, Map<String, Object> params)
			throws WebApplicationException
	{
		s3_backup s3 = (s3_backup) uctx.getProperty(PROP_S3_BACKUP);
		if (s3 == null)
		{
			File dest_dir = new File(GET_MODULE_DATA_DIRECTORY(_application), "S3-" + DateTime.getTimeStamp());
			s3 = new s3_backup((PSS3PathProvider) path_provider, dest_dir);
			uctx.setProperty(PROP_S3_BACKUP, s3);
		}
		// ////////////////
		DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE, RAW_UI_LINK_COLOR, RAW_UI_LINK_HOVER_COLOR);
		SPAN(uctx, "BACKUP S3", 18);
		P(uctx);
		SPAN(uctx, "phase=" + s3.phase + " offset=" + s3.offset + " total=" + s3.total, 12);
		P(uctx);
		if (s3.processed != null)
		{
			int pagesize = 20;
			int i = Math.max(s3.processed.size() - pagesize, 0);
			int s = s3.processed.size();
			
			for (; i < s; i++)
			{
				SPAN(uctx, "... backing up " + s3.processed.get(i), 10);
				BR(uctx);
			}
		}
		P(uctx);
		if (s3.phase == 1)
		{
			uctx.removeProperty(PROP_S3_BACKUP);
			SPAN(uctx, "COMPLETE", 14);
		}
		else
		{
			SPAN(uctx, "WORKING...", 14);
			JS_TIMED_REDIRECT(uctx, getName(), SM_BACKUP_S3, 7500);
		}
		P(uctx);
		NAV(uctx);
		DOCUMENT_END(uctx);
	}

	private class s3_backup extends TimerTask
	{
		private s3_resources s3;
		public Timer timer;
		public int phase = 0;
		public int offset = 0;
		public int pagesize = 15;
		public int total = -1;
		public File root_dir;
		public ArrayList<String> processed;

		public s3_backup(PSS3PathProvider p, File d)
		{
			root_dir = d;
			s3 = new s3_resources(p, false);
			total = s3.s3list.size();
			processed = new ArrayList<String>();
			timer = new Timer();
			timer.schedule(this, 0, 250);
		}

		public void next() throws PersistenceException, WebApplicationException,
				IOException
		{
			System.out.println(">HERE");
			for (int i = offset; i < offset + pagesize; i++)
			{
				String key = s3.s3list.get(i);
				File dest_file = new File(s3_backup_dir, key);
				System.out.println("  i="+i+" key="+key+" exists="+dest_file.exists());
				if (!dest_file.exists())
				{
					File f = s3.s3pp.getFile(key);
					if (f==null)
					{
						System.out.println("CANT DOWNLOAD... cause i am out of memory");
					}
					FileUtils.copyFile(f, dest_file);
					processed.add(f.getName());
					System.out.println("s3_backup " + dest_file);
				}
				else
				{
					processed.add("["+key+"]");
				}
			}
			offset += pagesize;
			if (offset >= total)
			{
				timer.cancel();
				phase = 1;
			}
		}

		public void run()
		{
			try
			{
				next();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	// //////////////// util
	private PagingQueryResult get_resources(int offset, int pagesize)
			throws PersistenceException
	{
		Query q = new Query(resource_entity_name);
		q.idx(Query.PRIMARY_IDX);
		q.gt(0);
		q.offset(offset);
		q.pageSize(pagesize);
		return WebStoreModule.PAGING_QUERY(store.getStore(), q);
	}
}
