package com.pagesociety.web.module.ecommerce;

import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.encryption.IEncryptionModule;


public class BillingModule extends WebStoreModule 
{

	private static final String SLOT_ENCRYPTION_MODULE  	 = "encryption-module"; 
	private static final String SLOT_BILLING_GATEWAY_MODULE  = "billing-gateway"; 
	private static final String SLOT_BILLING_GUARD_MODULE  		 = "billing-guard"; 

	IBillingGateway 	billing_gateway;
	IBillingGuard   	guard;
	IEncryptionModule   encryption_module;
	
	public static final int CC_TYPE_VISA 	   = 0x01;
	public static final int CC_TYPE_MASTERCARD = 0x02;
	public static final int CC_TYPE_AMEX 	   = 0x03;
	public static final int CC_TYPE_DISCOVER   = 0x04;
	
	public static final int EVENT_BILLING_RECORD_CREATED = 0x1001;
	public static final int EVENT_BILLING_RECORD_UPDATED = 0x1002;
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);
		billing_gateway 	= (IBillingGateway)getSlot(SLOT_BILLING_GATEWAY_MODULE);
		guard				= (IBillingGuard)getSlot(SLOT_BILLING_GUARD_MODULE);
		encryption_module 	= (IEncryptionModule)getSlot(SLOT_ENCRYPTION_MODULE);
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_BILLING_GATEWAY_MODULE,IBillingGateway.class,true);
		DEFINE_SLOT(SLOT_ENCRYPTION_MODULE,IEncryptionModule.class,true);
		DEFINE_SLOT(SLOT_BILLING_GUARD_MODULE,IBillingGuard.class,false,DefaultBillingGuard.class);
	
	}
	
	/////////////////BEGIN  M O D U L E   F U N C T I O N S/////////////////////////////////////////

	@Export
	public Entity CreateBillingRecord(UserApplicationContext uctx,Entity billing_record) throws WebApplicationException,PersistenceException,BillingGatewayException
	{

		VALIDATE_TYPE(BILLINGRECORD_ENTITY, billing_record);
		VALIDATE_NEW_INSTANCE(billing_record);

		return CreateBillingRecord(uctx,
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_FIRST_NAME),
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_MIDDLE_INITIAL),
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_LAST_NAME),
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_ADDRESS_LINE_1),
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_ADDRESS_LINE_2),
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_CITY),
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_STATE),
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_COUNTRY),
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_POSTAL_CODE),
								   (Integer)billing_record.getAttribute(BILLINGRECORD_FIELD_CC_TYPE),
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_CC_NO),
								   (Integer)billing_record.getAttribute(BILLINGRECORD_FIELD_EXP_MONTH),
								   (Integer)billing_record.getAttribute(BILLINGRECORD_FIELD_EXP_YEAR));
	
	}
	

	
	@Export
	public Entity CreateBillingRecord(UserApplicationContext uctx,
									  String first_name,
									  String middle_initial,
									  String last_name,
									  String add_1,
									  String add_2,
									  String city,
									  String state,									  
									  String country,
									  String postal_code,
									  int cc_type,
									  String cc_no,
									  int exp_month,
									  int exp_year) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(guard.canCreateBillingRecord(user,user));
		
		return createBillingRecord(user,first_name,middle_initial,last_name,add_1,add_2,city,state,country,postal_code,cc_type,cc_no,exp_month,exp_year,false);
	
	}
	
	
	public Entity createBillingRecord(Entity creator,String first_name,String middle_initial,String last_name,String add_1,String add_2,String city,String state,String country,String postal_code,int cc_type,String cc_no,int exp_month,int exp_year,boolean preferred) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		billing_gateway.doValidate(first_name,middle_initial,last_name,add_1,add_2,city,state,country,postal_code,cc_type,cc_no,exp_month,exp_year);	
		String last_4_digits = cc_no.substring(cc_no.length()-4);
		Entity billing_record =  NEW(BILLINGRECORD_ENTITY,
				   					creator,
				   					BILLINGRECORD_FIELD_FIRST_NAME,first_name,
				   					BILLINGRECORD_FIELD_MIDDLE_INITIAL,middle_initial,
				   					BILLINGRECORD_FIELD_LAST_NAME,last_name,
				   					BILLINGRECORD_FIELD_ADDRESS_LINE_1,add_1,
				   					BILLINGRECORD_FIELD_ADDRESS_LINE_2,add_2,
				   					BILLINGRECORD_FIELD_CITY,city,
				   					BILLINGRECORD_FIELD_STATE,state,
				   					BILLINGRECORD_FIELD_COUNTRY,country,
				   					BILLINGRECORD_FIELD_POSTAL_CODE,postal_code,
				   					BILLINGRECORD_FIELD_CC_TYPE,cc_type,
				   					BILLINGRECORD_FIELD_CC_NO,encryption_module.encryptString(cc_no),
				   					BILLINGRECORD_FIELD_LAST_FOUR_DIGITS,last_4_digits,
				   					BILLINGRECORD_FIELD_EXP_MONTH,exp_month,
				   					BILLINGRECORD_FIELD_EXP_YEAR,exp_year);
		if(preferred)
			setPreferredBillingRecord(creator,billing_record);
	
		dispatchEvent(EVENT_BILLING_RECORD_CREATED, billing_record);
		return billing_record;
	}
			  
	
	@Export
	public Entity UpdateBillingRecord(UserApplicationContext uctx,Entity billing_record) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		VALIDATE_TYPE(BILLINGRECORD_ENTITY, billing_record);
		VALIDATE_EXISTING_INSTANCE(billing_record);
		return UpdateBillingRecord(uctx,
									billing_record.getId(),
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_FIRST_NAME),
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_MIDDLE_INITIAL),
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_LAST_NAME),
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_ADDRESS_LINE_1),
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_ADDRESS_LINE_2),
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_CITY),
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_STATE),
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_COUNTRY),
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_POSTAL_CODE),
								   (Integer)billing_record.getAttribute(BILLINGRECORD_FIELD_CC_TYPE),
								   (String)billing_record.getAttribute(BILLINGRECORD_FIELD_CC_NO),
								   (Integer)billing_record.getAttribute(BILLINGRECORD_FIELD_EXP_MONTH),
								   (Integer)billing_record.getAttribute(BILLINGRECORD_FIELD_EXP_YEAR));

	}
	
	@Export
	public Entity UpdateBillingRecord(UserApplicationContext uctx,
									  long billing_record_id,
									  String first_name,
									  String middle_initial,
									  String last_name,
									  String add_1,
									  String add_2,
									  String city,
									  String state,									  
									  String country,
									  String postal_code,
									  int cc_type,
									  String cc_no,
									  int exp_month,
									  int exp_year)throws WebApplicationException,PersistenceException,BillingGatewayException
	  {
		
		Entity user = (Entity)uctx.getUser();
		GUARD(guard.canUpdateBillingRecord(user,user));
		Entity billing_record = GET(BILLINGRECORD_ENTITY,billing_record_id);
		return updateBillingRecord(billing_record,first_name,middle_initial,last_name,add_1,add_2,city,state,country,postal_code,cc_type,cc_no,exp_month,exp_year);
	  }
	
	
	public Entity updateBillingRecord(Entity billing_record,
			  						  String first_name,
			  						  String middle_initial,
			  						  String last_name,
			  						  String add_1,
			  						  String add_2,
			  						  String city,
			  						  String state,									  
			  						  String country,
			  						  String postal_code,
			  						  int cc_type,
			  						  String cc_no,
			  						  int exp_month,
			  						  int exp_year) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		
		billing_gateway.doValidate(first_name,middle_initial,last_name,add_1,add_2,city,state,country,postal_code,cc_type,cc_no,exp_month,exp_year);	
		String last_4_digits = cc_no.substring(cc_no.length()-4);
		
		billing_record = UPDATE(billing_record,
				BILLINGRECORD_FIELD_FIRST_NAME,first_name,
				BILLINGRECORD_FIELD_MIDDLE_INITIAL,middle_initial,
				BILLINGRECORD_FIELD_LAST_NAME,last_name,
				BILLINGRECORD_FIELD_ADDRESS_LINE_1,add_1,
				BILLINGRECORD_FIELD_ADDRESS_LINE_2,add_2,
				BILLINGRECORD_FIELD_CITY,city,
				BILLINGRECORD_FIELD_STATE,state,
				BILLINGRECORD_FIELD_COUNTRY,country,
				BILLINGRECORD_FIELD_POSTAL_CODE,postal_code,
				BILLINGRECORD_FIELD_CC_TYPE,cc_type,
				BILLINGRECORD_FIELD_CC_NO,encryption_module.encryptString(cc_no),
				BILLINGRECORD_FIELD_LAST_FOUR_DIGITS,last_4_digits,
				BILLINGRECORD_FIELD_EXP_MONTH,exp_month,
				BILLINGRECORD_FIELD_EXP_YEAR,exp_year);
	
		dispatchEvent(EVENT_BILLING_RECORD_UPDATED, billing_record);
		return billing_record;
	}
	
	
	@Export
	public PagingQueryResult GetBillingRecords(UserApplicationContext uctx,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(guard.canGetBillingRecords(user,user));		
		Query q = new Query(BILLINGRECORD_ENTITY);
		q.idx(IDX_BY_USER_BY_PREFERRED);
		q.eq(q.list(user,Query.VAL_GLOB));
		q.offset(offset);
		q.pageSize(page_size);
		return PAGING_QUERY(q);
	}
	
	@Export
	public Entity GetPreferredBillingRecord(UserApplicationContext uctx) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(guard.canGetBillingRecords(user,user));		
		return getPreferredBillingRecord(user);
	}
	
	public Entity getPreferredBillingRecord(Entity user)throws PersistenceException
	{
		Query q = new Query(BILLINGRECORD_ENTITY);
		q.idx(IDX_BY_USER_BY_PREFERRED);
		q.eq(q.list(user,true));
		
		QueryResult result = QUERY(q);
		int n = result.size();
		if(n == 0)
			return null;
		else if(n > 1)
			ERROR("DATA INTEGRETY ISSUE. MORE THAN ONE PREFERRED BILLING RECORD FOR USER "+user);
		
		return result.getEntities().get(0);		
	}
	
	@Export
	public Entity SetPreferredBillingRecord(UserApplicationContext uctx,long billing_record_id) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		
		Entity billing_record = GET(BILLINGRECORD_ENTITY,billing_record_id); 
		
		GUARD(guard.canUpdateBillingRecord(user,user));
		return setPreferredBillingRecord(user,billing_record);
	}
	
	public Entity setPreferredBillingRecord(Entity user,Entity billing_record) throws WebApplicationException,PersistenceException
	{
		Entity existing_preferred_billing_record = getPreferredBillingRecord(user);
		if(existing_preferred_billing_record != null)
			UPDATE(existing_preferred_billing_record,BILLINGRECORD_FIELD_PREFERRED,false);
		
		return UPDATE(billing_record,
					  BILLINGRECORD_FIELD_PREFERRED,true);
	}
	
	public IBillingGateway getBillingGateway()
	{
		return billing_gateway;
	}
	/////////////////E N D  M O D U L E   F U N C T I O N S/////////////////////////////////////////
		
	public static String BILLINGRECORD_ENTITY = "BillingRecord";
	public static String BILLINGRECORD_FIELD_FIRST_NAME = "first_name";
	public static String BILLINGRECORD_FIELD_MIDDLE_INITIAL = "middle_initial";
	public static String BILLINGRECORD_FIELD_LAST_NAME = "last_name";
	public static String BILLINGRECORD_FIELD_COMPANY   = "company";
	public static String BILLINGRECORD_FIELD_ADDRESS_LINE_1 = "address_line_1";
	public static String BILLINGRECORD_FIELD_ADDRESS_LINE_2 = "address_line_2";
	public static String BILLINGRECORD_FIELD_CITY = "city";
	public static String BILLINGRECORD_FIELD_STATE = "state";
	public static String BILLINGRECORD_FIELD_COUNTRY = "country";
	public static String BILLINGRECORD_FIELD_POSTAL_CODE = "postal_code";
	public static String BILLINGRECORD_FIELD_CC_TYPE = "cc_type";
	public static String BILLINGRECORD_FIELD_CC_NO = "cc_no";
	public static String BILLINGRECORD_FIELD_LAST_FOUR_DIGITS = "last_four_digits";
	public static String BILLINGRECORD_FIELD_EXP_MONTH = "exp_month";
	public static String BILLINGRECORD_FIELD_EXP_YEAR = "exp_year";
	public static String BILLINGRECORD_FIELD_PREFERRED = "preferred";

	

	protected void defineEntities(Map<String,Object> config) throws PersistenceException,SyncException
	{
		DEFINE_ENTITY(BILLINGRECORD_ENTITY,
			BILLINGRECORD_FIELD_FIRST_NAME,Types.TYPE_STRING,null,
			BILLINGRECORD_FIELD_MIDDLE_INITIAL,Types.TYPE_STRING,null,
			BILLINGRECORD_FIELD_LAST_NAME,Types.TYPE_STRING,null,
			BILLINGRECORD_FIELD_COMPANY,Types.TYPE_STRING,null,
			BILLINGRECORD_FIELD_ADDRESS_LINE_1,Types.TYPE_STRING,null,
			BILLINGRECORD_FIELD_ADDRESS_LINE_2,Types.TYPE_STRING,null,
			BILLINGRECORD_FIELD_CITY,Types.TYPE_STRING,null,
			BILLINGRECORD_FIELD_STATE,Types.TYPE_STRING,null,
			BILLINGRECORD_FIELD_COUNTRY,Types.TYPE_STRING,null,
			BILLINGRECORD_FIELD_POSTAL_CODE,Types.TYPE_STRING,null,
			BILLINGRECORD_FIELD_CC_TYPE,Types.TYPE_INT,null,
			BILLINGRECORD_FIELD_CC_NO,Types.TYPE_STRING,null,
			BILLINGRECORD_FIELD_LAST_FOUR_DIGITS,Types.TYPE_STRING,null,
			BILLINGRECORD_FIELD_EXP_MONTH,Types.TYPE_INT,null,
			BILLINGRECORD_FIELD_EXP_YEAR,Types.TYPE_INT,null,
			BILLINGRECORD_FIELD_PREFERRED,Types.TYPE_BOOLEAN,false);

	}

	public static final String IDX_BY_USER_BY_PREFERRED = "byUserByPreferred";
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,SyncException
	{
		DEFINE_ENTITY_INDEX(BILLINGRECORD_ENTITY,IDX_BY_USER_BY_PREFERRED , EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, FIELD_CREATOR,BILLINGRECORD_FIELD_PREFERRED);
	}
}
