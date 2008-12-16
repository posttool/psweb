package com.pagesociety.web.module.ecommerce;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.module.WebModule;

public class TestBillingGateway extends WebModule implements IBillingGateway
{
	
	/* Check if card is valid */
	public BillingGatewayResponse doValidate(Entity billing_record) throws BillingGatewayException
	{
		BillingGatewayResponse response = new BillingGatewayResponse();
		System.out.println("DOING VALIDATION OF BILLING RECORD:");
		System.out.println(billing_record.toString());
		return response;
		
	}
	
	/* Transaction sales are submitted and immediately flagged for settlement.*/
	public BillingGatewayResponse doSale(Entity billing_record,float amount) 	throws BillingGatewayException
	{
		BillingGatewayResponse response = new BillingGatewayResponse();
		System.out.println("DOING SALE FOR BILLING RECORD IN THE AMOUNT OF: "+amount);
		System.out.println(billing_record.toString());
		return response;
	}
	/* Transaction authorizations are authorized immediately but are not flagged for settlement.
	 *  These transactions must be flagged for settled using doCapture.
	 *  Authorizations typically remain active for 3 to 7 business days. */
	public BillingGatewayResponse doAuth(Entity billing_record,float amount) 	throws BillingGatewayException
	{
		BillingGatewayResponse response = new BillingGatewayResponse();
		System.out.println("DOING AUTH FOR BILLING RECORD IN THE AMOUNT OF: "+amount);
		System.out.println(billing_record.toString());
		return response;
	}
	
	/* Transaction captures flag existing authorizations for settlement.
	 * Only authorizations can be captured. Captures can be submitted for an amount
	 * equal to or less than the original authorization. */
	//TODO: need auth code here probably
	public BillingGatewayResponse doCapture(Entity billing_record,float amount) 	throws BillingGatewayException
	{
		BillingGatewayResponse response = new BillingGatewayResponse();
		System.out.println("DOING CAPTURE FOR BILLING RECORD IN THE AMOUNT OF: "+amount);
		System.out.println(billing_record.toString());
		return response;
	}
	
	/* Transaction voids will cancel an existing sale or captured authorization. 
	 * In addition, non-captured authorizations can be voided to prevent any future capture. 
	 * Voids can only occur if the transaction has not been settled.*/
	
	//TODO: need some sort of transaction code here probably
	public BillingGatewayResponse doVoid(Entity billing_record) 	throws BillingGatewayException
	{
		BillingGatewayResponse response = new BillingGatewayResponse();
		System.out.println("DOING VOID FOR BILLING RECORD.");
		System.out.println(billing_record.toString());
		return response;
	}
	
	/*Transaction refunds will reverse a previously settled transaction. 
	 *If the transaction has not been settled, it must be voided instead of refunded.*/
	public BillingGatewayResponse doRefund(Entity billing_record,float amount) 	throws BillingGatewayException
	{
		BillingGatewayResponse response = new BillingGatewayResponse();
		System.out.println("DOING REFUND FOR BILLING RECORD IN THE AMOUNT OF: "+amount);
		System.out.println(billing_record.toString());
		return response;
	}
	
	/*Transaction credits apply a negative amount to the cardholder’s card.
	 *In most situations, credits are disabled as transaction refunds should
	 *be used instead.*/	
	public BillingGatewayResponse doCredit(Entity billing_record,float amount) 	throws BillingGatewayException
	{
		BillingGatewayResponse response = new BillingGatewayResponse();
		System.out.println("DOING CREDIT FOR BILLING RECORD IN THE AMOUNT OF: "+amount);
		System.out.println(billing_record.toString());
		return response;
	}
	
	/*Transaction updates can be used to update previous transactions 
	 *with specific order information,
	 *such as a tracking number and shipping carrier.show/hide details */
	

}
