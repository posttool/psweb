package com.pagesociety.web.module.email;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage; 


import com.pagesociety.persistence.Entity;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.WebModule;
import com.pagesociety.web.template.FreemarkerRenderer;



public class EmailModule extends WebModule implements IEmailModule 
{	
	private static final String SLOT_EMAIL_GUARD				 = "email-guard";
	private static final String PARAM_SMTP_SERVER 	 	  		 = "smtp-server";
	private static final String PARAM_EMAIL_TEMPLATE_DIR  		 = "email-template-dir";
	private static final String PARAM_EMAIL_RETURN_ADDRESS	     = "email-return-address";
	
	protected IEmailGuard guard;
	protected String smtp_server;
	protected String email_template_dir;
	protected String email_return_address;
	
	protected FreemarkerRenderer fm_renderer;

	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		
		smtp_server 	     = GET_REQUIRED_CONFIG_PARAM(PARAM_SMTP_SERVER, config);
		email_template_dir   = GET_REQUIRED_CONFIG_PARAM(PARAM_EMAIL_TEMPLATE_DIR, config);
		email_return_address = GET_REQUIRED_CONFIG_PARAM(PARAM_EMAIL_RETURN_ADDRESS, config);
		fm_renderer 		 = new FreemarkerRenderer(email_template_dir);
		guard = (IEmailGuard)getSlot(SLOT_EMAIL_GUARD);
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_EMAIL_GUARD,IEmailGuard.class,false,DefaultEmailGuard.class);
	}

	@Export
	public void SendEmail(UserApplicationContext uctx,String from, List<String> to, String subject,
			String template_name, Map<String, Object> template_data)
			throws WebApplicationException { 
		Entity user = (Entity)uctx.getUser();
		GUARD(guard.canSendEmail(user));
		String[] s_to = new String[to.size()];
		s_to = to.toArray(s_to);
		sendEmail(from, s_to, subject, template_name, template_data);
	}


	public void sendEmail(String from, String[] to, String subject,
			String template_name, Map<String, Object> template_data)
			throws WebApplicationException 
	{
		File template_file = new File(email_template_dir,template_name);
		if(!template_file.exists())
			throw new WebApplicationException("CANT SEND EMAIL.TEMPLATE: "+email_template_dir+"/"+template_name+" DOES NOT EXIST");
	
		if(to == null)
			throw new  WebApplicationException("CANT SEND EMAIL TO A NULL ADDRESS");
		
		if(from == null)
			from = email_return_address;
		
		if(subject == null)
			subject = "";
		
		do_send_mail(from,to,subject,template_name,template_data);
		
	}
	
	private void do_send_mail(String from, String[] to, String subject,String template_name,Map<String,Object> template_data) throws WebApplicationException
	{
	    boolean debug = false;
	    try{
	    	
		     //Set the host smtp address
		     Properties props = new Properties();
		     props.put("mail.transport.protocol", "smtp");
		     props.put("mail.smtp.host", smtp_server);
		
		    // create some properties and get the default Session
		    Session session = Session.getDefaultInstance(props, null);
		    session.setDebug(debug);
	
		    // create a message
		    MimeMessage msg = new MimeMessage(session);
		    msg.setSubject(subject);
		    
		    // set the from and to address
		    
		    InternetAddress addressFrom = new InternetAddress(from);
		    msg.setFrom(addressFrom);
		  
		    InternetAddress[] tos = new InternetAddress[to.length];
		    for(int i = 0;i < to.length;i++)
		    {
		    	tos[i] = new InternetAddress(to[i]); 
		    }
		    msg.setRecipients(Message.RecipientType.TO,tos);
			
		    //expand the body
		    String body =  fm_renderer.render(template_name, template_data);
		    // Setting the Subject and Content Type
		    msg.setContent(body, "text/html");
		    Transport.send(msg);
		    
	    }catch(Exception e)
	    {
	    	e.printStackTrace();
	    	throw new WebApplicationException("AN ERROR OCCURRED.UNABLE TO SEND EMAIL.SEE LOGS.");
	    }
	    
	}
}
