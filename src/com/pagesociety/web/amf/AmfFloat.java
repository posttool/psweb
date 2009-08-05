package com.pagesociety.web.amf;

public class AmfFloat
{

	private String _v;

	public AmfFloat()
	{
	}
	
	public AmfFloat(Float v)
	{
		_v = Float.toString(v);
	}

	public Float floatValue()
	{
		try {
		return Float.parseFloat(_v);
		} catch (Exception e)
		{
			return -1F;
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
