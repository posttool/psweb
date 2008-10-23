package com.pagesociety.web.module.user;


import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;

public interface IUserGuard 
{
	public boolean canCreatePrivilegedUser(Entity user,int role)throws PersistenceException;
	public boolean canCreatePublicUser(Entity user)throws PersistenceException;
	public boolean canUpdateUser(Entity user,Entity target)throws PersistenceException;
	public boolean canUpdateField(Entity user,Entity target,String fieldname,Object new_value)throws PersistenceException;
	public boolean canLockUser(Entity user,int lock_code)throws PersistenceException;
	public boolean canUnlockUser(Entity user,int old_lock_code)throws PersistenceException;
	public boolean canAddRole(Entity user,Entity target,int role)throws PersistenceException;
	public boolean canRemoveRole(Entity user,Entity target,int role)throws PersistenceException;
	public boolean canGetLockedUsers(Entity user)throws PersistenceException;
	public boolean canGetUsersByRole(Entity user)throws PersistenceException;
	public boolean canDeleteUser(Entity editor, Entity target)throws PersistenceException;


}
