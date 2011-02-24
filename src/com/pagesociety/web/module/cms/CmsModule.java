package com.pagesociety.web.module.cms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.TransactionProtect;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.permissions.DefaultPermissionsModule;
import com.pagesociety.web.module.permissions.PermissionsModule;
import com.pagesociety.web.module.tree.TreeModule;

public class CmsModule extends WebStoreModule
{
	private static final String SLOT_PERMISSIONS_MODULE = "permissions-module";
	public static final String CAN_READ_SCHEMA = "CAN_READ_SCHEMA";
	public static final String CAN_CREATE_ENTITY = "CAN_CREATE_ENTITY";
	public static final String CAN_READ_ENTITY 	= "CAN_READ_ENTITY";
	public static final String CAN_UPDATE_ENTITY = "CAN_UPDATE_ENTITY";
	public static final String CAN_DELETE_ENTITY = "CAN_DELETE_ENTITY";
	public static final String CAN_BROWSE_ENTITY = "CAN_BROWSE_ENTITY";
	
	public static final int EVENT_ENTITY_PRE_CREATE	  = 0x1001;
	public static final int EVENT_ENTITY_POST_CREATE  = 0x1002;
	public static final int EVENT_ENTITY_PRE_UPDATE   = 0x1003;
	public static final int EVENT_ENTITY_POST_UPDATE  = 0x1004;
	public static final int EVENT_ENTITY_PRE_DELETE   = 0x1005;
	public static final int EVENT_ENTITY_POST_DELETE  = 0x1006;


	protected void exportPermissions()
	{
		EXPORT_PERMISSION(CAN_READ_SCHEMA);
		EXPORT_PERMISSION(CAN_CREATE_ENTITY);
		EXPORT_PERMISSION(CAN_READ_ENTITY);
		EXPORT_PERMISSION(CAN_UPDATE_ENTITY);
		EXPORT_PERMISSION(CAN_DELETE_ENTITY);
		EXPORT_PERMISSION(CAN_BROWSE_ENTITY);
	}
	
	
	

	public void init(WebApplication app, Map<String, Object> config)
			throws InitializationException
	{
		super.init(app, config);
		permissions = (PermissionsModule) getSlot(SLOT_PERMISSIONS_MODULE);
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_PERMISSIONS_MODULE, DefaultPermissionsModule.class, false, DefaultPermissionsModule.class);
	}

	@Export
	public List<EntityDefinition> GetEntityDefinitions(UserApplicationContext ctx)
			throws WebApplicationException, PersistenceException
	{
		Entity user = (Entity) ctx.getUser();
		GUARD(user, CAN_READ_SCHEMA);
		return getEntityDefinitions();
	}

	public List<EntityDefinition> getEntityDefinitions() throws PersistenceException
	{
		return store.getEntityDefinitions();
	}

	@Export
	public List<EntityIndex> GetEntityIndices(UserApplicationContext ctx, String entity)
			throws WebApplicationException, PersistenceException
	{
		Entity user = (Entity) ctx.getUser();
		GUARD(user, CAN_READ_SCHEMA);
		return getEntityIndices(entity);
	}

	@Export
	public Map<String, List<EntityIndex>> GetEntityIndices(UserApplicationContext ctx)
			throws WebApplicationException, PersistenceException
	{
		Entity user = (Entity) ctx.getUser();
		GUARD(user, CAN_READ_SCHEMA);
		Map<String, List<EntityIndex>> indices = new HashMap<String, List<EntityIndex>>();
		List<EntityDefinition> defs = store.getEntityDefinitions();
		int s = defs.size();
		for (int i = 0; i < s; i++)
		{
			String entity = defs.get(i).getName();
			indices.put(entity, store.getEntityIndices(entity));
		}
		return indices;
	}

	public List<EntityIndex> getEntityIndices(String entity) throws PersistenceException
	{
		return store.getEntityIndices(entity);
	}

	@Export(ParameterNames = { "entity_type", "index_info", "order_by_attribute", "asc",
			"offset", "page_size" })
	public PagingQueryResult BrowseEntities(UserApplicationContext ctx,
			String entity_type, List<Object> query, String order_by_attribute,
			boolean asc, int offset, int page_size) throws WebApplicationException,
			PersistenceException
	{
		return BrowseEntities(ctx,entity_type,query,order_by_attribute,asc,offset,page_size,true);
	}
	
	@Export(ParameterNames = { "entity_type", "index_info", "order_by_attribute", "asc",
			"offset", "page_size", "fill_deep" })
	public PagingQueryResult BrowseEntities(UserApplicationContext ctx,
			String entity_type, List<Object> query, String order_by_attribute,
			boolean asc, int offset, int page_size, boolean fill) throws WebApplicationException,
			PersistenceException
	{
		Entity user = (Entity) ctx.getUser();
		GUARD(user, CAN_BROWSE_ENTITY, "entity_type", entity_type);
		PagingQueryResult result = browseEntities(entity_type, query, order_by_attribute, asc, offset, page_size, fill);
		return result;
	}

	public PagingQueryResult browseEntities(String entity_type, List<Object> query,
			String order_by_attribute, boolean asc, int offset, int page_size, boolean fill)
			throws PersistenceException
	{
		Query q = new Query(entity_type);
		int s = query.size();
		int c = 0;
		while (c < s)
		{
			String index_name = (String) query.get(c);
			q.idx(index_name);
			c++;
			int code = (Integer) query.get(c);
			c++;
			Object val = query.get(c);
			c++;
			Object bottom_val = null;
			Object top_val = null;
			switch (code)
			{
			case Query.BETWEEN_ITER_TYPE:
			case Query.BETWEEN_INCLUSIVE_ASC:
			case Query.BETWEEN_EXCLUSIVE_ASC:
			case Query.BETWEEN_START_INCLUSIVE_ASC:
			case Query.BETWEEN_END_INCLUSIVE_ASC:
			case Query.BETWEEN_INCLUSIVE_DESC:
			case Query.BETWEEN_EXCLUSIVE_DESC:
			case Query.BETWEEN_START_INCLUSIVE_DESC:
			case Query.BETWEEN_END_INCLUSIVE_DESC:
				bottom_val = val;
				top_val = query.get(c);
				c++;
				break;
			}
			switch (code)
			{
			case 996:
				q.startIntersection();
				break;
			case 997:
				q.endIntersection();
				break;
			case 998:
				q.startUnion();
				break;
			case 999:
				q.endUnion();
				break;
			case Query.EQ:
				q.eq(val);
				break;
			case Query.GT:
				q.gt(val);
				break;
			case Query.GTE:
				q.gte(val);
				break;
			case Query.LT:
				q.lt(val);
				break;
			case Query.LTE:
				q.lte(val);
				break;
			case Query.STARTSWITH:
				q.startsWith(val);
				break;
			case Query.IS_ANY_OF:
				q.isAnyOf((List<?>) val);
				break;
			case Query.BETWEEN_ITER_TYPE:
				q.between(bottom_val, top_val);
				break;
			case Query.BETWEEN_INCLUSIVE_ASC:
				q.betweenStartInclusive(bottom_val, top_val);
				break;
			case Query.BETWEEN_EXCLUSIVE_ASC:
				q.betweenExclusive(bottom_val, top_val);
				break;
			case Query.BETWEEN_START_INCLUSIVE_ASC:
				q.betweenStartInclusive(bottom_val, top_val);
				break;
			case Query.BETWEEN_END_INCLUSIVE_ASC:
				q.betweenEndInclusive(bottom_val, top_val);
				break;
			case Query.BETWEEN_INCLUSIVE_DESC:
				q.betweenStartInclusiveDesc(bottom_val, top_val);
				break;
			case Query.BETWEEN_EXCLUSIVE_DESC:
				q.betweenExclusiveDesc(top_val, bottom_val);
				break;
			case Query.BETWEEN_START_INCLUSIVE_DESC:
				q.betweenStartInclusiveDesc(top_val, bottom_val);
				break;
			case Query.BETWEEN_END_INCLUSIVE_DESC:
				q.betweenEndInclusiveDesc(top_val, bottom_val);
				break;
			case Query.SET_CONTAINS_ALL:
				q.setContainsAll((List<?>) val);
				break;
			case Query.SET_CONTAINS_ANY:
				q.setContainsAny((List<?>) val);
				break;
			case Query.FREETEXT_CONTAINS_PHRASE:
				EntityIndex index = store.getEntityIndex(entity_type, index_name);
				String[] vals = ((String) val).split(" ");
				if (index.getType() == EntityIndex.TYPE_FREETEXT_INDEX)
				{
					q.textContainsAny(Arrays.asList(vals));
				}
				else
				{
					List<FieldDefinition> fields = index.getFields();
					int fs = fields.size();
					List<Object> field_args = new ArrayList<Object>(fs);
					for (int i = 0; i < fs; i++)
					{
						FieldDefinition fd = fields.get(i);
						field_args.add(fd.getName());
					}
					q.textContainsAny(q.list(field_args, Arrays.asList(vals)));
				}
			}
		}
		q.offset(offset);
		q.pageSize(page_size);
		if (order_by_attribute != null)
			q.orderBy(order_by_attribute, asc ? Query.ASC : Query.DESC);
		q.cacheResults(false);
		if (fill)
			return PAGING_QUERY_FILL(q);
		else
			return PAGING_QUERY(q);
	}


	@Export(ParameterNames = { "entity_type", "order_by_attribute", "asc", "offset",
	"page_size" })
	public PagingQueryResult BrowseEntities(UserApplicationContext ctx,
		String entity_type, String order_by_attribute, boolean asc, int offset,
		int page_size) throws WebApplicationException, PersistenceException
	{
		return BrowseEntities( ctx,
				 entity_type,  order_by_attribute,  asc,  offset,
				 page_size,  true);
	}
		
	@Export(ParameterNames = { "entity_type", "order_by_attribute", "asc", "offset",
			"page_size" })
	public PagingQueryResult BrowseEntities(UserApplicationContext ctx,
			String entity_type, String order_by_attribute, boolean asc, int offset,
			int page_size, boolean fill) throws WebApplicationException, PersistenceException
	{
		Entity user = (Entity) ctx.getUser();
		GUARD(user, CAN_BROWSE_ENTITY, "entity_type", entity_type);
		PagingQueryResult result = browseEntities(entity_type, order_by_attribute, asc, offset, page_size, fill);
		return result;
	}

	public PagingQueryResult browseEntities(String entity_type,
			String order_by_attribute, boolean asc, int offset, int page_size, boolean fill)
			throws PersistenceException
	{
		Query q = new Query(entity_type);
		q.idx(Query.PRIMARY_IDX);
		q.eq(Query.VAL_GLOB);
		q.offset(offset);
		q.pageSize(page_size);
		if (order_by_attribute != null)
			q.orderBy(order_by_attribute, asc ? Query.ASC : Query.DESC);
		q.cacheResults(true);
		if (fill)
			return PAGING_QUERY_FILL(q);
		else
			return PAGING_QUERY(q);
	}

	@Deprecated
	@Export(ParameterNames = { "entity" })
	@TransactionProtect
	public Entity CreateEntity(UserApplicationContext uctx, Entity e)
			throws WebApplicationException, PersistenceException
	{
		Entity creator = (Entity) uctx.getUser();
		GUARD(creator, CAN_CREATE_ENTITY, "type", e.getType(), "instance", e);
		return createEntity(creator, e);
	}
	//TODO call createEntity(creator, e.getType(), e.getAttributes()) and fix SWA event that looks for "candidate"

	@Deprecated
	public Entity createEntity(Entity creator, Entity e) throws PersistenceException,WebApplicationException
	{
		DISPATCH_EVENT(EVENT_ENTITY_PRE_CREATE, "candidate",e);
		Entity ce = CREATE_ENTITY(creator, e);
		DISPATCH_EVENT(EVENT_ENTITY_POST_CREATE, "candidate",e,"instance",ce);
		return ce;
	}
	
	@Export(ParameterNames = { "type","map" })
	@TransactionProtect
	public Entity CreateEntity(UserApplicationContext uctx, String type, Map<String,Object> value_map)
			throws WebApplicationException, PersistenceException
	{
		Entity creator = (Entity) uctx.getUser();
		GUARD(creator, CAN_CREATE_ENTITY, "type", type, "values", value_map);
		return createEntity(creator, type,value_map);
	}

	public Entity createEntity(Entity creator, String type, Map<String,Object> value_map) throws PersistenceException,WebApplicationException
	{
		DISPATCH_EVENT(EVENT_ENTITY_PRE_CREATE,"type",type, "values",value_map);
		Entity ce = NEW(type,creator,value_map);
		DISPATCH_EVENT(EVENT_ENTITY_POST_CREATE, "type",type,"values",value_map,"instance",ce);
		return ce;
	}

	@Deprecated
	@Export(ParameterNames = { "e" })
	@TransactionProtect
	public Entity UpdateEntity(UserApplicationContext uctx, Entity e)
			throws WebApplicationException, PersistenceException
	{
		Entity editor = (Entity) uctx.getUser();
		Entity existing_instance = GET(e.getType(), e.getId());
		GUARD(editor, CAN_UPDATE_ENTITY, "type", e.getType(), "instance", existing_instance);
	
		return updateEntity(e);
	}
	
	@Deprecated
	public Entity updateEntity(Entity e) throws PersistenceException,WebApplicationException
	{
		DISPATCH_EVENT(EVENT_ENTITY_PRE_UPDATE, "candidate",e);
		Entity ue = SAVE_ENTITY(e);
		DISPATCH_EVENT(EVENT_ENTITY_POST_UPDATE, "candidate",e,"instance",ue);
		return ue;
	}
	
	@Export(ParameterNames = { "type","id","map" })
	@TransactionProtect
	public Entity UpdateEntity(UserApplicationContext uctx, String type,long id,Map<String,Object> update_map)
			throws WebApplicationException, PersistenceException
	{
		Entity editor = (Entity) uctx.getUser();
		//Entity existing_instance = GET(e.getType(), e.getId());
		GUARD(editor, CAN_UPDATE_ENTITY, "type", type, "values", update_map);
		return updateEntity(type,id,update_map);
	}
	
	public Entity updateEntity(String type,long id,Map<String,Object> update_map) throws PersistenceException,WebApplicationException
	{
		DISPATCH_EVENT(EVENT_ENTITY_PRE_UPDATE, "type",type,"id",id,"values",update_map);
		Entity ue = UPDATE(type, id, update_map);
		DISPATCH_EVENT(EVENT_ENTITY_POST_UPDATE, "type",type,"id",id,"values",update_map,"instance",ue);
		return ue;
	}

	@Export(ParameterNames = { "entity" })
	@TransactionProtect
	public Entity DeleteEntity(UserApplicationContext uctx, Entity e)
			throws WebApplicationException, PersistenceException
	{
		Entity deleter = (Entity) uctx.getUser();
		GUARD(deleter, CAN_DELETE_ENTITY, "type", e.getType(), "instance", e);		
		return deleteEntity(e);

	}

	public Entity deleteEntity(Entity e) throws PersistenceException,WebApplicationException
	{
		DISPATCH_EVENT(EVENT_ENTITY_PRE_DELETE, "candidate",e);
		Entity de = DELETE(e);
		DISPATCH_EVENT(EVENT_ENTITY_POST_DELETE, "candidate",e,"instance",de);
		return de;
	}

	@Export(ParameterNames = { "entity_type", "entity_id" })
	public Entity GetEntityById(UserApplicationContext uctx, String entity_type,
			long entity_id) throws WebApplicationException, PersistenceException
	{
		Entity getter = (Entity) uctx.getUser();
		Entity e = GET(entity_type, entity_id);
		GUARD(getter, CAN_READ_ENTITY, "entity_type", entity_type, "instance", e);
		return FILL_DEEP_AND_MASK(e, FILL_ALL_FIELDS, new String[]{ TreeModule.TREE_NODE_FIELD_TREE, TreeModule.TREE_NODE_FIELD_PARENT_NODE });
	//	return FILL_REFS(e);
	}

	public Entity getEntityById(String entity_type, long entity_id)
			throws PersistenceException, WebApplicationException
	{
		Entity e = FILL_REFS(GET(entity_type, entity_id));
		return e;
	}
}
