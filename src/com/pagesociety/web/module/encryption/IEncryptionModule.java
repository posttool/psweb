package com.pagesociety.web.module.encryption;

import com.pagesociety.web.exception.WebApplicationException;

public interface IEncryptionModule 
{
	
	public String encryptString(String s) throws WebApplicationException;
	public String decryptString(String s) throws WebApplicationException;
	public boolean isConfigured();
	public void setSecretKeyPhrase(String secret_key_phrase) throws WebApplicationException;
}
