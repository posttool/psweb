package com.pagesociety.web.module.user;

import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.LoginFailedException;
import com.pagesociety.web.module.RawUIModule;
import com.pagesociety.web.module.util.Util;

public class UserModuleRawUI extends RawUIModule
{
	private static final String SLOT_USER_MODULE = "user-module";
	protected UserModule user_module;
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		user_module = (UserModule)getSlot(SLOT_USER_MODULE);
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_USER_MODULE,UserModule.class,true);
	}
	
	private static final int RAW_SUBMODE_DO_LOGIN 	  = 0x01;
	private static final int RAW_SUBMODE_DO_LOGOUT 	  = 0x02;
	private static final int RAW_SUBMODE_SHOW_USER 	  = 0x03;
	protected void declareSubmodes(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		try{
			declareSubmode(RAW_SUBMODE_DEFAULT,   "submode_default");
			declareSubmode(RAW_SUBMODE_DO_LOGIN,  "submode_do_login");
			declareSubmode(RAW_SUBMODE_DO_LOGOUT, "submode_do_logout");
			declareSubmode(RAW_SUBMODE_SHOW_USER, "submode_showuser");
		}catch(Exception e)
		{
			ERROR(e);
			throw new InitializationException("FAILED BINDING SUBMODE "+e.getMessage());
		}
	}
	
	protected boolean canExecSubmode(Entity user,int submode,Map<String,Object> params)
	{
			return true;
	}
	
	public void submode_default(UserApplicationContext uctx,Map<String,Object> params)
	{
		if(uctx.getUser() != null)
		{
			GOTO(uctx,RAW_SUBMODE_DO_LOGOUT);
		}
		else
		{
			//HANDLE ERRORS AND MESSAGES IN DOCUMENT_START
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,"LOGIN",18);BR(uctx);DISPLAY_ERROR(uctx,params);BR(uctx);DISPLAY_INFO(uctx,params);
			P(uctx);
			FORM_START(uctx,getName(),RAW_SUBMODE_DO_LOGIN);
				TABLE_START(uctx, 0, 400);
					TR_START(uctx);
					TD(uctx, "email:");TD_START(uctx);FORM_INPUT_FIELD(uctx, "email", 30,(String)params.get("email"));TD_END(uctx);
					TR_END(uctx);
					TR_START(uctx);
					TD(uctx, "password:");TD_START(uctx);FORM_PASSWORD_FIELD(uctx, "password", 30);TD_END(uctx);
					TR_END(uctx);
				TABLE_END(uctx);
				P(uctx);
			 FORM_SUBMIT_BUTTON(uctx, "Login");
		   
		FORM_END(uctx);
		DOCUMENT_END(uctx);
		}
	}
	
	public void submode_do_login(UserApplicationContext uctx,Map<String,Object> params)
	{
		String e = (String)params.get("email");//arg_email
		String p = Util.stringToHexEncodedMD5((String)params.get("password"));//arg_password
		
		try{
			user_module.Login(uctx,e,p);
			RETURN(uctx);
		}catch(LoginFailedException lfe)
		{
			GOTO_WITH_ERROR(uctx,RAW_SUBMODE_DEFAULT,"Bad Login.",params);
		}
		catch(Exception ee)
		{
			ERROR_PAGE(uctx,ee);
		}
	}
	
	public void submode_do_logout(UserApplicationContext uctx,Map<String,Object> params)
	{
		Entity user = (Entity)uctx.getUser();
		if(user == null)
		{
			GOTO_WITH_INFO(uctx,RAW_SUBMODE_DEFAULT," you have been logged out.",params);
		}
		else if(params.get("do_logout") != null)
		{
			try{
				user_module.Logout(uctx);
			}catch(Exception e){}
			GOTO(uctx,RAW_SUBMODE_DO_LOGOUT);
		}
		else
		{
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,"LOGIN",16);
			P(uctx);
			if(user != null)
				SPAN(uctx,"You are currently logged in as "+user.getAttribute(UserModule.FIELD_EMAIL),12);
			else
				SPAN(uctx,"You not currently logged in.",12);
			DOCUMENT_END(uctx);//DOCUMENT_END_RETURN_IN(uctx,3000);
			P(uctx);
			A(uctx,getName(),RAW_SUBMODE_DO_LOGOUT,"[ LOGOUT ]","do_logout","true");
			DOCUMENT_END(uctx);
		} 
	}
	
	public void submode_showuser(UserApplicationContext uctx,Map<String,Object> params)
	{
		Entity user = (Entity)uctx.getUser();
		DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
		P(uctx);
		SPAN(uctx,"LOGIN",16);
		P(uctx);
		if(user != null)
			SPAN(uctx,"You are currently logged in as "+user.getAttribute(UserModule.FIELD_EMAIL),12);
		else
			SPAN(uctx,"You not currently logged in.",12);

		//RETURN_TO_CALLER();
		//JS_TIMED_REDIRECT(buf, return_to, 1500);
		//uctx.setProperty(RETURN_TO, null);
		DOCUMENT_END(uctx);//DOCUMENT_END_RETURN_IN(uctx,3000);
		//RETURN(uctx);
	}
	
}
