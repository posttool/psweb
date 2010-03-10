package com.pagesociety.web.module.ecommerce.gateway;

@SuppressWarnings("serial")
public class BillingGatewayException extends Exception 
{

	public boolean user_recoverable = false;
	public BillingGatewayException(String msg)
	{
		super(msg);
		user_recoverable = true;
	}

	public BillingGatewayException(String msg,boolean recoverable)
	{
		super(msg);
		this.user_recoverable = recoverable;
	}
	
	public boolean isUserRecoverable()
	{
		return user_recoverable;
	}
}
