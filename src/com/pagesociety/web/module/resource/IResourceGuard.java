package com.pagesociety.web.module.resource;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;

public interface IResourceGuard 
{

	boolean canCreateResource(Entity user) throws PersistenceException;
	boolean canUpdateResource(Entity user, Entity update_resource)throws PersistenceException;
	boolean canGetResource(Entity user, Entity resource)throws PersistenceException;
	boolean canGetUploadProgress(Entity user, String channel_name)throws PersistenceException;
	boolean canGetResourceURL(Entity user, Entity resource)throws PersistenceException;
	boolean canDeleteResource(Entity user, Entity resource)throws PersistenceException;
	


}
