package com.pagesociety.web.page;

import com.pagesociety.bdb.BDBQueryResult;
import com.pagesociety.persistence.Entity;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.page.component.InputComponent;

public class PageTest
{
	
	public static void main(String[] args)
	{
//		System.out.println(test0().render());
//		System.out.println("------------------------");
//		System.out.println(test1().render());
//		System.out.println("------------------------");
		System.out.println(test3().render());
	}
	
	public static Page test0()
	{
		Page p = new Page();
		p.beginContainer("xyz");
			p.addInput("X", "Y").setStyle("font","Arial","color","#ff00ff");
			p.addInput("A", "Y");
			p.beginContainer("abc");
				p.addLabel("Hi");
				p.addInput("AAA", "AAA");
				p.addImage("http://googogole.cinifd/dshjds.jpg");
			p.endContainer();
		p.endContainer();
		return p;
	}
	
	public static Page test1()
	{
		Page p = new Page();
		BodyContainer b = p.getBody();
		Container c = new Container(b, "id", null);
		new InputComponent(c, "ic", null);
		new InputComponent(c, "ci", "classy");
		return p;
	}
	
	public static Page test2()
	{
		Page p = new Page();
		p.addStyle("body", Style.BACKGROUND_COLOR, "#f0f0f0");
		p.addStyle("#table_id th", 
				Style.FONT_FAMILY, Style.FONT_FAMILY_ARIAL, 
				Style.FONT_SIZE, "10", 
				Style.FONT_WEIGHT, Style.FONT_WEIGHT_BOLD,
				Style.COLOR, "#cccccc", 
				Style.BACKGROUND_COLOR, "#eeeeee");
		p.addStyle("#table_id td", 
				Style.FONT_FAMILY, Style.FONT_FAMILY_ARIAL, 
				Style.FONT_SIZE, "10", 
				Style.COLOR, "#333333");
		p.addFontStyle(".small_label", Style.FONT_FAMILY_ARIAL, "7", "#555555");
		p.addFontStyle(".big_text", Style.FONT_FAMILY_ARIAL, "18", "#777777");
		//
		p.beginContainer("column0");
			p.addLabeledInput("first_name", "Topher", "FIRST NAME");
			p.addLabeledInput("last_name", "LaFata", "LAST NAME");
			p.beginContainer("footer");
				p.addLabel("This is a bunch of text about something.", "big_text");
				p.addImage("http://www.pricerock.com/UI/GoldChains_large.jpg");
				p.addTable(get_table_data(), null, "table_id", new String[]{"whatsit", "ok", "not"});
			p.endContainer();
		p.endContainer();
		return p;
	}
	
	public static Page test3()
	{
		Page p = new Page();
		// styles
		p.addStyle("body", 		Style.BACKGROUND_COLOR, "#444444", Style.FONT_FAMILY, Style.FONT_FAMILY_ARIAL);
		p.addStyle("div, th, td", Style.FONT_SIZE, "10", Style.COLOR, "#555555");
		p.addStyle("table", "border-collapse", "collapse");
		p.addStyle("table, th, td", "border", "1px solid #777777", "padding", "5px");
		p.addStyle("input[type=\"text\"]", "border", "1px solid #444444", Style.FONT_SIZE, "20", "padding", "3px", "margin", "0px");
		p.addStyle("#content", 	Style.WIDTH, "800px", Style.HEIGHT, "800px", "margin", "10px auto", "padding", "10px", "background-color", "#ffffff");
		p.addStyle("#logo_nav", Style.BACKGROUND_COLOR, "#cccccc", Style.HEIGHT, "50px", "margin-bottom", "10px");
		p.addStyle("#column0", 	Style.WIDTH, "380px", "padding-right", "10px");
		p.addStyle("#column1", 	Style.WIDTH, "380px");
		p.addStyle(".label_container", "margin-bottom", "10px");
		p.addStyle(".small_label", Style.FONT_FAMILY, "Arial", Style.FONT_SIZE, "8", Style.COLOR, "#555555", Style.LETTER_SPACING, ".1em");
		p.addStyle(".big_text", 	Style.FONT_FAMILY, "Arial", Style.FONT_SIZE, "18", Style.COLOR, "#777777");
		// set default styles for components
		p.setLabelClassName("small_label");
		p.setLabelContainerClassName("label_container");
		//
		p.beginContainer("content");
			p.beginContainer("logo_nav");
				p.addLabel("THIS IS SOME LITTLE TEXT");
			p.endContainer();
			p.beginContainer("columns",Container.LAYOUT_LEFT_TO_RIGHT);
				p.beginContainer("column0");
					p.addLabeledInput("first_name", "FIRST NAME", "Topher");
					p.addLabeledInput("last_name", "LAST NAME", "LaFata");
					p.addTable(get_table_data(), null, null, new String[]{"whatsit", "ok", "not"});
					p.addImage("http://www.pricerock.com/UI/GoldChains_large.jpg", 150, 150);
				p.endContainer();
				p.beginContainer("column1");
					p.addLabeledInput("more_info", "MORE INFO", "heya");
					p.addRichTextEditor(null,null);
				p.endContainer();
			p.endContainer();
		p.endContainer();
		return p;
	}

	private static PagingQueryResult get_table_data()
	{
		BDBQueryResult q = new BDBQueryResult();
		for (int i=0; i<10; i++)
		{
			Entity e = new Entity();
			e.setType("Whosie");
			e.setId(i);
			e.setAttribute("whatsit", Math.random());
			e.setAttribute("ok", Math.random()>.5?"yes":"no");
			e.setAttribute("not", Math.random()>.5?"maybe":"possible");
			q.add(e);
		}
		return new PagingQueryResult(q, 10, 0, 20);
		
	}
}
