package com.pagesociety.web.page;

import java.util.ArrayList;
import java.util.List;

public class Container extends Component
{
	protected List<Component> children;
	public static final int LAYOUT_NONE = 0;
	public static final int LAYOUT_TOP_TO_BOTTOM = 1;
	public static final int LAYOUT_LEFT_TO_RIGHT = 2;
	private int layout_direction = LAYOUT_TOP_TO_BOTTOM;

	public Container(Container parent, String id, String class_name)
	{
		super(parent, id, class_name);
		children = new ArrayList<Component>();
	}
	
	public void render(StringBuilder b)
	{
		int s = children.size();
		b.append("<div ");
		render_id_attr(b);
		render_class_attr(b);
		render_start_layout(b);
		b.append(">\n");
		for (int i = 0; i < s; i++)
		{
			Component c = children.get(i);
			if (layout_direction==LAYOUT_LEFT_TO_RIGHT)
				c.setStyle("float","left");
			c.render(b);
		}
		render_end_layout(b);
		b.append("</div>\n");
	}

	private void render_start_layout(StringBuilder b)
	{
		switch (layout_direction)
		{
		case LAYOUT_NONE:
		case LAYOUT_TOP_TO_BOTTOM:
			render_style_attr(b);
			break;
		case LAYOUT_LEFT_TO_RIGHT:
			if (style != null)
				System.err.println("CANT USE STYLE INFO WITH LEFT TO RIGHT LAYOUT");
			b.append("style=\"");
			render_styles(b, "position", "relative", "float", "left");
			b.append("\" ");
			break;
		}
	}

	private void render_end_layout(StringBuilder b)
	{
		switch (layout_direction)
		{
		case LAYOUT_NONE:
		case LAYOUT_TOP_TO_BOTTOM:
			break;
		case LAYOUT_LEFT_TO_RIGHT:
			b.append("<br style=\"clear:all;\"/>");
			break;
		}
	}

	public void setLayoutDirection(int layout_direction)
	{
		this.layout_direction = layout_direction;
	}
}
