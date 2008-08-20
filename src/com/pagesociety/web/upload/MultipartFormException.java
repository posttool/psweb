package com.pagesociety.web.upload;

public class MultipartFormException extends Exception
{
	private static final long serialVersionUID = 6088960090759042892L;

	public MultipartFormException(String msg)
	{
		super(msg);
	}

	public MultipartFormException(String msg, Throwable e)
	{
		super(msg, e);
	}
}
