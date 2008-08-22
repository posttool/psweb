package com.pagesociety.web.gateway;

import javax.servlet.http.HttpServletRequest;

import com.pagesociety.web.WebApplicationException;
import com.pagesociety.web.module.ModuleRegistry;
import com.pagesociety.web.module.ModuleRequest;

public class GatewayUtil
{
	public static final String REQUEST_PATH_SEPARATOR = "/";

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
		String json = servlet_request.getParameter("json");
		Object[] args = JsonEncoder.decode(json);
		Object[] typed_args;
		try
		{
			typed_args = ModuleRegistry.coerceArgs(module_request.getModuleName(), module_request.getMethodName(), args);
		}
		catch (Exception e)
		{
			throw new WebApplicationException("fillModuleRequest() - CANT COERCE ARGS FOR " + module_request.getModuleName() + " " + module_request.getMethodName() + " " + args, e);
		}
		module_request.setArguments(typed_args);
		return module_request;
	}
}
