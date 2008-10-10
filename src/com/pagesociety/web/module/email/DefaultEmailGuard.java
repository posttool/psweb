package com.pagesociety.web.module.email;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.module.PermissionsModule;

public class DefaultEmailGuard extends PermissionsModule implements IEmailGuard 
{
	public boolean canSendEmail(Entity user)              			 {return false;}
	
}

