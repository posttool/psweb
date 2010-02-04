package com.pagesociety.web.module.ecommerce.gateway;

@SuppressWarnings("serial")
public class BillingGatewayException extends Exception 
{
	private double amount;
	private boolean user_recoverable = false;
	public BillingGatewayException(String msg)
	{
		super(msg);
	}
	
	public BillingGatewayException(String msg,double amount)
	{
		super(msg);
		this.amount = amount;
	}

	public BillingGatewayException(String msg, Throwable e)
	{
		super(msg, e);
	}

	public BillingGatewayException(String msg,boolean recoverable)
	{
		super(msg);
		this.user_recoverable = recoverable;
	}
	
	public double getAmount()
	{
		return amount;
	}
	
	public boolean isUserRecoverable()
	{
		return user_recoverable;
	}
}
