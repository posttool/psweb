package com.pagesociety.web.module.ecommerce;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.module.PermissionsModule;

public class DefaultBillingGuard extends PermissionsModule implements IBillingGuard
{
	public boolean canCreateBillingRecord(Entity creator,Entity user) throws PersistenceException
	{
		return false;
	}
	public boolean canUpdateBillingRecord(Entity editor,Entity user) throws PersistenceException
	{
		return false;
	}
	public boolean canDeleteBillingRecord(Entity deleter,Entity user) throws PersistenceException
	{
		return false; 
	}
	
	public boolean canGetBillingRecords(Entity getter,Entity user) throws PersistenceException
	{
		return false; 
	}
}
