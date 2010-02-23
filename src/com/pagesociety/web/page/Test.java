package com.pagesociety.web.page;

import com.pagesociety.web.page.component.InputComponent;

public class Test
{
	
	public static void main(String[] args)
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
		
		System.out.println(p.render());
	}
	
	public static void test1()
	{
		Page p = new Page();
		BodyContainer b = p.getBody();
		Container c = new Container(b, "id");
		InputComponent ic = new InputComponent(c, "ic");
		String s = p.render();
		System.out.println(s);
	}
}
