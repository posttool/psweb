package com.pagesociety.web.module.user;


import com.pagesociety.persistence.Entity;
import com.pagesociety.web.module.PermissionsModule;

public class DefaultUserGuard extends PermissionsModule implements IUserGuard
{
	public boolean canCreatePrivilegedUser(Entity user,int role){return false;}
	public boolean canCreatePublicUser(Entity user){return false;}
	public boolean canUpdateUser(Entity user,Entity target){return false;}
	public boolean canUpdateField(Entity user,Entity target,String fieldname,Object new_value){return false;}
	public boolean canLockUser(Entity user,int lock_code){return false;}
	public boolean canUnlockUser(Entity user,int lock_code){return false;}
	public boolean canAddRole(Entity user,Entity target,int role){return false;}
	public boolean canRemoveRole(Entity user,Entity target,int role){return false;}
	public boolean canGetLockedUsers(Entity user) {return false;}
	public boolean canGetUsersByRole(Entity user) {return false;}

}
