package com.pagesociety.web.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.pagesociety.util.XML;
import com.pagesociety.web.config.ModuleInitParams.ModuleInfo;
import com.pagesociety.web.config.UrlMapInitParams.UrlMapInfo;
import com.pagesociety.web.exception.InitializationException;

public class WebApplicationInitParams
{
	
	private static final String DEPLOYMENT_PROPERTIES_FILE_NAME = "deployment.properties";
	private static final String APPLICATION_SPEC_FILE_NAME 		= "application.xml";
	private static final String APPLICATION_URL_MAPPINGS 		= "url-mappings.xml";

	private static final String NODE_TYPE_APPLICATION   		= "application";
	private static final String ATTR_APP_CLASS 					= "class";
	private static final String ATTR_APP_NAME  					= "name";
	private static final String ATTR_APP_WEB_ROOT_DIR  			= "web-root-directory";
	private static final String ATTR_APP_WEB_ROOT_URL  			= "web-root-url";
	private static final String ATTR_APP_WEB_ROOT_URL_SECURE  	= "web-root-url-secure";
	private static final String ATTR_APP_VERSION  				= "version";
	private static final String ATTR_APP_USER_CONTEXT_CLASS		= "user-application-context-class";
	private static final String ATTR_APP_MODULE_DATA_DIRECTORY  = "module-data-directory";
	//
	private File configDir;
	//
	private Document application_doc;
	private Document url_map_doc;
	private Element application_element;
	
	//
	private String applicationClassName;
	private String name;
	private String webRootDir;
	private String webRootUrl;
	private String webRootUrlSecure;
	private String version;
	private File   moduleDataDir;
	private Class  userApplicationContextClass;
	//
	private ModuleInitParams modules;
	private UrlMapInitParams urlMap;
	private Properties		 deployment_properties;
	
	private Map<String,String> init_parameters = new HashMap<String,String>();
	

	public WebApplicationInitParams(File config_dir) throws InitializationException
	{

		configDir 				= config_dir;
		deployment_properties 		= new Properties();
		File deployments_file 	= new File(configDir,DEPLOYMENT_PROPERTIES_FILE_NAME);
		if(deployments_file.exists())
		{
			try {
		        deployment_properties.load(new FileInputStream(deployments_file));
			} catch (IOException e) {
				throw new InitializationException("FAILED READING "+DEPLOYMENT_PROPERTIES_FILE_NAME);
			}
		}
		
		try
		{
			application_doc = XML.read(new File(config_dir, APPLICATION_SPEC_FILE_NAME));
			url_map_doc 	= XML.read(new File(config_dir, APPLICATION_URL_MAPPINGS));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new InitializationException("WebAppConfig can't read xml from " + config_dir);
		}
		
		setup_application_params();
		modules = new ModuleInitParams(deployment_properties,application_doc);
		urlMap  = new UrlMapInitParams(url_map_doc);
	}



	private void setup_application_params() throws InitializationException
	{
		NodeList app_es 	= application_doc.getElementsByTagName(NODE_TYPE_APPLICATION);
		if(app_es.getLength() == 0)
			throw new InitializationException("application.xml: missing required node:<"+NODE_TYPE_APPLICATION+">");

		application_element 	= (Element)app_es.item(0);
		name 					= expand_property(application_element.getAttribute(ATTR_APP_NAME));
		if(name == null)
			throw new InitializationException("application.xml: application node is missing required attribute "+ATTR_APP_NAME);
		applicationClassName 	= expand_property(application_element.getAttribute(ATTR_APP_CLASS));
		if(applicationClassName == null)
			throw new InitializationException("application.xml: application node is missing required attribute "+ATTR_APP_CLASS);
		webRootDir 				= expand_property(application_element.getAttribute(ATTR_APP_WEB_ROOT_DIR));
		if(webRootDir == null)
			throw new InitializationException("application.xml: application node is missing required attribute "+ATTR_APP_WEB_ROOT_DIR);	
		webRootUrl 				= expand_property(application_element.getAttribute(ATTR_APP_WEB_ROOT_URL));
		if(webRootUrl == null)
			throw new InitializationException("application.xml: application node is missing required attribute "+ATTR_APP_WEB_ROOT_URL);
		webRootUrlSecure 		= expand_property(application_element.getAttribute(ATTR_APP_WEB_ROOT_URL_SECURE));
		version = expand_property(application_element.getAttribute(ATTR_APP_VERSION));
		if(version == null)
			throw new InitializationException("application.xml: application node is missing required attribute "+ATTR_APP_VERSION);
		
		//everything ends up in this paramters map//
		//all the attributes of the application tag in application.xml//
		NamedNodeMap optional_atts = application_element.getAttributes();
		int s = optional_atts.getLength();
		for(int i = 0;i < s;i++)
		{
			Node n = optional_atts.item(i);
			init_parameters.put(n.getNodeName(), expand_property(n.getNodeValue()));
		}
		
		String userApplicationContextClassS = expand_property(application_element.getAttribute(ATTR_APP_USER_CONTEXT_CLASS));
		if(userApplicationContextClassS == null)
			userApplicationContextClassS = "com.pagesociety.web.UserApplicationContext";
		try {
			userApplicationContextClass = Class.forName(userApplicationContextClassS);
		} catch (ClassNotFoundException e) {
			throw new InitializationException("application.xml: Unable to find UserApplicationContext class "+userApplicationContextClassS);
		}
		if(!com.pagesociety.web.UserApplicationContext.class.isAssignableFrom(userApplicationContextClass))
			throw new InitializationException("application.xml: user-application-context-class "+userApplicationContextClassS+" does not appear to extend com.pagesociety.web.UserApplicationContext");
		
		String moduleDataDirS = expand_property(application_element.getAttribute(ATTR_APP_MODULE_DATA_DIRECTORY));	
		if(moduleDataDirS == null)
			moduleDataDirS = configDir.getAbsolutePath()+File.separator+"module-data";
		
		moduleDataDir = new File(moduleDataDirS);
		if(!moduleDataDir.exists())
		{
			try{	

				moduleDataDir.mkdirs();
			}catch(Exception e)
			{
				e.printStackTrace();
				throw new InitializationException("application.xml: Problem creating module data base dir"+moduleDataDirS);
			}
		}
		
	}
	/*
	private String expand_property(String value) throws InitializationException
	{
		if(value == null)
			return null;
		if(value.startsWith("$"))
		{
			String raw_deployment_prop = deployment_properties.getProperty(value.substring(1));
			char[] cc = raw_deployment_prop.toCharArray();
			int last_part_of_key = 1;
			for(int i = 0;i < cc.length;i++)
			{
				char c = cc[i];
				if(Character.isJavaLetterOrDigit(c) || c == '_' || c == '-')
				{
					last_part_of_key++;
					
				}
			}
			
			String deployment_prop = 
				if(deployment_prop == null)
					throw new InitializationException("UNBOUND VARIABLE "+value+" IN application.xml");
			return deployment_prop;
		}
		return value;
	}
	*/
	private String expand_property(String value) throws InitializationException
	{
		
		char[] cc 		  = value.toCharArray();
		StringBuilder buf = new StringBuilder();
		for(int i = 0;i < cc.length;i++)
		{
			char c = cc[i];
			if(c == '$')
			{
			
				StringBuilder key = new StringBuilder();
				while(true)
				{
					i++;
					if(i > cc.length-1) 
						break;
					c = cc[i];
					if(Character.isJavaIdentifierPart(c))
						key.append(c);
					else
					{
						i--;
						break;
					}
				}
				
				String prop_key = key.toString();
				String deployment_prop = deployment_properties.getProperty(prop_key);
				if(deployment_prop == null)
					throw new InitializationException("UNBOUND VARIABLE "+prop_key+" IN application.xml");
				System.out.println("APPENDING :"+deployment_prop+":");
				buf.append(deployment_prop.trim());
					
				
			}
			else
			{
				buf.append(c);
			}
			
		}
		if(buf.length() == 0)
			return null;
		else
			return buf.toString();
	}
	
	
	public String getInitParameter(String name)
	{
		return (String)init_parameters.get(name);
	}

	public Map<String,String> getInitParameters(String name)
	{
		return init_parameters;
	}

	public String getApplicationClassName()
	{
		return applicationClassName;
	}

	public File getConfigDirectory()
	{
		return configDir;
	}

	public String getName()
	{
		return name;
	}

	public String getWebRootDir()
	{
		return webRootDir;
	}

	public String getWebRootUrl()
	{
		return webRootUrl;
	}
	
	public String getWebRootUrlSecure()
	{
		return webRootUrlSecure;
	}

	public String getVersion()
	{
		return version;
	}

	public Class getUserApplicationContextClass()
	{
		return userApplicationContextClass;
	}
	
	public File getModuleDataDirectory()
	{
		return moduleDataDir;
	}
	
	public List<ModuleInfo> getModuleInfo()
	{
		return modules.getInfo();
	}

	public UrlMapInitParams getUrlMapInfo()
	{
		return urlMap;
	}

	public List<UrlMapInfo> getUrlMapInfoItems()
	{
		return urlMap.getInfo();
	}

	public String getParameterValue(String key)
	{
		return getParameterValue(key, application_element);
	}

	private String getParameterValue(String s, Element d)
	{
		NodeList l = d.getElementsByTagName(s);
		if (l.getLength() != 1)
			return null;
		if (!l.item(0).hasChildNodes())
			return null;
		String v = l.item(0).getFirstChild().getTextContent().trim();
		return v;
	}

	public String toString()
	{
		StringBuffer b = new StringBuffer();
		b.append("WebApplicationConfig: " + name + " " + applicationClassName + "\n");
		b.append(modules.toString());
		b.append(urlMap.toString());
		return b.toString();
	}
}
