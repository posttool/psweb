package com.pagesociety.web.gateway;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.ModuleRequest;
import com.pagesociety.web.upload.MultipartForm;

public class RawGateway
{
	private WebApplication _web_application;

	public RawGateway(WebApplication web_app)
	{
		_web_application = web_app;
	}

	@SuppressWarnings("unchecked")
	public void doService(UserApplicationContext user_context,
			HttpServletRequest request, HttpServletResponse response) throws IOException,
			ServletException, WebApplicationException
	{
		String requestPath = request.getRequestURI().substring(request.getContextPath().length());
		Object[] args = new Object[1];
		args[0] = new RawCommunique(request,response);

		ModuleRequest module_request;
		try
		{
			module_request = GatewayUtil.parseModuleRequest(request, requestPath);
			module_request.setArguments(args);
			module_request.setUserContext(user_context);
			_web_application.dispatch(module_request);
		}
		catch (Throwable e)
		{
			throw new WebApplicationException("RAW GATEWAY ERROR", e);
		}
		try{
		PrintWriter out = response.getWriter();
		out.close();
		}catch(IllegalStateException ise)
		{
			//this happens if someone accesses the outputstream directly such as in the case
			//of streaming files.
		}
		
	}
}
