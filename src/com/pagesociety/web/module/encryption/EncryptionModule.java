package com.pagesociety.web.module.encryption;


import java.security.Security;
import java.util.Map;

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
	String secret_key = "everyone has had a broken heart.";
	SecretKeySpec key;
	Cipher cipher;
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());  
		byte[] key_bytes = secret_key.getBytes();
		System.out.println("KEY BYTES LENGTH IS "+key_bytes.length);
		//TODO: have david install unlimited strength encryption policy files
		//and change back to 32....or make it a configurable parameter or module
		//along with pass phrase startup stuff..
		key 		 = new SecretKeySpec(key_bytes,0,16,"AES");
		try{
		cipher		 = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");

		String enc_f = encryptString("FOOFY");
		System.out.println("ENCRYPTING STRING FOOFY "+enc_f);
		System.out.println("DECRYPTING STRING FOOFY "+decryptString(enc_f));
		
		}catch(Exception e)
		{
			throw new InitializationException("FAILED INITIALIZING ENCRYPTION MODULE.COULDNT GET BOUNCEY CASTLE INSTANCE: "+e.getMessage());
		}

		//app.getConfig().getConfigDirectory();
	}

	protected void defineSlots()
	{
		super.defineSlots();
		//DEFINE_SLOT(SLOT_EMAIL_GUARD,IEmailGuard.class,false,DefaultEmailGuard.class);
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
			byte[] cipherText = HexUtil.fromHexString(s);
			int ctLength = cipherText.length;
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] plainText = new byte[cipher.getOutputSize(ctLength)];
			int ptLength = cipher.update(cipherText, 0, ctLength, plainText, 0);
			ptLength += cipher.doFinal(plainText, ptLength);
			//System.out.println("PT LENGTH "+ptLength);
			return new String(plainText,0,ptLength);
		}catch(Exception e)
		{
			e.printStackTrace();
			throw new WebApplicationException("DECRYPTION FAILED DUE TO EXCEPTION. SEE LOGS");
		}
	}
	
    	
	


}
