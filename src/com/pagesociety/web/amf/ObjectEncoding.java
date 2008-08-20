package com.pagesociety.web.amf;

public enum ObjectEncoding
{
	SEALED((byte) 0x00), EXTERNALIZABLE((byte) 0x01), DYNAMIC((byte) 0x02);
	private byte code;

	ObjectEncoding(byte code)
	{
		this.code = code;
	}

	public byte getCode()
	{
		return code;
	}

	public static ObjectEncoding fromByteCode(byte code)
	{
		for (ObjectEncoding encoding : values())
		{
			if (encoding.getCode() == code)
			{
				return encoding;
			}
		}
		throw new IllegalArgumentException("Illegal code for ObjectEncoding: " + code);
	}
}
