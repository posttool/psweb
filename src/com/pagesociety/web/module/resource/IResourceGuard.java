package com.pagesociety.web.module.resource;

import com.pagesociety.persistence.Entity;

public interface IResourceGuard 
{

	boolean canCreateResource(Entity user);
	boolean canUpdateResource(Entity user, Entity update_resource);
	boolean canGetResource(Entity user, Entity resource);
	boolean canGetUploadProgress(Entity user, String channel_name);
	boolean canGetResourceURL(Entity user, Entity resource);
	


}
