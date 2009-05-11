package com.pagesociety.web.module.registration;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.WebModule;

public class RestrictListModule extends WebModule
{
	public static final String PARAM_RESTRICT_LIST = "restrict-list-file";
	
	Set<String> allowed_email_addresses;
	Set<String> allowed_domains;
	String restrict_list_filename;
	long current_resrict_list_length;
	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);
		allowed_email_addresses = new HashSet<String>();
		allowed_domains 		= new HashSet<String>();
		restrict_list_filename  = GET_REQUIRED_CONFIG_PARAM(PARAM_RESTRICT_LIST, config);
		File rl = new File(restrict_list_filename);
		try{
		
		if(!rl.exists())
		{
			rl.createNewFile();
			//throw new InitializationException("UNABLE TO FIND RESTRICT FILE "+restrict_list_filename);
		}
		load_restrict_list();
		}catch(Exception e)
		{
			throw new InitializationException("PROBLEM LOADING RESTRICT LIST "+restrict_list_filename,e);
		}
	}

	
	public void load_restrict_list() throws WebApplicationException
	{
		File rl = new File(restrict_list_filename);
		current_resrict_list_length = rl.length();
		allowed_email_addresses.clear();
		allowed_domains.clear();
	
		try{
			BufferedReader fin = new BufferedReader(new FileReader(rl));
			String line;
			while((line = fin.readLine()) != null) 
			{
				if(line.trim().equals(""))
					continue;
				if(line.startsWith("*"))
					allowed_domains.add(line.toLowerCase().substring(line.indexOf("@")));
				else
					allowed_email_addresses.add(line.toLowerCase());
			}
		}catch(Exception e)
		{
			throw new WebApplicationException("FAILED LOADING RESTRICT LIST",e);
		}
	}
	
	public synchronized boolean isAllowed(String email) throws WebApplicationException
	{
		if(email == null || email.indexOf("@") == -1)
			return false;
		if(allowed_email_addresses.contains(email.toLowerCase()))
			return true;
		if(allowed_domains.contains(email.toLowerCase().substring(email.indexOf("@"))))
			return true;
		else
		{
			//make sure the file doesnt need to be reloaded//
			File rl = new File(restrict_list_filename);
			if(rl.length() == current_resrict_list_length)
				return false;
			load_restrict_list();
			return isAllowed(email);
		}
	}
}
