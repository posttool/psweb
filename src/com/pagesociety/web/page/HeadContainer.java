package com.pagesociety.web.page;

import java.util.List;

public class HeadContainer
{
	private String external_script_root;
	private String external_style_root;
	private List<String> external_scripts;
	private List<String> external_styles;
	private List<String> embedded_scripts;
	private List<String> embedded_styles;

	public void render(StringBuilder b)
	{
		b.append("<head>\n");
		// todo imports
		b.append("</head>\n");
	}
}
