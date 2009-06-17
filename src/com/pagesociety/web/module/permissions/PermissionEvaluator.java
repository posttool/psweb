package com.pagesociety.web.module.permissions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.PersistentStore;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.user.UserModule;

public class PermissionEvaluator
{ 
	
	public boolean exec(Entity user,String namespace,String permission_code,Map<String,Object> context) throws PersistenceException
	{
		return false;
	}
	
	public static PermissionEvaluator ALWAYS_TRUE_EVALUATOR = new PermissionEvaluator()
	{
		public boolean exec(Entity user,String namespace,String permission_code,Map<String,Object> context) throws PersistenceException
		{
			return true;
		}	
	}; 
	
	public static PermissionEvaluator ALWAYS_FALSE_EVALUATOR = new PermissionEvaluator()
	{
		public boolean exec(Entity user,String namespace,String permission_code,Map<String,Object> context) throws PersistenceException
		{
			return false;
		}	
	}; 

	
	
	//UTIL MACROS//

	/*public Map<String,Object> unflatten_params(Object... flattened_entity)
	{
		Map<String,Object> props = new HashMap<String, Object>();
		for(int i = 0;i < flattened_entity.length;i+=2)
			props.put((String)flattened_entity[i],flattened_entity[i+1]);

		return props;
	}
	*/
	/////PERMISSIONS MACROS//
	
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
		record = WebStoreModule.EXPAND(store,record);
		
		return (is_user(user) 
				&&
				record.getAttribute(WebStoreModule.FIELD_CREATOR).equals(user));
	
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
	
	public static boolean ENTITY_IS(Entity e,String type)
	{
		return e.getType().equals(type);
	}
	
	public static boolean TYPE_IS(String type1,String type2)
	{
		return type1.equals(type2);
	}
	
	public static boolean ENTITY_IS_ANY_OF(Entity e,String... names)
	{
		String type = e.getType();
		for(int i = 0;i < names.length;i++)
		{
			if(names[i].equals(type))
				return true;
		}
		return false;
	}
	
	public static boolean TYPE_IS_ANY_OF(String type,String... types)
	{
		for(int i = 0;i < types.length;i++)
		{
			if(types[i].equals(type))
				return true;
		}
		return false;
	}

	

	
	public static boolean FIELD_EQ(Entity instance,String fieldname,Object value)
	{
		return instance.getAttribute(fieldname).equals(value);
	}
}