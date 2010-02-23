package com.pagesociety.web.page;

public abstract class Component
{
	protected Container parent;
	protected Page page;
	protected String id;
	protected String class_name;
	protected String[] style;

	public Component(Container parent, String id, String class_name)
	{
		this.parent = parent;
		this.id = id;
		this.class_name = class_name;
		if (parent == null)
			return;
		this.page = parent.page;
		parent.children.add(this);
	}

	public Component(Container parent, String id)
	{
		this(parent, id, null);
	}

	public void setStyle(String... style)
	{
		this.style = style;
	}

	public abstract void render(StringBuilder b);

	protected void render_self_closed_tag(StringBuilder b, String tag_name,
			String... attrs)
	{
		b.append("<");
		b.append(tag_name);
		b.append(" ");
		render_class_attr(b);
		render_attrs(b, attrs);
		render_styles(b);
		b.append("/>\n");
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

	protected void render_styles(StringBuilder b)
	{
		if (style == null)
			return;
		b.append("style=\"");
		render_styles(b, style);
		b.append("\" ");
	}

	protected void render_styles(StringBuilder b, String... styles)
	{
		for (int i = 0; i < styles.length; i += 2)
		{
			b.append(styles[i]);
			b.append(": ");
			b.append(styles[i + 1]);// TODO escape
			b.append(";");
		}
	}

	protected void render_class_attr(StringBuilder b)
	{
		if (class_name != null)
			b.append("class=\"" + class_name + "\" ");
	}
}
