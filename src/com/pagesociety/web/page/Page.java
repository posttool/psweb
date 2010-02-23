package com.pagesociety.web.page;

import java.util.Stack;

import com.pagesociety.web.page.component.ImageComponent;
import com.pagesociety.web.page.component.InputComponent;
import com.pagesociety.web.page.component.LabelComponent;

public class Page
{
	private HeadContainer head;
	private BodyContainer body;
	private Stack<Container> stack;
	private Container target;

	public Page()
	{
		head = new HeadContainer();
		body = new BodyContainer(this);
		target = body;
		stack = new Stack<Container>();
	}

	public BodyContainer getBody()
	{
		return body;
	}

	public HeadContainer getHead()
	{
		return head;
	}

	public String render()
	{
		StringBuilder b = new StringBuilder();
		render(b);
		return b.toString();
	}

	public void render(StringBuilder b)
	{
		b.append("<html>");
		head.render(b);
		body.render(b);
		b.append("</html>");
	}

	// how to make a stateful builder
	public Container beginContainer(String id)
	{
		Container c = new Container(target, id);
		stack.push(target);
		target = c;
		return c;
	}

	public Container endContainer()
	{
		return stack.pop();
	}

	public InputComponent addInput(String id, String type, String value)
	{
		InputComponent i = new InputComponent(target, id);
		i.setType(type);
		i.setValue(value);
		return i;
	}

	public LabelComponent addLabel(String text)
	{
		LabelComponent i = new LabelComponent(target, null, null);
		i.setValue(text);
		return i;
	}

	public ImageComponent addImage(String id, String src)
	{
		ImageComponent i = new ImageComponent(target, id, null);
		i.setSrc(src);
		return i;
	}
}
