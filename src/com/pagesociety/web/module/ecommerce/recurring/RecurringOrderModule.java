package com.pagesociety.web.module.ecommerce.recurring;


import java.util.ArrayList;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Types;

import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.PermissionsException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PermissionsModule;
import com.pagesociety.web.module.ecommerce.BillingModule;
import com.pagesociety.web.module.resource.ResourceModule;
import com.pagesociety.web.module.user.UserModule;


public class RecurringOrderModule extends ResourceModule 
{
	
	private static final String SLOT_BILLING_MODULE = "billing-module";
	
	public static final int ORDER_STATUS_INIT 				= 0x0000;
	public static final int ORDER_STATUS_OPEN 				= 0x0001;
	public static final int ORDER_STATUS_CLOSED  			= 0x0002;
	public static final int ORDER_STATUS_LAST_BILL_FAILED 	= 0x0003;

	private BillingModule billing_module;
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);
		/* see notes in super class...this is cheap inheritence */
		super.setResourceEntityName(RECURRING_SKU_RESOURCE_ENTITY);
		billing_module = (BillingModule)getSlot(SLOT_BILLING_MODULE);
	}

	public void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_BILLING_MODULE, com.pagesociety.web.module.ecommerce.BillingModule.class, true);
	}
	

	/////////////////BEGIN  M O D U L E   F U N C T I O N S/////////////////////////////////////////

	
	///RECURRING SKU
	
	@Export //TODO: how do we hook into promotions here.//
	public Entity CreateRecurringSKU(UserApplicationContext uctx,Entity recurring_sku) throws WebApplicationException,PersistenceException
	{
		Entity user 	   	= (Entity)uctx.getUser();
		String product_name 	   = (String)recurring_sku.getAttribute(RECURRING_SKU_FIELD_TITLE);
		String product_description = (String)recurring_sku.getAttribute(RECURRING_SKU_FIELD_DESCRIPTION);
		float  product_price       = (Float)recurring_sku.getAttribute(RECURRING_SKU_FIELD_RECURRING_PRICE);
		int	   billing_period	   = (Integer)recurring_sku.getAttribute(RECURRING_SKU_FIELD_BILLING_PERIOD);
		String catalog_no		   = (String)recurring_sku.getAttribute(RECURRING_SKU_FIELD_CATALOG_NUMBER);
		Entity user_data		   = (Entity)recurring_sku.getAttribute(RECURRING_SKU_FIELD_USER_DATA);
		//TODO: guard
		
		if(user_data != null)
			user_data = GET(user_data.getType(),user_data.getId());
		
		Entity new_recurring_sku = createRecurringSKU(user,product_name, product_description,product_price,billing_period,catalog_no,user_data);
		Entity sku_resource = (Entity)recurring_sku.getAttribute(RECURRING_SKU_FIELD_RESOURCE);
		if(sku_resource != null)
			UpdateRecurringSKUResource(uctx,new_recurring_sku.getId(),sku_resource.getId());

		return new_recurring_sku;
	}
	
	@Export //TODO: how do we hook into promotions here.//
	public Entity CreateRecurringSKU(UserApplicationContext uctx,String product_name,String product_description,float product_price,int billing_period,String catalog_no,String user_data_type,long user_data_id) throws WebApplicationException,PersistenceException
	{
		Entity user 	   	= (Entity)uctx.getUser();
		Entity user_data 	= null;
		if(user_data_type  != null)
			user_data = GET(user_data_type,user_data_id);

		//TODO: guard
		return createRecurringSKU(user,product_name, product_description,product_price,billing_period,catalog_no,user_data);
	}
	
	
	//TODO: do initial billing. set prev and next bill dates,deal with promotions //
	public Entity createRecurringSKU(Entity creator,String product_name,String product_description,float product_price,int billing_period,String catalog_no,Entity user_data) throws PersistenceException
	{
		return NEW(RECURRING_SKU_ENTITY,
				creator,
				RECURRING_SKU_FIELD_TITLE,product_name,
				RECURRING_SKU_FIELD_DESCRIPTION,product_description,
				RECURRING_SKU_FIELD_RECURRING_PRICE,product_price,
				RECURRING_SKU_FIELD_BILLING_PERIOD,billing_period,
				RECURRING_SKU_FIELD_CATALOG_NUMBER,catalog_no,
				RECURRING_SKU_FIELD_USER_DATA,user_data);
	}


	@Export
	public Entity UpdateRecurringSKU(UserApplicationContext uctx,Entity recurring_sku)  throws WebApplicationException,PersistenceException
	{
		Entity user 	   		   = (Entity)uctx.getUser();
		Entity old_recurring_sku   = GET(RECURRING_SKU_ENTITY,recurring_sku.getId());
		
		String product_name 	   = (String)recurring_sku.getAttribute(RECURRING_SKU_FIELD_TITLE);
		String product_description = (String)recurring_sku.getAttribute(RECURRING_SKU_FIELD_DESCRIPTION);
		float  product_price       = (Float)recurring_sku.getAttribute(RECURRING_SKU_FIELD_RECURRING_PRICE);
		int	   billing_period	   = (Integer)recurring_sku.getAttribute(RECURRING_SKU_FIELD_BILLING_PERIOD);
		String catalog_no		   = (String)recurring_sku.getAttribute(RECURRING_SKU_FIELD_CATALOG_NUMBER);
		Entity user_data		   = (Entity)recurring_sku.getAttribute(RECURRING_SKU_FIELD_USER_DATA);
		if(user_data != null)
			user_data = GET(user_data.getType(),user_data.getId());
		return updateRecurringSKU(recurring_sku, product_name, product_description, product_price, billing_period, catalog_no, user_data);
	}
	
	
	@Export
	public Entity UpdateRecurringSKU(UserApplicationContext uctx,long recurring_sku_id,String product_name,String product_description,float product_price,int billing_period,String catalog_no,String user_data_type,long user_data_id) throws WebApplicationException,PersistenceException
	{
		Entity user 	   	= (Entity)uctx.getUser();
		Entity recurring_sku = GET(RECURRING_SKU_ENTITY,recurring_sku_id);

		//TODO: guard
		
		Entity user_data 	= null;
		if(user_data_type  != null)
			user_data = GET(user_data_type,user_data_id);

		Entity sku_resource = (Entity)recurring_sku.getAttribute(RECURRING_SKU_FIELD_RESOURCE);
		if(sku_resource != null)
			UpdateRecurringSKUResource(uctx,recurring_sku.getId(),sku_resource.getId());
		
		return updateRecurringSKU(recurring_sku,product_name, product_description, product_price, billing_period, catalog_no, user_data);
	}
	
	
	public Entity updateRecurringSKU(Entity recurring_sku,String product_name,String product_description,float product_price,int billing_period,String catalog_no,Entity user_data) throws WebApplicationException,PersistenceException
	{
		return UPDATE(recurring_sku,
				RECURRING_SKU_FIELD_TITLE,product_name,
				RECURRING_SKU_FIELD_DESCRIPTION,product_description,
				RECURRING_SKU_FIELD_RECURRING_PRICE,product_price,
				RECURRING_SKU_FIELD_BILLING_PERIOD,billing_period,
				RECURRING_SKU_FIELD_CATALOG_NUMBER,catalog_no,
				RECURRING_SKU_FIELD_USER_DATA,user_data);	
	}

	@Export
	public Entity UpdateRecurringSKUResource(UserApplicationContext uctx,long recurring_sku_id,long recurring_sku_resource_id) throws WebApplicationException,PersistenceException
	{
		Entity recurring_sku 		  = GET(RECURRING_SKU_ENTITY,recurring_sku_id);
		Entity recurring_sku_resource = GET(RECURRING_SKU_RESOURCE_ENTITY,recurring_sku_resource_id);
		
		//TODO:guard
		
		return updateRecurringSKUResource(recurring_sku, recurring_sku_resource);
		
	}
	
	public Entity updateRecurringSKUResource(Entity recurring_sku,Entity recurring_sku_resource) throws WebApplicationException,PersistenceException
	{
		Entity old_resource = (Entity)GET(RECURRING_SKU_ENTITY,recurring_sku.getId()).getAttribute(RECURRING_SKU_FIELD_RESOURCE);
		if(old_resource.equals(recurring_sku_resource))
			return old_resource;
		
		if(old_resource != null)
			deleteResource(old_resource);
		
		return UPDATE(recurring_sku,
					  RECURRING_SKU_FIELD_RESOURCE,recurring_sku_resource);
	}
	
	@Export
	public Entity DeleteRecurringSKU(UserApplicationContext uctx,long recurring_sku_id) throws WebApplicationException,PersistenceException
	{
		Entity user 	   	= (Entity)uctx.getUser();
		Entity recurring_sku = GET(RECURRING_SKU_ENTITY,recurring_sku_id);
		//TODO: guard //	
		return deleteRecurringSKU(recurring_sku);
		
	}
	
	public Entity deleteRecurringSKU(Entity recurring_sku) throws WebApplicationException,PersistenceException
	{
		Entity old_resource = (Entity)recurring_sku.getAttribute(RECURRING_SKU_FIELD_RESOURCE);
		if(old_resource != null)
			deleteResource(old_resource);
		return DELETE(recurring_sku);
	}
	
	
	///////RECURRING ORDER///////////
	
	@Export //TODO: how do we hook into promotions here.//
	public Entity CreateRecurringOrder(UserApplicationContext uctx,long target_user_id,long recurring_sku_id) throws WebApplicationException,PersistenceException
	{
		Entity user 	   = (Entity)uctx.getUser();
		Entity target_user = GET(UserModule.USER_ENTITY,target_user_id);
		
		if(!PermissionsModule.IS_ADMIN(user) || !PermissionsModule.IS_SAME(user, target_user))
			throw new PermissionsException("NO PERMISSION");

		Entity sku = GET(RECURRING_SKU_ENTITY,recurring_sku_id);
		return createRecurringOrder(user,target_user, sku);

	}
	
	//TODO: do initial billing. set prev and next bill dates,deal with promotions //
	public Entity createRecurringOrder(Entity creator,Entity user,Entity sku) throws PersistenceException
	{
		Entity recurring_order =  NEW(RECURRING_ORDER_ENTITY,
									  creator,
									  RECURRING_ORDER_FIELD_SKU,sku,
									  RECURRING_ORDER_FIELD_USER,user,
									  RECURRING_ORDER_FIELD_STATUS,ORDER_STATUS_OPEN,
									  RECURRING_ORDER_FIELD_LAST_BILL_DATE,null,
									  RECURRING_ORDER_FIELD_NEXT_BILL_DATE,null,
									  RECURRING_ORDER_FIELD_PROMOTIONS,new ArrayList<Entity>());
		
		sku = EXPAND(sku);
		//TODO: bill setup fee here //
		return recurring_order;
	}
	
	@Export
	public Entity CloseRecurringOrder(UserApplicationContext uctx,long recurring_order_id) throws WebApplicationException,PersistenceException
	{
		Entity user 	   = (Entity)uctx.getUser();
		Entity recurring_order = GET(RECURRING_ORDER_ENTITY,recurring_order_id);
		
		if(!PermissionsModule.IS_ADMIN(user) || !PermissionsModule.IS_CREATOR(store,user, recurring_order))
			throw new PermissionsException("NO PERMISSION");
	
		return closeRecurringOrder(recurring_order);
	}
	
	public Entity closeRecurringOrder(Entity recurring_order) throws WebApplicationException,PersistenceException
	{
		return updateRecurringOrderStatus(recurring_order, ORDER_STATUS_CLOSED);
	}
	
	public Entity updateRecurringOrderStatus(Entity recurring_order,int status) throws WebApplicationException,PersistenceException
	{
		return UPDATE(recurring_order,
					RECURRING_ORDER_FIELD_STATUS,status);
	}
	
	/////////////////E N D  M O D U L E   F U N C T I O N S/////////////////////////////////////////
		
	public static String RECURRING_SKU_ENTITY 			  		= "RecurringSKU";
	public static String RECURRING_SKU_FIELD_TITLE 			 	= "title";
	public static String RECURRING_SKU_FIELD_DESCRIPTION 		= "description";
	public static String RECURRING_SKU_FIELD_RESOURCE 		  	= "resource";

	public static String RECURRING_SKU_FIELD_RECURRING_PRICE 	= "price";
	public static String RECURRING_SKU_FIELD_CATALOG_NUMBER 	= "catalog_number";/*application provides(optional)*/
	public static String RECURRING_SKU_FIELD_USER_DATA 	  	  	= "data";	/*application provides(optional)*/
	public static String RECURRING_SKU_FIELD_BILLING_PERIOD 	  		= "billing_period";//in days//
	
	public static String RECURRING_SKU_RESOURCE_ENTITY 				= "RecurringSKUResource";
	public static String RECURRING_SKU_RESOURCE_FIELD_CONTENT_TYPE 	= RESOURCE_FIELD_CONTENT_TYPE;
	public static String RECURRING_SKU_RESOURCE_FIELD_SIMPLE_TYPE 	= RESOURCE_FIELD_SIMPLE_TYPE;
	public static String RECURRING_SKU_RESOURCE_FIELD_FILENAME 		= RESOURCE_FIELD_FILENAME;
	public static String RECURRING_SKU_RESOURCE_FIELD_EXTENSION 	= RESOURCE_FIELD_EXTENSION;
	public static String RECURRING_SKU_RESOURCE_FIELD_FILE_SIZE 	= RESOURCE_FIELD_FILE_SIZE;
	public static String RECURRING_SKU_RESOURCE_FIELD_PATH_TOKEN 	= RESOURCE_FIELD_PATH_TOKEN;

	public static String RECURRING_ORDER_ENTITY 				= "RecurringOrder";
	public static String RECURRING_ORDER_FIELD_USER 	    	= "user";
	public static String RECURRING_ORDER_FIELD_SKU	 			= "sku";
	public static String RECURRING_ORDER_FIELD_INITIAL_FEE 	  	= "initial_fee";
	public static String RECURRING_ORDER_FIELD_STATUS 			= "order_status";
	public static String RECURRING_ORDER_FIELD_LAST_BILL_DATE 	= "last_bill_date";
	public static String RECURRING_ORDER_FIELD_NEXT_BILL_DATE 	= "next_bill_date";
	public static String RECURRING_ORDER_FIELD_PROMOTIONS		= "promotions";
	
	public static final String PROMOTION_ENTITY = "Promotion";
	protected void defineEntities(Map<String,Object> config) throws PersistenceException,SyncException
	{
		DEFINE_ENTITY(RECURRING_SKU_ENTITY,
					  RECURRING_SKU_FIELD_TITLE,Types.TYPE_STRING,"",
					  RECURRING_SKU_FIELD_DESCRIPTION,Types.TYPE_STRING,"",
					  RECURRING_SKU_FIELD_RESOURCE,Types.TYPE_REFERENCE, RECURRING_SKU_RESOURCE_ENTITY,null,
					  RECURRING_SKU_FIELD_RECURRING_PRICE,Types.TYPE_FLOAT,0.0f,
					  RECURRING_SKU_FIELD_CATALOG_NUMBER,Types.TYPE_STRING,null,
					  RECURRING_SKU_FIELD_USER_DATA,Types.TYPE_REFERENCE,FieldDefinition.REF_TYPE_UNTYPED_ENTITY,null,
					  RECURRING_SKU_FIELD_BILLING_PERIOD,Types.TYPE_INT,0);
				
		DEFINE_ENTITY(RECURRING_SKU_RESOURCE_ENTITY,
				RECURRING_SKU_RESOURCE_FIELD_CONTENT_TYPE,Types.TYPE_STRING,null,
				RECURRING_SKU_RESOURCE_FIELD_SIMPLE_TYPE,Types.TYPE_STRING,null,
				RECURRING_SKU_RESOURCE_FIELD_FILENAME,Types.TYPE_STRING,null,
				RECURRING_SKU_RESOURCE_FIELD_EXTENSION,Types.TYPE_STRING,null,
				RECURRING_SKU_RESOURCE_FIELD_FILE_SIZE,Types.TYPE_LONG,null,
				RECURRING_SKU_RESOURCE_FIELD_PATH_TOKEN,Types.TYPE_STRING,null);
		
		DEFINE_ENTITY(RECURRING_ORDER_ENTITY,
					  RECURRING_ORDER_FIELD_USER,Types.TYPE_REFERENCE, UserModule.USER_ENTITY,null,
					  RECURRING_ORDER_FIELD_SKU,Types.TYPE_REFERENCE,RECURRING_SKU_ENTITY,null,
					  RECURRING_ORDER_FIELD_INITIAL_FEE,Types.TYPE_FLOAT,0.0f,
					  RECURRING_ORDER_FIELD_STATUS,Types.TYPE_INT,ORDER_STATUS_INIT,
					  RECURRING_ORDER_FIELD_LAST_BILL_DATE,Types.TYPE_DATE,null,
					  RECURRING_ORDER_FIELD_NEXT_BILL_DATE,Types.TYPE_DATE,null,					  
					  RECURRING_ORDER_FIELD_PROMOTIONS,Types.TYPE_REFERENCE|Types.TYPE_ARRAY, PROMOTION_ENTITY,new ArrayList<Entity>());

	}

	public static final String IDX_SYSTEM_BY_TYPE     = "byType";
	public static final String IDX_SYSTEM_BY_USER 	  = "byCreator";	
	
	public static final String IDX_SYSTEM_INSTANCE_BY_USER_BY_CURRENT_FLAG = "byUserByCurrentFlag";
	public static final String IDX_SYSTEM_INSTANCE_BY_USER_BY_SYSTEM 	   = "byUserBySystem";
	
	public static final String IDX_SITE_BY_USER 	  = "byCreator";	
	
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,SyncException
	{
	//	DEFINE_ENTITY_INDEX(SYSTEM_ENTITY, IDX_SYSTEM_BY_TYPE, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, SYSTEM_FIELD_TYPE);
	//	DEFINE_ENTITY_INDEX(SYSTEM_ENTITY, IDX_SYSTEM_BY_USER, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, FIELD_CREATOR);
	//	DEFINE_ENTITY_INDEX(SYSTEM_INSTANCE_ENTITY, IDX_SYSTEM_INSTANCE_BY_USER_BY_CURRENT_FLAG, EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, FIELD_CREATOR,SYSTEM_INSTANCE_FIELD_CURRENT_FLAG);
	//	DEFINE_ENTITY_INDEX(SYSTEM_INSTANCE_ENTITY, IDX_SYSTEM_INSTANCE_BY_USER_BY_SYSTEM, EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, FIELD_CREATOR,SYSTEM_INSTANCE_FIELD_SYSTEM);
	//	DEFINE_ENTITY_INDEX(SITE_ENTITY, IDX_SITE_BY_USER, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, FIELD_CREATOR);
	}
}
