package com.pagesociety.web.page.component;

import com.pagesociety.web.page.Component;
import com.pagesociety.web.page.Container;

public class InputComponent extends Component
{
	private String type;
	private String value;

	public InputComponent(Container parent, String id)
	{
		super(parent, id);
		type = "input";
		value = "";
	}

	public void render(StringBuilder b)
	{
		render_self_closed_tag(b, "input", "id", id, "type", type, "value", value);
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
