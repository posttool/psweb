package com.pagesociety.web.module.ecommerce;

@SuppressWarnings("serial")
public class BillingGatewayException extends Exception 
{
	private float amount;
	public BillingGatewayException(String msg)
	{
		super(msg);
	}
	
	public BillingGatewayException(String msg,float amount)
	{
		super(msg);
		this.amount = amount;
	}

	public BillingGatewayException(String msg, Throwable e)
	{
		super(msg, e);
	}
	
	public float getAmount()
	{
		return amount;
	}
}
