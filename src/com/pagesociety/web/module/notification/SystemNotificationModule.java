package com.pagesociety.web.module.notification;

import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.Types;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.PermissionsException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.PermissionsModule;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.user.UserModule;

public class SystemNotificationModule extends WebStoreModule 
{
	public static final int NOTIFICATION_TYPE_UNDEFINED     = 0x00;
	public static final int NOTIFICATION_TYPE_USER 			= 0x01;
	public static final int NOTIFICATION_TYPE_GLOBAL  		= 0x02;
	
	public static final int NOTIFICATION_LEVEL_ALERT 		= 0x01;
	public static final int NOTIFICATION_LEVEL_INFO  		= 0x02;
	
	

	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
	}

	protected void defineSlots()
	{
		super.defineSlots();
	}

	@Export
	public Entity CreateInfoNotificationForUser(UserApplicationContext uctx,long user_id,String notification_text) throws WebApplicationException,PersistenceException 
	{
		Entity user = (Entity)uctx.getUser();
		Entity target_user = GET(UserModule.USER_ENTITY,user_id);
		if(!PermissionsModule.IS_ADMIN(user))
			throw new WebApplicationException("NO PERMISSION");
		return createInfoNotificationForUser(user, target_user, notification_text);
	}
	
	public Entity createInfoNotificationForUser(Entity creator,Entity user,String notification_text) throws PersistenceException 
	{
		return createNotificationForUser(creator, user, NOTIFICATION_LEVEL_INFO, notification_text);
	}
	
	@Export
	public Entity CreateAlertNotificationForUser(UserApplicationContext uctx,long user_id,String notification_text) throws WebApplicationException,PersistenceException 
	{
		Entity user = (Entity)uctx.getUser();
		Entity target_user = GET(UserModule.USER_ENTITY,user_id);
		if(!PermissionsModule.IS_ADMIN(user))
			throw new WebApplicationException("NO PERMISSION");
		return createAlertNotificationForUser(user, target_user, notification_text);
	}
	
	public Entity createAlertNotificationForUser(Entity creator,Entity user,String notification_text) throws PersistenceException 
	{
		return createNotificationForUser(creator, user, NOTIFICATION_LEVEL_ALERT, notification_text);
	}
	
	public Entity createNotificationForUser(Entity creator,Entity user,int notification_level,String notification_text) throws PersistenceException 
	{
		return NEW(SYSTEM_NOTIFICATION_ENTITY,
				creator,
				SYSTEM_NOTIFICATION_FIELD_USER,user,
				SYSTEM_NOTIFICATION_FIELD_TYPE,NOTIFICATION_TYPE_USER,
				SYSTEM_NOTIFICATION_FIELD_LEVEL,notification_level,
				SYSTEM_NOTIFICATION_FIELD_TEXT,notification_text);	
	}
	
	@Export
	public Entity CreateGlobalInfoNotification(UserApplicationContext uctx,String notification_text) throws WebApplicationException,PersistenceException 
	{
		Entity user = (Entity)uctx.getUser();
		if(!PermissionsModule.IS_ADMIN(user))
			throw new WebApplicationException("NO PERMISSION");
		return createGlobalInfoNotification(user, notification_text);
	}
	
	public Entity createGlobalInfoNotification(Entity creator,String notification_text) throws PersistenceException 
	{
		return createGlobalNotification(creator,NOTIFICATION_LEVEL_INFO, notification_text);
	}

	@Export
	public Entity CreateGlobalAlertNotification(UserApplicationContext uctx,String notification_text) throws WebApplicationException,PersistenceException 
	{
		Entity user = (Entity)uctx.getUser();
		if(!PermissionsModule.IS_ADMIN(user))
			throw new WebApplicationException("NO PERMISSION");
		return createGlobalAlertNotification(user, notification_text);	
	}
	
	public Entity createGlobalAlertNotification(Entity creator,String notification_text) throws PersistenceException 
	{
		return createGlobalNotification(creator, NOTIFICATION_LEVEL_ALERT, notification_text);
	}
	
	public Entity createGlobalNotification(Entity creator,int notification_level,String notification_text) throws PersistenceException 
	{
		return NEW(SYSTEM_NOTIFICATION_ENTITY,
				creator,
				SYSTEM_NOTIFICATION_FIELD_USER,null,
				SYSTEM_NOTIFICATION_FIELD_TYPE,NOTIFICATION_TYPE_GLOBAL,
				SYSTEM_NOTIFICATION_FIELD_LEVEL,notification_level,
				SYSTEM_NOTIFICATION_FIELD_TEXT,notification_text);	
	}
	
	@Export
	public Entity DeleteNotification(UserApplicationContext uctx,long notification_id) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity notification = GET(SYSTEM_NOTIFICATION_ENTITY,notification_id);
		int notification_type = (Integer)notification.getAttribute(SYSTEM_NOTIFICATION_FIELD_TYPE);
		Entity notification_user = (Entity)notification.getAttribute(SYSTEM_NOTIFICATION_FIELD_USER);
		
		switch(notification_type)
		{
			case NOTIFICATION_TYPE_USER:
				if(!PermissionsModule.IS_ADMIN(user) && !PermissionsModule.IS_SAME(user, notification_user))
					throw new PermissionsException("NO PERMISSION");
				break;
			case NOTIFICATION_TYPE_GLOBAL:
				if(!PermissionsModule.IS_ADMIN(user))
					throw new PermissionsException("NO PERMISSION");
				break;
			default:
				throw new WebApplicationException("UNKNOWN NOTIFICATION TYPE");
		}
		
		return deleteNotification(notification);
	}
	
	public Entity deleteNotification(Entity notification) throws PersistenceException 
	{
		return DELETE(notification);
	}
	
	@Export
	public PagingQueryResult GetUserNotifications(UserApplicationContext uctx,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		if(!PermissionsModule.IS_LOGGED_IN(user))
			throw new PermissionsException("NO PERMISSION");
		
		Query q = getUserNotificationsQ(user);
		q.offset(offset);
		q.pageSize(page_size);
		q.orderBy(FIELD_LAST_MODIFIED, Query.DESC);
		return PAGING_QUERY(q);
	}
	
	public Query getUserNotificationsQ(Entity user) throws PersistenceException
	{
		Query q = new Query(SYSTEM_NOTIFICATION_ENTITY);
		q.idx(IDX_BY_USER);
		q.eq(user);
		return q;
	}
	
	@Export
	public PagingQueryResult GetGlobalNotifications(UserApplicationContext uctx,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		if(!PermissionsModule.IS_LOGGED_IN(user))
			throw new PermissionsException("NO PERMISSION");
		
		Query q = getGlobalNotificationsQ();
		q.offset(offset);
		q.pageSize(page_size);
		q.orderBy(FIELD_LAST_MODIFIED, Query.DESC);
		return PAGING_QUERY(q);
	}
	
	public Query getGlobalNotificationsQ() throws PersistenceException
	{
		Query q = new Query(SYSTEM_NOTIFICATION_ENTITY);
		q.idx(IDX_BY_TYPE);
		q.eq(NOTIFICATION_TYPE_GLOBAL);
		return q;
	}

	//// END MODULE STUFF ///

	//DDL STUFF
	public static final String SYSTEM_NOTIFICATION_ENTITY			= "SystemNotification";
	public static final String SYSTEM_NOTIFICATION_FIELD_TYPE		= "notification_type";
	public static final String SYSTEM_NOTIFICATION_FIELD_USER		= "notification_user";
	public static final String SYSTEM_NOTIFICATION_FIELD_LEVEL		= "notification_level";
	public static final String SYSTEM_NOTIFICATION_FIELD_TEXT		= "notification_text";

	
	protected void defineEntities(Map<String,Object> config) throws PersistenceException,SyncException,InitializationException
	{
		DEFINE_ENTITY(SYSTEM_NOTIFICATION_ENTITY,
					  SYSTEM_NOTIFICATION_FIELD_TYPE, Types.TYPE_INT,NOTIFICATION_TYPE_UNDEFINED, 
					  SYSTEM_NOTIFICATION_FIELD_USER, Types.TYPE_REFERENCE,UserModule.USER_ENTITY,null,
					  SYSTEM_NOTIFICATION_FIELD_LEVEL, Types.TYPE_INT,NOTIFICATION_LEVEL_INFO,
					  SYSTEM_NOTIFICATION_FIELD_TEXT, Types.TYPE_STRING,null);
	}
	
	

	public static String IDX_BY_TYPE		=   "byType";
	public static String IDX_BY_USER		=   "byUser";
	
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY_INDICES
		(
			SYSTEM_NOTIFICATION_ENTITY,
			ENTITY_INDEX(IDX_BY_TYPE, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, SYSTEM_NOTIFICATION_FIELD_TYPE),
			ENTITY_INDEX(IDX_BY_USER, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, SYSTEM_NOTIFICATION_FIELD_USER)		
		);
	}
	
}
