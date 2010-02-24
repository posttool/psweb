package com.pagesociety.web.page;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class Component
{
	private static final boolean CHECK_UNIQUE_ID = true;
	
	protected Container parent;
	protected Page page;
	protected String id;
	protected String class_name;
	protected Map<String,String> style;

	public Component(Container parent, String id, String class_name)
	{
		this.parent = parent;
		this.id = id;
		this.class_name = class_name;
		if (parent == null)
			return;
		this.page = parent.page;
		boolean id_ok = page.registerComponent(id, this);
		if (CHECK_UNIQUE_ID && !id_ok)
			throw new RuntimeException("Non unique id "+id);
		parent.children.add(this);
		//
	}

	public void setStyle(String... style)
	{
		if (this.style==null)
			this.style = new HashMap<String,String>();
		for (int i=0; i<style.length; i+=2)
			this.style.put(style[i],style[i+1]);
	}
	
	public void setClassName(String class_name)
	{
		this.class_name = class_name;
	}
	
	public String getClassName()
	{
		return class_name;
	}

	public abstract void render(StringBuilder b);

	protected void render_attributes_id_class_style(StringBuilder b)
	{
		render_id_attr(b);
		render_class_attr(b);
		render_style_attr(b);
	}

	protected void render_attrs(StringBuilder b, String... attrs)
	{
		for (int i = 0; i < attrs.length; i += 2)
		{
			b.append(attrs[i]);
			b.append("=\"");
			b.append(attrs[i + 1]);// TODO escape
			b.append("\" ");
		}
	}

	protected void render_style_attr(StringBuilder b)
	{
		if (style == null)
			return;
		b.append("style=\"");
		render_styles(b, style);
		b.append("\" ");
	}

	protected void render_styles(StringBuilder b, Map<String,String> styles)
	{
		Set<String> keys = style.keySet();
		for (String key : keys)
		{
			b.append(key);
			b.append(": ");
			b.append(styles.get(key));// TODO escape
			b.append(";");
		}
	}
	
	protected void render_styles(StringBuilder b, String... styles)
	{
		for (int i=0; i<styles.length; i+=2)
		{
			b.append(styles[i]);
			b.append(": ");
			b.append(styles[i+1]);// TODO escape
			b.append(";");
		}
	}

	protected void render_class_attr(StringBuilder b)
	{
		if (class_name != null)
			b.append("class=\"" + class_name + "\" ");
	}

	protected void render_id_attr(StringBuilder b)
	{
		if (id != null)
			b.append("id=\"" + id + "\" ");
	}
}
