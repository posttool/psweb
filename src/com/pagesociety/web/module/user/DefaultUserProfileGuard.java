package com.pagesociety.web.module.user;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.module.PermissionsModule;

public class DefaultUserProfileGuard extends PermissionsModule implements IUserProfileGuard 
{

	public boolean canUpdateProfile(Entity user, Entity user_profile_entity) throws PersistenceException
	{return false;}

	public boolean canDeleteUserProfile(Entity user, Entity user_profile) throws PersistenceException
	{return false;}

	public boolean canFlagUserProfile(Entity user, Entity user_profile) throws PersistenceException
	{return false;}

	public boolean canUnflagProfile(Entity user, Entity user_profile) throws PersistenceException
	{return false;}

	public boolean canGetUserProfile(Entity user, Entity profile_user) throws PersistenceException
	{return false;}

	public boolean canGetFlaggedUserProfiles(Entity user) throws PersistenceException
	{return false;}

	

}
