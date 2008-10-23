package com.pagesociety.web.module;

import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.module.user.UserModule;

public class PermissionsModule extends WebStoreModule
{
	
	private boolean is_user(Entity user)
	{
		return(user != null && user.getType().equals(UserModule.USER_ENTITY)); 	
	}
	
	protected boolean IS_ADMIN(Entity user)
	{
		return  is_user(user)
				&& 
			    (user.getId()==1 || IS_ROLE(user,UserModule.USER_ROLE_WHEEL));

	}
	
	protected boolean IS_LOGGED_IN(Entity user)
	{
		return user != null;
	}
	
	protected boolean IS_CREATOR(Entity user,Entity record) throws PersistenceException
	{		
		if(record == null)
			return false;
		record = EXPAND(record);
		
		return (is_user(user) 
				&&
				record.getAttribute(FIELD_CREATOR).equals(user));
	
	}
	
	protected boolean IS_ROLE(Entity user,int role)
	{
		return is_user(user)
				&&
				((List<Integer>)user.getAttribute(UserModule.FIELD_ROLES)).contains(role);
	}
	
	
}
