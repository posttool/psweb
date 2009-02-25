package com.pagesociety.web.module.user;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityDefinition;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.IEventListener;
import com.pagesociety.web.module.Module;
import com.pagesociety.web.module.ModuleEvent;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.ecommerce.gateway.BillingGatewayException;

public class UserProfileModule extends WebStoreModule implements IEventListener
{
	protected static final String SLOT_USER_PROFILE_GUARD = "user-profile-guard"; 
	protected static final String SLOT_USER_MODULE 				 = "user-module"; 
	
	/*this module needs to be after modules containing any entites you are referring to 
	as commentable in application.xml. this is just due to the way the app is bootstrapped 
	see the defineEntities routine.*/ 
	private static final String PARAM_PROFILE_FIELDS = "profile-fields";
	

	protected IUserProfileGuard	 	guard;
	protected UserModule user_module;
	
	public static final int EVENT_USER_PROFILE_CREATED    = 0x21;
	public static final int EVENT_USER_PROFILE_UPDATED    = 0x22;
	public static final int EVENT_USER_PROFILE_DELETED    = 0x23;
	public static final int EVENT_USER_PROFILE_FLAGGED    = 0x24;
	public static final int EVENT_USER_PROFILE_UNFLAGGED  = 0x25;
	
	public static final String USER_PROFILE_EVENT_PROFILE 	   		= "profile";
	public static final String USER_PROFILE_EVENT_USER     		    = "user";
	public static final String USER_PROFILE_EVENT_FLAGGING_USER     		    = "flagging_user";
	public static final String USER_PROFILE_EVENT_UNFLAGGING_USER     		    = "unflagging_user";

	protected List<FieldDefinition> profile_fields;

	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);
		guard				 = (IUserProfileGuard)getSlot(SLOT_USER_PROFILE_GUARD);
		user_module 		 = (UserModule)getSlot(SLOT_USER_MODULE);
		user_module.addEventListener(this);
		System.out.println(getName()+"PROFILE FIELDS IS "+profile_fields);
	}
	
	
	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_USER_PROFILE_GUARD,IUserProfileGuard.class,false,DefaultUserProfileGuard.class);
		DEFINE_SLOT(SLOT_USER_MODULE,UserModule.class,true,null);
	}

	//TODO:defineParameters() would be better than just hoping they are there and throwing exception.
	//it would be more interesting with more parameter types as well//
	
	/////////////////BEGIN  M O D U L E   F U N C T I O N S/////////////////////////////////////////
	
	public Entity createUserProfile(Entity creator,Map<String,Object> profile_data) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		
		Entity profile =  NEW(USER_PROFILE_ENTITY,
				   			 creator,
				   			 profile_data);
	
		DISPATCH_EVENT(EVENT_USER_PROFILE_CREATED, 
					   USER_PROFILE_EVENT_PROFILE,profile);

		return profile;
	}
			  
	
	@Export(ParameterNames={"user_profile"})
	public Entity UpdateUserProfile(UserApplicationContext uctx,Entity user_profile) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		VALIDATE_TYPE(USER_PROFILE_ENTITY, user_profile);
		VALIDATE_EXISTING_INSTANCE(user_profile);
		Map<String,Object> atts = new HashMap<String, Object>();
		for(int i = 0;i < profile_fields.size();i++)
		{
			String key = profile_fields.get(i).getName();
			atts.put(key,user_profile.getAttribute(key));
		}
		
		return UpdateUserProfile(uctx,
							 	 user_profile.getId(),
							 	 atts);
	}
	

	
	@Export(ParameterNames={"profile_id","profile_data"})
	public Entity UpdateUserProfile(UserApplicationContext uctx,
								long  user_profile_id,
								Map<String,Object> profile_data) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		Entity user = (Entity)uctx.getUser();
		Entity user_profile_entity = GET(USER_PROFILE_ENTITY,user_profile_id);
		GUARD(guard.canUpdateProfile(user,user_profile_entity));
		
		return updateUserProfile(user_profile_entity,profile_data);
	
	}
	

	
	public Entity updateUserProfile(Entity profile_entity,
			  						Map<String,Object> profile_data) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		System.out.println("PROFILE DATA IS "+profile_data);
		profile_entity = UPDATE(profile_entity,
								profile_data);
	
		DISPATCH_EVENT(EVENT_USER_PROFILE_UPDATED,
					   USER_PROFILE_EVENT_PROFILE,profile_entity);
		return profile_entity;
	}
	
	@Export(ParameterNames={"user_profile"})
	public Entity DeleteUserProfile(UserApplicationContext uctx,Entity user_profile) throws PersistenceException,WebApplicationException
	{
		VALIDATE_TYPE(USER_PROFILE_ENTITY, user_profile);
		return DeleteUserProfile(uctx,user_profile.getId());
	}
	
	@Export(ParameterNames={"comment_id"})
	private Entity DeleteUserProfile(UserApplicationContext uctx, long user_profile_id) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		Entity user_profile = GET(USER_PROFILE_ENTITY,user_profile_id);
		GUARD(guard.canDeleteUserProfile(user,user_profile));
		return deleteUserProfile(user_profile);
	}

	
	private Entity deleteUserProfile(Entity user_profile) throws PersistenceException,WebApplicationException
	{
		long b4_delete_id = user_profile.getId();
		DELETE(user_profile);
		user_profile.setId(b4_delete_id);
		DISPATCH_EVENT(EVENT_USER_PROFILE_DELETED,
					   USER_PROFILE_EVENT_PROFILE,user_profile);
		user_profile.setId(Entity.UNDEFINED);
		return user_profile;
	}
	
	@Export(ParameterNames={"user_profile_id"})
	private Entity FlagComment(UserApplicationContext uctx, long user_profile_id) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		Entity user_profile = GET(USER_PROFILE_ENTITY,user_profile_id);
		if((Integer)user_profile.getAttribute(USER_PROFILE_FIELD_FLAGGED_STATUS) == FLAGGED_STATUS_FLAGGED)
			return user_profile;
		GUARD(guard.canFlagUserProfile(user,user_profile));
		return flagUserProfile(user,user_profile);
	}

	
	private Entity flagUserProfile(Entity flagging_user,Entity user_profile) throws PersistenceException,WebApplicationException
	{
		List<Entity> flagging_users = (List<Entity>)user_profile.getAttribute(USER_PROFILE_FIELD_FLAGGING_USERS);
		flagging_users.add(flagging_user);
		
		user_profile = UPDATE(user_profile,
						USER_PROFILE_FIELD_FLAGGED_STATUS,FLAGGED_STATUS_FLAGGED,		
						USER_PROFILE_FIELD_FLAGGING_USERS,flagging_users);
						
		DISPATCH_EVENT(EVENT_USER_PROFILE_FLAGGED,
					   USER_PROFILE_EVENT_PROFILE,user_profile,
					   USER_PROFILE_EVENT_FLAGGING_USER,flagging_user);
		return user_profile;
	}
	
	@Export(ParameterNames={"user_profile_id"})
	public Entity UnflagComment(UserApplicationContext uctx, long user_profile_id) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		Entity user_profile = GET(USER_PROFILE_ENTITY,user_profile_id);
		if((Integer)user_profile.getAttribute(USER_PROFILE_FIELD_FLAGGED_STATUS) == FLAGGED_STATUS_NOT_FLAGGED)
			return user_profile;
		GUARD(guard.canUnflagProfile(user,user_profile));
		return unflagUserProfile(user,user_profile);
	}

	
	public Entity unflagUserProfile(Entity unflagging_user,Entity user_profile) throws PersistenceException,WebApplicationException
	{
		List<Entity> flagging_users = new ArrayList();
		
		user_profile= UPDATE(user_profile,
						USER_PROFILE_FIELD_FLAGGED_STATUS,FLAGGED_STATUS_FLAGGED,		
						USER_PROFILE_FIELD_FLAGGING_USERS,flagging_users);
						
		DISPATCH_EVENT(EVENT_USER_PROFILE_UNFLAGGED,
					   USER_PROFILE_EVENT_PROFILE,user_profile,
					   USER_PROFILE_EVENT_UNFLAGGING_USER,unflagging_user);
		return user_profile;
	}


	@Export(ParameterNames={"user_id"})
	public Entity GetUserProfile(UserApplicationContext uctx,long user_id) throws WebApplicationException,PersistenceException
	{
		Entity user 		= (Entity)uctx.getUser();
		Entity profile_user = GET(UserModule.USER_ENTITY,user_id);
		GUARD(guard.canGetUserProfile(user,profile_user));		
		return getUserProfile(profile_user);
	}

	public Entity getUserProfile(Entity user) throws PersistenceException,WebApplicationException
	{
		Query q = new Query(USER_PROFILE_ENTITY);
		q.idx(IDX_BY_CREATOR);
		q.eq(user);
		
		QueryResult result = QUERY(q);
		if(result.size() == 0)
			throw new WebApplicationException("NO USER PROFILE FOR USER "+user);
		else
		{
			if(result.size() > 1)
				ERROR(new WebApplicationException("WARNING:  MORE THAN ONE USER PROFILE FOR USER "+user));
			return result.getEntities().get(0);
		}
	}
	
	@Export(ParameterNames={"offset","page_size"})
	public PagingQueryResult GetFlaggedUserProfiles(UserApplicationContext uctx,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(guard.canGetFlaggedUserProfiles(user));		
		//GUARD(guard.canGetBillingRecords(user,user));		
		Query q = getFlaggedUserProfilesQ(FLAGGED_STATUS_FLAGGED);
		q.offset(offset);
		q.pageSize(page_size);
		return PAGING_QUERY_FILL(q,FIELD_CREATOR,USER_PROFILE_FIELD_FLAGGING_USERS);
	}


	public Query getFlaggedUserProfilesQ(int flagged_status)
	{
		Query q = new Query(USER_PROFILE_ENTITY);
		q.idx(IDX_BY_CREATOR);
		q.eq(flagged_status);
		return q;
	}
	
	public void onEvent(Module src, ModuleEvent e) 
	{
		Map<String,Object> default_profile_data = new HashMap<String, Object>();
		try{
			Entity user;
			switch(e.type)
			{
			
			case UserModule.EVENT_USER_CREATED:
				user = (Entity)e.getProperty(UserModule.USER_EVENT_USER);
				createUserProfile(user, default_profile_data);
				break;
			case UserModule.EVENT_USER_DELETED:
				user = (Entity)e.getProperty(UserModule.USER_EVENT_USER);
				deleteUserProfile(getUserProfile(user));
				break;
			}
		}catch(Exception ee)//this should be thrown and handled by user module in this case
		{				   //onEvent should throw Exception
			ERROR(ee);
		}
		
	}	
	
	


	/////////////////E N D  M O D U L E   F U N C T I O N S/////////////////////////////////////////		

	
	public static final String USER_PROFILE_ENTITY 				  = "UserProfile";
	public static String USER_PROFILE_FIELD_FLAGGED_STATUS  = "flagged_status";
	public static String USER_PROFILE_FIELD_FLAGGING_USERS  	  = "flagging_users";
	
	private static final int FLAGGED_STATUS_NOT_FLAGGED = 0x00;
	private static final int FLAGGED_STATUS_FLAGGED 	= 0x10;
	
	protected void defineEntities(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		String[] field_delarations = GET_REQUIRED_LIST_PARAM(PARAM_PROFILE_FIELDS, config);
		profile_fields			   = UNFLATTEN_FIELD_DEFINITIONS(FROM_STRING_LIST,(Object[])field_delarations);
		EntityDefinition d = DEFINE_ENTITY(USER_PROFILE_ENTITY,profile_fields,
				USER_PROFILE_FIELD_FLAGGED_STATUS,Types.TYPE_INT,FLAGGED_STATUS_NOT_FLAGGED,
				USER_PROFILE_FIELD_FLAGGING_USERS,Types.TYPE_REFERENCE|Types.TYPE_ARRAY,UserModule.USER_ENTITY, new ArrayList<Entity>());
	}
	
	public static final String IDX_BY_CREATOR 		 = "byCreator";
	public static final String IDX_BY_FLAGGED_STATUS = "byFlaggedStatus";
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY_INDEXES
		(
			USER_PROFILE_ENTITY,
			ENTITY_INDEX(IDX_BY_CREATOR, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX,FIELD_CREATOR),
			ENTITY_INDEX(IDX_BY_FLAGGED_STATUS, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX,USER_PROFILE_FIELD_FLAGGED_STATUS)
		);
	}

}
