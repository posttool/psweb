package com.pagesociety.web.gateway;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.template.FreemarkerRenderer;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.TemplateException;

public class FreemarkerGateway
{
	// stuff for the freemarker / template context
	public static final String APPLICATION_KEY = "application";
	public static final String CONTEXT_KEY = "user_context";
	public static final String REQUEST_URL_KEY = "request_url";
	public static final String REQUEST_SERVER_NAME = "request_server_name";
	public static final String REQUEST_HOSTNAME = "hostname";
	public static final String REQUEST_PARAMS_KEY = "params";
	public static final String WEB_URL_KEY = "web_url";
    public static final String WEB_URL_SECURE_KEY = "web_url_secure";
    public static final String WEB_APP_VERSION_KEY = "version";
	public static final String MODULE_DATA_KEY = "data";
	public static final String EXCEPTION_KEY = "exception";
	public static final String EXCEPTION_STRING_KEY = "exceptionString";
	//
	private WebApplication _web_application;
	private FreemarkerRenderer _fm_renderer;
	
	public FreemarkerGateway(WebApplication web_app)
	{
		_web_application = web_app;
		_fm_renderer = new FreemarkerRenderer(web_app.getConfig().getWebRootDir());
	}

	public void doService(UserApplicationContext user_context, String requestPath,
			HttpServletRequest request, HttpServletResponse response) throws IOException,
			ServletException
	{

		String text_response;
		String mime_type;
		Map<String, Object> templateData;
		try
		{
			mime_type = getMimeType(requestPath);
			templateData = setup_template_context_object(user_context, requestPath,request.getServerName(), request.getParameterMap());
			text_response = _fm_renderer.render(requestPath, templateData);
		}
		catch (Exception e)
		{
			doError(e, user_context, request, requestPath, response);
			return;
		}
		response.setContentType(mime_type);
		if (text_response == null)
			text_response = "*NULL*";
		response.setContentLength(text_response.length());
		PrintWriter out = response.getWriter();
		out.write(text_response);
		out.close();
	}

	private String getMimeType(String requestPath)
	{
		if (requestPath.endsWith(GatewayConstants.SUFFIX_FREEMARKER_HTML))
			return GatewayConstants.MIME_TYPE_HTML;
		else if (requestPath.endsWith(GatewayConstants.SUFFIX_FREEMARKER_CSS))
			return GatewayConstants.MIME_TYPE_CSS;
		else if (requestPath.endsWith(GatewayConstants.SUFFIX_FREEMARKER_JS))
			return GatewayConstants.MIME_TYPE_JS;
		return null;
	}

	private void doError(Exception e, UserApplicationContext user_context,
			HttpServletRequest servlet_request, String request_path,
			HttpServletResponse response) throws IOException
	{
		String url_mapped_request = _web_application.getErrorMapping();
		PrintWriter out = response.getWriter();
		if (url_mapped_request == null)
		{
			response.setContentType(GatewayConstants.MIME_TYPE_TEXT);
			e.printStackTrace(new PrintWriter(out, true));
			out.close();
		}
		else
		{
			response.setContentType(GatewayConstants.MIME_TYPE_HTML);
			response.setStatus(404);
			Map<String, Object> templateData = setup_template_context_object(user_context, url_mapped_request,servlet_request.getServerName(), servlet_request.getParameterMap());
			templateData.put(EXCEPTION_KEY, e);
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			pw.flush();
			sw.flush();
			templateData.put(EXCEPTION_STRING_KEY, sw.toString());
			try
			{
				String template = _fm_renderer.render(url_mapped_request, templateData);
				out.write(template);
				out.close();
			}
			catch (TemplateException t)
			{
				out.write("<pre>");
				e.printStackTrace(new PrintWriter(out, true));
				out.write("</pre>");
				out.close();
			}
		}
	}

	private Map<String, Object> setup_template_context_object(
			UserApplicationContext user_context, String request_path, String host_name,@SuppressWarnings("rawtypes") Map params)
	{
		Map<String, Object> data = new HashMap<String, Object>();
		data.put(APPLICATION_KEY, _web_application);
		data.put(CONTEXT_KEY, user_context);
		//
		Map<Object, Object> params0 = new HashMap<Object, Object>();
		@SuppressWarnings("rawtypes") Iterator i = params.keySet().iterator();
		while (i.hasNext())
		{
			Object k = i.next();
			Object[] o = (Object[]) params.get(k);
			if (o.length == 1)
				params0.put(k, o[0]);
			else
				params0.put(k, o);
		}
		data.put(REQUEST_PARAMS_KEY, params0);
		data.put(REQUEST_URL_KEY, request_path);
		data.put(REQUEST_SERVER_NAME, host_name);
		data.put(REQUEST_HOSTNAME, host_name);
		data.put(WEB_URL_KEY, _web_application.getConfig().getWebRootUrl());
        data.put(WEB_URL_SECURE_KEY, _web_application.getConfig().getWebRootUrlSecure());
        data.put(WEB_APP_VERSION_KEY, _web_application.getConfig().getVersion());
		data.put("statics", BeansWrapper.getDefaultInstance().getStaticModels());
		//
		return data;
	}
}
