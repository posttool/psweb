package com.pagesociety.web.module.ecommerce;

@SuppressWarnings("serial")
public class BillingGatewayException extends Exception 
{
	public BillingGatewayException(String msg)
	{
		super(msg);
	}

	public BillingGatewayException(String msg, Throwable e)
	{
		super(msg, e);
	}
}
