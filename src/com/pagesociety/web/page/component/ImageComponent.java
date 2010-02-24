package com.pagesociety.web.page.component;

import com.pagesociety.web.page.Component;
import com.pagesociety.web.page.Container;

public class ImageComponent extends Component
{
	private String src;
	private int w =-1;
	private int h =-1;

	public ImageComponent(Container parent, String id, String class_name)
	{
		super(parent, id, class_name);
	}

	public void render(StringBuilder b)
	{
		b.append("<img ");
		render_attributes_id_class_style(b);
		render_attrs(b, "src", src);
		if (w!=-1)
			render_attrs(b, "width", Integer.toString(w));
		if (h!=-1)
			render_attrs(b, "height", Integer.toString(h));
		b.append(" />\n");
	}

	public void setSrc(String src)
	{
		this.src = src;
	}
	
	public void setSize(int w, int h)
	{
		this.w = w;
		this.h = h;
	}
}
