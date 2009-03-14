package com.pagesociety.web.upload;

public interface MultipartFormConstants
{
	public static final boolean TESTING = false;
	//DEFAULT SIZE THRESHOLD IS THE NUMBER OF BYTES ABOVE WHICH THE FILE ITEM SHOULD
	//BE PARSED INTO A FILE INSTEAD OF INTO MEMORY
	public static final int DEFAULT_SIZE_THRESHOLD = 10240;
	public static final String CONTENT_TYPE = "content-type";
	public static final String CONTENT_LENGTH = "content-length";
	public static final String MULTI_PART = "multipart/form-data";
}
