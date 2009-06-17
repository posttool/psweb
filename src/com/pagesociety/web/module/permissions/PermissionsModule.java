package com.pagesociety.web.module.permissions;


import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.module.WebStoreModule;
public abstract class PermissionsModule extends WebStoreModule
{
	

	public abstract void definePermission(String namespace,String permission_id);
	public abstract boolean checkPermission(Entity user,String namespace,String permission_id,Map<String,Object> context) throws PersistenceException;
	public abstract void bindPermission(int role,String namespace,String permission,PermissionEvaluator pe) throws PersistenceException;

	/*
	public Object[] flatten_entity(Entity e)
	{
		Set<String> ss 	= e.getAttributes().keySet();
		int 		s  	= ss.size();
		String[] keys 	= ss.toArray(new String[s]);
		Object[] ret 	= new Object[s*2];
		for(int i = 0, j=0;i < s;i++,j+=2)
		{
			String att_name = keys[i];
			ret[j]   = att_name;
			ret[j+1] =  e.getAttribute(att_name);
		}
		return ret;
	}

	public Object[] flatten_entity_dirty_fields(Entity e)
	{
		List<String> dirty_attributes = e.getDirtyAttributes();
		int s = dirty_attributes.size();
		Object[] ret 	= new Object[s*2];
		for(int i = 0, j=0;i < s;i++,j+=2)
		{
			String att_name = dirty_attributes.get(i);
			ret[j]   = att_name;
			ret[j+1] =  e.getAttribute(att_name);
		}
		return ret;
	}
*/
}
