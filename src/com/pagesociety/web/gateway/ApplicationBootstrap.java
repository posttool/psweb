package com.pagesociety.web.gateway;

import java.io.File;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.pagesociety.web.WebApplication;
import com.pagesociety.web.config.WebApplicationInitParams;
import com.pagesociety.web.exception.InitializationException;

public class ApplicationBootstrap extends HttpServlet
{
	private static final long serialVersionUID = -5524927480184007418L;
	private static final Logger logger = Logger.getLogger(ApplicationBootstrap.class);
	private static final String APPLICATION_ATTRIBUTE_NAME = "application";
	private WebApplication application;

	public void init(ServletConfig cfg) throws ServletException
	{
		String config_path = cfg.getServletContext().getRealPath("/") + "WEB-INF" + File.separator + "config";
		try
		{
			DOMConfigurator.configure(config_path + "/log4j.xml");
			logger.info("-----------------------------------------------");
			logger.info("Initializing ApplicationBootstrap 771");
			logger.info("-----------------------------------------------");
			//
			File cfg_dir = new File(config_path);
			WebApplicationInitParams config;
			try{
				config = new WebApplicationInitParams(cfg_dir);
			}catch(InitializationException ie)
			{
				ie.printStackTrace();
				throw new ServletException("UNABLE TO BOOTSTRAP APPLICATION.PROBLEM WITH APPLICATION CONFIG",ie);
			}
			//
			logger.info("Loaded config:");
			logger.info(config.getApplicationClassName());
			//
			WebApplication app;
			app = (WebApplication) Class.forName(config.getApplicationClassName()).newInstance();
			app.init(config);
		}
		catch (Exception e)
		{
			logger.error("Can't initialize application.", e);
			logger.info("Can't initialize application: " + e.getMessage());
			return;
		}
		cfg.getServletContext().setAttribute(APPLICATION_ATTRIBUTE_NAME, application);
		logger.info("Initialized servlet gateway with " + application);
		logger.info("ServletGateway init complete");
	}
}
