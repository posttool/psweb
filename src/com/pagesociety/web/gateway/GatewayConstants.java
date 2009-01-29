package com.pagesociety.web.gateway;

public class GatewayConstants
{
	// mime type prefixes
	public static final String MIME_TYPE_PREFIX_IMAGE = "image";
	public static final String MIME_TYPE_PREFIX_VIDEO = "video";
	public static final String MIME_TYPE_PREFIX_AUDIO = "audio";
	public static final String MIME_TYPE_PREFIX_APPLICATION = "application";
	public static final String[] MIME_TYPE_PREFIXES = new String[] {
			MIME_TYPE_PREFIX_IMAGE, MIME_TYPE_PREFIX_VIDEO, MIME_TYPE_PREFIX_AUDIO,
			MIME_TYPE_PREFIX_APPLICATION };
	public static final String MIME_TYPE_TEXT 	= "text/plain";
	public static final String MIME_TYPE_HTML 	= "text/html";
	public static final String MIME_TYPE_CSS 	= "text/css";
	public static final String MIME_TYPE_JS 	= "application/x-javascript";
	// suffixes
	public static final String SUFFIX_JSON = ".json";
	public static final String SUFFIX_AMF  = ".amf";
	public static final String SUFFIX_FORM = ".form";
	public static final String SUFFIX_RAW  = ".raw";
	// free-marker suffixes
	public static final String SUFFIX_FREEMARKER_HTML = ".fhtml";
	public static final String SUFFIX_FREEMARKER_CSS  = ".fcss";
	public static final String SUFFIX_FREEMARKER_JS   = ".fjs";
	public static final String[] SUFFIXES_FREEMARKER  = new String[] {
			SUFFIX_FREEMARKER_HTML, SUFFIX_FREEMARKER_CSS, SUFFIX_FREEMARKER_JS };
	// session cookie id
	public static final String SESSION_ID_KEY = "ps_session_id";
}
