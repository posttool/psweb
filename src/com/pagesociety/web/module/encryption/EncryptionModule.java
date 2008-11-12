package com.pagesociety.web.module.encryption;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Security;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.util.HexUtil;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.WebModule;

//SECRET KEY AES ENCRYPTION//
//SEE:
//http://www.java2s.com/Code/Java/Security/EncryptionanddecryptionwithAESECBPKCS7Padding.htm
//
//TODO: add methods for encrypting/decrypting a whole file//
public class EncryptionModule extends WebModule
{	
	
	private static final String PARAM_ENCRYPTION_STRENGTH = "encryption-strength";//num of chars used in phrase 32 is 256 bit//
	private int encryption_strength;
	private String secret_key;
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
		
		try{
			BufferedReader reader = new BufferedReader(new FileReader(key_test_file));
			String v_question = reader.readLine();
			String ev_answer  = reader.readLine();
		while(true)
		{
			String phrase = GET_CONSOLE_INPUT("Enter secret phrase for Encryption Module: "+getName()+">\n>");
			if(phrase.length() < encryption_strength)
			{
					System.out.println("Phrase must be at least "+encryption_strength+" characters long.");
					continue;
			}
			
			set_secret_key_phrase(phrase);
			String decrypted_answer;
			
			try{
				decrypted_answer = decryptString(ev_answer);
			}catch(WebApplicationException bpe)
			{
				System.out.println("BAD KEY.");
				continue;
			}
			String response = GET_CONSOLE_INPUT("Does this make sense(Y,N)?\nQ: "+v_question+"\nA: "+decrypted_answer);
			if(response.equalsIgnoreCase("N"))
				continue;
			else
				break;
		}	
		}catch(Exception e)
		{
			throw new WebApplicationException(getName()+": PROBLEM READING KEY VERIFICATION FILE.");
		}
	}
	
	private void setup_initial_secret_key_v_file(WebApplication app,File key_test_file) throws WebApplicationException
	{
		String response = GET_CONSOLE_INPUT("Encryption module "+getName()+" needs to be setup. Are you ready?(Y/N)>\n>");
		if(response.equalsIgnoreCase("N"))
			throw new WebApplicationException("UNABLE TO START ENCRYPTION MODULE "+getName()+". USER CANCELLED.");
		
		String v_question = GET_CONSOLE_INPUT("Provide verification question.>\n>");
		String v_data     = GET_CONSOLE_INPUT("Provide verification response.>\n>");
		
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
			fw.write(v_question+"\n");
			fw.write(encryptString(v_data)+"\n");
			fw.close();
		}catch(IOException ioe)
		{
			ioe.printStackTrace();
			throw new WebApplicationException("FAILED WRITING SECRET KEY VERIFICATION FILE.");
		}		
	}
	

	private void set_secret_key_phrase(String phrase)
	{
		secret_key = phrase;
		key 		 = new SecretKeySpec(secret_key.getBytes(),0,encryption_strength,"AES");
	}
		



	@Export
	public String EncryptString(UserApplicationContext uctx,String s) throws PersistenceException,WebApplicationException 
	{ 
		return null;
	}
	
	public String encryptString(String s) throws WebApplicationException
	{
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
		try{ 
			System.out.println("KEY IS "+secret_key);
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
	
    	
}
