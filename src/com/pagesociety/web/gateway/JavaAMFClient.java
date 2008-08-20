package com.pagesociety.web.gateway;

import java.io.ByteArrayInputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;

import com.pagesociety.web.amf.AmfIn;
import com.pagesociety.web.amf.AmfOut;

public class JavaAMFClient
{
	private String _gateway;

	public JavaAMFClient(String gateway)
	{
		_gateway = gateway;
	}

	public Object execute(String module_method, Object... args)
	{
		HttpClient client = new HttpClient();
		PostMethod httppost = new PostMethod(_gateway);
		AmfOut amf_out = new AmfOut(module_method, args);
		int responseLength = amf_out.buffer.remaining();
		httppost.setRequestEntity(new InputStreamRequestEntity(new ByteArrayInputStream(amf_out.buffer.array(), amf_out.buffer.position(), responseLength)));
		try
		{
			client.executeMethod(httppost);
			if (httppost.getStatusCode() == HttpStatus.SC_OK)
			{
				AmfIn amf_in = new AmfIn((int) httppost.getResponseContentLength(), httppost.getResponseBodyAsStream());
				return amf_in.getReturn();
			}
			else
			{
				System.out.println("Unexpected failure: " + httppost.getStatusLine().toString());
				return null;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		finally
		{
			httppost.releaseConnection();
		}
	}
}