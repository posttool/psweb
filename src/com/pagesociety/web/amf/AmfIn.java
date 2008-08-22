package com.pagesociety.web.amf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.mina.common.ByteBuffer;

import com.pagesociety.web.bean.Bean;
import com.pagesociety.web.bean.BeanRegistry;

public class AmfIn
{
	public ByteBuffer buffer;
	// references
	private List<String> stringReferences;
	private List<Object> objectReferences;
	private List<AmfClassIn> classDescriptors;
	// the standard amf 3 preamble
	public int version;
	public int bodyCount;
	public int headerCount;
	public String[] module_method;
	public byte should_be_amf3_in_0_type;
	public int should_be_array_length_of_1;
	public byte should_be_array_type;
	public int msg_length;
	public String client_id;
	// the list of passed arguments
	public List<Object> args;
	private Object return_value;

	@SuppressWarnings("unchecked")
	public AmfIn(HttpServletRequest request) throws IOException
	{
		stringReferences = new ArrayList<String>();
		objectReferences = new ArrayList<Object>();
		classDescriptors = new ArrayList<AmfClassIn>();
		//
		int expectedLength = request.getContentLength();
		InputStream input = request.getInputStream();
		buffer = ByteBuffer.allocate(expectedLength + 1);
		ReadableByteChannel channel = Channels.newChannel(input);
		int bytesRead = 0;
		while (bytesRead != -1)
		{
			if (buffer.remaining() == 0)
			{
				// won't happen if expectedLength is accurate
				buffer.expand(expectedLength);
			}
			bytesRead = channel.read(buffer.buf());
		}
		buffer.flip();
		// read stuff from buffer
		version = buffer.getUnsignedShort();
		headerCount = buffer.getUnsignedShort();
		bodyCount = buffer.getUnsignedShort();
		module_method = readUTF().split("/");
		client_id = readUTF();
		msg_length = buffer.getInt();
		should_be_array_type = buffer.get(); // AMFType.AMF0_ARRAY
		should_be_array_length_of_1 = buffer.getInt();
		should_be_amf3_in_0_type = buffer.get(); // AMFType.AMF3_IN_AMF0
		args = (List<Object>) readObject();
	}

	@SuppressWarnings("unchecked")
	public AmfIn(int expectedLength, InputStream input) throws IOException
	{
		stringReferences = new ArrayList<String>();
		objectReferences = new ArrayList<Object>();
		classDescriptors = new ArrayList<AmfClassIn>();
		//
		buffer = ByteBuffer.allocate(expectedLength + 1);
		ReadableByteChannel channel = Channels.newChannel(input);
		int bytesRead = 0;
		while (bytesRead != -1)
		{
			if (buffer.remaining() == 0)
			{
				// won't happen if expectedLength is accurate
				buffer.expand(expectedLength);
			}
			bytesRead = channel.read(buffer.buf());
		}
		buffer.flip();
		// read stuff from buffer
		version = buffer.getUnsignedShort();
		headerCount = buffer.getUnsignedShort();
		bodyCount = buffer.getUnsignedShort();
		client_id = readUTF().split("/")[0];
		readUTF();
		msg_length = buffer.getInt(); //-1
		should_be_amf3_in_0_type = buffer.get(); // AMFType.AMF3_IN_AMF0
		return_value = readObject();
	}

	

	

	public List<Object> getArguments()
	{
		return args;
	}
	
	public Object getReturn()
	{
		return return_value;
	}

	protected int readAMFInt()
	{
		int result = 0;
		int cnt = 0;
		int b = buffer.get();
		while ((b & 0x80) != 0 && cnt < 3)
		{
			result <<= 7;
			result |= (b & 0x7f);
			b = buffer.get();
			cnt++;
		}
		if (cnt < 3)
		{
			result <<= 7;
			result |= b;
		}
		else
		{
			result <<= 8;
			result |= (b & 0xff);
			if ((result & 0x10000000) != 0)
				result |= 0xe0000000;
		}
		return result;
	}

	protected String readAMFString()
	{
		int type = readAMFInt();
		if (type == 1)
		{
			return "";
		}
		if ((type & 1) == 0)
		{
			String result = getStringReference(type >> 1);
			return result;
		}
		String result = readString(AmfConstants.UTF8_CHARSET, type >> 1);
		addStringReference(result);
		return result;
	}

	protected String readString(Charset charset, int length)
	{
		int limit = buffer.limit();
		/*
		 * final java.nio.ByteBuffer nioBuffer = buffer.buf();
		 * nioBuffer.limit(nioBuffer.position() + length); String result =
		 * charset.decode(nioBuffer).toString();
		 */
		buffer.limit(buffer.position() + length);
		String result = charset.decode(buffer.buf()).toString();
		buffer.limit(limit);
		return result;
	}

	protected Date readAMFDate()
	{
		int type = readAMFInt();
		if ((type & 1) == 0)
		{
			return (Date) getObjectReference(type >> 1);
		}
		else
		{
			Date result = new Date((long) buffer.getDouble());
			addObjectReference(result);
			return result;
		}
	}

	@SuppressWarnings("unchecked")
	protected List<Object> readAMFArray()
	{
		int type = readAMFInt();
		if ((type & 1) == 0)
		{
			return (List<Object>) getObjectReference(type >> 1);
		}
		else
		{
			int size = type >> 1;
			List<Object> array = new ArrayList<Object>(size);
			addObjectReference(array);
			String key = readAMFString();
			if (key.length() > 0)
			{
				throw new AmfException("Mixed Arrays not supported for sake of consistency. Please use Object or Dictionary instead.");
			}
			for (int i = 0; i < size; i++)
			{
				array.add(readObject());
			}
			return array;
		}
	}

	@SuppressWarnings("unchecked")
	protected Object readAMFObject()
	{
		int type = readAMFInt();
		if ((type & 1) == 0)
		{
			return getObjectReference(type >> 1);
		}
		else
		{
			Object result;
			AmfClassIn class_in = null;
			if (((type >> 1) & 1) == 0)
			{
				class_in = getClassDescriptor(type >> 2);
			}
			else
			{
				int propertyCount = type >> 4;
				byte encoding = (byte) ((type >> 2) & 0x03);
				ObjectEncoding oe = ObjectEncoding.fromByteCode(encoding);
				String alias = readAMFString();
				switch (oe)
				{
				case SEALED:
					Bean bean = BeanRegistry.getBeanByName(alias);
					if (bean == null)
					{
//						try
//						{
//							Class<?> cfn = Class.forName(alias);
//							BeanRegistry.register(cfn);
//							bean = BeanRegistry.getBeanByName(alias);
//						}
//						catch (ClassNotFoundException e)
//						{
							String message = "No ClassMapping registered for alias " + alias;
							throw new AmfException(message);
//						}
					}
					String[] propertyNames = new String[propertyCount];
					for (int i = 0; i < propertyCount; i++)
					{
						propertyNames[i] = readAMFString();
					}
					class_in = new AmfClassIn(oe, propertyNames, bean);
					break;
				case EXTERNALIZABLE:
					result = null;
					break;
				case DYNAMIC:
					class_in = new AmfClassIn();
					break;
				default:
					result = null;
					break;
				}
				addClassDescriptor(class_in);
			}
			// first create the reference
			result = class_in.createObject();
			// add it to the reference pool before deserializing
			addObjectReference(result);
			// then populate it
			class_in.readObject(this, result);
			// the recursive nature of read object
			// will cause the children to be read.
			// adding the reference before children
			// ensures correct index.
			return result;
		}
	}

	protected Object readObject()
	{
		int type = buffer.get();
		switch (type)
		{
		case AmfType.UNDEFINED:
		case AmfType.NULL:
			return null;
		case AmfType.FALSE:
			return Boolean.FALSE;
		case AmfType.TRUE:
			return Boolean.TRUE;
		case AmfType.INT:
			return readAMFInt();
		case AmfType.NUMBER:
			return buffer.getDouble();
		case AmfType.STRING:
			return readAMFString();
		case AmfType.XML_LEGACY:
			return readAMFXml();
		case AmfType.DATE:
			return readAMFDate();
		case AmfType.ARRAY:
			return readAMFArray();
		case AmfType.OBJECT:
			return readAMFObject();
		case AmfType.XML:
			return readAMFXml();
		case AmfType.BYTE_ARRAY:
			return readAMFByteArray();
		default:
			return null;
			// throw new IllegalArgumentException("Unknown type: " + type);
		}
	}

	protected Object readAMFXml()
	{
		throw new AmfException("AMFIn XML unsupported!");
		// int type = readAMFInt();
		// if ((type & 1) == 0)
		// {
		// return getObjectReference(type >> 1);
		// }
		// else
		// {
		// String xml = readString(IOUtil.UTF8_CHARSET, type >> 1);
		// if (xml.length() == 0)
		// {
		// return null;
		// }
		// Object doc = config.getXmlAdapter().buildDocument(xml);
		// addObjectReference(doc);
		// return doc;
		// }
	}

	protected byte[] readAMFByteArray()
	{
		int type = readAMFInt();
		if ((type & 1) == 0)
		{
			return (byte[]) getObjectReference(type >> 1);
		}
		else
		{
			byte[] array = new byte[type >> 1];
			buffer.get(array);
			return array;
		}
	}

	protected void addStringReference(String str)
	{
		stringReferences.add(str);
	}

	protected String getStringReference(int index)
	{
		String ref = stringReferences.get(index);
		if (ref == null)
		{
			throw new AmfException("Illegal string reference index: " + index);
		}
		return ref;
	}

	protected int addObjectReference(Object obj)
	{
		objectReferences.add(obj);
		return objectReferences.size() - 1;
	}

	protected Object getObjectReference(int index)
	{
		Object ref = objectReferences.get(index);
		if (ref == null)
		{
			throw new AmfException("Illegal object reference index: " + index);
		}
		return ref;
	}

	protected void addClassDescriptor(AmfClassIn desc)
	{
		classDescriptors.add(desc);
	}

	protected AmfClassIn getClassDescriptor(int index)
	{
		AmfClassIn desc = classDescriptors.get(index);
		if (desc == null)
		{
			throw new AmfException("Illegal class descriptor index: " + index);
		}
		return desc;
	}

	protected String readUTF()
	{
		int length = buffer.getUnsignedShort();
		return readString(AmfConstants.UTF8_CHARSET, length);
	}
}
