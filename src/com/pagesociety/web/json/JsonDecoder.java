package com.pagesociety.web.json;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonStreamParser;
import com.pagesociety.util.ARRAY;
import com.pagesociety.util.OBJECT;

public class JsonDecoder
{
	@SuppressWarnings("rawtypes")
	public static Map decodeAsMap(String json)
	{
		Gson gson = new Gson();
		return gson.fromJson(json, Map.class);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List decodeAsList(String json)
	{
		Gson gson = new Gson();
		List v = gson.fromJson(json, List.class);
		for (int i = 0; i < v.size(); i++)
		{
			Object o = v.get(i);
			if (o instanceof Number)
			{
				try
				{
					Number n = (Number) o;
					if (is_int(n))
						v.set(i, n.intValue());
				}
				catch (Exception e)
				{
					System.out.println("JSON DECODER CANT DECODE NUMBER " + e.getMessage());
					v.set(i, 0);
				}
			}
			else if (o instanceof Map)
				v.set(i, getOBJECT((Map) o));
			else if (o instanceof List)
				v.set(i, getARRAY((List) o));
		}
		return v;
	}

	public static boolean is_int(Number n)
	{
		return n.doubleValue() == n.intValue();
	}

	@SuppressWarnings({ "rawtypes" })
	public static OBJECT getOBJECT(Map m)
	{
		OBJECT O = new OBJECT();
		for (Object k : m.keySet())
		{
			String key = k.toString();
			Object v = m.get(key);
			if (v instanceof List)
				O.put(key, getARRAY((List) v));
			else if (v instanceof Map)
				O.put(key, getOBJECT((Map) v));
			else
				O.put(key, v);
		}
		return O;
	}

	@SuppressWarnings({ "rawtypes" })
	public static ARRAY getARRAY(List r)
	{
		ARRAY R = new ARRAY();
		int s = r.size();
		for (int i = 0; i < s; i++)
		{
			Object v = r.get(i);
			if (v instanceof List)
				R.set(i, getARRAY((List) v));
			else if (v instanceof Map)
				R.set(i, getOBJECT((Map) v));
			else
				R.set(i, v);
		}
		return R;
	}

	public static OBJECT decodeAsOBJECT(String json)
	{
		JsonStreamParser parser = new JsonStreamParser(json);
		JsonElement element;
		synchronized (parser)
		{
			if (parser.hasNext())
			{
				element = parser.next();
				if (element.isJsonObject())
					return (OBJECT) process(element);
			}
		}
		return new OBJECT();
	}

	public static Object process(JsonElement element)
	{
		if (element.isJsonNull())
		{
			return null;
		}
		else if (element.isJsonPrimitive())
		{
			JsonPrimitive p = (JsonPrimitive) element;
			if (p.isBoolean())
				return p.getAsBoolean();
			else if (p.isNumber())
			{
				try
				{
					Number n = p.getAsNumber();
					if (is_int(n))
						return n.intValue();
					else
						return n;
				}
				catch (Exception e)
				{
					System.out.println("JSON DECODER CANT DECODE NUMBER " + e.getMessage());
					return 0;
				}
			}
			else if (p.isString())
				return p.getAsString();
			else
				return "?????";
		}
		else if (element.isJsonArray())
		{
			ARRAY oo = new ARRAY();
			JsonArray o = (JsonArray) element;
			for (int i = 0; i < o.size(); i++)
			{
				oo.add(process(o.get(i)));
			}
			return oo;
		}
		else if (element.isJsonObject())
		{
			OBJECT oo = new OBJECT();
			JsonObject o = (JsonObject) element;
			for (Entry<String, JsonElement> e : o.entrySet())
			{
				oo.put(e.getKey(), process(e.getValue()));
			}
			return oo;
		}
		return null;
	}

}
