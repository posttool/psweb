package com.pagesociety.web.module.resource;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.module.PermissionsModule;

public class DefaultResourceGuard extends PermissionsModule implements IResourceGuard
{
	public boolean canCreateResource(Entity user) throws PersistenceException
	{return false;}
	public boolean canGetResource(Entity user, Entity resource) throws PersistenceException
	{return false;}
	public boolean canGetResourceURL(Entity user, Entity resource)throws PersistenceException
	{return false;}
	public boolean canGetUploadProgress(Entity user, String channel_name)throws PersistenceException 
	{return false;}
	public boolean canUpdateResource(Entity user, Entity update_resource)throws PersistenceException
	{return false;}
	public boolean canDeleteResource(Entity user, Entity resource)throws PersistenceException 
	{return false;}

}
