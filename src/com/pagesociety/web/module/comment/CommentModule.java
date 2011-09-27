package com.pagesociety.web.module.comment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.util.CALLBACK;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.ecommerce.gateway.BillingGatewayException;
import com.pagesociety.web.module.ecommerce.gateway.IBillingGateway;
import com.pagesociety.web.module.encryption.IEncryptionModule;
import com.pagesociety.web.module.user.UserModule;


public class CommentModule extends WebStoreModule
{

	protected static final String SLOT_COMMENT_RATING_MODULE  	= "comment-rating-module";


	/*this module needs to be after modules containing any entites you are referring to
	as commentable in application.xml. this is just due to the way the app is bootstrapped
	see the defineEntities routine.*/
	private static final String PARAM_COMMENTABLE_ENTITIES 		= "commentable-entities";

	protected ICommentRatingModule 	rating_module;
	protected String[] commentable_entities;

	public static final int EVENT_COMMENT_CREATED    = 0x21;
	public static final int EVENT_COMMENT_UPDATED    = 0x22;
	public static final int EVENT_COMMENT_DELETED    = 0x23;
	public static final int EVENT_COMMENT_FLAGGED    = 0x24;
	public static final int EVENT_COMMENT_UNFLAGGED  = 0x25;
	public static final int EVENT_COMMENT_APPROVED   = 0x30;




	public static final int SYSTEM_ACTION_GET_FLAGGED_COMMENTS 	= 0x1001;
	public static final int SYSTEM_ACTION_FLAG_COMMENT	   		= 0x1002;
	public static final int SYSTEM_ACTION_UNFLAG_COMMENT 	   	= 0x1003;

	public static final String COMMENT_EVENT_COMMENT 	   		= "comment";
	public static final String COMMENT_EVENT_FLAGGING_USER      = "flagging_user";
	public static final String COMMENT_EVENT_UNFLAGGING_USER 	= "unflagging_user";



	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);
		commentable_entities = GET_REQUIRED_LIST_PARAM(PARAM_COMMENTABLE_ENTITIES, config);
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_COMMENT_RATING_MODULE,ICommentRatingModule.class,false,null);
	}

	public static final String CAN_CREATE_COMMENT  				= "CAN_CREATE_COMMENT";
	public static final String CAN_READ_COMMENT    				= "CAN_READ_COMMENT";
	public static final String CAN_UPDATE_COMMENT  				= "CAN_UPDATE_COMMENT";
	public static final String CAN_DELETE_COMMENT  				= "CAN_DELETE_COMMENT";

	public static final String CAN_BROWSE_COMMENTS 				= "CAN_BROWSE_COMMENTS";
	public static final String CAN_BROWSE_FLAGGED_COMMENTS 		= "CAN_BROWSE_FLAGGED_COMMENTS";
	public static final String CAN_FLAG_COMMENT 		   		= "CAN_BROWSE_FLAG_COMMENT";
	public static final String CAN_UNFLAG_COMMENT 		   		= "CAN_BROWSE_UNFLAG_COMMENT";
	public static final String CAN_APPROVE_COMMENT  			= "CAN_APPROVE_SUBMITTED_COMMENT";



	protected void exportPermissions()
	{
		EXPORT_PERMISSION(CAN_CREATE_COMMENT);
		EXPORT_PERMISSION(CAN_READ_COMMENT);
		EXPORT_PERMISSION(CAN_UPDATE_COMMENT);
		EXPORT_PERMISSION(CAN_DELETE_COMMENT);

		EXPORT_PERMISSION(CAN_BROWSE_COMMENTS);
		EXPORT_PERMISSION(CAN_BROWSE_FLAGGED_COMMENTS);
		EXPORT_PERMISSION(CAN_FLAG_COMMENT);
		EXPORT_PERMISSION(CAN_UNFLAG_COMMENT);
		EXPORT_PERMISSION(CAN_APPROVE_COMMENT);
	}

	/////////////////BEGIN  M O D U L E   F U N C T I O N S/////////////////////////////////////////

	@Export(ParameterNames={"comment","rating_data"})
	public Entity CreateComment(UserApplicationContext uctx,Entity comment,Map<String,Object> rating_data) throws WebApplicationException,PersistenceException,BillingGatewayException
	{

		VALIDATE_TYPE(COMMENT_ENTITY, comment);
		VALIDATE_NEW_INSTANCE(comment);

		Entity comment_target = (Entity)comment.getAttribute(COMMENT_FIELD_TARGET);
		String comment_target_type = null;
		long   comment_target_id = Entity.UNDEFINED;
		if(comment_target != null)
		{
			comment_target_type = comment_target.getType();
			comment_target_id = comment_target.getId();

		}
		else
		{
			throw new WebApplicationException("CANT PROVIDE A NULL TARGET FOR A COMMENT.");
		}

		return CreateComment(uctx,
							(String)comment.getAttribute(COMMENT_FIELD_TITLE),
							(String)comment.getAttribute(COMMENT_FIELD_COMMENT),
							comment_target_type,
							comment_target_id,
							rating_data);
	}



	@Export(ParameterNames={"title","comment","target_type","target_id","rating_data"})
	public Entity CreateComment(UserApplicationContext uctx,
									  String title,
									  String comment,
									  String target_type,
									  long target_id,
									  Map<String,Object> rating_data) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		Entity user = (Entity)uctx.getUser();
		Entity target = GET(target_type,target_id);
		if(!isCommentableEntity(target))
		{
			throw new WebApplicationException("CANT CREATE COMMENT ON "+target.getType()+" IT IS NOT LISTED AS A COMMENTABLE ENTITY");
		}

		GUARD(user,CAN_CREATE_COMMENT, COMMENT_FIELD_TITLE,title,
									   COMMENT_FIELD_COMMENT,comment,
									   COMMENT_FIELD_TARGET,target);

		return createComment(user,title,comment,target,rating_data,FLAGGED_STATUS_NOT_FLAGGED);

	}

	@Export(ParameterNames={"title","comment","target_type","target_id","rating_data"})
	public Entity CreateUnapprovedComment(UserApplicationContext uctx,
									  String title,
									  String comment,
									  String target_type,
									  long target_id,
									  Map<String,Object> rating_data) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		Entity user = (Entity)uctx.getUser();
		Entity target = GET(target_type,target_id);
		if(!isCommentableEntity(target))
		{
			throw new WebApplicationException("CANT CREATE COMMENT ON "+target.getType()+" IT IS NOT LISTED AS A COMMENTABLE ENTITY");
		}

		GUARD(user,CAN_CREATE_COMMENT, COMMENT_FIELD_TITLE,title,
									   COMMENT_FIELD_COMMENT,comment,
									   COMMENT_FIELD_TARGET,target);

		return createComment(user,title,comment,target,rating_data,FLAGGED_STATUS_UNAPPROVED);

	}

	public boolean isCommentableEntity(Entity e)
	{
		String type = e.getType();
		for(int i = 0;i < commentable_entities.length;i++)
		{
			if(commentable_entities[i].equals(type))
				return true;
		}
		return false;
	}
	public Entity createComment(Entity creator,String title,String comment,Entity target,Map<String,Object> rating_data) throws WebApplicationException,PersistenceException
	{
		return createComment(creator, title, comment, target, rating_data, FLAGGED_STATUS_NOT_FLAGGED);
	}

	public Entity createComment(Entity creator,String title,String comment,Entity target,Map<String,Object> rating_data,int initial_status) throws WebApplicationException,PersistenceException
	{

		Entity c =  NEW(COMMENT_ENTITY,
				   		creator,
				   		COMMENT_FIELD_TITLE,title,
				   		COMMENT_FIELD_COMMENT,comment,
				   		COMMENT_FIELD_TARGET,target,
				   		COMMENT_FIELD_FLAGGED_STATUS,initial_status);

		if(rating_module != null)
		{
			Object[] comment_rating_vals = new Object[additional_comment_rating_fields.size()];
			for(int i = 0;i < additional_comment_rating_fields.size();i++)
				comment_rating_vals[i] = rating_data.get(additional_comment_rating_fields.get(i).getName());

			Object[] target_comment_rating_vals = new Object[additional_comment_target_rating_fields.size()];
			for(int i = 0;i < additional_comment_target_rating_fields.size();i++)
				target_comment_rating_vals[i] = target.getAttribute(additional_comment_target_rating_fields.get(i).getName());

			rating_module.onCreateComment(target_comment_rating_vals, comment_rating_vals);

			for(int i = 0;i < additional_comment_rating_fields.size();i++)
				c.setAttribute(additional_comment_rating_fields.get(i).getName(), comment_rating_vals[i]);

			for(int i = 0;i < additional_comment_target_rating_fields.size();i++)
				target.setAttribute(additional_comment_target_rating_fields.get(i).getName(), target_comment_rating_vals[i]);

			SAVE_ENTITY(c);
			SAVE_ENTITY(target);
		}

		DISPATCH_EVENT(EVENT_COMMENT_CREATED,
					   COMMENT_EVENT_COMMENT,c);

		return c;
	}




	@Export(ParameterNames={"comment","rating_data"})
	public Entity UpdateComment(UserApplicationContext uctx,Entity comment,Map<String,Object> rating_data) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		VALIDATE_TYPE(COMMENT_ENTITY, comment);
		VALIDATE_EXISTING_INSTANCE(comment);
		return UpdateComment(uctx,
							 comment.getId(),
							(String)comment.getAttribute(COMMENT_FIELD_TITLE),
							(String)comment.getAttribute(COMMENT_FIELD_COMMENT),
							rating_data);
	}



	@Export(ParameterNames={"comment_id","title","comment","rating_data"})
	public Entity UpdateComment(UserApplicationContext uctx,
								long  comment_id,
								String title,
								String comment,
								Map<String,Object> rating_data) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		Entity user = (Entity)uctx.getUser();
		Entity comment_entity = GET(COMMENT_ENTITY,comment_id);
//TODO FIX E!@
//		GUARD(user,CAN_UPDATE_COMMENT,user, GUARD_INSTANCE,comment_entity,
//											COMMENT_FIELD_TITLE,title,
//											COMMENT_FIELD_COMMENT,comment);

		return updateComment(comment_entity,title,comment,rating_data);

	}



	public Entity updateComment(Entity comment_entity,
			  					String title,
			  					String comment,
			  					Map<String,Object> rating_data) throws WebApplicationException,PersistenceException,BillingGatewayException
	{


		if(rating_module != null)
		{
			Entity comment_entity_before_update = GET(COMMENT_ENTITY,comment_entity.getId());
			Entity target = EXPAND((Entity)comment_entity.getAttribute(COMMENT_FIELD_TARGET));
			Object[] old_comment_rating_vals = new Object[additional_comment_rating_fields.size()];
			for(int i = 0;i < additional_comment_rating_fields.size();i++)
				old_comment_rating_vals[i] = comment_entity_before_update.getAttribute(additional_comment_rating_fields.get(i).getName());

			Object[] comment_rating_vals = new Object[additional_comment_rating_fields.size()];
			for(int i = 0;i < additional_comment_rating_fields.size();i++)
				comment_rating_vals[i] = rating_data.get(additional_comment_rating_fields.get(i).getName());

			Object[] target_comment_rating_vals = new Object[additional_comment_target_rating_fields.size()];
			for(int i = 0;i < additional_comment_target_rating_fields.size();i++)
				target_comment_rating_vals[i] = target.getAttribute(additional_comment_target_rating_fields.get(i).getName());

			rating_module.onUpdateComment(target_comment_rating_vals,old_comment_rating_vals,comment_rating_vals);

			for(int i = 0;i < additional_comment_rating_fields.size();i++)
				comment_entity.setAttribute(additional_comment_rating_fields.get(i).getName(), comment_rating_vals[i]);

			for(int i = 0;i < additional_comment_target_rating_fields.size();i++)
				target.setAttribute(additional_comment_target_rating_fields.get(i).getName(), target_comment_rating_vals[i]);

			SAVE_ENTITY(target);
		}
		comment_entity = UPDATE(comment_entity,
								COMMENT_FIELD_TITLE,title,
								COMMENT_FIELD_COMMENT,comment);



		DISPATCH_EVENT(EVENT_COMMENT_UPDATED,
					   COMMENT_EVENT_COMMENT,comment_entity);
		return comment_entity;
	}

	@Export(ParameterNames={"comment"})
	public Entity DeleteComment(UserApplicationContext uctx,Entity comment) throws PersistenceException,WebApplicationException
	{
		VALIDATE_TYPE(COMMENT_ENTITY, comment);
		return DeleteComment(uctx,comment.getId());
	}

	@Export(ParameterNames={"comment_id"})
	public Entity DeleteComment(UserApplicationContext uctx, long comment_id) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		Entity comment = GET(COMMENT_ENTITY,comment_id);
		GUARD(user,CAN_DELETE_COMMENT,GUARD_INSTANCE,comment);
		return deleteComment(comment);
	}


	private Entity deleteComment(Entity comment) throws PersistenceException,WebApplicationException
	{
		if(rating_module != null)
		{
			Entity target = EXPAND((Entity)comment.getAttribute(COMMENT_FIELD_TARGET));
			Object[] comment_rating_vals = new Object[additional_comment_rating_fields.size()];
			for(int i = 0;i < additional_comment_rating_fields.size();i++)
				comment_rating_vals[i] = comment.getAttribute(additional_comment_rating_fields.get(i).getName());

			Object[] target_comment_rating_vals = new Object[additional_comment_target_rating_fields.size()];
			for(int i = 0;i < additional_comment_target_rating_fields.size();i++)
				target_comment_rating_vals[i] = target.getAttribute(additional_comment_target_rating_fields.get(i).getName());

			rating_module.onDeleteComment(target_comment_rating_vals, comment_rating_vals);

			for(int i = 0;i < additional_comment_target_rating_fields.size();i++)
				target.setAttribute(additional_comment_target_rating_fields.get(i).getName(), target_comment_rating_vals[i]);

			SAVE_ENTITY(target);
		}
		long b4_delete_id = comment.getId();
		DELETE(comment);
		comment.setId(b4_delete_id);
		DISPATCH_EVENT(EVENT_COMMENT_DELETED,
					   COMMENT_EVENT_COMMENT,comment);
		return comment;
	}

	@Export(ParameterNames={"comment_id"})
	public Entity FlagComment(UserApplicationContext uctx, long comment_id) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		Entity comment = GET(COMMENT_ENTITY,comment_id);
		if((Integer)comment.getAttribute(COMMENT_FIELD_FLAGGED_STATUS) == FLAGGED_STATUS_FLAGGED)
			return comment;
		GUARD(user, CAN_FLAG_COMMENT,GUARD_INSTANCE,comment);
		return flagComment(user,comment);
	}


	public Entity flagComment(Entity flagging_user,Entity comment) throws PersistenceException,WebApplicationException
	{
		List<Entity> flagging_users = (List<Entity>)comment.getAttribute(COMMENT_FIELD_FLAGGING_USERS);
		flagging_users.add(flagging_user);

		comment = UPDATE(comment,
						COMMENT_FIELD_FLAGGED_STATUS,FLAGGED_STATUS_FLAGGED,
						COMMENT_FIELD_FLAGGING_USERS,flagging_users);

		DISPATCH_EVENT(EVENT_COMMENT_FLAGGED,
					   COMMENT_EVENT_COMMENT,comment,
					   COMMENT_EVENT_FLAGGING_USER,flagging_user);
		return comment;
	}

	@Export(ParameterNames={"comment_id"})
	public Entity ApproveComment(UserApplicationContext uctx, long comment_id) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		Entity comment = GET(COMMENT_ENTITY,comment_id);
		if((Integer)comment.getAttribute(COMMENT_FIELD_FLAGGED_STATUS) == FLAGGED_STATUS_APPROVED)
			return comment;
		GUARD(user, CAN_APPROVE_COMMENT,GUARD_INSTANCE,comment);
		return approveComment(user,comment);
	}


	public Entity approveComment(Entity approving_user,Entity comment) throws PersistenceException,WebApplicationException
	{

		comment = UPDATE(comment,
						COMMENT_FIELD_FLAGGED_STATUS,FLAGGED_STATUS_APPROVED);

		DISPATCH_EVENT(EVENT_COMMENT_APPROVED,
					   COMMENT_EVENT_COMMENT,comment);
		return comment;
	}


	@Export(ParameterNames={"comment_id"})
	public Entity UnapproveComment(UserApplicationContext uctx, long comment_id) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		Entity comment = GET(COMMENT_ENTITY,comment_id);
		if((Integer)comment.getAttribute(COMMENT_FIELD_FLAGGED_STATUS) == FLAGGED_STATUS_UNAPPROVED)
			return comment;
		GUARD(user, CAN_APPROVE_COMMENT,GUARD_INSTANCE,comment);
		return unapproveComment(user,comment);
	}


	public Entity unapproveComment(Entity approving_user,Entity comment) throws PersistenceException,WebApplicationException
	{

		comment = UPDATE(comment,
						COMMENT_FIELD_FLAGGED_STATUS,FLAGGED_STATUS_UNAPPROVED);

		return comment;
	}



	@Export(ParameterNames={"comment_id"})
	public Entity UnflagComment(UserApplicationContext uctx, long comment_id) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		Entity comment = GET(COMMENT_ENTITY,comment_id);
		if((Integer)comment.getAttribute(COMMENT_FIELD_FLAGGED_STATUS) == FLAGGED_STATUS_NOT_FLAGGED)
			return comment;
		GUARD(user,CAN_UNFLAG_COMMENT,GUARD_INSTANCE,comment);
		return unflagComment(user,comment);
	}


	public Entity unflagComment(Entity unflagging_user,Entity comment) throws PersistenceException,WebApplicationException
	{
		List<Entity> flagging_users = new ArrayList();

		comment = UPDATE(comment,
						COMMENT_FIELD_FLAGGED_STATUS,FLAGGED_STATUS_FLAGGED,
						COMMENT_FIELD_FLAGGING_USERS,flagging_users);

		DISPATCH_EVENT(EVENT_COMMENT_UNFLAGGED,
					   COMMENT_EVENT_COMMENT,comment,
					   COMMENT_EVENT_UNFLAGGING_USER,unflagging_user);
		return comment;
	}


	@Export(ParameterNames={"entity_type","entity_id","offset","page_size"})
	public PagingQueryResult GetAllComments(UserApplicationContext uctx,String entity_type,long entity_id,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity target = GET(entity_type,entity_id);

		Query q = getCommentsForTargetQ(target);
		q.offset(offset);
		q.pageSize(page_size);
		GUARD(user,CAN_BROWSE_COMMENTS);
		PagingQueryResult result =  PAGING_QUERY(q);
		return result;
	}

	@Export(ParameterNames={"offset","page_size"})
	public PagingQueryResult GetUnflaggedComments(UserApplicationContext uctx,String entity_type,long entity_id,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity target = GET(entity_type,entity_id);

		Query q = getCommentsForTargetWithStatusQ(target, FLAGGED_STATUS_NOT_FLAGGED);
		q.offset(offset);
		q.pageSize(page_size);
		GUARD(user,CAN_BROWSE_COMMENTS);
		PagingQueryResult result =  PAGING_QUERY(q);
		return result;
	}

	@Export(ParameterNames={"offset","page_size"})
	public PagingQueryResult GetUnapprovedComments(UserApplicationContext uctx,String entity_type,long entity_id,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity target = GET(entity_type,entity_id);

		Query q = getCommentsForTargetWithStatusQ(target, FLAGGED_STATUS_UNAPPROVED);
		q.offset(offset);
		q.pageSize(page_size);
		GUARD(user,CAN_BROWSE_COMMENTS);
		PagingQueryResult result =  PAGING_QUERY(q);
		return result;
	}

	@Export(ParameterNames={"offset","page_size"})
	public PagingQueryResult GetApprovedComments(UserApplicationContext uctx,String entity_type,long entity_id,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity target = GET(entity_type,entity_id);

		Query q = getCommentsForTargetWithStatusQ(target, FLAGGED_STATUS_APPROVED);
		q.offset(offset);
		q.pageSize(page_size);
		GUARD(user,CAN_BROWSE_COMMENTS);
		PagingQueryResult result =  PAGING_QUERY(q);
		return result;
	}


	public PagingQueryResult getAllUnapprovedComments(int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Query q = getAllCommentsByStatusQ(FLAGGED_STATUS_UNAPPROVED);
		q.offset(offset);
		q.pageSize(page_size);
		PagingQueryResult result =  PAGING_QUERY(q);
		return result;
	}

	public void unapproveAllComments() throws WebApplicationException,PersistenceException
	{

		PAGE_APPLY(COMMENT_ENTITY, new CALLBACK(){

			public Object exec(Object... args) throws Exception
			{
				Entity comment = (Entity)args[0];
				if((Integer)comment.getAttribute(COMMENT_FIELD_FLAGGED_STATUS) != FLAGGED_STATUS_UNAPPROVED)
				{
					UPDATE(comment,
							COMMENT_FIELD_FLAGGED_STATUS,FLAGGED_STATUS_UNAPPROVED);
				}
				return CALLBACK_VOID;
			}
		});

	}

	public PagingQueryResult getAllApprovedComments(int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Query q = getAllCommentsByStatusQ(FLAGGED_STATUS_APPROVED);
		q.offset(offset);
		q.pageSize(page_size);
		PagingQueryResult result =  PAGING_QUERY(q);
		return result;
	}

	public PagingQueryResult getAllComments(int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Query q = getAllCommentsByStatusQ(Query.VAL_GLOB);
		q.offset(offset);
		q.pageSize(page_size);
		PagingQueryResult result =  PAGING_QUERY(q);
		return result;
	}
	@Export(ParameterNames={"offset","page_size"})
	public PagingQueryResult GetFlaggedComments(UserApplicationContext uctx,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(user,CAN_BROWSE_FLAGGED_COMMENTS);
		//GUARD(guard.canGetBillingRecords(user,user));
		Query q = getFlaggedCommentsQ(FLAGGED_STATUS_FLAGGED);
		q.offset(offset);
		q.pageSize(page_size);
		return PAGING_QUERY_FILL(q,COMMENT_FIELD_TARGET);
	}







	public Query getCommentsForTargetQ(Object target)
	{
		Query q = new Query(COMMENT_ENTITY);
		q.idx(IDX_BY_TARGET);
		q.eq(target);
		return q;
	}

	public Query getCommentsForTargetWithStatusQ(Object target,int flagged_status)
	{
		Query q = new Query(COMMENT_ENTITY);
		q.idx(IDX_BY_TARGET_BY_FLAGGED_STATUS);
		q.eq(q.list(target,flagged_status));
		return q;
	}

	public Query getUnflaggedCommentsForTargetQ(Object target)
	{
		Query q = new Query(COMMENT_ENTITY);
		q.idx(IDX_BY_TARGET);
		q.eq(target);
		return q;
	}

	public Query getAllCommentsByStatusQ(Object status)
	{
		Query q = new Query(COMMENT_ENTITY);
		q.idx(IDX_BY_FLAGGED_STATUS);
		q.eq(status);
		return q;
	}


	public Query getFlaggedCommentsQ(int flagged_status)
	{
		Query q = new Query(COMMENT_ENTITY);
		q.idx(IDX_BY_FLAGGED_STATUS);
		q.eq(flagged_status);
		return q;
	}





	/////////////////E N D  M O D U L E   F U N C T I O N S/////////////////////////////////////////


	public static final int FLAGGED_STATUS_NOT_FLAGGED = 0x00;
	public static final int FLAGGED_STATUS_FLAGGED 	= 0x10;

	public static final int FLAGGED_STATUS_UNAPPROVED   = 0x20;
	public static final int FLAGGED_STATUS_APPROVED    = 0x30;

	public static String COMMENT_ENTITY 	  		   = "Comment";
	public static String COMMENT_FIELD_TITLE  		   = "title";
	public static String COMMENT_FIELD_COMMENT  	   = "comment";
	public static String COMMENT_FIELD_TARGET		   = "target";
	public static String COMMENT_FIELD_FLAGGED_STATUS  = "flagged_status";
	public static String COMMENT_FIELD_FLAGGING_USERS  = "flagging_users";

	protected List<FieldDefinition> additional_comment_target_rating_fields = null;
	protected List<FieldDefinition> additional_comment_rating_fields 	    = null;
	protected void defineEntities(Map<String,Object> config) throws PersistenceException,InitializationException
	{

		DEFINE_ENTITY(COMMENT_ENTITY,
			COMMENT_FIELD_TITLE,Types.TYPE_STRING,"",
			COMMENT_FIELD_COMMENT,Types.TYPE_STRING,"",
			COMMENT_FIELD_TARGET,Types.TYPE_REFERENCE,FieldDefinition.REF_TYPE_UNTYPED_ENTITY,null,
			COMMENT_FIELD_FLAGGED_STATUS,Types.TYPE_INT,FLAGGED_STATUS_NOT_FLAGGED,
			COMMENT_FIELD_FLAGGING_USERS,Types.TYPE_REFERENCE|Types.TYPE_ARRAY,UserModule.USER_ENTITY, new ArrayList<Entity>());


		rating_module	= (ICommentRatingModule)getSlot(SLOT_COMMENT_RATING_MODULE);
		if(rating_module != null)
		{
			//add fields to ourself related to rating subsystem
			Object[] comment_rating_field_descriptions = rating_module.getCommentRatingFields(COMMENT_ENTITY);
			ADD_FIELDS(COMMENT_ENTITY,comment_rating_field_descriptions);
			additional_comment_rating_fields = UNFLATTEN_FIELD_DEFINITIONS(comment_rating_field_descriptions);

			//add fields to targets related to rating subsystem
			commentable_entities = GET_REQUIRED_LIST_PARAM(PARAM_COMMENTABLE_ENTITIES, config);
			additional_comment_target_rating_fields = UNFLATTEN_FIELD_DEFINITIONS(rating_module.getCommentTargetRatingFields(null));
			for(int i = 0;i < commentable_entities.length;i++)
				ADD_FIELDS(commentable_entities[i],rating_module.getCommentTargetRatingFields(commentable_entities[i]));
		}
	}



	public static final String IDX_BY_FLAGGED_STATUS 				 = "byFlaggedStatus";
	public static final String IDX_BY_TARGET 		 				 = "byTarget";
	public static final String IDX_BY_TARGET_BY_FLAGGED_STATUS 		 = "byTargetByFlaggedStatus";
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY_INDICES
		(
			COMMENT_ENTITY,
			ENTITY_INDEX(IDX_BY_FLAGGED_STATUS, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX,COMMENT_FIELD_FLAGGED_STATUS),
			ENTITY_INDEX(IDX_BY_TARGET, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX,COMMENT_FIELD_TARGET),
			ENTITY_INDEX(IDX_BY_TARGET_BY_FLAGGED_STATUS, EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX,COMMENT_FIELD_TARGET,COMMENT_FIELD_FLAGGED_STATUS)
		);
	}


	/////BOOTSTRAPPY STUFF//
	//system init happens first.all modules are linked and store is set for
	//webstore modules. nothing elise is ready but here we are interceding to
	//teel evolution to ignore the fields that our rating plugin will be responsible for.
	public void system_init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.system_init(app,config);
		addModuleAttribute(ATTRIBUTE_INIT_LATE);
		notify_evolution(app,config);
	}

	private void notify_evolution(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		rating_module		 = (ICommentRatingModule)getSlot(SLOT_COMMENT_RATING_MODULE);


	}


	//implements SiteManagerUserActions//

	public OBJECT getGlobalActions(Entity user)
	{
		return null;
	}

	public OBJECT getUserPermissionsForEntityType(Entity user,String entity_type)
	{
		return null;
	}

	public OBJECT getUserPermissionsForEntityInstance(Entity user, Entity e)
	{

		return null;
	}


}
