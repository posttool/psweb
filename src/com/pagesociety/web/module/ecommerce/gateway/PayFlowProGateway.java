package com.pagesociety.web.module.ecommerce.gateway;

 import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.module.WebModule;
import com.pagesociety.web.module.ecommerce.billing.BillingModule;
import com.pagesociety.web.module.ecommerce.gateway.BillingGatewayException;
import com.pagesociety.web.module.ecommerce.gateway.BillingGatewayResponse;
import com.pagesociety.web.module.util.Validator;
import com.sun.corba.se.spi.orbutil.fsm.Guard.Result;

  public class PayFlowProGateway extends WebModule implements IBillingGateway 
  {
        
     public static final String TARGET_HTTPS_SERVER = "pilot-payflowpro.paypal.com"; 
     public static final int    TARGET_HTTPS_PORT   = 443; 

     private String payflow_vendor  = "new226423611";
     private String payflow_user    = payflow_vendor;
     private String payflow_partner = "wfb";
     private String payflow_pwd	    = "nc1203091996";

     
     public static void main(String[] args) throws Exception 
     {
        PayFlowProGateway c = new PayFlowProGateway();
        //Map<String,String> response = c.do_ppf_sale(24.00);
        //dump_nvp_response(response);
     }
     
     /* Check if card is valid */
 	public BillingGatewayResponse doValidate(String first_name,String middle_initial,String last_name,String add_1,String add_2,String city,String state,String country,String postal_code,int cc_type,String cc_no,int exp_month,int exp_year,String ccvn) throws BillingGatewayException
 	{
		if(Validator.isEmptyOrNull(first_name))
			throw new BillingGatewayException("FIRST NAME IS REQUIRED");
		if(!Validator.isAlpha(first_name))
			throw new BillingGatewayException("FIRST NAME IS INVALID");
		
		if(Validator.isEmptyOrNull(last_name))
			throw new BillingGatewayException("LAST NAME IS REQUIRED");
		if(!Validator.isAlpha(last_name))
			throw new BillingGatewayException("LAST NAME IS INVALID");
		
		if(Validator.isEmptyOrNull(add_1))
			throw new BillingGatewayException("ADDRESS IS REQUIRED");
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
	
		validate_credit_card_number(cc_type,cc_no);
		
		////FINALLY SEND IT TO THE PROCESSOR TO VALIDATE/////
 		Map<String,String> ppf_response = null;
 		try{
 			ppf_response = do_ppf_validate(normalize_cc_no(cc_no), 
 									   normalize_month(exp_month),
 									   normalize_year(exp_year),
 									   add_1,
 									   city,
 									   state,
 									   postal_code,
 									   ccvn);
 		}catch(PPFException ppfe)
 		{
 			TRANSLATE_PPF_EXCEPTION(ppfe);
 		}
 		return TRANSLATE_PPF_RESPONSE(ppf_response);
 	}
 	
 	/* Transaction sales are submitted and immediately flagged for settlement. ccvn can be null*/
 	public BillingGatewayResponse doSale(Entity billing_record,double amount,String ccvn,String ponum,String comment) throws BillingGatewayException
 	{
 		
 		
 		String first_name 		= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_FIRST_NAME);
 		String middle_initial 	= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_MIDDLE_INITIAL);
 		String last_name 		= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_LAST_NAME);
 		String add_1 			= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_ADDRESS_LINE_1);
 		String add_2 			= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_ADDRESS_LINE_2);
 		String city 			= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_CITY);
 		String state 			= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_STATE);
 		String country 			= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_COUNTRY);
 		String postal_code 		= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_POSTAL_CODE);
 		String cc_no  			= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_CC_NO);
 		int exp_month 			= (Integer)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_EXP_MONTH);
 		int exp_year  			= (Integer)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_EXP_YEAR);
 		Map<String,String> ppf_response = null;
 		
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
 		
 		try{
 			ppf_response = do_ppf_sale(normalize_cc_no(cc_no), 
 									   normalize_month(exp_month),
 									   normalize_year(exp_year),
 									   first_name, 
 									   last_name,
 									   add_1,
 									   city,
 									   state,
 									   postal_code,
 									   country,
 									   ccvn,
 									   normalize_amount(amount),
 									   ponum,
 									   comment);
 		}catch(PPFException ppfe)
 		{
 			TRANSLATE_PPF_EXCEPTION(ppfe);
 		}
 		return TRANSLATE_PPF_RESPONSE(ppf_response);
 	}
 	
 	
 	/* Transaction authorizations are authorized immediately but are not flagged for settlement.
 	 *  These transactions must be flagged for settled using doCapture.
 	 *  Authorizations typically remain active for 3 to 7 business days. */
 	public BillingGatewayResponse doAuth(Entity billing_record,double amount,String ccvn,String ponum,String comment) 	throws BillingGatewayException
 	{
 		String first_name 		= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_FIRST_NAME);
 		String middle_initial 	= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_MIDDLE_INITIAL);
 		String last_name 		= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_LAST_NAME);
 		String add_1 			= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_ADDRESS_LINE_1);
 		String add_2 			= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_ADDRESS_LINE_2);
 		String city 			= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_CITY);
 		String state 			= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_STATE);
 		String country 			= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_COUNTRY);
 		String postal_code 		= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_POSTAL_CODE);
 		String cc_no  			= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_CC_NO);
 		int exp_month 			= (Integer)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_EXP_MONTH);
 		int exp_year  			= (Integer)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_EXP_YEAR);
 		Map<String,String> ppf_response = null;
 		
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
 		
 		try{
 			ppf_response = do_ppf_auth(normalize_cc_no(cc_no), 
 									   normalize_month(exp_month),
 									   normalize_year(exp_year),
 									   first_name, 
 									   last_name,
 									   add_1,
 									   city,
 									   state,
 									   country,
 									   postal_code,
 									   ccvn,
 									   normalize_amount(amount),
 									   ponum,
 									   comment);
 		}catch(PPFException ppfe)
 		{
 			TRANSLATE_PPF_EXCEPTION(ppfe);
 		}
 		return TRANSLATE_PPF_RESPONSE(ppf_response);
 	}
 	
 	/* Transaction captures flag existing authorizations for settlement.
 	 * Only authorizations can be captured. Captures can be submitted for an amount
 	 * equal to or less than the original authorization. Pass in null for amount to capture for original amount.*/
 	public BillingGatewayResponse doCapture(Entity billing_record,String ref_num,Double amount) 	throws BillingGatewayException
 	{
 		Map<String,String> ppf_response = null;
 		try{
 			ppf_response = do_ppf_capture(ref_num, 
 									  	  amount);
 		}catch(PPFException ppfe)
 		{
 			TRANSLATE_PPF_EXCEPTION(ppfe);
 		}
 		return TRANSLATE_PPF_RESPONSE(ppf_response);
 	}
 	
 	/* Transaction voids will cancel an existing sale or captured authorization. 
 	 * In addition, non-captured authorizations can be voided to prevent any future capture. 
 	 * Voids can only occur if the transaction has not been settled.*/
 	public BillingGatewayResponse doVoid(Entity billing_record,String ref_num) 	throws BillingGatewayException
 	{
 		Map<String,String> ppf_response = null;
 		try{
 			ppf_response = do_ppf_void(ref_num);
 		}catch(PPFException ppfe)
 		{
 			TRANSLATE_PPF_EXCEPTION(ppfe);
 		}
 		return TRANSLATE_PPF_RESPONSE(ppf_response);
 	}
 	
 	/*Transaction refunds will reverse a previously settled transaction. 
 	 *If the transaction has not been settled, it must be voided instead of refunded.*/
 	public BillingGatewayResponse doRefund(Entity billing_record,String ref_num) 	throws BillingGatewayException
 	{
 		Map<String,String> ppf_response = null;
 		try{
 			ppf_response = do_ppf_refund(ref_num);
 		}catch(PPFException ppfe)
 		{
 			TRANSLATE_PPF_EXCEPTION(ppfe);
 		}
 		return TRANSLATE_PPF_RESPONSE(ppf_response);
 	}
 	
 	/*Transaction credits apply a negative amount to the cardholder’s card.
 	 *In most situations, credits are disabled as transaction refunds should
 	 *be used instead.*/	
 	public BillingGatewayResponse doCredit(Entity billing_record,double amount) 	throws BillingGatewayException
 	{

 		String cc_no  			= (String)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_CC_NO);
 		int exp_month 			= (Integer)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_EXP_MONTH);
 		int exp_year  			= (Integer)billing_record.getAttribute(BillingModule.BILLINGRECORD_FIELD_EXP_YEAR);
 		Map<String,String> ppf_response = null;
 		try{
 			ppf_response = do_ppf_credit(normalize_cc_no(cc_no),
 										 normalize_month(exp_month),
 										 normalize_year(exp_year),
 										 normalize_amount(amount)
 										);
 		}catch(PPFException ppfe)
 		{
 			TRANSLATE_PPF_EXCEPTION(ppfe);
 		}
 		return TRANSLATE_PPF_RESPONSE(ppf_response);
 	}
 	
 	


    public Map<String,String> do_ppf_validate(String cc_no,
    										  String exp_mo,
    										  String exp_yr_2_digits,
    										  String address,
    										  String city,
    										  String state,
    										  String zip,
    										  String ccvn) throws PPFException
    {
    	/*null values arent included in the request*/
    	/* this if we set a value to null it means it is optional */
    	Map<String,String> response = do_ppf_request(
        			"TRXTYPE","A",
					"TENDER","C",
        			"ACCT",cc_no,
					"EXPDATE",exp_mo+exp_yr_2_digits,
					"AMT",0.00,
					"CITY",city,
					"STATE",state,
					"STREET",address,
					"ZIP",zip,
					"CVV2",ccvn);
    	
    	check_ppf_response(response);
        return response;
    }
 	
    public Map<String,String> do_ppf_sale(String cc_no,
			  							  String exp_mo,
			  							  String exp_yr_2_digits,
			  							  String firstname,
			  							  String lastname,
			  							  String address,
			  							  String city,
			  							  String state,
			  							  String country,
			  							  String zip,
			  							  String ccvn,
			  							  String amt,
			  							  String orderno,
			  							  String comment) throws PPFException
    {
        Map<String,String> response = do_ppf_request(        			
        		"TRXTYPE","S",
				"TENDER","C",
    			"ACCT",cc_no,
				"EXPDATE",exp_mo+exp_yr_2_digits,
				"AMT",amt,
				"FIRSTNAME",firstname,
				"LASTNAME",lastname,
				"STREET",address,
				"CITY",city,
				"STATE",state,
				"BILLTOCOUNTRY",country,
				"ZIP",zip,
				"CVV2",ccvn,
				"PONUM",orderno,
				"COMMENT1",comment); 
        
        check_ppf_response(response);
        return response;
    }
    
    
    public Map<String,String> do_ppf_auth(String cc_no,
			  							  String exp_mo,
			  							  String exp_yr_2_digits,
			  							  String firstname,
			  							  String lastname,
			  							  String address,
			  							  String city,
			  							  String state,
			  							  String country,
			  							  String zip,
			  							  String ccvn,
			  							  String amt,
			  							  String orderno,
			  							  String comment) throws PPFException
    {
    	Map<String,String> response = do_ppf_request(
        		"TRXTYPE","A",
				"TENDER","C",
    			"ACCT",cc_no,
				"EXPDATE",exp_mo+exp_yr_2_digits,
				"AMT",amt,
				"STREET",address,
				"CITY",city,
				"STATE",state,
				"BILLTOCOUNTRY",country,
				"ZIP",zip,
				"CVV2",ccvn,
				"PONUM",orderno,
				"COMMENT1",comment);    	 
        check_ppf_response(response);
    	return response;
    }

    
    /*pass in an amount of null to capture the original ref amount */
    /* we let you pass in an amount for less than the original amount
     * because of the case of partial shipments. if you do a capture for a lesser amount
     * you would then do a sale for the remaining amount when the rest of the
     * stuff ships. you could do a sale with an ORIGID parameter which would use
     * the card info from the original transaction. we dont have this exposed but
     * could add a different sale parameter
     */
    public Map<String,String> do_ppf_capture(String original_ref_id,Double amt) throws PPFException
    {
    	String samt= null;
    	if(amt != null)
    		samt = normalize_amount(amt);
        
    	Map<String,String> response = do_ppf_request(
        			"TRXTYPE","D",
        			"ORIGID",original_ref_id,
					"AMT",samt);    	 
        check_ppf_response(response);
    	return response;
    }
    
    public Map<String,String> do_ppf_void(String original_ref_id) throws PPFException
    {
        Map<String,String> response = do_ppf_request(
        			"TRXTYPE","V",
        			"ORIGID",original_ref_id);
        check_ppf_response(response);
        return response;
    }
    
    public Map<String,String> do_ppf_refund(String original_ref_id) throws PPFException
    {
        Map<String,String> response = do_ppf_request(
        			"TRXTYPE","C",
					"ORIGID",original_ref_id);    	 
        check_ppf_response(response);
        return response;
    }
    
    /* this is usally disabled. it is called a non referenced credit transaction */
    public Map<String,String> do_ppf_credit(String cc_no,
			  								String exp_mo,
			  								String exp_yr_2_digits,
			  								String amt) throws PPFException
    {

    	Map<String,String> response = do_ppf_request(
        			"TRXTYPE","C",
					"TENDER","C",
        			"ACCT",cc_no,
					"EXPDATE",exp_mo+exp_yr_2_digits,
					"AMT",amt);    	 
        return response;
    }


    //passing a null value means that the parameter wont show up in the request//
    private Map<String,String> do_ppf_request(Object... params) throws PPFException
     {
    	 Socket socket 					= null;
    	 Map<String,String> nvp_response = null;
		 String request_id  	= gen_request_id(); 
		 String current_request =  gen_nvp_request(request_id,params);		 
		 int tries = 0;
		 while(true)
		 {
			 try {    	 	 	 
	    		 socket = SSLSocketFactory.getDefault().createSocket(TARGET_HTTPS_SERVER, TARGET_HTTPS_PORT);
	    		 Writer out = new OutputStreamWriter(socket.getOutputStream(), "ISO-8859-1");
	    		 printSocketInfo((SSLSocket)socket);
	    		 out.write(current_request);
	    		 out.flush();  
	    		 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "ISO-8859-1"));
	    		 nvp_response = parse_nvp_response(in);
	    		 
	    		 if(nvp_response.get("DUPLICATE") != null)
	    		 {
	    			 /* somehow we ended up with a duplicate id or at least the server thinks it is a duplicate */
	    			 request_id  = gen_request_id(); 
	    			 current_request =  gen_nvp_request(request_id,params);		 
	    			 throw new Exception("continuing...");
	    		 }
				 break;	    		 
			 }catch(Exception e)
	    	{
				 try{
					 socket.close();
				 }catch(IOException ioe)
				 {
					 ERROR(ioe);
				 }
					 // ERROR(e);
				 if(++tries < 3)
				 {
					 try{
						Thread.sleep(1000);
					}catch(InterruptedException ie){/*oh well*/}

					 continue;
				 }
				 else	 
					 throw new PPFException("BAILING OUT IN BILLING GATEWAY MODULE BECAUSE OF WEIRD ERROR."+e.getMessage(),false);
	    	}
		 }
		 try{
			 socket.close();
		 }catch(IOException ioe)
		 {
			 ERROR(ioe);
		 }
		return nvp_response;
     }

     private Map<String,String> parse_nvp_response(BufferedReader in) throws IOException
     {
    	 Map<String,String> nvp_response = new HashMap<String,String>();
    	 String  line 			= null;
    	 boolean parsing_header = true;
    	 StringBuilder body 	= new StringBuilder();
    	 String status_line 	= in.readLine();
    	 nvp_response.put("http-status-line",status_line);
    	 while((line = in.readLine()) != null)
    	 {
    		if(line.length() == 0)
    		{
    			parsing_header = false;
    			continue;
    		}
    		if(parsing_header)
    		{
    			StringTokenizer st = new StringTokenizer(line,":");
    			try{
    				String key   = st.nextToken().trim();
    				String value = st.nextToken().trim();
    				nvp_response.put(key,value);
    			}catch(NoSuchElementException nsee)
    			{
    				//just fuck it if it is a weird header line//
    			}
    		}
    		else
    		{
    			body.append(line+"\n");
    		}
    	 }

    	 StringTokenizer st = new StringTokenizer(body.toString(),"&");
    	 while(st.hasMoreTokens())
    	 {
    		StringTokenizer nvp = new StringTokenizer(st.nextToken(),"=");
    		nvp_response.put(nvp.nextToken().trim(), nvp.nextToken().trim());
    	 }

    	 return nvp_response;
     }
     
     
     private static void dump_nvp_response(Map<String,String> response)
     {
    	 Iterator<String> it = response.keySet().iterator();
    	 while(it.hasNext())
    	 {
    		 String key = it.next();
    		 System.out.println(key+" = "+response.get(key));
    	 }
     }
     
     String payflow_product_name 			= "NewCaliforniaMusic";
     String payflow_product_version 		= "0.1";
     int 	payflow_request_timeout			= 45;
     String payflow_server					= TARGET_HTTPS_SERVER;
     
     private String gen_nvp_request(String request_id,Object... key_values)
     {
     	String body = gen_nvp_body(key_values);
    	StringBuilder buf = new StringBuilder(); 
    	int content_length = body.length();
    	gen_nvp_request_header(buf,
    							payflow_server,
    							request_id,
    							content_length,
    							payflow_request_timeout,
    							payflow_product_name,
    							payflow_product_version); 
    	buf.append(body);
    	return finalize_nvp_request(buf);
     }

     private void gen_nvp_request_header(StringBuilder buf,String server,String request_id,int content_length,int timeout,String product_name,String product_version)
     {
         buf.append("POST / HTTP/1.1\r\n");
         buf.append("Connection: close\r\n");
         buf.append("Content-Type: text/namevalue\r\n");
         buf.append("Content-Length: "+content_length+"\r\n");
         buf.append("Host: "+TARGET_HTTPS_SERVER+"\r\n");
         buf.append("X-VPS-REQUEST-ID: "+request_id+"\r\n");
         buf.append("X-VPS-CLIENT-TIMEOUT: "+String.valueOf(timeout)+"\r\n");
         buf.append("X-VPS-VIT-Integration-Product: "+product_name+"\r\n");
         buf.append("X-VPS-VIT-Integration-Version: "+product_version+"\r\n");
         buf.append("\r\n"); 	  
     }
     
     private String finalize_nvp_request(StringBuilder buf)
     {
    	return buf.toString(); 
     }
     

     Object[] MESSAGE_AUTH_PARAMS = new Object[]{	"VENDOR",payflow_vendor,
    	 											"USER",payflow_user,
    	 											"PARTNER",payflow_partner,
    	 											"PWD",payflow_pwd};
    	 									
     private String gen_nvp_body(Object... key_values)
     {
    	 StringBuilder buf = new StringBuilder();
    	 append_to_nvp_body(buf, key_values);
    	 append_to_nvp_body(buf, MESSAGE_AUTH_PARAMS);
    	 return finalize_nvp_body(buf);
    	 
     }
     
     private  void append_to_nvp_body(StringBuilder buf,Object... key_values)
     {    	 
    	 for(int i=0;i < key_values.length; i+=2)
    	 {
    		 String key 	= String.valueOf(key_values[i]);
    		 Object value 	= key_values[i+1];
    		if(value == null)
    			continue;
    		else
    		{
    			String svalue = String.valueOf(value).trim();
    			buf.append(key+"["+svalue.length()+"]="+value+"&");
    		}
    	} 
     }
     
     private String finalize_nvp_body(StringBuilder buf)
     {
    	 int l = buf.length();
    	if(buf.charAt(l-1)=='&')
    		buf.setLength(l-1);
    	String ret = buf.toString();
    	return ret;
     }

     
     private String gen_request_id()
     {
    	 return "NC"+String.valueOf(new Date().getTime());
     }
     
    

  	@SuppressWarnings("serial")
	class PPFException extends Exception
 	{
 		/* recoverable means is there anything the user can do to
 		 * prevent the exception from happening i.e. change the expr date
 		 */
 		public boolean recoverable = true;
 		public PPFException(String msg,boolean recoverable)
 		{
 			super(msg);
 			this.recoverable = recoverable;
 		}
 	}
  	private void check_ppf_response(Map<String,String> response) throws PPFException
  	{
     	String avsaddr_response = response.get("AVSADDR");
     	if(avsaddr_response != null && avsaddr_response.equals("N"))
     		throw new PPFException("ADDRESS MISMATCH",true);
     	String avszip_response  = response.get("AVSZIP");
     	if(avszip_response != null && avszip_response.equals("N"))
     		throw new PPFException("ADDRESS MISMATCH",true);

     	String cvv2match_response = response.get("CVV2MATCH");
     	if(cvv2match_response != null && cvv2match_response.equals("N"))
     		throw new PPFException("BAD SECURITY CODE",true);

     	int result = Integer.parseInt(response.get("RESULT"));
     	if(result < 0)
     		throw new PPFException("COMMUNICATION ERROR. RETRY LATER. SPECIFIC CODE WAS "+result+": "+response.get("RESPMSG"),false);//add message
     	    	
     	switch(result)
     	{
     		case 0:
     			break;
     		case 12://declined
     		case 23://invalid acct number
     		case 24://invalid expr date
     			throw new PPFException(result+": "+response.get("RESPMSG"),true);
     		default:
     			throw new PPFException(result+": "+response.get("RESPMSG"),false);
     	}
  	}
  	
  	private static String normalize_amount(double amt)
  	{
    	DecimalFormat df = new DecimalFormat("#####.##");
    	return df.format(amt);
  	}

  	private static String normalize_cc_no(String acct_no)
  	{
        return acct_no.replaceAll("[^\\d]", "" );
  	}
     
    private static String normalize_month(int month)
    {
    	if(month > 9)
    		return String.valueOf(month);
    	else
    		return "0"+String.valueOf(month);
    }
    
    private static String normalize_year(int year)
    {
    	String y_string = String.valueOf(year);
    	if(y_string.length() == 2)
    		return y_string;
    	else
    		return y_string.substring(2);
    }
     
 	private void TRANSLATE_PPF_EXCEPTION(PPFException ppfe) throws BillingGatewayException
 	{
 		throw new BillingGatewayException(ppfe.getMessage(),ppfe.recoverable); 
 	}
 	
 	private BillingGatewayResponse TRANSLATE_PPF_RESPONSE(Map<String,String> ppf_response) throws BillingGatewayException
 	{
 		if(ppf_response == null)
 			return null;
 		String PNREF 	= ppf_response.get("PNREF");
 		String MESSAGE 	= ppf_response.get("RESPMSG");
 		BillingGatewayResponse response = new BillingGatewayResponse(PNREF,MESSAGE);
 		Iterator<String> it = ppf_response.keySet().iterator();
 		while(it.hasNext())
 		{
 			String key = it.next();
 			response.setProperty(key, ppf_response.get(key));
 		}
 		return response;
 	}

  ////////////////////////////////////////////////////////////////////////////////////////////////
  ///// C C VALIDATION ///////////////////////////////////////////////////////////////////////////
 	
 	
 	private void validate_credit_card_number(int type,String number) throws BillingGatewayException
    {
    	
        Matcher m = Pattern.compile("[^\\d\\s\\.-]").matcher(number);
        
        if (m.find()) 
            throw new BillingGatewayException("Credit card number can only contain numbers, spaces, \"-\", and \".\"");
    
        Matcher matcher = Pattern.compile("[\\s\\.-]").matcher(number);
        number = matcher.replaceAll("");
        do_validate(number, type);
    }  
 	
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
 	
 	    ////////////////////////////////////////////////////////////////////////////////////
 	    //////////////////CRAP////////////////////////////////////////
 	private static void printSocketInfo(SSLSocket s)
     {
         System.out.println("Socket class: "+s.getClass());
         System.out.println("   Remote address = "
            +s.getInetAddress().toString());
         System.out.println("   Remote port = "+s.getPort());
         System.out.println("   Local socket address = "
            +s.getLocalSocketAddress().toString());
         System.out.println("   Local address = "
            +s.getLocalAddress().toString());
         System.out.println("   Local port = "+s.getLocalPort());
         System.out.println("   Need client authentication = "
            +s.getNeedClientAuth());
         SSLSession ss = s.getSession();
         System.out.println("   Cipher suite = "+ss.getCipherSuite());
         System.out.println("   Protocol = "+ss.getProtocol());
      }
  }
        