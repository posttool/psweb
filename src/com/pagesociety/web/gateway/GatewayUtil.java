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

	
}
