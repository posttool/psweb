package com.pagesociety.web.page;

public class BodyContainer extends Container
{
	public BodyContainer(Page page)
	{
		super(null, null, null);
		this.page = page;
	}
	
	public void render(StringBuilder b)
	{
		int s = children.size();
		b.append("<body ");
		render_attributes_id_class_style(b);
		b.append(">\n");
		for (int i = 0; i < s; i++)
			children.get(i).render(b);
		b.append("</body>\n");
	}
}
