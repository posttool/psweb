package com.pagesociety.web.module.registration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.email.IEmailModule;
import com.pagesociety.web.module.user.UserModule;




public class ForgotPasswordModule extends WebStoreModule 
{	
	

	private static final String SLOT_USER_MODULE  = "user-module"; 
	private static final String SLOT_EMAIL_MODULE = "email-module"; 

	private static final String PARAM_EMAIL_TEMPLATE_NAME  	  	 = "forgot-password-email-template";
	private static final String PARAM_EMAIL_SUBJECT		  	  	 = "forgot-password-email-subject";
	private static final String PARAM_RESTORE_PASSWORD_URL	  	 = "restore-password-url";

	private String				email_template_name;
	private String				email_subject;
	private String				restore_password_url;
	private UserModule 			user_module;
	private IEmailModule 		email_module;

	
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		user_module  = (UserModule)getSlot(SLOT_USER_MODULE);
		email_module = (IEmailModule)getSlot(SLOT_EMAIL_MODULE);
		
		email_template_name = GET_REQUIRED_CONFIG_PARAM(PARAM_EMAIL_TEMPLATE_NAME, config);
		email_subject	    = GET_REQUIRED_CONFIG_PARAM(PARAM_EMAIL_SUBJECT, config);
		restore_password_url= GET_REQUIRED_CONFIG_PARAM(PARAM_RESTORE_PASSWORD_URL, config);
	}

	
	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_USER_MODULE,UserModule.class,true);
		DEFINE_SLOT(SLOT_EMAIL_MODULE,IEmailModule.class,true);
	}
	
	/// B E G I N      M O D U L E      F U N C T I O N S //////////
	@Export
	public boolean ForgotPassword(UserApplicationContext uctx,String email) throws WebApplicationException,PersistenceException
	{
		Entity user = user_module.getUserByEmail(email);
		if(user == null)
			throw new WebApplicationException("NO USER WITH EMAIL "+email+"EXISTS");
		/*creator is null. means system created this user */
		String forgot_password_token = com.pagesociety.util.RandomGUID.getGUID();
	
		
		NEW(OUTSTANDING_FORGOT_PASSWORD_ENTITY,null,
			FIELD_ACTIVATION_TOKEN,forgot_password_token,
			FIELD_ACTIVATION_UID,user.getId());
			

		Map<String,Object> template_data = new HashMap<String,Object>();
		template_data.put("user", user);
		template_data.put("username", user.getAttribute(UserModule.FIELD_USERNAME));
		template_data.put("email", email);
		template_data.put("forgot_password_token", forgot_password_token);
		template_data.put("restore_password_url", restore_password_url);
		
		email_module.sendEmail(null, new String[]{email}, email_subject, email_template_name, template_data);
		return true;
	}
	

	
	@Export
	public Entity LoginWithForgotPasswordToken(UserApplicationContext uctx,String token) throws WebApplicationException,PersistenceException
	{
		Query q = new Query(OUTSTANDING_FORGOT_PASSWORD_ENTITY);
		q.idx(INDEX_BY_ACTIVATION_TOKEN);
		q.eq(token);
		QueryResult result = QUERY(q);
		if (result.size() == 0)
			throw new WebApplicationException("BAD FORGOT PASSWORD TOKEN");
		
		Entity forgot_password_record = result.getEntities().get(0);
		Entity user = GET(UserModule.USER_ENTITY, (Long) forgot_password_record.getAttribute(FIELD_ACTIVATION_UID));
		DELETE(forgot_password_record);
		// log them in//
		uctx.setUser(user);
		return user;
	}
	
	//// E N D       M O D U L E      F U N C T I O N S //////////

	
	
	///// E N T I T Y     D E F S   ////////
	
	public static String OUTSTANDING_FORGOT_PASSWORD_ENTITY = "OutstandingForgotPassword";
	public static String FIELD_ACTIVATION_TOKEN	= "activation_token";
	public static String FIELD_ACTIVATION_UID   = "activation_uid";
	protected void defineEntities(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY(OUTSTANDING_FORGOT_PASSWORD_ENTITY,
					  FIELD_ACTIVATION_TOKEN, Types.TYPE_STRING,null, 
					  FIELD_ACTIVATION_UID, Types.TYPE_LONG,null);
					  
	}
	
	public static String INDEX_BY_ACTIVATION_TOKEN	=   "byActivationToken";
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY_INDEX(OUTSTANDING_FORGOT_PASSWORD_ENTITY,INDEX_BY_ACTIVATION_TOKEN, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, FIELD_ACTIVATION_TOKEN);
	}
	
}
