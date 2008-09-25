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
	public static final String MODULES 					= "modules";
	public static final String MODULE 					= "module";
	public static final String MODULE_NAME_ATT 			= "name";
	public static final String MODULE_CLASS 			= "module-class";
	public static final String MODULE_SLOTS 			= "module-slots";
	public static final String MODULE_SLOT 				= "module-slot";
	public static final String MODULE_SLOT_NAME_ATT 	= "name";
	public static final String MODULE_SLOT_INSTANCE_ATT = "instance";
	public static final String MODULE_PARAMS 			= "module-params";
	
	private List<ModuleInfo> _modules;

	public ModuleInitParams(Document module_doc)
	{
		_modules = new ArrayList<ModuleInfo>();
		NodeList modules = module_doc.getElementsByTagName(MODULE);
		for (int i = 0; i < modules.getLength(); i++)
		{
			Element module 					= (Element) modules.item(i);
			String moduleName 				= module.getAttribute(MODULE_NAME_ATT);
			String moduleClassName 			= getParameterValue(MODULE_CLASS, module);
			Map<String, Object> moduleProps = getProps(MODULE_PARAMS, module);
			Map<String, String> moduleSlots = getSlots(module);
			_modules.add(new ModuleInfo(moduleName, moduleClassName,moduleSlots,moduleProps));
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

	private Map<String, String> getSlots(Element d)
	{
		Map<String, String> slots = new HashMap<String, String>();
		NodeList l = d.getElementsByTagName(MODULE_SLOTS);
		if(l.getLength() == 0)
			return slots;
	
		for (int i = 0; i < l.getLength(); i++)
		{
			NodeList module_slots = l.item(i).getChildNodes();
			for(int ii = 0;ii < module_slots.getLength();ii++)
			{
				Node n = module_slots.item(ii);
				if (n.getNodeType() 				== Node.ELEMENT_NODE && 
				    n.getChildNodes().getLength() 	== 0 && 
				    n.getNodeName().equals(MODULE_SLOT))
					{
						slots.put(((Element)n).getAttribute(MODULE_SLOT_NAME_ATT), 
								 ((Element)n).getAttribute(MODULE_SLOT_INSTANCE_ATT)); 
					}
			}
		}
		return slots;
	}

	private Map<String, Object> getProps(String s, Element d)
	{
		Map<String, Object> props = new HashMap<String, Object>();
		NodeList l = d.getElementsByTagName(s);
		if(l.getLength() == 0)
			return props;
	
		for (int i = 0; i < l.getLength(); i++)
		{
			NodeList nl = l.item(i).getChildNodes();
			for(int ii = 0;ii < nl.getLength();ii++)
			{
				Node n = nl.item(ii);
				if (n.getNodeType() == Node.ELEMENT_NODE && n.getChildNodes().getLength() != 0)
				{
					props.put(n.getNodeName(), n.getFirstChild().getTextContent().trim());
				}
			}
		}
		return props;
	}

	public class ModuleInfo
	{
		private String name;
		private String className;
		private Map<String, String> slots;
		private Map<String, Object> props;

		public ModuleInfo(String name, String className, Map<String,String> slots,Map<String, Object> props)
		{
			this.name 	   = name;
			this.className = className;
			this.slots     = slots;
			this.props     = props; 
		}

		public String getName()
		{
			return name;
		}

		public String getClassName()
		{
			return className;
		}

		public Map<String, Object> getProps()
		{
			return props;
		}
		
		public Map<String, String> getSlots()
		{
			return slots;
		}

		public String toString()
		{
			return "ModuleInfo " + name + " " + className + " slots:" + slots + " props:" + props;
		}
	}
}
