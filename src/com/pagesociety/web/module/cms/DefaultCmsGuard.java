package com.pagesociety.web.module.cms;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.module.PermissionsModule;

public class DefaultCmsGuard extends PermissionsModule implements ICmsGuard 
{
	public boolean canGetEntityDefinitions(Entity user)              {return false;}
	public boolean canCreateEntity(Entity user,Entity e)             {return false;}
	public boolean canUpdateEntity(Entity user,Entity e)             {return false;}
	public boolean canDeleteEntity(Entity user,Entity e)             {return false;}
	public boolean canBrowseEntities(Entity user,String entity_type) {return false;}	
	public boolean canGetEntity(Entity user,Entity e) 				 {return false;}
	
}


