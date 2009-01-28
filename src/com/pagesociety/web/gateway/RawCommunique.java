package com.pagesociety.web.gateway;

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
	

}
