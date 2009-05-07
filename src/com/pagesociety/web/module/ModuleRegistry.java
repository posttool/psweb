package com.pagesociety.web.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.pagesociety.persistence.PersistentStore;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.config.ModuleInitParams.ModuleInfo;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.upload.MultipartForm;

/**
 * The module factory is responsible for maintaining a list of named modules.
 * The module factory uses reflection to map all of the public methods by name.
 * 
 * Use the module factory to instantiate modules. Use it to invoke methods by
 * name on a module. Ex:
 * 
 * <code>
 * ModuleFactory.register("authenticate", AuthenticationModule.class);
 * Module module = ModuleFactory.instantiate("authenticate");
 * ModuleFactory.invoke(module, "login", new String[] { "name", "pw3d" });
 * </code>
 * 
 * @see Module
 * @see ModuleDefinition
 */
public class ModuleRegistry
{
	private static final Logger   logger = Logger.getLogger(ModuleRegistry.class);
//	private static WebApplication _app_config;
	// private static PersistentStore _store;
	//
	private static Map<String, ModuleDefinition> MODULES = new HashMap<String, ModuleDefinition>();
	private static List<ModuleDefinition> MODULE_LIST = new ArrayList<ModuleDefinition>();

	public static void init(WebApplication app_context)
	{
//		_app_config = app_context;
	}

	@SuppressWarnings("unchecked")
	
	public static ModuleDefinition register(String moduleName,String moduleClassName)throws InitializationException
	{
			if (moduleClassName == null || moduleName == null)
				throw new InitializationException("IMPROPER MODULE REGISTRATION:  A module name attribute and module-class tag are required!");
			if (MODULES.get(moduleName) != null)
				throw new InitializationException("MODULE " + moduleName + " ALREADY DEFINED ");
		
			Class<? extends Module> moduleClass;
			try
			{
				moduleClass = (Class<? extends Module>) Class.forName(moduleClassName);
			}
			catch (ClassNotFoundException e)
			{
				throw new InitializationException("ClassNotFound " + moduleClassName, e);
			}		
			return register(moduleName, moduleClass);
	}
	
	public static ModuleDefinition register(String moduleName,Class<? extends Module> module_class)throws InitializationException

	{

		ModuleDefinition module_def;
		module_def = new ModuleDefinition(moduleName,module_class);
		MODULES.put(moduleName, module_def);
		MODULE_LIST.add(module_def);

		logger.info("ModuleRegistry registered " + moduleName + " with instance of " + module_class);
		logger.info(module_def);
		return module_def;
	}

	public static List<ModuleDefinition> getModules()
	{
		return MODULE_LIST;
	}

	public static Module instantiate(ModuleDefinition module_def, Map<String,Object> params) throws InitializationException
	{

		try
		{
			Module module = module_def.newInstance();
			module.setName(module_def.getName());
			//module.setName(module_info.getName());
			//module.setModuleInfo(module_info);
			module.setParams(params);
			module.setup_slots();
			return module;
		}
		catch (Exception e)
		{
			logger.error("instantiate(String, UserApplicationContext)", e);
			throw new InitializationException("ModuleRegistry UNABLE TO INSTANTIATE MODULE " + module_def.getName(), e);
		}
	}

	public static Object invoke(Module module, String method_name,
			UserApplicationContext user_context, Object[] args) throws Throwable
	{
		ModuleDefinition module_def = MODULES.get(module.getName());
		if (module_def == null)
			throw new WebApplicationException("ModuleRegistry NO MODULE " + module.getName());
		
		List<ModuleMethod> potential_matches = module_def.getMethodsForMethodName(method_name);
		if (potential_matches == null)
			throw new WebApplicationException("ModuleRegistry NO METHOD " + method_name);
		int s = potential_matches.size();
		
		Object[] args_with_user = new Object[args.length + 1];
		System.arraycopy(args, 0, args_with_user, 1, args.length);
		args_with_user[0] = user_context;
		
		ModuleMethod resolved_method = null;
		for(int i = 0;i < s;i++)
		{
			ModuleMethod m = potential_matches.get(i);
			if(m.isValidForArgs(args_with_user))
			{
				if(resolved_method != null)
					throw new WebApplicationException("AMBIGUOUS MODULE METHOD INVOCATION FOR MODULE "+module.getName()+" METHODS "+m+" AND "+resolved_method+" CAN BOTH BE CALLED FOR ARGS "+args);
				resolved_method = m;
			}
		}
		if(resolved_method == null)
		{
			StringBuilder args_string = new StringBuilder();
			for(int i = 0;i < args.length;i++)
			{
				if(args[i]==null)
					args_string.append("null,");
				args_string.append(args[i].getClass().getSimpleName()+":"+args[i].toString()+", ");
			
			}
			if(args_string.length() != 0)
				args_string.setLength(args_string.length()-1);
			
			throw new WebApplicationException("NO METHOD NAMED "+method_name+" EXISTS IN "+module.getName()+" WHICH CAN BE CALLED FOR ARGS -\n "+args_string);
		}
		if(resolved_method.isTransactionProtected())
		{
			PersistentStore store = ((WebStoreModule)module).store;
			try{
				WebStoreModule.START_TRANSACTION(store);
				Object ret = resolved_method.invoke(module,args_with_user);
				WebStoreModule.COMMIT_TRANSACTION(store);
				return ret;
			}catch(Throwable t)
			{
				WebStoreModule.ROLLBACK_TRANSACTION(store);
				throw t;
			}
		}
		else
			return resolved_method.invoke(module,args_with_user);
	}

	public static ModuleDefinition getModuleDefinition(String module_name)
	{
		return MODULES.get(module_name);
	}

}
