package com.pagesociety.web.amf;

import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.w3c.dom.Document;

import com.pagesociety.web.bean.Bean;
import com.pagesociety.web.bean.BeanRegistry;

public class AmfOut
{
	public static final int MAX_INT = Integer.MAX_VALUE >> 3;
	public static final int MIN_INT = Integer.MIN_VALUE >> 3;
	public final ByteBuffer buffer;
	//
	private Map<String, Integer> stringReferences;
	private Map<Object, Integer> objectReferences;
	private Map<String, Integer> classDescriptors;

	public AmfOut(int version, String client_id, String amf_routing, Object return_value)
	{
		stringReferences = new HashMap<String, Integer>();
		objectReferences = new IdentityHashMap<Object, Integer>();
		classDescriptors  = new HashMap<String, Integer>();
		//
		buffer = ByteBuffer.allocate(AmfConstants.DEFAULT_BUFFER_SIZE, false);
		buffer.setAutoExpand(true);
		buffer.putShort((short) version);
		buffer.putShort((short) 0); // headers
		buffer.putShort((short) 1); // msg len
		writeUTF(client_id + amf_routing);
		writeUTF("null"); // spec says must be "null"
		buffer.putInt(-1); // spec says this is content length...?
		buffer.put(AmfType.AMF3_IN_AMF0);
		writeObject(return_value);
		buffer.flip();
	}
	
	public AmfOut(String module_method, Object... args)
	{
		stringReferences = new HashMap<String, Integer>();
		objectReferences = new IdentityHashMap<Object, Integer>();
		classDescriptors  = new HashMap<String, Integer>();
		//
		buffer = ByteBuffer.allocate(AmfConstants.DEFAULT_BUFFER_SIZE, false);
		buffer.setAutoExpand(true);
		buffer.putShort((short) AmfConstants.DEFAULT_VERSION);
		buffer.putShort((short) 0); // headers
		buffer.putShort((short) 1); // msg len
		writeUTF(module_method);
		writeUTF("null"); // spec says must be "null"
		buffer.putInt(-1); // spec says this is content length...?
		buffer.put(AmfType.AMF0_ARRAY);
		buffer.putInt(1);
		buffer.put(AmfType.AMF3_IN_AMF0);
		writeObject(args);
		buffer.flip();

	}

	protected void writeObject(Object value)
	{
		if (value == null)
		{
			buffer.put(AmfType.NULL);
		}
		else if (value instanceof CharSequence || value instanceof Character)
		{
			writeAMFString(value.toString());
		}
		else if (value instanceof Boolean)
		{
			buffer.put((Boolean) value ? AmfType.TRUE : AmfType.FALSE);
		}
		else if (value instanceof Long)
		{
			writeAMFObject(new AmfLong((Long)value));
		}
		else if (value instanceof Number)
		{
			writeAMFIntOrNumber((Number) value);
		}
		else if (value instanceof byte[])
		{
			writeAMFByteArray((byte[]) value);
		}
		else if (value.getClass().isArray())
		{
			writeAMFArray(value);
		}
		else if (value instanceof Date)
		{
			writeAMFDate((Date) value);
		}
		else if (value instanceof Calendar)
		{
			writeAMFDate(((Calendar) value).getTime());
		}
		else if (value instanceof Document)
		{
			// writeAMFXml(value);
			throw new AmfException("Out err UPSUPPORTED TYPE Document");
		}
		else
		{
			writeAMFObject(value);
		}
	}

	
	protected void writeAMFIntOrNumber(Number value)
	{
		if (value instanceof Double || value instanceof Float)
		{
			writeAMFNumber(value.doubleValue());
		}
		else
		{
			long longValue = value.longValue();
			if (longValue < MIN_INT || longValue > MAX_INT)
			{
				writeAMFNumber(longValue);
			}
			else
			{
				writeAMFInt((int) longValue);
			}
		}
	}

	protected void writeAMFInt(int value)
	{
		buffer.put(AmfType.INT);
		writeAMFIntValue(value);
	}

	protected void writeAMFIntValue(int value)
	{
		if (value < MIN_INT || value > MAX_INT)
		{
			throw new IllegalArgumentException("AMF3 integer out of range: " + value);
		}
		if (value < 0 || value >= 0x200000)
		{
			buffer.put((byte) (((value >> 22) & 0x7f) | 0x80));
			buffer.put((byte) (((value >> 15) & 0x7f) | 0x80));
			buffer.put((byte) (((value >> 8) & 0x7f) | 0x80));
			buffer.put((byte) (value & 0xff));
		}
		else
		{
			if (value >= 0x4000)
			{
				buffer.put((byte) (((value >> 14) & 0x7f) | 0x80));
			}
			if (value >= 0x80)
			{
				buffer.put((byte) (((value >> 7) & 0x7f) | 0x80));
			}
			buffer.put((byte) (value & 0x7f));
		}
	}

	protected void writeAMFNumber(double value)
	{
		buffer.put(AmfType.NUMBER);
		writeDouble(value);
	}

	protected void writeAMFString(String value)
	{
		buffer.put(AmfType.STRING);
		writeAMFStringValue(value);
	}

	protected void writeAMFStringValue(String value)
	{
		if (value.length() == 0)
		{
			buffer.put((byte) 1);
			return;
		}
		if (!writeStringReference(value))
		{
			addStringReference(value);
			writeAMFLengthAndString(value);
		}
	}

	protected void writeAMFLengthAndString(String value)
	{
		java.nio.ByteBuffer nioBuffer = AmfConstants.UTF8_CHARSET.encode(value);
		int length = nioBuffer.limit();
		writeAMFIntValue((length << 1) | 1);
		buffer.put(nioBuffer);
	}

	protected void writeAMFXml(Object value)
	{
		throw new RuntimeException("UNSUPPORTED XML");
		// buffer.put(AMFType.XML);
		// String xmlString = config.getXmlAdapter().toXmlString(value);
		// writeAMFLengthAndString(xmlString);
	}

	protected void writeAMFDate(Date value)
	{
		buffer.put(AmfType.DATE);
		if (!writeObjectReference(value))
		{
			addObjectReference(value);
			writeAMFIntValue(1);
			writeDouble(value.getTime());
		}
	}

	protected void writeAMFArray(Object value)
	{
		buffer.put(AmfType.ARRAY);
		if (!writeObjectReference(value))
		{
			addObjectReference(value);
			writeAMFIntValue(getLength(value) << 1 | 1);
			buffer.put((byte) 1); // empty string
			if (value instanceof Collection)
			{
				Iterator<?> iterator = ((Collection<?>) value).iterator();
				while (iterator.hasNext())
				{
					writeObject(iterator.next());
				}
			}
			else
			{
				Object[] a = (Object[]) value;
				for (int i = 0; i < a.length; i++)
				{
					writeObject(a[i]);
				}
			}
		}
	}

	private int getLength(Object value)
	{
		return (value instanceof Collection) ? ((Collection<?>) value).size() : Array.getLength(value);
	}

	protected void writeAMFByteArray(byte[] value)
	{
		buffer.put(AmfType.BYTE_ARRAY);
		if (!writeObjectReference(value))
		{
			addObjectReference(value);
			writeAMFIntValue(value.length << 1 | 1);
			buffer.put(value);
		}
	}

	protected void writeAMFObject(Object object)
	{
		if (object instanceof Collection || object.getClass().isArray())
		{
			writeAMFArray(object);
			return;
		}
		AmfClassOut mapping = null;
		if (object instanceof Map)
		{
			mapping = new AmfClassOut();
		}
		else
		{
			Bean bean = BeanRegistry.getBeanByClass(object.getClass());
			if (bean != null)
				mapping = new AmfClassOut(bean);
		}
		if (mapping == null)
		{
			String message = "No ClassMapping registered for Java class " + object.getClass().getName();
			System.err.println("AmfOut ERROR :"+message);
			return;
		}
		buffer.put(AmfType.OBJECT);
		if (!writeObjectReference(object))
		{
			addObjectReference(object);
			Integer descIndex = getClassDescriptorIndex(mapping);
			if (descIndex != null)
			{
				writeAMFIntValue(descIndex << 2 | 1);
			}
			else
			{
				addClassDescriptor(mapping);
				mapping.writeClassDescriptor(object, this);
			}
			mapping.writeObject(object, this);
		}
	}

	protected void addStringReference(String str)
	{
		stringReferences.put(str, stringReferences.size());
	}

	protected boolean writeStringReference(String value)
	{
		Integer index = stringReferences.get(value);
		if (index != null)
		{
			writeAMFIntValue(index << 1);
			return true;
		}
		return false;
	}

	protected void addObjectReference(Object value)
	{
		objectReferences.put(value, objectReferences.size());
	}

	protected boolean writeObjectReference(Object value)
	{
		Integer index = objectReferences.get(value);
		if (index != null)
		{
			writeAMFIntValue(index << 1);
			return true;
		}
		return false;
	}

	protected void addClassDescriptor(AmfClassOut mapping)
	{
		classDescriptors.put(mapping.getAlias(), classDescriptors.size());
	}

	protected Integer getClassDescriptorIndex(AmfClassOut mapping)
	{
		return classDescriptors.get(mapping.getAlias());
	}

	protected void writeDouble(double value)
	{
		buffer.putDouble(value);
	}

	protected void writeUTF(String value)
	{
		buffer.putShort((short) value.length());
		writeString(value, AmfConstants.UTF8_CHARSET);
	}

	protected void writeString(String value, Charset charset)
	{
		buffer.put(charset.encode(value));
	}
}
