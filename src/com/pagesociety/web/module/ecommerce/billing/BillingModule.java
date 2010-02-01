package com.pagesociety.web.module.ecommerce.billing;

import java.util.List;
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
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.ecommerce.gateway.BillingGatewayException;
import com.pagesociety.web.module.ecommerce.gateway.IBillingGateway;
import com.pagesociety.web.module.encryption.IEncryptionModule;


public class BillingModule extends WebStoreModule 
{
	//TODO: i think we need to be able to delete billing records as well..perhaps
	//ones that are not preferred...evaluate what things would point to them. i dont
	//think any really do. they are not stored with the order//
	private static final String SLOT_ENCRYPTION_MODULE  	 = "encryption-module"; 
	private static final String SLOT_BILLING_GATEWAY_MODULE  = "billing-gateway"; 


	IBillingGateway 	billing_gateway;
	IEncryptionModule   encryption_module;
	
	public static final int CC_TYPE_VISA 	   = 0x01;
	public static final int CC_TYPE_MASTERCARD = 0x02;
	public static final int CC_TYPE_AMEX 	   = 0x03;
	public static final int CC_TYPE_DISCOVER   = 0x04;
	public static final int CC_TYPE_DINERS	   = 0x05; 
	
	public static final int EVENT_BILLING_RECORD_CREATED = 0x1001;
	public static final int EVENT_BILLING_RECORD_UPDATED = 0x1002;
	public static final int EVENT_BILLING_RECORD_DELETED = 0x1003;
	public static final String BILLING_EVENT_BILLING_RECORD = "billling_record";
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);
		billing_gateway 	= (IBillingGateway)getSlot(SLOT_BILLING_GATEWAY_MODULE);
		encryption_module 	= (IEncryptionModule)getSlot(SLOT_ENCRYPTION_MODULE);
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_BILLING_GATEWAY_MODULE,IBillingGateway.class,true);
		DEFINE_SLOT(SLOT_ENCRYPTION_MODULE,IEncryptionModule.class,true);
	
	}
	
	public static final String CAN_CREATE_BILLING_RECORD   		   = "CAN_CREATE_BILLING_RECORD";
//	public static final String CAN_READ_BILLING_RECORD     		   = "CAN_READ_BILLING_RECORD";
	public static final String CAN_UPDATE_BILLING_RECORD   		   = "CAN_UPDATE_BILLING_RECORD";
	public static final String CAN_DELETE_BILLING_RECORD   		   = "CAN_DELETE_BILLING_RECORD";
	public static final String CAN_BROWSE_BILLING_RECORDS  		   = "CAN_BROWSE_BILLING_RECORDS";
	public static final String CAN_BROWSE_BILLING_RECORDS_BY_USER  = "CAN_BROWSE_BILLING_RECORDS_BY_USER";
	public static final String CAN_SET_PREFERRED_BILLING_RECORD    = "CAN_SET_PREFERRED_BILLING_RECORD";

	protected void exportPermissions()
	{
		EXPORT_PERMISSION(CAN_CREATE_BILLING_RECORD);
//		EXPORT_PERMISSION(CAN_READ_BILLING_RECORD);
		EXPORT_PERMISSION(CAN_UPDATE_BILLING_RECORD);
		EXPORT_PERMISSION(CAN_DELETE_BILLING_RECORD);
		EXPORT_PERMISSION(CAN_BROWSE_BILLING_RECORDS);
		EXPORT_PERMISSION(CAN_BROWSE_BILLING_RECORDS_BY_USER);
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
				   (Integer)billing_record.getAttribute(BILLINGRECORD_FIELD_EXP_YEAR),
				   (String)billing_record.getAttribute("ccvn"),
				   (Boolean)billing_record.getAttribute(BILLINGRECORD_FIELD_PREFERRED)
				   );
	
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
			  int exp_year,
			  String ccvn,
			  Boolean preferred) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(user, CAN_CREATE_BILLING_RECORD,GUARD_TYPE,BILLINGRECORD_ENTITY,
				 								BILLINGRECORD_FIELD_FIRST_NAME, first_name,
				 								BILLINGRECORD_FIELD_MIDDLE_INITIAL, middle_initial,
				 								BILLINGRECORD_FIELD_LAST_NAME, last_name,
				 								BILLINGRECORD_FIELD_ADDRESS_LINE_1, add_1,
				 								BILLINGRECORD_FIELD_ADDRESS_LINE_2, add_2,
				 								BILLINGRECORD_FIELD_CITY, city,
				 								BILLINGRECORD_FIELD_STATE, state,									  
				 								BILLINGRECORD_FIELD_COUNTRY, country,
				 								BILLINGRECORD_FIELD_POSTAL_CODE, postal_code,
				 								BILLINGRECORD_FIELD_CC_TYPE,cc_type,
				 								BILLINGRECORD_FIELD_CC_NO, cc_no,
				 								BILLINGRECORD_FIELD_EXP_MONTH, exp_month,
				 								BILLINGRECORD_FIELD_EXP_YEAR, exp_year);
		
		exp_year = validate_and_normalize_year(exp_year);
		return createBillingRecord(user,first_name,middle_initial,last_name,add_1,add_2,city,state,country,postal_code,cc_type,cc_no,exp_month,exp_year,ccvn,preferred);
	
	}
	
	public void validateBillingRecord(Entity billing_record,String ccvn) throws BillingGatewayException,WebApplicationException
	{
		   String first_name      = (String)billing_record.getAttribute(BILLINGRECORD_FIELD_FIRST_NAME);
		   String middle_initial  = (String)billing_record.getAttribute(BILLINGRECORD_FIELD_MIDDLE_INITIAL);
		   String last_name       = (String)billing_record.getAttribute(BILLINGRECORD_FIELD_LAST_NAME);
		   String add_1 		  = (String)billing_record.getAttribute(BILLINGRECORD_FIELD_ADDRESS_LINE_1);
		   String add_2 		  = (String)billing_record.getAttribute(BILLINGRECORD_FIELD_ADDRESS_LINE_2);
		   String city 			  = (String)billing_record.getAttribute(BILLINGRECORD_FIELD_CITY);
		   String state           = (String)billing_record.getAttribute(BILLINGRECORD_FIELD_STATE);
		   String country         = (String)billing_record.getAttribute(BILLINGRECORD_FIELD_COUNTRY);
		   String postal_code     = (String)billing_record.getAttribute(BILLINGRECORD_FIELD_POSTAL_CODE);
		   int cc_type            = (Integer)billing_record.getAttribute(BILLINGRECORD_FIELD_CC_TYPE);
		   String cc_no  		  = (String)billing_record.getAttribute(BILLINGRECORD_FIELD_CC_NO);
		   int exp_month          = (Integer)billing_record.getAttribute(BILLINGRECORD_FIELD_EXP_MONTH);
		   int exp_year           = (Integer)billing_record.getAttribute(BILLINGRECORD_FIELD_EXP_YEAR);
		   exp_year = 			   validate_and_normalize_year(exp_year);
		   billing_gateway.doValidate(first_name,middle_initial,last_name,add_1,add_2,city,state,country,postal_code,cc_type,cc_no,exp_month,exp_year,ccvn);	
	}
	
	public Entity createBillingRecord(Entity creator,String first_name,String middle_initial,String last_name,String add_1,String add_2,String city,String state,String country,String postal_code,int cc_type,String cc_no,int exp_month,int exp_year,String ccvn,Boolean preferred) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		if(!isConfigured())
			throw new WebApplicationException(getName()+" IS NOT CONFIGURED");
		
		billing_gateway.doValidate(first_name,middle_initial,last_name,add_1,add_2,city,state,country,postal_code,cc_type,cc_no,exp_month,exp_year,ccvn);	
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
		

		DISPATCH_EVENT(EVENT_BILLING_RECORD_CREATED,
				   	   BILLING_EVENT_BILLING_RECORD,billing_record);
		
		if(preferred != null && preferred == true)
			setPreferredBillingRecord(creator,billing_record);

 		return billing_record;
	
	}
			  
	
	private int validate_and_normalize_year(int year)  throws WebApplicationException
	{	
		if(year < 2009)
			throw new WebApplicationException("PLEASE PROVIDE A VALID FOUR DIGIT YEAR. e.g. 2011");
		return year;
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
								   (Integer)billing_record.getAttribute(BILLINGRECORD_FIELD_EXP_YEAR),
								   (String)billing_record.getAttribute("ccvn")
								   );

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
									  int exp_year,
									  String ccvn)throws WebApplicationException,PersistenceException,BillingGatewayException
	  {
		
		Entity user = (Entity)uctx.getUser();

		
		Entity billing_record = GET(BILLINGRECORD_ENTITY,billing_record_id);
		GUARD(user,CAN_UPDATE_BILLING_RECORD,GUARD_INSTANCE,billing_record,		
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
											BILLINGRECORD_FIELD_CC_NO,cc_no,
											BILLINGRECORD_FIELD_EXP_MONTH,exp_month,
											BILLINGRECORD_FIELD_EXP_YEAR,exp_year);
		exp_year 			  = validate_and_normalize_year(exp_year);
		return updateBillingRecord(billing_record,first_name,middle_initial,last_name,add_1,add_2,city,state,country,postal_code,cc_type,cc_no,exp_month,exp_year,ccvn);
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
			  						  int exp_year,
			  						  String ccvn) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		if(!isConfigured())
			throw new WebApplicationException(getName()+" IS NOT CONFIGURED");
		billing_gateway.doValidate(first_name,middle_initial,last_name,add_1,add_2,city,state,country,postal_code,cc_type,cc_no,exp_month,exp_year,ccvn);	
		String last_4_digits = cc_no.substring(cc_no.length()-4);
		
		try
		{
			START_TRANSACTION();
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
	
	
			DISPATCH_EVENT(EVENT_BILLING_RECORD_UPDATED,
			BILLING_EVENT_BILLING_RECORD,billing_record);
			COMMIT_TRANSACTION();		
			return billing_record;
		}catch(Exception e)
		{
			ROLLBACK_TRANSACTION();
			throw new WebApplicationException("BILLING RECORD UPDATED FAILED.",e);
		}

	}

	@Export
	public Entity UpdateBillingRecord(UserApplicationContext uctx,
									 	long billing_record_id)throws WebApplicationException,PersistenceException,BillingGatewayException
	  {
		Entity user = (Entity)uctx.getUser();
		Entity billing_record = GET(BILLINGRECORD_ENTITY,billing_record_id);
		GUARD(user,CAN_DELETE_BILLING_RECORD,GUARD_INSTANCE,billing_record);
		return deleteBillingRecord(billing_record);
	  }
	
	public Entity deleteBillingRecord(Entity billing_record) throws WebApplicationException,PersistenceException,BillingGatewayException
	{		
		DELETE(billing_record);
		DISPATCH_EVENT(EVENT_BILLING_RECORD_DELETED,
				   	   BILLING_EVENT_BILLING_RECORD,billing_record);
		
		return billing_record;
	}
		
	
	@Export
	public PagingQueryResult GetBillingRecords(UserApplicationContext uctx,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Query q = new Query(BILLINGRECORD_ENTITY);
		q.idx(IDX_BY_USER_BY_PREFERRED);
		q.eq(q.list(user,Query.VAL_GLOB));
		q.offset(offset);
		q.pageSize(page_size);
		GUARD(user, CAN_BROWSE_BILLING_RECORDS_BY_USER, GUARD_USER,user);
		PagingQueryResult result = PAGING_QUERY(q);		
		return result;
	}
	
	public List<Entity> getBillingRecords(Entity user) throws WebApplicationException,PersistenceException
	{	
		Query q = new Query(BILLINGRECORD_ENTITY);
		q.idx(IDX_BY_USER_BY_PREFERRED);
		q.eq(q.list(user,Query.VAL_GLOB));
		return QUERY(q).getEntities();
	}
	
	@Export
	public Entity GetPreferredBillingRecord(UserApplicationContext uctx) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity billing_record = getPreferredBillingRecord(user);
		GUARD(user, CAN_BROWSE_BILLING_RECORDS_BY_USER, GUARD_USER, user);
		return billing_record;
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
		
		GUARD(user,CAN_UPDATE_BILLING_RECORD,GUARD_INSTANCE,billing_record);
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
	
	public boolean isConfigured()
	{
		return encryption_module.isConfigured();
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

	

	protected void defineEntities(Map<String,Object> config) throws PersistenceException,InitializationException
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
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY_INDICES
		(
				BILLINGRECORD_ENTITY,
				ENTITY_INDEX(IDX_BY_USER_BY_PREFERRED , EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, FIELD_CREATOR,BILLINGRECORD_FIELD_PREFERRED)
		);
	}
}
