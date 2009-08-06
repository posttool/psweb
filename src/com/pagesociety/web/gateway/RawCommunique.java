package com.pagesociety.web.gateway;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RawCommunique 
{
	private Object _request;
	private Object _response;
	
	public RawCommunique(Object request,Object response)
	{
		_request = request;
		_response = response;
	}
	
	public Object getRequest()
	{
		return _request;
	}
	
	public Object getResponse()
	{
		return _response;
	}
	
	public Map<String,String> getParams()
	{
		Map<String,String> params = new HashMap<String,String>();
		Enumeration<String> e = ((HttpServletRequest)_request).getParameterNames();
		while(e.hasMoreElements())
		{
			String pname = (String)e.nextElement();
			String val 	 = ((HttpServletRequest)_request).getParameter(pname);
			params.put(pname, val);
		}
		return params;
	}
	
	public void println(String s) throws IOException
	{
			((HttpServletResponse)_response).getWriter().println(s);
	
	}
	

}
