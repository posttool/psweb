package com.pagesociety.web.module.ecommerce.billing;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jws.soap.SOAPBinding.Use;

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
import com.pagesociety.web.module.TransactionProtect;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.ecommerce.gateway.BillingGatewayException;
import com.pagesociety.web.module.ecommerce.gateway.IBillingGateway;
import com.pagesociety.web.module.encryption.EncryptionModule;
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
	
	public IEncryptionModule getEncryptionModule()
	{
		return encryption_module;
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
		if(ccvn != null && ccvn.trim().equals(""))
			ccvn = null;
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
        Matcher matcher = Pattern.compile("[\\s\\.-]").matcher(cc_no);
        cc_no = matcher.replaceAll("");

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
		Calendar now = Calendar.getInstance();
		int now_year = now.get(Calendar.YEAR);
		if(year < now_year)
			throw new WebApplicationException("PLEASE PROVIDE A VALID FOUR DIGIT YEAR. e.g."+now_year);
		return year;
	}

	@Export
	@TransactionProtect
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
	@TransactionProtect/*leave cc number null to set it to exisitng cc no */
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
		if(!isConfigured())
			throw new WebApplicationException(getName()+" IS NOT CONFIGURED");
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
		if(cc_no == null)
			cc_no = encryption_module.decryptString((String)billing_record.getAttribute(BILLINGRECORD_FIELD_CC_NO));
		if(ccvn != null && ccvn.trim().equals(""))
			ccvn = null;
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
	public Entity DeleteBillingRecord(UserApplicationContext uctx,
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
		PagingQueryResult result = PAGING_QUERY_FILL_AND_MASK(q,BILLINGRECORD_FIELD_CC_NO);		
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
		if(billing_record != null)
			billing_record.setAttribute(BILLINGRECORD_FIELD_CC_NO, null);
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
		

		Entity billing_record = result.getEntities().get(0);		 
		return billing_record;
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
	
	

	
	private List<String> countries_list;
	private Map<String,String> country_to_code_map;
	private Map<String,String> code_to_country_map;
	
	@Export
	public List<String> GetCountries(UserApplicationContext uctx)
	{
		return getCountries();
	}
	
	public List<String> getCountries()
	{
		return countries_list;
	}
	
	
	@Export
	public String GetCountryCode(UserApplicationContext uctx,String country) throws WebApplicationException
	{
		return getCountryCode(country);
	}
	
	public String getCountryCode(String country) throws WebApplicationException
	{
		String code =  country_to_code_map.get(country);
		if(code == null)
			throw new WebApplicationException("BAD ISO 3166-1 COUNTRY NAME");
		return code;
	}
	
	@Export
	public String GetCountry(UserApplicationContext uctx,String code) throws WebApplicationException
	{
		return getCountry(code);
	}
	
	public String getCountry(String code) throws WebApplicationException
	{
		String country = code_to_country_map.get(code);	
		if(country == null)
			throw new WebApplicationException("BAD ISO 3166-1 COUNTRY CODE");
		return country;
	}
	
	public void build_iso_3166_country_code_data()
	{
	String[] COUNTRY_CODES_LIST = new String[]{
						"004", "Afghanistan",
						"008", "Albania",
						"010", "Antarctica",
						"012", "Algeria",
						"016", "American Samoa",
						"020", "Andorra",
						"024", "Angola",
						"028", "Antigua and Barbuda",
						"031", "Azerbaijan",
						"032", "Argentina",
						"036", "Australia",
						"040", "Austria",
						"044", "Bahamas",
						"048", "Bahrdi",
						"112", "Belain",
						"050", "Bangladesh",
						"051", "Armenia",
						"052", "Barbados",
						"056", "Belgium",
						"060", "Bermuda",
						"064", "Bhutan",
						"068", "Bolivia, Plurinational State of",
						"070", "Bosnia and Herzegovina",
						"072", "Botswana",
						"074", "Bouvet Island",
						"076", "Brazil",
						"084", "Belize",
						"086", "British Indian Ocean Territory",
						"090", "Solomon Islands",
						"092", "Virgin Islands, British",
						"096", "Brunei Darussalam",
						"100", "Bulgaria",
						"104", "Myanmar",
						"108", "Burunarus",
						"116", "Cambodia",
						"120", "Cameroon",
						"124", "Canada",
						"132", "Cape Verde",
						"136", "Cayman Islands",
						"140", "Central African Republic",
						"144", "Sri Lanka",
						"148", "Chad",
						"152", "Chile",
						"156", "China",
						"158", "Taiwan, Province of China",
						"162", "Christmas Island",
						"166", "Cocos (Keeling) Islands",
						"170", "Colombia",
						"174", "Comoros",
						"175", "Mayotte",
						"178", "Congo",
						"180", "Congo, the Democratic Republic of the",
						"184", "Cook Islands",
						"188", "Costa Rica",
						"191", "Croatia",
						"192", "Cuba",
						"196", "Cyprus",
						"203", "Czech Republic",
						"204", "Benin",
						"208", "Denmark",
						"212", "Dominica",
						"214", "Dominican Republic",
						"218", "Ecuador",
						"222", "El Salvador",
						"226", "Equatorial Guinea",
						"231", "Ethiopia",
						"232", "Eritrea",
						"233", "Estonia",
						"234", "Faroe Islands",
						"238", "Falkland Islands (Malvinas)",
						"239", "South Georgia and the South Sandwich Islands",
						"242", "Fiji",
						"246", "Finland",
						"248", "Åland Islands",
						"250", "France",
						"254", "French Guiana",
						"258", "French Polynesia",
						"260", "French Southern Territories",
						"262", "Djibouti",
						"266", "Gabon",
						"268", "Georgia",
						"270", "Gambia",
						"275", "Palestinian Territory, Occupied",
						"276", "Germany",
						"288", "Ghana",
						"292", "Gibraltar",
						"296", "Kiribati",
						"300", "Greece",
						"304", "Greenland",
						"308", "Grenada",
						"312", "Guadeloupe",
						"316", "Guam",
						"320", "Guatemala",
						"324", "Guinea",
						"328", "Guyana",
						"332", "Haiti",
						"334", "Heard Island and McDonald Islands",
						"336", "Holy See (Vatican City State)",
						"340", "Honduras",
						"344", "Hong Kong",
						"348", "Hungary",
						"352", "Iceland",
						"356", "India",
						"360", "Indonesia",
						"364", "Iran, Islamic Republic of",
						"368", "Iraq",
						"372", "Ireland",
						"376", "Israel",
						"380", "Italy",
						"384", "Côte d'Ivoire",
						"388", "Jamaica",
						"392", "Japan",
						"398", "Kazakhstan",
						"400", "Jordan",
						"404", "Kenya",
						"408", "Korea, Democratic People's Republic of",
						"410", "Korea, Republic of",
						"414", "Kuwait",
						"417", "Kyrgyzstan",
						"418", "Lao People's Democratic Republic",
						"422", "Lebanon",
						"426", "Lesotho",
						"428", "Latvia",
						"430", "Liberia",
						"434", "Libyan Arab Jamahiriya",
						"438", "Liechtenstein",
						"440", "Lithuania",
						"442", "Luxembourg",
						"446", "Macao",
						"450", "Madagascar",
						"454", "Malawi",
						"458", "Malaysia",
						"462", "Maldives",
						"466", "Mali",
						"470", "Malta",
						"474", "Martinique",
						"478", "Mauritania",
						"480", "Mauritius",
						"484", "Mexico",
						"492", "Monaco",
						"496", "Mongolia",
						"498", "Moldova, Republic of",
						"499", "Montenegro",
						"500", "Montserrat",
						"504", "Morocco",
						"508", "Mozambique",
						"512", "Oman",
						"516", "Namibia",
						"520", "Nauru",
						"524", "Nepal",
						"528", "Netherlands",
						"530", "Netherlands Antilles",
						"533", "Aruba",
						"540", "New Caledonia",
						"548", "Vanuatu",
						"554", "New Zealand",
						"558", "Nicaragua",
						"562", "Niger",
						"566", "Nigeria",
						"570", "Niue",
						"574", "Norfolk Island",
						"578", "Norway",
						"580", "Northern Mariana Islands",
						"581", "United States Minor Outlying Islands",
						"583", "Micronesia, Federated States of",
						"584", "Marshall Islands",
						"585", "Palau",
						"586", "Pakistan",
						"591", "Panama",
						"598", "Papua New Guinea",
						"600", "Paraguay",
						"604", "Peru",
						"608", "Philippines",
						"612", "Pitcairn",
						"616", "Poland",
						"620", "Portugal",
						"624", "Guinea-Bissau",
						"626", "Timor-Leste",
						"630", "Puerto Rico",
						"634", "Qatar",
						"638", "Réunion",
						"642", "Romania",
						"643", "Russian Federation",
						"646", "Rwanda",
						"652", "Saint Barthélemy",
						"654", "Saint Helena, Ascension and Tristan da Cunha",
						"659", "Saint Kitts and Nevis",
						"660", "Anguilla",
						"662", "Saint Lucia",
						"663", "Saint Martin (French part)",
						"666", "Saint Pierre and Miquelon",
						"670", "Saint Vincent and the Grenadines",
						"674", "San Marino",
						"678", "Sao Tome and Principe",
						"682", "Saudi Arabia",
						"686", "Senegal",
						"688", "Serbia",
						"690", "Seychelles",
						"694", "Sierra Leone",
						"702", "Singapore",
						"703", "Slovakia",
						"704", "Viet Nam",
						"705", "Slovenia",
						"706", "Somalia",
						"710", "South Africa",
						"716", "Zimbabwe",
						"724", "Spain",
						"732", "Western Sahara",
						"736", "Sudan",
						"740", "Suriname",
						"744", "Svalbard and Jan Mayen",
						"748", "Swaziland",
						"752", "Sweden",
						"756", "Switzerland",
						"760", "Syrian Arab Republic",
						"762", "Tajikistan",
						"764", "Thailand",
						"768", "Togo",
						"772", "Tokelau",
						"776", "Tonga",
						"780", "Trinidad and Tobago",
						"784", "United Arab Emirates",
						"788", "Tunisia",
						"792", "Turkey",
						"795", "Turkmenistan",
						"796", "Turks and Caicos Islands",
						"798", "Tuvalu",
						"800", "Uganda",
						"804", "Ukraine",
						"807", "Macedonia, the former Yugoslav Republic of",
						"818", "Egypt",
						"826", "United Kingdom",
						"831", "Guernsey",
						"832", "Jersey",
						"833", "Isle of Man",
						"834", "Tanzania, United Republic of",
						"840", "United States",
						"850", "Virgin Islands, U.S.",
						"854", "Burkina Faso",
						"858", "Uruguay",
						"860", "Uzbekistan",
						"862", "Venezuela, Bolivarian Republic of",
						"876", "Wallis and Futuna",
						"882", "Samoa",
						"887", "Yemen",
						"894", "Zambia"};
	
		countries_list = new ArrayList<String>();
		country_to_code_map = new HashMap<String,String>();
		code_to_country_map = new HashMap<String,String>();
		for(int i = 1;i < COUNTRY_CODES_LIST.length;i+=2)
		{
			String code 	= COUNTRY_CODES_LIST[i-1];
			String country 	= COUNTRY_CODES_LIST[i];
			countries_list.add(country);
			country_to_code_map.put(country,code);
			code_to_country_map.put(code,country);
		}
		
		
	}

}
