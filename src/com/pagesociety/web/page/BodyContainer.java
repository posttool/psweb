package com.pagesociety.web.page;

public class BodyContainer extends Container
{
	public BodyContainer(Page page)
	{
		super(null, null);
		this.page = page;
	}
	
	public void render(StringBuilder b)
	{
		int s = children.size();
		b.append("<body>\n");
		for (int i = 0; i < s; i++)
			children.get(i).render(b);
		b.append("</body>\n");
	}
}
