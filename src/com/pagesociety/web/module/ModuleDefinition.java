package com.pagesociety.web.module;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pagesociety.web.InitializationException;

public class ModuleDefinition
{
	private Class<? extends Module> module;
	private Map<String, ModuleMethod> exported_method_map;
	private List<ModuleMethod> exported_methods;

	public ModuleDefinition(Map<String, Object> config)
	{
		this.exported_method_map = new HashMap<String, ModuleMethod>();
		this.exported_methods = new ArrayList<ModuleMethod>();
	}

	public void reflect(Class<? extends Module> module) throws InitializationException
	{
		this.module = module;
		Method declared_methods[] = module.getDeclaredMethods();
		for (int i = 0; i < declared_methods.length; i++)
		{
			Export export = declared_methods[i].getAnnotation(Export.class);
			if (export != null)
			{
				ModuleMethod module_method = new ModuleMethod();
				module_method.reflect(declared_methods[i]);
				exported_method_map.put(module_method.getName(), module_method);
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

	public ModuleMethod getMethod(String method_name)
	{
		return exported_method_map.get(method_name);
	}

	public List<ModuleMethod> getMethods()
	{
		return exported_methods;
	}

}
