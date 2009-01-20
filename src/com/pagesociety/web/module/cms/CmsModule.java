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



public class CmsModule extends WebStoreModule 
{	
	
	private static final String SLOT_CMS_GUARD = "cms-guard"; 
	private ICmsGuard guard;
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		guard		= (ICmsGuard)getSlot(SLOT_CMS_GUARD);
	}

	
	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_CMS_GUARD,ICmsGuard.class,false,DefaultCmsGuard.class);
	}
	
	@Export
	public List<EntityDefinition> GetEntityDefinitions(UserApplicationContext ctx) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)ctx.getUser();
		GUARD(guard.canGetEntityDefinitions(user));
		
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
		GUARD(guard.canBrowseEntities(user, entity_type));
		
		return browseEntities(entity_type, order_by_attribute, asc, offset, page_size);
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
	
	
	@Export(ParameterNames={"e"})
	public Entity CreateEntity(UserApplicationContext uctx,Entity e) throws WebApplicationException, PersistenceException
	{
		Entity creator = (Entity)uctx.getUser();

		GUARD(guard.canCreateEntity(creator, e));
		return createEntity(creator, e);
	}
	
	public Entity createEntity(Entity creator,Entity e) throws PersistenceException
	{
		return CREATE_ENTITY(creator, e);
	}
	
	
	@Export(ParameterNames={"e"})
	public Entity UpdateEntity(UserApplicationContext uctx,Entity e) throws WebApplicationException, PersistenceException
	{
		Entity editor = (Entity)uctx.getUser();
		
		GUARD(guard.canUpdateEntity(editor, e));
		return updateEntity(editor, e);
	}
	
	public Entity updateEntity(Entity editor,Entity e) throws PersistenceException
	{
		return SAVE_ENTITY(e);
	}
	
	@Export(ParameterNames={"e"})
	public Entity DeleteEntity(UserApplicationContext uctx,Entity e) throws WebApplicationException, PersistenceException
	{
		Entity deleter = (Entity)uctx.getUser();
		GUARD(guard.canDeleteEntity(deleter, e));
		/* call to delete provider slot here */
		return deleteEntity(deleter, e);	
	}
	
	public Entity deleteEntity(Entity deleter,Entity e) throws PersistenceException
	{
		return DELETE(e);
	}
		
	@Export(ParameterNames={"entity_type","entity_id"})
	public Entity GetEntityById(UserApplicationContext uctx,String entity_type,long entity_id) throws WebApplicationException, PersistenceException
	{
		Entity getter = (Entity)uctx.getUser();
		Entity e = GET(entity_type,entity_id);
		GUARD(guard.canGetEntity(getter,e));
		return FILL_REFS(e);
	}
	
	public Entity getEntityById(String entity_type,long entity_id) throws PersistenceException,WebApplicationException
	{
		Entity e = FILL_REFS(GET(entity_type, entity_id));
		return e;		
	}

}
