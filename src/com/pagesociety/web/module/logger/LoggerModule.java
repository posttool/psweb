package com.pagesociety.web.module.logger;


import java.util.Map;





import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.Types;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.WebStoreModule;


public class LoggerModule extends WebStoreModule
{	
	//private static final String SLOT_LOGGER_GUARD				 = "log-guard";
	private static final String PARAM_LOG_NAME  		 		 = "log-name";

	
	protected String log_entity_name;

	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		
		
		//guard = (ILoggerGuard)getSlot(SLOT_EMAIL_GUARD);
	}

	protected void defineSlots()
	{
		super.defineSlots();
		//DEFINE_SLOT(SLOT_EMAIL_GUARD,IEmailGuard.class,false,DefaultEmailGuard.class);
	}

	
	public Entity createLogMessage(Entity user,int message_type,String message,Entity data)
			throws PersistenceException 
	{
		return NEW(log_entity_name,
					user,
					FIELD_LOG_MESSAGE,message,
					FIELD_LOG_MESSAGE_TYPE,message_type,
					FIELD_LOG_MESSAGE_DATA,data);
	}
	
	
	public Entity deleteLogMessage(Entity log_message) throws PersistenceException 
	{
		return DELETE(log_message);
	}
	
	
	public Query getLogMessagesByUserQ(Entity user) throws PersistenceException
	{
		Query q = new Query(log_entity_name);
		q.idx(IDX_BY_CREATOR_BY_TYPE);
		q.eq(q.list(user,Query.VAL_GLOB));
		return q;
	}
	
	public Query getLogMessagesByMessageTypeQ(int type) throws WebApplicationException,PersistenceException
	{
		Query q = new Query(log_entity_name);
		q.idx(IDX_BY_TYPE);
		q.eq(type);
		return q;	
	}
	
	public Query getLogMessagesByUserByTypeQ(Entity user,int type) throws PersistenceException
	{
		Query q = new Query(log_entity_name);
		q.idx(IDX_BY_CREATOR_BY_TYPE);
		q.eq(q.list(user,type));
		return q;
	}
	

	//// END MODULE STUFF ///
	//ENTITY NAME IS ALWAYS USER DEFINED...see defineEntities//
	public static String FIELD_LOG_MESSAGE_TYPE	  = "message_type";
	public static String FIELD_LOG_MESSAGE		  = "message";
	public static String FIELD_LOG_MESSAGE_DATA	  = "data";
	
	protected void defineEntities(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		//sort of a little hack here so we can specify the entity name from the cofig file
		log_entity_name = GET_REQUIRED_CONFIG_PARAM(PARAM_LOG_NAME, config);
		DEFINE_ENTITY(log_entity_name,
					  FIELD_LOG_MESSAGE_TYPE, Types.TYPE_INT,0, 
					  FIELD_LOG_MESSAGE, Types.TYPE_STRING,"", 
					  FIELD_LOG_MESSAGE_DATA, Types.TYPE_REFERENCE,FieldDefinition.REF_TYPE_UNTYPED_ENTITY,null);
	
	}
	
	

	public static String IDX_BY_TYPE				=   "byType";
	public static String IDX_BY_CREATOR_BY_TYPE		=   "byCreatorByType";
	
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY_INDICES
		(
				log_entity_name,
				ENTITY_INDEX(IDX_BY_TYPE, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, FIELD_LOG_MESSAGE_TYPE),
				ENTITY_INDEX(IDX_BY_CREATOR_BY_TYPE, EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, FIELD_CREATOR,FIELD_LOG_MESSAGE_TYPE)	
		);
	}
	

}
