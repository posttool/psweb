package com.pagesociety.web.module.ecommerce.recurring;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;

import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.PermissionsException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.PermissionsModule;
import com.pagesociety.web.module.ecommerce.billing.BillingModule;
import com.pagesociety.web.module.ecommerce.gateway.BillingGatewayException;
import com.pagesociety.web.module.ecommerce.gateway.IBillingGateway;
import com.pagesociety.web.module.ecommerce.promo.PromotionModule;
import com.pagesociety.web.module.email.IEmailModule;
import com.pagesociety.web.module.logger.LoggerModule;
import com.pagesociety.web.module.notification.SystemNotificationModule;
import com.pagesociety.web.module.resource.ResourceModule;
import com.pagesociety.web.module.user.UserModule;


public class RecurringOrderModule extends ResourceModule 
{
	
	protected static final String SLOT_USER_MODULE  		  = "user-module";
	private static final String SLOT_BILLING_MODULE 		  = "billing-module";
	private static final String SLOT_EMAIL_MODULE 		  	  = "email-module";
	private static final String SLOT_LOGGER_MODULE 		  	  = "logger-module";
	private static final String SLOT_NOTIFICATION_MODULE 	  = "notification-module";
	private static final String SLOT_PROMOTION_MODULE 	  	  = "promotion-module";
	private static final String PARAM_BILLING_THREAD_INTERVAL = "billing-thread-interval";
	
	public static final int ORDER_STATUS_INIT 							= 0x0000;
	public static final int ORDER_STATUS_OPEN 							= 0x0001;
	public static final int ORDER_STATUS_CLOSED  						= 0x0002;
	public static final int ORDER_STATUS_INITIAL_BILL_FAILED 			= 0x0004;
	public static final int ORDER_STATUS_LAST_MONTHLY_BILL_FAILED 		= 0x0008;
	public static final int ORDER_STATUS_NO_PREFERRED_BILLING_RECORD 	= 0x0010;

	protected UserModule user_module;
	protected BillingModule billing_module;
	protected IBillingGateway billing_gateway;
	protected IEmailModule email_module;
	protected LoggerModule logger_module;
	protected SystemNotificationModule notification_module;
	protected PromotionModule promotion_module;
	private long billing_thread_interval;
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);
		/* see notes in super class...this is cheap inheritence */
		super.setResourceEntityName(RECURRING_SKU_RESOURCE_ENTITY);
		user_module = (UserModule)getSlot(SLOT_USER_MODULE);
		billing_module  		= (BillingModule)getSlot(SLOT_BILLING_MODULE);
		billing_gateway 		= billing_module.getBillingGateway(); 
		billing_thread_interval = Long.parseLong(GET_REQUIRED_CONFIG_PARAM(PARAM_BILLING_THREAD_INTERVAL, config));
		email_module 		= (IEmailModule)getSlot(SLOT_EMAIL_MODULE);
		logger_module 		= (LoggerModule)getSlot(SLOT_LOGGER_MODULE);
		notification_module = (SystemNotificationModule)getSlot(SLOT_NOTIFICATION_MODULE);
		promotion_module = (PromotionModule)getSlot(SLOT_PROMOTION_MODULE);
	
	}
	
	public void loadbang(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		start_billing_thread();	
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_BILLING_MODULE, com.pagesociety.web.module.ecommerce.billing.BillingModule.class, true);
		DEFINE_SLOT(SLOT_EMAIL_MODULE, com.pagesociety.web.module.email.IEmailModule.class, true);
		DEFINE_SLOT(SLOT_LOGGER_MODULE, com.pagesociety.web.module.logger.LoggerModule.class, true);
		DEFINE_SLOT(SLOT_NOTIFICATION_MODULE, com.pagesociety.web.module.notification.SystemNotificationModule.class, true);
		DEFINE_SLOT(SLOT_PROMOTION_MODULE, com.pagesociety.web.module.ecommerce.promo.PromotionModule.class, true);
		DEFINE_SLOT(SLOT_USER_MODULE, com.pagesociety.web.module.user.UserModule.class, true);
	}
	

	/////////////////BEGIN  M O D U L E   F U N C T I O N S/////////////////////////////////////////

	
	///RECURRING SKU
	@Export
	public PagingQueryResult GetActiveRecurringSKUs(UserApplicationContext uctx,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		//Potential guard here...but active ones shoud be public or at least have the person logged in so no big deal//
		return getRecurringSKUs(offset, page_size,RECURRING_SKU_CATALOG_STATE_ACTIVE,null);
	}
	
	@Export
	public PagingQueryResult GetAllRecurringSKUs(UserApplicationContext uctx,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		if(!PermissionsModule.IS_ADMIN(user))
			throw new PermissionsException("NO PERMISSION");
		
		return getRecurringSKUs(offset, page_size,Query.VAL_GLOB,null);
	}
	
	public PagingQueryResult getRecurringSKUs(int offset,int page_size,Object state,String order_by) throws PersistenceException
	{
		Query q = new Query(RECURRING_SKU_ENTITY);
		q.idx(IDX_RECURRING_SKU_BY_CATALOG_STATE);
		q.eq(state);
		
		if(order_by != null)
			q.orderBy(order_by);
		q.offset(offset);
		q.pageSize(page_size);

		return PAGING_QUERY(q);
	}

	public Entity getRecurringSKUByCatalogNumber(String catalog_num) throws PersistenceException
	{
		Query q = new Query(RECURRING_SKU_ENTITY);
		q.idx(IDX_RECURRING_SKU_BY_CATALOG_NUMBER_BY_CATALOG_STATE);
		q.eq(q.list(catalog_num,RECURRING_SKU_CATALOG_STATE_ACTIVE));
		QueryResult result = QUERY(q);
		if(result.size() == 0)
			return null;
		else if(result.size() > 0)
			WARNING("THERE ARE MORE THAN ONE RECURRING SKUS WITH CATALOG NUM: "+catalog_num);
		return result.getEntities().get(0);
	}

	
	@Export
	public Entity CreateRecurringSKU(UserApplicationContext uctx,Entity recurring_sku) throws WebApplicationException,PersistenceException
	{
		VALIDATE_TYPE(RECURRING_SKU_ENTITY, recurring_sku);
		
		String product_name 	   = (String)recurring_sku.getAttribute(RECURRING_SKU_FIELD_TITLE);
		String product_description = (String)recurring_sku.getAttribute(RECURRING_SKU_FIELD_DESCRIPTION);
		float  initial_fee         = (Float)recurring_sku.getAttribute(RECURRING_SKU_FIELD_INITIAL_FEE);
		float  product_price       = (Float)recurring_sku.getAttribute(RECURRING_SKU_FIELD_RECURRING_PRICE);
		String  catalog_no		   = (String)recurring_sku.getAttribute(RECURRING_SKU_FIELD_CATALOG_NUMBER);
		Integer catalog_state	   = (Integer)recurring_sku.getAttribute(RECURRING_SKU_FIELD_CATALOG_STATE);
		Entity user_data		   = (Entity)recurring_sku.getAttribute(RECURRING_SKU_FIELD_USER_DATA);
		
		long 	recurring_sku_resource_id 	= 0;
		String 	user_data_type		   		= null;
		long 	user_data_id			   	= 0;
		if(user_data != null)
		{
			user_data_type			= user_data.getType();
			user_data_id			= user_data.getId();
		}

		Entity sku_resource = (Entity)recurring_sku.getAttribute(RECURRING_SKU_FIELD_RESOURCE);
		if(sku_resource != null)
		{
			VALIDATE_TYPE(RECURRING_SKU_RESOURCE_ENTITY, sku_resource);
			recurring_sku_resource_id = sku_resource.getId();
		}
		
		if(catalog_state == null)
			catalog_state = RECURRING_SKU_CATALOG_STATE_ACTIVE;
		
		return CreateRecurringSKU(uctx,product_name, product_description,initial_fee,product_price,catalog_no,recurring_sku_resource_id,user_data_type,user_data_id,catalog_state);

	}
	
	@Export 
	public Entity CreateRecurringSKU(UserApplicationContext uctx,String product_name,String product_description,float initial_fee,float product_price,String catalog_no,long recurring_sku_resource_id,String user_data_type,long user_data_id,int catalog_state) throws WebApplicationException,PersistenceException
	{
		Entity user 	   	= (Entity)uctx.getUser();
		
		if(!PermissionsModule.IS_ADMIN(user))
			throw new PermissionsException("NO PERMISSION");
		
		Entity user_data 	= null;
		if(user_data_type  != null)
			user_data = GET(user_data_type,user_data_id);
		
		Entity recurring_sku_resource = null;
		if(recurring_sku_resource_id != 0)//0 for a ref pointer means null//
			recurring_sku_resource = GET(RECURRING_SKU_ENTITY,recurring_sku_resource_id);
			
		return createRecurringSKU(user,product_name, product_description,initial_fee,product_price,catalog_no,recurring_sku_resource,user_data,catalog_state);
	}
	
	
	public Entity createRecurringSKU(Entity creator,String product_name,String product_description,float initial_fee,float product_price,String catalog_no,Entity recurring_sku_resource,Entity user_data,int catalog_state) throws PersistenceException
	{
		return NEW(RECURRING_SKU_ENTITY,
				creator,
				RECURRING_SKU_FIELD_TITLE,product_name,
				RECURRING_SKU_FIELD_DESCRIPTION,product_description,
				RECURRING_SKU_FIELD_INITIAL_FEE,initial_fee,
				RECURRING_SKU_FIELD_RECURRING_PRICE,product_price,
				RECURRING_SKU_FIELD_CATALOG_NUMBER,catalog_no,
				RECURRING_SKU_FIELD_RESOURCE,recurring_sku_resource,
				RECURRING_SKU_FIELD_USER_DATA,user_data,
				RECURRING_SKU_FIELD_CATALOG_STATE,catalog_state);
	}


	@Export
	public Entity UpdateRecurringSKU(UserApplicationContext uctx,Entity recurring_sku)  throws WebApplicationException,PersistenceException
	{

		VALIDATE_TYPE(RECURRING_SKU_ENTITY, recurring_sku);
		
		String product_name 	   = (String)recurring_sku.getAttribute(RECURRING_SKU_FIELD_TITLE);
		String product_description = (String)recurring_sku.getAttribute(RECURRING_SKU_FIELD_DESCRIPTION);
		float  product_price       = (Float)recurring_sku.getAttribute(RECURRING_SKU_FIELD_RECURRING_PRICE);
		String catalog_no		   = (String)recurring_sku.getAttribute(RECURRING_SKU_FIELD_CATALOG_NUMBER);
		Entity user_data		   = (Entity)recurring_sku.getAttribute(RECURRING_SKU_FIELD_USER_DATA);
		Integer catalog_state	   = (Integer)recurring_sku.getAttribute(RECURRING_SKU_FIELD_CATALOG_STATE);
		
		long 	recurring_sku_resource_id 	= 0;
		String 	user_data_type		   		= null;
		long 	user_data_id			   	= 0;
		if(user_data != null)
		{
			user_data_type			= user_data.getType();
			user_data_id			= user_data.getId();
		}

		Entity sku_resource = (Entity)recurring_sku.getAttribute(RECURRING_SKU_FIELD_RESOURCE);
		if(sku_resource != null)
		{
			VALIDATE_TYPE(RECURRING_SKU_RESOURCE_ENTITY, sku_resource);
			recurring_sku_resource_id = sku_resource.getId();
		}

		if(catalog_state == null)
			catalog_state = RECURRING_SKU_CATALOG_STATE_ACTIVE;
		
		return UpdateRecurringSKU(uctx,recurring_sku.getId(), product_name, product_description, product_price,catalog_no,recurring_sku_resource_id, user_data_type,user_data_id,catalog_state);
	}
	
	
	@Export
	public Entity UpdateRecurringSKU(UserApplicationContext uctx,long recurring_sku_id,String product_name,String product_description,float product_price,String catalog_no,long recurring_sku_resource_id,String user_data_type,long user_data_id,int catalog_state) throws WebApplicationException,PersistenceException
	{
		Entity user 	   	= (Entity)uctx.getUser();
		Entity recurring_sku = GET(RECURRING_SKU_ENTITY,recurring_sku_id);

		if(!PermissionsModule.IS_ADMIN(user))
			throw new PermissionsException("NO PERMISSION");
		
		Entity user_data 	= null;
		if(user_data_type  != null)
			user_data = GET(user_data_type,user_data_id);
		
		Entity recurring_sku_resource = null;
		if(recurring_sku_resource_id != 0)//0 for a ref pointer means null//
			recurring_sku_resource = GET(RECURRING_SKU_ENTITY,recurring_sku_resource_id);
		
		return updateRecurringSKU(recurring_sku,product_name, product_description, product_price,catalog_no, recurring_sku_resource,user_data,catalog_state);
	}
	
	
	public Entity updateRecurringSKU(Entity recurring_sku,String product_name,String product_description,float product_price,String catalog_no,Entity recurring_sku_resource,Entity user_data,int catalog_state) throws WebApplicationException,PersistenceException
	{
		Entity old_resource = (Entity)GET(RECURRING_SKU_ENTITY,recurring_sku.getId()).getAttribute(RECURRING_SKU_FIELD_RESOURCE);
		if(old_resource != null && !old_resource.equals(recurring_sku_resource))
			deleteResource(old_resource);
	
		return UPDATE(recurring_sku,
				RECURRING_SKU_FIELD_TITLE,product_name,
				RECURRING_SKU_FIELD_DESCRIPTION,product_description,
				RECURRING_SKU_FIELD_RECURRING_PRICE,product_price,
				RECURRING_SKU_FIELD_CATALOG_NUMBER,catalog_no,
				RECURRING_SKU_FIELD_RESOURCE,recurring_sku_resource,
				RECURRING_SKU_FIELD_USER_DATA,user_data,
				RECURRING_SKU_FIELD_CATALOG_STATE,catalog_state);	
	}
	
	@Export
	public Entity DeleteRecurringSKU(UserApplicationContext uctx,long recurring_sku_id) throws WebApplicationException,PersistenceException
	{
		Entity user 	   	= (Entity)uctx.getUser();
		Entity recurring_sku = GET(RECURRING_SKU_ENTITY,recurring_sku_id);
		if(!PermissionsModule.IS_ADMIN(user))
			throw new PermissionsException("NO PERMISSION");

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
	public Entity CreateRecurringOrder(UserApplicationContext uctx,long target_user_id,Entity recurring_order) throws WebApplicationException,PersistenceException
	{
		List<Entity> recurring_skus 	= (List<Entity>)recurring_order.getAttribute(RECURRING_ORDER_FIELD_SKUS);
		List<Entity> promotions = (List<Entity>)recurring_order.getAttribute(RECURRING_ORDER_FIELD_PROMOTIONS);
		
		VALIDATE_TYPE_LIST(RECURRING_SKU_ENTITY, recurring_skus);
		VALIDATE_TYPE_LIST(PROMOTION_ENTITY, promotions);
		int recurring_unit   = (Integer)recurring_order.getAttribute(RECURRING_ORDER_FIELD_RECURRING_UNIT);
		int recurring_period = (Integer)recurring_order.getAttribute(RECURRING_ORDER_FIELD_RECURRING_PERIOD);
		
		return CreateRecurringOrder(uctx,target_user_id,recurring_unit,recurring_period,ENTITIES_TO_IDS(recurring_skus),ENTITIES_TO_IDS(promotions));
	}
	
	@Export 
	public Entity CreateRecurringOrder(UserApplicationContext uctx,long target_user_id,int recurring_unit,int recurring_period,List<Long> recurring_sku_ids,List<Long> promotion_ids) throws WebApplicationException,PersistenceException
	{
		Entity user 	   = (Entity)uctx.getUser();
		Entity target_user = user_module.getUser(target_user_id);
		
		if(!PermissionsModule.IS_ADMIN(user) && !PermissionsModule.IS_SAME(user, target_user))
			throw new PermissionsException("NO PERMISSION");

		List <Entity>skus 		= IDS_TO_ENTITIES(RECURRING_SKU_ENTITY,recurring_sku_ids);
		List<Entity> promotions = IDS_TO_ENTITIES(PROMOTION_ENTITY, promotion_ids);
		
	
		return createRecurringOrder(user,target_user,recurring_unit,recurring_period,skus,promotions);

	}
	
	public Entity createRecurringOrder(Entity creator,Entity user,int recurring_unit,int recurring_period,List<Entity> skus,List<Entity> promotions) throws PersistenceException
	{

		Entity recurring_order =  NEW(RECURRING_ORDER_ENTITY,
									  creator,
									  RECURRING_ORDER_FIELD_SKUS,skus,
									  RECURRING_ORDER_FIELD_USER,user,
									  RECURRING_ORDER_FIELD_STATUS,ORDER_STATUS_INIT,
									  RECURRING_ORDER_FIELD_LAST_BILL_DATE,null,
									  RECURRING_ORDER_FIELD_NEXT_BILL_DATE,new Date(),
									  RECURRING_ORDER_FIELD_PROMOTIONS,new ArrayList<Entity>(),
									  RECURRING_ORDER_FIELD_RECURRING_UNIT,recurring_unit,
									  RECURRING_ORDER_FIELD_RECURRING_PERIOD,recurring_period);

		MODULE_LOG(0,"CREATED RECURRING ORDER "+recurring_order.getId()+" OK");
		return recurring_order;
	}
	

	@Export
	public Entity DeleteRecurringOrder(UserApplicationContext uctx,long recurring_order_id) throws WebApplicationException,PersistenceException
	{
		Entity user 	   = (Entity)uctx.getUser();
		Entity recurring_order = GET(RECURRING_ORDER_ENTITY,recurring_order_id);
		
		if(!PermissionsModule.IS_ADMIN(user))
			throw new PermissionsException("NO PERMISSION");
	
		return deleteRecurringOrder(recurring_order);
	}
	
	public Entity deleteRecurringOrder(Entity recurring_order) throws WebApplicationException,PersistenceException
	{
		return DELETE(recurring_order);
	}

	private void send_system_alert_notification(Entity user,String notification_text) throws PersistenceException
	{
		notification_module.createAlertNotificationForUser(null, user, notification_text);
	}
	
	public Entity openRecurringOrder(Entity recurring_order) throws WebApplicationException,PersistenceException
	{
		if(!billing_module.isConfigured())
			throw new WebApplicationException("ERROR:CANNOT OPEN ORDER DUE TO BILLING MODULE NOT BEING CONFIGURED.MAKE SURE ENCRYPTION MODULE IS CONFIGURED.");
		
		recurring_order = EXPAND(recurring_order);
		FILL_REFS(recurring_order);
		MODULE_LOG(0,"OPENING RECURRING ORDER "+recurring_order.getId());
		int status 		= (Integer)recurring_order.getAttribute(RECURRING_ORDER_FIELD_STATUS);
		
		Entity order_user     = (Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER);
		Entity billing_record = billing_module.getPreferredBillingRecord(order_user);
		
		if(billing_record == null)
		{
			List<Entity> promotion_instances = (List<Entity>)recurring_order.getAttribute(RECURRING_ORDER_FIELD_PROMOTIONS);
			if(promotion_instances != null)
			{
				for(int i = 0;i< promotion_instances.size();i++)
				{
					Entity promotion_instance = promotion_instances.get(i);
					if(promotion_module.isFreeForLifePromotion(promotion_instance))
						break;
				}
			}
			else
			{
				updateRecurringOrderStatus(recurring_order, ORDER_STATUS_NO_PREFERRED_BILLING_RECORD);
				MODULE_LOG("NO PREFERRED BILLING RECORD FOR USER:"+order_user+" WHEN TRYING TO OPEN RECURRING ORDER "+recurring_order.getId());
				send_system_alert_notification(order_user,"There was a problem with your order. Please make sure you have a valid billing record. We don't seem to have one on file for you.");
				send_billing_failed_email(recurring_order, "NO PREFERRED BILLING RECORD. PLEASE LOGIN AND UPDATE BILLING INFORMATION.");
				return recurring_order;
			}
		}
		
		try{
			promotion_module.applyPromotions(recurring_order);
		}catch(Exception e)
		{
			ERROR(e);
			MODULE_LOG("FAILED TO APPLY PROMOTION FOR MONTHLY BILLING FOR ORDER "+recurring_order);
		}
		switch(status)
		{
			case ORDER_STATUS_INIT:
				//TODO: log order created??
			case ORDER_STATUS_INITIAL_BILL_FAILED:
				try{
					do_initial_fee_billing(recurring_order,billing_record);
				}catch(BillingGatewayException bge)
				{
					log_order_init_bill_failed(recurring_order, bge.getAmount());
					updateRecurringOrderStatus(recurring_order, ORDER_STATUS_INITIAL_BILL_FAILED);	
					MODULE_LOG(0,"ERROR: RECURRING ORDER "+recurring_order.getId()+" INITIAL BILLING FAILED.");
					send_system_alert_notification(order_user,"There was a problem with your order. Please make sure you have a valid billing record.");
					send_billing_failed_email(recurring_order, null);
					return recurring_order;
				}
				try{
					do_monthly_billing(recurring_order,billing_record);				
				}catch(BillingGatewayException bge2)
				{
					log_order_monthly_bill_failed(recurring_order, bge2.getAmount());
					updateRecurringOrderStatus(recurring_order, ORDER_STATUS_LAST_MONTHLY_BILL_FAILED);	
					send_system_alert_notification(order_user,"There was a problem billing your account. Please make sure you have a valid billing record.");
					send_billing_failed_email(recurring_order, null);
					MODULE_LOG(0,"ERROR: RECURRING ORDER "+recurring_order.getId()+" FIRST MONTHLY BILLING FAILED.");
					return recurring_order;
				}
				updateRecurringOrderStatus(recurring_order, ORDER_STATUS_OPEN);
				MODULE_LOG(0,"OPENED RECURRING ORDER "+recurring_order.getId()+" OK");
				break;
				
			case ORDER_STATUS_LAST_MONTHLY_BILL_FAILED://just reopen it. probaby need to see how much time elapsed.

				try{
					do_monthly_billing(recurring_order,billing_record);
				}catch(BillingGatewayException bge2)
				{
					log_order_monthly_bill_failed(recurring_order, bge2.getAmount());
					updateRecurringOrderStatus(recurring_order, ORDER_STATUS_LAST_MONTHLY_BILL_FAILED);	
					send_system_alert_notification(order_user,"There was a problem billing your account. Please make sure you have a valid billing record.");
					send_billing_failed_email(recurring_order, "");
					MODULE_LOG(0,"ERROR: RECURRING ORDER "+recurring_order.getId()+" FIRST MONTHLY BILLING FAILED");
					return recurring_order;
				}
				updateRecurringOrderStatus(recurring_order, ORDER_STATUS_OPEN);
				MODULE_LOG(0,"OPENED RECURRING ORDER "+recurring_order.getId()+" OK");
				break;
				
				
		}
		
		return recurring_order; 
	}
	
	@Export
	public Entity CloseRecurringOrder(UserApplicationContext uctx,long recurring_order_id) throws WebApplicationException,PersistenceException
	{
		Entity user 	   		= (Entity)uctx.getUser();
		Entity recurring_order 	= GET(RECURRING_ORDER_ENTITY,recurring_order_id);
		
		if(!PermissionsModule.IS_ADMIN(user) && !PermissionsModule.IS_CREATOR(store,user, recurring_order))
			throw new PermissionsException("NO PERMISSION");
	

		return closeRecurringOrder(recurring_order);
	}
	
	public Entity closeRecurringOrder(Entity recurring_order) throws WebApplicationException,PersistenceException
	{
		MODULE_LOG(0,"CLOSING RECURRING ORDER "+recurring_order.getId()+" OK");
		//TODO: log transaction
		return updateRecurringOrderStatus(recurring_order, ORDER_STATUS_CLOSED);
	}
	
	public Entity updateRecurringOrderStatus(Entity recurring_order,int status) throws PersistenceException
	{
		int old_status = (Integer)recurring_order.getAttribute(RECURRING_ORDER_FIELD_STATUS);
		recurring_order =  UPDATE(recurring_order,
						RECURRING_ORDER_FIELD_STATUS,status);
		
		//log the status change//
		switch(status)
		{
			case ORDER_STATUS_INIT:
				break;
			case ORDER_STATUS_OPEN:
				log_order_opened(recurring_order);
				break;
			case ORDER_STATUS_CLOSED:
			case ORDER_STATUS_INITIAL_BILL_FAILED:
			case ORDER_STATUS_LAST_MONTHLY_BILL_FAILED:
			case ORDER_STATUS_NO_PREFERRED_BILLING_RECORD:
				log_order_suspended(recurring_order, old_status);
				break;
		}
			
		return recurring_order;	
	}
	
	
	@Export
	public PagingQueryResult GetRecurringOrdersByStatus(UserApplicationContext uctx,int status,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		if(!PermissionsModule.IS_ADMIN(user))
			throw new PermissionsException("NO PERMISSION");
	
		return getRecurringOrdersByStatus(status, page_size, offset);
	}
	
	public PagingQueryResult getRecurringOrdersByStatus(int status,int page_size,int offset) throws WebApplicationException,PersistenceException
	{
		Query q = new Query(IDX_RECURRING_ORDER_BY_STATUS);
		q.eq(status);
		q.offset(offset);
		q.pageSize(page_size);
		return PAGING_QUERY(q);
	}
	
	@Export
	public PagingQueryResult GetRecurringOrdersByUser(UserApplicationContext uctx,long user_id,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity target_user = user_module.getUser(user_id);

		
		if(!PermissionsModule.IS_ADMIN(user) && !PermissionsModule.IS_SAME(user, target_user))
			throw new PermissionsException("NO PERMISSION");
			
		return getRecurringOrdersByUser(target_user,offset,page_size);
	}
	
	
	public PagingQueryResult getRecurringOrdersByUser(Entity user,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Query q = new Query(IDX_RECURRING_ORDER_BY_USER);
		q.eq(user);
		q.offset(offset);
		q.pageSize(page_size);
		return PAGING_QUERY(q);
	}
	
	@Export
	public PagingQueryResult GetTransactionHistoryByUser(UserApplicationContext uctx,long user_id,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity target_user = user_module.getUser(user_id);
		
		if(!PermissionsModule.IS_ADMIN(user) && !PermissionsModule.IS_SAME(user, target_user))
			throw new PermissionsException("NO PERMISSION");
			
		return getTransactionHistoryByUser(target_user,offset,page_size);
	}
	
	
	public PagingQueryResult getTransactionHistoryByUser(Entity user,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Query q = logger_module.getLogMessagesByUserQ(user);
		q.offset(offset);
		q.pageSize(page_size);
		q.orderBy(FIELD_LAST_MODIFIED, Query.DESC);
		return PAGING_QUERY(q);
	}
	
	
	/////////////////E N D  M O D U L E   F U N C T I O N S/////////////////////////////////////////
	
	private void do_initial_fee_billing(Entity recurring_order,Entity billing_record) throws PersistenceException,BillingGatewayException
	{
		List<Entity> skus 	 = (List<Entity>)recurring_order.getAttribute(RECURRING_ORDER_FIELD_SKUS);
		float initial_fee = 0f;
		for(int i = 0;i < skus.size();i++)
		{
			Entity sku = skus.get(i);
			initial_fee+= (Float)sku.getAttribute(RECURRING_SKU_FIELD_INITIAL_FEE);
		}
		if(initial_fee == 0)
			return;

		billing_gateway.doSale(billing_record, initial_fee);
		if(initial_fee != 0)
			log_order_init_bill_ok(recurring_order, initial_fee);
		
	}
	
	private void do_monthly_billing(Entity recurring_order,Entity billing_record) throws PersistenceException,BillingGatewayException
	{
		Date next_bill_date = (Date)recurring_order.getAttribute(RECURRING_ORDER_FIELD_NEXT_BILL_DATE);
		Date now     = new Date();
		if(now.getTime() < next_bill_date.getTime())
		{
			//cant bill a date before the next bill date
			MODULE_LOG(0,"WARNING: SKIPPING BILLING RECURRING ORDER BECAUSE NEXT BILL DATE IS IN THE FUTURE.RECURRING ORDER: "+recurring_order.getId());
			return;
		}
		
		List<Entity> skus 	 = (List<Entity>)recurring_order.getAttribute(RECURRING_ORDER_FIELD_SKUS);
		float amount = 0f;
		for(int i = 0;i < skus.size();i++)
		{
			Entity sku = skus.get(i);
			amount    += (Float)sku.getAttribute(RECURRING_SKU_FIELD_RECURRING_PRICE);
		}
		
		if(amount > 0)
		{
			billing_gateway.doSale(billing_record, amount);
			log_order_monthly_bill_ok(recurring_order, amount);
		}
		else
		{
			log_order_monthly_bill_ok(recurring_order, 0);
		}

		
		recurring_order.setAttribute(RECURRING_ORDER_FIELD_LAST_BILL_DATE, now);
		recurring_order.setAttribute(RECURRING_ORDER_FIELD_NEXT_BILL_DATE, calculate_next_bill_date(recurring_order,now));
		SAVE_ENTITY(recurring_order);
	}


	private Date calculate_next_bill_date(Entity recurring_order, Date now)
	{
		Calendar c1 = Calendar.getInstance(); 
		c1.setTime(now);
		int recurring_unit 		= (Integer)recurring_order.getAttribute(RECURRING_ORDER_FIELD_RECURRING_UNIT);
		int recurring_period 	= (Integer)recurring_order.getAttribute(RECURRING_ORDER_FIELD_RECURRING_PERIOD);
		c1.add(recurring_unit,recurring_period);
		return c1.getTime();
	}
	
	
	private void start_billing_thread()
	{
		Thread t = new Thread(){
			public void run()
			{
				while(true)
				{
					
					MODULE_LOG(0,"\nSTARTING BILLING CYCLE.");
					billing_thread_run();
					MODULE_LOG(0,"BILLING CYCLE COMPLETE.\n");
					
					
					try{
						Thread.sleep(billing_thread_interval*1000);//TODO: right now this is in seconds
					}catch(InterruptedException ie)
					{
						ie.printStackTrace();
						continue;
					}
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}
	
	private void billing_thread_run()
	{
		Entity recurring_order 	= null;
		Entity order_user 		= null;
		Entity billing_record 	= null;
		Query q 				= null;
		QueryResult result 		= null;

		try{
			Date now = new Date();
			q = new Query(RECURRING_ORDER_ENTITY);
			q.idx(IDX_RECURRING_ORDER_BY_STATUS_BY_NEXT_BILL_DATE);
			q.betweenDesc(q.list(ORDER_STATUS_OPEN,now), q.list(ORDER_STATUS_OPEN,Query.VAL_MIN));
			//q.lte(now);
			result = QUERY(q);
			MODULE_LOG( 1,result.size()+" records to bill.");
		}catch(PersistenceException pe1)
		{
			MODULE_LOG( 1,"ERROR: ABORTING BILLING CYCLE DUE TO FAILED QUERY. "+q);
			ERROR("ABORTING BILLING CYCLE DUE TO FAILED QUERY. "+q);
			return;
		}
		
		if(!billing_module.isConfigured())
		{
			MODULE_LOG( 1,"ERROR: ABORTING BILLING CYCLE DUE TO BILLING MODULE NOT BEING CONFIGURED.MAKE SURE ENCRYPTION MODULE IS CONFIGURED.");
			ERROR("ERROR: ABORTING BILLING CYCLE DUE TO BILLING MODULE NOT BEING CONFIGURED.MAKE SURE ENCRYPTION MODULE IS CONFIGURED.");
			//make function that takes no template and just a body//
			//email_module.sendEmail(from, to, subject, template_name, template_data);
			return;
		}
		
		List<Entity> orders_that_need_to_be_billed = result.getEntities();
		for(int i = 0;i < orders_that_need_to_be_billed.size();i++)
		{
			try{
				recurring_order = orders_that_need_to_be_billed.get(i);
				FILL_REFS(recurring_order);
				order_user     = (Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER);
				billing_record = billing_module.getPreferredBillingRecord(order_user);
				if(billing_record == null)
				{
					List<Entity> promotion_instances = (List<Entity>)recurring_order.getAttribute(RECURRING_ORDER_FIELD_PROMOTIONS);
					if(promotion_instances != null)
					{
						for(int j = 0;j< promotion_instances.size();j++)
						{
							Entity promotion_instance = promotion_instances.get(j);
							if(promotion_module.isFreeForLifePromotion(promotion_instance))
								break;
						}
					}
					else
					{
						MODULE_LOG( 1,"!!!MONTHLY BILL FAILED FOR RECURRING ORDER "+recurring_order.getId()+" "+recurring_order);
						MODULE_LOG( 1,"USER DOES NOT HAVE PREFERRED BILLING RECORD SOMEHOW "+order_user);
						updateRecurringOrderStatus(recurring_order, ORDER_STATUS_NO_PREFERRED_BILLING_RECORD);
						send_system_alert_notification(order_user,"There was a problem billing your account. Please make sure you have a valid billing record.");
						send_billing_failed_email(recurring_order, "ERROR: NO PREFERRED BILLING RECORD. PLEASE LOGIN AND SPECIFY PREFERRED BILLING RECORD.");
						continue;
					}
				}
				
				try{
					promotion_module.applyPromotions(recurring_order);
				}catch(Exception e)
				{
					ERROR(e);
					MODULE_LOG("FAILED TO APPLY PROMOTION FOR MONTHLY BILLING FOR ORDER "+recurring_order);
				}
				
				try{
					do_monthly_billing(recurring_order, billing_record);
				}catch(BillingGatewayException bge)
				{
					ERROR(bge);
					MODULE_LOG( 1,"!!!MONTHLY BILL FAILED FOR RECURRING ORDER "+recurring_order.getId()+" "+recurring_order);
					log_order_monthly_bill_failed(recurring_order, bge.getAmount());
					updateRecurringOrderStatus(recurring_order, ORDER_STATUS_LAST_MONTHLY_BILL_FAILED);	
					send_system_alert_notification(order_user,"There was a problem billing your account. Please make sure you have a valid billing record.");
					send_billing_failed_email(recurring_order, null);
					continue;
				}

				MODULE_LOG( 1,"MONTHLY BILL OK FOR RECURRING ORDER "+recurring_order.getId());
				
			}catch(PersistenceException pe)
			{
				MODULE_LOG( 0,"ERROR:ABORTING BILLING THREAD DUE TO PERSISTENCE EXCEPTION.");
				ERROR("ABORTING BILLING THREAD DUE TO PERSISTENCE EXCEPTION.", pe);
				break;
			}

		}
	}
	
	private void send_billing_failed_email(Entity recurring_order,String additional_info)
	{
		
		Entity user = null;
		String user_email = null;
		try{
			user = EXPAND((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER));
			Map<String,Object> template_data = new HashMap<String, Object>();
			template_data.put("username",(String)user.getAttribute(UserModule.FIELD_EMAIL));
			if(additional_info == null)
				template_data.put("additional_information","");
			else
				template_data.put("additional_information",additional_info);
			user_email = (String)user.getAttribute(UserModule.FIELD_EMAIL);
			email_module.sendEmail(null, new String[]{user_email}, "Your monthly billing failed.", "billing-failed.fm", template_data);
		}catch(Exception e)
		{
			e.printStackTrace();
			MODULE_LOG("EMAIL MODULE FAILED SENDING BILLING FAILED EMAIL TO USER "+user.getId()+" "+user_email);
		}
	}
	
	///TRANSACTION LOGGING STUFF
	private static final int LOG_ORDER_OPENED 	      		= 0x10;	
	private static final int LOG_ORDER_SUSPENDED 	      	= 0x20;	
	private static final int LOG_ORDER_CLOSED 	      		= 0x30;	
	private static final int LOG_INIT_BILLING_OK 	      	= 0x40;	
	private static final int LOG_INIT_BILLING_FAILED    	= 0x50;	
	private static final int LOG_MONTHLY_BILLING_OK 	  	= 0x60;	
	private static final int LOG_MONTHLY_BILLING_FAILED 	= 0x70;	

	private void log_order_opened(Entity recurring_order) throws PersistenceException
	{
		logger_module.createLogMessage((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER), LOG_ORDER_OPENED, "Order "+recurring_order.getId()+" opened OK.", recurring_order);
	}
	
	private void log_order_suspended(Entity recurring_order,int old_status) throws PersistenceException
	{
		int current_status = (Integer)recurring_order.getAttribute(RECURRING_ORDER_FIELD_STATUS);
		logger_module.createLogMessage((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER), LOG_ORDER_SUSPENDED, "Order "+recurring_order.getId()+" has been suspended. Order status was "+old_status+" and is now "+current_status+".", recurring_order);
	}
	
	private void log_order_closed(Entity recurring_order) throws PersistenceException
	{
		logger_module.createLogMessage((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER), LOG_ORDER_CLOSED, "Order "+recurring_order.getId()+" has been closed.", recurring_order);
	}

	private void log_order_init_bill_ok(Entity recurring_order,float amount) throws PersistenceException
	{
		logger_module.createLogMessage((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER), LOG_INIT_BILLING_OK, "Order "+recurring_order.getId()+" was billed for initial fee of "+amount+".", recurring_order);
	}
	
	private void log_order_init_bill_failed(Entity recurring_order,float amount) throws PersistenceException
	{
		logger_module.createLogMessage((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER), LOG_INIT_BILLING_FAILED, "Order "+recurring_order.getId()+" failed initial billing for the amount of "+amount+".", recurring_order);
	}
	
	private void log_order_monthly_bill_ok(Entity recurring_order,float amount) throws PersistenceException
	{
		logger_module.createLogMessage((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER), LOG_MONTHLY_BILLING_OK, "Order "+recurring_order.getId()+" was billed for monthly fee of "+amount+".", recurring_order);
	}
	
	private void log_order_monthly_bill_failed(Entity recurring_order,float amount) throws PersistenceException
	{
		logger_module.createLogMessage((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER), LOG_MONTHLY_BILLING_FAILED, "Order "+recurring_order.getId()+" failed monthly billing for the amount of "+amount+".", recurring_order);
	}


	
	///BEGIN DDL STUFF
	public static String RECURRING_SKU_ENTITY 			  		= "RecurringSKU";
	public static String RECURRING_SKU_FIELD_TITLE 			 	= "title";
	public static String RECURRING_SKU_FIELD_DESCRIPTION 		= "description";
	public static String RECURRING_SKU_FIELD_RESOURCE 		  	= "resource";
	public static String RECURRING_SKU_FIELD_INITIAL_FEE 	  	= "initial_fee";
	public static String RECURRING_SKU_FIELD_RECURRING_PRICE 	= "price";
	public static String RECURRING_SKU_FIELD_CATALOG_NUMBER 	= "catalog_number";/*application provides(optional)*/
	public static String RECURRING_SKU_FIELD_USER_DATA 	  	  	= "data";	/*application provides(optional)*/
	public static String RECURRING_SKU_FIELD_CATALOG_STATE 		= "catalog_state";/*application provides(optional)*/
	
	public static String RECURRING_SKU_RESOURCE_ENTITY 				= "RecurringSKUResource";
	public static String RECURRING_SKU_RESOURCE_FIELD_CONTENT_TYPE 	= RESOURCE_FIELD_CONTENT_TYPE;
	public static String RECURRING_SKU_RESOURCE_FIELD_SIMPLE_TYPE 	= RESOURCE_FIELD_SIMPLE_TYPE;
	public static String RECURRING_SKU_RESOURCE_FIELD_FILENAME 		= RESOURCE_FIELD_FILENAME;
	public static String RECURRING_SKU_RESOURCE_FIELD_EXTENSION 	= RESOURCE_FIELD_EXTENSION;
	public static String RECURRING_SKU_RESOURCE_FIELD_FILE_SIZE 	= RESOURCE_FIELD_FILE_SIZE;
	public static String RECURRING_SKU_RESOURCE_FIELD_PATH_TOKEN 	= RESOURCE_FIELD_PATH_TOKEN;

	public static String RECURRING_ORDER_ENTITY 				= "RecurringOrder";
	public static String RECURRING_ORDER_FIELD_USER 	    	= "user";
	public static String RECURRING_ORDER_FIELD_SKUS	 			= "sku";
	public static String RECURRING_ORDER_FIELD_STATUS 			= "order_status";
	public static String RECURRING_ORDER_FIELD_LAST_BILL_DATE 	= "last_bill_date";
	public static String RECURRING_ORDER_FIELD_NEXT_BILL_DATE 	= "next_bill_date";
	public static String RECURRING_ORDER_FIELD_PROMOTIONS		= "promotions";
	public static String RECURRING_ORDER_FIELD_RECURRING_UNIT 	= "recurring_unit";//in days//
	public static String RECURRING_ORDER_FIELD_RECURRING_PERIOD = "recurring_period";//in unit//
	

	
	public static final String PROMOTION_ENTITY = "Promotion";
	public static final int RECURRING_SKU_CATALOG_STATE_INACTIVE   = 0; 
	public static final int RECURRING_SKU_CATALOG_STATE_ACTIVE     = 1; 

	protected void defineEntities(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY(RECURRING_SKU_ENTITY,
					  RECURRING_SKU_FIELD_TITLE,Types.TYPE_STRING,"",
					  RECURRING_SKU_FIELD_DESCRIPTION,Types.TYPE_STRING,"",
					  RECURRING_SKU_FIELD_RESOURCE,Types.TYPE_REFERENCE, RECURRING_SKU_RESOURCE_ENTITY,null,
					  RECURRING_SKU_FIELD_INITIAL_FEE,Types.TYPE_FLOAT,0.0f,
					  RECURRING_SKU_FIELD_RECURRING_PRICE,Types.TYPE_FLOAT,0.0f,
					  RECURRING_SKU_FIELD_CATALOG_NUMBER,Types.TYPE_STRING,null,
					  RECURRING_SKU_FIELD_USER_DATA,Types.TYPE_REFERENCE,FieldDefinition.REF_TYPE_UNTYPED_ENTITY,null,
					  RECURRING_SKU_FIELD_CATALOG_STATE,Types.TYPE_INT,RECURRING_SKU_CATALOG_STATE_INACTIVE);
				
		DEFINE_ENTITY(RECURRING_SKU_RESOURCE_ENTITY,
				RECURRING_SKU_RESOURCE_FIELD_CONTENT_TYPE,Types.TYPE_STRING,null,
				RECURRING_SKU_RESOURCE_FIELD_SIMPLE_TYPE,Types.TYPE_STRING,null,
				RECURRING_SKU_RESOURCE_FIELD_FILENAME,Types.TYPE_STRING,null,
				RECURRING_SKU_RESOURCE_FIELD_EXTENSION,Types.TYPE_STRING,null,
				RECURRING_SKU_RESOURCE_FIELD_FILE_SIZE,Types.TYPE_LONG,null,
				RECURRING_SKU_RESOURCE_FIELD_PATH_TOKEN,Types.TYPE_STRING,null);
		
		DEFINE_ENTITY(RECURRING_ORDER_ENTITY,
					  RECURRING_ORDER_FIELD_USER,Types.TYPE_REFERENCE, UserModule.USER_ENTITY,null,
					  RECURRING_ORDER_FIELD_SKUS,Types.TYPE_ARRAY | Types.TYPE_REFERENCE,RECURRING_SKU_ENTITY,null,
					  RECURRING_ORDER_FIELD_STATUS,Types.TYPE_INT,ORDER_STATUS_INIT,
					  RECURRING_ORDER_FIELD_LAST_BILL_DATE,Types.TYPE_DATE,null,
					  RECURRING_ORDER_FIELD_NEXT_BILL_DATE,Types.TYPE_DATE,null,					  
					  RECURRING_ORDER_FIELD_PROMOTIONS,Types.TYPE_REFERENCE|Types.TYPE_ARRAY, PROMOTION_ENTITY,new ArrayList<Entity>(),
					  RECURRING_ORDER_FIELD_RECURRING_UNIT,Types.TYPE_INT,Calendar.DATE,
					  RECURRING_ORDER_FIELD_RECURRING_PERIOD,Types.TYPE_INT,Integer.MAX_VALUE
					  );

	}

	public static final String IDX_RECURRING_ORDER_BY_NEXT_BILL_DATE 		   = "byNextBillDate";
	public static final String IDX_RECURRING_ORDER_BY_USER			 		   = "byUser";
	public static final String IDX_RECURRING_ORDER_BY_STATUS		 		   = "byStatus";
	public static final String IDX_RECURRING_ORDER_BY_STATUS_BY_NEXT_BILL_DATE = "byStatusByNextBillDate";
	
	
	public static final String IDX_RECURRING_SKU_BY_CATALOG_STATE 			 = "byCatalogState";
	public static final String IDX_RECURRING_SKU_BY_CATALOG_NUMBER_BY_CATALOG_STATE = "byCatalogNumberByCatalogState";

	
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY_INDICES
		(
				RECURRING_ORDER_ENTITY,
				ENTITY_INDEX(IDX_RECURRING_ORDER_BY_STATUS_BY_NEXT_BILL_DATE, EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX,RECURRING_ORDER_FIELD_STATUS,RECURRING_ORDER_FIELD_NEXT_BILL_DATE),
				ENTITY_INDEX(IDX_RECURRING_ORDER_BY_USER, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX,RECURRING_ORDER_FIELD_USER),
				ENTITY_INDEX(IDX_RECURRING_ORDER_BY_STATUS, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX,RECURRING_ORDER_FIELD_STATUS)
		);
		
		DEFINE_ENTITY_INDICES
		(
				RECURRING_SKU_ENTITY,
				ENTITY_INDEX(IDX_RECURRING_SKU_BY_CATALOG_STATE, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX,RECURRING_SKU_FIELD_CATALOG_STATE),
				ENTITY_INDEX(IDX_RECURRING_SKU_BY_CATALOG_NUMBER_BY_CATALOG_STATE, EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX,RECURRING_SKU_FIELD_CATALOG_NUMBER,RECURRING_SKU_FIELD_CATALOG_STATE)
		);
	}
}
