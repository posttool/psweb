package com.pagesociety.web.module;

import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.PersistentStore;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.module.user.UserModule;

public class PermissionsModule extends WebStoreModule
{
	
	private static boolean is_user(Entity user)
	{
		return(user != null && user.getType().equals(UserModule.USER_ENTITY)); 	
	}
	
	public static boolean IS_ADMIN(Entity user)
	{
		return  is_user(user)
				&& 
			    (user.getId()==1 || IS_ROLE(user,UserModule.USER_ROLE_WHEEL));

	}
	
	public static boolean IS_LOGGED_IN(Entity user)
	{
		return user != null;
	}
	

	public static boolean IS_CREATOR(PersistentStore store,Entity user,Entity record) throws PersistenceException
	{		
		if(record == null)
			return false;
		record = EXPAND(store,record);
		
		return (is_user(user) 
				&&
				record.getAttribute(FIELD_CREATOR).equals(user));
	
	}
	
	public boolean IS_CREATOR(Entity user,Entity record) throws PersistenceException
	{		
		if(record == null)
			return false;
		record = EXPAND(store,record);
		
		return (is_user(user) 
				&&
				record.getAttribute(FIELD_CREATOR).equals(user));
	
	}
	
	public static boolean IS_ROLE(Entity user,int role)
	{
		return is_user(user)
				&&
				((List<Integer>)user.getAttribute(UserModule.FIELD_ROLES)).contains(role);
	}
	
	public static boolean IS_SAME(Entity user1,Entity user2)
	{
		return user1 != null && user1.equals(user2);
	}
	
}
