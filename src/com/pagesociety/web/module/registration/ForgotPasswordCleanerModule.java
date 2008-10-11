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
import com.pagesociety.web.module.WebStoreModule;




public class ForgotPasswordCleanerModule extends WebStoreModule 
{	
	
	/* TODO: NOTE: THESE ARE EXPRESSED IN HOURS FOR NOW */
	private static final String PARAM_FORGOT_PASSWORD_PRUNE_PERIOD  	  		= "forgot-password-prune-period";
	private static final String PARAM_FORGOT_PASSWORD_EXPIRATION_THRESHOLD  	= "forgot-password-expiration-threshold";

	private int					forgot_password_prune_period;//hours
	private int					forgot_password_expiration_threshold;
	
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		forgot_password_prune_period 			= (int)(1000 * 60 * 60 * Float.parseFloat(GET_REQUIRED_CONFIG_PARAM(PARAM_FORGOT_PASSWORD_PRUNE_PERIOD, config)));
		forgot_password_expiration_threshold 	= (int)(1000 * 60 * 60 * Float.parseFloat(GET_REQUIRED_CONFIG_PARAM(PARAM_FORGOT_PASSWORD_EXPIRATION_THRESHOLD, config)));
		start_cleaner();
	}
	
	///  B E G I N      M O D U L E      F U N C T I O N S //////////

	
	//// E N D         M O D U L E       F U N C T I O N S //////////
	
	private void start_cleaner()
	{
		Thread t = new Thread()
		{
			
			public void run()
			{
				while(true)
				{
					try{
						clean_unactivated_forgot_passwords();
					}catch(Exception e)
					{
						e.printStackTrace();
					}
					try{
						Thread.sleep(forgot_password_prune_period);//in hours
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

	
	private void clean_unactivated_forgot_passwords() throws PersistenceException
	{
		long expiration_period = forgot_password_expiration_threshold;
		Date the_past = new Date(new Date().getTime() - expiration_period);
		
		Query q = new Query(ForgotPasswordModule.OUTSTANDING_FORGOT_PASSWORD_ENTITY);
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
		if(store.getEntityDefinition(ForgotPasswordModule.OUTSTANDING_FORGOT_PASSWORD_ENTITY) == null)
			throw new SyncException("Please place RegistrationCleanerModule after RegistrationModule in application.xml.It is dependent on it.");

		DEFINE_ENTITY_INDEX(ForgotPasswordModule.OUTSTANDING_FORGOT_PASSWORD_ENTITY,INDEX_BY_DATE_CREATED, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX,WebStoreModule.FIELD_DATE_CREATED);
	}
		
}
