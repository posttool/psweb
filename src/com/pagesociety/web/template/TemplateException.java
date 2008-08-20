package com.pagesociety.web.template;

import com.pagesociety.web.module.ModuleRequest;

public class TemplateException extends RuntimeException
{
	private static final long serialVersionUID = 3993090292660316466L;

	public TemplateException(String msg, ModuleRequest request)
	{
		super(msg + " " + request.getModuleName() + "/" + request.getMethodName());
	}
}
