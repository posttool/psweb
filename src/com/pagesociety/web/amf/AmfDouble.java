package com.pagesociety.web.amf;

public class AmfDouble
{
	private String _v;

	public AmfDouble()
	{
	}
	
	public AmfDouble(Double v)
	{
		_v = Double.toString(v);
	}

	public Double doubleValue()
	{
		try {
		return Double.parseDouble(_v);
		} catch (Exception e)
		{
			return -1D;
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
