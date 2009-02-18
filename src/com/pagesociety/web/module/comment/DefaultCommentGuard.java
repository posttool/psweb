package com.pagesociety.web.module.comment;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.module.PermissionsModule;

public class DefaultCommentGuard extends PermissionsModule implements ICommentGuard
{

	public boolean canCreateComment(Entity user, Entity target) throws PersistenceException
	{return false; }
	public boolean canUpdateComment(Entity user, Entity comment)throws PersistenceException
	{return false;}
	public boolean canDeleteComment(Entity user,Entity comment) throws PersistenceException
	{return false;}
	public boolean canFlagComment(Entity user, Entity comment) 	throws PersistenceException
	{return false;}
	public boolean canUnflagComment(Entity user, Entity comment)throws PersistenceException
	{return false;}
	public boolean canGetComments(Entity user, Entity target) throws PersistenceException
	{return false;}
	public boolean canGetFlaggedComments(Entity user) throws PersistenceException
	{return false;}

}
