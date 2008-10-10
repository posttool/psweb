package com.pagesociety.web.module.registration;



import java.util.Date;
import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.web.InitializationException;
import com.pagesociety.web.SyncException;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.module.WebStoreModule;





public class RegistrationCleanerModule extends WebStoreModule 
{	
	
	/* TODO: NOTE: THESE ARE EXPRESSED IN HOURS FOR NOW */
	private static final String PARAM_REGISTRATION_PRUNE_PERIOD  	  		= "registration-prune-period";
	private static final String PARAM_REGISTRATION_EXPIRATION_THRESHOLD  	= "registration-expiration-threshold";

	private int					registration_prune_period;//hours
	private int					registration_expiration_threshold;
	
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		registration_prune_period 		  = Integer.parseInt(GET_REQUIRED_CONFIG_PARAM(PARAM_REGISTRATION_PRUNE_PERIOD, config));
		registration_expiration_threshold = Integer.parseInt(GET_REQUIRED_CONFIG_PARAM(PARAM_REGISTRATION_EXPIRATION_THRESHOLD, config));;
	
		registration_prune_period 			= 1000 * 60 * 60 * registration_prune_period;
		registration_expiration_threshold 	= 1000 * 60 * 60 * registration_expiration_threshold;
		start_cleaner();
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
			DELETE(old_record);
		}

	}

	public static String INDEX_BY_DATE_CREATED		=   "byDateCreated";
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,SyncException
	{
		if(store.getEntityDefinition(RegistrationModule.OUTSTANDING_REGISTRATION_CONFIRMATION_ENTITY) == null)
			throw new SyncException("Please place RegistrationCleanerModule after RegistrationModule in application.xml.It is dependent on it.");

		DEFINE_ENTITY_INDEX(RegistrationModule.OUTSTANDING_REGISTRATION_CONFIRMATION_ENTITY,INDEX_BY_DATE_CREATED, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX,WebStoreModule.FIELD_DATE_CREATED);
	}
		
}
