package com.pagesociety.web.page;

import java.util.ArrayList;
import java.util.List;

public class Container extends Component
{
	protected List<Component> children;

	public Container(Container parent, String id)
	{
		super(parent, id);
		children = new ArrayList<Component>();
	}

	public void render(StringBuilder b)
	{
		int s = children.size();
		b.append("<div id='" + id + "'>\n");
		for (int i = 0; i < s; i++)
			children.get(i).render(b);
		b.append("</div>\n");
	}
}
