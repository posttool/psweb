package com.pagesociety.web.module.persistence;


import java.util.Map;

import com.pagesociety.bdb.BDBStore;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.PersistentStore;
import com.pagesociety.web.InitializationException;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.module.Module;

public class BDBPersistenceModule extends Module implements IPersistenceProvider
{
	private static final String PARAM_STORE_ROOT_DIRECTORY = "store-root-directory";
	
	private PersistentStore bdb_store;
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		String root_dir = (String)config.get(PARAM_STORE_ROOT_DIRECTORY);
		
		if(root_dir == null)
			throw new InitializationException("BDBPersistenceModule requires paramter "+PARAM_STORE_ROOT_DIRECTORY);
		
		bdb_store = new BDBStore();
		try{
			bdb_store.init(config);	
		}catch(PersistenceException pe)
		{
			pe.printStackTrace();
			throw new InitializationException("UNABLE TO INITIALIZE BDB STORE WITH CONFIG "+config,pe);
		}
	}
	
	public PersistentStore getStore()
	{
		return bdb_store;
	}
}
