package com.pagesociety.web.module.encryption;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.util.HexUtil;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.LoginFailedException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.gateway.RawCommunique;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PermissionsModule;
import com.pagesociety.web.module.RawUIModule;
import com.pagesociety.web.module.WebModule;
import com.pagesociety.web.module.user.UserModule;
import com.pagesociety.web.module.util.Util;

//SECRET KEY AES ENCRYPTION//
//SEE:
//http://www.java2s.com/Code/Java/Security/EncryptionanddecryptionwithAESECBPKCS7Padding.htm
//
//TODO: add methods for encrypting/decrypting a whole file//
public class EncryptionModule extends WebModule implements IEncryptionModule
{	
	
	private static final String PARAM_ENCRYPTION_STRENGTH = "encryption-strength";//num of chars used in phrase 32 is 256 bit//
	public static final String VERIFY_KEY_DATA_FILENAME = "000";
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
		
		set_secret_phrase(secret_phrase);
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
	

	public void setSecretKeyPhrase(String phrase) throws WebApplicationException
	{
		String v_word = null;
		int v_encryption_strength = 0;
		try{
			File key_test_file = GET_MODULE_DATA_FILE(getApplication(),VERIFY_KEY_DATA_FILENAME,false);
			BufferedReader reader = new BufferedReader(new FileReader(key_test_file));
			v_encryption_strength = Integer.parseInt(reader.readLine());
			if(v_encryption_strength != encryption_strength)
				throw new WebApplicationException("ENCRYPTION STRENGTHS DONT MATCH IN MODULE SPECIFICATION AND WHAT IS ON DISK. ON DISK: "+v_encryption_strength+" IN MODULE: "+encryption_strength);
			v_word  = reader.readLine();
		}catch(Exception e)
		{
			ERROR(e);
			throw new WebApplicationException("SET KEY FAILED.");
		}
		
		if(phrase.length() < encryption_strength)
			throw new WebApplicationException("PHRASE IS TOO SHORT. ENCRYPTION STRENGTH IS "+encryption_strength+" AND PHRASE LENGTH IS "+phrase.length());

		set_secret_phrase(phrase);	
		try{
			decryptString(v_word);
		}catch(Exception e)
		{
			clearKey();
			throw new WebApplicationException("DECRYPT FAILED. SETTING KEY TO NULL.TRY AGAIN.");
		}		

	}
	
	private void set_secret_phrase(String phrase)
	{
		secret_phrase_bytes = new byte[phrase.length()];
		System.arraycopy(phrase.getBytes(), 0, secret_phrase_bytes, 0, secret_phrase_bytes.length);
		key 		 = new SecretKeySpec(secret_phrase_bytes,0,encryption_strength,"AES");		
	}
	
	public int getEncryptionStrength()
	{
		return encryption_strength;
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
	

}
