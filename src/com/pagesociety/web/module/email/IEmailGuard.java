package com.pagesociety.web.module.email;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;

public interface IEmailGuard 
{
	public boolean canSendEmail(Entity user) throws PersistenceException;
}
