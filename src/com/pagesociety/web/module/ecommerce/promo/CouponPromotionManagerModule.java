package com.pagesociety.web.module.ecommerce.promo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.util.RandomGUID;
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
	private static final String PARAM_EMAIL_SENDER		= "promo-sender-address";

	private static final String DEFAULT_TEMPLATE_NAME	= "promotion.ftl";

	PromotionModule 	promotion_module;
	IEmailModule		email_module;

	private String template_name;
	private String promo_sender_address;
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);
		promotion_module 	= (PromotionModule)getSlot(SLOT_PROMOTION_MODULE);
		email_module     	= (IEmailModule)getSlot(SLOT_EMAIL_MODULE);
		promo_sender_address = GET_REQUIRED_CONFIG_PARAM(PARAM_EMAIL_SENDER, config);
		template_name 		= GET_OPTIONAL_CONFIG_PARAM(PARAM_TEMPLATE_NAME, config);
		if(template_name == null)
			template_name = DEFAULT_TEMPLATE_NAME;
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_PROMOTION_MODULE,PromotionModule.class,true);
		DEFINE_SLOT(SLOT_EMAIL_MODULE,IEmailModule.class,true);
	}


	/////////////////BEGIN  M O D U L E   F U N C T I O N S/////////////////////////////////////////
	public Entity createCouponPromotionCampaign(Entity creator,String title,String promo_prefix,long promotion_id,long ir1,long ir2,long ir3,long ir4,String sr1,String sr2,double fpr1,double fpr2,int expr_in_days,String subject,String link,String presslink,String message,List<String> promo_list) throws PersistenceException
	{
			try{
				START_TRANSACTION(getName()+" createCouponPromotionCampaign");
				Entity promotion = GET(PromotionModule.PROMOTION_ENTITY,promotion_id);

				List<Entity> recips = new ArrayList<Entity>();
				for(int i = 0;i < promo_list.size();i++)
				{
					String name = promo_list.get(i);
					Entity recipient = NEW(COUPON_PROMOTION_CAMPAIGN_RECIPIENT_ENTITY,
								   			creator,
								   			COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_RECIPIENT,name,
								   			COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_ACTIVATED,false,
								   			COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_ACTIVATION_DATE,null,
								   			COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_PROMO_CODE,null);
					recips.add(recipient);
				}

				Entity cp = NEW(COUPON_PROMOTION_CAMPAIGN_ENTITY,
								creator,
								COUPON_PROMOTION_CAMPAIGN_TITLE,title,
								COUPON_PROMOTION_CAMPAIGN_CODE_PREFIX,promo_prefix,
								COUPON_PROMOTION_CAMPAIGN_PROMOTION,promotion,
								COUPON_PROMOTION_CAMPAIGN_EXPIRES_NUM_DAYS,expr_in_days,
								COUPON_PROMOTION_CAMPAIGN_MESSAGE_SUBJECT,subject,
								COUPON_PROMOTION_CAMPAIGN_MESSAGE_LINK,link,
								COUPON_PROMOTION_CAMPAIGN_MESSAGE_PRESSLINK,presslink,
								COUPON_PROMOTION_CAMPAIGN_MESSAGE,message,
								COUPON_PROMOTION_CAMPAIGN_PROMO_LIST,promo_list,
								COUPON_PROMOTION_CAMPAIGN_RECIPIENTS,recips,
								COUPON_PROMOTION_CAMPAIGN_IR1,ir1,
								COUPON_PROMOTION_CAMPAIGN_IR2,ir2,
								COUPON_PROMOTION_CAMPAIGN_IR3,ir3,
								COUPON_PROMOTION_CAMPAIGN_IR4,ir4,
								COUPON_PROMOTION_CAMPAIGN_SR1,sr1,
								COUPON_PROMOTION_CAMPAIGN_SR2,sr2,
								COUPON_PROMOTION_CAMPAIGN_FPR1,fpr1,
								COUPON_PROMOTION_CAMPAIGN_FPR2,fpr2);
				COMMIT_TRANSACTION();
				return cp;

			}catch(PersistenceException e)
			{
				ROLLBACK_TRANSACTION();
				throw e;
			}
	}

	public List<Entity> activateCouponPromotionCampaign(long promo_id) throws WebApplicationException,PersistenceException
	{
		Entity promotion 		= FILL_REFS(GET(COUPON_PROMOTION_CAMPAIGN_ENTITY,promo_id));
		List<Entity> recips 	= (List<Entity>)promotion.getAttribute(COUPON_PROMOTION_CAMPAIGN_RECIPIENTS);
		for(int i = 0;i < recips.size();i++)
		{
			Entity recipient 		= recips.get(i);
			boolean activated = (Boolean)recipient.getAttribute(COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_ACTIVATED);
			if(activated)
				continue;
			recipient = activate_recipient(promotion,recipient);
			recips.set(i,recipient);
			try{
				Thread.sleep(1000);
			}catch(InterruptedException ie)
			{

			}
		}
		return recips;
	}

	public Entity addRecipient(Entity creator,long promo_id,String name) throws WebApplicationException,PersistenceException
	{
		try{
			START_TRANSACTION(getName()+" Add Recipient");
			Entity promotion 		= FILL_REFS(GET(COUPON_PROMOTION_CAMPAIGN_ENTITY,promo_id));
			List<String> promo_list = (List<String>)promotion.getAttribute(COUPON_PROMOTION_CAMPAIGN_PROMO_LIST);
			List<Entity> recips 	= (List<Entity>)promotion.getAttribute(COUPON_PROMOTION_CAMPAIGN_RECIPIENTS);
			Entity recipient = NEW(COUPON_PROMOTION_CAMPAIGN_RECIPIENT_ENTITY,
		   			creator,
		   			COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_RECIPIENT,name,
		   			COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_ACTIVATED,false,
		   			COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_ACTIVATION_DATE,null,
		   			COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_PROMO_CODE,null);
			recips.add(recipient);
			promo_list.add(name);
			UPDATE(promotion,
					COUPON_PROMOTION_CAMPAIGN_RECIPIENTS,recips,
					COUPON_PROMOTION_CAMPAIGN_PROMO_LIST,promo_list);
			COMMIT_TRANSACTION();
			return recipient;
		}catch(PersistenceException e)
		{
			ROLLBACK_TRANSACTION();
			throw e;
		}
	}

	public Entity activateCouponPromotionRecipient(long promo_id,int recip_idx) throws WebApplicationException,PersistenceException
	{
		Entity promotion 		= FILL_REFS(GET(COUPON_PROMOTION_CAMPAIGN_ENTITY,promo_id));
		List<Entity> recips 	= (List<Entity>)promotion.getAttribute(COUPON_PROMOTION_CAMPAIGN_RECIPIENTS);
		Entity recipient 		= recips.get(recip_idx);
		return activate_recipient(promotion, recipient);
	}

	private Entity activate_recipient(Entity promotion,Entity recipient) throws PersistenceException,WebApplicationException
	{

		try{
			START_TRANSACTION(getName()+" activate_recipient");
			String promo_prefix 	= (String)promotion.getAttribute(COUPON_PROMOTION_CAMPAIGN_CODE_PREFIX);
			String promotion_code 	= promo_prefix==null?RandomGUID.getGUID().substring(16):promo_prefix+RandomGUID.getGUID().substring(16);
			Entity the_promotion    = (Entity)promotion.getAttribute(COUPON_PROMOTION_CAMPAIGN_PROMOTION);
			int num_days_for_promo = (Integer)promotion.getAttribute(COUPON_PROMOTION_CAMPAIGN_EXPIRES_NUM_DAYS);
			Date promo_expr;
			if(num_days_for_promo == 0)
				promo_expr = null;
			else
			{
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DATE, num_days_for_promo);
				promo_expr = cal.getTime();
			}
			Entity coupon_promo = promotion_module.createCouponPromotion(null, promotion_code, 1, the_promotion, promo_expr,
													(Long)promotion.getAttribute(COUPON_PROMOTION_CAMPAIGN_IR1),
													(Long)promotion.getAttribute(COUPON_PROMOTION_CAMPAIGN_IR2),
													(Long)promotion.getAttribute(COUPON_PROMOTION_CAMPAIGN_IR3),
													(Long)promotion.getAttribute(COUPON_PROMOTION_CAMPAIGN_IR4),
													(String)promotion.getAttribute(COUPON_PROMOTION_CAMPAIGN_SR1),
													(String)promotion.getAttribute(COUPON_PROMOTION_CAMPAIGN_SR2),
													(Double)promotion.getAttribute(COUPON_PROMOTION_CAMPAIGN_FPR1),
													(Double)promotion.getAttribute(COUPON_PROMOTION_CAMPAIGN_FPR2));

			recipient =  UPDATE(recipient,
					COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_ACTIVATED, true,
					COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_ACTIVATION_DATE, new Date(),
					COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_PROMO_CODE, promotion_code,
					COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_COUPON_PROMOTION,coupon_promo);


			COMMIT_TRANSACTION();
			sendRecipientEmail(promotion, recipient);
			return recipient;
		}catch (PersistenceException e) {
			ROLLBACK_TRANSACTION();
			throw e;
		}
	}



	public void sendRecipientEmail(Entity campaign_promo,Entity recipient) throws WebApplicationException
	{
		if(!(Boolean)recipient.getAttribute(COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_ACTIVATED))
			throw new WebApplicationException("Recipient "+recipient+" is not activated. Can't send email.");

		String recipient_name = (String)recipient.getAttribute(COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_RECIPIENT);
		if(recipient_name.indexOf('@') != -1)
		{
			String recipient_email	=	recipient_name;
			String subject 			= (String)campaign_promo.getAttribute(COUPON_PROMOTION_CAMPAIGN_MESSAGE_SUBJECT);
			String message 			= (String)campaign_promo.getAttribute(COUPON_PROMOTION_CAMPAIGN_MESSAGE);
			String name = null;
			if(recipient_name.indexOf('<') != -1)
				name 			= recipient_name.substring(0, recipient_name.indexOf('<'));
			else
				name 			= recipient_name.substring(0, recipient_name.indexOf('@'));
			String promo_code 		= (String)recipient.getAttribute(COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_PROMO_CODE);
			String link				= (String)campaign_promo.getAttribute(COUPON_PROMOTION_CAMPAIGN_MESSAGE_LINK);
			String presslink		= (String)campaign_promo.getAttribute(COUPON_PROMOTION_CAMPAIGN_MESSAGE_PRESSLINK);
			Map<String,Object> template_data = new HashMap<String,Object>();
			template_data.put("message",message);
			template_data.put("name",name);
			template_data.put("promo_code",promo_code);
			template_data.put("link",link);
			template_data.put("presslink",presslink);
			email_module.sendEmail(promo_sender_address,new String[]{recipient_email}, subject, template_name, template_data);

		}
	}

	public void resendEmail(long c_id,long r_id) throws WebApplicationException,PersistenceException
	{
		Entity campaign_promo 		= FILL_REFS(GET(COUPON_PROMOTION_CAMPAIGN_ENTITY,c_id));
		Entity recipient			= GET(COUPON_PROMOTION_CAMPAIGN_RECIPIENT_ENTITY,r_id);
		sendRecipientEmail(campaign_promo, recipient);
	}

	public Entity deleteCouponPromotionCampaign(long coupon_promotion_campaign_id) throws PersistenceException,WebApplicationException
	{
		Entity cp = GET(COUPON_PROMOTION_CAMPAIGN_ENTITY,coupon_promotion_campaign_id);
		return DELETE_DEEP(cp,new delete_policy(FIELD_CREATOR,COUPON_PROMOTION_CAMPAIGN_PROMOTION));
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

	public Entity getCouponPromotionCampaign(long id) throws PersistenceException,WebApplicationException
	{
		Entity promo_campaign =  GET(COUPON_PROMOTION_CAMPAIGN_ENTITY,id);
		FILL_DEEP_AND_MASK(promo_campaign, new String[]{}, new String[]{});
		return promo_campaign;
	}

	public List<Entity> getExistingPromotions() throws PersistenceException
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
	public static String COUPON_PROMOTION_CAMPAIGN_PROMO_LIST 		= "promo-list";
	public static String COUPON_PROMOTION_CAMPAIGN_EXPIRES_NUM_DAYS = "expiration_num_days";
	public static String COUPON_PROMOTION_CAMPAIGN_CODE_PREFIX 		= "code_prefix";
	public static String COUPON_PROMOTION_CAMPAIGN_MESSAGE_SUBJECT 	= "campaign_message_subject";
	public static String COUPON_PROMOTION_CAMPAIGN_MESSAGE_LINK 	= "campaign_message_link";
	public static String COUPON_PROMOTION_CAMPAIGN_MESSAGE_PRESSLINK = "campaign_message_presslink";
	public static String COUPON_PROMOTION_CAMPAIGN_MESSAGE 			= "campaign_message";
	public static String COUPON_PROMOTION_CAMPAIGN_RECIPIENTS 		= "campaign_recipients";
	public static String COUPON_PROMOTION_CAMPAIGN_IR1 				= "ir1";
	public static String COUPON_PROMOTION_CAMPAIGN_IR2 				= "ir2";
	public static String COUPON_PROMOTION_CAMPAIGN_IR3 				= "ir3";
	public static String COUPON_PROMOTION_CAMPAIGN_IR4 				= "ir4";
	public static String COUPON_PROMOTION_CAMPAIGN_SR1 				= "sr1";
	public static String COUPON_PROMOTION_CAMPAIGN_SR2 				= "sr2";
	public static String COUPON_PROMOTION_CAMPAIGN_FPR1 			= "fpr1";
	public static String COUPON_PROMOTION_CAMPAIGN_FPR2 			= "fpr2";

	public static String COUPON_PROMOTION_CAMPAIGN_RECIPIENT_ENTITY	   				= "CouponPromotionCampaignRecipient";
	public static String COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_RECIPIENT	  	= "recipient";
	public static String COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_ACTIVATED	   	= "activated";
	public static String COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_ACTIVATION_DATE	= "activation_date";
	public static String COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_PROMO_CODE		= "promo_code";
	public static String COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_COUPON_PROMOTION	= "coupon_promotion";

	protected void defineEntities(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY(COUPON_PROMOTION_CAMPAIGN_ENTITY,
				COUPON_PROMOTION_CAMPAIGN_TITLE,Types.TYPE_STRING,null,
				COUPON_PROMOTION_CAMPAIGN_CODE_PREFIX,Types.TYPE_STRING,null,
				COUPON_PROMOTION_CAMPAIGN_MESSAGE_SUBJECT,Types.TYPE_STRING,"",
				COUPON_PROMOTION_CAMPAIGN_MESSAGE_LINK,Types.TYPE_STRING,"",
				COUPON_PROMOTION_CAMPAIGN_MESSAGE_PRESSLINK,Types.TYPE_STRING,null,
				COUPON_PROMOTION_CAMPAIGN_MESSAGE,Types.TYPE_STRING,null,
				COUPON_PROMOTION_CAMPAIGN_PROMOTION,Types.TYPE_REFERENCE,PromotionModule.PROMOTION_ENTITY,null,
				COUPON_PROMOTION_CAMPAIGN_PROMO_LIST,Types.TYPE_STRING | Types.TYPE_ARRAY,null,
				COUPON_PROMOTION_CAMPAIGN_EXPIRES_NUM_DAYS,Types.TYPE_INT,null,
				COUPON_PROMOTION_CAMPAIGN_RECIPIENTS,Types.TYPE_ARRAY | Types.TYPE_REFERENCE,COUPON_PROMOTION_CAMPAIGN_RECIPIENT_ENTITY,EMPTY_LIST,
				COUPON_PROMOTION_CAMPAIGN_IR1,Types.TYPE_LONG,0L,
				COUPON_PROMOTION_CAMPAIGN_IR2,Types.TYPE_LONG,0L,
				COUPON_PROMOTION_CAMPAIGN_IR3,Types.TYPE_LONG,0L,
				COUPON_PROMOTION_CAMPAIGN_IR4,Types.TYPE_LONG,0L,
				COUPON_PROMOTION_CAMPAIGN_SR1,Types.TYPE_STRING,null,
				COUPON_PROMOTION_CAMPAIGN_SR2,Types.TYPE_STRING,null,
				COUPON_PROMOTION_CAMPAIGN_FPR1,Types.TYPE_DOUBLE,0.0,
				COUPON_PROMOTION_CAMPAIGN_FPR2,Types.TYPE_DOUBLE,0.0);

		DEFINE_ENTITY(COUPON_PROMOTION_CAMPAIGN_RECIPIENT_ENTITY,
				COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_RECIPIENT,Types.TYPE_STRING,null,
				COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_ACTIVATED,Types.TYPE_BOOLEAN,false,
				COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_ACTIVATION_DATE,Types.TYPE_DATE,null,
				COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_PROMO_CODE,Types.TYPE_STRING,null,
				COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_COUPON_PROMOTION,Types.TYPE_REFERENCE,PromotionModule.COUPON_PROMOTION_ENTITY,null
		);

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
