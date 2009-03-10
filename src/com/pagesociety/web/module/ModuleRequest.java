package com.pagesociety.web.module;

import java.util.Date;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.UserApplicationContext;

public class ModuleRequest
{
	//
	private String _path;
	private String _module_name;
	private String _method_name;
	private Object[] _arguments;
	private Object _result_value;
	private UserApplicationContext _user;

	public ModuleRequest()
	{
	}
	
	public ModuleRequest(String module_name, String method_name)
	{
		_module_name = module_name;
		_method_name = method_name;
	}

	public ModuleRequest(String path)
	{
		_path = path;
	}

	public String getModuleName()
	{
		return _module_name;
	}

	public String getMethodName()
	{
		return _method_name;
	}

	public void setArguments(Object[] args)
	{
		_arguments = args;
	}

	public Object[] getArguments()
	{
		return _arguments;
	}

	public void setResult(Object result_value)
	{
		_result_value = result_value;
	}

	public Object getResult()
	{
		return _result_value;
	}

	public String toString()
	{
		StringBuffer b = new StringBuffer();
		b.append(_module_name);
		b.append("/");
		b.append(_method_name);
		b.append("/");
		for (int i = 0; i < _arguments.length; i++)
		{
			Object o = _arguments[i];
			if (o instanceof Entity)
				b.append(((Entity) o).getId());
			else if (o instanceof Date)
				b.append(((Date) o).getTime());
			else
				b.append(o);
			b.append("/");
		}
		return b.toString();
	}

	public void setModuleName(String module_name)
	{
		_module_name = module_name;
	}

	public void setMethodName(String method_name)
	{
		_method_name = method_name;
	}

	public String getPath()
	{
		return _path;
	}

	public void setUserContext(UserApplicationContext user_context)
	{
		_user = user_context;
	}

	public UserApplicationContext getUserContext()
	{
		return _user;
	}
}
