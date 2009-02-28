package com.pagesociety.web.module;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pagesociety.web.exception.InitializationException;

public class ModuleDefinition
{
	private String name;
	private Class<? extends Module> module;
	private Map<String, List<ModuleMethod>> exported_method_map;


	public ModuleDefinition(String name, Map<String, Object> config)
	{
		this.name = name;
		this.exported_method_map = new HashMap<String, List<ModuleMethod>>();
	}
	
	public ModuleDefinition(String name, Class<? extends Module> module_class) throws InitializationException
	{
		this.name = name;
		this.exported_method_map = new HashMap<String, List<ModuleMethod>>();
		this.module = module_class;
		reflect(module);
	}
	
	public String getName()
	{
		return name;
	}

	private void reflect(Class<? extends Module> module) throws InitializationException
	{
		this.module = module;
		Method methods[] = module.getMethods();
		
		for (int i = 0; i < methods.length; i++)
		{

			Export export = methods[i].getAnnotation(Export.class);
			if (export != null)
			{
				ModuleMethod module_method = new ModuleMethod();
				module_method.init(methods[i],export);

				List<ModuleMethod> overloaded_methods = exported_method_map.get(module_method.getName());
				if(overloaded_methods == null)
				{
					overloaded_methods = new ArrayList<ModuleMethod>(4);
					exported_method_map.put(module_method.getName(), overloaded_methods);
				}
				overloaded_methods.add(module_method);

			}
		}
	}

	public String toString()
	{
		String line_break = "\n";
		StringBuffer b = new StringBuffer();
		b.append(module.getCanonicalName());
		b.append(line_break);
		b.append(exported_method_map.toString());
		return b.toString();
	}

	public Module newInstance()
	{
		try
		{
			return module.newInstance();
		}
		catch (InstantiationException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public List<ModuleMethod> getMethodsForMethodName(String method_name)
	{
		return exported_method_map.get(method_name);
	}
	
	// i need this for the generated docs/ or i need another method 'getUniqueMethodNames',
	// but i prefer this for now...!
	public List<ModuleMethod> getMethods()
	{
		List<ModuleMethod> all_methods = new ArrayList<ModuleMethod>();
		Set<String> keys = exported_method_map.keySet();
		for (String s : keys)
		{
			all_methods.addAll(exported_method_map.get(s));
		}
		return all_methods;
	}

}
