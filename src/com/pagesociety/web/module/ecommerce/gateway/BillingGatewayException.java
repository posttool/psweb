package com.pagesociety.web.module.ecommerce.gateway;

@SuppressWarnings("serial")
public class BillingGatewayException extends Exception 
{

	public static final int CREATE_ACCOUNT_BILLING_FAILED 	= 0x02;
	public static final int NO_BILLING_RECORD 				= 0x03;
	public static final int BAD_CC_NUMBER 					= 0x04;
	public static final int BAD_CC_TYPE 					= 0x05;
	public static final int CC_NUMBER_EXPIRED 				= 0x06;
	
	private double amount;
	private boolean user_recoverable = false;
	public BillingGatewayException(String msg)
	{
		super(msg);
		user_recoverable = true;
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
