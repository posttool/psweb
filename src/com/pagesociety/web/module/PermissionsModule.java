package com.pagesociety.web.module;

import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.module.user.UserModule;

public class PermissionsModule extends WebModule
{
	UserModule user_module;
	private static final String SLOT_USER_MODULE = "user-module"; 
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
		
	protected boolean IS_ADMIN(Entity user)
	{
		return user_module.isAdmin(user);
	}
	
	protected boolean IS_CREATOR(Entity user,Entity record)
	{
		return user != null && user_module.isCreator(user, record);
	}
	
	protected boolean IS_ROLE(Entity user,int role)
	{
		return user != null && user_module.isRole(user, role);
	}

}
