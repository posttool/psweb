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
import com.pagesociety.web.exception.PermissionsException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.PermissionsModule;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.email.IEmailModule;
import com.pagesociety.web.module.encryption.IEncryptionModule;
import com.pagesociety.web.module.user.UserModule;
import com.pagesociety.web.module.util.Util;

public class BillingModule extends WebStoreModule 
{

	private static final String SLOT_ENCRYPTION_MODULE  	 = "encryption-module"; 
	private static final String SLOT_BILLING_GATEWAY_MODULE  = "billing-gateway"; 
	private static final String SLOT_BILLING_GUARD  		 = "billing-guard"; 

	IBillingGateway 	billing_gateway;
	IBillingGuard   	guard;
	IEncryptionModule   encryption_module;
	
	public static final int CC_TYPE_VISA 	   = 0x01;
	public static final int CC_TYPE_MASTERCARD = 0x02;
	public static final int CC_TYPE_AMEX 	   = 0x03;
	public static final int CC_TYPE_DISCOVER   = 0x04;
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);
		billing_gateway 	= (IBillingGateway)getSlot(SLOT_BILLING_GATEWAY_MODULE);
		guard				= (IBillingGuard)getSlot(SLOT_BILLING_GUARD);
		encryption_module 	= (IEncryptionModule)getSlot(SLOT_ENCRYPTION_MODULE);
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_BILLING_GATEWAY_MODULE,IBillingGateway.class,true);
		DEFINE_SLOT(SLOT_ENCRYPTION_MODULE,IEncryptionModule.class,true);
		DEFINE_SLOT(SLOT_BILLING_GATEWAY_MODULE,IBillingGateway.class,false,DefaultBillingGuard.class);
	
	}
	
	/////////////////BEGIN  M O D U L E   F U N C T I O N S/////////////////////////////////////////

	@Export
	public Entity CreateBillingRecord(UserApplicationContext uctx,Entity billing_record) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(guard.canCreateBillingRecord(user,user));
		return createBillingRecord(user, billing_record, false);
	}
	

	
	@Export
	public Entity CreateBillingRecord(UserApplicationContext uctx,
									  String first_name,
									  String middle_initial,
									  String last_name,
									  String add_1,
									  String add_2,
									  String city,
									  String country,
									  String postal_code,
									  int cc_type,
									  String cc_no,
									  int exp_month,
									  int exp_year) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(guard.canCreateBillingRecord(user,user));
		
		Entity billing_record = Entity.createInstance();
		billing_record.setType(BILLINGRECORD_ENTITY);
		billing_record.setAttribute(BILLINGRECORD_FIELD_FIRST_NAME,first_name);
		billing_record.setAttribute(BILLINGRECORD_FIELD_MIDDLE_INITIAL,middle_initial);
		billing_record.setAttribute(BILLINGRECORD_FIELD_LAST_NAME,last_name);
		billing_record.setAttribute(BILLINGRECORD_FIELD_ADDRESS_LINE_1,add_1);
		billing_record.setAttribute(BILLINGRECORD_FIELD_ADDRESS_LINE_2,add_2);
		billing_record.setAttribute(BILLINGRECORD_FIELD_CITY,city);
		billing_record.setAttribute(BILLINGRECORD_FIELD_COUNTRY,country);
		billing_record.setAttribute(BILLINGRECORD_FIELD_POSTAL_CODE,postal_code);
		billing_record.setAttribute(BILLINGRECORD_FIELD_CC_TYPE,cc_type);
		billing_record.setAttribute(BILLINGRECORD_FIELD_CC_NO,cc_no);
		billing_record.setAttribute(BILLINGRECORD_FIELD_EXP_MONTH,exp_month);
		billing_record.setAttribute(BILLINGRECORD_FIELD_EXP_YEAR,exp_year);


		return createBillingRecord(user,billing_record,false);
	}
	
	public Entity createBillingRecord(Entity creator,
			 						  Entity billing_record,/* see above for how filled it must be!*/
			 						  boolean preferred) throws WebApplicationException,PersistenceException,BillingGatewayException
				
	{
		
		if(billing_record.getId() != Entity.UNDEFINED)
			throw new WebApplicationException("TRYING TO CREATE AN ALREADY INITIALIZED ENTITY. ALREADY HAS ID OF "+billing_record.getId());
		if(!billing_record.getType().equals(BILLINGRECORD_ENTITY))
			throw new WebApplicationException("TRYING TO SAVE AN ENTITY OF BILLING RECORD BUT IT IS NOT A BILLING RECORD. IT IS A "+billing_record.getType());
		
		billing_gateway.doValidate(billing_record);	
		String cc_no         = (String)billing_record.getAttribute(BILLINGRECORD_FIELD_CC_NO);
		String last_4_digits = cc_no.substring(cc_no.length()-4);
		billing_record.setAttribute(BILLINGRECORD_FIELD_LAST_FOUR_DIGITS,last_4_digits);		
		billing_record.setAttribute(BILLINGRECORD_FIELD_PREFERRED,preferred);				
		billing_record.setAttribute(BILLINGRECORD_FIELD_CC_NO,encryption_module.encryptString(cc_no));
		
		return CREATE_ENTITY(creator,billing_record);

	}
	
	@Export
	public Entity UpdateBillingRecord(UserApplicationContext uctx,Entity billing_record) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(guard.canUpdateBillingRecord(user,user));
		return updateBillingRecord(uctx,billing_record);
	}
	
	public Entity updateBillingRecord(UserApplicationContext uctx,Entity billing_record) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		if(billing_record.getId() == Entity.UNDEFINED)
			throw new WebApplicationException("TRYING TO UPDATE AN UNINITIALIZED ENTITY.MAYBE YOU MEANT TO CREATE IT INSTEAD. ID IS "+billing_record.getId());
		if(!billing_record.getType().equals(BILLINGRECORD_ENTITY))
			throw new WebApplicationException("TRYING TO SAVE AN ENTITY OF BILLING RECORD BUT IT IS NOT A BILLING RECORD. IT IS A "+billing_record.getType());
		
		billing_gateway.doValidate(billing_record);	
		
		String cc_no         = (String)billing_record.getAttribute(BILLINGRECORD_FIELD_CC_NO);
		String last_4_digits = cc_no.substring(cc_no.length()-4);
		billing_record.setAttribute(BILLINGRECORD_FIELD_LAST_FOUR_DIGITS,last_4_digits);		
		billing_record.setAttribute(BILLINGRECORD_FIELD_CC_NO,encryption_module.encryptString(cc_no));
		return SAVE_ENTITY(billing_record);
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
	
	public Entity getPreferredBillingRecord(Entity user)throws WebApplicationException,PersistenceException
	{
		Query q = new Query(BILLINGRECORD_ENTITY);
		q.idx(IDX_BY_USER_BY_PREFERRED);
		q.eq(q.list(user,true));
		
		QueryResult result = QUERY(q);
		int n = result.size();
		if(n == 0)
			return null;
		else if(n > 1)
			throw new WebApplicationException("DATA INTEGRETY ISSUE. MORE THAN ONE PREFERRED BILLING RECORD.");
		else
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
	
	/////////////////E N D  M O D U L E   F U N C T I O N S/////////////////////////////////////////
		
	public static String BILLINGRECORD_ENTITY = "BillingRecord";
	public static String BILLINGRECORD_FIELD_FIRST_NAME = "first_name";
	public static String BILLINGRECORD_FIELD_MIDDLE_INITIAL = "middle_initial";
	public static String BILLINGRECORD_FIELD_LAST_NAME = "last_name";
	public static String BILLINGRECORD_FIELD_ADDRESS_LINE_1 = "address_line_1";
	public static String BILLINGRECORD_FIELD_ADDRESS_LINE_2 = "address_line_2";
	public static String BILLINGRECORD_FIELD_CITY = "city";
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
			BILLINGRECORD_FIELD_ADDRESS_LINE_1,Types.TYPE_STRING,null,
			BILLINGRECORD_FIELD_ADDRESS_LINE_2,Types.TYPE_STRING,null,
			BILLINGRECORD_FIELD_CITY,Types.TYPE_STRING,null,
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
