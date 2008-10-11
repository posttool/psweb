package com.pagesociety.web.module.user;


import com.pagesociety.persistence.Entity;

public interface IUserGuard 
{

	public boolean canCreatePrivilegedUser(Entity user,int role);
	public boolean canCreatePublicUser(Entity user);
	public boolean canUpdateUser(Entity user,Entity target);
	public boolean canUpdateField(Entity user,Entity target,String fieldname,Object new_value);
	public boolean canLockUser(Entity user,int lock_code);
	public boolean canUnlockUser(Entity user,int old_lock_code);
	public boolean canAddRole(Entity user,Entity target,int role);
	public boolean canRemoveRole(Entity user,Entity target,int role);
	public boolean canGetLockedUsers(Entity user);
	public boolean canGetUsersByRole(Entity user);
	public boolean canDeleteUser(Entity editor, Entity target);


}
