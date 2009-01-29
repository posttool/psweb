package com.pagesociety.web.module.encryption;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Security;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.util.HexUtil;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.gateway.RawCommunique;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.WebModule;

//SECRET KEY AES ENCRYPTION//
//SEE:
//http://www.java2s.com/Code/Java/Security/EncryptionanddecryptionwithAESECBPKCS7Padding.htm
//
//TODO: add methods for encrypting/decrypting a whole file//
public class EncryptionModule extends WebModule implements IEncryptionModule
{	
	
	private static final String PARAM_ENCRYPTION_STRENGTH = "encryption-strength";//num of chars used in phrase 32 is 256 bit//
	private int encryption_strength;
	private byte[] secret_phrase_bytes;
	private SecretKeySpec key;
	private Cipher cipher;
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		encryption_strength = Integer.parseInt(GET_REQUIRED_CONFIG_PARAM(PARAM_ENCRYPTION_STRENGTH, config));
		
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());  
		try{
			cipher		 = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");
			setup_secret_key(app);
		}catch(Exception e)
		{
			e.printStackTrace();
			throw new InitializationException("FAILED INITIALIZING ENCRYPTION MODULE.COULDNT GET BOUNCEY CASTLE INSTANCE: "+e.getMessage());
		}
	}

	private static final String VERIFY_KEY_DATA_FILENAME = "000";
	private void setup_secret_key(WebApplication app) throws WebApplicationException,InitializationException
	{
		File key_test_file = GET_MODULE_DATA_FILE(app, VERIFY_KEY_DATA_FILENAME,false);
		if(key_test_file == null) 
		{
			setup_initial_secret_key_v_file(app, key_test_file);
			return;
		}
	}
	
	private void setup_initial_secret_key_v_file(WebApplication app,File key_test_file) throws WebApplicationException
	{
		String response = GET_CONSOLE_INPUT("Encryption module "+getName()+" needs to be setup. Are you ready?(Y/N)>\n>");
		if(response.equalsIgnoreCase("N"))
			throw new WebApplicationException("UNABLE TO START ENCRYPTION MODULE "+getName()+". USER CANCELLED.");
		
		String v_word = GET_CONSOLE_INPUT("Type any word.>\n>");

		String secret_phrase;
		while(true)
		{
			secret_phrase = GET_CONSOLE_INPUT("Choose encryption phrase.\n "+"" +
					"It must be at least "+encryption_strength+" characters long."+
					"IT WILL BE REQUIRED TO START THE SYSTEM "+
					"THE NEXT TIME YOU START UP!!!>\n>");
			if(secret_phrase.length() < encryption_strength)
				continue;
			else
				break;
		}
		
		set_secret_key_phrase(secret_phrase);
		key_test_file = CREATE_MODULE_DATA_FILE(app, VERIFY_KEY_DATA_FILENAME);
		try{
			FileWriter fw = new FileWriter(key_test_file);
			fw.write(encryption_strength+"\n");
			fw.write(encryptString(v_word)+"\n");
			fw.close();
		}catch(IOException ioe)
		{
			ioe.printStackTrace();
			throw new WebApplicationException("FAILED WRITING SECRET KEY VERIFICATION FILE.");
		}		
	}
	

	private void set_secret_key_phrase(String phrase)
	{
		secret_phrase_bytes = new byte[phrase.length()];
		System.arraycopy(phrase.getBytes(), 0, secret_phrase_bytes, 0, secret_phrase_bytes.length);
		key 		 = new SecretKeySpec(secret_phrase_bytes,0,encryption_strength,"AES");
	}
		
//////////////MODULE FUNCTIONS/////////////////////////////////////////////////
	@Export
	public String EncryptString(UserApplicationContext uctx,String s) throws PersistenceException,WebApplicationException 
	{ 
		return null;
	}
	
	public String encryptString(String s) throws WebApplicationException
	{
		if(!isConfigured())
			throw new WebApplicationException(getName()+" IS NOT CONFIGURED");
		try{
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] input = s.getBytes();
			byte[] cipherText = new byte[cipher.getOutputSize(input.length)];
			int ctLength = cipher.update(input, 0, input.length, cipherText, 0);
			ctLength += cipher.doFinal(cipherText, ctLength);
			//System.out.println("CT LENGTH "+ctLength);
			return new String(HexUtil.toHexString(cipherText));
		}catch(Exception e)
		{
			e.printStackTrace();
			throw new WebApplicationException("ENCRYPTION FAILED DUE TO EXCEPTION. SEE LOGS");
		}
	}
	
	@Export
	public String DecryptString(UserApplicationContext uctx,String s) throws PersistenceException,WebApplicationException 
	{ 
		return null;
	}

	public String decryptString(String s) throws WebApplicationException
	{
		if(!isConfigured())
			throw new WebApplicationException(getName()+" IS NOT CONFIGURED");
		try{ 
			byte[] cipherText = HexUtil.fromHexString(s);
			int ctLength = cipherText.length;
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] plainText = new byte[cipher.getOutputSize(ctLength)];
			int ptLength = cipher.update(cipherText, 0, ctLength, plainText, 0);
			ptLength += cipher.doFinal(plainText, ptLength);
			//System.out.println("PT LENGTH "+ptLength);
			return new String(plainText,0,ptLength);
		}
		catch(BadPaddingException bpe)
		{
			throw new WebApplicationException("BAD PADDING EXCEPTION.PROBABLY BAD KEY");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new WebApplicationException("DECRYPTION FAILED DUE TO EXCEPTION. SEE LOGS");
		}
	}
	
	public boolean isConfigured()
	{
		return (secret_phrase_bytes != null && key != null);
	}
	
	public void clearKey()
	{
		secret_phrase_bytes = null;
		key = null;
	}
	
	@Export
	public void configure(UserApplicationContext uctx, RawCommunique c) 
	{
		HttpServletRequest request   = null;
		HttpServletResponse response = null;

		try {
			request   = (HttpServletRequest)c.getRequest();
			response = (HttpServletResponse)c.getResponse();
			StringBuilder buf = new StringBuilder();
			document_start(buf, getName(), "black", "arial", "aqua", 14);
			if(secret_phrase_bytes != null)
			{
				buf.append("OK "+getName()+" IS CONFIGURED.");
				response.getWriter().println(buf.toString());
				return;
			}
			else
			{
				File key_test_file = GET_MODULE_DATA_FILE(getApplication(), VERIFY_KEY_DATA_FILENAME,false);
				BufferedReader reader = new BufferedReader(new FileReader(key_test_file));
				int v_encryption_strength = Integer.parseInt(reader.readLine());
				if(v_encryption_strength != encryption_strength)
					throw new WebApplicationException("ENCRYPTION STRENGTHS DONT MATCH IN MODULE SPECIFICATION AND WHAT IS ON DISK. ON DISK: "+v_encryption_strength+" IN MODULE: "+encryption_strength);

				String v_word  = reader.readLine();
				
				String secret_phrase = request.getParameter("secret_phrase");
				if(secret_phrase != null && !secret_phrase.equals(""))
				{
					if(secret_phrase.length() < encryption_strength)
					{
						buf.append("ERROR: SECRET PHRASE MUST BE AT LEAST "+encryption_strength+" CHARACTERS LONG. TRY AGAIN");		
						p(buf);
						render_phrase_form(buf);
						document_end(buf);
						response.getWriter().println(buf.toString());
						return;
					}
						set_secret_key_phrase(secret_phrase);
					try{
						decryptString(v_word);
					}catch(Exception e)
					{
						clearKey();
						buf.append("ERROR: BAD KEY. TRY AGAIN.");
						p(buf);
						render_phrase_form(buf);
						document_end(buf);
						response.getWriter().println(buf.toString());
						return;
					}
					p(buf);
					buf.append(getName()+" IS CONFIGURED AND READY TO ENCRYPT/DECRYPT.");
					document_end(buf);
					response.getWriter().println(buf.toString());
					return;
				}
				else
				{
					render_phrase_form(buf);
					document_end(buf);
					response.getWriter().println(buf.toString());
					return;
				}
			}

		} catch (Exception e) 
		{
			ERROR(e);
			try{
				response.getWriter().println("<font color='red'>ERROR: "+e.getClass().getName()+" "+e.getMessage());
			}catch(IOException ioe)
			{
				ERROR(ioe);
			}
		}
	}
	
	private void render_phrase_form(StringBuilder buf)
	{
		form_start(buf, getApplication().getConfig().getWebRootUrl()+"/"+getName()+"/configure/.raw", "post");
		table_start(buf, 1, 400);
		tr_start(buf);
		td(buf, "ENTER SECRET PHRASE:");td_start(buf);form_password_field(buf, "secret_phrase",32);td_end(buf);
		tr_end(buf);
		table_end(buf);
		form_submit_button(buf, "submit");
		form_end(buf);						
	}
	
	protected void document_start(StringBuilder buf,String title,String bgcolor,String font_family,String font_color,int font_size)
	{
		html_start(buf);
		head_start(buf,title);
		head_end(buf);
		body_start(buf, bgcolor, font_family, font_color, font_size);
		
	}
	
	protected void document_end(StringBuilder buf)
	{
		body_end(buf);
		html_end(buf);		
	}
	
	protected void html_start(StringBuilder buf)
	{
		buf.append("<HTML>\n");
	}
	
	protected void head_start(StringBuilder buf,String title)
	{
		buf.append("<HEAD> \n<TITLE>"+title+"</TITLE>\n");
	}
	
	protected void head_end(StringBuilder buf)
	{
		buf.append("</HEAD>");
	}
	
	
	protected void body_start(StringBuilder buf,String bgcolor,String font_family,String font_color,int font_size)
	{
		if(font_family == null)
			font_family = "arial";
		buf.append("<BODY style='background-color:"+bgcolor+";font-family:"+font_family+";color:"+font_color+";font-size:"+String.valueOf(font_size)+"px'>");
	}
	
	protected void form_start(StringBuilder buf,String action,String method)
	{
		buf.append("<FORM ACTION='"+action+"' METHOD='"+method+"'>\n");
		
	}	  
	
	protected void form_input_field(StringBuilder buf,String name,int size)
	{
		buf.append("<INPUT TYPE='text' name='"+name+"' size='"+size+"'/>\n");
	}
	
	protected void form_password_field(StringBuilder buf,String name,int size)
	{
		buf.append("<INPUT TYPE='password' name='"+name+"' size='"+size+"'/>\n");
	}
	
	protected void form_hidden_field(StringBuilder buf,String name,String value)
	{
		buf.append("<INPUT TYPE='hidden' name='"+name+"' value='"+value+"'/>\n");
	}
	
	protected void form_submit_button(StringBuilder buf,String label)
	{
		buf.append("<INPUT TYPE='submit' value='"+label+"'/>");
	}
	
	protected void form_end(StringBuilder buf)
	{
		buf.append("</FORM>\n");
	}
	
	protected void body_end(StringBuilder buf)
	{
		buf.append("</BODY>\n");
	}
	protected void html_end(StringBuilder buf)
	{
		buf.append("</HTML>\n");
	}
    
	protected void br(StringBuilder buf)
	{
		buf.append("<BR/>\n");
	}
	
	protected void p(StringBuilder buf)
	{
		buf.append("<P/>\n");
	}
	
	protected void table_start(StringBuilder buf,int border,int width)
	{
		buf.append("<TABLE BORDER='"+border+"' WIDTH='"+width+"'>\n");
	}
	
	protected void tr_start(StringBuilder buf)
	{
		buf.append("<TR>\n");
	}
	
	protected void td(StringBuilder buf,String data)
	{
		buf.append("<TD>"+data+"</TD>\n");		
	}
	
	protected void td_start(StringBuilder buf)
	{
		buf.append("<TD>\n");		
	}
	
	protected void td_end(StringBuilder buf)
	{
		buf.append("</TD>\n");		
	}

	
	protected void tr_end(StringBuilder buf)
	{
		buf.append("</TR>\n");
	}
	
	protected void table_end(StringBuilder buf)
	{
		buf.append("</TABLE>\n");				
	}
}
