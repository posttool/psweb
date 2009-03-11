package com.pagesociety.web.module.ecommerce.gateway;

@SuppressWarnings("serial")
public class BillingGatewayException extends Exception 
{
	private double amount;
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
	
	public double getAmount()
	{
		return amount;
	}
}
