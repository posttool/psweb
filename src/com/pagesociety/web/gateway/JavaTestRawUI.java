package com.pagesociety.web.gateway;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.gateway.Form;
import com.pagesociety.web.gateway.RawCommunique;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.RawUIModule;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.permissions.PermissionEvaluator;
import com.pagesociety.web.module.qa.BugReporterModule;
import com.pagesociety.web.upload.MultipartForm;
import com.pagesociety.web.upload.MultipartFormException;


public class JavaTestRawUI extends RawUIModule
{



	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);

	}

	protected void defineSlots()
	{
		super.defineSlots();

	}



	protected void declareSubmodes(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		try{
			declareSubmode(RAW_SUBMODE_DEFAULT,    "submode_default");
		}catch(Exception e)
		{
			ERROR(e);
			throw new InitializationException("FAILED BINDING SUBMODE "+e.getMessage());
		}
	}


	public void submode_default(UserApplicationContext uctx,Map<String,Object> params)
	{
		try{
			Entity user = (Entity)uctx.getUser();
		//	if(!PermissionEvaluator.IS_ADMIN(user))
		//	{
		//		CALL_WITH_INFO(uctx,"UserModuleRawUI",RAW_SUBMODE_DEFAULT,RAW_SUBMODE_DEFAULT,getName()+" manager module requires admin login.");
		//		return;
		//	}

			if(params.get("do_test") != null)
			{
				run_test();
			}
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			P(uctx);
			SPAN(uctx,"JAVA TEST",16);
			DISPLAY_ERROR(uctx,params);
			DISPLAY_INFO(uctx,params);
			P(uctx);

			HR(uctx);
			A_GET(uctx, getName(),RAW_SUBMODE_DEFAULT , "[ run test ] ", "do_test","true");


			DOCUMENT_END(uctx);

		}catch(Exception e)
		{
			ERROR_PAGE(uctx,e);
		}
	}

	public void run_test()
	{
		try{

			Map<String,Object> data = (Map<String,Object>) JavaGateway.executeModuleMethod("http://www.thepodolls-dev.com", "Public", "GetNoFlashData");

		}catch(Exception e)
		{
			e.printStackTrace();
		}

	}





}
