package com.pagesociety.web.module.ecommerce.gateway;

import java.util.Calendar;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.pagesociety.persistence.Entity;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.module.WebModule;
import com.pagesociety.web.module.ecommerce.billing.BillingModule;
import com.pagesociety.web.module.util.Validator;

public class TestBillingGateway extends WebModule implements IBillingGateway
{
	public static final String PARAM_DO_FULL_CREDIT_CARD_VALIDATION = "do-full-credit-card-validation"; 
	private boolean do_full_credit_card_validation = false;
	public void init(WebApplication app,Map<String,Object> config) throws  InitializationException
	{
		String p = GET_OPTIONAL_CONFIG_PARAM(PARAM_DO_FULL_CREDIT_CARD_VALIDATION, config);
		if(p == null)
			do_full_credit_card_validation = false;
		else if(p.equalsIgnoreCase("true"))
			do_full_credit_card_validation = true;
	}
	/* Check if card is valid */
	public BillingGatewayResponse doValidate(String first_name,String middle_initial,String last_name,String add_1,String add_2,String city,String state,String country,String postal_code,int cc_type,String cc_no,int exp_month,int exp_year,String ccvn) throws BillingGatewayException
	{
		BillingGatewayResponse response = new BillingGatewayResponse();
		if(Validator.isEmptyOrNull(first_name))
			throw new BillingGatewayException("FIRST NAME IS REQUIRED");
		if(Validator.isEmptyOrNull(last_name))
			throw new BillingGatewayException("LAST NAME IS REQUIRED");
		if(Validator.isEmptyOrNull(add_1))
			throw new BillingGatewayException("ADRESS IS REQUIRED");
		if(Validator.isEmptyOrNull(city))
			throw new BillingGatewayException("CITY IS REQUIRED");
		if(Validator.isEmptyOrNull(state))
			throw new BillingGatewayException("STATE IS REQUIRED");
		if(Validator.isEmptyOrNull(country))
			throw new BillingGatewayException("COUNTRY IS REQUIRED");
		if(Validator.isEmptyOrNull(postal_code))
			throw new BillingGatewayException("POSTAL CODE IS REQUIRED");
		if(Validator.isEmptyOrNull(cc_no))
			throw new BillingGatewayException("CREDIT CARD NUMBER IS REQUIRED");


		
		
		switch(cc_type)
		{
			case BillingModule.CC_TYPE_VISA:
			case BillingModule.CC_TYPE_MASTERCARD:
			case BillingModule.CC_TYPE_DISCOVER:
				if(cc_no.length() < 16 && cc_no.length() != 13)
					throw new BillingGatewayException("CREDIT CARD NUMBER IS INVALID. NOT ENOUGH DIGITS.");
				break;
			case BillingModule.CC_TYPE_DINERS:
				if(cc_no.length() < 14)
					throw new BillingGatewayException("CREDIT CARD NUMBER IS INVALID. NOT ENOUGH DIGITS.");
				break;
			case BillingModule.CC_TYPE_AMEX:
				if(cc_no.length() < 15)
					throw new BillingGatewayException("CREDIT CARD NUMBER IS INVALID. NOT ENOUGH DIGITS.");
				break;
			default:
					throw new BillingGatewayException("BAD CREDIT CARD TYPE"); 
		}
					
		Calendar now = Calendar.getInstance();
		int year = now.get(Calendar.YEAR);
		int month = now.get(Calendar.MONTH)+1;
		

		if(year > exp_year)
		{
			throw new BillingGatewayException("BAD EXPIRATION DATE. CREDIT CARD IS EXPIRED.");
		}
		
		if(year == exp_year && month > exp_month)
		{
			throw new BillingGatewayException("BAD EXPIRATION DATE. CREDIT CARD IS EXPIRED.");
		}
	
		if(!do_full_credit_card_validation)
			return new BillingGatewayResponse();
		
		validate_credit_card_number(cc_type,cc_no);
		return response;
	}

		
	/* Transaction sales are submitted and immediately flagged for settlement.*/
	
	public BillingGatewayResponse doSale(Entity billing_record,double amount,String ccvn) 	throws BillingGatewayException
	{
		//double d = Math.random();
		//if(d > 0.85)
		//	throw new BillingGatewayException("FAILED BILLING BECAUSE YOU ARE IN THE CYCLE OF FAILURE.");

		BillingGatewayResponse response = new BillingGatewayResponse();
		System.out.println("DOING SALE FOR BILLING RECORD IN THE AMOUNT OF: "+amount+" "+billing_record);
		return response;
	}
	/* Transaction authorizations are authorized immediately but are not flagged for settlement.
	 *  These transactions must be flagged for settled using doCapture.
	 *  Authorizations typically remain active for 3 to 7 business days. */
	public BillingGatewayResponse doAuth(Entity billing_record,double amount,String ccvn) 	throws BillingGatewayException
	{
		BillingGatewayResponse response = new BillingGatewayResponse();
		System.out.println("DOING AUTH FOR BILLING RECORD IN THE AMOUNT OF: "+amount+" "+billing_record);
		return response;
	}
	
	/* Transaction captures flag existing authorizations for settlement.
	 * Only authorizations can be captured. Captures can be submitted for an amount
	 * equal to or less than the original authorization. */

	public BillingGatewayResponse doCapture(Entity billing_record,double amount,String auth_code) 	throws BillingGatewayException
	{
		BillingGatewayResponse response = new BillingGatewayResponse();
		System.out.println("DOING CAPTURE FOR BILLING RECORD IN THE AMOUNT OF: "+amount+" "+billing_record);
		return response;
	}
	
	/* Transaction voids will cancel an existing sale or captured authorization. 
	 * In addition, non-captured authorizations can be voided to prevent any future capture. 
	 * Voids can only occur if the transaction has not been settled.*/
	
	//TODO: need some sort of transaction code here probably
	public BillingGatewayResponse doVoid(Entity billing_record,String auth_code) 	throws BillingGatewayException
	{
		BillingGatewayResponse response = new BillingGatewayResponse();
		System.out.println("DOING VOID FOR BILLING RECORD."+" "+billing_record);
		//System.out.println(billing_record.toString());
		return response;
	}
	
	/*Transaction refunds will reverse a previously settled transaction. 
	 *If the transaction has not been settled, it must be voided instead of refunded.*/
	public BillingGatewayResponse doRefund(Entity billing_record,double amount,String auth_code) 	throws BillingGatewayException
	{
		BillingGatewayResponse response = new BillingGatewayResponse();
		System.out.println("DOING REFUND FOR BILLING RECORD IN THE AMOUNT OF: "+amount+" "+billing_record);
		//System.out.println(billing_record.toString());
		return response;
	}
	
	/*Transaction credits apply a negative amount to the cardholder’s card.
	 *In most situations, credits are disabled as transaction refunds should
	 *be used instead.*/	
	public BillingGatewayResponse doCredit(Entity billing_record,double amount) 	throws BillingGatewayException
	{
		BillingGatewayResponse response = new BillingGatewayResponse();
		System.out.println("DOING CREDIT FOR BILLING RECORD IN THE AMOUNT OF: "+amount+" "+billing_record);
		//System.out.println(billing_record.toString());
		return response;
	}
	
	/*Transaction updates can be used to update previous transactions 
	 *with specific order information,
	 *such as a tracking number and shipping carrier.show/hide details */
	
 
    private void validate_credit_card_number(int type,String number) throws BillingGatewayException
    {
    	
        Matcher m = Pattern.compile("[^\\d\\s.-]").matcher(number);
        
        if (m.find()) 
            throw new BillingGatewayException("Credit card number can only contain numbers, spaces, \"-\", and \".\"");
    
        Matcher matcher = Pattern.compile("[\\s.-]").matcher(number);
        number = matcher.replaceAll("");
        do_validate(number, type);
    }
 
    // Check that cards start with proper digits for
    // selected card type and are also the right length.    
 
    private void do_validate(String number, int type) throws BillingGatewayException
    {
        switch(type) 
        {
		
        case BillingModule.CC_TYPE_MASTERCARD:
            if (number.length() != 16 ||
                Integer.parseInt(number.substring(0, 2)) < 51 ||
                Integer.parseInt(number.substring(0, 2)) > 55)
            {
            	throw new BillingGatewayException("BAD MASTERCARD CREDIT CARD NUMBER");
            }
            break;
			
        case BillingModule.CC_TYPE_VISA:
            if ((number.length() != 13 && number.length() != 16) ||
                    Integer.parseInt(number.substring(0, 1)) != 4)
            {
            	throw new BillingGatewayException("BAD VISA CREDIT CARD NUMBER");
            }
            break;
			
        case BillingModule.CC_TYPE_AMEX:
            if (number.length() != 15 ||
                (Integer.parseInt(number.substring(0, 2)) != 34 &&
                    Integer.parseInt(number.substring(0, 2)) != 37))
            {
            	throw new BillingGatewayException("BAD AMEX CREDIT CARD NUMBER");
            }
            break;
			
        case BillingModule.CC_TYPE_DISCOVER:
            if (number.length() != 16 ||
                Integer.parseInt(number.substring(0, 5)) != 6011)
            {
            	throw new BillingGatewayException("BAD DISCOVER CREDIT CARD NUMBER");
            }
            break;
			
        case BillingModule.CC_TYPE_DINERS:
            if (number.length() != 14 ||
                ((Integer.parseInt(number.substring(0, 2)) != 36 &&
                    Integer.parseInt(number.substring(0, 2)) != 38) &&
                    Integer.parseInt(number.substring(0, 3)) < 300 ||
                        Integer.parseInt(number.substring(0, 3)) > 305))
            {
            	throw new BillingGatewayException("BAD DINERS CREDIT CARD NUMBER");
            }
            break;
        }
        luhnValidate(number);
    }
 
    // The Luhn algorithm is basically a CRC type
    // system for checking the validity of an entry.
    // All major credit cards use numbers that will
    // pass the Luhn check. Also, all of them are based
    // on MOD 10.
	
    private void luhnValidate(String numberString) throws BillingGatewayException
    {
        char[] charArray = numberString.toCharArray();
        int[] number = new int[charArray.length];
        int total = 0;
		
        for (int i=0; i < charArray.length; i++) {
            number[i] = Character.getNumericValue(charArray[i]);
        }
		
        for (int i = number.length-2; i > -1; i-=2) {
            number[i] *= 2;
			
            if (number[i] > 9)
                number[i] -= 9;
        }
		
        for (int i=0; i < number.length; i++)
            total += number[i];
		
            if (total % 10 != 0)
                throw new BillingGatewayException("INVALID CREDIT CARD NUMBER");
    }

}
