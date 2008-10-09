package com.pagesociety.web.module.cms;

import com.pagesociety.persistence.Entity;

public interface ICmsGuard 
{
	public boolean canGetEntityDefinitions(Entity user);
	public boolean canCreateEntity(Entity user,Entity e);
	public boolean canUpdateEntity(Entity user,Entity e);
	public boolean canDeleteEntity(Entity user,Entity e);
	public boolean canBrowseEntities(Entity user,String entity_type);	
	public boolean canGetEntity(Entity user,Entity e);
	
}


