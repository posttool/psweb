package com.pagesociety.web.gateway;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.pagesociety.util.Text;
import com.pagesociety.web.ErrorMessage;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.module.ModuleRequest;

public class JsonGateway
{
	private WebApplication _web_application;

	public JsonGateway(WebApplication web_application)
	{
		_web_application = web_application;
	}

	public void doService(UserApplicationContext user_context,
			HttpServletRequest request, HttpServletResponse response) throws IOException,
			ServletException
	{
		String requestPath = request.getRequestURI().substring(request.getContextPath().length());
		ModuleRequest module_request;
		Object module_return;
		String text_response = null;
		try
		{
			module_request = GatewayUtil.parseModuleRequestJson(request, requestPath);
			module_request.setUserContext(user_context);
			module_return = _web_application.dispatch(module_request);
			text_response = JsonEncoder.encode(module_return);
			if (request.getParameter("noencode") == null)
				text_response = Text.encodeURIComponent(text_response);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			doError(e, user_context, request, text_response, response);
			return;
		}
		response.setContentType(GatewayConstants.MIME_TYPE_JS);
		if (text_response == null)
			text_response = "*NULL*";
		response.setContentLength(text_response.length());
		PrintWriter out = response.getWriter();
		out.write(text_response);
		out.close();
	}

	private void doError(Exception e, UserApplicationContext user_context,
			HttpServletRequest servlet_request, String request_path,
			ServletResponse response) throws IOException
	{
		PrintWriter out = response.getWriter();
		response.setContentType(GatewayConstants.MIME_TYPE_JS);
		out.write(JsonEncoder.encode(new ErrorMessage(e)));
		out.close();
	}
	
	
}
