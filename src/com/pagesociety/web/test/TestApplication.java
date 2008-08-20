package com.pagesociety.web.test;

import com.pagesociety.web.InitializationException;
import com.pagesociety.web.WebApplication;

public class TestApplication extends WebApplication
{

	public TestApplication() throws InitializationException
	{
		super();
		System.out.println("TEST APP OK");
	}
}
