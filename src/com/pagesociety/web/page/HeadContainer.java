package com.pagesociety.web.page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HeadContainer
{
	private String script_root;
	private String style_root;
	private List<String> external_scripts;
	private List<String> external_styles;
	private List<String> embedded_scripts;
	public List<String> embedded_style_list;
	public Map<String, Map<String, String>> embedded_style_map;

	public HeadContainer()
	{
		external_scripts = new ArrayList<String>();
		external_styles = new ArrayList<String>();
		embedded_style_list = new ArrayList<String>();
		embedded_style_map = new HashMap<String, Map<String, String>>();
	}

	public void render(StringBuilder b)
	{
		b.append("<head>\n");
		
		// external script
		for (String src : external_scripts)
		{
			if (!src.startsWith("http") && script_root != null)
				src = script_root + src;
			b.append("<script src=\"");
			b.append(src);
			b.append("\" type=\"text/javascript\" ></script>\n");
		}
		// external style
		for (String src : external_styles)
		{
			if (!src.startsWith("http") && style_root != null)
				src = style_root + src;
			b.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
			b.append(src);
			b.append("\" /> \n");
		}
		// internal style
		b.append("<style>\n");
		for (String key : embedded_style_list)
		{
			b.append(key);
			b.append(" {\n");
			Map<String, String> style_params = embedded_style_map.get(key);
			Set<String> param_keys = style_params.keySet();
			for (String k : param_keys)
			{
				b.append("  ");
				b.append(k);
				b.append(": ");
				b.append(style_params.get(k));
				b.append(";\n");
			}
			b.append("}\n");
		}
		b.append("</style>\n");
		b.append("</head>\n");
	}

	public void addStyle(String name, String... style)
	{
		if (!embedded_style_list.contains(name))
			embedded_style_list.add(name);
		
		Map<String, String> style_map = embedded_style_map.get(name);
		if (style_map == null)
		{
			style_map = new HashMap<String, String>();
			embedded_style_map.put(name, style_map);
		}
		for (int i = 0; i < style.length; i += 2)
			style_map.put(style[i], style[i + 1]);
	}

	public void addScriptSrc(String src)
	{
		if (src==null || external_scripts.contains(src))
			return;
		external_scripts.add(src);
	}
	
	public void addStyleSrc(String src)
	{
		if (src==null || external_styles.contains(src))
			return;
		external_styles.add(src);
	}
}
