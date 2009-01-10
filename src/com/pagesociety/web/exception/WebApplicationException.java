package com.pagesociety.web.exception;

public class WebApplicationException extends Exception
{
	private static final long serialVersionUID = 1800014981832535683L;
	
	private int error_code = 0;


	public WebApplicationException(String message, int error_code,Throwable t)
	{
		super(message, t);
		this.error_code = error_code;		
	}
	
	public WebApplicationException(String message, Throwable t)
	{
		this(message,0,t);
	}
	public WebApplicationException(String message,int error_code)
	{
		this(message,error_code,null);
	}
	
	public WebApplicationException(String message)
	{
		this(message,0,null);
	}
	
	public int getCode()
	{
		return error_code;
	}
}
