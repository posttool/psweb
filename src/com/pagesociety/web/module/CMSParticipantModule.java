
package com.pagesociety.web.module;

import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityDefinition;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;


public class CMSParticipantModule extends WebStoreModule 
{
	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);
	}
	
	
	/* 
		Fill Fields use FILL_ALL_FIELDS to fill all or FILL_NO_FIELDS to fill none. 
		Mask fields are ignored in the context of no fill fields right now
		
		For between type queries wrap your two values in an object array
	  	i.e. new Object[]{top_val,bottom_val} for a single field between
	   	new Object[]{Query.l("a","b"),Query.l("z","x")} for multi field.
	   	Desc betweens are top_val,bottom_val ascending betweens are bottm_val,top_val.
	*/
	

	public PagingQueryResult doBrowseAll(String type,int offset, int page_size,String order_by,int order_by_order, String[] fill_fields,String[] mask_fields,boolean cache_results) throws PersistenceException,WebApplicationException
	{
		VERIFY_MODULE_IS_DEFINER_OF_ENTITY_TYPE(type);
		return BROWSE(type,Query.PRIMARY_IDX,Query.EQ,Query.VAL_GLOB,offset,page_size,order_by,order_by_order,fill_fields,mask_fields,cache_results);
	}


	public EntityDefinition doGetDefinition(String type) throws PersistenceException,WebApplicationException
	{
		VERIFY_MODULE_IS_DEFINER_OF_ENTITY_TYPE(type);
		return store.getEntityDefinition(type);
	}
	
	public Entity doCreate(String type,Entity creator,Object... args) throws PersistenceException,WebApplicationException
	{
		VERIFY_MODULE_IS_DEFINER_OF_ENTITY_TYPE(type);
	 	return NEW(type,creator,args);	
	}
	
	public Entity doUpdate(Entity e,Object ...args) throws PersistenceException,WebApplicationException
	{
		VERIFY_MODULE_IS_DEFINER_OF_ENTITY_TYPE(e.getType());
	 	return UPDATE(e,args);
	}
	
	public Entity doGet(String type,long id) throws PersistenceException, WebApplicationException
	{
		VERIFY_MODULE_IS_DEFINER_OF_ENTITY_TYPE(type);	
		return GET(type,id);
	}
	
	public Entity doDelete(Entity e) throws PersistenceException,WebApplicationException
	{
		VERIFY_MODULE_IS_DEFINER_OF_ENTITY_TYPE(e.getType());
		return DELETE(e);
	}
	
	
}
