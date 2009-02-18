package com.pagesociety.web.module.comment;

import java.util.List;


import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.exception.WebApplicationException;

public interface ICommentRatingModule 
{
	public Object[] getCommentRatingFields(String comment_entity_name) throws PersistenceException;
	public Object[] getCommentTargetRatingFields(String target_entity) throws PersistenceException;
	public void onCreateComment(Object[] comment_target_vals,Object[] comment_rating_vals) throws WebApplicationException,PersistenceException;
	public void onUpdateComment(Object[] comment_target_vals,Object[] old_comment_rating_vals,Object[]comment_rating_vals) throws WebApplicationException,PersistenceException;
	public void onDeleteComment(Object[] comment_target_vals,Object[] comment_rating_vals) throws WebApplicationException,PersistenceException;

}
