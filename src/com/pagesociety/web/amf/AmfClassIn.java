package com.pagesociety.web.amf;

import java.util.HashMap;
import java.util.Map;

import com.pagesociety.web.bean.Bean;

public class AmfClassIn
{
	ObjectEncoding encoding;
	Bean bean;
	String[] propertyNames;

	public AmfClassIn()
	{
		this.encoding = ObjectEncoding.DYNAMIC;
	}

	public AmfClassIn(ObjectEncoding encoding, String[] prop_names, Bean bean)
	{
		this.encoding = encoding;
		this.bean = bean;
		this.propertyNames = prop_names;
	}

	public Object createObject()
	{
		switch (encoding)
		{
		case SEALED:
			Object instance = bean.newJavaInstance();
			return instance;
		case EXTERNALIZABLE:
			// TODO
			break;
		case DYNAMIC:
			Map<String, Object> map = new HashMap<String, Object>();
			return map;
		default:
			break;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public void readObject(AmfIn input, Object instance)
	{
		switch (encoding)
		{
		case SEALED:
			for (String propertyName : propertyNames)
			{
				Object value = input.readObject();
				bean.setProperty(instance, propertyName, value);
			}
		case EXTERNALIZABLE:
			// TODO
			break;
		case DYNAMIC:
			Map<String, Object> map = (Map<String, Object>) instance;
			while (true)
			{
				String propertyName = input.readAMFString();
				if (propertyName.length() == 0)
				{
					break;
				}
				Object value = input.readObject();
				map.put(propertyName, value);
			}
		default:
			break;
		}
	}
}
