package com.pagesociety.web.module.ecommerce.promo;


import java.util.Date;
import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.module.RawUIModule;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.permissions.PermissionEvaluator;



public class CouponPromotionManagerRawUI extends RawUIModule 
{	
	private static final String SLOT_PROMOTION_CAMPAIGN_MODULE = "promotion-campaign-module";
	protected CouponPromotionManagerModule promotion_campaign_module;
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		promotion_campaign_module = (CouponPromotionManagerModule)getSlot(SLOT_PROMOTION_CAMPAIGN_MODULE);
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_PROMOTION_CAMPAIGN_MODULE,CouponPromotionManagerModule.class,true);
	}

	private static final int RAW_SUBMODE_CREATE_CAMPAIGN = 0x01;
	private static final int RAW_SUBMODE_SHOW_CAMPAIGN 	 = 0x02;
	private static final int RAW_SUBMODE_ADD_PROMOTION 	 = 0x03;
	protected void declareSubmodes(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		try{
			declareSubmode(RAW_SUBMODE_DEFAULT,   "submode_default");
			declareSubmode(RAW_SUBMODE_CREATE_CAMPAIGN,  "submode_create_campaign");
			declareSubmode(RAW_SUBMODE_SHOW_CAMPAIGN, 	 "submode_show_campaign");
			declareSubmode(RAW_SUBMODE_ADD_PROMOTION, 	 "submode_add_promotion");
		}catch(Exception e)
		{
			ERROR(e);
			throw new InitializationException("FAILED BINDING SUBMODE "+e.getMessage());
		}
	}
	
	public void submode_default(UserApplicationContext uctx,Map<String,Object> params) throws Exception
	{
		
		Entity user = (Entity)uctx.getUser();
		if(!PermissionEvaluator.IS_ADMIN(user))
		{
			CALL_WITH_INFO(uctx,"UserModuleRawUI",RAW_SUBMODE_DEFAULT,RAW_SUBMODE_DEFAULT,"Coupon promotion manager module requires admin login.");
			return;
		}
		if(params.get("delete_campaign") != null)
		{
			long c_id 		  		  = Long.parseLong((String)params.get("delete_campaign"));

			try{
				promotion_campaign_module.deleteCouponPromotionCampaign(c_id);
			}catch(Exception e)
			{
			
				GOTO_WITH_ERROR(uctx, RAW_SUBMODE_DEFAULT,e.getMessage());
				return;
			}
			
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,"DELETED PROMOTION OK.",18);

			JS_TIMED_REDIRECT(uctx, getName(), RAW_SUBMODE_DEFAULT, 2000);
		}
		else
		{
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,"PROMO CAMPAIGN MANAGER",18);
			DISPLAY_ERROR(uctx,params);
			DISPLAY_INFO(uctx,params);
			P(uctx);
			TABLE_START(uctx, 0, 400);
			List<Entity> campaigns = promotion_campaign_module.getCouponPromotionCampaigns();
			for(int i = 0;i < campaigns.size();i++ )
			{
				Entity campaign = campaigns.get(i);
				String title = (String)campaign.getAttribute(CouponPromotionManagerModule.COUPON_PROMOTION_CAMPAIGN_TITLE);
				TR_START(uctx);
				TD(uctx, title);TD_LINK(uctx,getName(),RAW_SUBMODE_SHOW_CAMPAIGN,"[view]","campaign_id",campaign.getId());TD_LINK(uctx,getName(),RAW_SUBMODE_DEFAULT,"[delete]","delete_campaign",String.valueOf(campaign.getId()));
				TR_END(uctx);
			}
			TABLE_END(uctx);
			P(uctx);
			A(uctx,getName(),RAW_SUBMODE_CREATE_CAMPAIGN,"[ + Add New Campaign ]");

			DOCUMENT_END(uctx);
			
		}
	}
	
	
	public void submode_create_campaign(UserApplicationContext uctx,Map<String,Object> params)
	{
		Entity user = (Entity)uctx.getUser();
		if(!PermissionEvaluator.IS_ADMIN(user))
		{
			CALL_WITH_INFO(uctx,"UserModuleRawUI",RAW_SUBMODE_DEFAULT,RAW_SUBMODE_DEFAULT,"Coupon promotion manager module requires admin login.");
			return;
		}
		
		if(params.get("do_submit") != null)
		{
			String c_title;
			String c_promo_prefix; 	
			int c_num_days_valid;	
			long c_promo_id;	
			List<String> c_promo_list;
			String c_promo_subject;
			String c_promo_link;
			String c_promo_presslink;
			String c_promo_message;
			long c_promo_ir1;
			long c_promo_ir2;		
			long c_promo_ir3;		
			long c_promo_ir4;		
			String c_promo_sr1;		
			String c_promo_sr2;	
			double c_promo_fpr1;		
			double c_promo_fpr2;	
			
				try{
					c_title 		  	  = NORMALIZE((String)params.get("campaign_title"));
					REQUIRED("title",c_title);	
					c_promo_prefix 		  = NORMALIZE((String)params.get("promo_prefix"));
					c_num_days_valid 	  = Integer.parseInt((String)params.get("num_days_valid"));
					c_promo_id	  		  = Long.parseLong((String)params.get("promo_id"));
					c_promo_subject		  =	NORMALIZE((String)params.get("promo_message_subject"));
					REQUIRED("promo_subject",c_promo_subject);
					c_promo_link		  =	NORMALIZE((String)params.get("promo_message_link"));
					c_promo_presslink		  =	NORMALIZE((String)params.get("promo_message_presslink"));
					c_promo_message		  = NORMALIZE((String)params.get("promo_message"));
					REQUIRED("promo_message",c_promo_message);
					c_promo_list	  	  = PARSE_LIST((String)params.get("campaign_promo_list"));
					REQUIRED("promo_list",c_promo_list);
					c_promo_ir1		  	  = Long.parseLong((String)params.get("ir1"));
					c_promo_ir2		  	  = Long.parseLong((String)params.get("ir2"));
					c_promo_ir3		  	  = Long.parseLong((String)params.get("ir3"));
					c_promo_ir4		  	  = Long.parseLong((String)params.get("ir4"));
					c_promo_sr1		  	  = NORMALIZE((String)params.get("sr1"));
					c_promo_sr2		  	  = NORMALIZE((String)params.get("sr2"));
					c_promo_fpr1		  = Double.parseDouble((String)params.get("fpr1"));
					c_promo_fpr2		  = Double.parseDouble((String)params.get("fpr2"));
				}catch(Exception e)
				{
					GOTO_WITH_ERROR(uctx, RAW_SUBMODE_CREATE_CAMPAIGN,e.getMessage(),
							"campaign_title",params.get("campaign_title"),
							"promo_prefix",params.get("promo_prefix"),
							"num_days_valid",params.get("num_days_valid"),
							"promo_id",params.get("promo_id"),
							"campaign_promo_list",(String)params.get("campaign_promo_list"),
							"promo_message_subject",params.get("promo_message_subject"),
							"promo_message_link",params.get("promo_message_link"),
							"promo_message_presslink",params.get("promo_message_presslink"),
							"promo_message",params.get("promo_message"),
							"ir1",params.get("ir1"),
							"ir2",params.get("ir2"),
							"ir3",params.get("ir3"),
							"ir4",params.get("ir4"),
							"sr1",params.get("sr1"),
							"sr2",params.get("sr2"),
							"fpr1",params.get("fpr1"),
							"fpr2",params.get("fpr2"));
					return;
				}
				try{
				Entity promotion = promotion_campaign_module.createCouponPromotionCampaign(null, c_title, c_promo_prefix, c_promo_id,c_promo_ir1,c_promo_ir2,c_promo_ir3,c_promo_ir4,c_promo_sr1,c_promo_sr2,c_promo_fpr1,c_promo_fpr2, c_num_days_valid,c_promo_subject,c_promo_link,c_promo_presslink,c_promo_message, c_promo_list);
				
				DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
				P(uctx);
				SPAN(uctx,"CREATED CAMPAIGN....",18);
				PRE(uctx,"title:\t\t"+c_title+"\n"+
						"num days valid:\t\t"+c_num_days_valid+"\n"+
						"promo prefix:\t\t"+c_promo_prefix+"\n"+
						"promo id:\t\t"+c_promo_id+"\n"+
						"promo subject:\t\t"+c_promo_subject+"\n"+
						"promo link:\t\t"+c_promo_link+"\n"+
						"promo presslink:\t\t"+c_promo_presslink+"\n"+
						"promo press link:\t\t"+c_promo_presslink+"\n"+
						"promo message:\t\t"+c_promo_message+"\n"+
						"promo_list list:\t\t"+c_promo_list);
				JS_TIMED_REDIRECT(uctx, getName(), RAW_SUBMODE_SHOW_CAMPAIGN, 1000,"campaign_id",promotion.getId());
			}catch(Exception e)
			{
				ERROR_PAGE(uctx,e);
			}
		}
		else
		{
			
			String c_title 		  		= NORMALIZE((String)params.get("campaign_title"));
			String c_promo_prefix 	  	= NORMALIZE((String)params.get("promo_prefix"));
			String c_num_days_valid  	= NORMALIZE((String)params.get("num_days_valid"));
			String c_promo_id	  	  	= NORMALIZE((String)params.get("promo_id"));
			String c_promo_list	  		= NORMALIZE((String)params.get("campaign_promo_list"));
			String c_promo_subject	  	= NORMALIZE((String)params.get("promo_message_subject"));
			String c_promo_link		  	= NORMALIZE((String)params.get("promo_message_link"));
			String c_promo_presslink	= NORMALIZE((String)params.get("promo_message_presslink"));
			String c_promo_message	  	= NORMALIZE((String)params.get("promo_message"));
			String c_promo_ir1		  	= NORMALIZE((String)params.get("ir1"));
			String c_promo_ir2		  	= NORMALIZE((String)params.get("ir2"));
			String c_promo_ir3		  	= NORMALIZE((String)params.get("ir3"));
			String c_promo_ir4		  	= NORMALIZE((String)params.get("ir4"));
			String c_promo_sr1		  	= NORMALIZE((String)params.get("sr1"));
			String c_promo_sr2		  	= NORMALIZE((String)params.get("sr2"));
			String c_promo_fpr1		  	= NORMALIZE((String)params.get("fpr1"));
			String c_promo_fpr2		  	= NORMALIZE((String)params.get("fpr2"));
			
			try{
				List<Entity> promotions = promotion_campaign_module.getExistingPromotions();
				String[] promo_options = new String[promotions.size()];
				String[] promo_values = new String[promotions.size()];
				for(int i = 0;i < promotions.size();i++)
				{
					promo_options[i] = (String)promotions.get(i).getAttribute(PromotionModule.PROMOTION_FIELD_TITLE)+" - "+(String)promotions.get(i).getAttribute(PromotionModule.PROMOTION_FIELD_DESCRIPTION);
					promo_values[i]  = String.valueOf(promotions.get(i).getId());
				}
				
				DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
				P(uctx);
				SPAN(uctx,"CREATE NEW PROMO CAMPAIGN",18);
				DISPLAY_ERROR(uctx,params);
				DISPLAY_INFO(uctx,params);
				P(uctx);
				FORM_START(uctx,getName(),RAW_SUBMODE_CREATE_CAMPAIGN,"do_submit",true);
					TABLE_START(uctx, 0, 400);
						TR_START(uctx);
						TD(uctx, "Title:");TD_START(uctx);FORM_INPUT_FIELD(uctx, "campaign_title",30,c_title == null?"":c_title);TD_END(uctx);
						TR_END(uctx);
						TR_START(uctx);
						TD(uctx, "Num Days Valid:");TD_START(uctx);FORM_INPUT_FIELD(uctx, "num_days_valid",4,c_num_days_valid == null?"90":c_num_days_valid);NBSP(uctx);SPAN(uctx,"(0 == forever)",9);TD_END(uctx);
						TR_END(uctx);
						TR_START(uctx);
						TD(uctx, "Promo Prefix:");TD_START(uctx);FORM_INPUT_FIELD(uctx, "promo_prefix",30,c_promo_prefix==null?"":c_promo_prefix);TD_END(uctx);
						TR_END(uctx);
						TR_START(uctx);
						TD(uctx, "Promotion:");TD_START(uctx);FORM_PULLDOWN_MENU(uctx, "promo_id",promo_options,promo_values,210,c_promo_id==null?promo_values[0]:c_promo_id);NBSP(uctx);A_GET(uctx,getName(),RAW_SUBMODE_ADD_PROMOTION,"[+]");TD_END(uctx);
						TR_END(uctx);
						TR_START(uctx);
						TD(uctx, "Initial Register Vals:");
						TD_START(uctx);
							SPAN(uctx,"ir1:",8);FORM_INPUT_FIELD(uctx, "ir1",4,c_promo_ir1 == null?"0":c_promo_ir1);NBSP(uctx);
							SPAN(uctx,"ir2:",8);FORM_INPUT_FIELD(uctx, "ir2",4,c_promo_ir2 == null?"0":c_promo_ir2);NBSP(uctx);
							SPAN(uctx,"ir3:",8);FORM_INPUT_FIELD(uctx, "ir3",4,c_promo_ir3 == null?"0":c_promo_ir3);NBSP(uctx);
							SPAN(uctx,"ir4:",8);FORM_INPUT_FIELD(uctx, "ir4",4,c_promo_ir4 == null?"0":c_promo_ir4);P(uctx);
							SPAN(uctx,"sr1:",8);FORM_INPUT_FIELD(uctx, "sr1",4,c_promo_sr1 == null?"":c_promo_sr1);NBSP(uctx);
							SPAN(uctx,"sr2:",8);FORM_INPUT_FIELD(uctx, "sr2",4,c_promo_sr1 == null?"":c_promo_sr2);NBSP(uctx);
							SPAN(uctx,"fpr1:",8);FORM_INPUT_FIELD(uctx, "fpr1",4,c_promo_fpr1 == null?"0.0":c_promo_fpr1);NBSP(uctx);
							SPAN(uctx,"fpr2:",8);FORM_INPUT_FIELD(uctx, "fpr2",4,c_promo_fpr2 == null?"0.0":c_promo_fpr2);
						TD_END(uctx);
						TR_END(uctx);
						TR_START(uctx);
						TD(uctx, "Promo Subject:");TD_START(uctx);FORM_INPUT_FIELD(uctx, "promo_message_subject",30,c_promo_subject==null?"":c_promo_subject);TD_END(uctx);
						TR_END(uctx);
						TR_START(uctx);
						TD(uctx, "Promo Link:");TD_START(uctx);FORM_INPUT_FIELD(uctx, "promo_message_link",70,c_promo_link==null?"":c_promo_link);TD_END(uctx);
						TR_END(uctx);
						TR_START(uctx);
						TD(uctx, "Press Release Link:");TD_START(uctx);FORM_INPUT_FIELD(uctx, "promo_message_presslink",70,c_promo_presslink==null?"":c_promo_presslink);TD_END(uctx);
						TR_END(uctx);						
						TR_START(uctx);
						TD(uctx, "Promo Message:");TD_START(uctx);FORM_TEXTAREA_FIELD(uctx, "promo_message",70,5,c_promo_message == null?"":c_promo_message);TD_END(uctx);
						TR_END(uctx);
						TR_START(uctx);
						TD(uctx, "Promo List:");TD_START(uctx);SPAN(uctx,"(email addresses or names)",8);BR(uctx);FORM_TEXTAREA_FIELD(uctx, "campaign_promo_list",70,10,c_promo_list==null?"":c_promo_list);TD_END(uctx);
						TR_END(uctx);
					TABLE_END(uctx);
					FORM_SUBMIT_BUTTON(uctx,"+ Create");
					FORM_END(uctx);
				HR(uctx);
				A(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ Back ]");
				DOCUMENT_END(uctx);
			}catch(Exception e)
			{
				ERROR_PAGE(uctx, e);
			}
		}
		
	}
	
	public void submode_show_campaign(UserApplicationContext uctx,Map<String,Object> params)
	{
		
		Entity user = (Entity)uctx.getUser();
		if(!PermissionEvaluator.IS_ADMIN(user))
		{
			CALL_WITH_INFO(uctx,"UserModuleRawUI",RAW_SUBMODE_DEFAULT,RAW_SUBMODE_DEFAULT,"Coupon promotion manager module requires admin login.");
			return;
		}
		
		if(params.get("activate_campaign") != null)
		{
			long c_id=-1;
			try{
				c_id 				= Long.parseLong((String)params.get("campaign_id"));
				promotion_campaign_module.activateCouponPromotionCampaign(c_id);
				
			}catch(Exception e)
			{
				e.printStackTrace();
				GOTO_WITH_ERROR(uctx, RAW_SUBMODE_SHOW_CAMPAIGN,e.getMessage(),"campaign_id",c_id);
				return;
			}
			
			
		}
		else if(params.get("add_recipient") != null)
		{
			long c_id=-1;
			try{
				c_id 				= Long.parseLong((String)params.get("campaign_id"));
				String name 		= NORMALIZE((String)params.get("recipient_name"));
				REQUIRED("name", name);
				promotion_campaign_module.addRecipient(null, c_id, name);
			}catch(Exception e)
			{
				GOTO_WITH_ERROR(uctx, RAW_SUBMODE_SHOW_CAMPAIGN,e.getMessage(),"campaign_id",c_id);
				return;
			}
			
			
		}
		else if(params.get("resend") != null)
		{
			long c_id=-1;
			long r_id=-1;
			try{
				c_id 				= Long.parseLong((String)params.get("campaign_id"));
				r_id				= Long.parseLong((String)params.get("recipient_id"));
				promotion_campaign_module.resendEmail(c_id, r_id);
			}catch(Exception e)
			{
				GOTO_WITH_ERROR(uctx, RAW_SUBMODE_SHOW_CAMPAIGN,e.getMessage(),"campaign_id",c_id);
				return;
			}
			
			
		}

		try{
			long c_id 				= Long.parseLong((String)params.get("campaign_id"));
			Entity campaign 		= promotion_campaign_module.getCouponPromotionCampaign(c_id);
			List<Entity> recipients = (List<Entity>)campaign.getAttribute(CouponPromotionManagerModule.COUPON_PROMOTION_CAMPAIGN_RECIPIENTS);

			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR,RAW_UI_FONT_SIZE, 10,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,(String)campaign.getAttribute(CouponPromotionManagerModule.COUPON_PROMOTION_CAMPAIGN_TITLE),18);
			DISPLAY_ERROR(uctx,params);
			DISPLAY_INFO(uctx,params);
			A(uctx,getName(),RAW_SUBMODE_SHOW_CAMPAIGN,"[ ACTIVATE RECIPIENTS ]","activate_campaign",true,"campaign_id",c_id);
			P(uctx);
			TABLE_START(uctx, 1, 1000);
				TR_START(uctx);
				TH(uctx,"Recipient");
				TH(uctx,"Promo Code");
				TH(uctx,"Activation Date");
				TH(uctx,"Expiration Date");
				TH(uctx,"Promo Used");
				TH(uctx,"Redemption Date");
				TH(uctx,"");
				TR_END(uctx);	
			for(int i = 0;i < recipients.size();i++)
			{
				Entity r 			= recipients.get(i);
				String recipient_name		  	= (String)r.getAttribute(CouponPromotionManagerModule.COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_RECIPIENT);
				recipient_name = recipient_name.replaceAll("<", "&lt;");
				recipient_name = recipient_name.replaceAll(">", "&gt;");
				boolean activated 	= (Boolean)r.getAttribute(CouponPromotionManagerModule.COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_ACTIVATED);
				if(activated)
				{
					Date activation_date = (Date)r.getAttribute(CouponPromotionManagerModule.COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_ACTIVATION_DATE);
					String promo_code 	 = (String)r.getAttribute(CouponPromotionManagerModule.COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_PROMO_CODE);
					Entity coupon_promo  = (Entity)r.getAttribute(CouponPromotionManagerModule.COUPON_PROMOTION_CAMPAIGN_RECIPIENT_FIELD_COUPON_PROMOTION);
					int num_times_used   = (Integer)coupon_promo.getAttribute(PromotionModule.COUPON_PROMOTION_NO_TIMES_CODE_HAS_BEEN_USED);
					Date expr_date   = (Date)coupon_promo.getAttribute(PromotionModule.COUPON_PROMOTION_FIELD_EXPIRATION_DATE);
					boolean promo_used = num_times_used > 0;
					Date used_date = null;
					if(promo_used)
						used_date = (Date)coupon_promo.getAttribute(WebStoreModule.FIELD_LAST_MODIFIED); 
					TR_START(uctx);
						TD(uctx,recipient_name);
						TD(uctx,promo_code);
						TD(uctx,activation_date.toString());
						TD(uctx,expr_date.toString());
						TD_START(uctx);
						if(promo_used)
							SPAN(uctx, "Yes", "green");
						else
							SPAN(uctx, "No", "black");
						TD_END(uctx);
						TD(uctx,promo_used?used_date.toString():"");
						TD_START(uctx);
						if(promo_used)
							NBSP(uctx);
						else
							A_GET(uctx,getName(),RAW_SUBMODE_SHOW_CAMPAIGN,"[resend code]","resend",true,"recipient_id",r.getId(),"campaign_id",c_id);
						
						TD_END(uctx);
					TR_END(uctx);	
				}
				else
				{
				TR_START(uctx);
					TD(uctx,recipient_name);
					TD(uctx,"");
					TD(uctx,"");
					TD(uctx,"");
					TD(uctx,"");
					TD(uctx,"");
					TD(uctx,"");
				TR_END(uctx);
				}
			}
			TR_START(uctx);
				TD_START(uctx);FORM_START(uctx, getName(), RAW_SUBMODE_SHOW_CAMPAIGN,"add_recipient",true,"campaign_id",c_id);FORM_INPUT_FIELD(uctx, "recipient_name", 30, "");TD_END(uctx);
				TD_START(uctx);FORM_SUBMIT_BUTTON(uctx,"+ Add Recipient");FORM_END(uctx);TD_END(uctx);
				TD(uctx,"");
				TD(uctx,"");
				TD(uctx,"");
				TD(uctx,"");
				TD(uctx,"");
			TR_END(uctx);
			TABLE_END(uctx);
			HR(uctx);
			List<String> promo_list = (List<String>)campaign.getAttribute(CouponPromotionManagerModule.COUPON_PROMOTION_CAMPAIGN_PROMO_LIST); 
			StringBuilder buf = new StringBuilder();
			for(int i = 0;i < promo_list.size();i++)
			{
				String recipient_name = promo_list.get(i);
				recipient_name = recipient_name.replaceAll("<", "&lt;");
				recipient_name = recipient_name.replaceAll(">", "&gt;");
				buf.append(recipient_name);
				buf.append(", ");
			}
			buf.setLength(buf.length()-2);
			TABLE_START(uctx, 1, 1000);
			TR_START(uctx);
			TH(uctx,"recipients");
			TH(uctx,"promotion");
			TR_END(uctx);
				TR_START(uctx);
					TD_START(uctx);
						SPAN(uctx,buf.toString());
					TD_END(uctx);
					TD_START(uctx);
						PRE(uctx,
								((Entity)campaign.getAttribute(CouponPromotionManagerModule.COUPON_PROMOTION_CAMPAIGN_PROMOTION)).getAttribute(PromotionModule.PROMOTION_FIELD_TITLE)+"\n"+
								((Entity)campaign.getAttribute(CouponPromotionManagerModule.COUPON_PROMOTION_CAMPAIGN_PROMOTION)).getAttribute(PromotionModule.PROMOTION_FIELD_PROGRAM)+"\n"
						);
					TD_END(uctx);
				TR_END(uctx);
				TABLE_END(uctx);
				HR(uctx);
			A(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ Back ]");
			DOCUMENT_END(uctx);
		}catch(Exception e)
		{
			ERROR_PAGE(uctx,e);
		}

	}

	public void submode_add_promotion(UserApplicationContext uctx,Map<String,Object> params)
	{
		Entity user = (Entity)uctx.getUser();
		if(!PermissionEvaluator.IS_ADMIN(user))
		{
			CALL_WITH_INFO(uctx,"UserModuleRawUI",RAW_SUBMODE_DEFAULT,RAW_SUBMODE_DEFAULT,"Coupon promotion manager module requires admin login.");
			return;
		}
		if(params.get("do_submit") != null)
		{


				String p_title 		  		  = (String)params.get("title");
				String p_description 		  = (String)params.get("description");
				String p_program 	  		  = (String)params.get("program");
				try{
					if(IS_NULL(p_title))
						throw new Exception("title is required.");
					if(IS_NULL(p_program))
						throw new Exception("program is required.");
					if(p_description == null || p_description.trim() == "")
						p_description = null;
					promotion_campaign_module.createPromotion(null, p_title, p_description, p_program);
				}catch(Exception e)
				{
				
					GOTO_WITH_ERROR(uctx, RAW_SUBMODE_ADD_PROMOTION,e.getMessage(),"program",p_program,"title",p_title,"description",p_description);
					return;
				}
				
				DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
				P(uctx);
				SPAN(uctx,"CREATED PROMOTION",18);
				PRE(uctx,"title:\t\t"+p_title+"\n"+
						"description:\t\t"+p_description+"\n"+
						"program:\t\t"+p_program+"\n");

				JS_TIMED_REDIRECT(uctx, getName(), RAW_SUBMODE_CREATE_CAMPAIGN, 2000);

		}
		else
		{
			StringBuilder promo_examples = new StringBuilder();
			try{
				List<Entity> promotions = promotion_campaign_module.getExistingPromotions();
				for(int i = 0;i < promotions.size();i++)
				{
					Entity promo = promotions.get(i);
					promo_examples.append(promo.getAttribute(PromotionModule.PROMOTION_FIELD_TITLE)+"\n\n");
					promo_examples.append(promo.getAttribute(PromotionModule.PROMOTION_FIELD_DESCRIPTION)+"\n\n");
					promo_examples.append(promo.getAttribute(PromotionModule.PROMOTION_FIELD_PROGRAM)+"\n\n");
					if(i != promotions.size() - 1)
						promo_examples.append(". . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .\n\n");
				}
			}catch(Exception e)
			{
				ERROR_PAGE(uctx,e);
				return;
			}
			
			String p_title 		  		  = (String)params.get("title");
			String p_description 		  = (String)params.get("description");
			String p_program 	  		  = (String)params.get("program");
			
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,"ADD A PROMOTION",18);
			DISPLAY_ERROR(uctx,params);
			DISPLAY_INFO(uctx,params);
			P(uctx);
			FORM_START(uctx,getName(),RAW_SUBMODE_ADD_PROMOTION,"do_submit",true);
			TABLE_START(uctx, 0, 400);
			TR_START(uctx);
			TD(uctx, "Title:");TD_START(uctx);FORM_INPUT_FIELD(uctx, "title",30,p_title==null?"":p_title);TD_END(uctx);
			TR_END(uctx);
				TR_START(uctx);
				TD(uctx, "Description:");TD_START(uctx);FORM_TEXTAREA_FIELD(uctx, "description",65,3,p_description==null?"":p_description);TD_END(uctx);
				TR_END(uctx);
				TR_START(uctx);
				TD(uctx, "Program:");TD_START(uctx);FORM_TEXTAREA_FIELD(uctx, "program",65,25,p_program==null?"":p_program);TD_END(uctx);
				TR_END(uctx);
			TABLE_END(uctx);
			FORM_SUBMIT_BUTTON(uctx,"+ Create");
			FORM_END(uctx);
			HR(uctx);
			A(uctx,getName(),RAW_SUBMODE_CREATE_CAMPAIGN,"[ Back ]");
			P(uctx);
			PRE(uctx,"examples:\n"+promo_examples.toString());
			DOCUMENT_END(uctx);	
		}
	}
	
}
