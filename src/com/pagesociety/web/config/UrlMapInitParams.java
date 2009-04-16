package com.pagesociety.web.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class UrlMapInitParams
{
	public static final String URL_KEY = "url";
	public static final String SECURE_ATTRIBUTE_NAME = "secure";
	public static final String SECURE_ATTRIBUTE_TRUE_VALUE = "true";
	public static final String TEMPLATE_PATH_KEY = "template-path";
	public static final String DATA_PROVIDER_KEY = "data-provider";
	public static final String TYPE_ATTRIBUTE_KEY = "type";
	public static final String TYPE_ATTRIBUTE_VALUE_ERROR = "error";
	public static final String TYPE_ATTRIBUTE_VALUE_MAINTENANCE = "maintenance";
	private List<UrlMapInfo> _url_map_info;
	private UrlMapInfo _error;
	private UrlMapInfo _maintenance;

	public UrlMapInitParams(Document url_map_doc)
	{
		_url_map_info = new ArrayList<UrlMapInfo>();
		NodeList url_mappings = url_map_doc.getElementsByTagName("url-mapping");
		for (int i = 0; i < url_mappings.getLength(); i++)
		{
			Element url_map = (Element) url_mappings.item(i);
			String secure_value = url_map.getAttribute(SECURE_ATTRIBUTE_NAME);
			boolean secure = secure_value.equals(SECURE_ATTRIBUTE_TRUE_VALUE);
			String url = getParameterValue(URL_KEY, url_map);
			String template_path = getParameterValue(TEMPLATE_PATH_KEY, url_map);
			String type = url_map.getAttribute(TYPE_ATTRIBUTE_KEY);
			UrlMapInfo map = new UrlMapInfo(url, template_path, secure);
			_url_map_info.add(map);
			if (type.equals(TYPE_ATTRIBUTE_VALUE_ERROR))
			{
				_error = map;
			}
			else if (type.equals(TYPE_ATTRIBUTE_VALUE_MAINTENANCE))
			{
				_maintenance = map;
			}
		}
	}

	public List<UrlMapInfo> getInfo()
	{
		return _url_map_info;
	}

	public UrlMapInfo getErrorMapInfo()
	{
		return _error;
	}

	public UrlMapInfo getMaintenanceMapInfo()
	{
		return _maintenance;
	}

	public String toString()
	{
		return _url_map_info.toString();
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

	public class UrlMapInfo
	{
		private String url;
		private String templatePath;
		private boolean secure;
		private Pattern urlPattern;

		public UrlMapInfo(String url, String templatePath, boolean secure)
		{
			this.url = url;
			this.urlPattern = Pattern.compile(url);
			this.templatePath = templatePath;
			this.secure = secure;
		}

		public String getUrl()
		{
			return url;
		}

		public String getTemplatePath()
		{
			return templatePath;
		}

		public boolean isSecure()
		{
			return secure;
		}

		public Matcher matcher(String s)
		{
			return this.urlPattern.matcher(s);
		}

		public String toString()
		{
			return "UrlMapInfo " + url + " " + templatePath + " " + secure;
		}
	}
}
