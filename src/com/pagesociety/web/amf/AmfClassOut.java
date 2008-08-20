package com.pagesociety.web.amf;

import java.util.Map;

import com.pagesociety.web.bean.Bean;

public class AmfClassOut
{
	protected String alias;
	protected String actionScriptClass;
	protected Bean bean;
	protected ObjectEncoding encoding;

	protected AmfClassOut()
	{
		this("", "Object", null, ObjectEncoding.DYNAMIC);
	}

	protected AmfClassOut(Bean bean)
	{
		this(bean.getJavaClass().getCanonicalName(), bean.getJavaClass().getCanonicalName(), bean, ObjectEncoding.SEALED);
	}

	protected AmfClassOut(String alias, String actionScriptClass, Bean bean,
			ObjectEncoding encoding)
	{
		this.alias = alias;
		this.actionScriptClass = actionScriptClass;
		this.bean = bean;
		this.encoding = encoding;
	}

	public String getAlias()
	{
		return alias;
	}

	public String getActionScriptClass()
	{
		return actionScriptClass;
	}

	public ObjectEncoding getObjectEncoding()
	{
		return encoding;
	}

	@SuppressWarnings("unchecked")
	public void writeClassDescriptor(Object instance, AmfOut output)
	{
		switch (encoding)
		{
		case SEALED:
			String[] propertyNames = bean.getReadablePropertyNames();
			output.writeAMFIntValue(propertyNames.length << 4 | encoding.getCode() << 2 | 0x03);
			output.writeAMFStringValue(alias);
			for (int i = 0; i < propertyNames.length; i++)
			{
				output.writeAMFStringValue(propertyNames[i]);
			}
			break;
		case EXTERNALIZABLE:
			// TODO
			break;
		case DYNAMIC:
			output.writeAMFIntValue(encoding.getCode() << 2 | 0x03);
			output.writeAMFStringValue("");
			break;
		}
	}

	@SuppressWarnings("unchecked")
	public void writeObject(Object instance, AmfOut output)
	{
		// dynamic properties
		switch (encoding)
		{
		case SEALED:
			String[] propertyNames = bean.getReadablePropertyNames();
			for (int i = 0; i < propertyNames.length; i++)
			{
				Object o = bean.getProperty(instance, propertyNames[i]);
				output.writeObject(o);
			}
			break;
		case EXTERNALIZABLE:
			// TODO
			break;
		case DYNAMIC:
			Map<String, Object> map = (Map<String, Object>) instance;
			for (Map.Entry<String, Object> entry : map.entrySet())
			{
				String key = entry.getKey();
				if (key != null && key.length() > 0)
				{
					output.writeAMFStringValue(key);
					output.writeObject(entry.getValue());
				}
			}
			output.writeAMFStringValue("");
			break;
		default:
			break;
		}
	}
}
