package com.pagesociety.web.module.ecommerce.recurring;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import com.pagesociety.web.module.ecommerce.BillingGatewayException;
import com.pagesociety.web.module.ecommerce.BillingModule;
import com.pagesociety.web.module.ecommerce.IBillingGateway;
import com.pagesociety.web.module.resource.ResourceModule;
import com.pagesociety.web.module.user.UserModule;


public class RecurringOrderModule extends ResourceModule 
{
	
	private static final String SLOT_BILLING_MODULE = "billing-module";
	private static final String PARAM_BILLING_THREAD_INTERVAL = "billing-thread-interval";
	
	public static final int ORDER_STATUS_INIT 						= 0x0000;
	public static final int ORDER_STATUS_OPEN 						= 0x0001;
	public static final int ORDER_STATUS_CLOSED  					= 0x0002;
	public static final int ORDER_STATUS_INITIAL_BILL_FAILED 		= 0x0004;
	public static final int ORDER_STATUS_LAST_MONTHLY_BILL_FAILED 	= 0x0008;

	protected BillingModule billing_module;
	protected IBillingGateway billing_gateway;
	
	private long billing_thread_interval;
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);
		/* see notes in super class...this is cheap inheritence */
		super.setResourceEntityName(RECURRING_SKU_RESOURCE_ENTITY);
		billing_module  = (BillingModule)getSlot(SLOT_BILLING_MODULE);
		billing_gateway = billing_module.getBillingGateway(); 
		billing_thread_interval = Long.parseLong(GET_REQUIRED_CONFIG_PARAM(PARAM_BILLING_THREAD_INTERVAL, config));
	
		start_billing_thread();
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_BILLING_MODULE, com.pagesociety.web.module.ecommerce.BillingModule.class, true);
	}
	

	File   current_log_file;
	Writer current_log_writer;
	private static final String LOG_EXTENSION = "log";
	private Writer get_current_log_file_writer()
	{
		Calendar now = Calendar.getInstance();
		
		String year  = String.valueOf(now.get(Calendar.YEAR));
		String month = String.valueOf(now.get(Calendar.MONTH)+1);
		String day   = String.valueOf(now.get(Calendar.DATE));
		
		StringBuilder buf = new StringBuilder();
		buf.append(year);
		buf.append(month);
		buf.append(day);
		buf.append('.');
		buf.append(getName());
		buf.append('.');
		buf.append(LOG_EXTENSION);
		String current_log_filename = buf.toString();
		
		try{
			current_log_file = GET_MODULE_DATA_FILE(getApplication(), current_log_filename, false);
			if(current_log_file == null || current_log_writer == null)
			{
				current_log_file = GET_MODULE_DATA_FILE(getApplication(), current_log_filename, true);
				try{
					if(current_log_writer != null)
						current_log_writer.close();
					current_log_writer = new BufferedWriter(new FileWriter(current_log_file,true));
				}catch(IOException ioe)
				{
					ERROR("BIG TIME BARF ON LOG FILE SWITCHING.",ioe);
				}
			}
		}catch(WebApplicationException wae)
		{
			ERROR("PROBLEM GETTING CURRENT LOG FILE "+current_log_filename);
		}
		return current_log_writer;
	}
	
	protected void MODULE_LOG(String message)
	{
		MODULE_LOG(0, message);
    }

	protected void MODULE_LOG(int indent,String message)
	{
		Writer output = get_current_log_file_writer(); 
		Date now = new Date();
		
		try {
			if(message.startsWith("\n"))
			{
				output.write('\n');
				message = message.substring(1);
			}
			output.write(now+": ");
			for(int i = 0;i < indent;i++)
				output.write('\t');
			output.write(message+"\n");
			output.flush();
		}catch(IOException ioe)
		{
			ERROR("BARF ON MODULE_LOG() FUNCTION.MESSAGE WAS "+message,ioe);
		}
    }

	
	/////////////////BEGIN  M O D U L E   F U N C T I O N S/////////////////////////////////////////

	
	///RECURRING SKU
	@Export
	public PagingQueryResult GetActiveRecurringSKUs(UserApplicationContext uctx,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		//TODO: potential guard //
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
	
	
	@Export
	public Entity CreateRecurringSKU(UserApplicationContext uctx,Entity recurring_sku) throws WebApplicationException,PersistenceException
	{
		VALIDATE_TYPE(RECURRING_SKU_ENTITY, recurring_sku);
		
		String product_name 	   = (String)recurring_sku.getAttribute(RECURRING_SKU_FIELD_TITLE);
		String product_description = (String)recurring_sku.getAttribute(RECURRING_SKU_FIELD_DESCRIPTION);
		float  product_price       = (Float)recurring_sku.getAttribute(RECURRING_SKU_FIELD_RECURRING_PRICE);
		int	   billing_period	   = (Integer)recurring_sku.getAttribute(RECURRING_SKU_FIELD_BILLING_PERIOD);
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
		
		return CreateRecurringSKU(uctx,product_name, product_description,product_price,billing_period,catalog_no,recurring_sku_resource_id,user_data_type,user_data_id,catalog_state);

	}
	
	@Export //TODO: how do we hook into promotions here.//
	public Entity CreateRecurringSKU(UserApplicationContext uctx,String product_name,String product_description,float product_price,int billing_period,String catalog_no,long recurring_sku_resource_id,String user_data_type,long user_data_id,int catalog_state) throws WebApplicationException,PersistenceException
	{
		Entity user 	   	= (Entity)uctx.getUser();
		
		if(!PermissionsModule.IS_ADMIN(user))
			throw new WebApplicationException("NO PERMISSION");
		
		Entity user_data 	= null;
		if(user_data_type  != null)
			user_data = GET(user_data_type,user_data_id);
		
		Entity recurring_sku_resource = null;
		if(recurring_sku_resource_id != 0)//0 for a ref pointer means null//
			recurring_sku_resource = GET(RECURRING_SKU_ENTITY,recurring_sku_resource_id);
			
		return createRecurringSKU(user,product_name, product_description,product_price,billing_period,catalog_no,recurring_sku_resource,user_data,catalog_state);
	}
	
	
	public Entity createRecurringSKU(Entity creator,String product_name,String product_description,float product_price,int billing_period,String catalog_no,Entity recurring_sku_resource,Entity user_data,int catalog_state) throws PersistenceException
	{
		return NEW(RECURRING_SKU_ENTITY,
				creator,
				RECURRING_SKU_FIELD_TITLE,product_name,
				RECURRING_SKU_FIELD_DESCRIPTION,product_description,
				RECURRING_SKU_FIELD_RECURRING_PRICE,product_price,
				RECURRING_SKU_FIELD_BILLING_PERIOD,billing_period,
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
		int	   billing_period	   = (Integer)recurring_sku.getAttribute(RECURRING_SKU_FIELD_BILLING_PERIOD);
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
		
		return UpdateRecurringSKU(uctx,recurring_sku.getId(), product_name, product_description, product_price, billing_period, catalog_no,recurring_sku_resource_id, user_data_type,user_data_id,catalog_state);
	}
	
	
	@Export
	public Entity UpdateRecurringSKU(UserApplicationContext uctx,long recurring_sku_id,String product_name,String product_description,float product_price,int billing_period,String catalog_no,long recurring_sku_resource_id,String user_data_type,long user_data_id,int catalog_state) throws WebApplicationException,PersistenceException
	{
		Entity user 	   	= (Entity)uctx.getUser();
		Entity recurring_sku = GET(RECURRING_SKU_ENTITY,recurring_sku_id);

		if(!PermissionsModule.IS_ADMIN(user))
			throw new WebApplicationException("NO PERMISSION");
		
		Entity user_data 	= null;
		if(user_data_type  != null)
			user_data = GET(user_data_type,user_data_id);
		
		Entity recurring_sku_resource = null;
		if(recurring_sku_resource_id != 0)//0 for a ref pointer means null//
			recurring_sku_resource = GET(RECURRING_SKU_ENTITY,recurring_sku_resource_id);
		
		return updateRecurringSKU(recurring_sku,product_name, product_description, product_price, billing_period, catalog_no, recurring_sku_resource,user_data,catalog_state);
	}
	
	
	public Entity updateRecurringSKU(Entity recurring_sku,String product_name,String product_description,float product_price,int billing_period,String catalog_no,Entity recurring_sku_resource,Entity user_data,int catalog_state) throws WebApplicationException,PersistenceException
	{
		Entity old_resource = (Entity)GET(RECURRING_SKU_ENTITY,recurring_sku.getId()).getAttribute(RECURRING_SKU_FIELD_RESOURCE);
		if(old_resource != null && !old_resource.equals(recurring_sku_resource))
			deleteResource(old_resource);
	
		return UPDATE(recurring_sku,
				RECURRING_SKU_FIELD_TITLE,product_name,
				RECURRING_SKU_FIELD_DESCRIPTION,product_description,
				RECURRING_SKU_FIELD_RECURRING_PRICE,product_price,
				RECURRING_SKU_FIELD_BILLING_PERIOD,billing_period,
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
			throw new WebApplicationException("NO PERMISSION");

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
		Entity recurring_sku 	= (Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_SKU);
		List<Entity> promotions = (List<Entity>)recurring_order.getAttribute(RECURRING_ORDER_FIELD_PROMOTIONS);
		
		VALIDATE_TYPE(RECURRING_SKU_ENTITY, recurring_sku);
		VALIDATE_TYPE_LIST(PROMOTION_ENTITY, promotions);
		
		return CreateRecurringOrder(uctx,target_user_id,recurring_sku.getId(),ENTITIES_TO_IDS(promotions));
	}
	
	@Export 
	public Entity CreateRecurringOrder(UserApplicationContext uctx,long target_user_id,long recurring_sku_id,List<Long> promotion_ids) throws WebApplicationException,PersistenceException
	{
		Entity user 	   = (Entity)uctx.getUser();
		Entity target_user = GET(UserModule.USER_ENTITY,target_user_id);
		
		if(!PermissionsModule.IS_ADMIN(user) && !PermissionsModule.IS_SAME(user, target_user))
			throw new PermissionsException("NO PERMISSION");

		Entity sku 				= GET(RECURRING_SKU_ENTITY,recurring_sku_id);
		List<Entity> promotions = IDS_TO_ENTITIES(PROMOTION_ENTITY, promotion_ids);
		
		//TODO: pass through initial fee
		return createRecurringOrder(user,target_user,sku,0f,promotions);

	}
	
	//TODO: do initial billing. set prev and next bill dates,deal with promotions //
	public Entity createRecurringOrder(Entity creator,Entity user,Entity sku,float initial_fee,List<Entity> promotions) throws PersistenceException
	{

		Entity recurring_order =  NEW(RECURRING_ORDER_ENTITY,
									  creator,
									  RECURRING_ORDER_FIELD_SKU,sku,
									  RECURRING_ORDER_FIELD_USER,user,
									  RECURRING_ORDER_FIELD_INITIAL_FEE,initial_fee,
									  RECURRING_ORDER_FIELD_STATUS,ORDER_STATUS_INIT,
									  RECURRING_ORDER_FIELD_LAST_BILL_DATE,null,
									  RECURRING_ORDER_FIELD_NEXT_BILL_DATE,new Date(),
									  RECURRING_ORDER_FIELD_PROMOTIONS,new ArrayList<Entity>());//TODO: deal with promotions

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

	
	public Entity openRecurringOrder(Entity recurring_order) throws PersistenceException
	{
		
		recurring_order = EXPAND(recurring_order);
		MODULE_LOG(0,"OPENING RECURRING ORDER "+recurring_order.getId());
		int status = (Integer)recurring_order.getAttribute(RECURRING_ORDER_FIELD_STATUS);
		
		Entity order_user     = (Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER);
		Entity billing_record = billing_module.getPreferredBillingRecord(order_user);
		//TODO: deal with preferred billing record not being set. 
		//this relates to free acounts potentially!!//
		//how do we keep track of users with free accounts.
		//do they have to check out and enter a billing record//
		//or do we just make accounts for them//
		switch(status)
		{
			case ORDER_STATUS_INIT:
			case ORDER_STATUS_INITIAL_BILL_FAILED:

				try{
					do_initial_fee_billing(recurring_order,billing_record);
				}catch(BillingGatewayException bge)
				{
					//TODO: insert failed transaction record here
					
					updateRecurringOrderStatus(recurring_order, ORDER_STATUS_INITIAL_BILL_FAILED);	
					MODULE_LOG(0,"ERROR: RECURRING ORDER "+recurring_order.getId()+" INITIAL BILLING FAILED.");
					return recurring_order;
				}
				//TODO: insert successful initial billing transaction record here
				
				try{
					do_monthly_billing(recurring_order,billing_record);
				}catch(BillingGatewayException bge2)
				{
					//TODO: insert failed trans action record here
					updateRecurringOrderStatus(recurring_order, ORDER_STATUS_LAST_MONTHLY_BILL_FAILED);	
					MODULE_LOG(0,"ERROR: RECURRING ORDER "+recurring_order.getId()+" FIRST MONTHLY BILLING FAILED.");
					return recurring_order;
				}
				//TODO: insert successful trans action record here
				updateRecurringOrderStatus(recurring_order, ORDER_STATUS_OPEN);
				MODULE_LOG(0,"OPENED RECURRING ORDER "+recurring_order.getId()+" OK");
				break;
				
			case ORDER_STATUS_LAST_MONTHLY_BILL_FAILED://just reopen it. probaby need to see how much time elapsed.
				try{
					do_monthly_billing(recurring_order,billing_record);
				}catch(BillingGatewayException bge2)
				{
					//TODO: insert failed trans action record here
					updateRecurringOrderStatus(recurring_order, ORDER_STATUS_LAST_MONTHLY_BILL_FAILED);	
					MODULE_LOG(0,"ERROR: RECURRING ORDER "+recurring_order.getId()+" FIRST MONTHLY BILLING FAILED");
					return recurring_order;
				}
				//TODO: insert successful trans action record here
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
		return updateRecurringOrderStatus(recurring_order, ORDER_STATUS_CLOSED);
	}
	
	public Entity updateRecurringOrderStatus(Entity recurring_order,int status) throws PersistenceException
	{
		return UPDATE(recurring_order,
					RECURRING_ORDER_FIELD_STATUS,status);
	}
	

	

	
	
	@Export
	public PagingQueryResult GetRecurringOrdersByStatus(UserApplicationContext uctx,int status) throws WebApplicationException,PersistenceException
	{
		return null;
	}
	
	public PagingQueryResult getRecurringOrdersByStatus(UserApplicationContext uctx,int status) throws WebApplicationException,PersistenceException
	{
		return null;
	}
	
	@Export
	public PagingQueryResult GetRecurringOrdersByUser(UserApplicationContext uctx,long user_id) throws WebApplicationException,PersistenceException
	{
		return null;
	}
	
	
	/////////////////E N D  M O D U L E   F U N C T I O N S/////////////////////////////////////////
		
	private void do_initial_fee_billing(Entity recurring_order,Entity billing_record) throws PersistenceException,BillingGatewayException
	{
		float initial_fee = (Float)recurring_order.getAttribute(RECURRING_ORDER_FIELD_INITIAL_FEE);
		if(initial_fee == 0)
			return;

		//TODO: look for any promotion that waves initial fee.//
		//TODO: maybe insert our own transaction record for 0 billing if initial fee was waived
		//due to promotion

		billing_gateway.doSale(billing_record, initial_fee);	
	}
	
	private void do_monthly_billing(Entity recurring_order,Entity billing_record) throws PersistenceException,BillingGatewayException
	{
		Date next_bill_date = (Date)recurring_order.getAttribute(RECURRING_ORDER_FIELD_NEXT_BILL_DATE);
		Date now     = new Date();
		if(now.getTime() < next_bill_date.getTime())
		{
			//cant bill a date before the next bill date
			//TODO: JUST LOG IN THE BILLING LOG FOR THE DAY billing_gateway.getLog()//
			MODULE_LOG(0,"WARNING: SKIPPING BILLING RECURRING ORDER BECAUSE NEXT BILL DATE IS IN THE FUTURE.RECURRING ORDER: "+recurring_order.getId());
			return;
		}
		
		Entity sku 	 = EXPAND((Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_SKU));
		float amount = (Float)sku.getAttribute(RECURRING_SKU_FIELD_RECURRING_PRICE);
		//\\TODO: apply promotions //\\
		
		if(amount == 0)
		{
			//TODO: maybe insert our own transaction record for 0 billing
			return;
		}

		billing_gateway.doSale(billing_record, amount);
		recurring_order.setAttribute(RECURRING_ORDER_FIELD_LAST_BILL_DATE, now);
		recurring_order.setAttribute(RECURRING_ORDER_FIELD_NEXT_BILL_DATE, calculate_next_bill_date(sku,now));
		SAVE_ENTITY(recurring_order);

	}

	private Date calculate_next_bill_date(Entity sku, Date now)
	{
		Calendar c1 = Calendar.getInstance(); 
		c1.setTime(now);
		c1.add(Calendar.DATE,(Integer)sku.getAttribute(RECURRING_SKU_FIELD_BILLING_PERIOD));
		//c1.add(Calendar.SECOND,(Integer)sku.getAttribute(RECURRING_SKU_FIELD_BILLING_PERIOD));
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
			q.idx(IDX_RECURRING_ORDER_BY_NEXT_BILL_DATE);
			q.lte(now);
			result = QUERY(q);
			MODULE_LOG( 1,result.size()+" records to bill.");
		}catch(PersistenceException pe1)
		{
			MODULE_LOG( 1,"ERROR: ABORTING BILLING CYCLE DUE TO FAILED QUERY. "+q);
			ERROR("ABORTING BILLING CYCLE DUE TO FAILED QUERY. "+q);
			return;
		}
		
		List<Entity> orders_that_need_to_be_billed = result.getEntities();
		for(int i = 0;i < orders_that_need_to_be_billed.size();i++)
		{
			try{
				recurring_order = orders_that_need_to_be_billed.get(i);
				order_user     = (Entity)recurring_order.getAttribute(RECURRING_ORDER_FIELD_USER);
				billing_record = billing_module.getPreferredBillingRecord(order_user);
				if(billing_record == null)
				{
					MODULE_LOG( 1,"!!!MONTHLY BILL FAILED FOR RECURRING ORDER "+recurring_order.getId());
					MODULE_LOG( 1,"USER DOES NOT HAVE PREFERRED BILLING RECORD SOMEHOW "+order_user);
					continue;
				}
				try{
					do_monthly_billing(recurring_order, billing_record);

				}catch(BillingGatewayException bge)
				{
					MODULE_LOG( 1,"!!!MONTHLY BILL FAILED FOR RECURRING ORDER "+recurring_order.getId());
					updateRecurringOrderStatus(recurring_order, ORDER_STATUS_LAST_MONTHLY_BILL_FAILED);	
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
	
	///BEGIN DDL STUFF
	public static String RECURRING_SKU_ENTITY 			  		= "RecurringSKU";
	public static String RECURRING_SKU_FIELD_TITLE 			 	= "title";
	public static String RECURRING_SKU_FIELD_DESCRIPTION 		= "description";
	public static String RECURRING_SKU_FIELD_RESOURCE 		  	= "resource";
	public static String RECURRING_SKU_FIELD_RECURRING_PRICE 	= "price";
	public static String RECURRING_SKU_FIELD_CATALOG_NUMBER 	= "catalog_number";/*application provides(optional)*/
	public static String RECURRING_SKU_FIELD_USER_DATA 	  	  	= "data";	/*application provides(optional)*/
	public static String RECURRING_SKU_FIELD_BILLING_PERIOD 	= "billing_period";//in days//
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
	public static String RECURRING_ORDER_FIELD_SKU	 			= "sku";
	public static String RECURRING_ORDER_FIELD_INITIAL_FEE 	  	= "initial_fee";
	public static String RECURRING_ORDER_FIELD_STATUS 			= "order_status";
	public static String RECURRING_ORDER_FIELD_LAST_BILL_DATE 	= "last_bill_date";
	public static String RECURRING_ORDER_FIELD_NEXT_BILL_DATE 	= "next_bill_date";
	public static String RECURRING_ORDER_FIELD_PROMOTIONS		= "promotions";

	
	public static final String PROMOTION_ENTITY = "Promotion";
	public static final int RECURRING_SKU_CATALOG_STATE_INACTIVE   = 0; 
	public static final int RECURRING_SKU_CATALOG_STATE_ACTIVE     = 1; 

	protected void defineEntities(Map<String,Object> config) throws PersistenceException,SyncException
	{
		DEFINE_ENTITY(RECURRING_SKU_ENTITY,
					  RECURRING_SKU_FIELD_TITLE,Types.TYPE_STRING,"",
					  RECURRING_SKU_FIELD_DESCRIPTION,Types.TYPE_STRING,"",
					  RECURRING_SKU_FIELD_RESOURCE,Types.TYPE_REFERENCE, RECURRING_SKU_RESOURCE_ENTITY,null,
					  RECURRING_SKU_FIELD_RECURRING_PRICE,Types.TYPE_FLOAT,0.0f,
					  RECURRING_SKU_FIELD_CATALOG_NUMBER,Types.TYPE_STRING,null,
					  RECURRING_SKU_FIELD_USER_DATA,Types.TYPE_REFERENCE,FieldDefinition.REF_TYPE_UNTYPED_ENTITY,null,
					  RECURRING_SKU_FIELD_BILLING_PERIOD,Types.TYPE_INT,0,
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
					  RECURRING_ORDER_FIELD_SKU,Types.TYPE_REFERENCE,RECURRING_SKU_ENTITY,null,
					  RECURRING_ORDER_FIELD_INITIAL_FEE,Types.TYPE_FLOAT,0.0f,
					  RECURRING_ORDER_FIELD_STATUS,Types.TYPE_INT,ORDER_STATUS_INIT,
					  RECURRING_ORDER_FIELD_LAST_BILL_DATE,Types.TYPE_DATE,null,
					  RECURRING_ORDER_FIELD_NEXT_BILL_DATE,Types.TYPE_DATE,null,					  
					  RECURRING_ORDER_FIELD_PROMOTIONS,Types.TYPE_REFERENCE|Types.TYPE_ARRAY, PROMOTION_ENTITY,new ArrayList<Entity>());

	}

	public static final String IDX_RECURRING_ORDER_BY_NEXT_BILL_DATE = "byNextBillDate";
	
	public static final String IDX_RECURRING_SKU_BY_CATALOG_STATE 			 = "byCatalogState";
	public static final String IDX_SITE_BY_USER 	  = "byCreator";	
	
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,SyncException
	{
		DEFINE_ENTITY_INDEX(RECURRING_ORDER_ENTITY, IDX_RECURRING_ORDER_BY_NEXT_BILL_DATE, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX,RECURRING_ORDER_FIELD_NEXT_BILL_DATE);
		DEFINE_ENTITY_INDEX(RECURRING_SKU_ENTITY, IDX_RECURRING_SKU_BY_CATALOG_STATE, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX,RECURRING_SKU_FIELD_CATALOG_STATE);
	//	DEFINE_ENTITY_INDEX(SYSTEM_ENTITY, IDX_SYSTEM_BY_USER, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, FIELD_CREATOR);
	//	DEFINE_ENTITY_INDEX(SYSTEM_INSTANCE_ENTITY, IDX_SYSTEM_INSTANCE_BY_USER_BY_CURRENT_FLAG, EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, FIELD_CREATOR,SYSTEM_INSTANCE_FIELD_CURRENT_FLAG);
	//	DEFINE_ENTITY_INDEX(SYSTEM_INSTANCE_ENTITY, IDX_SYSTEM_INSTANCE_BY_USER_BY_SYSTEM, EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, FIELD_CREATOR,SYSTEM_INSTANCE_FIELD_SYSTEM);
	//	DEFINE_ENTITY_INDEX(SITE_ENTITY, IDX_SITE_BY_USER, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, FIELD_CREATOR);
	}
}
