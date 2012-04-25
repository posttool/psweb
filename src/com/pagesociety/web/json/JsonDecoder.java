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

	@SuppressWarnings("rawtypes")
	public static List decodeAsList(String json)
	{
		Gson gson = new Gson();
		List v = gson.fromJson(json, List.class);
		for (int i=0; i<v.size(); i++)
		{
			if (v.get(i) instanceof Number)
			{
				Number n = (Number)v.get(i);
				if (n.doubleValue() == n.intValue())
					v.set(i, n.intValue());
			}
		}
		return v;
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
				return p.getAsNumber();
			else if (p.isString())
				return p.getAsString();
			else
				return "?????";
		}
		else if (element.isJsonArray())
		{					
			ARRAY oo = new ARRAY();
			JsonArray o = (JsonArray) element;
			for (int i=0; i<o.size(); i++)
			{
				oo.add(process(o.get(i)));
			}
			return oo;
		}
		else if (element.isJsonObject())
		{					
			OBJECT oo = new OBJECT();
			JsonObject o = (JsonObject) element;
			for (Entry<String,JsonElement> e: o.entrySet())
			{
				oo.put(e.getKey(), process(e.getValue()));
			}
			return oo;
		}
		return null;
	}

}
