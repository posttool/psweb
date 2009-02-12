package com.pagesociety.web.module.encryption;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.PermissionsModule;
import com.pagesociety.web.module.RawUIModule;


public class EncryptionModuleRawUI extends RawUIModule 
{	
	private static final String SLOT_ENCRYPTION_MODULE = "encryption-module";
	protected IEncryptionModule encryption_module;
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		encryption_module = (IEncryptionModule)getSlot(SLOT_ENCRYPTION_MODULE);
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_ENCRYPTION_MODULE,IEncryptionModule.class,true);
	}
	
	protected void declareSubmodes(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		try{
			declareSubmode(RAW_SUBMODE_DEFAULT,  "submode_default");
		}catch(Exception e)
		{
			ERROR(e);
			throw new InitializationException("FAILED BINDING SUBMODE "+e.getMessage());
		}
	}
	
	public void submode_default(UserApplicationContext uctx,Map<String,Object> params) throws WebApplicationException,FileNotFoundException,IOException
	{
		Entity user = (Entity)uctx.getUser();
		if(!PermissionsModule.IS_ADMIN(user))
		{
			CALL_WITH_INFO(uctx,"UserModuleRawUI",RAW_SUBMODE_DEFAULT,RAW_SUBMODE_DEFAULT,"Encryption module requires login.");
		
		}else if( encryption_module.isConfigured())
		{	
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,"ENCRYPTION PHRASE",16);
			P(uctx);
			SPAN(uctx,"encryption phrase is set. please resart server to reset.");
			DOCUMENT_END_RETURN_IN(uctx, 1500);
		}
		else if(params.get("do_submit") != null)
		{

			String secret_phrase = (String)params.get("phrase");
			try{
				encryption_module.setSecretKeyPhrase(secret_phrase);
			}catch(Exception e)
			{
				GOTO_WITH_ERROR(uctx, RAW_SUBMODE_DEFAULT, "ERROR: "+e.getMessage());
				return;
			}
						
			GOTO(uctx,RAW_SUBMODE_DEFAULT);
		}
		else
		{
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			SPAN(uctx,"ENCRYPTION PHRASE",18);
			DISPLAY_ERROR(uctx,params);
			DISPLAY_INFO(uctx,params);
			P(uctx);
			FORM_START(uctx,getName(),RAW_SUBMODE_DEFAULT,"do_submit",true);
				TABLE_START(uctx, 0, 400);
					TR_START(uctx);
					TD(uctx, "phrase:");TD_START(uctx);FORM_PASSWORD_FIELD(uctx, "phrase", 30);TD_END(uctx);
					TR_END(uctx);
					TR_END(uctx);
				TABLE_END(uctx);
				P(uctx);
			 FORM_SUBMIT_BUTTON(uctx, "Submit");
		   
		   FORM_END(uctx);
		   DOCUMENT_END(uctx);
		}

	}
	

}
