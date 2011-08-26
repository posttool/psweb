package com.pagesociety.web.gateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.pagesociety.util.Base64;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.ModuleRequest;
import com.pagesociety.web.module.WebModule.CALLBACK;

public class JavaGateway
{
	private WebApplication _web_application;

	public JavaGateway(WebApplication web_application)
	{
		_web_application = web_application;
	}



	public void doService(UserApplicationContext user_context,
			HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{


			String request_path = request.getRequestURI().substring(request.getContextPath().length());
			ModuleRequest module_request = GatewayUtil.parseModuleRequest(request, request_path);
			String body 					= request.getParameter("payload");

			JavaGatewayRequest request_obj 	= null;
			JavaGatewayResponse jresponse 	= new JavaGatewayResponse();

			try{
				request_obj = (JavaGatewayRequest)ObjectFromString(body);

			}catch(ClassNotFoundException cnfe)
			{
				cnfe.printStackTrace();
				jresponse.request_time 		= 0;
				jresponse.response_time 	= new Date().getTime();
				jresponse.routing_id		= "unknown";
				jresponse.value				= cnfe;
				jresponse.response_code		= JavaGatewayResponse.MODULE_METHOD_INVOKE_ERROR;
			}

			module_request.setArguments(request_obj.arguments);
			/* need to figure out what the user context actually is here at some point*/
			module_request.setUserContext(user_context);
			Object return_value;

			jresponse.request_time 		= request_obj.request_time;
			jresponse.response_time 	= new Date().getTime();
			jresponse.routing_id		= request_obj.routing_id;
			try
			{
				return_value 			= _web_application.dispatch(module_request);
				jresponse.value 		= return_value;
				jresponse.response_code = JavaGatewayResponse.MODULE_METHOD_INVOKE_OK;
			}
			catch (Throwable e)
			{
				e.printStackTrace();
				jresponse.value = e;
				jresponse.response_code = JavaGatewayResponse.MODULE_METHOD_INVOKE_ERROR;
			}

			String text_response = ObjectToString(jresponse);
			response.setContentType("application/octet-stream");
			response.setContentLength(text_response.length());
			PrintWriter out = response.getWriter();
			out.write(text_response);
			out.close();

	}

	public static Object executeModuleMethod(String endpoint,String module_name,String method_name,Object... args) throws WebApplicationException
	{
		String url_string = endpoint+"/"+module_name+"/"+method_name+"/.jso";
		return execute_module_method_sync(url_string, args);
	}

	public static void executeModuleMethod(String endpoint,String module_name,String method_name,Object[] args,CALLBACK c) throws WebApplicationException
	{
		String url_string = endpoint+"/"+module_name+"/"+method_name+"/.jso";
		execute_module_method_async(url_string,args,null,c);
	}

	public static void executeModuleMethod(String endpoint,String module_name,String method_name,Object[] args,String routing_id,CALLBACK c) throws WebApplicationException
	{
		String url_string = endpoint+"/"+module_name+"/"+method_name+"/.jso";
		execute_module_method_async(url_string,args,routing_id,c);
	}

	private static Object execute_module_method_sync(String url_string,Object[] arguments) throws WebApplicationException
	{

		JavaGatewayRequest request 	= new JavaGatewayRequest();
		request.request_time 		= new Date().getTime();
		request.routing_id			= null;
		request.arguments			= arguments;

		try{
			   // Send data

		    URL url = new URL(url_string);
		    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		    conn.setConnectTimeout(10000);
		    conn.setDoOutput(true);
		    conn.setDoInput(true);
		    String encoded_request 	= ObjectToString(request);
		    OutputStreamWriter wr 	= new OutputStreamWriter(conn.getOutputStream());
		    wr.write("payload="+encoded_request);
		    wr.flush();
		    wr.close();

		    // Get the response

		    int response_code 	= conn.getResponseCode();
		    switch (response_code)
		    {
		    	case HttpURLConnection.HTTP_OK:
		    		String response 				 = convertStreamToString(conn.getInputStream());
		    		JavaGatewayResponse response_obj = (JavaGatewayResponse)ObjectFromString(response);
		    		conn.disconnect();
		    		switch (response_obj.response_code)
		    		{
	    				case JavaGatewayResponse.MODULE_METHOD_INVOKE_OK:
	    					return response_obj.value;
	    				case JavaGatewayResponse.MODULE_METHOD_INVOKE_ERROR:
	    					Exception e = (Exception)response_obj.value;
	    					throw e;
	    				default:
	    					throw new WebApplicationException("JavaGateway - REQUEST FAILED "+url_string);
		    		}
		    	default:
				    conn.disconnect();
				    throw new WebApplicationException("JavaGateway - REQUEST FAILED "+url_string+" with response code: "+response_code);
		    }


		}catch(Exception e)
		{
			throw new WebApplicationException("EXECUTE FAILED FOR "+url_string+" :"+e.getMessage(),e);
		}

	}

	private static void execute_module_method_async(final String url_string,final Object[] arguments, final String routing_id,final CALLBACK callback) throws WebApplicationException
	{

		Thread t = new Thread()
		{
			public void run()
			{
				JavaGatewayRequest request 	= new JavaGatewayRequest();
				request.request_time 		= new Date().getTime();
				request.routing_id			= routing_id;
				request.arguments			= arguments;

				try{
					   // Send data

				    URL url = new URL(url_string);
				    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
				    conn.setConnectTimeout(10000);
				    conn.setDoOutput(true);
				    String encoded_request 	= ObjectToString(request);
				    OutputStreamWriter wr 	= new OutputStreamWriter(conn.getOutputStream());
				    wr.write(encoded_request);
				    wr.flush();
				    wr.close();

				    // Get the response

				    int response_code 	= conn.getResponseCode();

				    switch (response_code)
				    {
			    	case HttpURLConnection.HTTP_OK:

			    		String response 	= convertStreamToString(conn.getInputStream());
			    		JavaGatewayResponse response_obj = (JavaGatewayResponse)ObjectFromString(response);
			    		conn.disconnect();
			    		switch (response_obj.response_code)
					    {
					    	case JavaGatewayResponse.MODULE_METHOD_INVOKE_OK:
					    		callback.exec(JavaGatewayResponse.MODULE_METHOD_INVOKE_OK,routing_id,response_obj.value);
					    		break;
					    	case JavaGatewayResponse.MODULE_METHOD_INVOKE_ERROR:
					    		Exception e = (Exception)response_obj.value;
					    		callback.exec(JavaGatewayResponse.MODULE_METHOD_INVOKE_ERROR,routing_id,e);
					    		break;
					    }
			    	default:
					    conn.disconnect();
			    		Exception we = new WebApplicationException("JavaGateway - REQUEST FAILED "+url_string+" with response code: "+response_code);
			    		callback.exec(JavaGatewayResponse.MODULE_METHOD_SERVER_ERROR,routing_id,we);
				    }


				}catch(Exception e)
				{
					try{
						callback.exec(0,routing_id,e);
					}catch(Exception ee)
					{
						ee.printStackTrace();
					}
				}
			}
		};
		t.start();
	}

    /** Read the object from Base64 string. */
    private static Object ObjectFromString( String s ) throws IOException ,
                                                        ClassNotFoundException {
        return Base64.decodeToObject(s);

    }

    /** Write the object to a Base64 string. */
    private static String ObjectToString( Serializable o ) throws IOException {
        return Base64.encodeObject( o);
    }


	public static String convertStreamToString(InputStream is)
	           throws IOException {
	        /*
	         * To convert the InputStream to String we use the
	         * Reader.read(char[] buffer) method. We iterate until the
	         * Reader return -1 which means there's no more data to
	         * read. We use the StringWriter class to produce the string.
	         */
	        if (is != null) {
	            Writer writer = new StringWriter();

	            char[] buffer = new char[1024];
	            try {
	                Reader reader = //new BufferedReader(
	                        new InputStreamReader(is, "UTF-8");
	                        //);
	                int n;
	                while ((n = reader.read(buffer)) != -1) {
	                    writer.write(buffer, 0, n);
	                }
	            } finally {
	                is.close();
	            }
	            return writer.toString();
	        } else {
	            return "";
	        }
	    }








}


