package com.pagesociety.web.gateway;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.pagesociety.web.ErrorMessage;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.amf.AmfConstants;
import com.pagesociety.web.amf.AmfIn;
import com.pagesociety.web.amf.AmfOut;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.ModuleRequest;

public class AmfGateway
{
	private WebApplication _web_application;

	public AmfGateway(WebApplication web_application)
	{
		_web_application = web_application;
	}

	public void doService(UserApplicationContext user_context,
			HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{
		AmfIn amf_in = null;
		AmfOut amf_out = null;
		try
		{
			amf_in = new AmfIn(request);
			String request_path = request.getRequestURI().substring(request.getContextPath().length());
			ModuleRequest module_request = GatewayUtil.parseModuleRequest(request, request_path);
			module_request.setArguments(amf_in.getArguments().toArray());
			module_request.setUserContext(user_context);
			Object return_value;
			String amf_routing;
			try
			{
				return_value = _web_application.dispatch(module_request);
				amf_routing = AmfConstants.RESULT_CALLBACK;
			}
			catch (Exception e)
			{
				return_value = new ErrorMessage(e);
				amf_routing = AmfConstants.ERROR_CALLBACK;
				e.printStackTrace();
			}
			amf_out = new AmfOut(amf_in.version, amf_in.client_id, amf_routing, return_value);
			int responseLength = amf_out.buffer.remaining();
			response.setContentType(AmfConstants.AMF_MIME_TYPE);
			response.setContentLength(responseLength);
			response.getOutputStream().write(amf_out.buffer.array(), amf_out.buffer.position(), responseLength);
		}
		catch (WebApplicationException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (amf_in != null && amf_in.buffer != null)
			{
				amf_in.buffer.release();
			}
			if (amf_out != null && amf_out.buffer != null)
			{
				amf_out.buffer.release();
			}
		}
	}
}
