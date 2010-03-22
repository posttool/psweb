package com.pagesociety.web.module.ecommerce.gateway;

@SuppressWarnings("serial")
public class BillingGatewayException extends Exception 
{

	public boolean user_recoverable = false;
	private int failure_code;
	
	public static final int FAILURE_CODE_SYSTEM_FAILURE			= 0x00;
	public static final int FAILURE_CODE_FAILED					= 0x01;
	public static final int FAILURE_CODE_FAILED_ON_ADDRESS		 = 0x02;
	public static final int FAILURE_CODE_FAILED_ON_EXPR_DATE	= 0x03;
	public static final int FAILURE_CODE_FAILED_ON_SECURITY_CODE = 0x04;
	
	public BillingGatewayException(String msg)
	{
		super(msg);
		user_recoverable = true;
		failure_code = FAILURE_CODE_FAILED;
	}

	public BillingGatewayException(String msg,boolean recoverable)
	{
		super(msg);
		this.user_recoverable = recoverable;
		failure_code = FAILURE_CODE_FAILED;
	}

	public BillingGatewayException(String msg,boolean recoverable,int failure_code)
	{
		super(msg);
		this.user_recoverable = recoverable;
		this.failure_code = failure_code;
	}

	public boolean isUserRecoverable()
	{
		return user_recoverable;
	}
	
	
	public int getFailureCode()
	{
		return failure_code;
	}
	
	
}
