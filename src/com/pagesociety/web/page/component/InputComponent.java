package com.pagesociety.web.page.component;

import com.pagesociety.web.page.Component;
import com.pagesociety.web.page.Container;
import com.pagesociety.web.page.IEditor;

public class InputComponent extends Component implements IEditor
{
	private String type;
	private String value;

	public InputComponent(Container parent, String id, String class_name)
	{
		super(parent, id, class_name);
		type = "text";
		value = "";
		// TODO register a javascript function for get_value_of(id) for this component
	}

	public void render(StringBuilder b)
	{
		b.append("<input ");
		render_attributes_id_class_style(b);
		render_attrs(b, "type", type, "value", value);
		b.append("/>\n");
	}

	

	public void setType(String type)
	{
		this.type = type;
	}

	public void setValue(String value)
	{
		this.value = value;
	}
}
