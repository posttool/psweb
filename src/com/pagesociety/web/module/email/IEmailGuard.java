package com.pagesociety.web.module.email;

import com.pagesociety.persistence.Entity;

public interface IEmailGuard 
{
	public boolean canSendEmail(Entity user);
}
