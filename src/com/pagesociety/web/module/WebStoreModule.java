package com.pagesociety.web.module;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.pagesociety.web.module.user.UserModule;

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

	public PersistentStore getStore()
	{
		return store;
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_STORE, IPersistenceProvider.class, true);

	}


	public static final String PROP_ENTITY 		 			= "WebStoreModuleProp";
	public static final String PROP_ENTITY_FIELD_KEY  		= "WebStoreModulePropKey";
	public static final String PROP_ENTITY_FIELD_VALUE  	= "WebStoreModulePropValue";


	protected void defineEntities(Map<String,Object> config)throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY(PROP_ENTITY,
						PROP_ENTITY_FIELD_KEY,Types.TYPE_STRING,null,
						PROP_ENTITY_FIELD_VALUE,Types.TYPE_STRING,null);

	}
	protected static final String IDX_BY_KEY = "byKey";
	protected void defineIndexes(Map<String,Object> config)throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY_INDEX(PROP_ENTITY, IDX_BY_KEY, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, PROP_ENTITY_FIELD_KEY);
	}


	protected String SET_PROP(String key,String value) throws PersistenceException
	{
		Entity prop 	= getPropByKey(key);
		if(prop == null)
		{
			prop = NEW(PROP_ENTITY,
								null,
								PROP_ENTITY_FIELD_KEY,key,
								PROP_ENTITY_FIELD_VALUE,value);
			return null;
		}
		else
		{
			String ret = (String)prop.getAttribute(PROP_ENTITY_FIELD_VALUE);
			UPDATE(prop,PROP_ENTITY_FIELD_VALUE,value);
			return ret;
		}
	}

	protected String CLEAR_PROP(String key) throws PersistenceException
	{
		Entity prop 	= getPropByKey(key);
		if(prop != null)
		{
			String ret = (String)prop.getAttribute(PROP_ENTITY_FIELD_VALUE);
			DELETE(prop);
			return ret;
		}
		return null;
	}

	protected String GET_PROP(String key) throws PersistenceException
	{
		Entity prop 	= getPropByKey(key);
		if(prop == null)
			return null;
		else
			return (String)prop.getAttribute(PROP_ENTITY_FIELD_VALUE);
	}

	protected Entity getPropByKey(String key) throws PersistenceException
	{
		Query q = new Query(PROP_ENTITY);
		q.idx(IDX_BY_KEY);
		q.eq(Query.VAL_GLOB);
		QueryResult r = QUERY(q);
		List<Entity> ee = r.getEntities();
		if(ee.size() == 0)
			return null;
		else
			return ee.get(0);
	}

	protected void defineRelationships(Map<String,Object> config)throws PersistenceException,InitializationException
	{
		/*do nothing by default */
	}

	public List<EntityDefinition> getAssociatedEntityDefinitions()
	{
		return associated_entity_definitions;
	}

	public void VERIFY_MODULE_IS_DEFINER_OF_ENTITY_TYPE(String type) throws WebApplicationException
	{
		List<EntityDefinition> defs = getAssociatedEntityDefinitions();
		for(int i = 0;i< defs.size();i++)
		{
			if(defs.get(i).getName().equals(type))
				return;
		}
		throw new WebApplicationException("THIS MODULE DID NOT DEFINE TYPE "+type);
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



	public static Entity NEW(PersistentStore store,String entity_type,Entity creator,Object...attribute_name_values) throws PersistenceException
	{
		//EntityDefinition def = store.getEntityDefinition(entity_type);
		//if (def==null)
		//	throw new PersistenceException("NO SUCH ENTITY TYPE IN STORE ["+entity_type+"]");
		//Entity e = def.createInstance();
		//set_attributes(e, attribute_name_values);

		Map<String,Object> value_map = KEY_VALUE_PAIRS_TO_MAP(attribute_name_values);

		Date now = new Date();
		value_map.put(FIELD_CREATOR,creator);
		value_map.put(FIELD_DATE_CREATED,now);
		value_map.put(FIELD_LAST_MODIFIED,now);
		//e.setAttribute("reverse_last_modified",new Date(Long.MAX_VALUE-now.getTime()));
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			return store.createEntity(entity_type, value_map);
		else
			return store.createEntity(tid, entity_type, value_map);
	}

	public static Entity NEW(PersistentStore store,String entity_type,Entity creator,Map<String,Object> entity_atts) throws PersistenceException
	{
		//EntityDefinition def = store.getEntityDefinition(entity_type);
		//if (def==null)
		//	throw new PersistenceException("NO SUCH ENTITY TYPE IN STORE ["+entity_type+"]");
		//Entity e = def.createInstance();
		//Iterator<String> keys = entity_atts.keySet().iterator();
		//while(keys.hasNext())
		//{
		//	String key = keys.next();
		//	e.setAttribute(key, entity_atts.get(key));
		//}
		Date now = new Date();
		entity_atts.put(FIELD_CREATOR,creator);
		entity_atts.put(FIELD_DATE_CREATED,now);
		entity_atts.put(FIELD_LAST_MODIFIED,now);
		//e.setAttribute("reverse_last_modified",new Date(Long.MAX_VALUE-now.getTime()));
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			return store.createEntity(entity_type,entity_atts);
		else
			return store.createEntity(tid,entity_type,entity_atts);
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
	//this should be deprecated...use NEW
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

	/*SAVE NEW*/
	@Deprecated
	public static Entity CREATE_ENTITY(PersistentStore store,Entity creator,String type,Map<String,Object> value_map) throws PersistenceException
	{
		Date now = new Date();
		value_map.put(FIELD_CREATOR,creator);
		value_map.put(FIELD_DATE_CREATED,now);
		value_map.put(FIELD_LAST_MODIFIED,now);
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			return store.createEntity(type,value_map);
		else
			return store.createEntity(tid,type,value_map);

	}


	public static Entity EXPAND(PersistentStore store, Entity e) throws PersistenceException
	{
		if(e == null)
			return null;
		if(!e.isLightReference())
			return e;
		Entity ee =  GET(store,e.getType(),e.getId());
		e.setAttributes(ee.getAttributes());
		return ee;
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

//do_fill_deep(store,e,0,MAX_INT,FILL(dsfs, sfsf, gdgd, sdgsdg), MASK(password, email))
	public static final String[] EMPTY_STRING_ARRAY  = new String[0];
	public static final String[] FILL_ALL_FIELDS     = EMPTY_STRING_ARRAY;
	public static final String[] FILL_NO_FIELDS    	 = null;
	public static final String[] MASK_NO_FIELDS    	 = EMPTY_STRING_ARRAY;
	public static final String[] MASK_CREATOR    	 = new String[]{FIELD_CREATOR};
	public static final String[] MASK_EMAIL_PASSWORD = new String[]{UserModule.FIELD_EMAIL,UserModule.FIELD_PASSWORD};
	public static String[] MASK(String... fieldnames)
	{
		return fieldnames;
	}

	public static String[] FILL(String... fieldnames)
	{
		return fieldnames;
	}

	public static void do_fill_deep(PersistentStore store,Entity e,int c,int d) throws PersistenceException
	{
		do_fill_deep(store, e, c, d, EMPTY_STRING_ARRAY, EMPTY_STRING_ARRAY,new HashMap<Entity,Entity>());
	}

	public static void do_fill_deep(PersistentStore store,Entity e,int c, int d,String... fill_fields) throws PersistenceException
	{
		do_fill_deep(store, e, c, d, fill_fields, EMPTY_STRING_ARRAY,new HashMap<Entity,Entity>());
	}

	/*pass EMPTY_STRING_ARRAY for mask fields to mask none and FILL_ALL_FIELDS && FILL_NO_FIELDS to fill none for fill)fields to fill all */
	public static void do_fill_deep(PersistentStore store,Entity e,int c,int d,String[] fill_fields,String[] mask_fields,Map<Entity,Entity> seen_references) throws PersistenceException
	{
		EXPAND(store,e);
		if(e == null || c == d)
			return;

		seen_references.put(e,e);
		EntityDefinition def = store.getEntityDefinition(e.getType());
		String[] fields_to_fill;
		if(fill_fields.length == 0)
		{	//fill all ref fields//
			List<FieldDefinition> r_fields = def.getReferenceFields();
			fields_to_fill = new String[r_fields.size()];
			for(int ii = 0;ii < fields_to_fill.length;ii++)
				fields_to_fill[ii] = r_fields.get(ii).getName();
		}
		else
			fields_to_fill = fill_fields;

		for(int m = 0;m < mask_fields.length;m++)
			e.setAttribute(mask_fields[m], null);

		for(int i = 0;i < fields_to_fill.length ;i++)
		{
			String ref_field_name = fields_to_fill[i];
			FieldDefinition fd    = def.getField(ref_field_name);

			if(fd == null)//this can hapen when you use the explict fill since all fieldnames will not be in all entities
				continue;

			//for(int m = 0;m < mask_fields.length;m++)
			//{
			//	if(mask_fields[m].equals(ref_field_name))
			//		continue;
			//}
			/* I replaced the above routine with the one below
			 * They should be finctionally equivalent since
			 * we are nulling out the masked fields above
			 */
			if(e.getAttribute(ref_field_name) == null)
				continue;

			FILL_REF(store,e,ref_field_name);

			if(fd.isArray())
			{
				c++;

				List<Entity> l = (List<Entity>)e.getAttribute(ref_field_name);
				if(l == null)
					continue;
				for(int ii = 0;ii < l.size();ii++)
				{
					Entity val = l.get(ii);
					if(val != null && seen_references.get(val) != null)
					{
						l.set(ii, seen_references.get(val));
						continue;
					}
					do_fill_deep(store,l.get(ii),c,d,fill_fields,mask_fields,seen_references);
				}
			}
			else
			{

				Entity val = (Entity)e.getAttribute(ref_field_name);
				if(val != null && seen_references.get(val) != null)
					e.setAttribute(ref_field_name, seen_references.get(val));
				else
					do_fill_deep(store,val, ++c, d,fill_fields,mask_fields,seen_references);
			}
		}
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


	public static Entity UPDATE(PersistentStore store,Entity instance,Map<String,Object> entity_atts) throws PersistenceException
	{
		//we do this setting of the attributes to
		//make sure the local copy attributes get updated
		//sometimes we do UPDATE(instance,NAME,"foo"
		//and we expect foo to be in the NAME field of instance
		Entity e = instance;
		Iterator<String> keys = entity_atts.keySet().iterator();
		while(keys.hasNext())
		{
			String key = keys.next();
			e.setAttribute(key, entity_atts.get(key));
		}

		Date now = new Date();
		e.setAttribute(FIELD_LAST_MODIFIED,now);
		entity_atts.put(FIELD_LAST_MODIFIED,now);
		return UPDATE(store,instance.getType(),instance.getId(),entity_atts);
	}

	public static Entity UPDATE(PersistentStore store,Entity instance,Object... name_value_pairs) throws PersistenceException
	{
		if(instance == null)
			throw new PersistenceException("BAD UPDATE.CANNOT UPDATE A NULL ENTITY");

		set_attributes(instance, name_value_pairs);
		Map<String,Object> update_vals = KEY_VALUE_PAIRS_TO_MAP(name_value_pairs);
		Date now = new Date();
		instance.setAttribute(FIELD_LAST_MODIFIED,now);
		update_vals.put(FIELD_LAST_MODIFIED,now);
		return UPDATE(store,instance.getType(),instance.getId(),update_vals);

	}

	public static Entity UPDATE(PersistentStore store,String entity_type,long entity_id,Map<String,Object> update_values) throws PersistenceException
	{
		Date now = new Date();
		update_values.put(FIELD_LAST_MODIFIED,now);
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			return store.updateEntity(entity_type,entity_id,update_values);
		else
			return store.updateEntity(tid,entity_type,entity_id,update_values);
	}

	/*SAVE UPDATE */
	//THIS SHOULD BE DEPRECATED USE NEW UPDATE//
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

	public static QueryResult QUERY_FILL_DEEP(PersistentStore store,Query q) throws PersistenceException
	{
		QueryResult results =  QUERY(store,q);
		List<Entity> entities = results.getEntities();
		int s = results.size();
		for(int i = 0; i < s;i++)
		{
			Entity e = entities.get(i);
			do_fill_deep(store, e,0,Integer.MAX_VALUE);
		}
		return results;
	}

	public static QueryResult QUERY_FILL_DEEP(PersistentStore store,Query q,String... fill_fields) throws PersistenceException
	{
		QueryResult results =  QUERY(store,q);
		List<Entity> entities = results.getEntities();
		int s = results.size();
		for(int i = 0; i < s;i++)
		{
			Entity e = entities.get(i);
			do_fill_deep(store, e,0,Integer.MAX_VALUE,fill_fields);
		}
		return results;
	}

	public static QueryResult QUERY_FILL_DEEP_AND_MASK(PersistentStore store,Query q,String[] fill_fields,String[] mask_fields) throws PersistenceException
	{
		QueryResult results =  QUERY(store,q);
		List<Entity> entities = results.getEntities();
		int s = results.size();
		for(int i = 0; i < s;i++)
		{
			Entity e = entities.get(i);
			do_fill_deep(store, e,0,Integer.MAX_VALUE,fill_fields,mask_fields, new HashMap<Entity,Entity>());
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
			for(int ii=0;ii <mask_fields.length;ii++)
				e.getAttributes().put(mask_fields[ii],null);
		}
		return results;
	}


	public static PagingQueryResult PAGING_QUERY(PersistentStore store,Query q) throws PersistenceException
	{
		QueryResult results = QUERY(store,q);
		int total_count 	= COUNT(store,q);//store.count(q);
		return new PagingQueryResult(results.getEntities(),total_count,q.getOffset(),q.getPageSize());
	}

	public static PagingQueryResult PAGING_QUERY_FILL(PersistentStore store,Query q) throws PersistenceException
	{
		QueryResult results = QUERY_FILL(store,q);
		int total_count = COUNT(store,q);
		return new PagingQueryResult(results,total_count,q.getOffset(),q.getPageSize());
	}

	public static PagingQueryResult PAGING_QUERY_FILL_DEEP(PersistentStore store,Query q) throws PersistenceException
	{
		QueryResult results = QUERY_FILL_DEEP(store,q);
		int total_count = COUNT(store,q);
		return new PagingQueryResult(results,total_count,q.getOffset(),q.getPageSize());
	}

	public static PagingQueryResult PAGING_QUERY_FILL(PersistentStore store,Query q,String... fill_fields) throws PersistenceException
	{
		QueryResult results = QUERY_FILL(store,q,fill_fields);
		int total_count = COUNT(store,q);
		return new PagingQueryResult(results,total_count,q.getOffset(),q.getPageSize());
	}

	public static PagingQueryResult PAGING_QUERY_FILL_DEEP(PersistentStore store,Query q,String... fill_fields) throws PersistenceException
	{
		QueryResult results = QUERY_FILL_DEEP(store,q,fill_fields);
		int total_count = COUNT(store,q);
		return new PagingQueryResult(results,total_count,q.getOffset(),q.getPageSize());
	}

	public static PagingQueryResult PAGING_QUERY_FILL_AND_MASK(PersistentStore store,Query q,String... args) throws PersistenceException
	{
		QueryResult results = QUERY_FILL_AND_MASK(store,q);
		int total_count = COUNT(store,q);
		return new PagingQueryResult(results,total_count,q.getOffset(),q.getPageSize());
	}
	public static PagingQueryResult PAGING_QUERY_FILL_DEEP_AND_MASK(PersistentStore store,Query q,String[] fill_fields,String[] mask_fields) throws PersistenceException
	{
		QueryResult results = QUERY_FILL_DEEP_AND_MASK(store,q,fill_fields,mask_fields);
		int total_count = COUNT(store,q);
		return new PagingQueryResult(results,total_count,q.getOffset(),q.getPageSize());
	}

	public static Entity FILL_DEEP_AND_MASK(PersistentStore store,Entity e,String[] fill_fields,String[] mask_fields) throws PersistenceException
	{
		do_fill_deep(store,e,0,Integer.MAX_VALUE,fill_fields,mask_fields,new HashMap<Entity,Entity>());
		return e;
	}

	public static int COUNT(PersistentStore store,Query q) throws PersistenceException
	{
		Integer tid = CURRENT_TRANSACTION_ID();
		if(tid == null)
			return store.count(q);
		else
			return store.count(tid,q);
	}


	public static Entity GET_ONE(PersistentStore store,String entity_type, String idx_name, Object... query_vals) throws PersistenceException,WebApplicationException
	{
		Query q = new Query(entity_type);
		q.idx(idx_name);
		if(query_vals.length == 1)
			q.eq(query_vals[0]);
		else
		{
			List<Object> ll = new ArrayList<Object>();
			for(int i = 0;i < query_vals.length;i++)
				ll.add(query_vals[i]);
			q.eq(ll);
		}
		List<Entity> ee = QUERY(store,q).getEntities();
		int s = ee.size();
		if(s == 0)
			return null;
		else if(s == 1)
			return ee.get(0);
		else
		{
			StringBuilder buf = new StringBuilder();
			for(int i = 0;i < query_vals.length;i++)
			{
				buf.append(String.valueOf(query_vals[i]));
				buf.append(' ');
			}
			throw new WebApplicationException("MORE THAN ONE ENTITY OF TYPE "+entity_type+" EXISTS FOR INDEX "+idx_name+" AND VALUES "+buf.toString());
		}
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
				e.printStackTrace();
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
	public Entity NEW(String entity_type,Entity creator,Object... attribute_name_values) throws PersistenceException
	{
		return NEW(store,entity_type,creator,attribute_name_values);
	}

	//DEPRECATE THIS
	@Deprecated
	public Entity CREATE_ENTITY(Entity creator,Entity instance) throws PersistenceException
	{
		return CREATE_ENTITY(store,creator,instance);
	}

	@Deprecated
	public Entity CREATE_ENTITY(Entity creator,String type,Map<String,Object> value_map) throws PersistenceException
	{
		return CREATE_ENTITY(store,creator,type,value_map);
	}

	public Entity EXPAND(Entity e) throws PersistenceException
	{
		return EXPAND(store,e);
	}

	public List<Entity> EXPAND(List<Entity> ee) throws PersistenceException
	{
		for(int i = 0;i < ee.size();i++)
			EXPAND(ee.get(i));

		return ee;
	}

	public Entity FORCE_EXPAND(Entity e) throws PersistenceException
	{
		return FORCE_EXPAND(store,e);
	}

	public Entity GET( String entity_type,long entity_id) throws PersistenceException//this should throw webapplication exception....not persistence execption duh.
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

	public Entity UPDATE(String entity_type,long entity_id,Map<String,Object> value_map) throws PersistenceException,WebApplicationException
	{
		return UPDATE(store,entity_type,entity_id,value_map);
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
	//TODO: DEPRECATE THIS
	public Entity SAVE_ENTITY(Entity instance) throws PersistenceException
	{
		return SAVE_ENTITY(store,instance);
	}


	public QueryResult QUERY(Query q) throws PersistenceException
	{
		return QUERY(store, q);
	}

	public Entity GET_ONE(String entity_type, String idx_name, Object... query_vals) throws PersistenceException,WebApplicationException
	{
		return GET_ONE(store,entity_type,idx_name,query_vals);
	}
	public QueryResult QUERY_FILL(Query q) throws PersistenceException
	{
		return QUERY_FILL(store, q);
	}

	public QueryResult QUERY_FILL_DEEP(Query q) throws PersistenceException
	{
		return QUERY_FILL_DEEP(store, q);
	}

	public QueryResult QUERY_FILL(Query q,String... fill_fieldnames) throws PersistenceException
	{
		return QUERY_FILL(store,q, fill_fieldnames);
	}

	public QueryResult QUERY_FILL_DEEP(Query q,String... fill_fieldnames) throws PersistenceException
	{
		return QUERY_FILL_DEEP(store,q, fill_fieldnames);
	}

	/* THIS FILLS ALL THE REFS BY DEFAULT. THE MASK FIELDS ARE APPLIED AFTER*/
	public QueryResult QUERY_FILL_AND_MASK(Query q,String... mask_fields) throws PersistenceException
	{
		return QUERY_FILL_AND_MASK(store, q, mask_fields);
	}

	public QueryResult QUERY_FILL_DEEP_AND_MASK(Query q,String[] fill_fieldnames,String[] mask_fieldnames) throws PersistenceException
	{
		return QUERY_FILL_DEEP_AND_MASK(store,q, fill_fieldnames,mask_fieldnames);
	}

	public PagingQueryResult PAGING_QUERY(Query q) throws PersistenceException
	{
		return PAGING_QUERY(store,q);
	}

	public PagingQueryResult PAGING_QUERY_FILL(Query q) throws PersistenceException
	{
		return PAGING_QUERY_FILL(store, q);
	}

	public PagingQueryResult PAGING_QUERY_FILL_DEEP(Query q) throws PersistenceException
	{
		return PAGING_QUERY_FILL_DEEP(store, q);
	}

	public PagingQueryResult PAGING_QUERY_FILL(Query q,String... fill_fields) throws PersistenceException
	{
		return PAGING_QUERY_FILL(store, q, fill_fields);
	}

	public PagingQueryResult PAGING_QUERY_FILL_DEEP(Query q,String... fill_fields) throws PersistenceException
	{
		return PAGING_QUERY_FILL(store, q, fill_fields);
	}

	public PagingQueryResult PAGING_QUERY_FILL_AND_MASK(Query q,String... mask_fields) throws PersistenceException
	{
		return PAGING_QUERY_FILL_AND_MASK(store, q, mask_fields);
	}

	public PagingQueryResult PAGING_QUERY_FILL_DEEP_AND_MASK(Query q,String[] fill_fields,String[] mask_fields) throws PersistenceException
	{
		return PAGING_QUERY_FILL_DEEP_AND_MASK(store, q,fill_fields,mask_fields);
	}

	public Entity FILL_DEEP_AND_MASK(Entity e,String[] fill_fields,String[] mask_fields) throws PersistenceException
	{

		return FILL_DEEP_AND_MASK(store, e,fill_fields,mask_fields);
	}

	public Entity FILL_DEEP(Entity e) throws PersistenceException
	{
		e = EXPAND(e);
		return FILL_DEEP_AND_MASK(store, e,FILL_ALL_FIELDS,MASK_NO_FIELDS);
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
		System.out.println("DEFINING "+entity_name);
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
		return store.saveEntity(e.copy());
	}

	public Entity CLONE_SHALLOW(Entity e) throws PersistenceException
	{
		return CLONE_SHALLOW(store,e);
	}


	//clone subsystem/////////
	//THIS MAKES COPIES IN THE DATABASE SO MKAE SURE THIS IS WHAT YOU WANT!!!
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
		Entity clone =  CLONE_DEEP(store, e, f,new HashMap<Entity,Entity>());
		return FILL_DEEP_AND_MASK(store, clone, FILL_ALL_FIELDS, MASK_NO_FIELDS);
	}

	//warning: this function is pretty intense. makes a deep clone in the database//
	public static Entity CLONE_DEEP(PersistentStore store,Entity e,clone_policy f,HashMap<Entity,Entity> clone_map) throws PersistenceException
	{
		if(e == null)
			return null;
		if(clone_map.containsKey(e))
		{
			//see note below about saving dirty references//
			//return the light reference//
			Entity c = clone_map.get(e);
			Entity cr = new Entity();
			cr.setType(c.getType());
			cr.setId(c.getId());
			return cr;
		}

		e = EXPAND(store, e);

		List<FieldDefinition> ref_fields = store.getEntityDefinition(e.getType()).getReferenceFields();

		Entity clone = e.copy();
		for(int i = 0;i < ref_fields.size();i++)
		{
			//we do this becuase the store is picky about saving references to dirty reference
			//fields. usually we always save a dirty reference before we 'link' it
			clone.setAttribute(ref_fields.get(i).getName(), null);
		}
		clone = CREATE_ENTITY(store,(Entity)clone.getAttribute(FIELD_CREATOR),clone);
		clone_map.put(e,clone);


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
							clone_vals.add(CLONE_DEEP(store,val,f,clone_map));
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
						clone.setAttribute(ref_fieldname,CLONE_DEEP(store,val,f,clone_map));
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

		clone = SAVE_ENTITY(store,clone);
		clone_map.put(e, clone);
		return clone;
	}

	/*BEFORE I FUCK THIS UP TOO MUCH! remove the clone map stuff to get back to the way it was
	public static Entity CLONE_DEEP(PersistentStore store,Entity e,clone_policy f,HashMap<Entity,Entity> clone_map) throws PersistenceException
	{
		if(e == null)
			return null;
		if(clone_map.containsKey(e))
			return clone_map.get(e);

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
							clone_vals.add(CLONE_DEEP(store,val,f,clone_map));
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
						clone.setAttribute(ref_fieldname,CLONE_DEEP(store,val,f,clone_map));
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

		clone = CREATE_ENTITY(store,(Entity)clone.getAttribute(FIELD_CREATOR),clone);
		clone_map.put(e, clone);
		return clone;
	}
	*/

	public Entity CLONE_IN_MEMORY(Entity e) throws PersistenceException
	{
		return CLONE_IN_MEMORY(e, new clone_policy(), new HashMap<Entity, Entity>());
	}
	public Entity CLONE_IN_MEMORY(Entity e,clone_policy p,Map<Entity,Entity> clone_map) throws PersistenceException
	{
		if(e == null)
			return null;

		if(clone_map.containsKey(e))
			return clone_map.get(e);

		Entity clone = e.clone();
		clone_map.put(e,clone);
		if(e.isLightReference())
			return clone;

		List<FieldDefinition> ref_fields = store.getEntityDefinition(e.getType()).getReferenceFields();
		for(int i = 0;i < ref_fields.size();i++)
		{
			FieldDefinition ref_field 	 = ref_fields.get(i);
			String ref_fieldname 		 = ref_field.getName();
			if(ref_field.isArray())
			{
				//List<Entity> vals = GET_LIST_REF(store,e,ref_fieldname);
				List<Entity> vals = (List<Entity>)e.getAttribute(ref_fieldname);

				if(vals == null)
					continue;

				int s = vals.size();
				List<Entity> clone_vals = new ArrayList<Entity>(s);
				for(int j = 0;j <s;j++)
				{
					Entity val = vals.get(j);
					int clone_behavior = p.exec(e, ref_fieldname, val);
					switch(clone_behavior)
					{
						case CLONE_REFERENCE:
							clone_vals.add(CLONE_IN_MEMORY(val,p,clone_map));
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

				//Entity val = GET_REF(store,e,ref_fieldname);
				Entity val = (Entity)e.getAttribute(ref_fieldname);
				int clone_behavior = p.exec(e, ref_fieldname, val);
				switch(clone_behavior)
				{
					case CLONE_REFERENCE:
						clone.setAttribute(ref_fieldname,CLONE_IN_MEMORY(val,p,clone_map));
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
		clone_map.put(e, clone);
		return clone;
	}


	public void DUMP_ENTITY(Entity e) throws PersistenceException
	{
		StringBuilder buf = new StringBuilder();
		do_print_deep(e,0,new HashMap<Entity,Entity>(),buf);
		INFO("DUMP\n"+buf.toString());
	}

	public String ENTITY_TO_STRING(Entity e) throws PersistenceException
	{
		StringBuilder buf = new StringBuilder();
		do_print_deep(e,0,new HashMap<Entity,Entity>(),buf);
		return buf.toString();
	}

	public void do_print_deep(Entity e,int indent,Map<Entity,Entity> seen_entities,StringBuilder buf) throws PersistenceException
	{
		if(e == null)
		{
			buf.append("NULL");
			return;
		}
		String mem_id = "0x"+Integer.toHexString(System.identityHashCode(e));
		if(seen_entities.containsKey(e))
		{
			buf.append(e.getType()+":"+e.getId()+"(CIRCULAR REF "+mem_id+")");
			return;
		}
		seen_entities.put(e, e);
		if(e.isLightReference())
		{
			buf.append(e.getType()+":"+e.getId()+"(LIGHT REF "+mem_id+")");
		}
		else
		{
			EntityDefinition def = store.getEntityDefinition(e.getType());
			buf.append("\n"+get_space(indent)+"{\n");
			buf.append(get_space(indent)+e.getType()+":"+e.getId()+"("+mem_id+")");
			buf.append("\n");
			List<FieldDefinition> ff = def.getFields();
			for(int i = 0;i < ff.size();i++)
			{
				FieldDefinition f = ff.get(i);
				if((f.getType() & Types.TYPE_REFERENCE) == Types.TYPE_REFERENCE)
				{
					if(f.isArray())
					{
						List<Entity> vals = (List<Entity>)e.getAttribute(f.getName());
						if(vals == null)
						{
							buf.append(get_space(indent+1)+f.getName()+" = NULL\n");
						}
						else if(vals.size() == 0)
						{
							buf.append(get_space(indent+1)+f.getName()+" = []\n");
						}
						else
						{
						buf.append(get_space(indent+1)+f.getName()+" = [");
						for(int ii=0;ii< vals.size();ii++)
						{
							do_print_deep(vals.get(ii),indent+f.getName().length()+4,seen_entities,buf);
							if(ii != vals.size()-1)
								buf.append("\n"+get_space(indent+f.getName().length()+4)+",");
						}
						buf.append("\n"+get_space(indent+f.getName().length()+4)+"]\n");
						}
					}
					else
					{
						Entity val = (Entity)e.getAttribute(f.getName());
						buf.append(get_space(indent+1)+f.getName()+" = ");
						do_print_deep(val,indent+f.getName().length()+4,seen_entities,buf);
						buf.append("\n");
					}
				}
				else
				{
					buf.append(get_space(indent+1)+f.getName()+" = "+e.getAttribute(f.getName())+"\n");
				}
			}
			buf.append(get_space(indent)+"}/*end - "+e.getType()+":"+e.getId()+"*/");
		}
	}



	private String get_space(int num)
	{
		StringBuilder buf = new StringBuilder();
		for(int i = 0;i < num;i++)
			buf.append(' ');
		return buf.toString();
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
		e = EXPAND(store,e);
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
						Entity val = vals.get(j);
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
				int ec = pe.getErrorCode();
				if (pe.getCause() != null && pe.getCause() instanceof PersistenceException)
					ec = ((PersistenceException)pe.getCause()).getErrorCode();
				if(ec == PersistenceException.ENTITY_DOES_NOT_EXIST)
					return e;
				throw pe;
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
			if((field_type & ~Types.TYPE_ARRAY) == Types.TYPE_REFERENCE)
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
		if(System.getProperty("ps.persistence.abort_after_evolution") != null)
		{
			app.applicationDestroyed();
			System.out.println("EXITING EVOLUTION");
			System.exit(0);
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

		//check the proposed def//
		List<FieldDefinition> ff = proposed_def.getFields();
		//validate fields//
		for(int i=0;i < ff.size();i++)
		{
			FieldDefinition f = ff.get(i);
			if(!f.isValidValue(f.getDefaultValue()))
				throw new InitializationException("ENTITY "+proposed_def.getName()+". FIELD  "+f.getName()+" HAS A DEFAULT VALUE OF THE WRONG TYPE. TYPE SHOULD BE "+FieldDefinition.typeAsString(f.getType())+" AND IS "+f.getDefaultValue().getClass().getName()+".");
		}

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
		List<EntityIndex> existing_indices  = null;
		try{
			existing_indices	= store.getEntityIndices(entity_name);

		}catch(PersistenceException e)
		{
			//someone might be defining a new entity that has never existed//
			//since now we are evloving indexes before entitites that entity
			//might not exist yet in the store. the store throws an exception
			//when asked for entities that it doesnt know about
			//ENTITY OF TYPE WebStoreModuleProp DOES NOT EXIST
			existing_indices = new ArrayList<EntityIndex>();

		}

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

	public PagingQueryResult BROWSE(String entity_type, String index, int op,Object value, int offset, int page_size,String order_by,int order_by_order,String[] fill_fields, String[] mask_fields, boolean cache_results) throws PersistenceException
	{
		Query q = new Query(entity_type);
		q.idx(index);
		switch(op)
		{
		case Query.EQ:
				q.eq(value);
				break;
		case Query.GT:
				q.gt(value);
				break;
		case Query.GTE:
				q.gte(value);
				break;
		case Query.LT:
				q.lt(value);
				break;
		case Query.LTE:
				q.lte(value);
				break;
		case Query.STARTSWITH:
				q.startsWith(value);
				break;
		case Query.SET_CONTAINS_ANY:
				q.setContainsAny((List<?>)value);
				break;
		case Query.SET_CONTAINS_ALL:
				q.setContainsAll((List<?>)value);
				break;
		case Query.BETWEEN_START_INCLUSIVE_ASC:
				Object[] vals = (Object[])value;
				q.betweenStartInclusive(vals[0],vals[1]);
				break;
		case Query.BETWEEN_START_INCLUSIVE_DESC:
				vals = (Object[])value;
				q.betweenStartInclusiveDesc(vals[0],vals[1]);
				break;
		case Query.BETWEEN_END_INCLUSIVE_ASC:
				vals = (Object[])value;
				q.betweenEndInclusive(vals[0],vals[1]);
				break;
		case Query.BETWEEN_END_INCLUSIVE_DESC:
				vals = (Object[])value;
				q.betweenEndInclusiveDesc(vals[0],vals[1]);
				break;
		case Query.BETWEEN_INCLUSIVE_ASC:
				vals = (Object[])value;
				q.between(vals[0],vals[1]);
				break;
		case Query.BETWEEN_INCLUSIVE_DESC:
				vals = (Object[])value;
				q.betweenDesc(vals[0],vals[1]);
				break;
		}

		q.offset(offset);
		q.pageSize(page_size);
		if(order_by != null)
			q.orderBy(order_by,order_by_order);
		q.cacheResults(cache_results);
		if(fill_fields == FILL_NO_FIELDS)
			return PAGING_QUERY(q);
		else
			return PAGING_QUERY_FILL_DEEP_AND_MASK(q, fill_fields, mask_fields);
	}

	//STEPPING THROUGH ALL ENTITIES OF A TYPE
	protected void PAGE_APPLY_INTERRUPTABLE(Object synchronization_obj,int throttle,String type,CALLBACK c) throws PersistenceException,WebApplicationException,InterruptedException
	{
		int page_size 	= 100;
		int num_results = page_size;
		int page = 0;
		do
		{
			List<Entity> ee = null;
			//whoever is doing the interrupting should synchronize on the same object//
			synchronized(synchronization_obj)
			{
				if(Thread.interrupted())
					throw new InterruptedException();

				Query q = new Query(type);
				q.idx(Query.PRIMARY_IDX);
				q.eq(Query.VAL_GLOB);
				q.offset(page*page_size);
				q.pageSize(page_size);
				QueryResult result = QUERY(q);
				num_results 		= result.size();
				ee 	= result.getEntities();
			}

			for(int i = 0;i < ee.size();i++)
			{
				try{
					synchronized(synchronization_obj)
					{
						if(Thread.interrupted())
							throw new InterruptedException();
						c.exec(ee.get(i));
					}
					if(throttle != 0)
						Thread.sleep(throttle);
				}
				catch(InterruptedException ie)
				{
					throw ie;
				}
				catch(Exception e)
				{
					e.printStackTrace();
					throw new WebApplicationException("PROBLEM APPLYING FUNCTION "+e.getMessage());
				}
			}
			page++;
		}while(num_results >= page_size);
	}


	protected void PAGE_APPLY(String type,CALLBACK c) throws PersistenceException,WebApplicationException
	{
		int page_size 	= 100;
		int num_results = page_size;
		int page = 0;
		do
		{
			Query q = new Query(type);
			q.idx(Query.PRIMARY_IDX);
			q.eq(Query.VAL_GLOB);
			q.offset(page*page_size);
			q.pageSize(page_size);
			QueryResult result = QUERY(q);
			num_results 		= result.size();
			List<Entity> ee 	= result.getEntities();
			for(int i = 0;i < ee.size();i++)
			{
				try{
					c.exec(ee.get(i));
				}
				catch(Exception e)
				{
					e.printStackTrace();
					throw new WebApplicationException("PROBLEM APPLYING FUNCTION "+e.getMessage());
				}
			}
			page++;
		}while(num_results >= page_size);
	}


	//TRANSACTIONS///
	private static ThreadLocal<List<Integer>> current_transaction_id_list = new ThreadLocal<List<Integer>>();
	public void START_TRANSACTION(String tag) throws PersistenceException
	{
		START_TRANSACTION(store,new Date().toString()+": "+tag);
	}

	public static void START_TRANSACTION(PersistentStore store,String tag) throws PersistenceException
	{
		List<Integer> current_tid_list = current_transaction_id_list.get();
		if(current_tid_list == null)
		{
			current_tid_list = new ArrayList<Integer>(4);
			current_transaction_id_list.set(current_tid_list);
		}

		if(current_tid_list.size()== 0)
			current_tid_list.add(store.startTransaction(tag));
		else
			current_tid_list.add(store.startTransaction(current_tid_list.get(current_tid_list.size()-1),tag));

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
		current_tid_list.remove(current_tid_list.size()-1);
		store.rollbackTransaction(c_tid);

	}

	protected static void ROLLBACK_ALL_ACTIVE_TRANSACTIONS(PersistentStore store) throws PersistenceException
	{
		List<Integer> current_tid_list = current_transaction_id_list.get();
		int c_tid_idx = current_tid_list.size() - 1;
		while(c_tid_idx > -1)
		{
			store.rollbackTransaction(current_tid_list.get(c_tid_idx));
			current_tid_list.remove(c_tid_idx);
			c_tid_idx = current_tid_list.size() - 1;
		}
	}


	protected abstract class TRANSACTION
	{
		String tag;
		public TRANSACTION(String name)
		{
			this.tag = name;
		}
		public abstract Object T();
		public Object exec() throws Exception
		{
			try{
				START_TRANSACTION(this.tag);
				Object ret = T();
				COMMIT_TRANSACTION();
				return ret;
			}catch(Exception e)
			{
				try{
					ROLLBACK_ALL_ACTIVE_TRANSACTIONS(store);
					throw e;
				}catch(PersistenceException ee)
				{
					ERROR("FAILED ROLLING BACK TRANSACTION!!!!",ee);
					throw ee;
				}
			}
		}

	}





}
