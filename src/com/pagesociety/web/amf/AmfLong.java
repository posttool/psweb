package com.pagesociety.web.amf;

public class AmfLong
{

	private String _v;

	public AmfLong()
	{
	}
	
	public AmfLong(Long v)
	{
		_v = Long.toString(v);
	}

	public Long longValue()
	{
		try {
		return Long.parseLong(_v);
		} catch (Exception e)
		{
			return -1L;
		}
	}

	public String getStringValue()
	{
		return _v;
	}
	
	public void setStringValue(String v)
	{
		_v = v;
	}
	

}
