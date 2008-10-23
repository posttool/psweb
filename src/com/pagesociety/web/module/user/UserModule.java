package com.pagesociety.web.module.user;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.AccountLockedException;
import com.pagesociety.web.exception.AuthenticationException;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.LoginFailedException;
import com.pagesociety.web.exception.PermissionsException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.gateway.GatewayConstants;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.util.Util;

public class UserModule extends WebStoreModule 
{
	private static final String PARAM_ADMIN_EMAIL 	 = "admin-email";
	private static final String PARAM_ADMIN_PASSWORD = "admin-password";
	
	public static final int LOCK_UNLOCKED 	 			  			= 0x10;
	public static final int LOCK_LOCKED				  				= 0x20;
	public static final int LOCK_CODE_UNLOCKED						= 0x00;
	public static final int LOCK_CODE_DEFAULT						= 0x01;
	
	private static final String SLOT_USER_GUARD = "user-guard"; 
	

	public static final int EVENT_USER_CREATED	 	 = 0x1001;
	public static final int EVENT_USER_LOGGED_IN  	 = 0x1002;
	public static final int EVENT_USER_LOGGED_OUT 	 = 0x1004;
	public static final int EVENT_USER_DELETED 	 	 = 0x1008;
	
	private IUserGuard guard;

	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		guard		= (IUserGuard)getSlot(SLOT_USER_GUARD);
		
		try{
			setup_admin_user(config);
		}catch(Exception e)
		{
			ERROR(e);
			throw new InitializationException("FAILED SETTING UP USER MODULE.COULDNT CREATE ADMIN USER",e);
		}

	}

	
	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_USER_GUARD,IUserGuard.class,false,DefaultUserGuard.class);
	}
	
	/////////////////BEGIN  M O D U L E   F U N C T I O N S/////////////////////////////////////////
	@Export
	public Entity CreatePrivilegedUser(UserApplicationContext uctx,String email,String password,String username,int role) throws PersistenceException,WebApplicationException
	{
		Entity user = getAuthenticatedUser(uctx);
		GUARD(guard.canCreatePrivilegedUser(user, role));
		
		List<Integer> roles = new ArrayList<Integer>();
		roles.add(role);
		return createPrivilegedUser(user, email, password, username, roles);
	}	
	
	public Entity createPrivilegedUser(Entity creator,String email,String password,String username,List<Integer> roles) throws WebApplicationException,PersistenceException
	{
		Entity existing_user = getUserByEmail(email);
		if(existing_user != null)
			throw new WebApplicationException("USER WITH EMAIL "+email+" ALREADY EXISTS.");
		
		Entity user =  NEW(USER_ENTITY,
					creator,
					FIELD_USERNAME,username,
					FIELD_EMAIL,email,
					FIELD_PASSWORD,password,
					FIELD_USERNAME,
					FIELD_ROLES,roles);					
		
		dispatchEvent(EVENT_USER_CREATED, user);
		return user;
	}
	
	@Export  
	public Entity CreatePublicUser(UserApplicationContext ctx,String email,String password,String username) throws PersistenceException,WebApplicationException
	{
		Entity user = getUser(ctx);
		GUARD(guard.canCreatePublicUser(user));
		return createPublicUser(user, email, password, username);
	}
	
	public Entity createPublicUser(Entity creator,String email,String password,String username) throws PersistenceException,WebApplicationException
	{
		Entity existing_user = getUserByEmail(email);
		if(existing_user != null)
			throw new WebApplicationException("USER WITH EMAIL "+email+" ALREADY EXISTS");

		Entity user =  NEW(USER_ENTITY,
						   creator,
						   FIELD_USERNAME,username,
						   FIELD_EMAIL,email,
						   FIELD_PASSWORD,password);				
		
		dispatchEvent(EVENT_USER_CREATED, user);
		return user;
	}

	@Export
	public Entity UpdateEmail(UserApplicationContext ctx,long user_entity_id,String email) throws WebApplicationException,PersistenceException
	{
		Entity editor    = getAuthenticatedUser(ctx);
		Entity target    = GET(USER_ENTITY,user_entity_id);
		GUARD(guard.canUpdateField(editor,target,FIELD_EMAIL,email));					
		return updateEmail(target, email);
	}
		
	public Entity updateEmail(Entity user,String email) throws WebApplicationException,PersistenceException
	{
		Entity existing_user = getUserByEmail(email);
		if(existing_user != null)
			throw new WebApplicationException("USER WITH EMAIL "+email+" ALREADY EXISTS");
		return UPDATE(user,
				  FIELD_EMAIL,email);					
	}
	
	
	@Export
	public Entity UpdateUserName(UserApplicationContext ctx,long user_entity_id,String username) throws PersistenceException,WebApplicationException
	{
		Entity editor    = getUser(ctx);
		Entity target    = GET(USER_ENTITY,user_entity_id);
		GUARD(guard.canUpdateField(editor,target,FIELD_USERNAME,username));
		return updateUserName(target, username);
	}
	
	public Entity updateUserName(Entity user,String username) throws PersistenceException
	{
		return UPDATE(user,
				  FIELD_USERNAME,username);		
	}
	
	@Export
	public Entity UpdatePassword(UserApplicationContext ctx,long user_entity_id,String old_password,String new_password/*,boolean do_md5_on_server*/) throws PersistenceException,WebApplicationException
	{
		Entity editor    = getUser(ctx);
		Entity target    = GET(USER_ENTITY,user_entity_id);
	
		GUARD(guard.canUpdateField(editor,target,FIELD_PASSWORD,new_password));
		
		if(!target.getAttribute(FIELD_PASSWORD).equals(old_password))
			throw new PermissionsException("BAD OLD PASSWORD");
					
		return updatePassword(target, new_password);
	}
	
	public Entity updatePassword(Entity user,String password) throws PersistenceException
	{
		return UPDATE(user,
				  UserModule.FIELD_PASSWORD,password);				
	}
		
	@Export
	public Entity AddRole(UserApplicationContext ctx,long user_entity_id,int role) throws PersistenceException,WebApplicationException
	{
		Entity editor    = getUser(ctx);
		Entity target    = GET(USER_ENTITY,user_entity_id);
		GUARD(guard.canAddRole(editor,target,role));
		return addRole(target, role);
	}
	
	public Entity addRole(Entity user,int role) throws PersistenceException
	{
		List<Integer> roles = (List<Integer>)user.getAttribute(FIELD_ROLES);
		roles.add(role);
		return UPDATE(user,
				  	  UserModule.FIELD_ROLES,roles);						
	}
	
	@Export
	public Entity RemoveRole(UserApplicationContext ctx,long user_entity_id,int role) throws PersistenceException,WebApplicationException
	{
		Entity editor    = getUser(ctx);
		Entity target    = GET(USER_ENTITY,user_entity_id);
		GUARD(guard.canRemoveRole(editor,target,role));
		return removeRole(target, role);
	}
	
	public Entity removeRole(Entity user,int role) throws PersistenceException
	{
		List<Integer> roles = (List<Integer>)user.getAttribute(FIELD_ROLES);
		roles.remove(role);
		return UPDATE(user,
			  UserModule.FIELD_ROLES,roles);			
	}
	
	@Export
	public Entity LockUser(UserApplicationContext ctx,long user_entity_id,int lock_code,String notes) throws WebApplicationException,PersistenceException
	{
		Entity editor    = getUser(ctx);
		Entity target    = GET(USER_ENTITY,user_entity_id);
		int lock = (Integer)target.getAttribute(FIELD_LOCK);
		if(lock == LOCK_LOCKED)
			return target;
		GUARD(guard.canLockUser(editor,lock_code));
		return lockUser(target, lock_code,notes);

	}
	
	public Entity lockUser(Entity user,int lock_code,String notes) throws PersistenceException
	{
		return UPDATE(user,
				  UserModule.FIELD_LOCK,LOCK_LOCKED,
				  UserModule.FIELD_LOCK_CODE,lock_code,
				  UserModule.FIELD_LOCK_NOTES,notes);
	}
	
	@Export
	public Entity UnlockUser(UserApplicationContext ctx,long user_entity_id) throws WebApplicationException,PersistenceException
	{
		Entity editor     = getUser(ctx);
		Entity target     = GET(USER_ENTITY,user_entity_id);
		
		int lock = (Integer)target.getAttribute(FIELD_LOCK);
		if(lock != LOCK_LOCKED)
			return target;
		int old_lock_code = (Integer)target.getAttribute(FIELD_LOCK_CODE); 
		GUARD(guard.canUnlockUser(editor,old_lock_code));
		return unlockUser(target);
	}
	
	public Entity unlockUser(Entity user) throws PersistenceException
	{
		return UPDATE(user,
				UserModule.FIELD_LOCK,LOCK_UNLOCKED,
				UserModule.FIELD_LOCK_CODE,LOCK_CODE_UNLOCKED,
				UserModule.FIELD_LOCK_NOTES,"");	
	}

		
	@Export
	public Entity Login(UserApplicationContext uctx,String email,String password) throws WebApplicationException,PersistenceException
	{
		Entity user = getUserByEmail(email);
		if(user == null)
			throw new LoginFailedException("LOGIN FAILED");

		if(user.getAttribute(FIELD_PASSWORD).equals(password))
		{
			if(user.getAttribute(FIELD_LOCK).equals(LOCK_LOCKED))
			{
				int lock_code = (Integer)user.getAttribute(FIELD_LOCK_CODE);
				String message = getLockCodeMessage(lock_code);
				if(message == null)
					message = "ACCOUNT IS LOCKED: code"+Integer.toHexString(lock_code); 
				
				throw new AccountLockedException(message);
			}
			uctx.setUser(user);
			dispatchEvent(EVENT_USER_LOGGED_IN, user);
			return UPDATE(user,
					  FIELD_LAST_LOGIN, new Date());
		}
		
		throw new LoginFailedException("LOGIN FAILED");
	}
	
	@Export
	public Entity Logout(UserApplicationContext uctx) throws PersistenceException
	{
		Entity user = getUser(uctx);
		uctx.setUser(null);
		dispatchEvent(EVENT_USER_LOGGED_OUT, user);
		return UPDATE(user,
				  FIELD_LAST_LOGOUT, new Date());
	}

	@Export 
	public Entity DeleteUser(UserApplicationContext uctx,long user_id)throws WebApplicationException,PersistenceException
	{
		Entity editor     = getUser(uctx);
		Entity target     = GET(USER_ENTITY,user_id);

		GUARD(guard.canDeleteUser(editor,target));
		return deleteUser(target);
	}
	
	public Entity deleteUser(Entity user)throws PersistenceException
	{
		long id = user.getId();
		DELETE(user);
		user.setId(id);
		dispatchEvent(EVENT_USER_DELETED, user);
		user.setId(Entity.UNDEFINED);
		return user;
	}
	
	@Export
	public PagingQueryResult GetUsersByRole(UserApplicationContext uctx,int role,int offset,int page_size) throws PersistenceException,WebApplicationException
	{
		Entity user = getUser(uctx);
		GUARD(guard.canGetUsersByRole(user));
		return getUsersByRole(role, offset, page_size);
	}
	
	
	public PagingQueryResult getUsersByRole(int role,int offset,int page_size) throws PersistenceException,WebApplicationException
	{
		Query q = new Query(USER_ENTITY);
		q.idx(INDEX_BY_ROLE);
		q.setContainsAny(q.list(role));
		q.setOffset(offset);
		q.setPageSize(page_size);
		q.orderBy(FIELD_EMAIL);
		return PAGING_QUERY(q);			
	}
	
	@Export
	public PagingQueryResult GetLockedUsers(UserApplicationContext uctx,int role,int offset,int page_size) throws PersistenceException,WebApplicationException
	{
		Entity user = getUser(uctx);
		GUARD(guard.canGetLockedUsers(user));
		return getLockedUsers(offset,page_size);
		
	}
	
	public PagingQueryResult getLockedUsers(int offset,int page_size) throws PersistenceException
	{
		Query q = new Query(USER_ENTITY);
		q.idx(INDEX_BY_LOCK_BY_LOCK_CODE);
		q.eq(q.list(LOCK_LOCKED,Query.VAL_GLOB));
		q.orderBy(FIELD_LAST_MODIFIED, Query.DESC);
		return PAGING_QUERY(q);	
	}
	
	@Export
	public PagingQueryResult GetLockedUsersByLockCode(UserApplicationContext uctx,int lock_code,int offset,int page_size) throws PersistenceException,WebApplicationException
	{
		Entity user = getUser(uctx);
		GUARD(guard.canGetLockedUsers(user));
		return getLockedUsersByLockCode(lock_code,offset,page_size);
	}
	
	public PagingQueryResult getLockedUsersByLockCode(int lock_code,int offset,int page_size) throws PersistenceException
	{
		Query q = new Query(USER_ENTITY);
		q.idx(INDEX_BY_LOCK_BY_LOCK_CODE);
		q.eq(q.list(LOCK_LOCKED,lock_code));
		q.orderBy(FIELD_LAST_MODIFIED, Query.DESC);
		return PAGING_QUERY(q);		
	}
		
	@Export
	public String GetSessionId(UserApplicationContext user_context)
	{
		return GatewayConstants.SESSION_ID_KEY + "=" + user_context.getId();
	}
	
	/////////////////E N D  M O D U L E   F U N C T I O N S/////////////////////////////////////////
	
	//BEGIN UTIL FUNCTIONS//
	
	public boolean isRole(Entity user,int role)
	{
		if(user == null)
			return false;
		List<Integer> roles = (List<Integer>)user.getAttribute(FIELD_ROLES);
		if(roles.contains(role))
			return true;
		return false;
	}

	public boolean isAdmin(Entity user) 
	{	
		return (user != null) && (user.getId() == 1);
	}
	
	public boolean isCreator(Entity user,Entity record) throws PersistenceException
	{
		if(record == null)
			return false;
		
		record = EXPAND(record);
		return (user != null) && (user.equals(record.getAttribute(FIELD_CREATOR)));
	}
	
	public Entity getUser(UserApplicationContext uctx) 
	{
		return (Entity)uctx.getUser();
	}
	


	public Entity getAuthenticatedUser(UserApplicationContext uctx) throws AuthenticationException
	{
		Entity user =  getUser(uctx);
		if(user == null)
			throw new AuthenticationException("USER IS NOT AUTHENTICATED");
		return user;
	}
	
	public boolean isAuthenticated(UserApplicationContext uctx)
	{
		return (getUser(uctx) != null);
	}

	public Entity getUserByEmail(String email) throws PersistenceException,WebApplicationException
	{
		Query q = new Query(USER_ENTITY);
		q.idx(INDEX_BY_EMAIL);
		q.eq(email);
		QueryResult result = QUERY(q);
		
		if(result.size() == 1)
			return result.getEntities().get(0);
		else if(result.size() == 0)
			return null;

		throw new WebApplicationException("MORE THAN ONE USER WITH EMAIL "+email+" EXISTS! FIX DATA.");			
	}
	
	


	private void setup_admin_user(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		Query q = new Query(USER_ENTITY).idx(Query.PRIMARY_IDX).eq(1);
		QueryResult r = QUERY(q);
		if(r.size()==0)
		{
			String admin_email 	  = GET_REQUIRED_CONFIG_PARAM(PARAM_ADMIN_EMAIL, config);
			String admin_password = GET_REQUIRED_CONFIG_PARAM(PARAM_ADMIN_PASSWORD, config);
			LOG("CREATING ADMIN USER - "+admin_email);
			NEW(USER_ENTITY,
				null,
				UserModule.FIELD_EMAIL,admin_email,
				UserModule.FIELD_PASSWORD,Util.stringToHexEncodedMD5(admin_password),
				UserModule.FIELD_LOCK,LOCK_UNLOCKED,
				UserModule.FIELD_LOCK_CODE,LOCK_CODE_DEFAULT);			
		}
	}
	
	private Map<Integer,String> _lock_code_messages = new HashMap<Integer,String>();
	public void registerLockMessage(int lock_code,String message) 
	{
		_lock_code_messages.put(lock_code,message);
	}

	protected String getLockCodeMessage(int lock_code)
	{
		return _lock_code_messages.get(lock_code);
	}
	//END UTIL FUNCTIONS //
	
	//ENTITY BOOTSTRAP STUFF //
	public static String USER_ENTITY 					= 	"User";
	public static String FIELD_EMAIL 					= 	"email";
	public static String FIELD_PASSWORD 				= 	"password";
	public static String FIELD_USERNAME 				= 	"username";
	public static String FIELD_ROLES	 				= 	"roles";
	public static String FIELD_LAST_LOGIN				=   "last_login";
	public static String FIELD_LAST_LOGOUT				=   "last_logout";
	public static String FIELD_LOCK						=   "lock";
	public static String FIELD_LOCK_CODE				=   "lock_code";
	public static String FIELD_LOCK_NOTES				=   "lock_notes";
	

	private List<Integer>DEFAULT_ROLES = new ArrayList<Integer>(); 
	protected void defineEntities(Map<String,Object> config) throws PersistenceException,SyncException
	{
		DEFINE_ENTITY(USER_ENTITY,
					  FIELD_EMAIL, Types.TYPE_STRING,null, 
					  FIELD_PASSWORD, Types.TYPE_STRING,null,
					  FIELD_USERNAME, Types.TYPE_STRING,null,
					  FIELD_ROLES,Types.TYPE_ARRAY|Types.TYPE_INT,DEFAULT_ROLES,
					  FIELD_LAST_LOGIN,Types.TYPE_DATE,null,
					  FIELD_LAST_LOGOUT,Types.TYPE_DATE,null,
					  FIELD_LOCK,Types.TYPE_INT,LOCK_UNLOCKED,
					  FIELD_LOCK_CODE,Types.TYPE_INT,LOCK_CODE_DEFAULT,
					  FIELD_LOCK_NOTES,Types.TYPE_STRING,null);
	}

	public static String INDEX_BY_EMAIL				 	= 	"byEmail";
	public static String INDEX_BY_LOCK_BY_LOCK_CODE		=   "byLockbyLockCode";
	public static String INDEX_BY_ROLE					=   "byRoles";
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,SyncException
	{
		DEFINE_ENTITY_INDEX(USER_ENTITY,INDEX_BY_EMAIL, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, FIELD_EMAIL);
		DEFINE_ENTITY_INDEX(USER_ENTITY,INDEX_BY_LOCK_BY_LOCK_CODE, EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, FIELD_LOCK,FIELD_LOCK_CODE);
		DEFINE_ENTITY_INDEX(USER_ENTITY,INDEX_BY_ROLE, EntityIndex.TYPE_ARRAY_MEMBERSHIP_INDEX, FIELD_ROLES);
	}
}
