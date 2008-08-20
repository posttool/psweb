package com.pagesociety.web.amf;

@SuppressWarnings("serial")
public class AmfException extends RuntimeException
{
	public AmfException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public AmfException(String message)
	{
		super(message);
	}
}
