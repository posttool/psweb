package com.pagesociety.web.page.component;

import com.pagesociety.web.page.Component;
import com.pagesociety.web.page.Container;

public class LabelComponent extends Component
{
	private String value;

	public LabelComponent(Container parent, String id, String class_name)
	{
		super(parent, id, class_name);
		value = "";
	}

	public void render(StringBuilder b)
	{
		b.append("<div ");
		render_attributes_id_class_style(b);
		b.append(">");
		b.append(value);
		b.append("</div>\n");
	}

	public void setValue(String value)
	{
		this.value = value;
	}
}
