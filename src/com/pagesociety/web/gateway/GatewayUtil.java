package com.pagesociety.web.gateway;

import javax.servlet.http.HttpServletRequest;

import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplicationException;
import com.pagesociety.web.module.ModuleRegistry;
import com.pagesociety.web.module.ModuleRequest;
import com.pagesociety.web.upload.MultipartForm;

public class GatewayUtil
{
	public static final String REQUEST_PATH_SEPARATOR = "/";

	public static ModuleRequest parseModuleRequest(HttpServletRequest servlet_request,
			String request_path, UserApplicationContext user_context)
			throws WebApplicationException
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
		String module_name = req_path_split[0];
		String method_name = req_path_split[1];
		//
		Object[] args;
		String content_type = servlet_request.getContentType();
		if (content_type != null && content_type.startsWith("multipart"))
		{
			args = new Object[1];
			args[0] = new MultipartForm(servlet_request);
		}
		else
		{
			String d = servlet_request.getParameter("json");
			args = JsonEncoder.decode(d);
		}
		return setupModuleRequest(module_request, module_name, method_name, args);
	}

	public static ModuleRequest setupModuleRequest(ModuleRequest module_request,
			String module_name, String method_name, Object[] args)
			throws WebApplicationException
	{
		Object[] typed_args;
		try
		{
			typed_args = ModuleRegistry.coerceArgs(module_name, method_name, args);
		}
		catch (Exception e)
		{
			throw new WebApplicationException("fillModuleRequest() - CANT COERCE ARGS FOR " + module_name + " " + method_name + " " + args, e);
		}
		module_request.setModuleName(module_name);
		module_request.setMethodName(method_name);
		module_request.setArguments(typed_args);
		return module_request;
	}
}
