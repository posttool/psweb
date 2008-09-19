package com.pagesociety.web.gateway;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.pagesociety.util.RandomGUID;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.WebApplicationException;

public class HttpRequestRouter extends HttpServlet
{
	private static final long serialVersionUID = 454337901877827397L;
	private static final Logger logger = Logger.getLogger(HttpRequestRouter.class);
	//
	private static final String HTTP = "http";
	private static final int SESSION_TIMEOUT = 30 * 60 * 1000;
	//
	private ServletConfig _servlet_config;
	private WebApplication _web_application;
	private boolean _is_closed;
	//
	private StaticHttpGateway static_gateway;
	private AmfGateway amf_gateway;
	private JsonGateway json_gateway;
	private FreemarkerGateway freemarker_gateway;
	private FormGateway form_gateway;

	public void init(ServletConfig cfg) throws ServletException
	{
		_servlet_config = cfg;
		_web_application = WebApplication.getInstance();
		if (_web_application == null)
			throw new ServletException("WebApplication was not initialized. Make sure ApplicationBootstrap has been loaded.");
		// _web_application.setGateway(this);
		_web_application.getSessionManager(HTTP).setTimeout(SESSION_TIMEOUT);
		_is_closed = false;
		//
		static_gateway = new StaticHttpGateway();
		amf_gateway = new AmfGateway(_web_application);
		json_gateway = new JsonGateway(_web_application);
		freemarker_gateway = new FreemarkerGateway(_web_application);
		form_gateway = new FormGateway(_web_application);
		//
		logger.info("ServletGateway init complete");
	}

	public void open()
	{
		_is_closed = false;
	}

	public void close()
	{
		_is_closed = true;
		// TODO
		// while(_is_serving)
		// sleep;
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException
	{
		long t = System.currentTimeMillis();
		try
		{
			doService(request, response);
		}
		catch (WebApplicationException e)
		{
			e.printStackTrace();
			throw new ServletException(e);
		}
		logger.debug("GET " + request.getRequestURI() + " from " + request.getRemoteHost() + " took " + (System.currentTimeMillis() - t) + "ms");
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException
	{
		long t = System.currentTimeMillis();
		try
		{
			doService(request, response);
		}
		catch (WebApplicationException e)
		{
			e.printStackTrace();
			throw new ServletException(e);
		}
		logger.debug("POST " + request.getRequestURI() + " from " + request.getRemoteHost() + " took " + (System.currentTimeMillis() - t) + "ms");
	}

	private void doService(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException, WebApplicationException
	{
		if (_is_closed)
		{
			// TODO
			// if html/freemarker: serve_file(_maintenance_page, request, response);
			// if javascript/amf: throw new ClosedException();
			PrintWriter out = response.getWriter();
			out.write("CLOSED");
			out.close();
			return;
		}
		// STATIC
		String requestPath = request.getRequestURI().substring(request.getContextPath().length());
		String mime_type = _servlet_config.getServletContext().getMimeType(requestPath);
		if (mime_type != null)
		{
			for (int i = 0; i < GatewayConstants.MIME_TYPE_PREFIXES.length; i++)
			{
				if (mime_type.startsWith(GatewayConstants.MIME_TYPE_PREFIXES[i]))
				{
					File request_file = new File(_web_application.getConfig().getWebRootDir(), requestPath);
					static_gateway.serveFile(request_file, mime_type, request, response);
					return;
				}
			}
		}
		// AMF
		if (requestPath.endsWith(GatewayConstants.SUFFIX_AMF))
		{
			amf_gateway.doService(get_user_context(request, response), request, response);
			return;
		}
		// JSON
		if (requestPath.endsWith(GatewayConstants.SUFFIX_JSON))
		{
			json_gateway.doService(get_user_context(request, response), request, response);
			return;
		}
		// FORM
		if (requestPath.endsWith(GatewayConstants.SUFFIX_FORM))
		{
			form_gateway.doService(get_user_context_for_form(request, response), request, response);
			return;
		}
		// MAPPED
		String url_mapped_request = _web_application.getMapping(requestPath);
		if (url_mapped_request != null)
		{
			RequestDispatcher dispatcher = request.getRequestDispatcher(url_mapped_request);
			dispatcher.forward(request, response);
			return;
		}
		// FREEMARKER
		for (int i = 0; i < GatewayConstants.SUFFIXES_FREEMARKER.length; i++)
		{
			if (requestPath.endsWith(GatewayConstants.SUFFIXES_FREEMARKER[i]))
			{
				freemarker_gateway.doService(get_user_context(request, response), requestPath, request, response);
				return;
			}
		}
		// UNKNOWN
		File request_file = new File(_web_application.getConfig().getWebRootDir(), requestPath);
		static_gateway.serveFile(request_file, mime_type, request, response);
		return;
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////
	
//	private UserApplicationContext get_user_context(HttpServletRequest request,
//			HttpServletResponse response)
//	{
//		// NOTE
//		// there may be a stray user context floating around if 2 requests are
//		// made quickly
//		// TODO
//		// maybe parse cookies out of url too at some point if it ever becomes a
//		// requirement
//		String http_sess_id = null;
//		if (request.getCookies() != null)
//		{
//			int s = request.getCookies().length;
//			for (int i=0; i<s; i++)
//			{
//				if (request.getCookies()[i].getName().equals(GatewayConstants.SESSION_ID_KEY))
//				{
//					http_sess_id = request.getCookies()[i].getValue();
//					break;
//				}
//			}
//		}
//		
//		if (http_sess_id==null)
//		{
//			http_sess_id = RandomGUID.getGUID();
//			Cookie c = new Cookie(GatewayConstants.SESSION_ID_KEY, http_sess_id);
//			c.setMaxAge(SESSION_TIMEOUT);
//			response.addCookie(c);
//		}
//		
//		return _web_application.getUserContext(HTTP, http_sess_id);
//	}

	private UserApplicationContext get_user_context_for_form(HttpServletRequest request,
			HttpServletResponse response)
	{
		// the session id is not passed back with firefox w/ uploads from flash
		// so we have to look for the parameter in the request instead of
		// looking at the cookie
		String http_sess_id = null;
		if (request.getParameter(GatewayConstants.SESSION_ID_KEY) != null)
		{
			http_sess_id = request.getParameter(GatewayConstants.SESSION_ID_KEY);
		}
		else
		{
			http_sess_id = request.getSession().getId();
		}
		
		return _web_application.getUserContext(HTTP, http_sess_id);
	}
	
	private UserApplicationContext get_user_context(HttpServletRequest request, HttpServletResponse response)
	{
	 String http_sess_id = request.getSession().getId();
	 return _web_application.getUserContext(HTTP, http_sess_id);
	}
}
