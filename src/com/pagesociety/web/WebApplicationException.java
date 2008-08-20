package com.pagesociety.web;

public class WebApplicationException extends Exception
{
	private static final long serialVersionUID = 1800014981832535683L;

	public WebApplicationException(String message)
	{
		super(message);
	}

	public WebApplicationException(String message, Throwable t)
	{
		super(message, t);
	}
}
