package com.pagesociety.web.template;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class FreemarkerRenderer
{
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(FreemarkerRenderer.class);

	private  String _template_root;
	private  Configuration _cfg;

	public FreemarkerRenderer(String template_root)
	{
		init(template_root);
	}

	public void init(String template_root)
	{
		logger.debug("FreemarkerRenderer - initialized with " + template_root);
		_template_root = template_root;
		_cfg = new Configuration();
		try
		{
			_cfg.setDirectoryForTemplateLoading(new File(template_root));
			_cfg.setURLEscapingCharset("ISO-8859-1");
			_cfg.setNumberFormat("0.#####");
		}
		catch (IOException e)
		{
			logger.error("init(String) - ERROR!!! FreemarkerRenderer.init: CAN NOT FIND DIRECTORY " + template_root, e);
			logger.error("init(String)", e);
		}
		_cfg.setObjectWrapper(new DefaultObjectWrapper());
	}
	
	public  String render(String templateName, Map<String, Object> pagedata)
	 throws IOException, TemplateException 
	{
		// long t = System.currentTimeMillis();
		StringWriter out = new StringWriter();
		render(templateName, pagedata, out);
		// System.out.println("FreemarkerRenderer " +
		// (System.currentTimeMillis() - t));
		return out.toString();
	}

	public String render(String templateName, Map<String, Object> pagedata,
			Writer out) throws IOException, TemplateException 
	{
		if (templateName.startsWith("/"))
			templateName = templateName.substring(1);
		Template temp = _cfg.getTemplate(templateName);
		temp.process(pagedata, out);
		out.flush();
		return out.toString();
	}

	public String getTemplateRoot()
	{
		return _template_root;
	}
}
