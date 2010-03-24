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
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.LoginFailedException;
import com.pagesociety.web.exception.PermissionsException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.gateway.GatewayConstants;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.TransactionProtect;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.permissions.PermissionEvaluator;
import com.pagesociety.web.module.permissions.PermissionsModule;
import com.pagesociety.web.module.util.Util;
import com.pagesociety.web.module.util.Validator;


public class UserModule extends WebStoreModule 
{
	private static final String PARAM_ENFORCE_UNIQUE_USERNAME = "enforce-unique-username";
	
	public static final int LOCK_UNLOCKED 	 			  			= 0x10;
	public static final int LOCK_LOCKED				  				= 0x20;
	public static final int LOCK_CODE_UNLOCKED						= 0x00;
	public static final int LOCK_CODE_DEFAULT						= 0x01;
	
	private static final String SLOT_USER_GUARD = "user-guard"; 
	

	public static final int EVENT_USER_CREATED	 	 = 0x1001;
	public static final int EVENT_USER_LOGGED_IN  	 = 0x1002;
	public static final int EVENT_USER_LOGGED_OUT 	 = 0x1004;
	public static final int EVENT_USER_DELETED 	 	 = 0x1008;
	public static final int EVENT_USER_ROLES_UPDATED = 0x1010;
	
	public static final String USER_EVENT_USER = "user";
	public static final String USER_EVENT_USER_CONTEXT = "user-context";
	
	public static final int USER_ROLE_WHEEL 				 = 0x1000;
	public static final int USER_ROLE_SYSTEM_USER	 		 = 0x0001;

	private boolean enforce_unique_username = false;
	

	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		String euu = GET_OPTIONAL_CONFIG_PARAM(PARAM_ENFORCE_UNIQUE_USERNAME, config);
		if(euu != null && !euu.equals("false"))
			enforce_unique_username = true;
	}
	
	public void loadbang(WebApplication app, Map<String,Object> config) throws InitializationException
	{
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
	}

	public static final String CAN_CREATE_PRIVILEGED_USER  = "CAN_CREATE_PRIVILEGED_USER";
	public static final String CAN_CREATE_PUBLIC_USER 	   = "CAN_CREATE_PUBLIC_USER";
	public static final String CAN_EDIT_USER 	  		   = "CAN_EDIT_USER";
	public static final String CAN_DELETE_USER 	  	   	   = "CAN_DELETE_USER";
	public static final String CAN_ADD_ROLE 	  		   = "CAN_ADD_ROLE";
	public static final String CAN_REMOVE_ROLE 	  	   	   = "CAN_REMOVE_ROLE";
	public static final String CAN_LOCK_USER	  	  	   = "CAN_LOCK_USER";
	public static final String CAN_UNLOCK_USER	  	   	   = "CAN_UNLOCK_USER";
	public static final String CAN_BROWSE_USERS_BY_ROLE    = "CAN_BROWSE_USERS_BY_ROLE";
	public static final String CAN_BROWSE_LOCKED_USERS     = "CAN_BROWSE_LOCKED_USERS";
	
	protected void exportPermissions()
	{
		EXPORT_PERMISSION(CAN_CREATE_PRIVILEGED_USER);
		EXPORT_PERMISSION(CAN_CREATE_PUBLIC_USER);
		EXPORT_PERMISSION(CAN_EDIT_USER);
		EXPORT_PERMISSION(CAN_DELETE_USER);
		EXPORT_PERMISSION(CAN_ADD_ROLE);
		EXPORT_PERMISSION(CAN_REMOVE_ROLE);
		EXPORT_PERMISSION(CAN_LOCK_USER);
		EXPORT_PERMISSION(CAN_UNLOCK_USER);
		EXPORT_PERMISSION(CAN_BROWSE_USERS_BY_ROLE);
		EXPORT_PERMISSION(CAN_BROWSE_LOCKED_USERS);
	}
	
	/////////////////BEGIN  M O D U L E   F U N C T I O N S/////////////////////////////////////////
	
	@TransactionProtect
	@Export(ParameterNames={"email","password","username","role"})
	public Entity CreatePrivilegedUser(UserApplicationContext uctx,String email,String password,String username,int role) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(user, CAN_CREATE_PRIVILEGED_USER, "role",role);
		
		List<Integer> roles = new ArrayList<Integer>();
		roles.add(role);
		return createPrivilegedUser(user, email, password, username, roles);
	}	
	
	public Entity createPrivilegedUser(Entity creator,String email,String password,String username,List<Integer> roles,Object... xtra_event_context_params) throws WebApplicationException,PersistenceException
	{
		Entity existing_user = getUserByEmail(email);
		if(existing_user != null)
			throw new WebApplicationException("USER WITH EMAIL "+email+" ALREADY EXISTS.",ERROR_USER_EMAIL_EXISTS);
		
		username = STRIP_TO_ALPHA_NUMERIC(username);
		if(username != null && username.trim().equals(""))
			username = null;
					
		if(enforce_unique_username && username != null)
		{
			existing_user = getUserByUsernameAndPassword(username, Query.VAL_GLOB);
			if(existing_user != null)
				throw new WebApplicationException("USER WITH USERNAME "+username+" ALREADY EXISTS.",ERROR_USER_USERNAME_EXISTS);
		}
		
		//System.out.println("USERNAME IS "+username);
		//System.out.println("PASSWORD IS "+password);
		//NOTE: this is only if we want people to be able to log in by username//
		//if(getUserByUsernameAndPassword(username,password) != null)
		//	throw new WebApplicationException("BAD PASSWORD",ERROR_BAD_PASSWORD);
		
		Entity user =  NEW(USER_ENTITY,
					creator,
					FIELD_USERNAME,username,
					FIELD_EMAIL,email,
					FIELD_PASSWORD,password,
					FIELD_USERNAME,username,
					FIELD_ROLES,roles);					
	
		Map<String,Object> event_context = new HashMap<String, Object>();
		event_context.put(USER_EVENT_USER, user);
		for(int i = 0;i < xtra_event_context_params.length;i+=2)
			event_context.put((String)xtra_event_context_params[i], xtra_event_context_params[i+1]);

		DISPATCH_EVENT(EVENT_USER_CREATED,event_context);
		return user;
	}
	
	@Export(ParameterNames={"email","password", "username"})  
	public Entity CreateSystemUser(UserApplicationContext ctx,String email,String password,String username) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)ctx.getUser();
		GUARD(user,CAN_CREATE_PUBLIC_USER);
		return createSystemUser(user, email, password, username);
	}
	
	public static final int ERROR_USER_EMAIL_EXISTS = 0x20001;
	public static final int ERROR_USER_USERNAME_EXISTS = 0x20002;
	public static final int ERROR_BAD_PASSWORD = 0x20003;
	public Entity createSystemUser(Entity creator,String email,String password,String username,Object... xtra_event_context_params) throws PersistenceException,WebApplicationException
	{
		Entity existing_user = getUserByEmail(email);
		if(existing_user != null)
			throw new WebApplicationException("USER WITH EMAIL "+email+" ALREADY EXISTS",ERROR_USER_EMAIL_EXISTS);

		username = STRIP_TO_ALPHA_NUMERIC(username);
		if(username != null && username.trim().equals(""))
			username = null;

		if(enforce_unique_username && username != null)
		{
			existing_user = getUserByUsernameAndPassword(username, Query.VAL_GLOB);
			if(existing_user != null)
				throw new WebApplicationException("USER WITH USERNAME "+username+" ALREADY EXISTS.",ERROR_USER_USERNAME_EXISTS);
		}
		//if(getUserByUsernameAndPassword(username,password) != null)
		//	throw new WebApplicationException("BAD PASSWORD",ERROR_BAD_PASSWORD);
		
		List<Integer> roles = new ArrayList<Integer>();
		roles.add(USER_ROLE_SYSTEM_USER);
		

		Entity user =  NEW(USER_ENTITY,
						   creator,
						   FIELD_USERNAME,username,
						   FIELD_EMAIL,email,
						   FIELD_PASSWORD,password,
						   FIELD_ROLES,roles);				
		
		Map<String,Object> event_context = new HashMap<String, Object>();
		event_context.put(USER_EVENT_USER, user);
		for(int i = 0;i < xtra_event_context_params.length;i+=2)
			event_context.put((String)xtra_event_context_params[i], xtra_event_context_params[i+1]);

		DISPATCH_EVENT(EVENT_USER_CREATED,event_context);

		return user;


	}

	@Export(ParameterNames={"user_entity_id","email"})
	public Entity UpdateEmail(UserApplicationContext uctx,long user_entity_id,String email) throws WebApplicationException,PersistenceException
	{
		Entity editor    = (Entity)uctx.getUser();
		Entity target    = GET(USER_ENTITY,user_entity_id);
		GUARD(editor,CAN_EDIT_USER,GUARD_USER,target);
		Entity updated_user =  updateEmail(target, email);
		if(editor.equals(target))
			uctx.setUser(updated_user);
		return updated_user;
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
	public boolean EmailExists(UserApplicationContext uctx,String email) throws PersistenceException,WebApplicationException
	{
		return (getUserByEmail(email) != null);
	}
	
	@Export(ParameterNames={"user_entity_id","username"})
	public Entity UpdateUserName(UserApplicationContext uctx,long user_entity_id,String username) throws PersistenceException,WebApplicationException
	{
		Entity editor    = (Entity)uctx.getUser();
		Entity target    = GET(USER_ENTITY,user_entity_id);
		GUARD(editor,CAN_EDIT_USER,GUARD_USER,target);
		Entity updated_user =  updateUserName(target, username);
		if(editor.equals(target))
			uctx.setUser(updated_user);
		return updated_user;
	}
	
	public Entity updateUserName(Entity user,String username) throws PersistenceException,WebApplicationException
	{
		username = STRIP_TO_ALPHA_NUMERIC(username);
		if(username != null && username.trim().equals(""))
			username = null;

		if(enforce_unique_username && username != null)
		{
			if(username.equals(user.getAttribute(FIELD_USERNAME)))
				return user;

			Entity existing_user = getUserByUsernameAndPassword(username, Query.VAL_GLOB);
			if(existing_user != null)
				throw new WebApplicationException("USER WITH USERNAME "+username+" ALREADY EXISTS.",ERROR_USER_USERNAME_EXISTS);
		}
		return UPDATE(user,
				  	  FIELD_USERNAME,username);		
		
	}
	
	@Export(ParameterNames={"user_entity_id","old_password","new_password"})
	public Entity UpdatePassword(UserApplicationContext uctx,long user_entity_id,String old_password,String new_password/*,boolean do_md5_on_server*/) throws PersistenceException,WebApplicationException
	{
		Entity editor    = (Entity)uctx.getUser();
		Entity target    = GET(USER_ENTITY,user_entity_id);
		GUARD(editor,CAN_EDIT_USER,GUARD_USER,target);
		
		if(editor.equals(target))
		{
			// AS LONG AS THE PERSON IS LOGGED IN, THEY GET TO UPDATE THE PASSWORD
			// THIS IS FOR THE CASE OF WHEN THEY HAVE BEEN AUTHENTICATED BY THE LOST PASSWORD SYSTEM
//			if(!target.getAttribute(FIELD_PASSWORD).equals(old_password))
//				throw new PermissionsException("BAD OLD PASSWORD");
		}
		
		Entity updated_user =  updatePassword(target, new_password);
		if(editor.equals(target))
			uctx.setUser(updated_user);
		return updated_user;
	}
	
	public Entity updatePassword(Entity user,String password) throws PersistenceException
	{
		return UPDATE(user,
				  UserModule.FIELD_PASSWORD,password);				
	}
	
	@Export(ParameterNames={"user_entity_id","role"})
	public Entity AddRole(UserApplicationContext ctx,long user_entity_id,int role) throws PersistenceException,WebApplicationException
	{
		Entity editor    = (Entity)ctx.getUser();
		Entity target    = GET(USER_ENTITY,user_entity_id);
		GUARD(editor,CAN_ADD_ROLE,
					 GUARD_USER,target,
					 "role",role);
		Entity updated_user =  addRole(target, role);
		if(editor.equals(target))
			ctx.setUser(updated_user);
		return updated_user;
	}
	
	public Entity addRole(Entity user,int role) throws PersistenceException,WebApplicationException
	{
		List<Integer> roles = (List<Integer>)user.getAttribute(FIELD_ROLES);
		roles.add(role);
		
		
		DISPATCH_EVENT(EVENT_USER_ROLES_UPDATED,
				   USER_EVENT_USER, user);
		return UPDATE(user,
				  	  UserModule.FIELD_ROLES,roles);						
	}
	
	@Export(ParameterNames={"user_entity_id","role"})
	public Entity RemoveRole(UserApplicationContext ctx,long user_entity_id,int role) throws PersistenceException,WebApplicationException
	{
		Entity editor    = (Entity)ctx.getUser();
		Entity target    = GET(USER_ENTITY,user_entity_id);
		GUARD(editor,CAN_REMOVE_ROLE,
				     GUARD_USER,target,
				     "role",role);

		Entity updated_user =  removeRole(target, role);
		if(editor.equals(target))
			ctx.setUser(updated_user);
		return updated_user;
	}
	
	public Entity removeRole(Entity user,int role) throws PersistenceException,WebApplicationException
	{
		List<Integer> roles = (List<Integer>)user.getAttribute(FIELD_ROLES);
		roles.remove(role);
		DISPATCH_EVENT(EVENT_USER_ROLES_UPDATED,
				   USER_EVENT_USER, user);
		return UPDATE(user,
			  UserModule.FIELD_ROLES,roles);			
	}
	
	
	public boolean isRole(Entity user,int role) throws PersistenceException
	{
		List<Integer> roles = (List<Integer>)user.getAttribute(FIELD_ROLES);
		return roles.contains(role);
	}
	
	@Export(ParameterNames={"user_entity_id","lock_code","notes"})
	public Entity LockUser(UserApplicationContext ctx,long user_entity_id,int lock_code,String notes) throws WebApplicationException,PersistenceException
	{
		Entity editor    = (Entity)ctx.getUser();
		Entity target    = GET(USER_ENTITY,user_entity_id);
		int lock = (Integer)target.getAttribute(FIELD_LOCK);
		if(lock == LOCK_LOCKED)
			return target;
		GUARD(editor, CAN_LOCK_USER, GUARD_USER,target);
		return lockUser(target, lock_code,notes);
	}
	
	public Entity lockUser(Entity user,int lock_code,String notes) throws PersistenceException
	{
		return UPDATE(user,
				  UserModule.FIELD_LOCK,LOCK_LOCKED,
				  UserModule.FIELD_LOCK_CODE,lock_code,
				  UserModule.FIELD_LOCK_NOTES,notes);
	}
	
	@Export(ParameterNames={"user_entity_id"})
	public Entity UnlockUser(UserApplicationContext ctx,long user_entity_id) throws WebApplicationException,PersistenceException
	{
		Entity editor     = (Entity)ctx.getUser();
		Entity target     = GET(USER_ENTITY,user_entity_id);
		
		int lock = (Integer)target.getAttribute(FIELD_LOCK);
		if(lock != LOCK_LOCKED)
			return target;
		int old_lock_code = (Integer)target.getAttribute(FIELD_LOCK_CODE); 
		GUARD(editor, CAN_UNLOCK_USER, GUARD_USER,target);
		return unlockUser(target);
	}
	
	public Entity unlockUser(Entity user) throws PersistenceException
	{
		System.out.println("!!!!UNLOCKING USER "+user);
		return UPDATE(user,
				UserModule.FIELD_LOCK,LOCK_UNLOCKED,
				UserModule.FIELD_LOCK_CODE,LOCK_CODE_UNLOCKED,
				UserModule.FIELD_LOCK_NOTES,"");	
	}

	public static final int ERROR_LOGIN_FAILED = 0x20010;	
	@Export(ParameterNames={"email_or_username", "password"})
	public Entity Login(UserApplicationContext uctx,String email_or_username,String password) throws WebApplicationException,PersistenceException
	{
		
		Entity user = null;
		//if(isValidEmail(email_or_username))
			user = loginViaEmail(email_or_username, password);
		//else
		//	user = loginViaUsername(email_or_username, password);
		
		if(user.getAttribute(FIELD_LOCK).equals(LOCK_LOCKED))
		{
			int lock_code = (Integer)user.getAttribute(FIELD_LOCK_CODE);
			String message = getLockCodeMessage(lock_code);
			if(message == null)
				message = "ACCOUNT IS LOCKED: code"+Integer.toHexString(lock_code); 
			
			throw new AccountLockedException(message);
		}

		uctx.setUser(user);
		DISPATCH_EVENT(EVENT_USER_LOGGED_IN,
				   USER_EVENT_USER, user);
		return UPDATE(user,
					  FIELD_LAST_LOGIN, new Date());
				
	}
	
	public Entity loginViaEmail(String email,String password)throws WebApplicationException,PersistenceException
	{
		//System.out.println("TRYING TO LOGIN WITH PASSWORD "+password);
		Entity user = getUserByEmail(email);
		//System.out.println("USER "+user+"\n"+email);
		if(user == null)
			throw new LoginFailedException("LOGIN FAILED",ERROR_LOGIN_FAILED);

		if(user.getAttribute(FIELD_PASSWORD).equals(password))
			return user;
		throw new LoginFailedException("LOGIN FAILED",ERROR_LOGIN_FAILED);
	}
	
	public Entity loginViaUsername(String username,String password)throws WebApplicationException,PersistenceException
	{
		Entity user = getUserByUsernameAndPassword(username, password);
		if(user == null)
			throw new LoginFailedException("LOGIN FAILED");
		return user;
	}
	
	public boolean isValidEmail(String email)
	{
		return Validator.isValidEmail(email);
	}
	
	@Export
	public Entity Logout(UserApplicationContext uctx) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		if(!PermissionEvaluator.IS_LOGGED_IN(user))
			return null;

		DISPATCH_EVENT(EVENT_USER_LOGGED_OUT,
					   USER_EVENT_USER,user,
					   USER_EVENT_USER_CONTEXT,uctx);
		uctx.setUser(null);
		
		return UPDATE(user,
				  FIELD_LAST_LOGOUT, new Date());
	}

	@Export (ParameterNames={"user_id"})
	public Entity DeleteUser(UserApplicationContext uctx,long user_id)throws WebApplicationException,PersistenceException
	{
		Entity editor     = (Entity)uctx.getUser();
		Entity target     = GET(USER_ENTITY,user_id);

		GUARD(editor, CAN_DELETE_USER, 
					  GUARD_USER,target);
		return deleteUser(target);
	}
	
	public Entity deleteUser(Entity user)throws PersistenceException,WebApplicationException
	{
		System.out.println("!!! DELETEING USER "+user);
		user = EXPAND(user);
		long id = user.getId();
		DELETE(user);
		user.setId(id);
		DISPATCH_EVENT(EVENT_USER_DELETED, 
		        		USER_EVENT_USER,user);
		//user.setId(Entity.UNDEFINED);
		return user;
	}
	
	@Export(ParameterNames={"role", "offset", "page_size"})
	public PagingQueryResult GetUsersByRole(UserApplicationContext uctx,int role,int offset,int page_size) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(user, CAN_BROWSE_USERS_BY_ROLE, "role",role);
		return getUsersByRole(role, offset, page_size);
	}
	
	@Export(ParameterNames={"role", "offset", "page_size", "order_by"})
	public PagingQueryResult GetUsersByRole(UserApplicationContext uctx,int role,int offset,int page_size, String order_by) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(user, CAN_BROWSE_USERS_BY_ROLE, "role",role);
		return getUsersByRole(role, offset, page_size,order_by);
	}
	
	@Export(ParameterNames={"role", "offset", "page_size", "order_by", "direction"})
	public PagingQueryResult GetUsersByRole(UserApplicationContext uctx,int role,int offset,int page_size, String order_by, int direction) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(user, CAN_BROWSE_USERS_BY_ROLE, "role",role);
		return getUsersByRole(role, offset, page_size,order_by,direction);
	}
	
	public PagingQueryResult getUsersByRole(int role,int offset,int page_size) throws PersistenceException,WebApplicationException
	{
		return getUsersByRole(role,offset,page_size,FIELD_EMAIL,Query.ASC);		
	}
	
	public PagingQueryResult getUsersByRole(int role,int offset,int page_size, String order_by) throws PersistenceException,WebApplicationException
	{
		return getUsersByRole(role,offset,page_size,order_by,Query.ASC);		
	}
	
	public PagingQueryResult getUsersByRole(int role,int offset,int page_size, String order_by, int dir) throws PersistenceException,WebApplicationException
	{
		Query q = new Query(USER_ENTITY);
		q.idx(INDEX_BY_ROLE);
		q.setContainsAny(q.list(role));
		q.setOffset(offset);
		q.setPageSize(page_size);
		q.orderBy(order_by,dir);
		return PAGING_QUERY(q);			
	}
	
	@Export(ParameterNames={"offset", "page_size"})
	public PagingQueryResult GetLockedUsers(UserApplicationContext uctx,int offset,int page_size) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(user, CAN_BROWSE_LOCKED_USERS);
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
	
	@Export(ParameterNames={"lock_code", "offset", "page_size"})
	public PagingQueryResult GetLockedUsersByLockCode(UserApplicationContext uctx,int lock_code,int offset,int page_size) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(user, CAN_BROWSE_LOCKED_USERS);
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
	public String GetSessionId(UserApplicationContext uctx)
	{
		return GatewayConstants.SESSION_ID_KEY + "=" + uctx.getId();
	}
	
	@Export
	public Entity GetUser(UserApplicationContext uctx)
	{
		Entity user = (Entity)uctx.getUser();
		return user;
	}
	

	/////////////////E N D  M O D U L E   F U N C T I O N S/////////////////////////////////////////
	
	public Entity getUser(long user_id) throws PersistenceException
	{
		return GET(USER_ENTITY,user_id);
	}
	
	public Entity getAdminUser() throws PersistenceException
	{
		return getUser(1);
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
	
	public Entity getUserByUniqueUsername(String username) throws PersistenceException,WebApplicationException
	{
		if(enforce_unique_username)
			return getUserByUsernameAndPassword(username, Query.VAL_GLOB);

		throw new WebApplicationException("CANNOT GET A UNIQUE USER BY USERNAME SINCE UNIQUE USERNAMES ARE NOT CURRENTLY ENFORCED.");
	}
	
	public Entity getUserByUsernameAndPassword(String username,Object password) throws PersistenceException,WebApplicationException
	{
		Query q = new Query(USER_ENTITY);
		q.idx(INDEX_BY_USERNAME_BY_PASSWORD);
		q.eq(q.list(username,password));
		QueryResult result = QUERY(q);
		if(result.size() == 1)
			return result.getEntities().get(0);
		else if(result.size() == 0)
			return null;
		throw new WebApplicationException("MORE THAN ONE USER WITH EMAIL "+username+" WITH SAME PASSWORD EXISTS! FIX DATA.");			
	}
	



	private void setup_admin_user(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		Query q = new Query(USER_ENTITY).idx(Query.PRIMARY_IDX).eq(1);
		QueryResult r = QUERY(q);
		if(r.size()==0)
		{
			
			String admin_email 		= null;
			String admin_password 	= null;
			String answer = "N";
			System.out.println("Please set up the admin user for the application.");
			while(true)
			{
				try{
					admin_email 	   = GET_CONSOLE_INPUT("admin email:");
					admin_password 	   = GET_CONSOLE_INPUT("password:");
					answer 			   = GET_CONSOLE_INPUT("CREATE ADMIN USER "+admin_email+" WITH PASSWORD "+admin_password+"?[Y/N]");
				}catch(Exception e)
				{
					e.printStackTrace();
					continue;
				}
				if(answer != null && (answer.startsWith("y") || answer.startsWith("Y")))
					break;
			}
			
			INFO("CREATING ADMIN USER - "+admin_email);
			
			List<Integer> admin_roles = new ArrayList<Integer>();
			admin_roles.add(USER_ROLE_WHEEL);
			
			try{
				createPrivilegedUser(null,admin_email,Util.stringToHexEncodedMD5(admin_password),"admin",admin_roles);
			}catch(WebApplicationException e)
			{
				ERROR(e);
				throw new InitializationException("FAILED CREATING ADMIN USER.");
			}
			//NEW(USER_ENTITY,
			//	null,
			//	UserModule.FIELD_EMAIL,admin_email,
			//	UserModule.FIELD_PASSWORD,Util.stringToHexEncodedMD5(admin_password),
			//	UserModule.FIELD_ROLES,admin_roles,
			//	UserModule.FIELD_LOCK,LOCK_UNLOCKED,
			//	UserModule.FIELD_LOCK_CODE,LOCK_CODE_DEFAULT);			
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
	protected void defineEntities(Map<String,Object> config) throws PersistenceException,InitializationException
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
	public static String INDEX_BY_USERNAME_BY_PASSWORD	=   "byUsernameByPassword";
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY_INDICES
		(
				USER_ENTITY,
				ENTITY_INDEX(INDEX_BY_EMAIL, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, FIELD_EMAIL),
				ENTITY_INDEX(INDEX_BY_LOCK_BY_LOCK_CODE, EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, FIELD_LOCK,FIELD_LOCK_CODE),
				ENTITY_INDEX(INDEX_BY_ROLE, EntityIndex.TYPE_ARRAY_MEMBERSHIP_INDEX, FIELD_ROLES),	
				ENTITY_INDEX(INDEX_BY_USERNAME_BY_PASSWORD, EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, FIELD_USERNAME,FIELD_PASSWORD)
		);	
	}
	
}
