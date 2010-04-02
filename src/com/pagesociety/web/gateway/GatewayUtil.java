package com.pagesociety.web.gateway;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.ModuleDefinition;
import com.pagesociety.web.module.ModuleMethod;
import com.pagesociety.web.module.ModuleRegistry;
import com.pagesociety.web.module.ModuleRequest;

public class GatewayUtil
{
	public static final String REQUEST_PATH_SEPARATOR = "/";

	public static ModuleRequest parseModuleRequest(HttpServletRequest servlet_request) throws WebApplicationException
	{
		return parseModuleRequest(servlet_request, servlet_request.getRequestURI().
			substring(servlet_request.getContextPath().length()));
	}
	
	public static ModuleRequest parseModuleRequest(HttpServletRequest servlet_request,
			String request_path) throws WebApplicationException
	{
		String path = request_path;
		if (path.startsWith(REQUEST_PATH_SEPARATOR))
			path = path.substring(1);
		ModuleRequest module_request = new ModuleRequest(path);
		//
		String[] req_path_split = path.split(REQUEST_PATH_SEPARATOR);
		if (req_path_split.length < 2)
			return module_request;
		//
		module_request.setModuleName(req_path_split[0]);
		module_request.setMethodName(req_path_split[1]);
		//
		return module_request;
	}

	public static ModuleRequest parseModuleRequestJson(
			HttpServletRequest servlet_request, String request_path)
			throws WebApplicationException
	{
		ModuleRequest module_request = parseModuleRequest(servlet_request, request_path);
		//
		String json = servlet_request.getParameter("args");
		Object[] args = JsonEncoder.decode(json);
		Object[] typed_args;
		try
		{
			typed_args = coerceArgs(module_request.getModuleName(), module_request.getMethodName(), args);
		}
		catch (Exception e)
		{
			throw new WebApplicationException("fillModuleRequest() - CANT COERCE ARGS FOR " + module_request.getModuleName() + " " + module_request.getMethodName() + " " + args, e);
		}
		module_request.setArguments(typed_args);
		return module_request;
	}
	
	public static Object[] coerceArgs(String module_name, String method_name,
			Object[] args) throws WebApplicationException
	{
	
		ModuleDefinition module_def = ModuleRegistry.getModuleDefinition(module_name);
		if (module_def == null)
			throw new RuntimeException("ModuleRegistry NO MODULE " + module_name);
		List<ModuleMethod> m = module_def.getMethodsForMethodName(method_name);
		if (m == null)
			throw new RuntimeException("ModuleRegistry NO METHOD " + method_name);

		int s = m.size();
		Object[] ca = null;
		for(int i = 0;i < s;i++)
		{
			ModuleMethod mm = m.get(i);
			if (mm.getParameterTypes().length!=args.length+1)
				continue;
			try {
				ca = mm.coerceArgs(args);
			} 
			catch(Exception e)
			{
			}
		}
		if (ca==null)
			throw new WebApplicationException("CANT CALL "+module_name+"/"+method_name+" with args "+args);

		return ca;
	}
}
