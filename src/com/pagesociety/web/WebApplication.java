package com.pagesociety.web;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;


import com.pagesociety.web.bean.Beans;
import com.pagesociety.web.config.WebApplicationInitParams;
import com.pagesociety.web.config.ModuleInitParams.ModuleInfo;
import com.pagesociety.web.config.UrlMapInitParams.UrlMapInfo;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SlotException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Module;
import com.pagesociety.web.module.ModuleDefinition;
import com.pagesociety.web.module.ModuleRegistry;
import com.pagesociety.web.module.ModuleRequest;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.Module.SlotDescriptor;


public abstract class WebApplication
{
	private static final Logger logger = Logger.getLogger(WebApplication.class);
	private WebApplicationInitParams 	 _config;
	private Map<String, Module> 		 _module_instances;
	private List<Module> 				 _module_list;
	private SessionNameSpaceManager 	 _sess_name_space_mgr;


	public WebApplication() throws InitializationException
	{

		_module_instances 		= new HashMap<String, Module>();
		_module_list 			= new ArrayList<Module>();
		_sess_name_space_mgr 	= new SessionNameSpaceManager();

	}

	public void init(WebApplicationInitParams config) throws InitializationException
	{

		_config = config;
		//
		Beans.initDefault();
		ModuleRegistry.init(this);
		//
		registerAndLinkModules();
		registerUrls();
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
			try{
				user_context = (UserApplicationContext)_config.getUserApplicationContextClass().newInstance();
				user_context.setId(sess_id);
				sess_mgr.put(sess_id, user_context);
				return (UserApplicationContext) sess_mgr.get(sess_id);
			}catch(Exception e)
			{
				ERROR("!!!!SERIOUS BARF GETTING SESSION ",e);
				return null;
			}
			
		}
	}

	public List<Module> getModules()
	{
		return _module_list;
	}
	
	public List<ModuleDefinition> getModuleDefinitions()
	{
		return ModuleRegistry.getModules();
	}

	public Map<String, Module> getModuleMap()
	{
		return _module_instances;
	}

	public Module getModule(String name)
	{
		return _module_instances.get(name);
	}

	private static ThreadLocal<UserApplicationContext> calling_user_context = new ThreadLocal<UserApplicationContext>();
	public UserApplicationContext getCallingUserContext()
	{
		return calling_user_context.get();
	}
	
	
	public Object dispatch(ModuleRequest request) throws WebApplicationException,Throwable
	{
		Module module = getModule(request.getModuleName());
		if (module == null)
			throw new WebApplicationException("WebApplication.dispatch MODULE " + request.getModuleName() + " DOES NOT EXIST");
		UserApplicationContext uctx = request.getUserContext();
		Object return_value = null;
		try{
			calling_user_context.set(uctx);
			return_value = ModuleRegistry.invoke(module, request.getMethodName(), uctx, request.getArguments());
		}finally
		{
			calling_user_context.remove();
		}
		request.setResult(return_value);
		return return_value;
	}

	public Object execute( UserApplicationContext user_context,String moduleAndMethod,
			Object... args) throws Throwable
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
			freemarker.template.SimpleSequence args) throws Throwable
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



	@SuppressWarnings("unchecked")
	protected void registerAndLinkModules() throws InitializationException
	{
		/*instantiate modules*/
		INFO("INSTANTIATING MODULES");
		for (int i = 0; i < _config.getModuleInfo().size(); i++)
		{
			ModuleInfo m 				= _config.getModuleInfo().get(i);
			String module_name 			= m.getName();
			logger.info("\tINSTANTIATING "+module_name);
			String module_classname   	= m.getClassName();
			
			ModuleDefinition def = ModuleRegistry.register(module_name,module_classname);
			Module module = ModuleRegistry.instantiate(def,m.getProps());

			_module_instances.put(module_name, module);
			_module_list.add(module);
		}
		
		INFO("LINKING MODULES");
		/*link modules */
		for (int i = 0; i < _config.getModuleInfo().size(); i++)
		{
			ModuleInfo m_info 	    			  = _config.getModuleInfo().get(i);
			String module_name 					  = m_info.getName();
			INFO("\tLINKING "+module_name);
			
			Map<String,String> module_info_slots  = m_info.getSlots();
			Module module_instance  			  = _module_instances.get(module_name);
			List<Module.SlotDescriptor> all_slots = module_instance.getSlotDescriptors();
			
			/* iterate through all slots so we can see if required ones are 'filled'*/
			for(int j = 0;j < all_slots.size();j++)
			{
				Module.SlotDescriptor d = all_slots.get(j);
				String slot_name   		= d.slot_name;
				/*this is the reference to another module by name in application.xml*/
				String slot_module_name = module_info_slots.get(slot_name);
				Module slot_instance;
				
				if(slot_module_name == null)
				{
					if(d.required)/* this means the user needs to supply a value in that slot via applicatin.xml*/
						throw new InitializationException("MODULE "+module_instance.getName()+" HAS A SLOT NAMED "+d.slot_name+" OF TYPE "+d.slot_type.getName()+" WHICH UNFORTUNATELY IS REQUIRED.");
					else
					{
						/*otherwise the slot definition could have a default slot class */
						/* this default slot class shouldnt really have any slots since they cannot */
						/* be linked */
						if(d.default_slot_val != null)
						{
							if(d.default_slot_val instanceof Class)
							{
								try{
									slot_instance = (Module)((Class)d.default_slot_val).newInstance();
								}catch(Exception e)
								{
									throw new InitializationException("MODULE "+module_instance.getName()+"FAILED SETTING UP DEFAULT FOR "+d.slot_name+" OF TYPE "+((Class)d.default_slot_val).getName()+" WHICH UNFORTUNATELY BOMBS THE INIT.");
								}
							}
							else if(d.default_slot_val instanceof Module)
							{
								slot_instance = (Module)d.default_slot_val;
							}
							else
							{
								throw new InitializationException("MODULE "+module_instance.getName()+"FAILED SETTING UP DEFAULT VAL. UNSUPPORTED DEFAULT VAL TYPE");
							}
						}
						else
							continue;
					}
				}
				else
				{
						slot_instance = _module_instances.get(slot_module_name);
						if(slot_instance == null)
							throw new InitializationException("SLOT "+slot_name+" OF MODULE "+module_instance.getName()+" REFERS TO A MODULE NAMED "+slot_module_name+" WHICH IS UNDEFINED");
				}
				
				try{
					INFO("\t\tLINKING "+module_name+" SLOT "+slot_name+" WITH "+((Module)slot_instance).getName());
					module_instance.setSlot(slot_name, slot_instance);
				}catch(SlotException se)
				{
					throw new InitializationException(se.getMessage());
				}
			}
		}
		
		/* system init...this is the lowest level bootstrap.slots are filled out
		 * but their init methods have not been called
		 */
		WebStoreModule.web_store_subsystem_init_start(this);	
		for (int i = 0; i < _config.getModuleInfo().size(); i++)
		{
			ModuleInfo m 				= _config.getModuleInfo().get(i);
			_module_instances.get(m.getName()).system_init(this,m.getProps());
		}
		/*init modules*/
		INFO("INITIALIZING MODULES");

		for (int i = 0; i < _config.getModuleInfo().size(); i++)
		{
			ModuleInfo m 				= _config.getModuleInfo().get(i);
			String module_name			= m.getName();
			Module module_instance 		= _module_instances.get(module_name);
			init_module(module_instance);
		}
	
		try{
			WebStoreModule.web_store_subsystem_init_complete(this);
		}catch(Exception e)
		{
			ERROR(e);
			throw new InitializationException(e.getMessage());
		}
		
		for(int i = 0;i < _module_list.size();i++)
		{
			Module m = _module_list.get(i);
			m.loadbang(this,m.getParams());
		}
	
	}
	
	
	
	private void init_module(Module m) throws InitializationException
	{
		if(m.isInitialized() || m.isInitializing())
			return;
		m.setInitializing(true);
		
		List<Integer> module_attributes = m.getModuleAttributes();
		INFO("\tINITIALIZING "+m.getName());
		if(m == null)
			System.out.println("\tWARNING: MODULE WITH NULL NAME "+m);

		List<SlotDescriptor> slot_descriptors = m.getSlotDescriptors();
		for(int i = 0;i < slot_descriptors.size();i++)
		{
			SlotDescriptor d 	 = slot_descriptors.get(i);
			Module slot_instance = (Module)m.getSlot(d.slot_name);


			if(slot_instance == null)
				continue;
			if(!slot_instance.isInitialized())
			{
				//INFO("\tINITIALIZING SLOT "+d.slot_name+" INSTANCE OF "+slot_instance.getClass().getSimpleName()+" TO "+slot_instance.getName());
				init_module(slot_instance);
			}
		}
		if(m.getParams() == null)
			m.init(this, new HashMap<String,Object>());
		else
			m.init(this, m.getParams());

		m.setInitializing(false);
		m.setInitialized(true);
	}
	
	@SuppressWarnings("unchecked")
	protected void registerUrls() throws InitializationException
	{
		for (int i = 0; i < _config.getUrlMapInfoItems().size(); i++)
		{
			UrlMapInfo u = _config.getUrlMapInfoItems().get(i);
			INFO("URL " + u.getUrl());
		}
	}

	public Object[] getMapping(String url)
	{
		Matcher matcher = null;
		int s = _config.getUrlMapInfoItems().size();
		for (int i = 0; i < s; i++)
		{
			UrlMapInfo url_map = _config.getUrlMapInfoItems().get(i);
			matcher = url_map.matcher(url);
			if (matcher.matches())
			{
				Object[] ret = new Object[2];
				ret[0] = url_map;
				ret[1] = matcher.replaceAll(url_map.getTemplatePath());
				return ret;
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
//		for (String key : _stores.keySet())
//			try
//			{
//				if (_stores.get(key) != null)
//					_stores.get(key).close();
//			}
//			catch (PersistenceException e)
//			{
//				logger.error("destroy", e);
//			}
		INFO("ApplicationConfig destroy complete");
	}

	public WebApplicationInitParams getConfig()
	{
		return _config;
	}

//	public PersistentStore getStore(String name)
//	{
//		return _stores.get(name);
//	}
//
//	public Map<String, PersistentStore> getStores()
//	{
//		return _stores;
//	}

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

	public void WARNING(String message)
	{
		System.err.println("WARNING: "+message);
	}
	
	public void INFO(String message)
	{
		System.out.println("INFO "+message);
	}
	
	public void ERROR(String message)
	{
		System.err.println("ERROR "+message);
	}
	


	public void ERROR(Exception e)
	{
		e.printStackTrace();
	}
	
	public void ERROR(String message,Exception e)
	{
		System.err.println("ERROR "+message);
		e.printStackTrace();
	}

	public void applicationDestroyed()
	{
		
		System.out.println("APPLICATION "+_config.getName()+" IS ABOUT TO BE DESTROYED");
		System.out.println(new Date());
		System.out.println("******************************************************************");
		System.out.println();
		for(int i = 0;i < _module_list.size();i++)
		{
			Module m = _module_list.get(i);
			m.onDestroy();
		}
		System.out.println("APPLICATION "+_config.getName()+" IS NOW DESTROYED");
		System.out.println(new Date());
		System.out.println("******************************************************************");
		System.out.println();
	}
	
	
}
