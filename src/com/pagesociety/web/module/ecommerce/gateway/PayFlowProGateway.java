package com.pagesociety.web.module.ecommerce.gateway;

 import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;


import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.module.WebModule;
import com.pagesociety.web.module.ecommerce.gateway.BillingGatewayException;
import com.pagesociety.web.module.ecommerce.gateway.BillingGatewayResponse;
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
 		return null;
 	}
 	
 	/* Transaction sales are submitted and immediately flagged for settlement.*/
 	public BillingGatewayResponse doSale(Entity billing_record,double amount,String ccvn) throws BillingGatewayException
 	{
 		return null;
 	}
 	
 	/* Transaction authorizations are authorized immediately but are not flagged for settlement.
 	 *  These transactions must be flagged for settled using doCapture.
 	 *  Authorizations typically remain active for 3 to 7 business days. */
 	public BillingGatewayResponse doAuth(Entity billing_record,double amount,String ccvn) 	throws BillingGatewayException
 	{
 		return null;
 	}
 	
 	/* Transaction captures flag existing authorizations for settlement.
 	 * Only authorizations can be captured. Captures can be submitted for an amount
 	 * equal to or less than the original authorization. */
 	public BillingGatewayResponse doCapture(Entity billing_record,double amount,String ref_num) 	throws BillingGatewayException
 	{
 		return null;
 	}
 	
 	/* Transaction voids will cancel an existing sale or captured authorization. 
 	 * In addition, non-captured authorizations can be voided to prevent any future capture. 
 	 * Voids can only occur if the transaction has not been settled.*/
 	public BillingGatewayResponse doVoid(Entity billing_record,String ref_num) 	throws BillingGatewayException
 	{
 		return null;
 	}
 	
 	/*Transaction refunds will reverse a previously settled transaction. 
 	 *If the transaction has not been settled, it must be voided instead of refunded.*/
 	public BillingGatewayResponse doRefund(Entity billing_record,double amount,String ref_num) 	throws BillingGatewayException
 	{
 		return null;
 	}
 	
 	/*Transaction credits apply a negative amount to the cardholder’s card.
 	 *In most situations, credits are disabled as transaction refunds should
 	 *be used instead.*/	
 	public BillingGatewayResponse doCredit(Entity billing_record,double amount) 	throws BillingGatewayException
 	{
 		return null;
 	}
 	
 	


    public Map<String,String> do_ppf_validate(String cc_no,
    										  String exp_mo,
    										  String exp_yr_2_digits,
    										  String address,
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
					"STREET",address,
					"ZIP",zip,
					"CVV2",ccvn);
    	
    	check_ppf_response(response);
        return response;
    }
 	
    public Map<String,String> do_ppf_sale(String cc_no,
			  							  String exp_mo,
			  							  String exp_yr_2_digits,
			  							  String address,
			  							  String zip,
			  							  String ccvn,
			  							  double amt,
			  							  String orderno,
			  							  String comment) throws PPFException
    {
    	String samt = normalize_amount(amt);
        Map<String,String> response = do_ppf_request(        			
        		"TRXTYPE","S",
				"TENDER","C",
    			"ACCT",cc_no,
				"EXPDATE",exp_mo+exp_yr_2_digits,
				"AMT",samt,
				"STREET",address,
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
			  							  String address,
			  							  String zip,
			  							  String ccvn,
			  							  double amt,
			  							  String orderno,
			  							  String comment) throws PPFException
    {
    	String samt = normalize_amount(amt);
    	Map<String,String> response = do_ppf_request(
        		"TRXTYPE","A",
				"TENDER","C",
    			"ACCT",cc_no,
				"EXPDATE",exp_mo+exp_yr_2_digits,
				"AMT",samt,
				"STREET",address,
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
    
    public Map<String,String> do_ppf_refund(String original_ref_id) throws Exception
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
			  								double amt) throws Exception
    {
    	String samt = normalize_amount(amt);
    	cc_no = normalize_cc_no(cc_no);
    	Map<String,String> response = do_ppf_request(
        			"TRXTYPE","C",
					"TENDER","C",
        			"ACCT",cc_no,
					"EXPDATE",exp_mo+exp_yr_2_digits,
					"AMT",samt);    	 
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
     		throw new PPFException("COMMUNICATION ERROR. RETRY LATER. SPECIFIC CODE WAS "+result,false);
     	    	
     	switch(result)
     	{
     		case 0:
     			break;
     		case 12://declined
     		case 23://invalid acct number
     		case 24://invalid expr date
     			throw new PPFException(response.get("RESPMSG"),true);
     		default:
     			throw new PPFException(response.get("RESPMSG"),false);
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
     
     
     
  ////////////////////////////////////////////////////////////////////////////////////////////////
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
        