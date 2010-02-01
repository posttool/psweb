package com.pagesociety.web.module.ecommerce.gateway;

import com.pagesociety.persistence.Entity;

public interface IBillingGateway
{
	//some of these probably need to take into account some sort of transaction id that the gateway will provide//
	
	/* Check if card is valid */
	public BillingGatewayResponse doValidate(String first_name,String middle_initial,String last_name,String add_1,String add_2,String city,String state,String country,String postal_code,int cc_type,String cc_no,int exp_month,int exp_year,String ccvn) 	throws BillingGatewayException;
	
	/* Transaction sales are submitted and immediately flagged for settlement.*/
	public BillingGatewayResponse doSale(Entity billing_record,double amount,String ccvn) 	throws BillingGatewayException;
	
	/* Transaction authorizations are authorized immediately but are not flagged for settlement.
	 *  These transactions must be flagged for settled using doCapture.
	 *  Authorizations typically remain active for 3 to 7 business days. */
	public BillingGatewayResponse doAuth(Entity billing_record,double amount,String ccvn) 	throws BillingGatewayException;
	
	/* Transaction captures flag existing authorizations for settlement.
	 * Only authorizations can be captured. Captures can be submitted for an amount
	 * equal to or less than the original authorization. */
	public BillingGatewayResponse doCapture(Entity billing_record,double amount,String ref_num) 	throws BillingGatewayException;
	
	/* Transaction voids will cancel an existing sale or captured authorization. 
	 * In addition, non-captured authorizations can be voided to prevent any future capture. 
	 * Voids can only occur if the transaction has not been settled.*/
	public BillingGatewayResponse doVoid(Entity billing_record,String ref_num) 	throws BillingGatewayException;
	
	/*Transaction refunds will reverse a previously settled transaction. 
	 *If the transaction has not been settled, it must be voided instead of refunded.*/
	public BillingGatewayResponse doRefund(Entity billing_record,double amount,String ref_num) 	throws BillingGatewayException;
	
	/*Transaction credits apply a negative amount to the cardholder’s card.
	 *In most situations, credits are disabled as transaction refunds should
	 *be used instead.*/	
	public BillingGatewayResponse doCredit(Entity billing_record,double amount) 	throws BillingGatewayException;
	
	/*Transaction updates can be used to update previous transactions 
	 *with specific order information,
	 *such as a tracking number and shipping carrier.show/hide details */
	
	

	
}
