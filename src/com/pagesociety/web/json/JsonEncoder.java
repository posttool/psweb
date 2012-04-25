package com.pagesociety.web.json;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.pagesociety.persistence.Entity;
import com.pagesociety.web.bean.Bean;
import com.pagesociety.web.bean.BeanRegistry;

public class JsonEncoder
{

	protected static Gson get_gson(boolean pretty_print, boolean escape_entities)
	{
		GsonBuilder builder = new GsonBuilder();
		if (pretty_print)
			builder.setPrettyPrinting();
		if (!escape_entities)
			builder.disableHtmlEscaping();
		return builder.create();
	}
	
	public static String encode(Object o)
	{
		return encode(o,true,true);
	}
	
	public static String encode(Object o, boolean wrapwithvalue, boolean include_oid)
	{
		Gson gson = get_gson(false,true);		
		String json = gson.toJson(encode_(o,wrapwithvalue,include_oid));
		return json;
	}

	public static JsonElement encode_(Object o, boolean wrapwithvalue, boolean include_oid)
	{
		try
		{
			JsonElement jso = (JsonElement)encode_json(o, new HashMap<Object, String>(), include_oid);
			if (wrapwithvalue)
				return wrap("value", jso);
			else
				return jso;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			JsonObject jsoerr = new JsonObject();
			jsoerr.addProperty("error", e.getMessage());
			return jsoerr;// wrap?
		}
	}

	public static JsonObject wrap(String property_name, JsonElement value)
	{
		JsonObject jsowrap = new JsonObject();
		jsowrap.add(property_name, value);
		return jsowrap;
	}

	@SuppressWarnings("unchecked")
	private static JsonElement encode_json(Object o, Map<Object, String> seen, boolean include_oid) throws Exception
	{
		if (o == null)
		{
			return JsonNull.INSTANCE;
		}

		if (seen.containsKey(o))
		{
			String mid = seen.get(o);
			JsonObject jso = new JsonObject();
			if (include_oid)
			{
				jso.addProperty("_object_id", mid);
				jso.addProperty("_circular_ref", true);
			}
			if (o instanceof Entity)
			{
				jso.addProperty("_ps_clazz", "Entity");
				jso.addProperty("type", ((Entity) o).getType());
				jso.addProperty("id", ((Entity) o).getId());
			}
			return jso;
		}

		Bean bean;
		String mem_id = "0x" + Integer.toHexString(System.identityHashCode(o));
		if (o.getClass().isArray())
		{
			Object[] oa = (Object[]) o;
			JsonArray jsa = new JsonArray();
			for (int i = 0; i < oa.length; i++)
			{
				JsonElement e = encode_json(oa[i], seen, include_oid);
				jsa.add(e);
			}
			return jsa;
		}
		else if (o instanceof List)
		{
			List<Object> oa = (List<Object>) o;
			JsonArray jsa = new JsonArray();
			for (int i = 0; i < oa.size(); i++)
			{
				JsonElement e = encode_json(oa.get(i), seen, include_oid);
				jsa.add(e);
			}
			return jsa;
		}
		else if (o instanceof Map)
		{
			seen.put(o, mem_id);
			@SuppressWarnings("rawtypes")
			Map om = (Map) o;
			JsonObject jso = new JsonObject();
			for (Object key : om.keySet())
			{
				Object field_val = om.get(key);
				JsonElement e = encode_json(field_val, seen, include_oid);
				jso.add(key.toString(), e);

			}
			if (include_oid)
			{
				jso.addProperty("_object_id", mem_id);
			}
			return jso;
		}
		bean = BeanRegistry.getBeanByClass(o.getClass());// entities get hit by
		if (bean != null)
		{

			seen.put(o, mem_id);
			JsonObject jso = new JsonObject();
			for (int i = 0; i < bean.getReadablePropertyNames().length; i++)
			{
				String name = bean.getReadablePropertyNames()[i];
				Object field_val = bean.getProperty(o, name);
				JsonElement e = encode_json(field_val, seen, include_oid);
				jso.add(name, e);
			}
			if (include_oid)
			{
				jso.addProperty("_ps_clazz", o.getClass().getSimpleName());
				jso.addProperty("_object_id", mem_id);
			}
			return jso;
		}
		else
		{
			return encode_value(o);
		}
	}

	private static JsonPrimitive encode_value(Object o) throws Exception
	{

		if (o.getClass() == Boolean.class)
		{
			return new JsonPrimitive((Boolean) o);
		}
		else if (o instanceof String)
		{
			return new JsonPrimitive((String) o);
		}
		else if (o instanceof Number)
		{
			return new JsonPrimitive((Number) o);
		}
		else if (o.getClass() == Date.class)
		{
			return new JsonPrimitive(((Date) o).getTime());
		}
		else if (o.getClass() == Integer.class)
		{
			return new JsonPrimitive((String) o);
		}
		throw new Exception("JSONENCODER: UNSUPPORTED PRIMITIVE " + o.getClass());
	}
}