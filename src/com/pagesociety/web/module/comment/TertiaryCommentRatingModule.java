package com.pagesociety.web.module.comment;

import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Types;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.WebModule;

public class TertiaryCommentRatingModule extends WebModule implements ICommentRatingModule
{

	public static final int RATING_NEGATIVE  = -1;
	public static final int RATING_NUETRAL   = 0;
	public static final int RATING_POSITIVE  = 1;
	
	public static final String TERTIARY_RATING_FIELD_RATING 	   = "comment_rating";
	public static final String TARGET_FIELD_RATING_TOTAL_NEGATIVE  = "comment_rating_total_negative";
	public static final String TARGET_FIELD_RATING_TOTAL_NEUTRAL   = "comment_rating_total_nuetral";
	public static final String TARGET_FIELD_RATING_TOTAL_POSITIVE  = "comment_rating_total_positive";
	
	//these ones get added to the comment entity
	public Object[] getCommentRatingFields(String comment_entity_name)throws PersistenceException 
	{
		return new Object[]
		{
				TERTIARY_RATING_FIELD_RATING,Types.TYPE_INT,RATING_NUETRAL
		};
	
	}			

	@Override
	//these ones get added to all the possible targets of the comment entity
	public Object[] getCommentTargetRatingFields(String target_entity)throws PersistenceException 
	{
		return new Object[]
		{
				TARGET_FIELD_RATING_TOTAL_NEGATIVE,Types.TYPE_INT,0,
				TARGET_FIELD_RATING_TOTAL_NEUTRAL,Types.TYPE_INT,0,
				TARGET_FIELD_RATING_TOTAL_POSITIVE,Types.TYPE_INT,0
		};
	}

	//right here you are being passed references to the value fields of all the things being commented on and
	//the values of the comment itself...these are defined by you above in the order defined//
	@Override
	public void onCreateComment(Object[] comment_target_vals,Object[] comment_rating_vals) throws WebApplicationException,PersistenceException
	{
		Integer tertiary_rating = (Integer)comment_rating_vals[0];
		if(tertiary_rating == null)
		{
			WARNING("creating comment with no rating but with rating module active.");
			tertiary_rating = RATING_NUETRAL;
		}
		
		switch(tertiary_rating)
		{
			case RATING_NEGATIVE:
				comment_target_vals[0] = (Integer)comment_target_vals[0] + 1;
				break;
			case RATING_NUETRAL:
				comment_target_vals[1] = (Integer)comment_target_vals[1] + 1;
				break;
			case RATING_POSITIVE:
				comment_target_vals[2] = (Integer)comment_target_vals[2] + 1;
				break;
			default:
				throw new WebApplicationException("UNKNOWN TERTIARY RATING TYPE "+tertiary_rating+"...IGNORING");
		}
		
	}

	//right here you are being passed references to the value fields of all the things being commented on and
	//the values of the comment itself...these are defined by you above in the order defined//
	@Override
	public void onUpdateComment(Object[] comment_target_vals,Object[] old_comment_rating_vals,Object[] comment_rating_vals) throws WebApplicationException,PersistenceException {
		Integer old_tertiary_rating = (Integer)old_comment_rating_vals[0];
		Integer new_tertiary_rating = (Integer)comment_rating_vals[0];
		if(new_tertiary_rating == null)
		{
			WARNING("updating comment with null rating but with rating module active.");
			new_tertiary_rating = old_tertiary_rating;
	
		}
		switch(old_tertiary_rating)
		{
		
			case RATING_NEGATIVE:
				comment_target_vals[0] = (Integer)comment_target_vals[0] - 1;
				break;
			case RATING_NUETRAL:
				comment_target_vals[1] = (Integer)comment_target_vals[1] - 1;
				break;
			case RATING_POSITIVE:
				comment_target_vals[2] = (Integer)comment_target_vals[2] - 1;
				break;
			default:
				throw new WebApplicationException("UNKNOWN TERTIARY RATING TYPE "+old_tertiary_rating+"...IGNORING");
		}
		switch(new_tertiary_rating)
		{
			case RATING_NEGATIVE:
				comment_target_vals[0] = (Integer)comment_target_vals[0] + 1;
				break;
			case RATING_NUETRAL:
				comment_target_vals[1] = (Integer)comment_target_vals[1] + 1;
				break;
			case RATING_POSITIVE:
				comment_target_vals[2] = (Integer)comment_target_vals[2] + 1;
				break;
			default:
				throw new WebApplicationException("UNKNOWN TERTIARY RATING TYPE "+new_tertiary_rating+"...IGNORING");
		}
	}	
	
	//right here you are being passed references to the value fields of all the things being commented on and
	//the values of the comment itself...these are defined by you above in the order defined//
	@Override
	public void onDeleteComment(Object[] comment_target_vals,Object[] comment_rating_vals) throws WebApplicationException,PersistenceException
	{
		Integer tertiary_rating = (Integer)comment_rating_vals[0];
		switch(tertiary_rating)
		{
			case RATING_NEGATIVE:
				comment_target_vals[0] = (Integer)comment_target_vals[0] - 1;
				break;
			case RATING_NUETRAL:
				comment_target_vals[1] = (Integer)comment_target_vals[1] - 1;
				break;
			case RATING_POSITIVE:
				comment_target_vals[2] = (Integer)comment_target_vals[2] - 1;
				break;
			default:
				throw new WebApplicationException("UNKNOWN TERTIARY RATING TYPE "+tertiary_rating+"...IGNORING");
		}
	}

}
