package com.pagesociety.web.module.user;


import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.module.PermissionsModule;

public class DefaultUserGuard extends PermissionsModule implements IUserGuard
{
	public boolean canCreatePrivilegedUser(Entity user,int role)throws PersistenceException
	{return false;}
	public boolean canCreatePublicUser(Entity user)throws PersistenceException
	{return false;}
	public boolean canUpdateUser(Entity user,Entity target)throws PersistenceException
	{return false;}
	public boolean canUpdateField(Entity user,Entity target,String fieldname,Object new_value)throws PersistenceException
	{return false;}
	public boolean canDeleteUser(Entity editor, Entity target)throws PersistenceException
	{return false;}
	public boolean canLockUser(Entity user,int lock_code)throws PersistenceException
	{return false;}
	public boolean canUnlockUser(Entity user,int lock_code)throws PersistenceException
	{return false;}
	public boolean canAddRole(Entity user,Entity target,int role)throws PersistenceException
	{return false;}
	public boolean canRemoveRole(Entity user,Entity target,int role)throws PersistenceException
	{return false;}
	public boolean canGetLockedUsers(Entity user)throws PersistenceException
	{return false;}
	public boolean canGetUsersByRole(Entity user)throws PersistenceException
	{return false;}


}
