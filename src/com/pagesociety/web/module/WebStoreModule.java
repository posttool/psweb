package com.pagesociety.web.module;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityDefinition;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.EntityRelationshipDefinition;
import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.PersistentStore;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SlotException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.persistence.DefaultPersistenceEvolver;
import com.pagesociety.web.module.persistence.IEvolutionProvider;
import com.pagesociety.web.module.persistence.IPersistenceProvider;

public abstract class WebStoreModule extends WebModule
{

	private static final String SLOT_STORE 			    = "store";
	private static final String SLOT_EVOLUTION_PROVIDER = "evolution-provider";
	protected PersistentStore store;
	protected IEvolutionProvider evolution_provider;
	protected List<EntityDefinition> associated_entity_definitions;
	

	public void system_init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.system_init(app, config);		
		//this breaks a loope since we need to pre-init the default one manually..see below//
		if(!(this instanceof DefaultPersistenceEvolver))
			setup_evolution_provider(app);//can probs pass config along here//
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
		if(store_provider != null)
			store = store_provider.getStore();
		
		associated_entity_definitions = new ArrayList<EntityDefinition>();
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
		DEFINE_SLOT(SLOT_EVOLUTION_PROVIDER, IEvolutionProvider.class, false,DefaultPersistenceEvolver.class);
	}	
	
	private void setup_evolution_provider(WebApplication app) throws InitializationException
	{
		
		evolution_provider = ((IEvolutionProvider)getSlot(SLOT_EVOLUTION_PROVIDER));
		if(!(evolution_provider instanceof DefaultPersistenceEvolver))
			return;

		//this is how to programaticcaly init a module from within the app//		
		((WebModule)evolution_provider).defineSlots();
		((WebModule)evolution_provider).setName("DefaultEvolutionProvider");
		//here we set the store on the default implmentation because we know it needs it//
		try{
			if(((WebStoreModule)evolution_provider).getSlot(SLOT_STORE) == null)
				((WebStoreModule)evolution_provider).setSlot(SLOT_STORE,((IPersistenceProvider)getSlot(SLOT_STORE)));

			((WebModule)evolution_provider).system_init(app, new HashMap<String,Object>());
		}catch(SlotException se)
		{
			ERROR(se);
			throw new InitializationException(getName()+ ": FAILED SETTING STORE SLOT IN EVOLUTION PROVIDER");
		}	
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
		EntityDefinition 		existing_def;
		EntityDefinition 		proposed_def;
			
		existing_def = store.getEntityDefinition(entity_name);
		proposed_def = new EntityDefinition(entity_name);
		/* add system fields to proposed def */

		for(int i = 0;i < FIELDS.length;i++)
			proposed_def.addField(FIELDS[i]);
		
		for(int i = 0;i < defs.size();i++)
			proposed_def.addField(defs.get(i));
		/*create it if it doesnt exist*/
		if(existing_def == null)
			store.addEntityDefinition(proposed_def);
		else
		{
			/* check it to make sure the version passed in matches the one in the store */
			
			if(existing_def.equals(proposed_def))
				return proposed_def;
			else
				evolution_provider.evolveEntity(getName(), existing_def, proposed_def);
			
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

	

	
	public static EntityDefinition ADD_FIELDS(PersistentStore store,String entity_name,Object... fields) throws PersistenceException,InitializationException
	{
		EntityDefinition existing_def = store.getEntityDefinition(entity_name);
		if(existing_def == null)
			throw new SyncException("TRYING TO ADD FIELDS TO ENTITY "+entity_name+" BUT "+entity_name+" DOES NOT EXIST IN STORE");
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
			
			FieldDefinition existing_field = existing_def.getField(fieldname);
			if(existing_field == null)
				store.addEntityField(entity_name, f);
			else
			{
				if(!existing_field.equals(f))
					throw new SyncException("FIELD "+f+" ALREADY EXISTS IN ENTITY "+entity_name+" BUT IS DIFFERENT "+existing_field);
			}
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
		store.getEntityRelationships();
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
		return store.saveEntity(e);
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
	
	public static Entity EXPAND(PersistentStore store, Entity e) throws PersistenceException
	{
		if(!e.isLightReference())
			return e;
		return GET(store,e.getType(),e.getId());
	}

	public static Entity GET(PersistentStore store, String entity_type,long entity_id) throws PersistenceException
	{
		Entity e = store.getEntityById(entity_type, entity_id);
		if(e == null)
			throw new PersistenceException(entity_type+" INSTANCE WITH ID "+entity_id+" DOES NOT EXIST IN STORE.");
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
		store.fillReferenceField(instance,fieldname);
		return (Entity)instance.getAttribute(fieldname);
	}

	public static List<Entity> GET_LIST_REF(PersistentStore store,String entity_type,long entity_id, String fieldname) throws PersistenceException
	{
		Entity e = GET(store,entity_type,entity_id);
		return GET_LIST_REF(store,e,fieldname);
	}
	
	public static List<Entity> GET_LIST_REF(PersistentStore store,Entity instance, String fieldname) throws PersistenceException
	{
		store.fillReferenceField(instance,fieldname);
		return (List<Entity>)instance.getAttribute(fieldname);
	}
	
	public static Entity FILL_REF(PersistentStore store,Entity instance, String fieldname) throws PersistenceException
	{
		store.fillReferenceField(instance,fieldname);
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
		store.fillReferenceFields(instance);
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
		return store.saveEntity(e);
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
	
			return DELETE(store,e);
		}catch(Exception ee)
		{
			ee.printStackTrace();
			throw new PersistenceException("BARFED IN DELETE DEEP "+ee.getMessage());
		}
	}

	
	protected void EVOLVE_IGNORE(String entity_name,Object ...flattened_field_definitions)
	{
		List<FieldDefinition> ff = UNFLATTEN_FIELD_DEFINITIONS(flattened_field_definitions);
	
		for(int i = 0;i < ff.size();i++)
			evolution_provider.evolveIgnore(entity_name, ff.get(i).getName());		
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
			
			if(field_name.equals("profile_image"))
				System.out.println("TYPE IS "+field_type);
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

}
