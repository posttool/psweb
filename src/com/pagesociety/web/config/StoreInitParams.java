package com.pagesociety.web.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class StoreInitParams
{
	public static final String STORE_KEY = "store";
	public static final String NAME_KEY = "name";
	public static final String CLASS_KEY = "class";
	private List<StoreInfo> _stores;

	public StoreInitParams(Document store_config)
	{
		_stores = new ArrayList<StoreInfo>();
		NodeList stores = store_config.getElementsByTagName(STORE_KEY);
		for (int i = 0; i < stores.getLength(); i++)
		{
			Element storeEl = (Element) stores.item(i);
			String name = storeEl.getAttribute(NAME_KEY);
			String storeClassName = storeEl.getAttribute(CLASS_KEY);
			HashMap<Object, Object> storeConfig = new HashMap<Object, Object>();
			NodeList configParams = storeEl.getChildNodes();
			for (int j = 0; j < configParams.getLength(); j++)
			{
				if (configParams.item(j).getNodeType() == Node.ELEMENT_NODE)
				{
					Element cfgEl = (Element) configParams.item(j);
					String val = getParameterValue(cfgEl);
					storeConfig.put(cfgEl.getNodeName(), val);
				}
			}
			_stores.add(new StoreInfo(name, storeClassName, storeConfig));
		}
	}

	public List<StoreInfo> getInfo()
	{
		return _stores;
	}

	public String toString()
	{
		return _stores.toString();
	}

	protected String getParameterValue(Element e)
	{
		if (!e.hasChildNodes())
			return null;
		String v = e.getFirstChild().getTextContent().trim();
		return v;
	}

	public class StoreInfo
	{
		private String name;
		private String className;
		private HashMap<Object, Object> config;

		public StoreInfo(String name, String className, HashMap<Object, Object> cfg)
		{
			this.name = name;
			this.className = className;
			this.config = cfg;
		}

		public String getName()
		{
			return name;
		}

		public String getClassName()
		{
			return className;
		}

		public HashMap<Object, Object> getConfig()
		{
			return config;
		}

		public String toString()
		{
			return "StoreInfo " + name + " " + className + " " + config;
		}
	}
}
