package com.pagesociety.web.module.persistence;


import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pagesociety.bdb.BDBStore;
import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.PersistentStore;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.PermissionsException;
import com.pagesociety.web.exception.SlotException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.Module;
import com.pagesociety.web.module.ModuleDefinition;
import com.pagesociety.web.module.ModuleRegistry;
import com.pagesociety.web.module.WebModule;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.permissions.PermissionsModule;

public class BDBPersistenceModule extends WebModule implements IPersistenceProvider
{
	private static final String PARAM_STORE_ROOT_DIRECTORY = "store-root-directory";
	private static final String PARAM_STORE_BACKUP_DIRECTORY = "store-backup-directory";
	private static final String SLOT_EVOLUTION_PROVIDER    = "evolution-provider";
	
	private PersistentStore    store;
	private IEvolutionProvider evolution_provider;
	
	public static final String CAN_DO_BACKUP  = "CAN_DO_BACKUP";
	public static final String CAN_DO_RESTORE = "CAN_DO_RESTORE";

	public void system_init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.system_init(app,config);
		
		String root_dir   = GET_REQUIRED_CONFIG_PARAM(PARAM_STORE_ROOT_DIRECTORY,config);
		String backup_dir = GET_OPTIONAL_CONFIG_PARAM(PARAM_STORE_BACKUP_DIRECTORY,config);
		System.out.println("BACKUP DIR IS "+backup_dir);
		File f;
		f = new File(root_dir);
		if(!f.exists())
			f.mkdirs();
		
		if(backup_dir != null)
		{
			f = new File(backup_dir);
			if(!f.exists())
			f.mkdirs();
		}
		
		store = new BDBStore();
		try{
			store.init(config);	
		}catch(PersistenceException pe)
		{
			pe.printStackTrace();
			throw new InitializationException("UNABLE TO INITIALIZE BDB STORE WITH CONFIG "+config,pe);
		}	
			
		setup_evolution_provider(app);
	}
	
	//we need to manually setup this slot because it is not declared in applicatin.xml//
	//allows a default and multiple versions of evolution as modules 
	//TODO: this crap should be a macro SETUP_SLOT
	private void setup_evolution_provider(WebApplication app) throws InitializationException
	{
		evolution_provider = ((IEvolutionProvider)getSlot(SLOT_EVOLUTION_PROVIDER));
		if(evolution_provider != null)
			return;
		
		//This is just like putting something in application.xml
		ModuleDefinition default_evolution_provider_def = ModuleRegistry.register(getName()+" DEFAULT_EVOLUTION_PROVIDER", DefaultPersistenceEvolver.class);
		//THIS SHIT SHOULD BE WRAPPED UP IN A MACRO NEXT TIME WE DO SOMEHTINNG LIKE THIS
		Map<String,Object> evo_params = new HashMap<String, Object>();
		Module evo_module = ModuleRegistry.instantiate(app,default_evolution_provider_def,evo_params);
		try{
			((WebStoreModule)evo_module).setSlot(WebStoreModule.SLOT_STORE,this);
			((WebModule)evo_module).system_init(app,evo_module.getParams());
			((WebModule)evo_module).init(app,evo_module.getParams());
		}catch(SlotException se)
		{
			ERROR(se);
			throw new InitializationException(getName()+ ": FAILED SETTING STORE SLOT IN EVOLUTION PROVIDER");
		}
		evolution_provider = (IEvolutionProvider)evo_module;
	}

	public void defineSlots()
	{
		defineSlot(SLOT_EVOLUTION_PROVIDER,IEvolutionProvider.class,false,null);
	}
	
	public PersistentStore getStore()
	{
		return store;
	}

	public IEvolutionProvider getEvolutionProvider() 
	{
		//return null;
		return evolution_provider;
	}
	
	
	public void loadbang(WebApplication app,Map<String,Object> params) throws InitializationException
	{
		try{
			System.out.println("STORE SUPPORTS FULL BACKUP: "+store.supportsFullBackup());
			System.out.println("STORE SUPPORTS INCREMENTAL: "+store.supportsIncrementalBackup());
			if(store.supportsFullBackup())
			{
				System.out.println("CURRENT BACKUPS: ");
				String[] backup_identifiers = store.getBackupIdentifiers();
				for(int i = 0;i < backup_identifiers.length;i++)
					System.out.println("\t"+backup_identifiers[i]);
			}
		}catch(PersistenceException pe)
		{
			pe.printStackTrace();
			throw new InitializationException("FAILED....");
		}
	}
	
	@Export
	public List<String> GetBackupIdentifiers(UserApplicationContext uctx) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(user,CAN_DO_BACKUP);
		return getBackupIdentifiers();
	}
	
	public List<String> getBackupIdentifiers() throws PersistenceException
	{
		return Arrays.asList(store.getBackupIdentifiers());
	}
	
	@Export
	public String DoFullBackup(UserApplicationContext uctx) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(user,CAN_DO_BACKUP);
		return doFullBackup();
	}
	
	public String doFullBackup() throws PersistenceException
	{
		return store.doFullBackup();
	}

	@Export
	public String DoIncrementalBackup(UserApplicationContext uctx,String fullbackup_token) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(user,CAN_DO_BACKUP);
		return doIncrementalBackup(fullbackup_token);
	}
	
	public String doIncrementalBackup(String fullbackup_token) throws PersistenceException
	{
		return store.doIncrementalBackup(fullbackup_token);
	}
	
	@Export
	public void RestoreFromBackup(UserApplicationContext uctx,String fullbackup_token) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(user,CAN_DO_RESTORE);
		restoreFromBackup(fullbackup_token);
	}
	
	public void restoreFromBackup(String fullbackup_token) throws PersistenceException
	{
		store.restoreFromBackup(fullbackup_token);
	}
	
	
	@Export
	public void DeleteBackup(UserApplicationContext uctx,String fullbackup_token) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(user,CAN_DO_BACKUP);
		deleteBackup(fullbackup_token);
	}
	
	public void deleteBackup(String fullbackup_token) throws PersistenceException
	{
		store.deleteBackup(fullbackup_token);
	}

	public String getStatistics()
	{
		return ((BDBStore)store).getStatistics();
	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			System.out.println("STORE IS ABOUT TO BE DESTROYED");
			store.close();
			System.out.println("STORE IS DESTROYED");
		} catch (PersistenceException e) {
			ERROR("FAILED CLOSING STORE "+store);
			e.printStackTrace();
		}
	}
}
