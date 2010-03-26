package com.pagesociety.web.page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.pagesociety.web.IPageRenderer;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.page.component.ImageComponent;
import com.pagesociety.web.page.component.InputComponent;
import com.pagesociety.web.page.component.LabelComponent;
import com.pagesociety.web.page.component.PagingSortedTable;
import com.pagesociety.web.page.component.RichTextEditor;

public class Page implements IPageRenderer
{
	private HeadContainer head;
	private BodyContainer body;
	// 
	private Stack<Container> stack;
	private Container target;
	//
	private Map<String,Component> component_map_by_id;
	private List<String> input_ids;
	


	public Page()
	{
		head = new HeadContainer();
		body = new BodyContainer(this);
		target = body;
		stack = new Stack<Container>();
		input_ids = new ArrayList<String>();
		component_map_by_id = new HashMap<String,Component>();
	}

	public BodyContainer getBody()
	{
		return body;
	}

	public HeadContainer getHead()
	{
		return head;
	}

	// 
	
	public void addScriptSrc(String src)
	{
		head.addScriptSrc(src);
	}
	public void addStyleSrc(String src)
	{
		head.addStyleSrc(src);
	}
	
	public void addStyle(String name, String... style)
	{
		head.addStyle(name, style);
	}

	public void addFontStyle(String style_name, String font_family, String font_size,
			String color)
	{
		addStyle(style_name, Style.FONT_FAMILY, font_family, Style.FONT_SIZE, font_size, Style.COLOR, color);
	}

	public String render()
	{
		StringBuilder b = new StringBuilder();
		render(b);
		return b.toString();
	}

	public void render(StringBuilder b)
	{
		b.append("<html>\n");
		head.render(b);
		body.render(b);
		b.append("</html>");
	}
	
	// document management
	public boolean registerComponent(String id, Component c)
	{
		if (id==null)
			return true;
		if (component_map_by_id.containsKey(id))
			return false;
		component_map_by_id.put(id, c);
		if (c instanceof IEditor) 
			input_ids.add(id);
		//TODO aggregate by class_name
		return true;
	}
	
	public Component getById(String id)
	{
		return component_map_by_id.get(id);
	}

	// ///////////////////////////////////////////////////
	// page builder
	public Container beginContainer(String id)
	{
		return beginContainer(id, null, Container.LAYOUT_NONE);
	}
	
	public Container beginContainer(String id, String class_name)
	{
		return beginContainer(id, class_name, Container.LAYOUT_NONE);
	}
	
	public Container beginContainer(String id, int layout_direction)
	{
		return beginContainer(id, null, layout_direction);
	}
	
	

	public Container beginContainer(String id, String class_name, int layout_direction)
	{
		Container c = new Container(target, id, class_name);
		c.setLayoutDirection(layout_direction);
		stack.push(target);
		target = c;
		return c;
	}

	public Container endContainer()
	{
		target = stack.pop();
		return target;
	}
	
	public Component add(Component c)
	{
		c.setParent(target);
		return c;
	}

	public InputComponent addInput(String id, String value)
	{
		if (id == null)
			throw new RuntimeException("null ptr! Page.addInput requires an id");
		InputComponent i = new InputComponent(target, id, null);
		i.setType("text");
		i.setValue(value);
		return i;
	}

	public Container addLabeledInput(String id, String label_text, String value)
	{
		Container c = beginContainer(id + "_labeled_input_container", label_container_class_name);
			addLabel(label_text, label_class_name);
			addInput(id, value);
		endContainer();
		return c;
	}
	
	// style defaults
	private String label_class_name;
	private String label_container_class_name;

	public String getLabelClassName()
	{
		return label_class_name;
	}

	public void setLabelClassName(String label_class_name)
	{
		this.label_class_name = label_class_name;
	}

	public String getLabelContainerClassName()
	{
		return label_container_class_name;
	}

	public void setLabelContainerClassName(String label_container_class_name)
	{
		this.label_container_class_name = label_container_class_name;
	}

	public LabelComponent addLabel(String text)
	{
		return addLabel(text, null, null);
	}

	public LabelComponent addLabel(String text, String class_name)
	{
		return addLabel(text, null, class_name);
	}

	public LabelComponent addLabel(String text, String id, String class_name)
	{
		LabelComponent i = new LabelComponent(target, id, class_name);
		i.setValue(text);
		return i;
	}

	public ImageComponent addImage(String src)
	{
		return addImage(src,-1,-1);
	}
	public ImageComponent addImage(String src, int w, int h)
	{
		ImageComponent i = new ImageComponent(target, null, null);
		i.setSrc(src);
		i.setSize(w,h);
		return i;
	}

	public PagingSortedTable addTable(PagingQueryResult results, String... visible_fields)
	{
		return addTable(results, null, null, visible_fields);
	}

	public PagingSortedTable addTable(PagingQueryResult results, String class_name,
			String id, String[] visible_fields)
	{
		PagingSortedTable pst = new PagingSortedTable(target, id, class_name);
		pst.setResults(results);
		pst.setVisibleColumns(visible_fields);
		return pst;
	}
	
	/////////////
	public RichTextEditor addRichTextEditor(String id, String class_name)
	{
		RichTextEditor pst = new RichTextEditor(target, id, class_name);
		return pst;
	}

	
}
