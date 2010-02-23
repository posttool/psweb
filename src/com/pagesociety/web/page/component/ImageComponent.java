package com.pagesociety.web.page.component;

import com.pagesociety.web.page.Component;
import com.pagesociety.web.page.Container;

public class ImageComponent extends Component
{
	private String src;

	public ImageComponent(Container parent, String id, String class_name)
	{
		super(parent, id, class_name);
	}

	public void render(StringBuilder b)
	{
		b.append("<img ");
		b.append(" ");
		render_class_attr(b);
		render_attrs(b, "src", src);
		b.append("/>\n");
	}

	public void setSrc(String src)
	{
		this.src = src;
	}
}
