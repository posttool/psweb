package com.pagesociety.web;

public class InitializationException extends Exception
{
	private static final long serialVersionUID = 7930594911218649539L;

	public InitializationException(String message)
	{
		super(message);
	}

	public InitializationException(String message, Throwable t)
	{
		super(message, t);
	}
}
