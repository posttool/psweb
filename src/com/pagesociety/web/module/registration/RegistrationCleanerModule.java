package com.pagesociety.web.module.registration;



import java.util.Date;
import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.user.UserModule;





public class RegistrationCleanerModule extends WebStoreModule 
{	
	
	/* TODO: NOTE: THESE ARE EXPRESSED IN HOURS FOR NOW */
	private static final String PARAM_REGISTRATION_PRUNE_PERIOD  	  		= "registration-prune-period";//in hours
	private static final String PARAM_REGISTRATION_EXPIRATION_THRESHOLD  	= "registration-expiration-threshold";//in hours

	private static final String SLOT_USER_MODULE						  	= "user-module";
	protected UserModule 		user_module;
	
	private int					registration_prune_period;//hours
	private int					registration_expiration_threshold;//hours
	
	
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
	
		registration_prune_period 			= (int)(1000 * 60 * 60 * Float.parseFloat(GET_REQUIRED_CONFIG_PARAM(PARAM_REGISTRATION_PRUNE_PERIOD, config)));
		registration_expiration_threshold 	= (int)(1000 * 60 * 60 * Float.parseFloat(GET_REQUIRED_CONFIG_PARAM(PARAM_REGISTRATION_EXPIRATION_THRESHOLD, config)));
		System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!prune period "+registration_prune_period);
		System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!exp threshold "+registration_expiration_threshold);
		
		user_module  = (UserModule)getSlot(SLOT_USER_MODULE);
		start_cleaner();
	}
	
	public void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_USER_MODULE,UserModule.class,true);
	}
	/// B E G I N      M O D U L E      F U N C T I O N S //////////

	
	//// E N D       M O D U L E      F U N C T I O N S //////////
	
	private void start_cleaner()
	{
		Thread t = new Thread()
		{
			
			public void run()
			{
				while(true)
				{
					try{
						clean_unactivated_registrations();
					}catch(Exception e)
					{
						e.printStackTrace();
					}
					try{
						Thread.sleep(registration_prune_period);//in hours
					}catch(InterruptedException ie)
					{
						ie.printStackTrace();
					}
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}

	
	private void clean_unactivated_registrations() throws PersistenceException
	{
		long expiration_period = registration_expiration_threshold;
		Date the_past = new Date(new Date().getTime() - expiration_period);
		
		Query q = new Query(RegistrationModule.OUTSTANDING_REGISTRATION_CONFIRMATION_ENTITY);
		q.idx(INDEX_BY_DATE_CREATED);
		q.lt(the_past);
		QueryResult result = QUERY(q);

		List<Entity> old_records = result.getEntities();
		//System.out.println("OLD ACTIVITY RECORDS IS "+old_records.size());
		for(int i = 0;i < result.size();i++)
		{
			Entity old_record = old_records.get(i);
			Entity user		  = null;
			try {
				user = GET(UserModule.USER_ENTITY, (Long)old_record.getAttribute(RegistrationModule.FIELD_ACTIVATION_UID));
				//System.out.println("ABOUT TO DELETE "+user);
				user_module.deleteUser(user);
				DELETE(old_record);
			} catch (PersistenceException e) {
				e.printStackTrace();
			}

		}

	}

	public static String INDEX_BY_DATE_CREATED		=   "byDateCreated";
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		if(store.getEntityDefinition(RegistrationModule.OUTSTANDING_REGISTRATION_CONFIRMATION_ENTITY) == null)
			throw new SyncException("Please place RegistrationCleanerModule after RegistrationModule in application.xml.It is dependent on it.");

		DEFINE_ENTITY_INDEX(RegistrationModule.OUTSTANDING_REGISTRATION_CONFIRMATION_ENTITY,INDEX_BY_DATE_CREATED, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX,WebStoreModule.FIELD_DATE_CREATED);
	}
		
}
