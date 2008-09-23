package com.pagesociety.web.gateway;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONStringer;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.bean.Bean;
import com.pagesociety.web.bean.BeanRegistry;

public class JsonEncoder
{
	

	public static Object[] decode(String s)
	{
		
		try
		{
			JSONArray a = new JSONArray(s);
			int len = a.length();
			Object[] args = new Object[len];
			for (int i = 0; i < len; i++)
			{
				args[i] = a.get(i);
			}
			return args;
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public static String encode(Object o)
	{
		if (o == null)
			return "NULL";
		JSONStringer js = new JSONStringer();
		try
		{
			boolean is_list = (o.getClass().isArray()) || (o instanceof List);
			boolean is_object = (o instanceof Map) || BeanRegistry.getBeanByClass(o.getClass()) != null;
			if (!is_list && !is_object)
				js.array();
			encode_json(js, o, new ArrayList<Object>());
			if (!is_list && !is_object)
				js.endArray();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return js.toString();
	}

	@SuppressWarnings("unchecked")
	private static void encode_json(JSONStringer js, Object o, ArrayList<Object> seen)
			throws Exception
	{
		if (o == null)
			return;
		// TODO
		// this could be optimized.
		// how do we hash these things when they have circular reference?
		int s = seen.size();//seen should probably be a map but then we need to make
							//sure hashcode() works right for entity which it doesnt
							//right now
		Bean bean;
		for (int i = 0; i < s; i++)
		{
			if (o == seen.get(i))
			{
				
				js.object();
				if (o instanceof Entity)
				{
					js.key("id");
					js.value(((Entity) o).getId());
					js.key("type");
					js.value(((Entity) o).getType());
				}
				else if((bean = BeanRegistry.getBeanByClass(o.getClass())) != null)
				{
					for (int ii = 0; ii < bean.getReadablePropertyNames().length; ii++)
					{
						String name 	 = bean.getReadablePropertyNames()[ii];
						Object field_val = bean.getProperty(o, name);
						js.key(name);
						encode_value(js, field_val, seen);
					}
				}
				js.endObject();
				return;
			}
		}
		if (o.getClass().isArray())
		{
			seen.add(o);
			Object[] oa = (Object[]) o;
			js.array();
			for (int i = 0; i < oa.length; i++)
			{
				encode_json(js, oa[i], seen);
			}
			js.endArray();
			return;
		}
		else if (o instanceof List)
		{
			seen.add(o);
			List<Object> oa = (List<Object>) o;
			js.array();
			for (int i = 0; i < oa.size(); i++)
			{
				encode_json(js, oa.get(i), seen);
			}
			js.endArray();
			return;
		}
		else if (o instanceof Map)
		{
			seen.add(o);
			Map om = (Map) o;
			js.object();
			for (Object key : om.keySet())
			{
				Object field_val = om.get(key);
				js.key(key.toString());
				encode_value(js, field_val, seen);
			}
			js.endObject();
			return;
		}
		bean = BeanRegistry.getBeanByClass(o.getClass());
		if (bean != null)
		{
			seen.add(o);
			js.object();
			for (int i = 0; i < bean.getReadablePropertyNames().length; i++)
			{
				String name = bean.getReadablePropertyNames()[i];
				Object field_val = bean.getProperty(o, name);
				js.key(name);
				encode_value(js, field_val, seen);
			}
			js.endObject();
		}
		else
		{
			encode_value(js, o, seen);
		}
	}

	private static void encode_value(JSONStringer js, Object o, ArrayList<Object> seen)
			throws Exception
	{
		if (o == null)
		{
			js.value(null);
		}
		else if (o.getClass() == Boolean.class)
		{
			js.value((Boolean) o);
		}
		else if (o.getClass() == Double.class)
		{
			js.value((Double) o);
		}
		else if (o.getClass() == Long.class)
		{
			js.value((Long) o);
		}
		else if (o.getClass() == Date.class)
		{
			js.value(((Date) o).getTime());
		}
		else if (o.getClass() == Integer.class)
		{
			js.value((Integer) o);
		}
		else if (o.getClass() == String.class)
		{
			js.value((String) o);
		}
		else
		{
			encode_json(js, o, seen);
		}
	}
}