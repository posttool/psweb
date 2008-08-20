package com.pagesociety.web.amf;

import java.nio.charset.Charset;

public class AmfConstants
{
	public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	public static final String AMF_MIME_TYPE = "application/x-amf";
	public static final int DEFAULT_BUFFER_SIZE = 4096;
	public static final int DEFAULT_VERSION = 3;

	public static final String RESULT_CALLBACK = "/onResult";
	public static final String ERROR_CALLBACK = "/onStatus";
}
