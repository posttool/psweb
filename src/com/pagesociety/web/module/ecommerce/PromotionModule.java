package com.pagesociety.web.module.ecommerce;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.util.RandomGUID;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.encryption.IEncryptionModule;


public class PromotionModule extends WebStoreModule 
{

	//private static final String SLOT_PROMOTION_GUARD  		 = "promotion-guard"; 
	//IPromotionGuard   	guard;

		
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);
		//guard				= (IBillingGuard)getSlot(SLOT_BILLING_GUARD);

	}

	protected void defineSlots()
	{
		super.defineSlots();
		//DEFINE_SLOT(SLOT_PROMOTION_GUARD,IPromotionGuard.class,false,DefaultPromotionGuard.class);
	
	}
	
	/////////////////BEGIN  M O D U L E   F U N C T I O N S/////////////////////////////////////////
	///THE PROMOTION
	@Export
	public Entity CreatePromotion(UserApplicationContext uctx,Entity promotion) throws WebApplicationException,PersistenceException,BillingGatewayException
	{

		VALIDATE_TYPE(PROMOTION_ENTITY, promotion);
		VALIDATE_NEW_INSTANCE(promotion);


		return CreatePromotion(uctx,
							  (String)promotion.getAttribute(PROMOTION_FIELD_TITLE),
							  (String)promotion.getAttribute(PROMOTION_FIELD_DESCRIPTION),
							  (String)promotion.getAttribute(PROMOTION_FIELD_PROGRAM));
	
	}
	

	
	@Export
	public Entity CreatePromotion(UserApplicationContext uctx,
								  String title,
							      String description,
							      String program) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		Entity user = (Entity)uctx.getUser();

		//TODO:WRITE GUARD
		//GUARD(guard.canCreateBillingRecord(user,user));


		return createPromotion(user,title,description,program);
	
	}
	

	
	public Entity createPromotion(Entity creator,String title,String description,String program) throws WebApplicationException,PersistenceException
	{
		//TODO: validate the program//
		return NEW(PROMOTION_ENTITY,
				   creator,
				   PROMOTION_FIELD_TITLE,title,
				   PROMOTION_FIELD_DESCRIPTION,description,
				   PROMOTION_FIELD_GR1,0L,
				   PROMOTION_FIELD_GR2,0L);
	}

	@Export
	public Entity UpdatePromotion(UserApplicationContext uctx,Entity promotion) throws WebApplicationException,PersistenceException,BillingGatewayException
	{

		VALIDATE_TYPE(PROMOTION_ENTITY, promotion);
		VALIDATE_EXISTING_INSTANCE(promotion);


		return UpdatePromotion(uctx,
							   promotion.getId(),
							  (String)promotion.getAttribute(PROMOTION_FIELD_TITLE),
							  (String)promotion.getAttribute(PROMOTION_FIELD_DESCRIPTION),
							  (String)promotion.getAttribute(PROMOTION_FIELD_PROGRAM));
	
	}
	
	@Export
	public Entity UpdatePromotion(UserApplicationContext uctx,
								  long promotion_id,
								  String title,
							      String description,
							      String program) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		Entity user = (Entity)uctx.getUser();
		Entity promotion = GET(PROMOTION_ENTITY,promotion_id);
		//TODO:WRITE GUARD
		//GUARD(guard.canCreateBillingRecord(user,user));
		long gr1 = (Long)promotion.getAttribute(PROMOTION_FIELD_GR1);
		long gr2 = (Long)promotion.getAttribute(PROMOTION_FIELD_GR2);

		return updatePromotion(promotion,title,description,program,gr1,gr2);
	
	}
	

	public Entity updatePromotion(Entity promotion,String title,String description,String program,long gr1,long gr2) throws WebApplicationException,PersistenceException
	{
		//TODO: validate the program//
		return NEW(PROMOTION_ENTITY,
				   promotion,
				   PROMOTION_FIELD_TITLE,title,
				   PROMOTION_FIELD_DESCRIPTION,description);
	}

	@Export
	public Entity SetPromotionProgramRegisters(UserApplicationContext uctx,
								  				long promotion_id,
								  				long gr1,
								  				long gr2) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		Entity user = (Entity)uctx.getUser();
		Entity promotion = GET(PROMOTION_ENTITY,promotion_id);
		//TODO:WRITE GUARD
		//GUARD(guard.canCreateBillingRecord(user,user));
		
		return UPDATE(promotion,
					PROMOTION_FIELD_GR1,gr1,
					PROMOTION_FIELD_GR2,gr2);
	
	}
	
	//BE CAREFUL CALLING THIS. LOTS OF THINGS REFERENCE THIS//
	@Export
	public Entity DeletePromotion(UserApplicationContext uctx,long promotion_id) throws WebApplicationException,PersistenceException
	{
		Entity promotion = GET(PROMOTION_ENTITY,promotion_id);
		
		//TODO: guard
		return deletePromotion(promotion);	
	}
	
	public Entity deletePromotion(Entity promotion) throws PersistenceException
	{
		return DELETE(promotion);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	//USER PROMOTION
	@Export
	public Entity CreateUserPromotion(UserApplicationContext uctx,Entity user_promotion) throws WebApplicationException,PersistenceException
	{

		VALIDATE_TYPE(USER_PROMOTION_INSTANCE_ENTITY, user_promotion);
		VALIDATE_NEW_INSTANCE(user_promotion);

		Entity promotion = (Entity)user_promotion.getAttribute(USER_PROMOTION_INSTANCE_FIELD_PROMOTION);
		if(promotion == null)
			throw new WebApplicationException("MUST PROVIDE PROMOTION TO CREATE PROMOTION INSTANCE");
		VALIDATE_TYPE(USER_PROMOTION_INSTANCE_ENTITY, promotion);
		promotion = GET(PROMOTION_ENTITY,promotion.getId());
		
		String promotion_code = (String)user_promotion.getAttribute(USER_PROMOTION_INSTANCE_FIELD_PROMO_CODE);
		return CreateUserPromotion(uctx,
									promotion_code,
									promotion,
								   (Date)user_promotion.getAttribute(USER_PROMOTION_INSTANCE_FIELD_EXPIRATION_DATE));
	
	}
	
	@Export//if promotionCode is null one will be generated for you. 
	public Entity CreateUserPromotion(UserApplicationContext uctx,
									   	String promotion_code,
										Entity promotion,
									   	Date expiration_date) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();

		//TODO:WRITE GUARD
		//GUARD(guard.canCreateBillingRecord(user,user));
		
		return createUserPromotionInstance(user,promotion_code,promotion,expiration_date);
	
	}
	
	//pass null expiration date for never expiring//
	public Entity createUserPromotionInstance(Entity creator,String promotion_code,Entity promotion,Date expiration_date) throws WebApplicationException,PersistenceException
	{
		if(promotion_code == null)
			promotion_code = RandomGUID.getGUID();
		else
			;//TODO: make sure promotion code is unique
		
		return NEW(USER_PROMOTION_INSTANCE_ENTITY,
				   creator,
				   USER_PROMOTION_INSTANCE_FIELD_PROMO_CODE,promotion_code,
				   USER_PROMOTION_INSTANCE_FIELD_PROMOTION,promotion,
				   USER_PROMOTION_INSTANCE_FIELD_EXPIRATION_DATE,expiration_date,
				   USER_PROMOTION_INSTANCE_FIELD_IR1,0L,
				   USER_PROMOTION_INSTANCE_FIELD_IR2,0L,
				   USER_PROMOTION_INSTANCE_FIELD_IR3,0L,	   
				   USER_PROMOTION_INSTANCE_FIELD_IR4,0L,
				   USER_PROMOTION_INSTANCE_FIELD_SR1,null,
				   USER_PROMOTION_INSTANCE_FIELD_SR2,null,
				   USER_PROMOTION_INSTANCE_FIELD_FPR1,0.0,
				   USER_PROMOTION_INSTANCE_FIELD_FPR2,0.0);
	}		  
	
	@Export
	public Entity SetUserPromotionExpirationDate(UserApplicationContext uctx,long user_promotion_id,Date expiration_date) throws WebApplicationException,PersistenceException
	{		
		Entity user_promotion = GET(USER_PROMOTION_INSTANCE_ENTITY,user_promotion_id);
		//TODO: guard
		return setGlobalPromotionState(user_promotion,GLOBAL_PROMOTION_STATE_ACTIVE);

	}

	public Entity setUserPromotionExpirationDate(Entity user_promotion,Date expiration_date) throws PersistenceException
	{
		return UPDATE(user_promotion, USER_PROMOTION_INSTANCE_FIELD_EXPIRATION_DATE, expiration_date);
	}
	

	public static int ERROR_EXPIRED_PROMO_CODE = 0x01;
	@Export
	public Entity GetUserPromotionByPromoCode(UserApplicationContext uctx,String promo_code) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		if(promo_code == null)
			throw new WebApplicationException("NEED TO PROVIDE NOT NULL PROMO CODE");
		
		//TODO: guard...Probably no guard.
		//GUARD(guard.canGetBillingRecords(user,user));		
		
		Query q = getUserPromotionByPromoCodeQ(promo_code);
		QueryResult result = QUERY(q);
		if(result.size() == 0)
			return null;
		if(result.size() > 1)
			LOG("MULTIPLE PROMOTIONS ["+result.getEntities()+"] FOR SINGLE PROMO CODE "+promo_code+" DATA INTEGRITY ISSUE.");
		
		Entity user_promo_instance = result.getEntities().get(0);
		Date expr_date = (Date)user_promo_instance.getAttribute(USER_PROMOTION_INSTANCE_FIELD_EXPIRATION_DATE);
		Date now = new Date();
		if(now.getTime() > expr_date.getTime())
			throw new WebApplicationException("EXPIRED PROMOCODE "+promo_code,ERROR_EXPIRED_PROMO_CODE);
		
		return user_promo_instance;
	}
	
	
	public Query getUserPromotionByPromoCodeQ(String promo_code)
	{
		Query q = new Query(USER_PROMOTION_INSTANCE_ENTITY);
		q.idx(IDX_USER_PROMOTION_BY_PROMO_CODE);
		if(promo_code == null)
			q.eq(Query.VAL_GLOB);
		else
			q.eq(promo_code);
		return q;
	}
	
	//////////////////////////////////////////////////////////////////////////////////
	//GLOBAL PROMOTION
	@Export
	public Entity CreateGlobalPromotion(UserApplicationContext uctx,Entity global_promotion) throws WebApplicationException,PersistenceException
	{

		VALIDATE_TYPE(GLOBAL_PROMOTION_INSTANCE_ENTITY, global_promotion);
		VALIDATE_NEW_INSTANCE(global_promotion);

		Entity promotion = (Entity)global_promotion.getAttribute(GLOBAL_PROMOTION_INSTANCE_FIELD_PROMOTION);
		if(promotion == null)
			throw new WebApplicationException("MUST PROVIDE PROMOTION TO CREATE PROMOTION INSTANCE");
		VALIDATE_TYPE(PROMOTION_ENTITY, promotion);
		promotion = GET(PROMOTION_ENTITY,promotion.getId());
		
		return CreateGlobalPromotion(uctx,
								     promotion,
								    (Integer)global_promotion.getAttribute(GLOBAL_PROMOTION_INSTANCE_FIELD_ACTIVE));
	
	}
	
	@Export
	public Entity CreateGlobalPromotion(UserApplicationContext uctx,
									   	Entity promotion,
									   	int active) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();

		//TODO:WRITE GUARD
		//GUARD(guard.canCreateBillingRecord(user,user));
		
		return createGlobalPromotionInstance(user,promotion,active);
	
	}
	
	
	public Entity createGlobalPromotionInstance(Entity creator,Entity promotion,int active) throws WebApplicationException,PersistenceException
	{
		return NEW(GLOBAL_PROMOTION_INSTANCE_ENTITY,
				   creator,
				   GLOBAL_PROMOTION_INSTANCE_FIELD_PROMOTION,promotion,
				   GLOBAL_PROMOTION_INSTANCE_FIELD_ACTIVE,active);
	}		  
	
	@Export
	public Entity ActivateGlobalPromotion(UserApplicationContext uctx,long global_promotion_id) throws WebApplicationException,PersistenceException
	{		
		Entity global_promotion = GET(GLOBAL_PROMOTION_INSTANCE_ENTITY,global_promotion_id);
		//TODO: guard
		return setGlobalPromotionState(global_promotion,GLOBAL_PROMOTION_STATE_ACTIVE);

	}

	@Export
	public Entity DectivateGlobalPromotion(UserApplicationContext uctx,long global_promotion_id) throws WebApplicationException,PersistenceException
	{		
		Entity global_promotion = GET(GLOBAL_PROMOTION_INSTANCE_ENTITY,global_promotion_id);	
		//TODO: guard
		return setGlobalPromotionState(global_promotion,GLOBAL_PROMOTION_STATE_INACTIVE);

	}

	public Entity setGlobalPromotionState(Entity global_promotion,int state) throws PersistenceException
	{
		return UPDATE(global_promotion, GLOBAL_PROMOTION_INSTANCE_FIELD_ACTIVE, state);
	}
	
	
	@Export
	public Entity DeleteGlobalPromotion(UserApplicationContext uctx,long global_promotion_id) throws WebApplicationException,PersistenceException
	{
		Entity global_promotion = GET(GLOBAL_PROMOTION_INSTANCE_ENTITY,global_promotion_id);
		
		//TODO: guard
		return deleteGlobalPromotion(global_promotion);	
	}
	
	public Entity deleteGlobalPromotion(Entity global_promotion) throws PersistenceException
	{
		return DELETE(global_promotion);
	}
	
	
	@Export
	public PagingQueryResult GetGlobalPromotions(UserApplicationContext uctx,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		//TODO: guard
		//GUARD(guard.canGetBillingRecords(user,user));		
		Query q = getGlobalPromotionsByStateQ(null);
		q.offset(offset);
		q.pageSize(page_size);
		q.orderBy(FIELD_DATE_CREATED,Query.DESC);
		return PAGING_QUERY(q);
	}
	
	public List<Entity> getActiveGlobalPromotions() throws PersistenceException
	{
		Query q = getGlobalPromotionsByStateQ(GLOBAL_PROMOTION_STATE_ACTIVE);
		QueryResult result = QUERY(q);
		return result.getEntities();
	}
	
	public Query getGlobalPromotionsByStateQ(Integer state)
	{
		Query q = new Query(GLOBAL_PROMOTION_INSTANCE_ENTITY);
		q.idx(IDX_GLOBAL_PROMOTION_BY_ACTIVE);
		if(state == null)
			q.eq(Query.VAL_GLOB);
		else
			q.eq(state);
		return q;
	}
	
	////////////////////////////////////////
	
	/////////////////E N D  M O D U L E   F U N C T I O N S/////////////////////////////////////////
		
	public static String PROMOTION_ENTITY 							= "Promotion";
	public static String PROMOTION_FIELD_TITLE	     				= "title";
	public static String PROMOTION_FIELD_DESCRIPTION 				= "description";
	public static String PROMOTION_FIELD_PROGRAM   					= "program";
	public static String PROMOTION_FIELD_GR1   						= "gr1";
	public static String PROMOTION_FIELD_GR2   						= "gr2";
	
	public static String GLOBAL_PROMOTION_INSTANCE_ENTITY 			= "GlobalPromotion";
	public static String GLOBAL_PROMOTION_INSTANCE_FIELD_PROMOTION 	= "promotion";
	public static String GLOBAL_PROMOTION_INSTANCE_FIELD_ACTIVE   	= "active";
	
	public static String USER_PROMOTION_INSTANCE_ENTITY 				= "UserPromotion";
	public static String USER_PROMOTION_INSTANCE_FIELD_PROMO_CODE 		= "code";
	public static String USER_PROMOTION_INSTANCE_FIELD_PROMOTION 		= "promotion";
	public static String USER_PROMOTION_INSTANCE_FIELD_EXPIRATION_DATE 	= "expiration_date";
	public static String USER_PROMOTION_INSTANCE_FIELD_IR1 				= "ir1";
	public static String USER_PROMOTION_INSTANCE_FIELD_IR2 				= "ir2";
	public static String USER_PROMOTION_INSTANCE_FIELD_IR3 				= "ir3";
	public static String USER_PROMOTION_INSTANCE_FIELD_IR4 				= "ir4";
	public static String USER_PROMOTION_INSTANCE_FIELD_SR1 				= "sr1";
	public static String USER_PROMOTION_INSTANCE_FIELD_SR2 			= "sr2";
	public static String USER_PROMOTION_INSTANCE_FIELD_FPR1 			= "fpr1";
	public static String USER_PROMOTION_INSTANCE_FIELD_FPR2 			= "fpr2";

	private static final int GLOBAL_PROMOTION_STATE_INACTIVE 	= 0x01;
	private static final int GLOBAL_PROMOTION_STATE_ACTIVE 		= 0x02;
	protected void defineEntities(Map<String,Object> config) throws PersistenceException,SyncException
	{
		DEFINE_ENTITY(PROMOTION_ENTITY,
					  PROMOTION_FIELD_TITLE,Types.TYPE_STRING,"",
					  PROMOTION_FIELD_DESCRIPTION,Types.TYPE_STRING,"",
					  PROMOTION_FIELD_PROGRAM,Types.TYPE_STRING,null,
					  PROMOTION_FIELD_GR1,Types.TYPE_LONG,0L,
					  PROMOTION_FIELD_GR2,Types.TYPE_LONG,0L);
	
		DEFINE_ENTITY(GLOBAL_PROMOTION_INSTANCE_ENTITY,
				  	  GLOBAL_PROMOTION_INSTANCE_FIELD_PROMOTION,Types.TYPE_REFERENCE,PROMOTION_ENTITY,null,
				  	  GLOBAL_PROMOTION_INSTANCE_FIELD_ACTIVE,Types.TYPE_INT,GLOBAL_PROMOTION_STATE_INACTIVE);
		
		DEFINE_ENTITY(USER_PROMOTION_INSTANCE_ENTITY,
					  USER_PROMOTION_INSTANCE_FIELD_PROMO_CODE,Types.TYPE_STRING,null,
					  USER_PROMOTION_INSTANCE_FIELD_PROMOTION,Types.TYPE_REFERENCE,PROMOTION_ENTITY,null,
					  USER_PROMOTION_INSTANCE_FIELD_EXPIRATION_DATE,Types.TYPE_DATE,null,
					  USER_PROMOTION_INSTANCE_FIELD_IR1,Types.TYPE_LONG,0L, 
					  USER_PROMOTION_INSTANCE_FIELD_IR2,Types.TYPE_LONG,0L,  			
					  USER_PROMOTION_INSTANCE_FIELD_IR3,Types.TYPE_LONG,0L, 			
					  USER_PROMOTION_INSTANCE_FIELD_IR4,Types.TYPE_LONG,0L, 			
					  USER_PROMOTION_INSTANCE_FIELD_SR1,Types.TYPE_STRING,null, 			
					  USER_PROMOTION_INSTANCE_FIELD_SR2,Types.TYPE_STRING,null, 		
					  USER_PROMOTION_INSTANCE_FIELD_FPR1,Types.TYPE_DOUBLE,0.0, 		
					  USER_PROMOTION_INSTANCE_FIELD_FPR2,Types.TYPE_DOUBLE,0.0);  

	}

	public static final String IDX_USER_PROMOTION_BY_PROMO_CODE   = "byPromoCode";
	public static final String IDX_GLOBAL_PROMOTION_BY_ACTIVE     = "byActive";
	
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,SyncException
	{
		DEFINE_ENTITY_INDEX(USER_PROMOTION_INSTANCE_ENTITY,IDX_USER_PROMOTION_BY_PROMO_CODE , EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, USER_PROMOTION_INSTANCE_FIELD_PROMO_CODE);
		DEFINE_ENTITY_INDEX(GLOBAL_PROMOTION_INSTANCE_ENTITY,IDX_GLOBAL_PROMOTION_BY_ACTIVE , EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, GLOBAL_PROMOTION_INSTANCE_FIELD_ACTIVE);
	}
}
