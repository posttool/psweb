package com.pagesociety.web.amf;

public class AmfLong
{
	private int _i;
	private int _j;

	public AmfLong()
	{
	}
	
	public AmfLong(Long value)
	{
		_i = (int) (value & 0xffffffffL);
		_j = (int) (value >> 32);
	}

	public Long longValue()
	{
		return (((long) _j) << 32) | (((long) _i) & 0xffffffffL);
	}

	public int getLow()
	{
		return _i;
	}

	public void setLow(int i)
	{
		_i = i;
	}

	public int getHigh()
	{
		return _j;
	}

	public void setHigh(int j)
	{
		_j = j;
	}

	public static void main(String[] args)
	{
		AmfLong a = new AmfLong(361873681799100999L);
		System.out.println(a.longValue());
		System.out.println(a.getLow() + " " + a.getHigh());
	}
}
