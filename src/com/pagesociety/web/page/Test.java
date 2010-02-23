package com.pagesociety.web.page;

import com.pagesociety.web.page.component.InputComponent;

public class Test
{
	
	public static void main(String[] args)
	{
		System.out.println(test0().render());
		System.out.println("------------------------");
		System.out.println(test1().render());
	}
	
	public static Page test0()
	{
		Page p = new Page();
		p.beginContainer("xyz");
			p.addInput("X", "Y", "Z").setStyle("font","Arial","color","#ff00ff");
			p.addInput("A", "Y", "Z");
			p.beginContainer("abc");
				p.addLabel("Hi");
				p.addInput("AAA", "AAA", "AAA");
				p.addImage("s","http://googogole.cinifd/dshjds.jpg");
			p.endContainer();
		p.endContainer();
		return p;
	}
	
	public static Page test1()
	{
		Page p = new Page();
		BodyContainer b = p.getBody();
		Container c = new Container(b, "id");
		new InputComponent(c, "ic", null);
		new InputComponent(c, "ci", "classy");
		return p;
	}
}
