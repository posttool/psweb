package com.pagesociety.web.module.ecommerce.recurring;

import java.text.DecimalFormat;
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
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.ecommerce.billing.BillingModule;
import com.pagesociety.web.module.ecommerce.gateway.BillingGatewayException;
import com.pagesociety.web.module.ecommerce.gateway.BillingGatewayResponse;
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

	private static final String PARAM_BILLING_THREAD_INTERVAL   = "billing-thread-interval";
	private static final String PARAM_HAS_TRIAL_PERIOD		    = "has-trial-period";
	private static final String PARAM_TRIAL_PERIOD			    = "trial-period";
	private static final String PARAM_EXPIRED_TRIAL_REAP_PERIOD = "expired-trial-reap-period";
	private static final String PARAM_BILLING_FAILED_GRACE_PERIOD = "billing-failed-grace-period";//how many days after billing fails before we lockout public site
	private static final String PARAM_BILLING_FAILED_REAP_PERIOD  = "billing-failed-reap-period";//how many days after grace period expires do we reap the user
	private static final String PARAM_SEND_EMAIL_IF_BILLING_NOT_CONFIGURED   = "send-email-if-billing-not-configured";


	public static final int ORDER_STATUS_INIT 										 = 0x0000;
	public static final int ORDER_STATUS_OPEN 										 = 0x0001;
	public static final int ORDER_STATUS_CLOSED  									 = 0x0002;
	public static final int ORDER_STATUS_INITIAL_BILL_FAILED 						 = 0x0004;
	public static final int ORDER_STATUS_BILLING_FAILED_GRACE_PERIOD 				 = 0x0008;
	public static final int ORDER_STATUS_BILLING_FAILED_GRACE_PERIOD_EXPIRED 		 = 0x0009;
	public static final int ORDER_STATUS_NO_PREFERRED_BILLING_RECORD 				 = 0x0010;
	public static final int ORDER_STATUS_IN_TRIAL_PERIOD = 0x0011;
	public static final int ORDER_STATUS_TRIAL_EXPIRED   = 0x0012;
	private static String OSS(int status)
	{
		switch(status)
		{
		case ORDER_STATUS_INIT:
			return "INIT";
		case ORDER_STATUS_OPEN:
			return "OPEN";
		case ORDER_STATUS_CLOSED:
			return "CLOSED";
		case ORDER_STATUS_INITIAL_BILL_FAILED:
			return "BILLING FAILED - INITIAL BILL FAILED";
		case ORDER_STATUS_BILLING_FAILED_GRACE_PERIOD:
			return "BILLING FAILED - GRACE PERIOD";
		case ORDER_STATUS_BILLING_FAILED_GRACE_PERIOD_EXPIRED:
			return "BILLING FAILED - PUBLIC SITE LOCKED";
		case ORDER_STATUS_NO_PREFERRED_BILLING_RECORD:
			return "BILLING FAILED - NO PREFERRED BILLING RECORD";
		case ORDER_STATUS_IN_TRIAL_PERIOD:
			return "TRIAL";
		case ORDER_STATUS_TRIAL_EXPIRED:
			return "TRIAL EXPIRED";
		default:
			return "UNKNOWN";
		}

	}


	protected UserModule 			   user_module;
	protected BillingModule 		   billing_module;
	protected IBillingGateway 		   billing_gateway;
	protected IEmailModule 			   email_module;
	protected LoggerModule 			   logger_module;
	protected SystemNotificationModule notification_module;
	protected PromotionModule 		   promotion_module;
	private long 					   billing_thread_interval;

	private boolean				   	   has_trial_period;
	private int					   	   trial_period_in_days;
	private int					   	   expired_trial_reap_period_in_days;
	private int					   	   billing_failed_grace_period_in_days;
	private int					   	   billing_failed_grace_period_expired_reap_period_in_days;
	private boolean					   send_email_if_billing_not_configured;
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);
		/* see notes in super class...this is cheap inheritence */
		super.setResourceEntityName(RECURRING_SKU_RESOURCE_ENTITY);
		user_module 			= (UserModule)getSlot(SLOT_USER_MODULE);
		billing_module  		= (BillingModule)getSlot(SLOT_BILLING_MODULE);
		billing_gateway 		= billing_module.getBillingGateway();
		billing_thread_interval = Long.parseLong(GET_REQUIRED_CONFIG_PARAM(PARAM_BILLING_THREAD_INTERVAL, config));
		email_module 			= (IEmailModule)getSlot(SLOT_EMAIL_MODULE);
		logger_module 			= (LoggerModule)getSlot(SLOT_LOGGER_MODULE);
		notification_module 	= (SystemNotificationModule)getSlot(SLOT_NOTIFICATION_MODULE);
		promotion_module 		= (PromotionModule)getSlot(SLOT_PROMOTION_MODULE);
		billing_failed_grace_period_in_days 			  		 	= Integer.parseInt(GET_REQUIRED_CONFIG_PARAM(PARAM_BILLING_FAILED_GRACE_PERIOD, config));
		billing_failed_grace_period_expired_reap_period_in_days 	= Integer.parseInt(GET_REQUIRED_CONFIG_PARAM(PARAM_BILLING_FAILED_REAP_PERIOD, config));
		send_email_if_billing_not_configured 						= GET_OPTIONAL_BOOLEAN_CONFIG_PARAM(PARAM_SEND_EMAIL_IF_BILLING_NOT_CONFIGURED, false, config);

		String s_has_tp = GET_OPTIONAL_CONFIG_PARAM(PARAM_HAS_TRIAL_PERIOD, config);
		if(("yes").equalsIgnoreCase(s_has_tp) || "true".equalsIgnoreCase(s_has_tp))
		{
			has_trial_period 	 			  = true;
			try{
				trial_period_in_days 			  = Integer.parseInt(GET_REQUIRED_CONFIG_PARAM(PARAM_TRIAL_PERIOD, config));
				expired_trial_reap_period_in_days = Integer.parseInt(GET_REQUIRED_CONFIG_PARAM(PARAM_EXPIRED_TRIAL_REAP_PERIOD, config));
			}catch(NumberFormatException nfe)
			{
				throw new InitializationException("COME ON YOU NOOB.");
			}
		}

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



	public static final String CAN_CREATE_RECURRING_SKU			= "CAN_CREATE_RECURRING_SKU";
	public static final String CAN_READ_RECURRING_SKU 			= "CAN_READ_RECURRING_SKU";
	public static final String CAN_UPDATE_RECURRING_SKU 	    = "CAN_UPDATE_RECURRING_SKU";
	public static final String CAN_DELETE_RECURRING_SKU 		= "CAN_DELETE_RECURRING_SKU";
	public static final String CAN_BROWSE_RECURRING_SKUS 	    = "CAN_BROWSE_RECURRING_SKUS";

	public static final String CAN_CREATE_RECURRING_ORDER		= "CAN_CREATE_RECURRING_ORDER";
	public static final String CAN_READ_RECURRING_ORDER			= "CAN_READ_RECURRING_ORDER";
	public static final String CAN_UPDATE_RECURRING_ORDER 	    = "CAN_UPDATE_RECURRING_ORDER";
	public static final String CAN_DELETE_RECURRING_ORDER		= "CAN_DELETE_RECURRING_ORDER";
	public static final String CAN_BROWSE_RECURRING_ORDERS 	    = "CAN_BROWSE_RECURRING_ORDERS";
	public static final String CAN_BROWSE_RECURRING_ORDERS_BY_USER = "CAN_BROWSE_RECURRING_ORDERS_BY_USER";


	protected  void exportPermissions()
	{
		super.exportPermissions();
		EXPORT_PERMISSION(CAN_CREATE_RECURRING_SKU);
		EXPORT_PERMISSION(CAN_READ_RECURRING_SKU);
		EXPORT_PERMISSION(CAN_UPDATE_RECURRING_SKU);
		EXPORT_PERMISSION(CAN_DELETE_RECURRING_SKU);
		EXPORT_PERMISSION(CAN_BROWSE_RECURRING_SKUS);
		EXPORT_PERMISSION(CAN_CREATE_RECURRING_ORDER);
		EXPORT_PERMISSION(CAN_READ_RECURRING_ORDER);
		EXPORT_PERMISSION(CAN_UPDATE_RECURRING_ORDER);
		EXPORT_PERMISSION(CAN_DELETE_RECURRING_ORDER);
		EXPORT_PERMISSION(CAN_BROWSE_RECURRING_ORDERS);
		EXPORT_PERMISSION(CAN_BROWSE_RECURRING_ORDERS_BY_USER);
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
		GUARD(user,CAN_BROWSE_RECURRING_SKUS);
		PagingQueryResult result =  getRecurringSKUs(offset, page_size,Query.VAL_GLOB,null);

		return result;
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
		else if(result.size() > 1)
			WARNING("THERE ARE MORE THAN ONE RECURRING SKUS WITH CATALOG NUM: "+catalog_num);
		return result.getEntities().get(0);
	}


	@Export
	public Entity CreateRecurringSKU(UserApplicationContext uctx,Entity recurring_sku) throws WebApplicationException,PersistenceException
	{
		VALIDATE_TYPE(RECURRING_SKU_ENTITY, recurring_sku);

		String product_name 	   = (String)recurring_sku.getAttribute(RECURRING_SKU_FIELD_TITLE);
		String product_description = (String)recurring_sku.getAttribute(RECURRING_SKU_FIELD_DESCRIPTION);
		double  initial_fee        = (Double)recurring_sku.getAttribute(RECURRING_SKU_FIELD_INITIAL_FEE);
		double  product_price      = (Double)recurring_sku.getAttribute(RECURRING_SKU_FIELD_RECURRING_PRICE);
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
	public Entity CreateRecurringSKU(UserApplicationContext uctx,String product_name,String product_description,double initial_fee,double  product_price,String catalog_no,long recurring_sku_resource_id,String user_data_type,long user_data_id,int catalog_state) throws WebApplicationException,PersistenceException
	{
		Entity user 	   	= (Entity)uctx.getUser();
		Entity user_data 	= null;
		if(user_data_type  != null)
			user_data = GET(user_data_type,user_data_id);

		Entity recurring_sku_resource = null;
		if(recurring_sku_resource_id != 0)//0 for a ref pointer means null//
			recurring_sku_resource = GET(RECURRING_SKU_ENTITY,recurring_sku_resource_id);
		GUARD(user,CAN_CREATE_RECURRING_SKU,GUARD_TYPE, RECURRING_SKU_ENTITY,
											RECURRING_SKU_FIELD_TITLE,product_name,
											RECURRING_SKU_FIELD_DESCRIPTION,product_description,
											RECURRING_SKU_FIELD_INITIAL_FEE,initial_fee,
											RECURRING_SKU_FIELD_RECURRING_PRICE,product_price,
											RECURRING_SKU_FIELD_CATALOG_NUMBER,catalog_no,
											RECURRING_SKU_FIELD_RESOURCE,recurring_sku_resource,
											RECURRING_SKU_FIELD_USER_DATA,user_data,
											RECURRING_SKU_FIELD_CATALOG_STATE,catalog_state);

		return createRecurringSKU(user,product_name, product_description,initial_fee,product_price,catalog_no,recurring_sku_resource,user_data,catalog_state);
	}


	public Entity createRecurringSKU(Entity creator,String product_name,String product_description,double initial_fee,double product_price,String catalog_no,Entity recurring_sku_resource,Entity user_data,int catalog_state) throws PersistenceException
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
		double product_price       = (Double)recurring_sku.getAttribute(RECURRING_SKU_FIELD_RECURRING_PRICE);
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
	public Entity UpdateRecurringSKU(UserApplicationContext uctx,long recurring_sku_id,String product_name,String product_description,double product_price,String catalog_no,long recurring_sku_resource_id,String user_data_type,long user_data_id,int catalog_state) throws WebApplicationException,PersistenceException
	{
		Entity user 	   	= (Entity)uctx.getUser();
		Entity recurring_sku = GET(RECURRING_SKU_ENTITY,recurring_sku_id);

		Entity user_data 	= null;
		if(user_data_type  != null)
			user_data = GET(user_data_type,user_data_id);

		Entity recurring_sku_resource = null;
		if(recurring_sku_resource_id != 0)//0 for a ref pointer means null//
			recurring_sku_resource = GET(RECURRING_SKU_ENTITY,recurring_sku_resource_id);
		GUARD(user,CAN_UPDATE_RECURRING_SKU,GUARD_INSTANCE, recurring_sku,
											RECURRING_SKU_FIELD_TITLE,product_name,
											RECURRING_SKU_FIELD_DESCRIPTION,product_description,
											RECURRING_SKU_FIELD_RECURRING_PRICE,product_price,
											RECURRING_SKU_FIELD_CATALOG_NUMBER,catalog_no,
											RECURRING_SKU_FIELD_RESOURCE,recurring_sku_resource,
											RECURRING_SKU_FIELD_USER_DATA,user_data,
											RECURRING_SKU_FIELD_CATALOG_STATE,catalog_state);

		return updateRecurringSKU(recurring_sku,product_name, product_description, product_price,catalog_no, recurring_sku_resource,user_data,catalog_state);
	}


	public Entity updateRecurringSKU(Entity recurring_sku,String product_name,String product_description,double product_price,String catalog_no,Entity recurring_sku_resource,Entity user_data,int catalog_state) throws WebApplicationException,PersistenceException
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
		GUARD(user,CAN_DELETE_RECURRING_SKU,GUARD_INSTANCE, recurring_sku);
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
		List<Entity> recurring_skus  = (List<Entity>)recurring_order.getAttribute("skus");
		List<Entity> promotions 	 = (List<Entity>)recurring_order.getAttribute(RECURRING_ORDER_FIELD_PROMOTIONS);

		VALIDATE_TYPE_LIST(RECURRING_SKU_ENTITY, recurring_skus);
		VALIDATE_TYPE_LIST(promotion_module.PROMOTION_INSTANCE_ENTITY, promotions);
		int recurring_unit   = (Integer)recurring_order.getAttribute(RECURRING_ORDER_FIELD_RECURRING_UNIT);
		int recurring_period = (Integer)recurring_order.getAttribute(RECURRING_ORDER_FIELD_RECURRING_PERIOD);

		return CreateRecurringOrder(uctx,target_user_id,recurring_unit,recurring_period,ENTITIES_TO_IDS(recurring_skus),ENTITIES_TO_IDS(promotions));
	}

	@Export
	public Entity CreateRecurringOrder(UserApplicationContext uctx,long target_user_id,int recurring_unit,int recurring_period,List<Long> recurring_sku_ids,List<Long> promotion_ids) throws WebApplicationException,PersistenceException
	{
		Entity user 	   = (Entity)uctx.getUser();
		Entity target_user = user_module.getUser(target_user_id);

		List <Entity>skus 		= IDS_TO_ENTITIES(RECURRING_SKU_ENTITY,recurring_sku_ids);
		List<Entity> promotions = IDS_TO_ENTITIES(PromotionModule.PROMOTION_INSTANCE_ENTITY, promotion_ids);


		GUARD(user,CAN_CREATE_RECURRING_ORDER,GUARD_TYPE,RECURRING_ORDER_ENTITY,
												  RECURRING_ORDER_FIELD_SKUS,skus,
												  RECURRING_ORDER_FIELD_USER,user,
												  RECURRING_ORDER_FIELD_PROMOTIONS,promotions,
												  RECURRING_ORDER_FIELD_RECURRING_UNIT,recurring_unit,
												  RECURRING_ORDER_FIELD_RECURRING_PERIOD,recurring_period);

		return createRecurringOrder(user,target_user,recurring_unit,recurring_period,skus,promotions);

	}

	public Entity createRecurringOrder(Entity creator,Entity user,int recurring_unit,int recurring_period,List<Entity> skus,List<Entity> promotions) throws PersistenceException
	{
		List<Entity> line_items = new ArrayList<Entity>();
		for(int i = 0;i < skus.size();i++)
		{
			Entity sku = skus.get(i);
			Entity line_item = NEW(RECURRING_ORDER_LINE_ITEM_ENTITY,user,
								   RECURRING_ORDER_LINE_ITEM_FIELD_INITIAL_FEE,sku.getAttribute(RECURRING_SKU_FIELD_INITIAL_FEE),
								   RECURRING_ORDER_LINE_ITEM_FIELD_PRICE,sku.getAttribute(RECURRING_SKU_FIELD_RECURRING_PRICE),
								   RECURRING_ORDER_LINE_ITEM_FIELD_CODE,sku.getAttribute(RECURRING_SKU_FIELD_CATALOG_NUMBER),
								   RECURRING_ORDER_LINE_ITEM_FIELD_SKU,sku);
			line_items.add(line_item);
		}


		Entity recurring_order =  NEW(RECURRING_ORDER_ENTITY,
									  creator,
									  RECURRING_ORDER_FIELD_LINE_ITEMS,line_items,
									  RECURRING_ORDER_FIELD_USER,user,
									  RECURRING_ORDER_FIELD_STATUS,ORDER_STATUS_INIT,
									  RECURRING_ORDER_FIELD_LAST_BILL_DATE,null,
									  RECURRING_ORDER_FIELD_NEXT_BILL_DATE,null,//new Date(0L),//TODO: set this somewhere in the past//
									  RECURRING_ORDER_FIELD_PROMOTIONS,promotions,
									  RECURRING_ORDER_FIELD_RECURRING_UNIT,recurring_unit,
									  RECURRING_ORDER_FIELD_RECURRING_PERIOD,recurring_period);

		MODULE_LOG(0,"CREATED RECURRING ORDER "+recurring_order.getId()+" OK");
		return recurring_order;
	}


	public void attachPromo(Entity target_user, Entity order, String promo_code) throws PersistenceException, WebApplicationException
	{
		Entity promotion_instance = promotion_module.getCouponPromotionByPromoCode(target_user,promo_code.trim());
		List<Entity> promotions = (List<Entity>) order.getAttribute(RECURRING_ORDER_FIELD_PROMOTIONS);
		if (promotions==null)
			promotions = new ArrayList<Entity>();
		promotions.add(promotion_instance);
		UPDATE(order,
				RECURRING_ORDER_FIELD_PROMOTIONS,promotions);
	}

	public void removePromo(Entity order, Entity promo_instance) throws PersistenceException, WebApplicationException
	{
		List<Entity> promotions = (List<Entity>) order.getAttribute(RECURRING_ORDER_FIELD_PROMOTIONS);
		if (promotions==null)
			throw new WebApplicationException("NO PROMOTIONS ON ORDER "+order+" "+promo_instance);
		boolean ok = promotions.remove(promo_instance);
		if (!ok)
			throw new WebApplicationException("CANNOT FIND PROMO INSTANCE IN ORDER "+order+" "+promo_instance);
		UPDATE(order,
				RECURRING_ORDER_FIELD_PROMOTIONS,promotions);
	}

	@Export
	public Entity DeleteRecurringOrder(UserApplicationContext uctx,long recurring_order_id) throws WebApplicationException,PersistenceException
	{
		Entity user 	   = (Entity)uctx.getUser();
		Entity recurring_order = GET(RECURRING_ORDER_ENTITY,recurring_order_id);

		GUARD(user, CAN_DELETE_RECURRING_ORDER, recurring_order);
		return deleteRecurringOrder(recurring_order);
	}

	public Entity deleteRecurringOrder(Entity recurring_order) throws WebApplicationException,PersistenceException
	{
		List<Entity> line_items = (List<Entity>)recurring_order.getAttribute(RECURRING_ORDER_FIELD_LINE_ITEMS);
		if(line_items != null)
		{
			for(int i = 0; i < line_items.size();i++)
			{
				Entity line_item = line_items.get(i);
				DELETE(line_item);
			}
		}
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
		Entity order_user     = (Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER);
		FILL_REFS(recurring_order);
		int status 		= (Integer)recurring_order.getAttribute(RECURRING_ORDER_FIELD_STATUS);

		//GET PREFERRED BILLING RECORD AND CHECK CONDITIONS SURROUNDING FREE FOR LIFE WITH NO BILLING RECORD\\
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
				updateRecurringOrderStatus(recurring_order, ORDER_STATUS_BILLING_FAILED_GRACE_PERIOD,"Need billing info.");
				//throw new WebApplicationException("USER DOES NOT HAVE PREFERRED BILLING RECORD SET. NO WAY TO OPEN ORDER.");
				return recurring_order;
			}
		}

		//SEE HOW THE ORDER IS COMING IN \\
		switch(status)
		{
		case ORDER_STATUS_INIT:
		case ORDER_STATUS_IN_TRIAL_PERIOD:
		case ORDER_STATUS_TRIAL_EXPIRED:
		case ORDER_STATUS_INITIAL_BILL_FAILED:
			recurring_order.setAttribute(RECURRING_ORDER_FIELD_NEXT_BILL_DATE, new Date());
			try{
					do_initial_fee_billing(recurring_order,billing_record);
				}catch(BillingGatewayException bge)
				{
					throw new WebApplicationException("UNABLE TO OPEN ORDER. FAILED INITIAL FEE BILLING");
				}

			try{
				double amount = 0;
				try{
					amount = get_order_amount_with_promotions_applied(recurring_order);
				}catch(WebApplicationException wae)
				{
					//script exception//
					ERROR(wae);
					send_promo_script_failed_email(wae, recurring_order);
					break;
				}
				do_regular_billing(order_user,recurring_order,billing_record,amount);
			}catch(BillingGatewayException bge2)
			{
				updateRecurringOrderStatus(recurring_order, ORDER_STATUS_BILLING_FAILED_GRACE_PERIOD);
				throw new WebApplicationException("UNABLE TO OPEN ORDER. FAILED BILLING");
			}
			updateRecurringOrderStatus(recurring_order, ORDER_STATUS_OPEN);
			break;
		case ORDER_STATUS_BILLING_FAILED_GRACE_PERIOD_EXPIRED:
		case ORDER_STATUS_BILLING_FAILED_GRACE_PERIOD:
				try{
					do_catchup_billing(order_user,recurring_order,billing_record);
				}catch(BillingGatewayException bge2)
				{
					throw new WebApplicationException("UNABLE TO OPEN ORDER. FAILED BILLING");
				}
				updateRecurringOrderStatus(recurring_order, ORDER_STATUS_OPEN);
				break;
		}
		return recurring_order;
	}

	@Export
	public Entity CloseRecurringOrder(UserApplicationContext uctx,long recurring_order_id) throws WebApplicationException,PersistenceException
	{
		Entity user 	   		= (Entity)uctx.getUser();
		Entity recurring_order 	= GET(RECURRING_ORDER_ENTITY,recurring_order_id);

		GUARD(user, CAN_UPDATE_RECURRING_ORDER,GUARD_INSTANCE,recurring_order,
											 	RECURRING_ORDER_FIELD_STATUS,ORDER_STATUS_CLOSED);
		return closeRecurringOrder(recurring_order);
	}

	public Entity closeRecurringOrder(Entity recurring_order) throws WebApplicationException,PersistenceException
	{
		return updateRecurringOrderStatus(recurring_order, ORDER_STATUS_CLOSED);
	}

	public Entity updateRecurringOrderStatus(Entity recurring_order,int status,Object... args) throws PersistenceException
	{
		int old_status  = (Integer)recurring_order.getAttribute(RECURRING_ORDER_FIELD_STATUS);
		recurring_order =  FILL_REFS(UPDATE(recurring_order,
								  	 RECURRING_ORDER_FIELD_STATUS,status));

		Entity order_user = EXPAND((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER));
		//log the status change//
		switch(status)
		{
			case ORDER_STATUS_INIT:
				break;
			case ORDER_STATUS_OPEN:
				MODULE_LOG("$+ OPENED ORDER SUCCESSFULLY FOR USER:"+order_user);
				UPDATE(recurring_order,
						RECURRING_ORDER_FIELD_BILLING_FAILED_GENESIS,null,
						RECURRING_ORDER_FIELD_OUTSTANDING_BALANCE,0.0);

				if(old_status == ORDER_STATUS_INIT ||
				   old_status == ORDER_STATUS_IN_TRIAL_PERIOD)
				{
					send_welcome_email(recurring_order,null);
					log_order_opened(recurring_order,false);
				}
				else
					log_order_opened(recurring_order,true);
				break;
			case ORDER_STATUS_CLOSED:
				MODULE_LOG(0,"CLOSING RECURRING ORDER "+recurring_order.getId()+" FOR USER "+order_user);
				log_order_closed(recurring_order);
				if(old_status == ORDER_STATUS_BILLING_FAILED_GRACE_PERIOD_EXPIRED)
					send_account_closed_due_to_grace_period_expired_email(recurring_order,null);
				else if(old_status == ORDER_STATUS_OPEN)
					send_account_closed_email(recurring_order, null);
				break;
			case ORDER_STATUS_NO_PREFERRED_BILLING_RECORD:
				MODULE_LOG("NO PREFERRED BILLING RECORD FOR USER:"+order_user+" WHEN TRYING TO OPEN RECURRING ORDER "+recurring_order.getId());
				send_system_alert_notification(order_user,"There was a problem with your order. Please make sure you have a valid billing record. We don't seem to have one on file for you.");
				send_billing_failed_email(recurring_order, "Please login and update billing information.");
				log_order_suspended(recurring_order, old_status);
				break;
			case ORDER_STATUS_INITIAL_BILL_FAILED:
				log_order_init_bill_failed(recurring_order, tally_order(recurring_order),(String)args[0]);
				MODULE_LOG(1,"ERROR: RECURRING ORDER "+recurring_order.getId()+" INITIAL BILLING FAILED. FOR USER: "+order_user);
				send_system_alert_notification(order_user,"There was a problem with your order. Please make sure you have a valid billing record.");
				send_billing_failed_email(recurring_order, null);
				break;
			case ORDER_STATUS_BILLING_FAILED_GRACE_PERIOD:
				double amount 	= tally_order(recurring_order);
				try{
					amount = get_order_amount_with_promotions_applied(recurring_order);
				}catch(WebApplicationException wae)
				{
					send_promo_script_failed_email(wae, recurring_order);
				}
				log_order_billing_failed(recurring_order,amount,(String)args[0]);
				Date now = new Date();
				//this needs to be in a seperate function???\\
				UPDATE(recurring_order,
						RECURRING_ORDER_FIELD_LAST_BILL_DATE,now,
						RECURRING_ORDER_FIELD_NEXT_BILL_DATE, calculate_next_bill_date(recurring_order,now),
						RECURRING_ORDER_FIELD_OUTSTANDING_BALANCE,roundDouble(amount,2),
						RECURRING_ORDER_FIELD_BILLING_FAILED_GENESIS,now);

				send_system_alert_notification(order_user,"There was a problem billing your account:"+ (String)args[0]+". Please make sure you have a valid billing record.");
				send_billing_failed_email(recurring_order, (String)args[0]);
				log_order_delinquent(recurring_order, old_status);
				MODULE_LOG( 2,"billing failure. order now in grace period for user: "+order_user.getAttribute(UserModule.FIELD_EMAIL)+" "+order_user);
				break;
			case ORDER_STATUS_BILLING_FAILED_GRACE_PERIOD_EXPIRED:
				MODULE_LOG( 2,"billing failure grace period expired for user: "+order_user.getAttribute(UserModule.FIELD_EMAIL)+" "+order_user);
				send_system_alert_notification(order_user,"There continues to be a problem billing your account. Please make sure you have a valid billing record. Your public site is now unavailable until this is rectified.");
				log_order_suspended(recurring_order, old_status);
				send_billing_failed_grace_period_expired_email(recurring_order, null);
				break;
			case ORDER_STATUS_IN_TRIAL_PERIOD:
				MODULE_LOG("OPENED USER TRIAL ORDER FOR:"+order_user);
				log_order_trial_started(recurring_order);
				break;
			case ORDER_STATUS_TRIAL_EXPIRED:
				MODULE_LOG("TRIAL PERIOD EXPIRED FOR USER:"+order_user);
				log_order_trial_expired(recurring_order);
				break;
		}

		return recurring_order;
	}

	private double tally_order(Entity recurring_order)
	{
		List<Entity> line_items 	 = (List<Entity>)recurring_order.getAttribute(RECURRING_ORDER_FIELD_LINE_ITEMS);
		double amount = 0;
		for(int ii = 0;ii < line_items.size();ii++)
		{
			Entity sku = line_items.get(ii);
			amount    += (Double)sku.getAttribute(RECURRING_ORDER_LINE_ITEM_FIELD_PRICE);
		}
		return amount;
	}

	private double get_order_amount_with_promotions_applied(Entity recurring_order) throws WebApplicationException
	{
		Double a= null;
		if((a = (Double)recurring_order.getAttribute("cached_amt")) != null)
			return a;
		promotion_module.applyPromotions(recurring_order);
		double amt =  tally_order(recurring_order);
		recurring_order.setAttribute("cached_amt",amt);
		return amt;
	}


	@Export
	public PagingQueryResult GetRecurringOrdersByStatus(UserApplicationContext uctx,int status,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(user, CAN_BROWSE_RECURRING_ORDERS);
		PagingQueryResult result =  getRecurringOrdersByStatus(status, page_size, offset);
		return result;
	}

	public PagingQueryResult getRecurringOrdersByStatus(int status,int page_size,int offset) throws WebApplicationException,PersistenceException
	{
		Query q = new Query(RECURRING_ORDER_ENTITY);
		q.idx(IDX_RECURRING_ORDER_BY_STATUS);
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
		GUARD(user,CAN_BROWSE_RECURRING_ORDERS_BY_USER, GUARD_USER,user);
		PagingQueryResult result =  getRecurringOrdersByUser(target_user,offset,page_size);

		return result;
	}


	public PagingQueryResult getRecurringOrdersByUser(Entity user,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Query q = new Query(RECURRING_ORDER_ENTITY);
		q.idx(IDX_RECURRING_ORDER_BY_USER);
		q.eq(user);
		q.offset(offset);
		q.pageSize(page_size);
		return PAGING_QUERY(q);
	}

	public List<Entity> getRecurringOrdersByUser(Entity user) throws WebApplicationException,PersistenceException
	{
		Query q = new Query(RECURRING_ORDER_ENTITY);
		q.idx(IDX_RECURRING_ORDER_BY_USER);
		q.eq(user);
		QueryResult result = QUERY(q);
		return result.getEntities();
	}

	@Export
	public PagingQueryResult GetTransactionHistoryByUser(UserApplicationContext uctx,long user_id,int offset,int page_size, boolean asc) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity target_user = user_module.getUser(user_id);

		return getTransactionHistoryByUser(target_user,offset,page_size,asc);
	}

	public List<Entity> getTransactionHistoryByUser(Entity user) throws PersistenceException
	{
		Query q = logger_module.getLogMessagesByUserQ(user);
		return QUERY(q).getEntities();
	}

	public PagingQueryResult getTransactionHistoryByUser(Entity user,int offset,int page_size, boolean asc) throws WebApplicationException,PersistenceException
	{
		Query q = logger_module.getLogMessagesByUserQ(user);
		q.offset(offset);
		q.pageSize(page_size);
		q.orderBy(FIELD_LAST_MODIFIED, asc ? Query.ASC : Query.DESC);
		PagingQueryResult pqr = PAGING_QUERY(q);
		return pqr;
	}


	/////////////////E N D  M O D U L E   F U N C T I O N S/////////////////////////////////////////

	private void do_initial_fee_billing(Entity recurring_order,Entity billing_record) throws PersistenceException,BillingGatewayException
	{
		double initial_fee = tally_initial_fee(recurring_order);
		if(initial_fee != 0)
		{
			BillingGatewayResponse bgr = billing_gateway.doSale(billing_record, initial_fee,null,"PSTRO"+recurring_order.getId(),"[INITIAL FEE]oid:"+recurring_order.getId()+"uid:"+((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER)).getId()+":initial_fee:"+normalize_amount(initial_fee));
			log_order_init_bill_ok(recurring_order, initial_fee, bgr);
		}
	}

	private double tally_initial_fee(Entity recurring_order) throws PersistenceException,BillingGatewayException
	{
		List<Entity> line_items 	 = (List<Entity>)recurring_order.getAttribute(RECURRING_ORDER_FIELD_LINE_ITEMS);
		double initial_fee = 0;
		for(int i = 0;i < line_items.size();i++)
		{
			Entity sku = line_items.get(i);
			initial_fee+= (Double)sku.getAttribute(RECURRING_SKU_FIELD_INITIAL_FEE);
		}
		return initial_fee;
	}


	private double do_regular_billing(Entity order_user,Entity recurring_order,Entity billing_record,double amount) throws PersistenceException,BillingGatewayException
	{
		Date next_bill_date = (Date)recurring_order.getAttribute(RECURRING_ORDER_FIELD_NEXT_BILL_DATE);
		Date now     = new Date();
		if(now.getTime() < next_bill_date.getTime())
		{
			//cant bill a date before the next bill date
			MODULE_LOG(0,"WARNING: SKIPPING BILLING RECURRING ORDER BECAUSE NEXT BILL DATE IS IN THE FUTURE.RECURRING ORDER: "+recurring_order.getId()+" user:"+order_user);
			//throw new WebApplicationException(" CANT BILL RECURRING ORDER BECAUSE NEXT BILL DATE IS IN THE FUTURE.RECURRING ORDER");
		}

		if(amount > 0)
		{
			BillingGatewayResponse response = billing_gateway.doSale(billing_record, amount,null,String.valueOf("PSTRO"+recurring_order.getId()),"[MONTHLY_BILLING]oid:"+recurring_order.getId()+":user:"+order_user.getAttribute("email")+":uid:"+order_user.getId()+":amt:$"+normalize_amount(amount));
			log_order_monthly_bill_ok(recurring_order, amount,response);
			send_billing_ok_email(recurring_order, amount, response);
		}
		else
		{
			log_order_monthly_bill_ok(recurring_order, 0,new BillingGatewayResponse("TO-YOUR-HEALTH", ""));
		}

		MODULE_LOG( 1,"MONTHLY BILL OK FOR RECURRING ORDER "+recurring_order.getId()+" "+amount+" user: "+order_user);
		UPDATE(recurring_order,
				RECURRING_ORDER_FIELD_LAST_BILL_DATE, now,
				RECURRING_ORDER_FIELD_NEXT_BILL_DATE, calculate_next_bill_date(recurring_order,now));
		return amount;
	}



	private void do_catchup_billing(Entity order_user,Entity recurring_order,Entity billing_record) throws PersistenceException,BillingGatewayException, WebApplicationException
	{
		Date now     = new Date();
		double amount = (Double)recurring_order.getAttribute(RECURRING_ORDER_FIELD_OUTSTANDING_BALANCE);
		if(amount > 0)
		{
			BillingGatewayResponse response = billing_gateway.doSale(billing_record, amount,null,"PSTRO"+recurring_order.getId(),"[CATCH_UP_BILLING]oid:"+recurring_order.getId()+":user:"+order_user.getAttribute("email")+":uid:"+order_user.getId()+":amt:$"+normalize_amount(amount));
			log_order_catchup_bill_ok(recurring_order, amount,response);
			send_billing_ok_email(recurring_order, amount, response);
		}
		else
		{
			throw new WebApplicationException("HOW DO WE HAVE A FAILED BILLLING WITH A 0 OUTSTANDING BALANCE");
		}
		//user is now caught up//
		MODULE_LOG( 1,"DELINQUENT BACK BILLING OK FOR ORDER "+recurring_order.getId()+" "+amount+" user: "+order_user);
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

	Thread billing_thread;
	private boolean billing_thread_running;

	private void start_billing_thread()
	{
		billing_thread_running = true;
		billing_thread = new Thread(){
			public void run()
			{
				while(billing_thread_running)
				{

					MODULE_LOG(0,"\nSTARTING BILLING CYCLE.");
					INFO("STARTING BILLING THREAD.");
					synchronized (getApplication().getApplicationLock())
					{
						INFO(" BILLING THREAD RUNNING.");
						billing_thread_run();
					}
					INFO(" BILLING THREAD COMPLETE.");
					MODULE_LOG(0,"BILLING CYCLE COMPLETE.\n");

					if(!billing_thread_running)
						break;
					try{
						Thread.sleep(billing_thread_interval*1000*60);//TODO: right now this is in minutes
					}catch(InterruptedException ie)
					{
						//ie.printStackTrace();
						continue;
					}
				}
			}
		};
		//billing_thread.setDaemon(true);
		billing_thread.start();
	}

	private boolean orderIsFreeForLife(Entity recurring_order) throws PersistenceException
	{
		List<Entity> promotion_instances = (List<Entity>)recurring_order.getAttribute(RECURRING_ORDER_FIELD_PROMOTIONS);
		if(promotion_instances != null)
		{
			for(int j = 0;j< promotion_instances.size();j++)
			{
				Entity promotion_instance = promotion_instances.get(j);
				if(promotion_module.isFreeForLifePromotion(promotion_instance))
					return true;
			}
		}
		return false;
	}

	private void billing_thread_run()
	{
		//JUST COMPLAIN IF THE BILLING MODULE ISNT CONFIGURED YET
		if(!billing_module.isConfigured())
		{
			MODULE_LOG( 1,"ERROR: ABORTING BILLING CYCLE DUE TO BILLING MODULE NOT BEING CONFIGURED.MAKE SURE ENCRYPTION MODULE IS CONFIGURED.");
			ERROR("ERROR: ABORTING BILLING CYCLE DUE TO BILLING MODULE NOT BEING CONFIGURED.MAKE SURE ENCRYPTION MODULE IS CONFIGURED.");
			if (send_email_if_billing_not_configured)
				send_billing_not_configured();
			return;
		}


		Entity recurring_order 	= null;
		Entity order_user 		= null;
		Entity billing_record 	= null;
		Query q 				= null;
		QueryResult result 		= null;



		INFO("BILLING THREAD -- STARTING");


		try{

			Date now = new Date();
			q = new Query(RECURRING_ORDER_ENTITY);
			q.idx(IDX_RECURRING_ORDER_BY_STATUS_BY_NEXT_BILL_DATE);
			q.betweenDesc(q.list(ORDER_STATUS_OPEN,now), q.list(ORDER_STATUS_OPEN,Query.VAL_MIN));
			q.cacheResults(false);
			result = QUERY(q);
			MODULE_LOG( 1,result.size()+" records to bill.");
		}catch(PersistenceException pe1)
		{

			MODULE_LOG( 1,"ERROR: ABORTING BILLING CYCLE DUE TO FAILED QUERY OR BACKUP INTERRUPTION SEE LOGS. "+q);
			ERROR("ABORTING BILLING CYCLE DUE TO FAILED QUERY. "+q);
			return;
		}


		List<Entity> orders_that_need_to_be_billed = result.getEntities();
		for(int i = 0;i < orders_that_need_to_be_billed.size();i++)
		{
			try{

				recurring_order = orders_that_need_to_be_billed.get(i);
				MODULE_LOG("ABOUT BILL ORDER\n"+recurring_order);
				INFO("ABOUT TO BILL ORDER "+recurring_order);
				FILL_REFS(recurring_order);
				order_user     = (Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER);

				double amount_after_promotions = 0;
				try {
					amount_after_promotions = get_order_amount_with_promotions_applied(recurring_order);
				} catch (WebApplicationException e1) {
					//script exception//
					ERROR(e1);
					send_promo_script_failed_email(e1, recurring_order);
					continue;
				}
				billing_record = billing_module.getPreferredBillingRecord(order_user);
				if(billing_record == null)
				{
					if(amount_after_promotions == 0)
						;
					else
					{
						updateRecurringOrderStatus(recurring_order, ORDER_STATUS_BILLING_FAILED_GRACE_PERIOD,"Need user credit card info.");
						MODULE_LOG( 1,"!!!MONTHLY BILL FAILED FOR RECURRING ORDER "+recurring_order.getId()+" "+recurring_order);
						MODULE_LOG( 1,"USER DOES NOT HAVE PREFERRED BILLING RECORD SOMEHOW "+order_user);
						continue;
					}
				}

				MODULE_LOG("ABOUT TO APPL PROMOTIONS ON ORDER\n"+recurring_order);
				double amount_before_promotions = tally_order(recurring_order);
				MODULE_LOG("\tBEFORE PROMOTIONS ORDER AMOUNT \n"+amount_before_promotions);
				try{

					do_regular_billing(order_user,recurring_order, billing_record,amount_after_promotions);
				}
				catch(PersistenceException pe4)
				{
					//throw pe4;
					ERROR("WE BARFED IN OUR OWN UPDATE BUT IT APPEARS THE BILLING MAY HAVE GONE THROUGH FOR PAYPAL ON ORDER FOR USER "+order_user);
					send_email_to_us(new Date()+" WE BARFED IN OUR OWN UPDATE BUT IT APPEARS THE BILLING MAY HAVE GONE THROUGH FOR PAYPAL ON ORDER FOR USER "+order_user+" Exception was:"+pe4.getMessage());
					ERROR(pe4);
					continue;//this is out fault for now//
				}
				catch(BillingGatewayException bge)
				{
					ERROR("CAUGHT BGE "+bge.isUserRecoverable()+ "bge "+bge.getMessage());
					ERROR(bge);
					if(bge.isUserRecoverable())
						updateRecurringOrderStatus(recurring_order, ORDER_STATUS_BILLING_FAILED_GRACE_PERIOD,bge.getMessage());
					continue;
				}

				MODULE_LOG("\tAFTER  PROMOTIONS ORDER AMOUNT \n"+amount_after_promotions);



			}catch(PersistenceException pe)
			{
				MODULE_LOG( 0,"ERROR:ABORTING BILLING THREAD DUE TO PERSISTENCE EXCEPTION.");
				ERROR("ABORTING BILLING THREAD DUE TO PERSISTENCE EXCEPTION.", pe);
				send_email_to_us(new Date()+" ERROR:ABORTING BILLING THREAD DUE TO PERSISTENCE EXCEPTION. "+pe.getMessage());
				return;//break out of for loop//
			}
			catch(Exception ee)
			{
				MODULE_LOG( 0,"ERROR:ABORTING BILLING THREAD DUE TO UNKNOWN EXCEPTION.");
				ERROR("ABORTING BILLING THREAD DUE TO UNKNOWN EXCEPTION.", ee);
				send_email_to_us(new Date()+" ERROR:ABORTING BILLING THREAD DUE TO UNKNOWN EXCEPTION. "+ee.getMessage());
				//	try{
			//		ROLLBACK_TRANSACTION();
			//	}catch(PersistenceException pe3)
			//	{
			//		ERROR("FAILED ROLLING BACK TXN "+pe3);
			//	}

				return;//break out of for loop//
			}

		}

		handle_monthly_failed_billings();
		purge_billing_failed_users();
		if(has_trial_period)
		{
			handle_trials();
			purge_expired_trial_users();
		}

	}

	private void handle_monthly_failed_billings()
	{
		Entity order_user		= null;
		Entity recurring_order 	= null;
		Query q 				= null;
		QueryResult result 		= null;

		try{
			Date now = new Date();
			q = new Query(RECURRING_ORDER_ENTITY);
			q.idx(IDX_RECURRING_ORDER_BY_STATUS_BY_NEXT_BILL_DATE);
			q.betweenDesc(q.list(ORDER_STATUS_BILLING_FAILED_GRACE_PERIOD,now), q.list(ORDER_STATUS_BILLING_FAILED_GRACE_PERIOD,Query.VAL_MIN));
			q.cacheResults(false);
			result = QUERY(q);
			MODULE_LOG( 1,result.size()+" records in monthly billing failed grace period state.");
		}catch(PersistenceException pe1)
		{
			MODULE_LOG( 1,"ERROR: ABORTING HANDLE MONTHLY BILLING FAILED CYCLE DUE TO FAILED QUERY. "+q);
			ERROR("ABORTING BILLING CYCLE DUE TO FAILED QUERY. "+q);
			return;
		}

		List<Entity> failed_orders = result.getEntities();
		for(int i = 0;i < failed_orders.size();i++)
		{

			recurring_order = failed_orders.get(i);
			try {
				FILL_REFS(recurring_order);
			} catch (PersistenceException e1) {
				ERROR(e1);
				MODULE_LOG("FAILED FILLING REFS IN HANDLE FAILED BILLING RECORDS. ORDER WAS: "+recurring_order);
				continue;
			}
			order_user     = (Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER);
			double amount = 0;
			try{
				amount = get_order_amount_with_promotions_applied(recurring_order);
			}catch(WebApplicationException wae)
			{
				MODULE_LOG("GETTING ORDER AMOUNT FOR FAILED ORDER: "+recurring_order);
				ERROR(wae);
				continue;
			}
			//TODO: might want to check for promotion here and set order back to init and 0 outstanding balance//
			try{
				Date now = new Date();
				double balance 	= (Double)recurring_order.getAttribute(RECURRING_ORDER_FIELD_OUTSTANDING_BALANCE);
				balance 		+= amount;
				UPDATE(recurring_order,
						RECURRING_ORDER_FIELD_LAST_BILL_DATE,now,
						RECURRING_ORDER_FIELD_NEXT_BILL_DATE,calculate_next_bill_date(recurring_order, now),
					    RECURRING_ORDER_FIELD_OUTSTANDING_BALANCE,roundDouble(balance,2));
				log_order_billing_failed(recurring_order, amount," balance accruing. need updated card info!");
				check_billing_failed_grace_period_expired(now, recurring_order);

			}catch(PersistenceException pe2)
			{
				MODULE_LOG("FAILED UPDATING FAILED ORDER "+recurring_order+" "+order_user);
				ERROR(pe2);
				continue;
			}
		}

	}

	private void check_billing_failed_grace_period_expired(Date now,Entity recurring_order) throws PersistenceException
	{
		Date billing_failed_genesis  =  (Date)recurring_order.getAttribute(RECURRING_ORDER_FIELD_BILLING_FAILED_GENESIS);

		long grace_period_in_ms = (long)billing_failed_grace_period_in_days * (long)(1000 * 60 * 60 * 24);

		if(now.getTime() > billing_failed_genesis.getTime()+grace_period_in_ms)
		{
			updateRecurringOrderStatus(recurring_order, ORDER_STATUS_BILLING_FAILED_GRACE_PERIOD_EXPIRED);
		}
		else
		{
			return;
		}
	}

	private void purge_billing_failed_users()
	{
		Entity order_user		= null;
		Entity recurring_order 	= null;
		Query q 				= null;
		QueryResult result 		= null;

		try{
			Date now = new Date();
			q = new Query(RECURRING_ORDER_ENTITY);
			q.idx(IDX_RECURRING_ORDER_BY_STATUS_BY_NEXT_BILL_DATE);
			q.betweenDesc(q.list(ORDER_STATUS_BILLING_FAILED_GRACE_PERIOD_EXPIRED,now), q.list(ORDER_STATUS_BILLING_FAILED_GRACE_PERIOD_EXPIRED,Query.VAL_MIN));
			q.cacheResults(false);
			result = QUERY(q);
			MODULE_LOG( 1,result.size()+" records in monthly billing failed grace period expired state.");
		}catch(PersistenceException pe1)
		{
			MODULE_LOG( 1,"ERROR: ABORTING HANDLE MONTHLY BILLING GRACE PERIOD EXPIRED FAILED CYCLE DUE TO FAILED QUERY. "+q);
			ERROR("ABORTING PURGING BILLING FAILED USERS DUE TO FAILED QUERY. "+q);
			return;
		}

		List<Entity> failed_orders = result.getEntities();
		for(int i = 0;i < failed_orders.size();i++)
		{
			recurring_order = failed_orders.get(i);
			try {
				FILL_REFS(recurring_order);
			} catch (PersistenceException e1) {
				ERROR(e1);
				MODULE_LOG("FAILED FILLING REFS IN HANDLE FAILED BILLING RECORDS. ORDER WAS: "+recurring_order);
				continue;
			}
			order_user     = (Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER);
			double amount = 0;
			try{
				amount = get_order_amount_with_promotions_applied(recurring_order);
			}catch(WebApplicationException wae)
			{
				MODULE_LOG("GETTING ORDER AMOUNT FOR FAILED ORDER: "+recurring_order);
				ERROR(wae);
				continue;
			}

			try{
				Date now = new Date();
				double balance = (Double)recurring_order.getAttribute(RECURRING_ORDER_FIELD_OUTSTANDING_BALANCE);
				balance += amount;
				UPDATE(recurring_order,
						RECURRING_ORDER_FIELD_LAST_BILL_DATE,now,
						RECURRING_ORDER_FIELD_NEXT_BILL_DATE,calculate_next_bill_date(recurring_order, now),
						RECURRING_ORDER_FIELD_OUTSTANDING_BALANCE,roundDouble(balance,2));
				log_order_billing_failed(recurring_order, amount," balance accruing. need updated card info! account in danger of being closed.");
				check_chuck_billing_failed_user(now, recurring_order);

			}catch(PersistenceException pe2)
			{
				MODULE_LOG("FAILED UPDATING FAILED ORDER "+recurring_order+" "+order_user);
				ERROR(pe2);
				continue;
			}
		}
	}



	private void check_chuck_billing_failed_user(Date now,Entity failed_order) throws PersistenceException
	{

		Date failed_genesis  				= (Date)failed_order.getAttribute(RECURRING_ORDER_FIELD_BILLING_FAILED_GENESIS);
		long grace_period_in_ms 			= (long)billing_failed_grace_period_in_days * (long)(1000 * 60 *60 * 24);
		long failed_billing_clean_period 	= grace_period_in_ms + (long)billing_failed_grace_period_expired_reap_period_in_days * (long)(1000 * 60 * 60 * 24);
		if(now.getTime() > failed_genesis.getTime()+failed_billing_clean_period)
		{
			updateRecurringOrderStatus(failed_order, ORDER_STATUS_CLOSED);
			Entity user = (Entity)failed_order.getAttribute(RECURRING_ORDER_FIELD_USER);
			MODULE_LOG( 2,"Deleting user and reaping order.grace period for failed billing expired: "+user.getAttribute(UserModule.FIELD_EMAIL)+" "+user);
			try{
				user_module.deleteUser(user);
			}catch(Exception e)
			{
				ERROR(e);
				MODULE_LOG("FAILED CHUCKING USER IN check_chuck_billing_failed_user: "+user);
			}
		}
		else
		{
			return;
		}
	}


	private void handle_trials()
	{
		Query q 				= null;
		QueryResult result 		= null;

		try{
			q = new Query(RECURRING_ORDER_ENTITY);
			q.idx(IDX_RECURRING_ORDER_BY_STATUS);
			q.eq(ORDER_STATUS_IN_TRIAL_PERIOD);
			result = QUERY(q);
			MODULE_LOG( 1,result.size()+" records currently in trial state.");
		}catch(PersistenceException pe1)
		{
			MODULE_LOG( 1,"ERROR: ABORTING BILLING CYCLE DUE TO FAILED QUERY. "+q);
			ERROR("ABORTING BILLING CYCLE DUE TO FAILED QUERY. "+q);
			return;
		}


		List<Entity> in_trial_orders = result.getEntities();
		Entity trial_order = null;
		Date now = new Date();
		for(int i = 0;i < in_trial_orders.size();i++)
		{
			try{
				trial_order 			= in_trial_orders.get(i);
				check_expired_trial(now, trial_order);
			}catch(Exception e)
			{
				ERROR(e);
				MODULE_LOG( 1,"ERROR: ABORTING BILLING CYCLE DUE TO FAILED QUERY. "+q);
				break;
			}
		}

	}


	private void purge_expired_trial_users()
	{
		Query q 				= null;
		QueryResult result 		= null;

		try{
			q = new Query(RECURRING_ORDER_ENTITY);
			q.idx(IDX_RECURRING_ORDER_BY_STATUS);
			q.eq(ORDER_STATUS_TRIAL_EXPIRED);
			result = QUERY(q);
			MODULE_LOG( 1,result.size()+" records currently in trial expired state.");
		}catch(PersistenceException pe1)
		{
			MODULE_LOG( 1,"ERROR: ABORTING BILLING CYCLE DUE TO FAILED QUERY. "+q);
			ERROR("ABORTING BILLING CYCLE DUE TO FAILED QUERY. "+q);
			return;
		}


		List<Entity> expired_trial_orders = result.getEntities();
		Entity expired_trial_order = null;
		Date now = new Date();
		for(int i = 0;i < expired_trial_orders.size();i++)
		{
			try{
				expired_trial_order 			= expired_trial_orders.get(i);
				check_chuck_trial_user(now, expired_trial_order);
			}catch(Exception e)
			{
				ERROR(e);
				MODULE_LOG( 1,"ERROR: ABORTING BILLING CYCLE DUE TO FAILED QUERY. "+q);
				break;
			}
		}
	}



	private void check_expired_trial(Date now,Entity trial_order) throws PersistenceException
	{

		Date trial_create_date  =  (Date)trial_order.getAttribute(FIELD_DATE_CREATED);
		long trial_period_in_ms = (long)trial_period_in_days * (long)(1000 * 60*60*24);
		if(now.getTime() > trial_create_date.getTime()+trial_period_in_ms)
		{
			updateRecurringOrderStatus(trial_order, ORDER_STATUS_TRIAL_EXPIRED);
			String additional_info = "Trial expired on: "+new Date(trial_create_date.getTime()+trial_period_in_ms);
			send_trial_expired_email(trial_order, additional_info);
			Entity user = EXPAND((Entity)trial_order.getAttribute(RECURRING_ORDER_FIELD_USER));
			MODULE_LOG( 2,"trial expired for user: "+user.getAttribute(UserModule.FIELD_EMAIL)+" "+user);
		}
		else
		{
			return;
		}
	}

	private void check_chuck_trial_user(Date now,Entity trial_order) throws PersistenceException
	{

		Date trial_create_date  		= (Date)trial_order.getAttribute(FIELD_DATE_CREATED);
		long trial_period_in_ms 			= (long)trial_period_in_days * (long)(1000 * 60 *60*24);
		long expired_trial_clean_period 	= (long)trial_period_in_ms + (long)expired_trial_reap_period_in_days * (1000 * 60*60*24);
		if(now.getTime() > trial_create_date.getTime()+expired_trial_clean_period)
		{
			updateRecurringOrderStatus(trial_order, ORDER_STATUS_CLOSED);
			Entity user = (Entity)trial_order.getAttribute(RECURRING_ORDER_FIELD_USER);
			MODULE_LOG( 2,"reaping trial for user: "+user.getAttribute(UserModule.FIELD_EMAIL)+" "+user);
			try{
				user_module.deleteUser(user);
			}catch(Exception e)
			{
				ERROR(e);
				MODULE_LOG("FAILED DELETING TRIAL USER in check_chuck_trial_user: "+user);
			}
		}
		else
		{
			return;
		}

	}

	private void send_welcome_email(Entity recurring_order,String additional_info)
	{
		Entity user = null;
		String user_email = null;
		try{
			user = EXPAND((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER));
			Map<String,Object> template_data = new HashMap<String, Object>();
			String username = (String)user.getAttribute(UserModule.FIELD_USERNAME);
			if(username == null || username.trim() == "")
				username = (String)user.getAttribute(UserModule.FIELD_EMAIL);
			template_data.put("username",username);

			if(additional_info == null)
				template_data.put("additional_information","");
			else
				template_data.put("additional_information",additional_info);
			user_email = (String)user.getAttribute(UserModule.FIELD_EMAIL);
			email_module.sendEmail(null, new String[]{user_email}, "Welcome to Postera.com.", "welcome.fm", template_data);
		}catch(Exception e)
		{
			e.printStackTrace();
			MODULE_LOG("EMAIL MODULE FAILED SENDING WELCOME EMAIL TO USER "+user.getId()+" "+user_email);
		}
	}

	private void send_billing_ok_email(Entity recurring_order,double amount,BillingGatewayResponse bgr)
	{
		Entity user = null;
		String user_email = null;
		Entity billing_record = null;

		try{
			user = EXPAND((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER));
			String username = (String)user.getAttribute(UserModule.FIELD_USERNAME);
			user_email = (String)user.getAttribute(UserModule.FIELD_EMAIL);
			if(username == null || username.trim() == "")
				username = (String)user.getAttribute(UserModule.FIELD_EMAIL);
			Date d = (Date)recurring_order.getAttribute(RECURRING_ORDER_FIELD_LAST_BILL_DATE);
			billing_record = billing_module.getPreferredBillingRecord(user);
			String first_name = (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_FIRST_NAME);
			String last_name = (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_LAST_NAME);
			String address1 = (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_ADDRESS_LINE_1);
			String address2 = (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_ADDRESS_LINE_2);
			String city = (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_CITY);
			String st = (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_STATE);
			String zip = (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_POSTAL_CODE);
			String country = (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_COUNTRY);
			String amt = "$"+normalize_amount(amount);
			String gateway_ref = bgr.getRefCode();
			String gateway_auth = bgr.getAuthCode();
			long order_id = recurring_order.getId();
			Map<String,Object> template_data = new HashMap<String, Object>();
			template_data.put("username",username);
			template_data.put("user_email",user_email);
			template_data.put("first_name",first_name);
			template_data.put("last_name",last_name);
			template_data.put("address1",address1);
			template_data.put("address2",address2);
			template_data.put("city",city);
			template_data.put("state",st);
			template_data.put("zip",zip);
			template_data.put("country",country);
			template_data.put("amount",amt);
			template_data.put("invoice_number",order_id);
			template_data.put("invoice_date",d);
			template_data.put("gateway_ref",gateway_ref);
			template_data.put("gateway_auth",gateway_auth);
			email_module.sendEmail(null, new String[]{user_email}, "Postera.com: Payment Receipt ["+order_id+"]", "recurring-bill.fm", template_data);
		}catch(Exception e)
		{
			e.printStackTrace();
			MODULE_LOG("EMAIL MODULE FAILED SENDING WELCOME EMAIL TO USER "+user.getId()+" "+user_email);
		}
	}

	private void send_billing_failed_email(Entity recurring_order,String additional_info)
	{
		Entity user = null;
		String user_email = null;
		try{
			user = EXPAND((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER));
			Map<String,Object> template_data = new HashMap<String, Object>();

			String username = (String)user.getAttribute(UserModule.FIELD_USERNAME);
			if(username == null || username.trim() == "")
				username = (String)user.getAttribute(UserModule.FIELD_EMAIL);
			template_data.put("username",username);

			if(additional_info == null)
				template_data.put("additional_information","");
			else
				template_data.put("additional_information",additional_info);
			user_email = (String)user.getAttribute(UserModule.FIELD_EMAIL);
			email_module.sendEmail(null, new String[]{user_email}, "Your Postera.com monthly billing failed.", "billing-failed.fm", template_data);
		}catch(Exception e)
		{
			e.printStackTrace();
			MODULE_LOG("EMAIL MODULE FAILED SENDING BILLING FAILED EMAIL TO USER "+user.getId()+" "+user_email);
		}
	}

	private void send_billing_failed_grace_period_expired_email(Entity recurring_order,String additional_info)
	{
		Entity user = null;
		String user_email = null;
		try{
			user = EXPAND((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER));
			Map<String,Object> template_data = new HashMap<String, Object>();
			template_data.put("username",(String)user.getAttribute(UserModule.FIELD_EMAIL));
			template_data.put("billing_failed_genesis",String.valueOf(recurring_order.getAttribute(RECURRING_ORDER_FIELD_BILLING_FAILED_GENESIS)));
			if(additional_info == null)
				template_data.put("additional_information","");
			else
				template_data.put("additional_information",additional_info);
			user_email = (String)user.getAttribute(UserModule.FIELD_EMAIL);
			email_module.sendEmail(null, new String[]{user_email}, "Your Postera.com account is delinquent.", "billing-failed-grace-period-expired.fm", template_data);
		}catch(Exception e)
		{
			e.printStackTrace();
			MODULE_LOG("EMAIL MODULE FAILED SENDING BILLING FAILED GRACE PERIOD EXPIRED EMAIL TO USER "+user.getId()+" "+user_email);
		}
	}

	public void send_account_closed_due_to_grace_period_expired_email(Entity recurring_order,String additional_info)
	{
		Entity user = null;
		String user_email = null;
		try{
			user = EXPAND((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER));
			Map<String,Object> template_data = new HashMap<String, Object>();
			template_data.put("username",(String)user.getAttribute(UserModule.FIELD_EMAIL));
			template_data.put("billing_failed_genesis",String.valueOf(recurring_order.getAttribute(RECURRING_ORDER_FIELD_BILLING_FAILED_GENESIS)));
			if(additional_info == null)
				template_data.put("additional_information","");
			else
				template_data.put("additional_information",additional_info);
			user_email = (String)user.getAttribute(UserModule.FIELD_EMAIL);
			email_module.sendEmail(null, new String[]{user_email}, "Your Postera.com account has been closed.", "account-closed-grace-period-expired.fm", template_data);
		}catch(Exception e)
		{
			e.printStackTrace();
			MODULE_LOG("EMAIL MODULE FAILED SENDING ACCOUNT CLOSED GRACE PERIOD EXPIRED EMAIL TO USER "+user.getId()+" "+user_email);
		}
	}

	public void send_account_closed_email(Entity recurring_order,String additional_info)
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
			email_module.sendEmail(null, new String[]{user_email}, "Your Postera.com account has been closed.", "account-closed.fm", template_data);
		}catch(Exception e)
		{
			e.printStackTrace();
			MODULE_LOG("EMAIL MODULE FAILED SENDING ACCOUNT CLOSED GRACE PERIOD EXPIRED EMAIL TO USER "+user.getId()+" "+user_email);
		}
	}

	private void send_trial_expired_email(Entity recurring_order,String additional_info)
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
			email_module.sendEmail(null, new String[]{user_email}, "Your Postera Trial Period Has Ended.", "trial-expired.fm", template_data);
		}catch(Exception e)
		{
			e.printStackTrace();
			MODULE_LOG("EMAIL MODULE FAILED SENDING TRIAL EXPIRED EMAIL TO USER "+user.getId()+" "+user_email);
		}
	}

	///TRANSACTION LOGGING STUFF
	private static final int LOG_ORDER_OPENED 	      		= 0x10;
	private static final int LOG_ORDER_SUSPENDED 	      	= 0x20;
	private static final int LOG_ORDER_CLOSED 	      		= 0x30;
	private static final int LOG_INIT_BILLING_OK 	      	= 0x40;
	private static final int LOG_INIT_BILLING_FAILED    	= 0x50;
	private static final int LOG_MONTHLY_BILLING_OK 	  	= 0x60;
	private static final int LOG_BILLING_FAILED 			= 0x70;
	private static final int LOG_TRIAL_STARTED				= 0x80;
	private static final int LOG_TRIAL_EXPIRED	 			= 0x90;

	private void log_order_opened(Entity recurring_order,boolean reopend) throws PersistenceException
	{
		String msg;
		if(reopend)
			msg = "reopened";
		else
			msg = "opened";
		logger_module.createLogMessage((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER), LOG_ORDER_OPENED, "Order "+recurring_order.getId()+" "+msg+" OK.", recurring_order);
	}

	private void log_order_suspended(Entity recurring_order,int old_status) throws PersistenceException
	{
		int current_status = (Integer)recurring_order.getAttribute(RECURRING_ORDER_FIELD_STATUS);
		logger_module.createLogMessage((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER), LOG_ORDER_SUSPENDED, "Order "+recurring_order.getId()+" has been suspended. Order status was "+OSS(old_status)+" and is now "+OSS(current_status)+".", recurring_order);
	}

	private void log_order_delinquent(Entity recurring_order,int old_status) throws PersistenceException
	{
		int current_status = (Integer)recurring_order.getAttribute(RECURRING_ORDER_FIELD_STATUS);
		logger_module.createLogMessage((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER), LOG_ORDER_SUSPENDED, "Order "+recurring_order.getId()+" is now delinquent. Order status was "+OSS(old_status)+" and is now "+OSS(current_status)+". Please update your billing info.", recurring_order);
	}


	private void log_order_closed(Entity recurring_order) throws PersistenceException
	{
		logger_module.createLogMessage((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER), LOG_ORDER_CLOSED, "Order "+recurring_order.getId()+" has been closed.", recurring_order);
	}

	private void log_order_init_bill_ok(Entity recurring_order,double amount,BillingGatewayResponse bgr) throws PersistenceException
	{
		logger_module.createLogMessage((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER), LOG_INIT_BILLING_OK, "Order "+recurring_order.getId()+" was billed for an initial fee of $"+normalize_amount(amount)+". REF:"+bgr.getRefCode()+" AUTH:"+bgr.getAuthCode(), recurring_order);
	}

	private void log_order_init_bill_failed(Entity recurring_order,double amount,String fail_message) throws PersistenceException
	{
		logger_module.createLogMessage((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER), LOG_INIT_BILLING_FAILED, "Order "+recurring_order.getId()+" failed initial billing for the amount of $"+normalize_amount(amount)+". MSG:"+fail_message, recurring_order);
	}

	private void log_order_monthly_bill_ok(Entity recurring_order,double amount,BillingGatewayResponse bgr) throws PersistenceException
	{
		logger_module.createLogMessage((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER), LOG_MONTHLY_BILLING_OK, "Order "+recurring_order.getId()+" was billed for a monthly fee of $"+normalize_amount(amount)+". REF:"+bgr.getRefCode()+" AUTH:"+bgr.getAuthCode(), recurring_order);
	}

	private void log_order_catchup_bill_ok(Entity recurring_order,double amount,BillingGatewayResponse bgr) throws PersistenceException
	{
		logger_module.createLogMessage((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER), LOG_MONTHLY_BILLING_OK, "Order "+recurring_order.getId()+" was billed for a fee of $"+normalize_amount(roundDouble(amount,2))+". REF:"+bgr.getRefCode()+" AUTH:"+bgr.getAuthCode(), recurring_order);
	}

	private void log_order_billing_failed(Entity recurring_order,double amount,String fail_msg) throws PersistenceException
	{
		logger_module.createLogMessage((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER), LOG_BILLING_FAILED, "Order "+recurring_order.getId()+" failed monthly billing for the amount of $"+normalize_amount(amount)+". MSG:"+fail_msg, recurring_order);
	}

	private void log_order_trial_started(Entity recurring_order) throws PersistenceException
	{
		logger_module.createLogMessage((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER), LOG_TRIAL_STARTED, "Trial order "+recurring_order.getId()+" was opened.", recurring_order);
	}

	private void log_order_trial_expired(Entity recurring_order) throws PersistenceException
	{
		logger_module.createLogMessage((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER), LOG_TRIAL_EXPIRED, "Trial order "+recurring_order.getId()+" has expired.",recurring_order);
	}

 	private static String normalize_amount(double amt)
  	{
    	DecimalFormat df = new DecimalFormat("#####.00");
    	return df.format(amt);
  	}

	public static final double roundDouble(double d, int places) {
        return Math.round(d * Math.pow(10, (double) places)) / Math.pow(10,
            (double) places);
    }

	public int getTrialPeriodInDays()
	{
		return trial_period_in_days;
	}

	public int getExpiredTrialReapPeriodInDays()
	{
		return expired_trial_reap_period_in_days;
	}

	public BillingModule getBillingModule()
	{
		return billing_module;
	}

	public boolean isConfigured()
	{
		return billing_module.isConfigured();
	}


	public void onDestroy()
	{
		super.onDestroy();

		synchronized (getApplication().getApplicationLock())
		{
			billing_thread_running = false;
			billing_thread.interrupt();
		}

	}

	public void send_promo_script_failed_email(WebApplicationException e1,Entity recurring_order)
	{
		//script exception//
		String message = "Hey Dudes. There was a script exception at "+new Date()+
		"("+e1.getMessage()+ ") while applying promotions to order "+recurring_order+
		". The order was not billed it needs to be looked in to";
		MODULE_LOG(message);
		Map<String,Object> data = new HashMap<String,Object>();
		data.put("message", message);
		try {
			email_module.sendEmail("support@postera.com", new String[]{"topher@topher.com","david@posttool.com"}, "script exception", "generic.fm", data);
		} catch (WebApplicationException e) {
			ERROR(e);
		}

	}

	private void send_email_to_us(String msg)
	{
		Map<String,Object> data = new HashMap<String,Object>();
		data.put("message", msg);
		try {
			email_module.sendEmail("support@postera.com", new String[]{"topher@topher.com","david@posttool.com"}, "BILLING PROBLEM", "generic.fm", data);
		} catch (WebApplicationException e) {
			ERROR(e);
		}
	}

	public void send_billing_not_configured()
	{
		//script exception//
		String message = "Hey Dudes. Billing is not configured ... ERROR: ABORTING BILLING CYCLE DUE TO BILLING MODULE NOT BEING CONFIGURED.MAKE SURE ENCRYPTION MODULE IS CONFIGURED.";
		MODULE_LOG(message);
		Map<String,Object> data = new HashMap<String,Object>();
		data.put("message", message);
		try {
			email_module.sendEmail("support@postera.com", new String[]{"topher@topher.com","david@posttool.com"}, "script exception", "generic.fm", data);
		} catch (WebApplicationException e) {
			ERROR(e);
		}

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
	public static String RECURRING_ORDER_FIELD_SKUS	 			= "skus";/* TODO: need to drop this field at some point */
	public static String RECURRING_ORDER_FIELD_LINE_ITEMS	 	= "line_items";
	public static String RECURRING_ORDER_FIELD_STATUS 			= "order_status";
	public static String RECURRING_ORDER_FIELD_LAST_BILL_DATE 	= "last_bill_date";
	public static String RECURRING_ORDER_FIELD_NEXT_BILL_DATE 	= "next_bill_date";
	public static String RECURRING_ORDER_FIELD_PROMOTIONS		= "promotions";
	public static String RECURRING_ORDER_FIELD_RECURRING_UNIT 	= "recurring_unit";//in days//
	public static String RECURRING_ORDER_FIELD_RECURRING_PERIOD = "recurring_period";//in unit//
	public static String RECURRING_ORDER_FIELD_OUTSTANDING_BALANCE 	    = "outstanding_balance";//in unit//
	public static String RECURRING_ORDER_FIELD_BILLING_FAILED_GENESIS 	= "billing_failed_genesis";

	public static String RECURRING_ORDER_LINE_ITEM_ENTITY 		     = "RecurringOrderLineItems";
	public static String RECURRING_ORDER_LINE_ITEM_FIELD_SKU		 = "sku";
	public static String RECURRING_ORDER_LINE_ITEM_FIELD_PRICE		 = "price";
	public static String RECURRING_ORDER_LINE_ITEM_FIELD_INITIAL_FEE = "initial_fee";
	public static String RECURRING_ORDER_LINE_ITEM_FIELD_CODE		 = "code";


	public static final int RECURRING_SKU_CATALOG_STATE_INACTIVE   = 0;
	public static final int RECURRING_SKU_CATALOG_STATE_ACTIVE     = 1;

	protected void defineEntities(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY(RECURRING_SKU_ENTITY,
					  RECURRING_SKU_FIELD_TITLE,Types.TYPE_STRING,"",
					  RECURRING_SKU_FIELD_DESCRIPTION,Types.TYPE_STRING,"",
					  RECURRING_SKU_FIELD_RESOURCE,Types.TYPE_REFERENCE, RECURRING_SKU_RESOURCE_ENTITY,null,
					  RECURRING_SKU_FIELD_INITIAL_FEE,Types.TYPE_DOUBLE,0.0,
					  RECURRING_SKU_FIELD_RECURRING_PRICE,Types.TYPE_DOUBLE,0.0,
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
				  	  RECURRING_ORDER_FIELD_LINE_ITEMS,Types.TYPE_ARRAY | Types.TYPE_REFERENCE,RECURRING_ORDER_LINE_ITEM_ENTITY,null,
					  RECURRING_ORDER_FIELD_STATUS,Types.TYPE_INT,ORDER_STATUS_INIT,
					  RECURRING_ORDER_FIELD_LAST_BILL_DATE,Types.TYPE_DATE,null,
					  RECURRING_ORDER_FIELD_NEXT_BILL_DATE,Types.TYPE_DATE,null,
					  RECURRING_ORDER_FIELD_PROMOTIONS,Types.TYPE_REFERENCE|Types.TYPE_ARRAY, promotion_module.PROMOTION_INSTANCE_ENTITY,new ArrayList<Entity>(),
					  RECURRING_ORDER_FIELD_RECURRING_UNIT,Types.TYPE_INT,Calendar.DATE,
					  RECURRING_ORDER_FIELD_RECURRING_PERIOD,Types.TYPE_INT,Integer.MAX_VALUE,
					  RECURRING_ORDER_FIELD_OUTSTANDING_BALANCE,Types.TYPE_DOUBLE,0.0,
					  RECURRING_ORDER_FIELD_BILLING_FAILED_GENESIS,Types.TYPE_DATE,null
					  );

		DEFINE_ENTITY(RECURRING_ORDER_LINE_ITEM_ENTITY,
					  RECURRING_ORDER_LINE_ITEM_FIELD_INITIAL_FEE,Types.TYPE_DOUBLE,0.0,
					  RECURRING_ORDER_LINE_ITEM_FIELD_PRICE,Types.TYPE_DOUBLE,0.0,
					  RECURRING_ORDER_LINE_ITEM_FIELD_SKU,Types.TYPE_REFERENCE,RECURRING_SKU_ENTITY,null,
					  RECURRING_ORDER_LINE_ITEM_FIELD_CODE,Types.TYPE_STRING,""
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
