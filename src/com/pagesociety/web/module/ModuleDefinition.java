package com.pagesociety.web.module;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pagesociety.web.exception.InitializationException;

public class ModuleDefinition
{
	private String name;
	private Class<? extends Module> module;
	private Map<String, List<ModuleMethod>> exported_method_map;
	private List<ModuleMethod> exported_methods;

	public ModuleDefinition(String name, Map<String, Object> config)
	{
		this.name = name;
		this.exported_method_map = new HashMap<String, List<ModuleMethod>>();
		this.exported_methods = new ArrayList<ModuleMethod>();
	}
	
	public String getName()
	{
		return name;
	}

	public void reflect(Class<? extends Module> module) throws InitializationException
	{
		this.module = module;
		Method methods[] = module.getMethods();
		
		for (int i = 0; i < methods.length; i++)
		{

			Export export = methods[i].getAnnotation(Export.class);
			if (export != null)
			{
				ModuleMethod module_method = new ModuleMethod();
				module_method.init(methods[i]);
				

				List<ModuleMethod> overloaded_methods = exported_method_map.get(module_method.getName());
				if(overloaded_methods == null)
				{
					overloaded_methods = new ArrayList<ModuleMethod>(4);
					exported_method_map.put(module_method.getName(), overloaded_methods);
				}
				overloaded_methods.add(module_method);
				exported_methods.add(module_method);
			}
		}
	}

	public String toString()
	{
		String line_break = "\n";
		StringBuffer b = new StringBuffer();
		b.append(module.getCanonicalName());
		b.append(line_break);
		for (int i = 0; i < exported_methods.size(); i++)
		{
			b.append("\t");
			b.append(exported_methods.get(i).toString());
			b.append(line_break);
		}
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

	public List<ModuleMethod> getMethods()
	{
		return exported_methods;
	}

}
