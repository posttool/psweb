package com.pagesociety.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.gateway.JsonEncoder;

public class OBJECT extends LinkedHashMap<String,Object>
{

	private static final long serialVersionUID = 6574754645564381545L;

	public OBJECT(Object... args)
	{
		for (int i = 0; i < args.length; i += 2)
			put((String) args[i], args[i + 1]);
	}

	public String toString()
	{
		StringBuilder b = new StringBuilder();
		b.append("{\n");
		for (String k : keySet())
		{
			b.append(k);
			b.append("=");
			b.append(get(k));
			b.append("\n");
		}
		b.append("}\n");
		return b.toString();
	}

	public String toJSON()
	{
		return toJSON(this);
	}
	
	public boolean hasValue(String path)
	{
		Object o = null;
		try { o = find(path); }
		catch (NullPointerException e) { return false; }
		return o != null;
	}

	public OBJECT O(String path)
	{
		return (OBJECT) find(path);
	}

	public ARRAY A(String path)
	{
		return (ARRAY) find(path);
	}

	public String S(String path)
	{
		return (String) find(path);
	}

	public boolean B(String path)
	{
		return (Boolean) find(path);
	}

	public int I(String path)
	{
		Object val =  find(path);
		if(val instanceof String)
			return Integer.parseInt((String)val);
		else
			return (Integer)val;
	}
	
	public Double F(String path)
	{
		Object val =  find(path);
		if(val instanceof String)
			return Double.parseDouble((String)val);
		else
			return (Double)val;
	}

	public double N(String path)
	{
		return (Double) find(path);
	}

	public Date D(String path)
	{
		return (Date) find(path);
	}

	public Object find(String path)
	{
		return find(this, path);
	}

	public static Object find(OBJECT o, String path)
	{
		Object target = o;
		String[] p = path.split("\\.");
		for (String pp : p)
		{
			if (target==null)
				throw new NullPointerException("cannot access item "+pp);
			int array_char_idx = pp.indexOf("[");
			if (array_char_idx != -1 && pp.endsWith("]"))
			{

				int idx = Integer.parseInt(pp.substring(array_char_idx + 1, pp.length() - 1));
				pp = pp.substring(0, array_char_idx);
				List<?> ta = ((OBJECT) target).A(pp);
				if (ta==null)
					throw new NullPointerException("cannot access item "+idx+" of "+pp);
				target = ta.get(idx);
			}
			else
			{
				target = ((OBJECT) target).get(pp);
			}
		}
		return target;
	}

	public static String encode(Serializable o) throws WebApplicationException
	{
		if(o == null)
			return null;
		try
		{
			return Base64.encodeObject(o);
		} catch (IOException e)
		{
			throw new WebApplicationException("Can't encode", e);
		}
	}

	public static OBJECT decode(String s) throws WebApplicationException
	{
		if (s == null)
			return null;
		try
		{
			return (OBJECT) Base64.decodeToObject(s);
		} catch (Exception e)
		{
			throw new WebApplicationException("Can't decode", e);
		}
	}

	public static String toJSON(OBJECT o)
	{
		return JsonEncoder.encode(o,false,false);
	}

	public static void main(String[] args)
	{
		OBJECT o = new OBJECT(
				"x",new ARRAY(1,2,"you will"),
				"y", new OBJECT(
					"a$a", 17,
					"b$b", new OBJECT(
						"start", 1010101,
						"end", "sometimes"
					),
					"ccc", new ARRAY("wonder","get me", new OBJECT("or","stop","and",true)
					)
				));

		System.out.println(o.toJSON());
		System.out.println("---");
		System.out.println(o.find("x"));
		System.out.println(o.find("x[2]"));
		System.out.println(o.find("y.b$b.end"));
		System.out.println(o.S("y.ccc[2].or"));
		System.out.println(o.B("y.ccc[2].and"));
//		System.out.println(o.I("y.ccc[2].and")); //will produce class cast exception
//		System.out.println(o.B("y.ccd[2].x"));  // null pointer
//		System.out.println(o.B("y.ccc[14].x")); // index out of bounds
		System.out.println(o.find("y.ccc[2].da"));

	}

}