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

public class FormGateway
{
	private WebApplication _web_application;

	public FormGateway(WebApplication web_app)
	{
		_web_application = web_app;
	}

	@SuppressWarnings("unchecked")
	public void doService(UserApplicationContext user_context,
			HttpServletRequest request, HttpServletResponse response) throws IOException,
			ServletException, WebApplicationException
	{
		String requestPath = request.getRequestURI().substring(request.getContextPath().length());
		String content_type = request.getContentType();
		Object[] args = new Object[1];
		if (content_type != null && content_type.startsWith("multipart"))
		{
			args[0] = new MultipartForm(request);
		}
		else
		{
			args[0] = new Form(request.getParameterMap());
		}
		ModuleRequest module_request;
		Object ret = null;
		try
		{
			module_request = GatewayUtil.parseModuleRequest(request, requestPath);
			module_request.setArguments(args);
			module_request.setUserContext(user_context);
			ret = _web_application.dispatch(module_request);
		}
		
		catch (Exception e)
		{
			throw new WebApplicationException("FORM GATEWAY ERROR", e);
		}
		response.setContentType(GatewayConstants.MIME_TYPE_HTML);
		PrintWriter out = response.getWriter();
		if(ret != null)
		{
			out.print(ret.toString());
			out.flush();
		}
		out.close();
	}
}
