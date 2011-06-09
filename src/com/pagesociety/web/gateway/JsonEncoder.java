package com.pagesociety.web.gateway;


import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONStringer;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.bean.Bean;
import com.pagesociety.web.bean.BeanRegistry;

public class JsonEncoder
{

	public static String encode(Object o,boolean wrapwithvalue)
	{
		JSONStringer js = new JSONStringer();
		try
		{
			if(wrapwithvalue)
			{
				js.object();
				js.key("value");
				encode_json(js, o, new HashMap<Object,String>());
				js.endObject();
			}
			else
			{
				encode_json(js, o, new HashMap<Object,String>());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return js.toString();
	}

	public static String encode(Object o)
	{
		return encode(o,true);
	}

	@SuppressWarnings("unchecked")
	private static void encode_json(JSONStringer js, Object o,Map<Object,String> seen)
			throws Exception
	{
		if (o == null)
		{
			js.value(null);
			return;
		}

		if (seen.containsKey(o))
		{
			String mid = seen.get(o);
			js.object();
			js.key("_object_id");
			js.value(mid);
			js.key("_circular_ref");
			js.value(true);
			if(o instanceof Entity)
			{
				js.key("_ps_clazz");
				js.value("Entity");
				js.key("type");
				js.value(((Entity)o).getType());
				js.key("id");
				js.value(((Entity)o).getId());
			}
			js.endObject();
			return;
		}

		Bean bean;
		String mem_id = "0x"+Integer.toHexString(System.identityHashCode(o));
		if (o.getClass().isArray())
		{
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
			seen.put(o,mem_id);
			Map om = (Map) o;
			js.object();
			for (Object key : om.keySet())
			{
				Object field_val = om.get(key);
				js.key(key.toString());
				encode_json(js, field_val, seen);

			}
			js.key("_object_id");
			js.value(mem_id);
			js.endObject();
			return;
		}
		bean = BeanRegistry.getBeanByClass(o.getClass());//entities get hit by this//
		if (bean != null)
		{

			seen.put(o,mem_id);
			js.object();
			for (int i = 0; i < bean.getReadablePropertyNames().length; i++)
			{
				String name = bean.getReadablePropertyNames()[i];
				Object field_val = bean.getProperty(o, name);
				js.key(name);
				encode_json(js, field_val,seen);
			}
			js.key("_ps_clazz");
			js.value(o.getClass().getSimpleName());

			js.key("_object_id");
			js.value(mem_id);

			js.endObject();
		}
		else
		{
			encode_value(js, o);
		}
	}

	private static void encode_value(JSONStringer js, Object o)
			throws Exception
	{
		if (o.getClass() == Boolean.class)
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

	}
}