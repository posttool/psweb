package com.pagesociety.web.module.comment;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;

public interface ICommentGuard 
{

	public boolean canCreateComment(Entity user, Entity target) throws PersistenceException;
	public boolean canUpdateComment(Entity user, Entity comment)throws PersistenceException;
	public boolean canDeleteComment(Entity user,Entity comment) throws PersistenceException;
	public boolean canFlagComment(Entity user, Entity comment) 	throws PersistenceException;
	public boolean canUnflagComment(Entity user, Entity comment)throws PersistenceException;
	public boolean canGetFlaggedComments(Entity user) throws PersistenceException;
	public boolean canGetComments(Entity user, Entity target) throws PersistenceException;

}
