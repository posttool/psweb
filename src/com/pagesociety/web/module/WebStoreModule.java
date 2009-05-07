package com.pagesociety.web.module;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Store;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityDefinition;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.EntityRelationshipDefinition;
import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.persistence.PersistenceAdapter;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.PersistentStore;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.persistence.IEvolutionProvider;
import com.pagesociety.web.module.persistence.IPersistenceProvider;

public class WebStoreModule extends WebModule
{

	public static final String SLOT_STORE 			    = "store";
	protected PersistentStore store;
	protected List<EntityDefinition> associated_entity_definitions;
	
	
	public void system_init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.system_init(app, config);		
		//TODO: we should have to do this any more//
		associated_entity_definitions = new ArrayList<EntityDefinition>();
		
	}

	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);		
		IPersistenceProvider store_provider = (IPersistenceProvider)getSlot(SLOT_STORE);
		//IN RARE CASES THE STORE PROVIDER SLOT IS NULL
		//MAINLY WHEN A DEFAULT GUARD IS BEING USED. WE MAKE
		//DEFAULT GUARDS EXTEND WEB STORE MODULE SO IT IS
		//EASY TO EXTEND THEM AND STILL HAVE STORE FUNCTIONALITY.
		//HOWEVER THE DEFAULT IMPLMENTATIONS NEVER HAVE THEIR STORE
		//SET SINCE THEY ALWAYS RETURN FALSE FOR EVERY METHOD
		if(store_provider == null)
			return;
		
		//collect entity defs for this web module via store adapter//
		schema_receiver r = get_schema_receiver_for_store(app,getName(),store_provider);
		store = r.getStore();
		r.setWebStoreModuleContext(this);

		try{
			defineEntities(config);
			defineIndexes(config);
			defineRelationships(config);
		}catch(Exception e)
		{
			ERROR(e);
			throw new InitializationException("FAILED SETTING UP "+getName()+" MODULE.");
		}
		r.setWebStoreModuleContext(null);
		store = store_provider.getStore();

	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_STORE, IPersistenceProvider.class, true);
	}	
	
	

	protected void defineEntities(Map<String,Object> config)throws PersistenceException,InitializationException
	{
		/* do nothing by default*/
	}
	
	protected void defineIndexes(Map<String,Object> config)throws PersistenceException,InitializationException
	{
		/*do nothing by default */
	}
	
	protected void defineRelationships(Map<String,Object> config)throws PersistenceException,InitializationException
	{
		/*do nothing by default */
	}
	
	public List<EntityDefinition> getAssociatedEntityDefinitions()
	{
		return associated_entity_definitions;
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
	
	
	public  EntityDefinition DEFINE_ENTITY(PersistentStore store,String entity_name,Object... fields) throws PersistenceException,SyncException
	{
		return DEFINE_ENTITY(store, entity_name,null, fields);
	}
	
	public  EntityDefinition DEFINE_ENTITY(PersistentStore store,String entity_name,List<FieldDefinition> defs,Object ...fields) throws PersistenceException,SyncException
	{
		if(defs == null)
			defs = new ArrayList<FieldDefinition>();
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
			
			defs.add(f);
			if(i>fields.length - 1)
				break;
		}
		return DEFINE_ENTITY(store, entity_name, defs);	
	}
	
	public  EntityDefinition DEFINE_ENTITY(PersistentStore store,String entity_name,List<FieldDefinition> defs) throws PersistenceException,SyncException
	{

		EntityDefinition proposed_def = new EntityDefinition(entity_name);
		/* add system fields to proposed def */

		for(int i = 0;i < FIELDS.length;i++)
			proposed_def.addField(FIELDS[i]);
		for(int i = 0;i < defs.size();i++)
			proposed_def.addField(defs.get(i));
		if(store.getEntityDefinition(entity_name) != null)
			return proposed_def;

		store.addEntityDefinition(proposed_def);
		return proposed_def;
	}
	
	public static EntityDefinition ADD_FIELDS(PersistentStore store,String entity_name,Object... fields) throws PersistenceException,InitializationException
	{
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
		
			store.addEntityField(entity_name, f);
			if(i>fields.length - 1)
				break;
		}
		return store.getEntityDefinition(entity_name);
	}
	

	
	public static void DEFINE_ENTITY_INDEX(PersistentStore store,String entity_name,String index_name,int index_type,String... field_names) throws PersistenceException,InitializationException
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
	}

	public static void DEFINE_ENTITY_RELATIONSHIP(PersistentStore store,String from_entity_name, String from_entity_field,int relationship_type, String to_entity_name, String to_entity_field) throws PersistenceException,SyncException
	{
		EntityRelationshipDefinition rel = new EntityRelationshipDefinition(from_entity_name,from_entity_field,relationship_type,to_entity_name,to_entity_field);
		if(store.getEntityRelationships().contains(rel))
			return;
		else
			store.addEntityRelationship(rel);
	}


	
	public static Entity NEW(PersistentStore store,String entity_type,Entity creator,Object ...attribute_name_values) throws PersistenceException
	{
		
		Entity e = store.getEntityDefinition(entity_type).createInstance();
		set_attributes(e, attribute_name_values);
		Date now = new Date();
		e.setAttribute(FIELD_CREATOR,creator);
		e.setAttribute(FIELD_DATE_CREATED,now);
		e.setAttribute(FIELD_LAST_MODIFIED,now);
		//e.setAttribute("reverse_last_modified",new Date(Long.MAX_VALUE-now.getTime()));
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			return store.saveEntity(e);
		else
			return store.saveEntity(tid,e);
	}
	
	public static Entity NEW(PersistentStore store,String entity_type,Entity creator,Map<String,Object> entity_atts) throws PersistenceException
	{
		
		Entity e = store.getEntityDefinition(entity_type).createInstance();
		Iterator<String> keys = entity_atts.keySet().iterator();
		while(keys.hasNext())
		{
			String key = keys.next();
			e.setAttribute(key, entity_atts.get(key));
		}
		Date now = new Date();
		e.setAttribute(FIELD_CREATOR,creator);
		e.setAttribute(FIELD_DATE_CREATED,now);
		e.setAttribute(FIELD_LAST_MODIFIED,now);
		//e.setAttribute("reverse_last_modified",new Date(Long.MAX_VALUE-now.getTime()));
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			return store.saveEntity(e);
		else
			return store.saveEntity(tid,e);
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
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			return store.saveEntity(e);
		else
			return store.saveEntity(tid,e);

	}
	
	public static Entity EXPAND(PersistentStore store, Entity e) throws PersistenceException
	{
		if(!e.isLightReference())
			return e;
		if(e == null)
			return null;
		return GET(store,e.getType(),e.getId());
	}
	
	public static Entity FORCE_EXPAND(PersistentStore store, Entity e) throws PersistenceException
	{
		if(e == null)
			return null;
		return GET(store,e.getType(),e.getId());
	}

	public static Entity GET(PersistentStore store, String entity_type,long entity_id) throws PersistenceException
	{
		Entity e;
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			e = store.getEntityById(entity_type, entity_id);
		else
			e = store.getEntityById(tid,entity_type, entity_id);

		if(e == null)
			throw new PersistenceException(entity_type+" INSTANCE WITH ID "+entity_id+" DOES NOT EXIST IN STORE.");
		return e;	
	}
	
	public static Entity GET_AND_MASK(PersistentStore store,String entity_type,long entity_id,Object...masked_fieldnames) throws PersistenceException,WebApplicationException
	{
		Entity e = store.getEntityById(entity_type, entity_id);
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			e = store.getEntityById(entity_type, entity_id);
		else
			e = store.getEntityById(tid,entity_type, entity_id);
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
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			store.deleteEntity(e);
		else
			store.deleteEntity(tid,e);
		return e;
	}
	
	public static Entity GET_REF(PersistentStore store,String entity_type,long entity_id, String fieldname) throws PersistenceException,WebApplicationException
	{
		Entity e = GET(store,entity_type,entity_id);
		return GET_REF(store,e,fieldname);
	}
	
	
	public static Entity GET_REF(PersistentStore store,Entity instance, String fieldname) throws PersistenceException
	{
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			store.fillReferenceField(instance,fieldname);
		else
			store.fillReferenceField(tid,instance, fieldname);
		return (Entity)instance.getAttribute(fieldname);
	}

	public static List<Entity> GET_LIST_REF(PersistentStore store,String entity_type,long entity_id, String fieldname) throws PersistenceException
	{
		Entity e = GET(store,entity_type,entity_id);
		return GET_LIST_REF(store,e,fieldname);
	}
	
	public static List<Entity> GET_LIST_REF(PersistentStore store,Entity instance, String fieldname) throws PersistenceException
	{
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			store.fillReferenceField(instance,fieldname);
		else
			store.fillReferenceField(tid,instance,fieldname);
		return (List<Entity>)instance.getAttribute(fieldname);
	}
	
	public static Entity FILL_REF(PersistentStore store,Entity instance, String fieldname) throws PersistenceException
	{
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			store.fillReferenceField(instance,fieldname);
		else
			store.fillReferenceField(tid,instance,fieldname);
		return (Entity)instance;
	}
	
	public static Entity FILL_REFS(PersistentStore store,Entity instance, String... fieldnames) throws PersistenceException
	{
		for(int i = 0;i < fieldnames.length;i++)
			FILL_REF(store, instance, fieldnames[i]);
		return instance;
	}
	
	
	public static Entity FILL_REFS(PersistentStore store,Entity instance) throws PersistenceException
	{
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			store.fillReferenceFields(instance);
		else
			store.fillReferenceFields(tid,instance);
		return (Entity)instance;
	}
	
	public static Entity UPDATE(PersistentStore store,String entity_type,long entity_id,Object... name_value_pairs) throws PersistenceException,WebApplicationException
	{
		Entity e = GET(store,entity_type,entity_id);
		return UPDATE(store,e,name_value_pairs);
	}
	
	public static Entity UPDATE(PersistentStore store,Entity instance,Map<String,Object> entity_atts) throws PersistenceException
	{
		Entity e = instance;
		Iterator<String> keys = entity_atts.keySet().iterator();
		while(keys.hasNext())
		{
			String key = keys.next();
			e.setAttribute(key, entity_atts.get(key));
		}
		Date now = new Date();
		e.setAttribute(FIELD_LAST_MODIFIED,now);
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			return store.saveEntity(e);	
		else
			return store.saveEntity(tid,e);	
	}

	public static Entity UPDATE(PersistentStore store,Entity instance,Object... name_value_pairs) throws PersistenceException
	{
		if(instance == null)
			throw new PersistenceException("BAD UPDATE.CANNOT UPDATE A NULL ENTITY");

		set_attributes(instance, name_value_pairs);
		Date now = new Date();
		instance.setAttribute(FIELD_LAST_MODIFIED,now);
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			return store.saveEntity(instance);	
		else
			return store.saveEntity(tid,instance);	
	
	}

	/*SAVE UPDATE */
	public static Entity SAVE_ENTITY(PersistentStore store,Entity instance) throws PersistenceException
	{
		if(instance == null)
			throw new PersistenceException("BAD UPDATE.CANNOT UPDATE A NULL ENTITY");

		Date now = new Date();
		instance.setAttribute(FIELD_LAST_MODIFIED,now);		
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			return store.saveEntity(instance);	
		else
			return store.saveEntity(tid,instance);	
	}

	
	public static QueryResult QUERY(PersistentStore store,Query q) throws PersistenceException
	{
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			return store.executeQuery(q);
		else
			return store.executeQuery(tid,q);
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
		Integer tid = CURRENT_TRANSACTION_ID();
		int total_count = 0;
		if(tid == null)
			total_count 	= store.count(q);
		else
			total_count 	= store.count(tid,q);
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
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			return store.count(q);
		else
			return store.count(tid,q);
	}

	public static Entity ID_TO_ENTITY(PersistentStore store,String entity_type,Long id) throws WebApplicationException,PersistenceException
	{
		Entity entity;
		entity = GET(store,entity_type,id);
		return entity;
	}


	public static List<Entity> IDS_TO_ENTITIES(PersistentStore store,String entity_type,List<Long> ids) throws WebApplicationException,PersistenceException
	{

		if(ids == null)
			return null;

		List<Entity> entities = new ArrayList<Entity>();
		int s = ids.size();
		Entity entity;
		for(int i = 0;i < s;i++)
		{
			long id = ids.get(i);
			//this will throw a persistence exception if id doesnt exist
//			if(id == 0)
//				entity = null;
//			else
//				entity = GET(store,entity_type,id);
// FIXME try to understand why this happens a bit more
// so that the cms never asks for non existant entities
// this fails when i ask for previews from the resource module
			try {
				entity = GET(store,entity_type,id);
			} catch (PersistenceException e) {
				continue;
			}
			if(entities.contains(entity))
				continue;
			entities.add(entity);
		}
		
		return entities;		
	}
	
	public static List<Entity> INT_IDS_TO_ENTITIES(PersistentStore store,String entity_type,List<Integer> ids) throws WebApplicationException,PersistenceException
	{
		
		if(ids == null)
			return null;
		
		List<Entity> entities = new ArrayList<Entity>();
		int s = ids.size();
		Entity entity;
		for(int i = 0;i < s;i++)
		{
			long id = ids.get(i);
			//this will throw a persistence exception if id doesnt exist
			if(id == 0)
				entity = null;
			else
				entity = GET(store,entity_type,id);

			if(entities.contains(entity))
				continue;
			entities.add(entity);
		}
		
		return entities;		
	}

	public static void CREATE_QUEUE(PersistentStore store,String queuename,int rec_size,int recs_per_extent) throws PersistenceException
	{
		List<String> app_queues = store.listQueues();
		if(app_queues.contains(queuename))
			return;
		store.createQueue(queuename,rec_size,recs_per_extent);	
	}
	
	public static void DELETE_QUEUE(PersistentStore store,String queuename) throws PersistenceException
	{
		List<String> app_queues = store.listQueues();
		if(app_queues.contains(queuename))
			return;
		store.deleteQueue(queuename);
		
	}
	
	/* convenience stuff for inheritors */
	
	public Entity NEW(String entity_type,Entity creator,Map<String,Object> atts) throws PersistenceException
	{
		return NEW(store,entity_type,creator,atts);
	}
	public Entity NEW(String entity_type,Entity creator,Object ...attribute_name_values) throws PersistenceException
	{
		return NEW(store,entity_type,creator,attribute_name_values);
	}
	
	public Entity CREATE_ENTITY(Entity creator,Entity instance) throws PersistenceException
	{
		return CREATE_ENTITY(store,creator,instance);
	}
	
	public Entity EXPAND(Entity e) throws PersistenceException
	{
		return EXPAND(store,e);
	}
	
	public Entity FORCE_EXPAND(Entity e) throws PersistenceException
	{
		return FORCE_EXPAND(store,e);
	}
	
	public Entity GET( String entity_type,long entity_id) throws PersistenceException
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
	
	public List<Entity> GET_LIST_REF(Entity instance, String fieldname) throws PersistenceException
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
	
	
	public Entity UPDATE(Entity instance,Map<String,Object> entity_data) throws PersistenceException
	{
		return UPDATE(store, instance, entity_data);
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
	
	public List<Long> ENTITIES_TO_IDS(List<Entity> entities) throws WebApplicationException,PersistenceException
	{
		if(entities == null)
			return null;
		
		int s = entities.size();
		List<Long> ids = new ArrayList<Long>(s);
		for(int i = 0;i < s;i++)
			ids.add(entities.get(i).getId());
		return ids;
	}
	

	public List<Entity> INT_IDS_TO_ENTITIES(String entity_type,List<Integer> ids) throws WebApplicationException,PersistenceException
	{
		return INT_IDS_TO_ENTITIES(store,entity_type, ids);
	}
	

	public void VALIDATE_TYPE(String type,Entity... instances) throws WebApplicationException
	{
		for(int i = 0;i < instances.length;i++)
		{
			Entity instance = instances[i];
			if(instance == null)
				continue;
			if(!instance.getType().equals(type))
				throw new WebApplicationException("ENTITY ARGUMENT FAILED VALIDATION. EXPECTED TYPE "+type+" BUT ENTITY WAS OF TYPE "+instance.getType());
		}
	}
	
	//validate all in list are of type type
	public void VALIDATE_TYPE_LIST(String type,List<Entity> entities) throws WebApplicationException
	{
		if(entities == null)
			return;
		
		int s = entities.size();
		for(int i = 0;i < s;i++)
			VALIDATE_TYPE(type, entities.get(i));

	}
	//DDL helpers for WebStoreModule //
	public void VALIDATE_NEW_INSTANCE(Entity e) throws WebApplicationException
	{
		if(e.getId() != Entity.UNDEFINED)
			throw new WebApplicationException("TRYING TO CREATE AN ALREADY INITIALIZED ENTITY. "+e.getType()+" ALREADY HAS ID OF "+e.getId());
	}
	
	public void VALIDATE_EXISTING_INSTANCE(Entity e) throws WebApplicationException
	{
		if(e.getId() == Entity.UNDEFINED)
			throw new WebApplicationException("TRYING TO UPDATE AN UNITINIALIZED ENTITY. "+e.getType()+" HAS ID OF "+e.getId());
	}
	
	public EntityDefinition DEFINE_ENTITY(String entity_name,List<FieldDefinition> field_defs,Object...args) throws PersistenceException,InitializationException
	{
		EntityDefinition d = DEFINE_ENTITY(store,entity_name,field_defs,args);
		associated_entity_definitions.add(d);
		return d;
	}
	
	public EntityDefinition DEFINE_ENTITY(String entity_name,List<FieldDefinition> field_defs) throws PersistenceException,InitializationException
	{
		EntityDefinition d = DEFINE_ENTITY(store,entity_name,field_defs);
		associated_entity_definitions.add(d);
		return d;
	}
	public EntityDefinition DEFINE_ENTITY(String entity_name,Object...args) throws PersistenceException,InitializationException
	{
		EntityDefinition d = DEFINE_ENTITY(store,entity_name,args);
		associated_entity_definitions.add(d);
		return d;
	}
	
	public EntityDefinition ADD_FIELDS(String entity_name,Object...args) throws PersistenceException,InitializationException
	{
		EntityDefinition d = ADD_FIELDS(store,entity_name,args); 
		associated_entity_definitions.add(d);
		return d;
	}
	
	public  void DEFINE_ENTITY_INDEX(String entity_name,String index_name,int index_type,String... field_names) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY_INDEX(store,entity_name, index_name, index_type,field_names);	
	}
	
	public  void DEFINE_ENTITY_RELATIONSHIP(String from_entity_name,String from_entity_field,int relationship_type,String to_entity_name,String to_entity_field) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY_RELATIONSHIP(store,from_entity_name,from_entity_field,relationship_type,to_entity_name,to_entity_field);
	}


	/* be careful with this if you have 1 to many relationships*/
	public static Entity CLONE_SHALLOW(PersistentStore store,Entity e) throws PersistenceException
	{
		return store.saveEntity(e.cloneShallow());
	}
	
	public Entity CLONE_SHALLOW(Entity e) throws PersistenceException
	{
		return CLONE_SHALLOW(store,e);
	}
	
	
	//clone subsystem/////////
	
	public Entity CLONE_DEEP(Entity e) throws PersistenceException
	{
		clone_policy default_clone_policy = new clone_policy();
		return CLONE_DEEP(e,default_clone_policy);
		
	}
	
	public Entity CLONE_DEEP(Entity e,clone_policy p) throws PersistenceException
	{
		Entity clone = CLONE_DEEP(store,e,p);
		return clone;
	}
	

	public  static final int CLONE_REFERENCE  	= 0x01;
	public  static final int LINK_REFERENCE 	= 0x02;
	public  static final int NULLIFY_REFERENCE	= 0x03;
	public  static final int USE_AS_REFERENCE 	= 0x04;

	public class clone_policy
	{		
		public int exec(Entity e,String fieldname,Entity reference_val)
		{
			if(fieldname.equals(FIELD_CREATOR))
				return LINK_REFERENCE;
			else
				return CLONE_REFERENCE;			
		}
	}

	public static Entity CLONE_DEEP(PersistentStore store,Entity e,clone_policy f) throws PersistenceException
	{
		if(e == null)
			return null;
		e = EXPAND(store, e);

		Entity clone = e.cloneShallow();
		List<FieldDefinition> ref_fields = store.getEntityDefinition(e.getType()).getReferenceFields();
		for(int i = 0;i < ref_fields.size();i++)
		{
			FieldDefinition ref_field 	 = ref_fields.get(i);
			String ref_fieldname 		 = ref_field.getName();
			if(ref_field.isArray())
			{
				List<Entity> vals = GET_LIST_REF(store,e,ref_fieldname);
				if(vals == null)
					continue;
				
				int s = vals.size();
				List<Entity> clone_vals = new ArrayList<Entity>(s);
				for(int j = 0;j <s;j++)
				{
					Entity val = vals.get(j);
					int clone_behavior = f.exec(e, ref_fieldname, val);
					switch(clone_behavior)
					{
						case CLONE_REFERENCE:
							clone_vals.add(CLONE_DEEP(store,val,f));
							break;
						case LINK_REFERENCE:
							clone_vals.add(val);
							break;
						case NULLIFY_REFERENCE:
							clone_vals.add(null);
							break;
					}
				}
				clone.setAttribute(ref_fieldname, clone_vals);
			}
			else
			{
				
				Entity val = GET_REF(store,e,ref_fieldname);
				int clone_behavior = f.exec(e, ref_fieldname, val);
				switch(clone_behavior)
				{
					case CLONE_REFERENCE:
						clone.setAttribute(ref_fieldname,CLONE_DEEP(store,val,f));
						break;
					case LINK_REFERENCE:
						clone.setAttribute(ref_fieldname,val);
						break;
					case NULLIFY_REFERENCE:
						clone.setAttribute(ref_fieldname,null);
						break;
				}
			}	
		}

		return CREATE_ENTITY(store,(Entity)clone.getAttribute(FIELD_CREATOR),clone);
	}
	
	
	//DELETE DEEP SUBSYSTEM...be careful with circular references!!!//
	public Entity DELETE_DEEP(Entity e) throws PersistenceException
	{
		delete_policy default_delete_policy = new delete_policy();
		return DELETE_DEEP(e, default_delete_policy);
	}
	
	public  Entity DELETE_DEEP(Entity e,delete_policy f) throws PersistenceException
	{
		return DELETE_DEEP(store,e, f);
	}
	
	public  static final int DELETE_REFERENCE  		= 0x01;
	public  static final int DONT_DELETE_REFERENCE 	= 0x02;

	public class delete_policy
	{	
		String[] protect_fields = new String[0];
		public delete_policy(String... protect_fields)
		{
			this.protect_fields = protect_fields;
		}
		
		public int exec(Entity e,String fieldname,Entity reference_val) throws Exception
		{
			if(fieldname.equals(FIELD_CREATOR))
				return DONT_DELETE_REFERENCE;
			for(int i = 0; i< protect_fields.length;i++)
				if(protect_fields[i].equals(fieldname))
					return DONT_DELETE_REFERENCE;
			return DELETE_REFERENCE;

		}
	}

	public static Entity DELETE_DEEP(PersistentStore store,Entity e,delete_policy f) throws PersistenceException
	{
		if(e == null)
			return null;

		try{
			List<FieldDefinition> ref_fields = store.getEntityDefinition(e.getType()).getReferenceFields();
			for(int i = 0;i < ref_fields.size();i++)
			{
				FieldDefinition ref_field 	 = ref_fields.get(i);
				String ref_fieldname 		 = ref_field.getName();
				if(ref_field.isArray())
				{
					List<Entity> vals = (List<Entity>)e.getAttribute(ref_fieldname);
					if(vals == null)
						continue;
					int s = vals.size();
					for(int j = 0;j <s;j++)
					{
						Entity val = vals.get(i);
						int delete_behavior = f.exec(e, ref_fieldname, val);
						switch(delete_behavior)
						{
							case DELETE_REFERENCE:
								DELETE_DEEP(store,val,f);
							case DONT_DELETE_REFERENCE:
								break;
						}
					}
				}
				else
				{				
					Entity val = (Entity)e.getAttribute(ref_fieldname);
					int delete_behavior = f.exec(e, ref_fieldname, val);
					switch(delete_behavior)
					{
						case DELETE_REFERENCE:
							DELETE_DEEP(store,val,f);
						case DONT_DELETE_REFERENCE:
							break;
					}
				}	
			}
	
			Entity dd=null;
			try{
				dd =  DELETE(store,e);
			}catch(PersistenceException pe)
			{
				if(pe.getErrorCode() == PersistenceException.ENTITY_DOES_NOT_EXIST)
					return e;
			}
			return dd;
		}catch(Exception ee)
		{
			ee.printStackTrace();
			throw new PersistenceException("BARFED IN DELETE DEEP "+ee.getMessage());
		}
	}

	
	//maybe a usefule util to move up? lets see
	protected List<FieldDefinition> UNFLATTEN_FIELD_DEFINITIONS(Object... flat_defs)
	{
		return UNFLATTEN_FIELD_DEFINITIONS(FROM_OBJECT_LIST,flat_defs);
	}
	
	protected static final int FROM_OBJECT_LIST = 0x01;
	protected static final int FROM_STRING_LIST = 0x02;
	protected List<FieldDefinition> UNFLATTEN_FIELD_DEFINITIONS(int from,Object... flat_defs)
	{
		String field_name;
		int field_type;
		String ref_type;
		Object default_val;
		List<FieldDefinition> ret = new ArrayList<FieldDefinition>();
		for(int i = 0;i < flat_defs.length;i+=3)
		{
			field_name  = (String)flat_defs[i];
			if(from == FROM_STRING_LIST)
			{
				try{
					field_type  =  Types.parseType((String)flat_defs[i+1]);
				}catch(PersistenceException pe)
				{
					field_type = Types.TYPE_UNDEFINED;
					ERROR(pe);
				}
			}else
			{
				field_type  =  (Integer)flat_defs[i+1];
			}
			
			FieldDefinition f;
			if(field_type == Types.TYPE_REFERENCE)
			{
				ref_type = (String)flat_defs[i+2];
				f = new FieldDefinition(field_name,field_type,ref_type);
				if(from == FROM_STRING_LIST)
				{
					try{
						default_val = Types.parseDefaultValue(field_type, (String)flat_defs[i+3]);
					}catch(PersistenceException pe)
					{
						default_val = null;
						ERROR(pe);
					}
				}
				else
					default_val = flat_defs[i+3];
				
				f.setReferenceType(ref_type);
				f.setDefaultValue(default_val);
				i++;
			}
			else
			{
				f = new FieldDefinition(field_name,field_type);
				if(from == FROM_STRING_LIST)
				{
					try{
						default_val = Types.parseDefaultValue(field_type, (String)flat_defs[i+2]);
					}catch(PersistenceException pe)
					{
						default_val = null;
						ERROR(pe);
					}
				}
				else
					default_val = flat_defs[i+2];
			
				f.setDefaultValue(default_val);
			}
			ret.add(f);
		}
		return ret;
	}

	
	public List<EntityIndex> DEFINE_ENTITY_INDICES(String entity_name,entity_index_descriptor... indexes) throws PersistenceException,InitializationException
	{
		return DEFINE_ENTITY_INDICES(store, entity_name, indexes);
	}
	
	public List<EntityIndex> DEFINE_ENTITY_INDICES(PersistentStore store,String entity_name,entity_index_descriptor... proposed_indexes) throws PersistenceException,InitializationException
	{
		EntityDefinition def 				= store.getEntityDefinition(entity_name);
		List<EntityIndex> proposed_indices 	= translate_proposed_indices(def, proposed_indexes);
		for(int i=0;i < proposed_indices.size();i++)
		{
			EntityIndex pro_idx = proposed_indices.get(i);			
			do_define_entity_index(store, entity_name, pro_idx);
		}
		return proposed_indices;
	}
	
	
	private static List<EntityIndex> translate_proposed_indices(EntityDefinition def, entity_index_descriptor[] proposed_indexes) throws InitializationException
	{
		List<EntityIndex> ret = new ArrayList<EntityIndex>();
		for(int i = 0;i < proposed_indexes.length;i++)
		{
			EntityIndex idx 	  = new EntityIndex(proposed_indexes[i].index_name,proposed_indexes[i].index_type);
			idx.setEntity(def.getName());
			String[] index_fields = proposed_indexes[i].field_names;
			for(int j=0;j < index_fields.length;j++)
			{
				FieldDefinition f;
				f = def.getField(index_fields[j]);
				if(f==null)
					throw new InitializationException("BAD FIELDNAME IN ENTITY INDEX DECL: "+proposed_indexes[i].index_name+" FIELD: "+index_fields[j]+" DOES NOT EXIST IN "+def.getName());
				idx.addField(f);
			}
			idx.setAttributes(proposed_indexes[i].attributes);
			ret.add(idx);
		}
		return ret;
	}

	private static EntityIndex do_define_entity_index(PersistentStore store,String entity_name,EntityIndex index) throws PersistenceException,InitializationException
	{

		String index_name    = index.getName();
		int index_type       = index.getType();
		Map<String,Object>	index_atts  = index.getAttributes();
		String[] field_names = new String[index.getFields().size()];
		for(int i=0;i < index.getFields().size();i++)
			field_names[i] = index.getFields().get(i).getName();
	
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
				return idx;//the index exists and it checked out
			}
		}
		//the index didnt exist..create it 
		return store.addEntityIndex(entity_name, field_names, index_type, index_name,index_atts );	
	}
	
	public class entity_index_descriptor
	{
		String index_name;
		int index_type;
		String[] field_names;
		Map<String,Object> attributes;
	}
	
	public entity_index_descriptor ENTITY_INDEX(String index_name,int index_type,String... field_names)
	{
		entity_index_descriptor d = new entity_index_descriptor();
		d.index_name = index_name;
		d.index_type = index_type;
		d.field_names = field_names;
		d.attributes = null;
		return d;
	}

	public entity_index_descriptor ENTITY_INDEX(String index_name,int index_type,Map<String,Object> attributes,String... field_names)
	{
		entity_index_descriptor d = new entity_index_descriptor();
		d.index_name = index_name;
		d.index_type = index_type;
		d.field_names = field_names;
		d.attributes = attributes;
		//System.out.println("ATTRIBUTES ARE "+d.attributes+" FOR "+index_name);
		return d;	
	}
	
	/////////SCHEMA//////////////

	//each persistent store has its own schema.
	private static  Map<String,schema_receiver> schema_map;
	private static  List<schema_receiver> schema_map_as_list;
	public static void web_store_subsystem_init_start(WebApplication app)
	{
		schema_map = new HashMap<String,schema_receiver>();	
		schema_map_as_list = new ArrayList<schema_receiver>();
	}

	public static void web_store_subsystem_init_complete(WebApplication app) throws PersistenceException,InitializationException
	{
		for(int i = 0;i < schema_map_as_list.size();i++)
		{
			schema_receiver s = schema_map_as_list.get(i);
			s.actualize();
		}
	}
	

	public static schema_receiver get_schema_receiver_for_store(WebApplication app,String module_name,IPersistenceProvider p)
	{
		if(p == null)
			return null;
		schema_receiver r = schema_map.get(p.getName());
		if(r == null)
		{
			r = new WebStoreModule().new schema_receiver(app,p);
			schema_map.put(p.getName(),r);
			schema_map_as_list.add(r);
		}

		return r;
	}
	
	public class schema_receiver extends PersistenceAdapter 
	{
		
		WebApplication app;
		List<EntityDefinition> 				entity_definitions;
		List<String>						entity_definition_declarers;

		//we are using entity def here just because it is a name and a collection of fields//
		List<EntityDefinition> 				entity_fields;
		List<String>						entity_field_declarers;
		
		List<EntityIndex> 	   				entity_indices;
		List<String>						entity_index_declarers;
		
		List<EntityRelationshipDefinition> 	entity_relationships;
		List<String>						entity_relationship_declarers;
				
		IPersistenceProvider				p;

		
		private WebStoreModule webstore_context;
		private HashMap<String, String> entity_to_declarer;
		private HashMap<String, String> entity_index_to_declarer;
		private HashMap<String, String> entity_fieldname_to_declarer;

		boolean made_backup_during_evolution = false;
		public schema_receiver(WebApplication app,IPersistenceProvider p)
		{
			this.app = app;
			this.entity_definitions 		 	 = new ArrayList<EntityDefinition>();
			this.entity_definition_declarers 	 = new ArrayList<String>();
			
			this.entity_fields 		 	 	 	 = new ArrayList<EntityDefinition>();
			this.entity_field_declarers 		 = new ArrayList<String>();			
			
			this.entity_indices 			 	 = new ArrayList<EntityIndex>();
			this.entity_index_declarers 	 	 = new ArrayList<String>();
			
			this.entity_relationships 		 	 = new ArrayList<EntityRelationshipDefinition>();
			this.entity_relationship_declarers 	 = new ArrayList<String>();
			
	
			this.p 		 					 	 = p;
		}
			
		public String getDeclaringModuleForIndex(String entity_name,String index_name)
		{
			return entity_index_to_declarer.get(entity_name+"."+index_name);
		}
		
		public String getDeclaringModuleForEntity(String entity_name)
		{
			return entity_to_declarer.get(entity_name);
		}
		
		public String getDeclaringModuleForEntityField(String entity_name,String fieldname)
		{
			return entity_fieldname_to_declarer.get(entity_name+"."+fieldname);
		}
		
		public String getDeclaringModuleForEntityRelationship(String entity_name,String fieldname)
		{
			//TODO: UNIMPLEMENTED //
			return null;
		}
		
		public void actualize() throws PersistenceException,InitializationException
		{
			actualize_entity_defs();
			actualize_entity_indices();
			actualize_entity_relationships();
		} 
		
		private void actualize_entity_defs() throws PersistenceException,InitializationException
		{
			Map<String,EntityDefinition>  entity_to_def = new LinkedHashMap<String, EntityDefinition>();
			entity_to_declarer = new HashMap<String,String>();
			entity_fieldname_to_declarer = new HashMap<String,String>();
			for(int i = 0;i < entity_definitions.size();i++)
			{
				EntityDefinition d = entity_definitions.get(i);
				String entity_name = d.getName();
				String declarer = entity_definition_declarers.get(i);
				entity_to_def.put(entity_name, d);
				entity_to_declarer.put(entity_name,declarer);
				for(int j=0;j < d.getFields().size();j++)
					entity_fieldname_to_declarer.put(entity_name+"."+d.getFields().get(j).getName(),declarer);
			}
			
			//entity fields in this case is a list of entity defs with one field and the entity name//
			//we use entity def becuase field definition does not have an entity name
			for(int i =0;i < entity_fields.size();i++)
			{
				EntityDefinition ff 		  = entity_fields.get(i);
				String declarer 			  = entity_field_declarers.get(i);	
				String entity_name 			  = ff.getName();
				FieldDefinition f 			  = ff.getFields().get(1);//1 here because field 0 is always id
				EntityDefinition proposed_def = entity_to_def.get(entity_name);
				if(proposed_def == null)
					throw new PersistenceException("ENTITY "+entity_name+" DOES NOT EXIST."+declarer+" CANNOT ADD FIELD "+ff.getFields().get(1).getName());
				entity_fieldname_to_declarer.put(proposed_def.getName()+"."+f.getName(),declarer);
				proposed_def.addField(f);
			}
			for(int i = 0;i < entity_definitions.size();i++)
			{
				EntityDefinition proposed_def = entity_definitions.get(i);
				app.INFO("WEBSTORE SUBSYSTEM ACTUALIZING ENTITY "+entity_to_declarer.get(proposed_def.getName())+"."+proposed_def.getName()+" PERSISTENCE PROVIDER IS "+p.getName());
				EVOLVE_ENTITY(this, entity_definitions.get(i));
			}
		}
		
		private void actualize_entity_indices() throws PersistenceException,InitializationException
		{

			if(entity_indices.size() == 0)
				return;
			
			Map<String,List<EntityIndex>> entity_to_indices 		= new LinkedHashMap<String, List<EntityIndex>>();
			entity_index_to_declarer 	= new HashMap<String,String>();
			
			//entity fields in this case is a list of entity defs with one field and the entity name//
			//we use entity def becuase field definition does not have an entity name
			for(int i =0;i < entity_indices.size();i++)
			{
				EntityIndex idx 		      	= entity_indices.get(i);
				String declarer 			  	= entity_index_declarers.get(i);	
				String entity_name 			  	= idx.getEntity();
				List<EntityIndex> proposed_idxs = entity_to_indices.get(entity_name);
				if(proposed_idxs == null)
				{
					proposed_idxs = new ArrayList<EntityIndex>();
					entity_to_indices.put(entity_name, proposed_idxs);
				}
				entity_index_to_declarer.put(entity_name+"."+idx.getName(),declarer);
				proposed_idxs.add(idx);
			}				
			
			Iterator<String> iter = entity_to_indices.keySet().iterator();
			while(iter.hasNext())
			{
				String entity_name = iter.next();
				List<EntityIndex> proposed_idxs = entity_to_indices.get(entity_name);
				app.INFO("WEBSTORE SUBSYSTEM ACTUALIZING ENTITY INDEXES FOR "+entity_name+" PERSISTENCE PROVIDER IS "+p.getName());
				EVOLVE_ENTITY_INDICES(this,entity_name, proposed_idxs);
			}

		}
			
		private void actualize_entity_relationships() throws PersistenceException,InitializationException
		{
			for(int i =0;i < entity_relationships.size();i++)
			{
				EntityRelationshipDefinition erd = entity_relationships.get(i);
				app.INFO("WEBSTORE SUBSYSTEM ACTUALIZING ENTITY RELATIONSHIP FOR "+entity_relationship_declarers.get(i)+":\n"+erd);
				List<EntityRelationshipDefinition> lr = p.getStore().getEntityRelationships();
				if(lr.contains(erd))
					return;
				p.getStore().addEntityRelationship(erd);
			}
		}
		
		//////
		public void addEntityDefinition(EntityDefinition entity_def) throws PersistenceException
		{
			//System.out.println(webstore_context.getName()+" IS ADDING ENTITY "+entity_def.getName());
			entity_definitions.add(entity_def);
			entity_definition_declarers.add(webstore_context.getName());
		}
		
		public int addEntityField(String entity, FieldDefinition entity_field_def) throws PersistenceException
		{
			EntityDefinition def = new EntityDefinition(entity);
			
			def.addField(entity_field_def);
			entity_fields.add(def);
			entity_field_declarers.add(webstore_context.getName());
			
			//System.out.println(webstore_context.getName()+"ADDING ENTITY FIELD "+def);
			//System.out.println("EXISTING DEF IS "+getEntityDefinition(entity));
			
			return 0;
		}
		
		public EntityIndex addEntityIndex(String entity, String field_name,int index_type, String index_name, Map<String, Object> attributes)throws PersistenceException
		{
			return addEntityIndex(entity, new String[]{field_name}, index_type, index_name, attributes);
		}
		
		public EntityIndex addEntityIndex(String entity, String[] field_names,int index_type, String index_name, Map<String, Object> attributes)throws PersistenceException
		{
			EntityIndex idx = new EntityIndex(index_name,index_type);
			idx.setAttributes(attributes);
			idx.setEntity(entity);

			for(int i = 0;i < field_names.length;i++)
			{
				//TODO: damn deep indexes//
				FieldDefinition f;
				f = get_field(entity,field_names[i]);
				if(f == null)
					throw new PersistenceException(webstore_context+" IS TRYING TO ADD AN INDEX "+idx.getName()+" WITH A BAD FIELD NAME "+field_names[i]);

				idx.addField(f);
			}
			entity_indices.add(idx);
			entity_index_declarers.add(webstore_context.getName());
			return idx;
		}
		
		private FieldDefinition get_field(String entity_name,String field_name)
		{
			EntityDefinition d = getEntityDefinition(entity_name);
			FieldDefinition f = d.getField(field_name);
			return f;
		}
		
		public void addEntityRelationship(EntityRelationshipDefinition r)throws PersistenceException
		{
			entity_relationships.add(r);
			entity_relationship_declarers.add(webstore_context.getName());
		}
		
		public EntityDefinition getEntityDefinition(String name)
		{
			for(int i = 0;i < entity_definitions.size();i++)
			{
				EntityDefinition d = entity_definitions.get(i);
				if(d.getName().equals(name))
				{
					//here we add any proposed fields to the def.
					//this is so a module can add a field and an
					//index at the same time.
					for(int j = 0;j < entity_fields.size();j++)
					{
						EntityDefinition field_declaration = entity_fields.get(j);
						if(field_declaration.getName().equals(d.getName()))
							d.addField(field_declaration.getFields().get(1));
					}
					return d;
			
				
				}
			}
			return null;
		}
		
		public List<EntityDefinition> getEntityDefinitions()
		{
			return entity_definitions;
		}
		
		public List<EntityIndex> getEntityIndices(String entity_name)
		{
			List<EntityIndex> ret = new ArrayList<EntityIndex>();
			for(int i = 0;i < entity_indices.size();i++)
			{
				EntityIndex idx = entity_indices.get(i);
				if(idx.getEntity().equals(entity_name))
					ret.add(idx);
			}
			return ret;
		}
		
		public List<EntityRelationshipDefinition> getEntityRelationships()
		{
			return entity_relationships;
		}
		
		//IPersistenceProvider
		public PersistentStore getStore()
		{ 
			return this;
		}
		
		public String getName()
		{ 
			return "schema receiver";
		}

		public IEvolutionProvider getEvolutionProvider() 
		{
			return null;
		}
	
		public void setWebStoreModuleContext(WebStoreModule c)
		{
			webstore_context = c;
		}

	}

	public static EntityDefinition EVOLVE_ENTITY(schema_receiver resolver,EntityDefinition proposed_def) throws PersistenceException,InitializationException
	{
		IPersistenceProvider p				   = resolver.p;
		PersistentStore 	store 			   = p.getStore();
		IEvolutionProvider 	evolution_provider = p.getEvolutionProvider();
		String entity_name = proposed_def.getName();
		EntityDefinition 		existing_def;			
		existing_def = store.getEntityDefinition(entity_name);

		/*create it if it doesnt exist*/
		if(existing_def == null)
		{
			store.addEntityDefinition(proposed_def);
		}
		else
		{
			/* check it to make sure the version passed in matches the one in the store */
			
			if(existing_def.equals(proposed_def))
				return proposed_def;
			else
			{
				if(!resolver.made_backup_during_evolution)
				{
					boolean make_backup = confirm("ABOUT TO DROP INTO ENTITY EVOLUTION. WOULD YOU LIKE TO MAKE A COMPLETE BACKUP OF THE DATABASE?");
					if(make_backup)
					{
						String token = p.doFullBackup();
						System.out.println("DID FULL BACKUP: "+token);
						resolver.made_backup_during_evolution = true;
					}
				}
				evolution_provider.evolveEntity(resolver, existing_def, proposed_def);
			}
			/* add any additional default system fields...deletes of system fields 
			 * have to be done with an alter for now...also need to take evolution into 
			 * account when adding system fields */
			for(int i = 0;i < FIELDS.length;i++)
			{
				FieldDefinition f = FIELDS[i];
				if(existing_def.getField(f.getName())==null)
					store.addEntityField(entity_name,f);
			}
		}

		return proposed_def;
	}

	public static List<EntityIndex> EVOLVE_ENTITY_INDICES(schema_receiver resolver,String entity_name,List<EntityIndex> proposed_indices) throws PersistenceException,InitializationException
	{
		IPersistenceProvider p				   = resolver.p;
		PersistentStore 	store 			   = p.getStore();
		IEvolutionProvider 	evolution_provider = p.getEvolutionProvider();
		List<EntityIndex> ret				= new ArrayList<EntityIndex>();
		List<EntityIndex> existing_indices	= store.getEntityIndices(entity_name); 

		if(existing_indices.size() == 0)
		{
			for(int i = 0;i < proposed_indices.size();i++)
			{
				EntityIndex pro_idx = proposed_indices.get(i);			
				pro_idx = do_define_entity_index(store, entity_name, pro_idx);
				ret.add(pro_idx);
			}
			return ret; 
		} 
		else if(existing_indices.size() == proposed_indices.size() && 
				existing_indices.containsAll(proposed_indices))
		{
			
			return existing_indices;
		}
		else
		{
			if(!resolver.made_backup_during_evolution)
			{
				boolean make_backup = confirm("ABOUT TO DROP INTO ENTITY INDEX EVOLUTION. WOULD YOU LIKE TO MAKE A COMPLETE BACKUP OF THE DATABASE?");
				if(make_backup)
				{
					String token = p.doFullBackup();
					System.out.println("DID FULL BACKUP: "+token);
					resolver.made_backup_during_evolution = true;
				}
			}
			System.out.println("EXISTING INDIXES DO NOT EQUAL PROPOSED INDICES. \nEXISTING:\n"+existing_indices+"\nPROPOSED:\n"+proposed_indices);
			evolution_provider.evolveIndexes(resolver,entity_name,existing_indices,proposed_indices);
		}
		return proposed_indices;
	}
	

	
	public void CREATE_QUEUE(String queuename,int rec_size,int recs_per_extent) throws PersistenceException
	{
		CREATE_QUEUE(store,queuename, rec_size, recs_per_extent);
	}
	
	public void DELETE_QUEUE(String queuename) throws PersistenceException
	{
		DELETE_QUEUE(queuename);
	}
	
	private static boolean confirm(String message) throws InitializationException
	{
		String answer = null;
		while(answer == null)
		{
			try{
				answer = GET_CONSOLE_INPUT(message+"\n\t[Y] YES\n\t[N] NO \n\t[A] Abort\n");
			}catch(WebApplicationException wae)
			{
				wae.printStackTrace();
			}
			answer = answer.toLowerCase();
			
			if(answer.equals("y"))
				return true;
			if(answer.equals("n"))
				return false;
			if(answer.equals("a"))
				throw new InitializationException("USER ABORTED STARTUP.");
			else
				answer = null;
		}
		return false;
	}
	
	
	//TRANSACTIONS///
	private static ThreadLocal<List<Integer>> current_transaction_id_list = new ThreadLocal<List<Integer>>();
	public void START_TRANSACTION() throws PersistenceException
	{
		START_TRANSACTION(store);
	}
	
	public static void START_TRANSACTION(PersistentStore store) throws PersistenceException
	{
		List<Integer> current_tid_list = current_transaction_id_list.get();
		if(current_tid_list == null)
		{
			current_tid_list = new ArrayList<Integer>(4);
			current_transaction_id_list.set(current_tid_list);		
		}
		
		if(current_tid_list.size()== 0)
			current_tid_list.add(store.startTransaction());
		else
			current_tid_list.add(store.startTransaction(current_tid_list.get(current_tid_list.size()-1)));

	}
	
	public void COMMIT_TRANSACTION() throws PersistenceException
	{
		COMMIT_TRANSACTION(store);
	}
	
	public static void COMMIT_TRANSACTION(PersistentStore store) throws PersistenceException
	{
		List<Integer> current_tid_list = current_transaction_id_list.get();
		int c_tid = current_tid_list.get(current_tid_list.size()-1);
		store.commitTransaction(c_tid);
		System.out.println("WEB STORE..attempting to commit "+c_tid);
		current_tid_list.remove(current_tid_list.size()-1);
	}
	
	public static Integer CURRENT_TRANSACTION_ID()
	{
		List<Integer> current_tid_list = current_transaction_id_list.get();
		if(current_tid_list == null || current_tid_list.size() == 0)
			return null;
		
		int s = current_tid_list.size();
		int c_tid = current_tid_list.get(s-1);
		return c_tid;
	}
		
	public void ROLLBACK_TRANSACTION() throws PersistenceException
	{
		ROLLBACK_TRANSACTION(store);
	}
	
	public static void ROLLBACK_TRANSACTION(PersistentStore store) throws PersistenceException
	{
		List<Integer> current_tid_list = current_transaction_id_list.get();
		int c_tid = current_tid_list.get(current_tid_list.size()-1);
		store.rollbackTransaction(c_tid);
		System.out.println("WEB STORE..attempting to rollback "+c_tid);
		current_tid_list.remove(current_tid_list.size()-1);
	}
	
}
