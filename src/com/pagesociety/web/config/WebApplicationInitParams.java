package com.pagesociety.web.config;

import java.io.File;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.pagesociety.util.XML;
import com.pagesociety.web.InitializationException;
import com.pagesociety.web.config.ModuleInitParams.ModuleInfo;
import com.pagesociety.web.config.StoreInitParams.StoreInfo;
import com.pagesociety.web.config.UrlMapInitParams.UrlMapInfo;

public class WebApplicationInitParams
{
	public static final String WEB_ROOT_DIR_KEY = "web-root-directory";
	public static final String WEB_ROOT_URL_KEY = "web-root-url";

	//
	private File configDir;
	//
	Document application_doc;
	Document stores_doc;
	Document module_doc;
	Document url_map_doc;
	//
	private String applicationClassName;
	private String name;
	private String webRootDir;
	private String webRootUrl;

	//
	private StoreInitParams stores;
	private ModuleInitParams modules;
	private UrlMapInitParams urlMap;

	public WebApplicationInitParams(File config_dir) throws InitializationException
	{
		configDir = config_dir;
		try
		{
			application_doc = XML.read(new File(config_dir, "application.xml"));
			stores_doc = XML.read(new File(config_dir, "stores.xml"));
			module_doc = XML.read(new File(config_dir, "modules.xml"));
			url_map_doc = XML.read(new File(config_dir, "url-mappings.xml"));
		}
		catch (Exception e)
		{
			throw new RuntimeException("WebAppConfig can't read xml from " + config_dir);
		}
		//
		applicationClassName = application_doc.getDocumentElement().getAttribute("class");
		name = application_doc.getDocumentElement().getAttribute("name");
		webRootDir = getParameterValue(WEB_ROOT_DIR_KEY, application_doc.getDocumentElement());
		webRootUrl = getParameterValue(WEB_ROOT_URL_KEY, application_doc.getDocumentElement());
		//
		stores = new StoreInitParams(stores_doc);
		modules = new ModuleInitParams(module_doc);
		urlMap = new UrlMapInitParams(url_map_doc);
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

	
	public List<StoreInfo> getStoreInfo()
	{
		return stores.getInfo();
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
		return getParameterValue(key, application_doc.getDocumentElement());
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
		b.append(stores.toString());
		b.append(modules.toString());
		b.append(urlMap.toString());
		return b.toString();
	}
}
