package com.pagesociety.web.test;

import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;

public class TestApplication extends WebApplication
{

	public TestApplication() throws InitializationException
	{
		super();
		System.out.println("TEST APP OK");
	}
}
