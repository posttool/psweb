package com.pagesociety.web.gateway;

import java.io.Serializable;



	public class JavaGatewayResponse implements Serializable
	{

		public static final int MODULE_METHOD_INVOKE_OK 	= 0x10;
		public static final int MODULE_METHOD_INVOKE_ERROR  = 0x20;
		public static final int MODULE_METHOD_SERVER_ERROR  = 0x30;

		public long 	request_time;
		public long 	response_time;
		public String 	routing_id;
		public Object 	value;
		public int		response_code;
	}