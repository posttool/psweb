package com.pagesociety.web.module.ecommerce.promo;

import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.email.IEmailModule;


public class CouponPromotionManagerModule extends WebStoreModule 
{
	//TODO: i think we need to be able to delete billing records as well..perhaps
	//ones that are not preferred...evaluate what things would point to them. i dont
	//think any really do. they are not stored with the order//
	private static final String SLOT_PROMOTION_MODULE  	 = "promotion-module"; 
	private static final String SLOT_EMAIL_MODULE  		 = "email-module"; 
	
	private static final String PARAM_TEMPLATE_NAME		= "email-template-name";


	PromotionModule 	promotion_module;
	IEmailModule		email_module;
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);
		promotion_module 	= (PromotionModule)getSlot(SLOT_PROMOTION_MODULE);
		email_module     	= (IEmailModule)getSlot(SLOT_EMAIL_MODULE);
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_PROMOTION_MODULE,PromotionModule.class,true);
		DEFINE_SLOT(SLOT_EMAIL_MODULE,IEmailModule.class,true);
	}
	
	/////////////////BEGIN  M O D U L E   F U N C T I O N S/////////////////////////////////////////
	public Entity createCouponPromotionCampaign(Entity creator,String title,long promotion_id,int expr_in_months,List<String> email_list) throws PersistenceException
	{
		Entity promotion = GET(PromotionModule.PROMOTION_ENTITY,promotion_id);
		Entity cp = NEW(COUPON_PROMOTION_CAMPAIGN_ENTITY,
						creator,
						COUPON_PROMOTION_CAMPAIGN_TITLE,title,
						COUPON_PROMOTION_CAMPAIGN_PROMOTION,promotion,
						COUPON_PROMOTION_CAMPAIGN_EXPIRES_NUM_MO,expr_in_months,
						COUPON_PROMOTION_CAMPAIGN_EMAIL_LIST,email_list);
		return cp; 
	}
	
	public Entity deleteCouponPromotionCampaign(long coupon_promotion_campaign_id) throws PersistenceException,WebApplicationException
	{
		Entity cp = GET(COUPON_PROMOTION_CAMPAIGN_ENTITY,coupon_promotion_campaign_id);
		return DELETE(cp);
	}
	
	public List<Entity> getCouponPromotionCampaigns() throws PersistenceException
	{
		Query q = new Query(COUPON_PROMOTION_CAMPAIGN_ENTITY);
		q.idx(Query.PRIMARY_IDX);
		q.eq(Query.VAL_GLOB);
		q.orderBy(FIELD_LAST_MODIFIED,Query.DESC);
		QueryResult results = QUERY(q);
		return results.getEntities();
	}

	public List<Entity> getExistingCouponPromotions() throws PersistenceException
	{
		Query q = new Query(PromotionModule.PROMOTION_ENTITY);
		q.idx(Query.PRIMARY_IDX);
		q.eq(Query.VAL_GLOB);
		q.orderBy(FIELD_LAST_MODIFIED,Query.DESC);
		QueryResult results = QUERY(q);
		return results.getEntities();
	}
	
	public Entity createPromotion(Entity creator,String title,String description,String program) throws PersistenceException,WebApplicationException
	{
		return promotion_module.createPromotion(creator, title, description, program);	
	}
	
	
	/////////////////E N D  M O D U L E   F U N C T I O N S/////////////////////////////////////////
		
	public static String COUPON_PROMOTION_CAMPAIGN_ENTITY 	  		= "CouponPromotionCampaign";
	public static String COUPON_PROMOTION_CAMPAIGN_TITLE  	  		= "title";
	public static String COUPON_PROMOTION_CAMPAIGN_PROMOTION  		= "promotion";
	public static String COUPON_PROMOTION_CAMPAIGN_EMAIL_LIST 		= "email-list";
	public static String COUPON_PROMOTION_CAMPAIGN_EXPIRES_NUM_MO 	= "expiration_num_months";

	

	protected void defineEntities(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY(COUPON_PROMOTION_CAMPAIGN_ENTITY,
			COUPON_PROMOTION_CAMPAIGN_TITLE,Types.TYPE_STRING,null,
			COUPON_PROMOTION_CAMPAIGN_PROMOTION,Types.TYPE_REFERENCE,PromotionModule.PROMOTION_ENTITY,null,
			COUPON_PROMOTION_CAMPAIGN_EMAIL_LIST,Types.TYPE_STRING | Types.TYPE_ARRAY,null,
			COUPON_PROMOTION_CAMPAIGN_EXPIRES_NUM_MO,Types.TYPE_INT,null);
	}

	//public static final String IDX_BY_USER_BY_PREFERRED = "byUserByPreferred";
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		//DEFINE_ENTITY_INDICES
		//(
		//		BILLINGRECORD_ENTITY,
		//		ENTITY_INDEX(IDX_BY_USER_BY_PREFERRED , EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, FIELD_CREATOR,BILLINGRECORD_FIELD_PREFERRED)
		//);
	}
}
