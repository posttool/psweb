package com.pagesociety.web.module.email;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage; 
import javax.mail.internet.MimeMultipart;


import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.WebModule;
import com.pagesociety.web.template.FreemarkerRenderer;

public class QueuedEmailModule extends WebModule implements IEmailModule 
{	

	private static final String PARAM_SMTP_SERVER 	 	  		 = "smtp-server";
	private static final String PARAM_EMAIL_TEMPLATE_DIR  		 = "email-template-dir";
	private static final String PARAM_EMAIL_RETURN_ADDRESS	     = "email-return-address";
	private static final String PARAM_EMAIL_QUEUE_SIZE		     = "email-queue-size";
	
	protected String smtp_server;
	protected String email_template_dir;
	protected String email_return_address;
	
	protected FreemarkerRenderer fm_renderer;

	private int					email_queue_size = 512;
	private BlockingQueue<queue_obj> email_queue;//this holds the user entity we are registering
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		
		smtp_server 	     = GET_REQUIRED_CONFIG_PARAM(PARAM_SMTP_SERVER, config);
		email_template_dir   = GET_REQUIRED_CONFIG_PARAM(PARAM_EMAIL_TEMPLATE_DIR, config);
		email_return_address = GET_REQUIRED_CONFIG_PARAM(PARAM_EMAIL_RETURN_ADDRESS, config);
		
		File f = new File(email_template_dir);
		if(!f.exists())
			f.mkdirs();
		
		fm_renderer 		 = new FreemarkerRenderer(email_template_dir);
		
		if(config.get(PARAM_EMAIL_QUEUE_SIZE) != null)
			email_queue_size = Integer.parseInt((String)config.get(PARAM_EMAIL_QUEUE_SIZE));
		email_queue         = new ArrayBlockingQueue<queue_obj>(email_queue_size);
		
	}
	
	public void loadbang(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		start_email_thread();
	}


	protected void defineSlots()
	{
		super.defineSlots();
	}

	//permissions
	public static final String CAN_SEND_EMAIL =  "CAN_SEND_EMAIL";
	public void exportPermissions()
	{
		EXPORT_PERMISSION(CAN_SEND_EMAIL);
	}
	
	@Export
	public void SendEmail(UserApplicationContext uctx,String from, List<String> to, String subject,
			String template_name, Map<String, Object> template_data)
			throws PersistenceException,WebApplicationException {
		Entity user = (Entity)uctx.getUser();
		GUARD(user, CAN_SEND_EMAIL);
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
		
		if(email_queue.remainingCapacity() == 0)
		{
			throw new WebApplicationException("EMAIL BUSY. TRY LATER.");
		}
		
		queue_obj qo = new queue_obj();
		qo.from    = from;
		qo.to 	   = to;
		qo.subject = subject;
		qo.template_name = template_name;
		qo.template_data = template_data;

		try{
			email_queue.put(qo);
		}catch(InterruptedException ie)
		{
			ie.printStackTrace();
			//what to do? put back on queue and sleep?
		}
	}
	//// END MODULE STUFF ///
	
	
	private void do_send_mail(String from, String[] to, String subject,String template_name,Map<String,Object> template_data) throws WebApplicationException
	{
		String plaintext_template_name = get_plaintext_template_name(template_name);
		if(fm_renderer.templateExists(plaintext_template_name))
			do_send_mail_html_and_plaintext(from, to, subject, template_name, plaintext_template_name,template_data);
		else
			do_send_mail_html_only(from, to, subject, template_name, template_data);
	}
	
	private String get_plaintext_template_name(String template_name)
	{
		String template_name_before_extension = template_name.substring(0,template_name.lastIndexOf('.'));
		String ext = template_name.substring(template_name.lastIndexOf('.'));
		String plaintext_template_name = template_name_before_extension+"-PLAINTEXT"+ext;
		return plaintext_template_name;
	}
	
	private void do_send_mail_html_only(String from, String[] to, String subject,String template_name,Map<String,Object> template_data) throws WebApplicationException
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
	
	
	private void do_send_mail_html_and_plaintext(String from, String[] to, String subject,String template_name,String plaintext_template_name,Map<String,Object> template_data) throws WebApplicationException
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
		    String PLAINTEXTbody =  fm_renderer.render(plaintext_template_name, template_data);
		    String HTMLbody =  fm_renderer.render(template_name, template_data);
		    

		    MimeMultipart content = new MimeMultipart("alternative");
		    MimeBodyPart text = new MimeBodyPart();
		    MimeBodyPart html = new MimeBodyPart();
		    text.setText( PLAINTEXTbody);
		    text.setHeader("MIME-Version" , "1.0" );
		    text.setHeader("Content-Type" , text.getContentType() );
		    content.addBodyPart(text);
		    
		    html.setContent(HTMLbody, "text/html");
		    html.setHeader("MIME-Version" , "1.0" );
		    html.setHeader("Content-Type" , "text/html" );
		    content.addBodyPart(html);

		    // Setting the Subject and Content Type
		    msg.setContent( content );
		    msg.setHeader("MIME-Version" , "1.0" );
		    msg.setHeader("Content-Type" , content.getContentType() );
		    msg.setHeader("X-Mailer", "Page Society Mailer Beeatch V1.0");
		    msg.setSentDate(new Date());

		    Transport.send(msg);
		    
	    }catch(Exception e)
	    {
	    	e.printStackTrace();
	    	throw new WebApplicationException("AN ERROR OCCURRED.UNABLE TO SEND EMAIL.SEE LOGS.");
	    }
	    
	}
	
	
	private Thread  email_thread;
	private boolean running = false;
	private void start_email_thread()
	{
		running = true;
		email_thread = new Thread()
		{
			public void run()
			{
				while(running)
				{
					queue_obj qo = null;
					try{
						qo = email_queue.take();
					}catch(InterruptedException ie){}
					if(!running)
						break;
					try{
						System.out.println(getName()+" SENDING MAIL");
						do_send_mail(qo.from, qo.to, qo.subject, qo.template_name, qo.template_data);

					}catch(Exception e)
					{
						//TODO:nothing we can really do here
						//maybe put it back on the queue
						e.printStackTrace();
					}
					try{
						Thread.sleep(1000);//600 emails an hour/ 60 a minute/ 1 a second
					}catch(InterruptedException ie)
					{
						
					}
				
				}
				System.out.println(getName()+" QUEUED EMAIL THREAD EXITED");
			}
		};
		//t.setDaemon(true);
		email_thread.start();
	}
	
	class queue_obj
	{
		public String from;
		public String[] to;
		public String subject;
		public String template_name;
		public Map<String, Object> template_data;

	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		running = false;
		email_thread.interrupt();			

	}

}
