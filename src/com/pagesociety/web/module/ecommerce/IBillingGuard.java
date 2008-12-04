package com.pagesociety.web.module.ecommerce;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;

public interface IBillingGuard 
{
	public boolean canCreateBillingRecord(Entity user,Entity user2) throws PersistenceException;
	public boolean canUpdateBillingRecord(Entity user,Entity user2) throws PersistenceException;
	public boolean canDeleteBillingRecord(Entity user,Entity user2) throws PersistenceException;
	public boolean canGetBillingRecords(Entity user,Entity user2) throws PersistenceException;

}
