package com.pagesociety.web.page.component;

import com.pagesociety.web.page.Component;
import com.pagesociety.web.page.Container;
import com.pagesociety.web.page.IEditor;

public class RichTextEditor extends Component implements IEditor
{
	public static final String YUI_SKIN_SAM = "yui-skin-sam";
	private Object value;

	public RichTextEditor(Container parent, String id, String class_name)
	{
		super(parent, id, class_name);
		if (page.getBody().getClassName() != null && !page.getBody().getClassName().equals(YUI_SKIN_SAM))
			throw new RuntimeException("ERROR: You have set the body classname and you trying to use YUI which needs a body class name! ");
		page.getBody().setClassName(YUI_SKIN_SAM);
		page.addStyleSrc("http://yui.yahooapis.com/2.8.0r4/build/fonts/fonts-min.css" );
		page.addStyleSrc("http://yui.yahooapis.com/2.8.0r4/build/editor/assets/skins/sam/simpleeditor.css" );
		page.addScriptSrc("http://yui.yahooapis.com/2.8.0r4/build/yahoo-dom-event/yahoo-dom-event.js");
		page.addScriptSrc("http://yui.yahooapis.com/2.8.0r4/build/element/element-min.js");
		page.addScriptSrc("http://yui.yahooapis.com/2.8.0r4/build/container/container_core-min.js");
		page.addScriptSrc("http://yui.yahooapis.com/2.8.0r4/build/editor/simpleeditor-min.js");
		//
		// TODO register a javascript function for get_value_of(id) for this component
	}

	@Override
	public void render(StringBuilder b)
	{
		b.append("<div>");
		b.append("<textarea id=\"editor\" name=\"editor\" rows=\"20\" cols=\"75\"> \n ");
		b.append(value);
		b.append("\n</textarea>  \n ");
		b.append("<script>  \n ");
		b.append("(function() { \n ");
		b.append("var Dom = YAHOO.util.Dom, \n");
		b.append("    Event = YAHOO.util.Event; \n");
		b.append("var myConfig = { \n");
		b.append("    height: '300px', \n");
		b.append("    width: '100%', \n");
		b.append("    dompath: true \n");
		b.append("}; \n ");
		b.append("var myEditor = new YAHOO.widget.SimpleEditor('editor', myConfig); \n"); //TODO add id to myEditor
		b.append("myEditor.render(); \n");
		b.append("})(); \n ");
		b.append("</script> \n ");
		b.append("</div> \n ");
	}
}
