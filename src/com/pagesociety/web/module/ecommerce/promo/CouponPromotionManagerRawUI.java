package com.pagesociety.web.module.ecommerce.promo;


import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.module.RawUIModule;
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
	protected void declareSubmodes(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		try{
			declareSubmode(RAW_SUBMODE_DEFAULT,   "submode_default");
			declareSubmode(RAW_SUBMODE_CREATE_CAMPAIGN,  "submode_create_campaign");
			declareSubmode(RAW_SUBMODE_SHOW_CAMPAIGN, 	 "submode_show_campaign");
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
				TD(uctx, title);TD_LINK(uctx,getName(),RAW_SUBMODE_DEFAULT,"[view]");TD_LINK(uctx,getName(),RAW_SUBMODE_DEFAULT,"[delete]");
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
		}
		else
		{
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,"CREATE NEW PROMO CAMPAIGN",18);
			DISPLAY_ERROR(uctx,params);
			DISPLAY_INFO(uctx,params);
			P(uctx);
			SPAN(uctx,"OK THIS IS WHERE WE ARE GONNA CREATE A NEW CAMPAIGN.");
			A(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ Cancel ]");
			DOCUMENT_END(uctx);
			
		}
		
	}
	
	public void submode_show_campaign(UserApplicationContext uctx,Map<String,Object> params)
	{
		
		Entity user = (Entity)uctx.getUser();
		if(!PermissionEvaluator.IS_ADMIN(user))
		{
			CALL_WITH_INFO(uctx,"UserModuleRawUI",RAW_SUBMODE_DEFAULT,RAW_SUBMODE_DEFAULT,"Coupon promotion manager module requires admin login.");
		}
		else
		{
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,"CAMPAIGN TITLE",18);
			DISPLAY_ERROR(uctx,params);
			DISPLAY_INFO(uctx,params);
			P(uctx);
			SPAN(uctx,"OK THIS IS WHERE WE ARE GONNA SHOW A NEW CAMPAIGN.");
			A(uctx,getName(),RAW_SUBMODE_DEFAULT,"[ Back ]");
			DOCUMENT_END(uctx);
			
		}
	}


}
