package com.pagesociety.web.module.user;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;

public interface IUserProfileGuard 
{

	boolean canUpdateProfile(Entity user, Entity user_profile_entity) throws PersistenceException;

	boolean canDeleteUserProfile(Entity user, Entity user_profile) throws PersistenceException;

	boolean canFlagUserProfile(Entity user, Entity user_profile) throws PersistenceException;

	boolean canUnflagProfile(Entity user, Entity user_profile) throws PersistenceException;

	boolean canGetUserProfile(Entity user, Entity profile_user) throws PersistenceException;

	boolean canGetFlaggedUserProfiles(Entity user) throws PersistenceException;

	

}
