package com.pagesociety.web;

import java.io.CharArrayWriter;
import java.io.PrintWriter;

public class ErrorMessage
{
	private String message;
	private Throwable exception;
	private String exceptionType;
	private String stacktrace;

	public ErrorMessage()
	{
	}
	
	

	public ErrorMessage(Throwable exception)
	{
		super();
		this.message = exception.getMessage().replace('"', '\'');
		this.exception = exception;
		this.exceptionType = exception.getClass().getName();
	}



	public String getMessage()
	{
		return message;
	}

	public String getExceptionType()
	{
		return exceptionType;
	}
	
	public Throwable getException()
	{
		return exception;
	}

//	public String getStacktrace()
//	{
//		if (stacktrace != null)
//		{
//			return stacktrace;
//		}
//		CharArrayWriter caw = new CharArrayWriter();
//		PrintWriter pw = new PrintWriter(caw)
//		{
//			/*
//			 * ignore system settings - AS3 uses \n only (never \r)
//			 */
//			@Override
//			public void println()
//			{
//				super.print('\n');
//			}
//		};
//		exception.printStackTrace(pw);
//		stacktrace = caw.toString();
//		return stacktrace;
//	}

	public void setExceptionType(String exceptionType)
	{
		this.exceptionType = exceptionType;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}

	public void setStacktrace(String stackTrace)
	{
		this.stacktrace = stackTrace;
	}

	public String toString()
	{
		return exceptionType + ": " + message + "\n" + stacktrace;
	}
}
