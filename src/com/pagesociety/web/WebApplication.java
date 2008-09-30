package com.pagesociety.web;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;

import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.PersistentStore;
import com.pagesociety.web.bean.Beans;
import com.pagesociety.web.config.WebApplicationInitParams;
import com.pagesociety.web.config.ModuleInitParams.ModuleInfo;
import com.pagesociety.web.config.StoreInitParams.StoreInfo;
import com.pagesociety.web.config.UrlMapInitParams.UrlMapInfo;
import com.pagesociety.web.module.Module;
import com.pagesociety.web.module.ModuleRegistry;
import com.pagesociety.web.module.ModuleRequest;
import com.pagesociety.web.module.Module.SlotDescriptor;
import com.pagesociety.web.template.FreemarkerRenderer;

public abstract class WebApplication
{
	private static final Logger logger = Logger.getLogger(WebApplication.class);
	private static WebApplication _instance;
	private Map<String, PersistentStore> _stores;
	private WebApplicationInitParams _config;
	private Map<String, Module> _module_instances;
	private List<Module> _module_list;
	private SessionNameSpaceManager _sess_name_space_mgr;

	public WebApplication() throws InitializationException
	{
		if (_instance != null)
			throw new InitializationException("The WebApplication has already been initialized [" + getClass().getName() + "]");
		_stores 				= new HashMap<String, PersistentStore>();
		_module_instances 		= new HashMap<String, Module>();
		_module_list 			= new ArrayList<Module>();
		_sess_name_space_mgr 	= new SessionNameSpaceManager();
		_instance 				= this;
	}

	public void init(WebApplicationInitParams config) throws InitializationException
	{
		_config = config;
		//
		Beans.initDefault();
		FreemarkerRenderer.init(_config.getWebRootDir());
		ModuleRegistry.init(this);
		//
		registerStores();
		registerAndLinkModules();
		registerUrls();
	}

	public static WebApplication getInstance()
	{
		return _instance;
	}

	public UserApplicationContext getUserContext(String name_space, String sess_id)
	{
		SessionManager sess_mgr = _sess_name_space_mgr.get(name_space);
		UserApplicationContext user_context = (UserApplicationContext) sess_mgr.get(sess_id);
		if (user_context != null)
		{
			user_context.increment();
			return user_context;
		}
		else
		{
			user_context = new UserApplicationContext();
			user_context.setId(sess_id);
			sess_mgr.put(sess_id, user_context);
			return (UserApplicationContext) sess_mgr.get(sess_id);
		}
	}

	public List<Module> getModules()
	{
		return _module_list;
	}

	public Map<String, Module> getModuleMap()
	{
		return _module_instances;
	}

	public Module getModule(String name)
	{
		return _module_instances.get(name);
	}

	public Object dispatch(ModuleRequest request) throws WebApplicationException,
			Exception
	{
		Module module = getModule(request.getModuleName());
		if (module == null)
			throw new WebApplicationException("WebApplication.dispatch MODULE " + request.getModuleName() + " DOES NOT EXIST");
		Object return_value = ModuleRegistry.invoke(module, request.getMethodName(), request.getUserContext(), request.getArguments());
		request.setResult(return_value);
		return return_value;
	}

	public Object execute( UserApplicationContext user_context,String moduleAndMethod,
			Object... args) throws Exception
	{
		String[] mm = moduleAndMethod.split("/");
		if (mm.length != 2)
			throw new RuntimeException("INCORRECTLY FORMATTED REQUEST " + moduleAndMethod);
		ModuleRequest req = new ModuleRequest();
		req.setModuleName(mm[0]);
		req.setMethodName(mm[1]);
		req.setUserContext(user_context);
		req.setArguments(args);
		return dispatch(req);
	}

	public Object execute(UserApplicationContext user_context,String moduleAndMethod, 
			freemarker.template.SimpleSequence args) throws Exception
	{
		Object[] obj_args = new Object[args.size()];
		for (int i = 0; i < obj_args.length; i++)
		{
			Object arg = args.get(i);
			if (arg instanceof freemarker.template.SimpleNumber)
			{
				freemarker.template.SimpleNumber sn = (freemarker.template.SimpleNumber) arg;
				Number n = sn.getAsNumber();
				obj_args[i] = n.intValue(); // TODO know the method signature
			}
			else if (arg instanceof freemarker.template.SimpleScalar)
			{
				freemarker.template.SimpleScalar ss = (freemarker.template.SimpleScalar) arg;
				obj_args[i] = ss.getAsString();
			}
			else if (arg instanceof freemarker.template.SimpleDate)
			{
				freemarker.template.SimpleDate sd = (freemarker.template.SimpleDate) arg;
				obj_args[i] = sd.getAsDate();
			}
			else
			{
				obj_args[i] = args.get(i);
			}
		}
		
		return execute(user_context,moduleAndMethod, obj_args);
	}

	public boolean isValid(ModuleRequest request)
	{
		return ModuleRegistry.isValid(request);
	}


	@SuppressWarnings("unchecked")
	public void registerStores() throws InitializationException
	{
		/*
		Class<? extends PersistentStore> storeClass = null;
		PersistentStore store = null;
		for (int i = 0; i < _config.getStoreInfo().size(); i++)
		{
			StoreInfo s = _config.getStoreInfo().get(i);
			try
			{
				storeClass = (Class<? extends PersistentStore>) Class.forName(s.getClassName());
				store = storeClass.newInstance();
			}
			catch (Exception e)
			{
				logger.error("registerStores", e);
				continue;
			}
			try
			{
				store.init(s.getConfig());
			}
			catch (PersistenceException e)
			{
				logger.error("registerStores", e);
				continue;
			}
			_stores.put(s.getName(), store);
			logger.info("Registered PersistentStore " + s.getName() + " " + s.getClassName());
		}
		*/
	}

	@SuppressWarnings("unchecked")
	protected void registerAndLinkModules() throws InitializationException
	{
		/*instantiate modules*/
		logger.info("INSTANTIATING MODULES");
		for (int i = 0; i < _config.getModuleInfo().size(); i++)
		{
			ModuleInfo m 				= _config.getModuleInfo().get(i);
			String module_name 			= m.getName();
			logger.info("\tINSTANTIATING "+module_name);
			String module_classname   	= m.getClassName();
			
			ModuleRegistry.register(module_name,module_classname, m.getProps());
			Module module = ModuleRegistry.instantiate(module_name);
			module.setup_slots();
			_module_instances.put(module_name, module);
			_module_list.add(module);
		}
		
		logger.info("LINKING MODULES");
		/*link modules */
		for (int i = 0; i < _config.getModuleInfo().size(); i++)
		{
			ModuleInfo m_info 	    			  = _config.getModuleInfo().get(i);
			String module_name 					  = m_info.getName();
			logger.info("\tLINKING "+module_name);
			
			Map<String,String> module_info_slots  = m_info.getSlots();
			Module module_instance  			  = _module_instances.get(module_name);
			List<Module.SlotDescriptor> all_slots = module_instance.getSlotDescriptors();
			
			for(int j = 0;j < all_slots.size();j++)
			{
				Module.SlotDescriptor d = all_slots.get(j);
				String slot_name   		= d.slot_name;
				String slot_module_name = module_info_slots.get(slot_name);
				if(slot_module_name == null)
					if(d.required)
						throw new InitializationException("MODULE "+module_instance.getName()+"HAS A SLOT NAMED "+d.slot_name+" OF TYPE "+d.slot_type.getName()+" WHICH UNFORTUNATELY IS REQUIRED.");
					else
						continue;

				Object slot_instance = _module_instances.get(slot_module_name);
				if(slot_instance == null)
					throw new InitializationException("SLOT "+slot_name+" OF MODULE "+module_instance.getName()+" REFERS TO A MODULE NAMED "+slot_module_name+" WHICH IS UNDEFINED");
				
				try{
					logger.info("\t\tLINKING "+module_name+" SLOT "+slot_name+" WITH "+((Module)slot_instance).getName());
					module_instance.setSlot(slot_name, slot_instance);
				}catch(SlotException se)
				{
					throw new InitializationException(se.getMessage());
				}
			}
		}
		
		
		/*init modules*/
		logger.info("INITIALIZING MODULES");
		for (int i = 0; i < _config.getModuleInfo().size(); i++)
		{
			ModuleInfo m 				= _config.getModuleInfo().get(i);
			String module_name			= m.getName();
			logger.info("\tINITIALIZING "+module_name);
			_module_instances.get(m.getName()).init(this,m.getProps());
		}
	}
	
	
	
	
	@SuppressWarnings("unchecked")
	protected void registerUrls() throws InitializationException
	{
		for (int i = 0; i < _config.getUrlMapInfoItems().size(); i++)
		{
			UrlMapInfo u = _config.getUrlMapInfoItems().get(i);
			logger.info("URL " + u.getUrl());
		}
	}

	public String getMapping(String url)
	{
		Matcher matcher = null;
		int s = _config.getUrlMapInfoItems().size();
		for (int i = 0; i < s; i++)
		{
			UrlMapInfo url_map = _config.getUrlMapInfoItems().get(i);
			matcher = url_map.matcher(url);
			if (matcher.matches())
			{
				return matcher.replaceAll(url_map.getTemplatePath());
			}
		}
		return null;
	}

	public String getErrorMapping()
	{
		if (_config.getUrlMapInfo().getErrorMapInfo() == null)
			return null;
		return _config.getUrlMapInfo().getErrorMapInfo().getTemplatePath();
	}

	public String getMaintenanceMapping()
	{
		if (_config.getUrlMapInfo().getMaintenanceMapInfo() == null)
			return null;
		return _config.getUrlMapInfo().getMaintenanceMapInfo().getTemplatePath();
	}

	public void destroy()
	{
		for (String key : _stores.keySet())
			try
			{
				if (_stores.get(key) != null)
					_stores.get(key).close();
			}
			catch (PersistenceException e)
			{
				logger.error("destroy", e);
			}
		logger.debug("ApplicationConfig destroy complete");
	}

	public WebApplicationInitParams getConfig()
	{
		return _config;
	}

	public PersistentStore getStore(String name)
	{
		return _stores.get(name);
	}

	public Map<String, PersistentStore> getStores()
	{
		return _stores;
	}

	public File getTempDir()
	{
		return new File(System.getProperty("java.io.tmpdir"));
	}

	// public void setGateway(ApplicationHttpGateway gateway)
	// {
	// _gateway = gateway;
	// }
	//
	// public ApplicationHttpGateway getGateway()
	// {
	// return _gateway;
	// }
	//
	// public void enterMaintenanceMode()
	// {
	// _gateway.close();
	// }
	//
	// public void exitMaintenanceMode()
	// {
	// _gateway.open();
	// }
	public SessionManager getSessionManager(String name_space)
	{
		return _sess_name_space_mgr.get(name_space);
	}
}
