package com.pagesociety.web.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
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
	public static void register(String moduleName,String moduleClassName, Map<String, Object> config)
			throws InitializationException
	{
		if (moduleClassName == null || moduleName == null)
			throw new InitializationException("IMPROPER MODULE REGISTRATION:  A module name attribute and module-class tag are required!");
		ModuleDefinition module_def = new ModuleDefinition(moduleName,config);
		Class<? extends Module> moduleClass;
		try
		{
			moduleClass = (Class<? extends Module>) Class.forName(moduleClassName);
		}
		catch (ClassNotFoundException e)
		{
			throw new InitializationException("ClassNotFound " + moduleClassName, e);
		}
		if (MODULES.get(moduleName) != null)
			throw new InitializationException("MODULE " + moduleName + " ALREADY DEFINED ");
		if (moduleClass != null)
		{
			module_def.reflect(moduleClass);
			MODULES.put(moduleName, module_def);
			MODULE_LIST.add(module_def);
		}
		logger.info("ModuleRegistry registered " + moduleName + " with instance of " + moduleClass);
		logger.info(module_def);
	}

	public static List<ModuleDefinition> getModules()
	{
		return MODULE_LIST;
	}

	public static Module instantiate(String module_name) throws InitializationException
	{
		ModuleDefinition module_def = MODULES.get(module_name);
		if (module_def == null)
			throw new InitializationException("ModuleRegistry NO SUCH MODULE " + module_name);
		try
		{
			Module module = module_def.newInstance();
			module.setName(module_name);
			return module;
		}
		catch (Exception e)
		{
			logger.error("instantiate(String, UserApplicationContext)", e);
			throw new InitializationException("ModuleRegistry UNABLE TO INSTANTIATE MODULE " + module_name, e);
		}
	}

	public static Object invoke(Module module, String method_name,
			UserApplicationContext user_context, Object[] args) throws Exception
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
			throw new WebApplicationException("NO METHOD NAMED "+method_name+" EXISTS IN "+module.getName()+" WHICH CAN BOTH BE CALLED FOR ARGS "+args);
		return resolved_method.invoke(module,args_with_user);
	}



}
