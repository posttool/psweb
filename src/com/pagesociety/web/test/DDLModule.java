package com.pagesociety.web.test;

import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.EntityDefinition;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.PersistentStore;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.Module;

public class DDLModule extends Module
{
	private PersistentStore _store;

	public DDLModule()
	{
	}

	public void init(WebApplication app, Map<String,Object> config)
	{
//		_store = app.getStore("LimeLifeStore");
	}

	@Export
	public List<EntityDefinition> getEntityDefinitions(UserApplicationContext u) throws PersistenceException
	{
		List<EntityDefinition> defs = _store.getEntityDefinitions();
		return defs;
	}
	
	@Export
	public List<EntityDefinition> list(UserApplicationContext u) throws PersistenceException
	{
		List<EntityDefinition> defs = _store.getEntityDefinitions();
		return defs;
	}

//	public List<EntityIndexDefinition> getIndexDefinitions() throws PersistenceException
//	{
//		return _store.getEntityIndexDefinitions();
//	}

	public List<EntityIndex> getEntityIndices(String entity) throws PersistenceException
	{
		return _store.getEntityIndices(entity);
	}

	public EntityDefinition addEntityDefinition(String entity_name)
			throws PersistenceException
	{
		EntityDefinition def = new EntityDefinition(entity_name);
		_store.addEntityDefinition(def);
		return def;
	}

	public FieldDefinition addField(String entity_name, String field_name,
			int field_type, String reference_type) throws PersistenceException
	{
		FieldDefinition field = new FieldDefinition(field_name, field_type, reference_type);
//		_store.addEntityField(entity_name, field, null);
		return field;
	}

//	public EntityIndex addIndex(String entity_name, String field_name, String index_type,
//			String index_name, Map<String, Object> attributes)
//			throws PersistenceException
//	{
//		_store.addEntityIndex(entity_name, field_name, index_type, index_name, attributes);
//		List<EntityIndex> indices = _store.getEntityIndices(entity_name);
//		return indices.get(indices.size() - 1);
//	}

	public boolean addIndex(String entity_name, String[] field_names, String index_type,
			String index_name, Map<String, String> attributes)
			throws PersistenceException
	{
		// _store.addEntityIndex(entity_name, field_names, index_type,
		// index_name, attributes);
		return false;
	}

	public boolean deleteEntityDefinition(String entity_name) throws PersistenceException
	{
		_store.deleteEntityDefinition(entity_name);
		return true;
	}

	public boolean deleteField(String entity_name, String field_name)
			throws PersistenceException
	{
		_store.deleteEntityField(entity_name, field_name);
		return true;
	}

	public boolean deleteIndex(String entity_name, String index_name)
			throws PersistenceException
	{
		_store.deleteEntityIndex(entity_name, index_name);
		throw new PersistenceException("CANT DELETE INDICES YET");
	}

	public EntityDefinition renameEntityDefinition(String entity_name, String new_name)
			throws PersistenceException
	{
		_store.renameEntityDefinition(entity_name, new_name);
		return new EntityDefinition(new_name);// should get the updated one
		// from the store
	}

	public FieldDefinition renameField(String entity_name, String old_name,
			String new_name) throws PersistenceException
	{
		_store.renameEntityField(entity_name, old_name, new_name);
		return _store.getEntityDefinition(entity_name).getField(new_name);
	}

	public boolean renameIndex(String entity_name, String old_name, String new_name)
			throws PersistenceException
	{
		_store.renameEntityIndex(entity_name, old_name, new_name);
		return true;
	}
}
