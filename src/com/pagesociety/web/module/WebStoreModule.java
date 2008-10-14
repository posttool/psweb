package com.pagesociety.web.module;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityDefinition;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.PersistentStore;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.persistence.IPersistenceProvider;

public abstract class WebStoreModule extends WebModule
{
	private static final String CONFIG_KEY_STORE_NAME = "store-name";
	private static final String SLOT_STORE = "store";
	protected PersistentStore store;
	
	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);
		store = ((IPersistenceProvider)getSlot(SLOT_STORE)).getStore();
		try{
			defineEntities(config);
			defineIndexes(config);
			defineRelationships(config);
		}catch(Exception e)
		{
			ERROR(e);
			throw new InitializationException("FAILED SETTING UP "+getName()+" MODULE.");
		}		
	}
	
	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_STORE, IPersistenceProvider.class, true);
	}	

	protected void defineEntities(Map<String,Object> config)throws PersistenceException,SyncException
	{
		/* do nothing by default*/
	}
	
	protected void defineIndexes(Map<String,Object> config)throws PersistenceException,SyncException
	{
		/*do nothing by default */
	}
	
	protected void defineRelationships(Map<String,Object> config)throws PersistenceException,SyncException
	{
		/*do nothing by default */
	}
	

	
	/* system level fields...at some point we will have the app potentially set these up 
	 * when it starts ADD_SYSTEM_LEVEL_FIELD,RENAME_SYSTEM_LEVEL_FIELD etc
	 */
	
	public static String FIELD_CREATOR					=   "creator"; 
	public static String FIELD_DATE_CREATED				=   "date_created";
	public static String FIELD_LAST_MODIFIED			=   "last_modified";	
	public static FieldDefinition[] FIELDS = new FieldDefinition[] 
	{
		(new FieldDefinition(FIELD_CREATOR, Types.TYPE_REFERENCE,"User")),
		(new FieldDefinition(FIELD_DATE_CREATED, Types.TYPE_DATE)),
		(new FieldDefinition(FIELD_LAST_MODIFIED, Types.TYPE_DATE)),
	};	
		
	public static EntityDefinition DEFINE_ENTITY(PersistentStore store,String entity_name,Object... fields) throws PersistenceException,SyncException
	{
		EntityDefinition 		existing_def;
		EntityDefinition 		proposed_def;
			
		existing_def = store.getEntityDefinition(entity_name);
		proposed_def = new EntityDefinition(entity_name);

		/* add system fields to proposed def */
		for(int i = 0;i < FIELDS.length;i++)
				proposed_def.addField(FIELDS[i]);
		
		int i = 0;
		for(;;)
		{
			String fieldname 	= (String)fields[i++]; 
			int type 		 	= (Integer)fields[i++];
			FieldDefinition f ;
			if ((type & ~Types.TYPE_ARRAY) != Types.TYPE_REFERENCE)
				f = new FieldDefinition(fieldname,type);
			else
				f = new FieldDefinition(fieldname,type,(String)fields[i++]);
			f.setDefaultValue(fields[i++]);
			
			proposed_def.addField(f);
			if(i>fields.length - 1)
				break;
		}
		
		/*create it if it doesnt exist*/
		if(existing_def == null)
			store.addEntityDefinition(proposed_def);
		else
		{
			/* check it to make sure the version passed in matches the one in the store */
			if(!existing_def.equals(proposed_def))
				throw new SyncException("FAILED DEFINING ENTITY: EXISTING DEF -\n "+existing_def+"\n"+
										"DOES NOT MATCH PROPOSED DEF -\n"+proposed_def+"\n");
		
			/* add any additional default system fields...deletes of system fields 
			 * have to be done with an alter for now... */
			for(i = 0;i < FIELDS.length;i++)
			{
				FieldDefinition f = FIELDS[i];
				if(existing_def.getField(f.getName())==null)
					store.addEntityField(entity_name,f);
			}
		}
		return proposed_def;
	}

	public static void DEFINE_ENTITY_INDEX(PersistentStore store,String entity_name,String index_name,int index_type,String... field_names) throws PersistenceException,SyncException
	{
		List<EntityIndex> idxs = store.getEntityIndices(entity_name);
		for(int i = 0;i < idxs.size();i++)
		{
			EntityIndex idx = idxs.get(i);
			if(idx.getName().equals(index_name))
			{
				List<FieldDefinition> fields = idx.getFields();
				if(fields.size() != field_names.length)
					throw new SyncException("ENTITY INDEX "+index_name+" ALREADY EXISTS ON ENTITY "+entity_name+" BUT THE DATABASE VERSION HAS A DIFFERENT NUMBER OF FIELDS dbVersion is "+idx);
					
				for(int j = 0;j < fields.size();j++)
				{
					FieldDefinition f = fields.get(j);
					if(field_names[j].equals(f.getName()))
						continue;
					else
						throw new SyncException("ENTITY INDEX "+index_name+" ALREADY EXISTS ON ENTITY "+entity_name+" BUT THE DATABASE VERSION DIFFERS ON FIELD "+field_names[j]+" dbVersion "+idx);

				}
				return;//the index exists and it checked out
			}
		}
		//the index didnt exist..create it
		store.addEntityIndex(entity_name, field_names, index_type, index_name, null);	
		store.getEntityRelationships();
	}

	public static Entity NEW(PersistentStore store,String entity_type,Entity creator,Object ...attribute_name_values) throws PersistenceException
	{
		
		Entity e = store.getEntityDefinition(entity_type).createInstance();
		set_attributes(e, attribute_name_values);
		//for(int i = 0;i < attribute_name_values.length;i+=2)
		//{
		//	e.setAttribute((String)attribute_name_values[i], attribute_name_values[i+1]);
		//}
		Date now = new Date();
		e.setAttribute(FIELD_CREATOR,creator);
		e.setAttribute(FIELD_DATE_CREATED,now);
		e.setAttribute(FIELD_LAST_MODIFIED,now);
		//e.setAttribute("reverse_last_modified",new Date(Long.MAX_VALUE-now.getTime()));
		return store.saveEntity(e);
	}
	
	public static void set_attributes(Entity e,Object[] attribute_name_values)
	{
		for(int i = 0;i < attribute_name_values.length;)
		{
			Object obj = attribute_name_values[i];			
			if(obj.getClass() == Object[].class)
			{
				set_attributes(e,(Object[])obj);
				i++;
			}
			else
			{
				e.setAttribute((String)attribute_name_values[i], attribute_name_values[i+1]);
				i+=2;
			}
		}
	}
	
	/*SAVE NEW*/
	public static Entity CREATE_ENTITY(PersistentStore store,Entity creator,Entity e) throws PersistenceException
	{			
		Date now = new Date();
		e.setAttribute(FIELD_CREATOR,creator);
		e.setAttribute(FIELD_DATE_CREATED,now);
		e.setAttribute(FIELD_LAST_MODIFIED,now);
		//e.setAttribute("reverse_last_modified",new Date(Long.MAX_VALUE-now.getTime()));
		return store.saveEntity(e);
	}
	

	public static Entity GET(PersistentStore store, String entity_type,long entity_id) throws PersistenceException,WebApplicationException
	{
		Entity e = store.getEntityById(entity_type, entity_id);
		if(e == null)
			throw new WebApplicationException(entity_type+" INSTANCE WITH ID "+entity_id+" DOES NOT EXIST IN STORE.");
		return e;	
	}
	
	public static Entity GET_AND_MASK(PersistentStore store,String entity_type,long entity_id,Object...masked_fieldnames) throws PersistenceException,WebApplicationException
	{
		Entity e = store.getEntityById(entity_type, entity_id);
		if(e == null)
			throw new PersistenceException(entity_type+" INSTANCE WITH ID "+entity_id+" DOES NOT EXIST IN STORE.");
		for(int i=0;i<masked_fieldnames.length;i++)
			e.getAttributes().remove(masked_fieldnames[i]);
		return e;	
	}

	public static Entity DELETE(PersistentStore store,String entity_type,long entity_id) throws PersistenceException,WebApplicationException
	{
		Entity e = GET(store,entity_type,entity_id);
		return DELETE(store,e);
	}
	
	public static Entity DELETE(PersistentStore store,Entity e) throws PersistenceException
	{
		store.deleteEntity(e);
		e.setId(Entity.UNDEFINED);
		return e;
	}
	
	public static Entity GET_REF(PersistentStore store,String entity_type,long entity_id, String fieldname) throws PersistenceException,WebApplicationException
	{
		Entity e = GET(store,entity_type,entity_id);
		return GET_REF(store,e,fieldname);
	}
	
	
	public static Entity GET_REF(PersistentStore store,Entity instance, String fieldname) throws PersistenceException
	{
		return FILL_REF(store, instance, fieldname);
	}

	public static List<Entity> GET_LIST_REF(PersistentStore store,String entity_type,long entity_id, String fieldname) throws PersistenceException,WebApplicationException
	{
		Entity e = GET(store,entity_type,entity_id);
		return GET_LIST_REF(store,e,fieldname);
	}
	
	public static List<Entity> GET_LIST_REF(PersistentStore store,Entity instance, String fieldname) throws PersistenceException,WebApplicationException
	{
		store.fillReferenceField(instance,fieldname);
		return (List<Entity>)instance.getAttribute(fieldname);
	}
	
	public static Entity FILL_REF(PersistentStore store,Entity instance, String fieldname) throws PersistenceException
	{
		store.fillReferenceField(instance,fieldname);
		return (Entity)instance.getAttribute(fieldname);
	}
	
	
	public static Entity FILL_REFS(PersistentStore store,Entity instance, String... fieldnames) throws PersistenceException
	{
		for(int i = 0;i < fieldnames.length;i++)
			FILL_REF(store, instance, fieldnames[i]);
		return instance;
	}
	
	public static Entity FILL_REFS(PersistentStore store,Entity instance) throws PersistenceException
	{
		store.fillReferenceFields(instance);
		return (Entity)instance;
	}
	
	public static Entity UPDATE(PersistentStore store,String entity_type,long entity_id,Object... name_value_pairs) throws PersistenceException,WebApplicationException
	{
		Entity e = GET(store,entity_type,entity_id);
		return UPDATE(store,e,name_value_pairs);
	}
	
	
	public static Entity UPDATE(PersistentStore store,Entity instance,Object... name_value_pairs) throws PersistenceException
	{
		if(instance == null)
			throw new PersistenceException("BAD UPDATE.CANNOT UPDATE A NULL ENTITY");
		
		set_attributes(instance, name_value_pairs);
		//for(int i = 0;i < name_value_pairs.length;i+=2)
		//	instance.setAttribute((String)name_value_pairs[i],name_value_pairs[i+1]);

		Date now = new Date();
		instance.setAttribute(FIELD_LAST_MODIFIED,now);		
		return store.saveEntity(instance);		
	}

	/*SAVE UPDATE */
	public static Entity SAVE_ENTITY(PersistentStore store,Entity instance) throws PersistenceException
	{
		if(instance == null)
			throw new PersistenceException("BAD UPDATE.CANNOT UPDATE A NULL ENTITY");

		Date now = new Date();
		instance.setAttribute(FIELD_LAST_MODIFIED,now);		
		return store.saveEntity(instance);		
	}

	
	public static QueryResult QUERY(PersistentStore store,Query q) throws PersistenceException
	{
		return store.executeQuery(q);
	}
	
	public static QueryResult QUERY_FILL(PersistentStore store,Query q) throws PersistenceException
	{
		QueryResult results =  QUERY(store,q);
		List<Entity> entities = results.getEntities();
		int s = results.size();
		for(int i = 0; i < s;i++)
		{
			Entity e = entities.get(i);
			FILL_REFS(store,e);
		}
		return results;
	}
	
	public static QueryResult QUERY_FILL(PersistentStore store,Query q,String... fill_fieldnames) throws PersistenceException
	{
		QueryResult results =  QUERY(store,q);
		List<Entity> entities = results.getEntities();
		int s = results.size();
		for(int i = 0; i < s;i++)
		{
			Entity e = entities.get(i);
			FILL_REFS(store,e,fill_fieldnames);
		}
		return results;
	}
	
	public static QueryResult QUERY_FILL_AND_MASK(PersistentStore store,Query q,String... mask_fields) throws PersistenceException
	{
		QueryResult results =  QUERY(store,q);
		List<Entity> entities = results.getEntities();
		int s = results.size();
		for(int i = 0; i < s;i++)
		{
			Entity e = entities.get(i);
			FILL_REFS(store,e);
			for(int ii=0;ii <mask_fields.length;ii+=2)
			{
				Entity ref = (Entity)e.getAttribute(mask_fields[ii]);
				if(ref != null)
					ref.getAttributes().put(mask_fields[ii+1],null);
			}
		}
		return results;
	}
	
	public static PagingQueryResult PAGING_QUERY(PersistentStore store,Query q) throws PersistenceException
	{
		QueryResult results = QUERY(store,q);
		int total_count 	= store.count(q);
		return new PagingQueryResult(results.getEntities(),total_count,q.getOffset(),q.getPageSize());
	}
	
	public static PagingQueryResult PAGING_QUERY_FILL(PersistentStore store,Query q) throws PersistenceException
	{
		QueryResult results = QUERY_FILL(store,q);
		int total_count = store.count(q);
		return new PagingQueryResult(results,total_count,q.getOffset(),q.getPageSize());
	}
	
	public static PagingQueryResult PAGING_QUERY_FILL(PersistentStore store,Query q,String... fill_fields) throws PersistenceException
	{
		QueryResult results = QUERY_FILL(store,q,fill_fields);
		int total_count = store.count(q);
		return new PagingQueryResult(results,total_count,q.getOffset(),q.getPageSize());
	}
	
	public static PagingQueryResult PAGING_QUERY_FILL_AND_MASK(PersistentStore store,Query q,String... args) throws PersistenceException
	{
		QueryResult results = QUERY_FILL_AND_MASK(store,q);
		int total_count = store.count(q);
		return new PagingQueryResult(results,total_count,q.getOffset(),q.getPageSize());
	}
	
	public static int COUNT(PersistentStore store,Query q) throws PersistenceException
	{
		return store.count(q);
	}

	public static Entity ID_TO_ENTITY(PersistentStore store,String entity_type,Long id) throws WebApplicationException,PersistenceException
	{
		Entity entity;
		entity = GET(store,entity_type,id);
		return entity;
	}


	public static List<Entity> IDS_TO_ENTITIES(PersistentStore store,String entity_type,List<Long> ids) throws WebApplicationException,PersistenceException
	{
		List<Entity> entities = null;		
		int s = ids.size();
		if(s > 0)
			entities = new ArrayList<Entity>();
		else
			return null;

		Entity entity;
		for(int i = 0;i < s;i++)
		{
			long id = ids.get(i);
			//this will throw a persistence exception if id doesnt exist
			entity = GET(store,entity_type,id);
			if(entities.contains(entity))
				continue;
			entities.add(entity);
		}
		
		return entities;		
	}
	
	public static List<Entity> INT_IDS_TO_ENTITIES(PersistentStore store,String entity_type,List<Integer> ids) throws WebApplicationException,PersistenceException
	{
		List<Entity> entities = null;		
		int s = ids.size(); 
		if(s > 0)
			entities = new ArrayList<Entity>();
		else
			return null;

		Entity entity;
		for(int i = 0;i < s;i++)
		{
			long id = ids.get(i);
			//this will throw a persistence exception if id doesnt exist
			entity = GET(store,entity_type,id);
			if(entities.contains(entity))
				continue;
			entities.add(entity);
		}
		
		return entities;		
	}

	/* convenience stuff for inheritors */
	
	public Entity NEW(String entity_type,Entity creator,Object ...attribute_name_values) throws PersistenceException
	{
		return NEW(store,entity_type,creator,attribute_name_values);
	}
	
	public Entity CREATE_ENTITY(Entity creator,Entity instance) throws PersistenceException
	{
		return CREATE_ENTITY(store,creator,instance);
	}
	

	public Entity GET( String entity_type,long entity_id) throws PersistenceException,WebApplicationException
	{
		return GET(store,entity_type,entity_id);
	}
	
	public Entity GET_AND_MASK(String entity_type,long entity_id,Object...masked_fieldnames) throws PersistenceException,WebApplicationException
	{
		return GET_AND_MASK(entity_type, entity_id, masked_fieldnames);
	}

	public Entity DELETE(String entity_type,long entity_id) throws PersistenceException,WebApplicationException
	{
		return DELETE(store,entity_type,entity_id);
	}
	
	public Entity DELETE(Entity e) throws PersistenceException
	{
		return DELETE(store,e);
	}
	
	public Entity GET_REF(String entity_type,long entity_id, String fieldname) throws PersistenceException,WebApplicationException
	{
		return GET_REF(store, entity_type, entity_id, fieldname);
	}
	
	public Entity GET_REF(Entity instance, String fieldname) throws PersistenceException
	{
		return GET_REF(store, instance, fieldname);
	}

	public List<Entity> GET_LIST_REF(String entity_type,long entity_id, String fieldname) throws PersistenceException,WebApplicationException
	{
		return GET_LIST_REF(store, entity_type, entity_id, fieldname);
	}
	
	public List<Entity> GET_LIST_REF(Entity instance, String fieldname) throws PersistenceException,WebApplicationException
	{
		return GET_LIST_REF(store, instance, fieldname);
	}
	
	public Entity FILL_REF(Entity instance, String fieldname) throws PersistenceException
	{
		return FILL_REF(store, instance, fieldname);
	}
	
	public  Entity FILL_REFS(Entity instance, String... fill_fields) throws PersistenceException
	{
		return FILL_REFS(store, instance, fill_fields);
	}
	
	public Entity FILL_REFS(Entity instance) throws PersistenceException
	{
		return FILL_REFS(store, instance);
	}
	
	public Entity UPDATE(String entity_type,long entity_id,Object... name_value_pairs) throws PersistenceException,WebApplicationException
	{
		return UPDATE(store,entity_type,entity_id,name_value_pairs);
	}
	
	
	public Entity UPDATE(Entity instance,Object... name_value_pairs) throws PersistenceException
	{
		return UPDATE(store, instance, name_value_pairs);
	}
	
	/*UPDATE for Entity*/
	public Entity SAVE_ENTITY(Entity instance) throws PersistenceException
	{
		return SAVE_ENTITY(store,instance);
	}
	
	public QueryResult QUERY(Query q) throws PersistenceException
	{
		return QUERY(store, q);
	}
	
	public QueryResult QUERY_FILL(Query q) throws PersistenceException
	{
		return QUERY_FILL(store, q);
	}
	
	public QueryResult QUERY_FILL(Query q,String... fill_fieldnames) throws PersistenceException
	{
		return QUERY_FILL(store,q, fill_fieldnames);
	}
	
	public QueryResult QUERY_FILL_AND_MASK(Query q,String... mask_fields) throws PersistenceException
	{
		return QUERY_FILL_AND_MASK(store, q, mask_fields);
	}
	
	public PagingQueryResult PAGING_QUERY(Query q) throws PersistenceException
	{
		return PAGING_QUERY(store,q);
	}
	
	public PagingQueryResult PAGING_QUERY_FILL(Query q) throws PersistenceException
	{
		return PAGING_QUERY_FILL(store, q);
	}
	
	public PagingQueryResult PAGING_QUERY_FILL(Query q,String... fill_fields) throws PersistenceException
	{
		return PAGING_QUERY_FILL(store, q, fill_fields);
	}
	
	public PagingQueryResult PAGING_QUERY_FILL_AND_MASK(Query q,String... mask_fields) throws PersistenceException
	{
		return PAGING_QUERY_FILL_AND_MASK(store, q, mask_fields);
	}
	
	public int COUNT(Query q) throws PersistenceException
	{
		return COUNT(store,q);
	}

	public Entity ID_TO_ENTITY(String entity_type,Long id) throws WebApplicationException,PersistenceException
	{
		return ID_TO_ENTITY(store, entity_type, id);
	}

	public List<Entity> IDS_TO_ENTITIES(String entity_type,List<Long> ids) throws WebApplicationException,PersistenceException
	{
		return IDS_TO_ENTITIES(store,entity_type, ids);
	}
	

	public List<Entity> INT_IDS_TO_ENTITIES(String entity_type,List<Integer> ids) throws WebApplicationException,PersistenceException
	{
		return INT_IDS_TO_ENTITIES(store,entity_type, ids);
	}
	
	//DDL helpers for WebStoreModule //
	
	
	public EntityDefinition DEFINE_ENTITY(String entity_name,Object...args) throws PersistenceException,SyncException
	{
		return DEFINE_ENTITY(store,entity_name,args);
	}
	
	public  void DEFINE_ENTITY_INDEX(String entity_name,String index_name,int index_type,String... field_names) throws PersistenceException,SyncException
	{
		DEFINE_ENTITY_INDEX(store,entity_name, index_name, index_type,field_names);	
	}
	
	
}
