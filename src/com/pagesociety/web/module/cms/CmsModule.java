package com.pagesociety.web.module.cms;



import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityDefinition;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.permissions.DefaultPermissionsModule;
import com.pagesociety.web.module.permissions.PermissionEvaluator;
import com.pagesociety.web.module.permissions.PermissionsModule;



public class CmsModule extends WebStoreModule 
{	
	
	private static final String SLOT_PERMISSIONS_MODULE = "permissions-module"; 
	private PermissionsModule permissions;
	
	public static final String CAN_READ_SCHEMA   = "CAN_READ_SCHEMA";
	public static final String CAN_CREATE_ENTITY = "CAN_CREATE_ENTITY";
	public static final String CAN_READ_ENTITY   = "CAN_READ_ENTITY";
	public static final String CAN_UPDATE_ENTITY = "CAN_UPDATE_ENTITY";
	public static final String CAN_DELETE_ENTITY = "CAN_DELETE_ENTITY";
	public static final String CAN_BROWSE_ENTITY = "CAN_BROWSE_ENTITY";
	
	protected void exportPermissions()
	{
		EXPORT_PERMISSION(CAN_READ_SCHEMA);
		EXPORT_PERMISSION(CAN_CREATE_ENTITY);
		EXPORT_PERMISSION(CAN_READ_ENTITY);
		EXPORT_PERMISSION(CAN_UPDATE_ENTITY);
		EXPORT_PERMISSION(CAN_DELETE_ENTITY);
		EXPORT_PERMISSION(CAN_BROWSE_ENTITY);
	}
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		permissions		= (PermissionsModule)getSlot(SLOT_PERMISSIONS_MODULE);
	}

	
	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_PERMISSIONS_MODULE,DefaultPermissionsModule.class,false,DefaultPermissionsModule.class);
	}
	
	@Export
	public List<EntityDefinition> GetEntityDefinitions(UserApplicationContext ctx) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)ctx.getUser();
		GUARD(user,CAN_READ_SCHEMA);		
		return getEntityDefinitions();
	}
	
	public List<EntityDefinition> getEntityDefinitions() throws PersistenceException
	{
		return store.getEntityDefinitions();
	}
	
	@Export(ParameterNames={"entity_type","order_by_attribute","asc","offset","page_size"})
	public PagingQueryResult BrowseEntities(UserApplicationContext ctx,String entity_type,String order_by_attribute,boolean asc,int offset, int page_size) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)ctx.getUser();
		GUARD(user,CAN_BROWSE_ENTITY,"entity_type",entity_type);
		PagingQueryResult result =  browseEntities(entity_type, order_by_attribute, asc, offset, page_size);
		return result;
	}
	
	public PagingQueryResult browseEntities(String entity_type,String order_by_attribute,boolean asc,int offset, int page_size) throws PersistenceException
	{
		Query q = new Query(entity_type);
		q.idx(Query.PRIMARY_IDX);
		q.eq(Query.VAL_GLOB);
		q.offset(offset);
		q.pageSize(page_size);
		if (order_by_attribute!=null)
			q.orderBy(order_by_attribute,asc?Query.ASC:Query.DESC);
		q.cacheResults(false);
		return PAGING_QUERY(q);
	}
	
	
	@Export(ParameterNames={"entity"})
	public Entity CreateEntity(UserApplicationContext uctx,Entity e) throws WebApplicationException, PersistenceException
	{
		Entity creator = (Entity)uctx.getUser();
		GUARD(creator,CAN_CREATE_ENTITY,"type",e.getType(),"instance",e);
		return createEntity(creator, e);
	}
	
	public Entity createEntity(Entity creator,Entity e) throws PersistenceException
	{
		return CREATE_ENTITY(creator, e);
	}
	
	
	@Export(ParameterNames={"e"})
	public Entity UpdateEntity(UserApplicationContext uctx,Entity e) throws WebApplicationException, PersistenceException
	{
		Entity editor 			 = (Entity)uctx.getUser();
		Entity existing_instance = GET(e.getType(),e.getId());
		GUARD(editor,CAN_UPDATE_ENTITY,"type",e.getType(),"instance",existing_instance);
		return updateEntity(e);
	}
	
	public Entity updateEntity(Entity e) throws PersistenceException
	{
		return SAVE_ENTITY(e);
	}
	
	@Export(ParameterNames={"entity"})
	public Entity DeleteEntity(UserApplicationContext uctx,Entity e) throws WebApplicationException, PersistenceException
	{
		Entity deleter = (Entity)uctx.getUser();
		GUARD(deleter,CAN_DELETE_ENTITY,"type",e.getType(),"instance", e);
		return deleteEntity(e);	
	}
	
	public Entity deleteEntity(Entity e) throws PersistenceException
	{
		return DELETE(e);
	}
		
	@Export(ParameterNames={"entity_type","entity_id"})
	public Entity GetEntityById(UserApplicationContext uctx,String entity_type,long entity_id) throws WebApplicationException, PersistenceException
	{
		Entity getter = (Entity)uctx.getUser();
		Entity e = GET(entity_type,entity_id);
		GUARD(getter,CAN_READ_ENTITY,"entity_type",entity_type,"instance",e);
		return FILL_REFS(e);
	}
	
	public Entity getEntityById(String entity_type,long entity_id) throws PersistenceException,WebApplicationException
	{
		Entity e = FILL_REFS(GET(entity_type, entity_id));
		return e;		
	}

}
