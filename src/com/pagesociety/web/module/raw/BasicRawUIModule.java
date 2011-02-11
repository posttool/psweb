package com.pagesociety.web.module.raw;

import java.util.Date;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.module.RawUIModule;
import com.pagesociety.web.module.permissions.PermissionEvaluator;

public abstract class BasicRawUIModule extends RawUIModule
{

	protected String[] submode_names;
	protected String module_name;

	public void declareSubmodes(String... modes) throws InitializationException
	{
		try
		{
			declareSubmode(0,"submode_default");
			for (int i=0; i<modes.length; i++)
			{
				declareSubmode(i+1, modes[i]);
			}
		}
		catch (Exception e)
		{
			ERROR(e);
			throw new InitializationException("FAILED BINDING SUBMODE " + e.getMessage());
		}
	}

	public void declareSubmodeNames(String...names)
	{
		submode_names = names;
	}

	
	// utilities
	protected boolean is_admin(UserApplicationContext uctx)
	{
		Entity user = (Entity) uctx.getUser();
		return PermissionEvaluator.IS_ADMIN(user);
	}

	protected void call_login(UserApplicationContext uctx, Map<String, Object> params)
	{
		CALL_WITH_INFO(uctx, "UserModuleRawUI", RAW_SUBMODE_DEFAULT, RAW_SUBMODE_DEFAULT, "Module requires login.");
	}

	protected void HEAD(UserApplicationContext uctx, Map<String, Object> params)
	{
		DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, RAW_UI_FONT_SIZE, RAW_UI_LINK_COLOR, RAW_UI_LINK_HOVER_COLOR);
		P(uctx);
		SPAN(uctx,module_name , 18);
		DISPLAY_ERROR(uctx, params);
		DISPLAY_INFO(uctx, params);
		P(uctx);
	}

	protected void NAV(UserApplicationContext uctx, Map<String, Object> params)
	{
		NAV(get_user_buf(uctx));
	}

	protected void NAV(StringBuilder uctx)
	{
		P(uctx);
		P(uctx);
		A_GET(uctx, getName(), RAW_SUBMODE_DEFAULT, "[ HOME ]");
		BR(uctx);
		
		for (int i=0; i<submode_names.length; i++)
		{
			A_GET(uctx, getName(), i+1, submode_names[i]);
			BR(uctx);
		}

		P(uctx);
	}
	
	protected void FOOTER(UserApplicationContext uctx, Map<String, Object> params)
	{
		P(uctx);
		SPAN(uctx, module_name + new Date(), 9);
		P(uctx);
		DOCUMENT_END(uctx);
	}

	// submodes
	public void submode_default(UserApplicationContext uctx, Map<String, Object> params)
	{
		if (!is_admin(uctx))
		{
			call_login(uctx, params);
			return;
		}
		HEAD(uctx, params);
		NAV(uctx, params);
		FOOTER(uctx, params);
	}
	
}
