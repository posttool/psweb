package com.pagesociety.web.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ModuleInitParams
{
	public static final String MODULES_KEY = "modules";
	public static final String MODULE_KEY = "module";
	public static final String MODULE_NAME_KEY = "module-name";
	public static final String MODULE_CLASS_KEY = "module-class";
	public static final String MODULE_CONFIG_CLASS_KEY = "module-config-class";
	public static final String MODULE_PARAMS = "module-params";
	private List<ModuleInfo> _modules;

	public ModuleInitParams(Document module_doc)
	{
		_modules = new ArrayList<ModuleInfo>();
		NodeList modules = module_doc.getElementsByTagName(MODULE_KEY);
		for (int i = 0; i < modules.getLength(); i++)
		{
			Element module = (Element) modules.item(i);
			String moduleName = getParameterValue(MODULE_NAME_KEY, module);
			String moduleClassName = getParameterValue(MODULE_CLASS_KEY, module);
			String moduleConfigClassName = getParameterValue(MODULE_CONFIG_CLASS_KEY, module);
			Map<String, Object> moduleProps = getProps(MODULE_PARAMS, module);
			_modules.add(new ModuleInfo(moduleName, moduleClassName, moduleConfigClassName, moduleProps));
		}
	}

	public List<ModuleInfo> getInfo()
	{
		return _modules;
	}

	public String toString()
	{
		return _modules.toString();
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

	private Map<String, Object> getProps(String s, Element d)
	{
		Map<String, Object> props = new HashMap<String, Object>();
		NodeList l = d.getElementsByTagName(s);
		for (int i = 0; i < l.getLength(); i++)
		{
			Node n = l.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE && n.getChildNodes().getLength() != 0)
			{
				props.put(n.getNodeName(), n.getFirstChild().getTextContent().trim());
			}
		}
		return props;
	}

	public class ModuleInfo
	{
		private String name;
		private String className;
		private String configClassName;
		private Map<String, Object> config;

		public ModuleInfo(String name, String className, String configClassName,
				Map<String, Object> config)
		{
			this.name = name;
			this.className = className;
			this.configClassName = configClassName;
			this.config = config;
		}
//
//		public String getName()
//		{
//			return name;
//		}

		public String getClassName()
		{
			return className;
		}

		public String getConfigClassName()
		{
			return configClassName;
		}

		public Map<String, Object> getConfig()
		{
			return config;
		}

		public String toString()
		{
			return "ModuleInfo " + name + " " + className + " " + configClassName + " " + config;
		}
	}
}
