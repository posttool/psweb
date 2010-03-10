package com.pagesociety.web.module.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityDefinition;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.Module;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.WebModule;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.WebModule.OBJECT;
import com.pagesociety.web.module.permissions.DefaultPermissionsModule;
import com.pagesociety.web.module.permissions.PermissionsModule;
import com.pagesociety.web.module.resource.ResourceModule;

public class ApplicationInfoModule extends WebModule
{
	
	protected void exportPermissions()
	{/*
		EXPORT_PERMISSION(CAN_READ_SCHEMA);
		EXPORT_PERMISSION(CAN_CREATE_ENTITY);
		EXPORT_PERMISSION(CAN_READ_ENTITY);
		EXPORT_PERMISSION(CAN_UPDATE_ENTITY);
		EXPORT_PERMISSION(CAN_DELETE_ENTITY);
		EXPORT_PERMISSION(CAN_BROWSE_ENTITY);
	*/
	}

	public void init(WebApplication app, Map<String, Object> config)
			throws InitializationException
	{
		super.init(app, config);
	//	permissions = (PermissionsModule) getSlot(SLOT_PERMISSIONS_MODULE);
	}

	protected void defineSlots()
	{
		super.defineSlots();
	//	DEFINE_SLOT(SLOT_PERMISSIONS_MODULE, DefaultPermissionsModule.class, false, DefaultPermissionsModule.class);
	}
	
	@Export
	public Map<String,String> GetApplicationInitParams(UserApplicationContext uctx)
	{
		return getApplication().getConfig().getInitParameters();		
	}
	
	@Export
	public String GetApplicationInitParam(UserApplicationContext uctx,String name)
	{
		return getApplication().getConfig().getInitParameter(name);		
	}

	//TODO:GUARD???
	//this should ultimately live in the application...not a module
	@Export
	public List<Map<String,Object>> GetAppResourceInfo(UserApplicationContext ctx) throws WebApplicationException
	{
		List<Module> modules = getApplication().getModules();
		List ret 			 = new ArrayList<Map<String,Object>>();

		
		for(int i = 0;i < modules.size();i++)
		{
			Module m = modules.get(i);
			if(m instanceof ResourceModule)
			{
				ResourceModule rm 		= (ResourceModule)m;
				String modulename 		= m.getName();
				String resource_entity 		= rm.getResourceEntityName();
				String resource_base_url 	= rm.getResourceBaseURL();
				ret.add(new OBJECT(	"resource_module_name",modulename,
									"resource_entity_name",resource_entity,
									"resource_base_url",resource_base_url));
			}
		}
		return ret;
	}

}
