package com.pagesociety.web.module.persistence;


import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.pagesociety.bdb.BDBStore;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.PersistentStore;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SlotException;
import com.pagesociety.web.module.Module;
import com.pagesociety.web.module.ModuleDefinition;
import com.pagesociety.web.module.ModuleRegistry;
import com.pagesociety.web.module.WebModule;
import com.pagesociety.web.module.WebStoreModule;

public class BDBPersistenceModule extends WebModule implements IPersistenceProvider
{
	private static final String PARAM_STORE_ROOT_DIRECTORY = "store-root-directory";
	private static final String SLOT_EVOLUTION_PROVIDER    = "evolution-provider";
	
	private PersistentStore    bdb_store;
	private IEvolutionProvider evolution_provider;


	public void system_init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.system_init(app,config);
		
		String root_dir = (String)config.get(PARAM_STORE_ROOT_DIRECTORY);
		if(root_dir == null)
			throw new InitializationException("BDBPersistenceModule requires paramter "+PARAM_STORE_ROOT_DIRECTORY);
		
		File f = new File(root_dir);
		if(!f.exists())
			f.mkdirs();
		bdb_store = new BDBStore();
		try{
			bdb_store.init(config);	
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
		Module evo_module = ModuleRegistry.instantiate(default_evolution_provider_def,evo_params);
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
		return bdb_store;
	}

	public IEvolutionProvider getEvolutionProvider() 
	{
		//return null;
		return evolution_provider;
	}
}
