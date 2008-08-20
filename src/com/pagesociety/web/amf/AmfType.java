package com.pagesociety.web.amf;

public interface AmfType
{
	public static final byte UNDEFINED = 0x00;
	public static final byte NULL = 0x01;
	public static final byte FALSE = 0x02;
	public static final byte TRUE = 0x03;
	public static final byte INT = 0x04;
	public static final byte NUMBER = 0x05;
	public static final byte STRING = 0x06;
	public static final byte XML_LEGACY = 0x07;
	public static final byte DATE = 0x08;
	public static final byte ARRAY = 0x09;
	public static final byte OBJECT = 0x0a;
	public static final byte XML = 0x0b;
	public static final byte BYTE_ARRAY = 0x0c;
	/**
	 * Constant for the AMF0 Array type code. The AMF request sent by the Flash
	 * Player always contains an AMF0 Array with the parameters sent by the
	 * client. This will always be an AMF0 Array with one AMF3 encoded element.
	 */
	public static final byte AMF0_ARRAY = 0x0a;
	/**
	 * Constant for the AMF3 data type in AMF0. Each AMF3 object contained in
	 * AMF0 data carries this type code as identifier.
	 */
	public static final byte AMF3_IN_AMF0 = 0x11;
}
