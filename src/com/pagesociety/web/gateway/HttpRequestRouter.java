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
import com.pagesociety.web.ApplicationBootstrap;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.config.UrlMapInitParams;
import com.pagesociety.web.config.UrlMapInitParams.UrlMapInfo;
import com.pagesociety.web.exception.WebApplicationException;

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
	private String _web_url;
	private String _web_url_secure;
	private boolean _is_closed;
	//
	private StaticHttpGateway static_gateway;
	private AmfGateway amf_gateway;
	private JsonGateway json_gateway;
	private FreemarkerGateway freemarker_gateway;
	private FormGateway form_gateway;
	private RawGateway  raw_gateway;

	public void init(ServletConfig cfg) throws ServletException
	{
		_servlet_config = cfg;
		_web_application = (WebApplication)cfg.getServletContext().getAttribute(ApplicationBootstrap.APPLICATION_ATTRIBUTE_NAME);

		if (_web_application == null)
			throw new ServletException("WebApplication was not initialized. Make sure ApplicationBootstrap has been loaded.");
		
			
			// _web_application.setGateway(this);
		_web_application.getSessionManager(HTTP).setTimeout(SESSION_TIMEOUT);
		_web_url = _web_application.getConfig().getWebRootUrl();
		_web_url_secure = _web_application.getConfig().getWebRootUrlSecure();
		_is_closed = false;
		//
		static_gateway = new StaticHttpGateway();
		amf_gateway = new AmfGateway(_web_application);
		json_gateway = new JsonGateway(_web_application);
		freemarker_gateway = new FreemarkerGateway(_web_application);
		form_gateway = new FormGateway(_web_application);
		raw_gateway  = new RawGateway(_web_application);
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
			System.err.println("ERROR CALLER IP WAS: "+request.getRemoteAddr());
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
		String completeUrl = getUrl(request);
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
		UserApplicationContext uctx = get_user_context(request, response);
		//System.out.println("!!!!>>"+uctx.getId()+" "+completeUrl);
		// FORM first, because sometimes it uses the ps_session_id and doesn't want the redirect to occur
		if (requestPath.endsWith(GatewayConstants.SUFFIX_FORM))
		{
			form_gateway.doService(uctx, request, response);
			return;
		}
		// redirect if the session id was included in requests for the following...
		if (request.getParameter(GatewayConstants.SESSION_ID_KEY)!=null)
		{
			response.sendRedirect( getPathWithoutSessionId(request) );
			return;
		}
		// AMF
		if (requestPath.endsWith(GatewayConstants.SUFFIX_AMF))
		{
			amf_gateway.doService(uctx, request, response);
			return;
		}
		// JSON
		if (requestPath.endsWith(GatewayConstants.SUFFIX_JSON))
		{
			json_gateway.doService(uctx, request, response);
			return;
		}
		//RAW
		if (requestPath.endsWith(GatewayConstants.SUFFIX_RAW))
		{
			raw_gateway.doService(uctx, request, response);
			return;
		}
		// MAPPED
		Object[] url_mapped_request = _web_application.getMapping(completeUrl);
		if (url_mapped_request != null)
		{
			UrlMapInfo url_map_info = (UrlMapInfo)url_mapped_request[0];
			String path = (String)url_mapped_request[1];
			if (url_map_info.isSecure()==UrlMapInitParams.SECURE && !completeUrl.startsWith(_web_url_secure))
			{
				response.sendRedirect( get_path(_web_url_secure,getContextPathEtc(request),uctx) );
				return;
			}
			else if (url_map_info.isSecure()==UrlMapInitParams.NOT_SECURE && !completeUrl.startsWith(_web_url))
			{
				response.sendRedirect( get_path(_web_url,getContextPathEtc(request),uctx) );
				return;
			}
			else
			{
				RequestDispatcher dispatcher = request.getRequestDispatcher(path);
				dispatcher.forward(request, response);
				return;
			}
		}
		// FREEMARKER
		for (int i = 0; i < GatewayConstants.SUFFIXES_FREEMARKER.length; i++)
		{
			if (requestPath.endsWith(GatewayConstants.SUFFIXES_FREEMARKER[i]))
			{
				freemarker_gateway.doService(uctx, requestPath, request, response);
				return;
			}
		}

		// UNKNOWN
		File request_file = new File(_web_application.getConfig().getWebRootDir(), requestPath);
		static_gateway.serveFile(request_file, mime_type, request, response);
		return;
	}

	
	private String get_path(String root, String path, UserApplicationContext uctx)
	{
		StringBuilder b = new StringBuilder();
		b.append(root);
		b.append(path);
		if (path.indexOf(GatewayConstants.SESSION_ID_KEY)==-1)
		{
			if (path.indexOf("?")==-1)
				b.append("?");
			else
				b.append("&");
			b.append(GatewayConstants.SESSION_ID_KEY);
			b.append("=");
			b.append(uctx.getId());
		}
		return b.toString();
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////
	private String getPathWithoutSessionId(HttpServletRequest req)
	{
		String scheme = req.getScheme();             // http
        String serverName = req.getServerName();     // hostname.com
        int serverPort = req.getServerPort();        // 80
        String contextPath = req.getContextPath();   // /mywebapp
        String servletPath = req.getServletPath();   // /servlet/MyServlet
        String pathInfo = req.getPathInfo();         // /a/b;c=123
        String queryString = req.getQueryString();    // d=789
   
        // Reconstruct original requesting URL
        StringBuilder b = new StringBuilder();
        b.append(scheme);
        b.append("://");
        b.append(serverName);
        if (serverPort != 80 && serverPort != 443)
        {
        	b.append(":");
            b.append(serverPort);
        }
        b.append(contextPath);
        b.append(servletPath);
        if (pathInfo != null) 
        {
        	b.append(pathInfo);
        }
        if (queryString != null) 
        {
        	StringBuilder bb = new StringBuilder();
        	bb.append("?");
        	String[] qp = queryString.split("&");
        	for (int i=0; i<qp.length; i++)
        	{
        		if (!qp[i].startsWith(GatewayConstants.SESSION_ID_KEY))
        		{
        			bb.append(qp[i]);
        			bb.append("&");
        		}
        	}
        	if (bb.length()!=1)
        		b.append(bb);
        }
        return b.toString();
	}

	private String getContextPathEtc(HttpServletRequest req)
	{
        String contextPath = req.getContextPath();   // /mywebapp
        String servletPath = req.getServletPath();   // /servlet/MyServlet
        String pathInfo = req.getPathInfo();         // /a/b;c=123
        String queryString = req.getQueryString();          // d=789

        StringBuilder b = new StringBuilder();
		b.append(contextPath);
        b.append(servletPath);
        if (pathInfo != null) 
        {
        	b.append(pathInfo);
        }
        if (queryString != null) 
        {
        	b.append("?");
            b.append(queryString);
        }
        return b.toString();
	}
	
	private String getUrl(HttpServletRequest req)
	{
        String scheme = req.getScheme();             // http
        String serverName = req.getServerName();     // hostname.com
        int serverPort = req.getServerPort();        // 80
        String contextPath = req.getContextPath();   // /mywebapp
        String servletPath = req.getServletPath();   // /servlet/MyServlet
        String pathInfo = req.getPathInfo();         // /a/b;c=123
        String queryString = req.getQueryString();   // d=789
   
        // Reconstruct original requesting URL
        StringBuilder b = new StringBuilder();
        b.append(scheme);
        b.append("://");
        b.append(serverName);
        if (serverPort != 80 && serverPort != 443)
        {
        	b.append(":");
            b.append(serverPort);
        }
        b.append(contextPath);
        b.append(servletPath);
        if (pathInfo != null) 
        {
        	b.append(pathInfo);
        }
        if (queryString != null) 
        {
        	b.append("?");
            b.append(queryString);
        }
        return b.toString();
	}
	
	
	
	private UserApplicationContext get_user_context(HttpServletRequest request,
			HttpServletResponse response)
	{
		
		String http_sess_id = null;
		Cookie cookie = null;
		Cookie[] cookies = request.getCookies();
		if (cookies != null)
		{
			int s = cookies.length;
			for (int i=0; i<s; i++)
			{
				if (cookies[i].getName().equals(GatewayConstants.SESSION_ID_KEY))
				{
					cookie = cookies[i];
					break;
				}
			}
		}
		if (request.getParameter(GatewayConstants.SESSION_ID_KEY) != null)
		{
			http_sess_id = request.getParameter(GatewayConstants.SESSION_ID_KEY);
		}
		if (cookie==null)
		{
			if (http_sess_id==null)
				http_sess_id = RandomGUID.getGUID();
			Cookie c = new Cookie(GatewayConstants.SESSION_ID_KEY, http_sess_id);
			c.setMaxAge(SESSION_TIMEOUT);
			c.setPath("/");
			response.addCookie(c);
		}
		else 
		{
			if (http_sess_id!=null)
			{
				cookie.setValue(http_sess_id);
				response.addCookie(cookie);
			}
			else
				http_sess_id = cookie.getValue();
		}
		return _web_application.getUserContext(HTTP, http_sess_id);
	}
}
