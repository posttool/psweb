package com.pagesociety.web.module.registration;




import java.util.HashMap;
import java.util.Map;
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
import com.pagesociety.web.module.TransactionProtect;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.email.IEmailModule;
import com.pagesociety.web.module.user.UserModule;




public class RegistrationModule extends WebStoreModule 
{	
	
	private static final String PARAM_EMAIL_CONFIRM  	  		 = "do-email-confirmation";
	private static final String PARAM_EMAIL_TEMPLATE_NAME  	  	 = "registration-email-template";
	private static final String PARAM_EMAIL_SUBJECT		  	  	 = "registration-email-subject";
	private static final String PARAM_ACTIVATE_ACCOUNT_URL		 = "activate-account-url";

	
	private static final String SLOT_USER_MODULE  = "user-module"; 
	private static final String SLOT_EMAIL_MODULE = "email-module"; 
	
	private boolean				do_email_confirmation;
	private String				email_template_name;
	private String				email_subject;
	private String				activate_account_url;
	private UserModule 			user_module;
	private IEmailModule 		email_module;
	
	private static final int LOCKED_PENDING_REGISTRATION = 0x2000;
	private static final String LOCK_MESSAGE = " Your account registration is pending."+
												" Check your email and activate your account "+
												" and then try to login again.";
	
	public static final int REGISTRATION_EVENT_ACCOUNT_REGISTERED 		  = 0x200;
	public static final int REGISTRATION_EVENT_ACCOUNT_ACTIVATED  		  = 0x202;
	public static final String REGISTRATION_EVENT_USER 		  = "user";
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		user_module  = (UserModule)getSlot(SLOT_USER_MODULE);
		String val = GET_REQUIRED_CONFIG_PARAM(PARAM_EMAIL_CONFIRM, config);
		if(val.equalsIgnoreCase("true"))
		{	
			do_email_confirmation = true;
			email_module = (IEmailModule)getSlot(SLOT_EMAIL_MODULE);
			if(email_module == null)
				throw new InitializationException("EMAIL CONFIRMATION FEATURE REQUIRES THAT YOU PROVIDE AN IEmailModule instance IN SLOT "+SLOT_EMAIL_MODULE);
			
			email_template_name  = GET_REQUIRED_CONFIG_PARAM(PARAM_EMAIL_TEMPLATE_NAME, config);
			email_subject	     = GET_REQUIRED_CONFIG_PARAM(PARAM_EMAIL_SUBJECT, config);			
			activate_account_url = GET_REQUIRED_CONFIG_PARAM(PARAM_ACTIVATE_ACCOUNT_URL, config);
			
			
			user_module.registerLockMessage(LOCKED_PENDING_REGISTRATION, LOCK_MESSAGE);

		}
		else
			do_email_confirmation = false;
	}

	
	public void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_USER_MODULE,UserModule.class,true);
		DEFINE_SLOT(SLOT_EMAIL_MODULE,IEmailModule.class,false,null);
	}
	
	/// B E G I N      M O D U L E      F U N C T I O N S //////////
	@Export(ParameterNames={"email", "username", "password"}) 
	@TransactionProtect
	public Entity Register(UserApplicationContext uctx,String email,String username,String password) throws WebApplicationException,PersistenceException
	{
		Entity creator = (Entity)uctx.getUser();
		Entity user = register(creator,email, username, password); 
		uctx.setUser(user);
		DISPATCH_EVENT(REGISTRATION_EVENT_ACCOUNT_REGISTERED,
			       	   REGISTRATION_EVENT_USER,user);
		return user;
	}
	
	public Entity register(Entity creator,String email,String username,String password,Object... xtra_event_context_params) throws WebApplicationException,PersistenceException
	{
		Entity user = user_module.createSystemUser(creator, email, password, username,xtra_event_context_params); 
		/*creator is null. means system created this user */
		if(do_email_confirmation)
		{	
			user_module.lockUser(user, LOCKED_PENDING_REGISTRATION, "Pending Registration");
			String activation_token = com.pagesociety.util.RandomGUID.getGUID();
			
			NEW(OUTSTANDING_REGISTRATION_CONFIRMATION_ENTITY,null,
				FIELD_ACTIVATION_TOKEN,activation_token,
				FIELD_ACTIVATION_UID,user.getId());
			
			Map<String,Object> template_data = new HashMap<String,Object>();
			template_data.put("user", user);
			template_data.put("username", username);
			template_data.put("email", email);
			template_data.put("activation_token", activation_token);
			/* our flash deep linking needs the # anchor after the query string*/
			String activate_account_url_anchor = null;
			int idx = -1;
			if((idx = activate_account_url.indexOf('#')) != -1)
			{
				activate_account_url_anchor = activate_account_url.substring(idx);
				activate_account_url		= activate_account_url.substring(0, idx);
			}
			template_data.put("activate_account_url", activate_account_url);
			template_data.put("activate_account_url_anchor", activate_account_url_anchor);
			email_module.sendEmail(null, new String[]{email}, email_subject, email_template_name, template_data);
		}

		return user;

	}
	
	@Export(ParameterNames={"token"}) 
	@TransactionProtect
	public Entity ActivateUserAccount(UserApplicationContext uctx,String token) throws WebApplicationException,PersistenceException
	{

		Entity user = null;
		try{
			Query q = new Query(OUTSTANDING_REGISTRATION_CONFIRMATION_ENTITY);
			q.idx(INDEX_BY_ACTIVATION_TOKEN);
			q.eq(token);
			QueryResult result = QUERY(q);
			if (result.size() == 0)
				throw new WebApplicationException("BAD REGISTRATION TOKEN");
			
			Entity activation_record = result.getEntities().get(0);
			user = GET(UserModule.USER_ENTITY, (Long) activation_record.getAttribute(FIELD_ACTIVATION_UID));
			System.out.println("ACTIVATING USER "+user);
			user_module.unlockUser(user);
			DELETE(activation_record);
			// log them in//
			if(uctx != null)
				uctx.setUser(user);
			DISPATCH_EVENT(REGISTRATION_EVENT_ACCOUNT_ACTIVATED,
				       	   REGISTRATION_EVENT_USER,user);

		}catch(Exception e)
		{
			if(uctx != null)
				uctx.setUser(null);
			WAE(e);
		}
		return user;
	}
	
	
	//// E N D       M O D U L E      F U N C T I O N S //////////
	
	
	public static String OUTSTANDING_REGISTRATION_CONFIRMATION_ENTITY = "OutstandingRegConfirmation";
	public static String FIELD_ACTIVATION_TOKEN	= "activation_token";
	public static String FIELD_ACTIVATION_UID   = "activation_uid";
	protected void defineEntities(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY(OUTSTANDING_REGISTRATION_CONFIRMATION_ENTITY,
					  FIELD_ACTIVATION_TOKEN, Types.TYPE_STRING,null, 
					  FIELD_ACTIVATION_UID, Types.TYPE_LONG,null);
					  
	}
	
	public static String INDEX_BY_ACTIVATION_TOKEN	=   "byActivationToken";
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY_INDICES
		(
			OUTSTANDING_REGISTRATION_CONFIRMATION_ENTITY,
			ENTITY_INDEX(INDEX_BY_ACTIVATION_TOKEN, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, FIELD_ACTIVATION_TOKEN)
		);
	}
	
}
