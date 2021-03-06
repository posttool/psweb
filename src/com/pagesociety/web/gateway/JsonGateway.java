package com.pagesociety.web.gateway;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.pagesociety.util.Text;
import com.pagesociety.web.ErrorMessage;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.json.JsonDecoder;
import com.pagesociety.web.json.JsonEncoder;
import com.pagesociety.web.module.ModuleRequest;
import com.pagesociety.web.upload.MultipartForm;
import com.pagesociety.web.upload.MultipartFormException;

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
			module_request 		= GatewayUtil.parseModuleRequest(request, requestPath);
			String json_args 	= request.getParameter("args");//Text.decode(request.getParameter("args"));
			boolean isform = false;
			if(json_args == null)//assume form submit
			{
				String content_type = request.getContentType();
				Object[] args = new Object[1];
				if (content_type != null && content_type.startsWith("multipart"))
				{
					try{
						args[0] = new MultipartForm(request);
					}catch(MultipartFormException mfe)
					{
						throw new WebApplicationException(mfe.getMessage());
					}
				}
				else
				{
					args[0] = new Form(request.getParameterMap());
				}
				module_request.setArguments(args);
				isform = true;
			}
			else
			{
				module_request.setArguments(unpackJSONArgs(json_args));
			}
			
			module_request.setUserContext(user_context);
			module_return = _web_application.dispatch(module_request);
			if(isform)
			{
				text_response = JsonEncoder.encode(module_return,false,true);
			}
			else
			{
				text_response = JsonEncoder.encode(module_return,true,true);	
			}
			
			if (request.getParameter("callback") != null)
				text_response = request.getParameter("callback")+"("+text_response+");";
			if (request.getParameter("encode") != null)
				text_response = Text.encodeURIComponent(text_response);
		}
		catch (Throwable e)
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

	private void doError(Throwable e, UserApplicationContext user_context,
			HttpServletRequest request, String request_path,
			ServletResponse response) throws IOException
	{
		String text_response = JsonEncoder.encode(new ErrorMessage(e));
		if (request.getParameter("callback") != null)
			text_response = request.getParameter("callback")+"("+text_response+");";
		if (request.getParameter("encode") != null)
			text_response = Text.encodeURIComponent(text_response);

		PrintWriter out = response.getWriter();
		response.setContentType(GatewayConstants.MIME_TYPE_JS);
		out.write(text_response);
		out.close();
	}

	public Object[] unpackJSONArgs(String args_s) throws WebApplicationException
	{

		try {
			List v = JsonDecoder.decodeAsList(args_s);
			Object[] o = v.toArray();
			return o;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebApplicationException("ARG "+e.getMessage());
		}
	}
	
}
